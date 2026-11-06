package local.tv.hue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class LightsActivity extends Activity {
 private static final Uri LAMPS=Uri.parse("content://org.droidtv.ambihue.provider/HueLampsInfo");
 private static final Uri BRIDGE=Uri.parse("content://org.droidtv.ambihue.provider/HueBridgeInfo");
 private static final String[] COLUMNS={"LampID","Angle","Placement","Brightness","ConfigureStatus"};
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final LinkedHashMap<Integer,CheckBox> choices=new LinkedHashMap<>();
 private final Map<Integer,Integer> positions=new LinkedHashMap<>();
 private final Map<Integer,Spinner> brightness=new LinkedHashMap<>();
 private Map<Integer,ContentValues> nativeRowsAtLoad;
 private String nativeMacAtLoad;
 private final ArrayList<Button> controls=new ArrayList<>();
 private Map<Integer,ContentValues> initial;
 private SharedPreferences prefs;
 private final ThreadLocal<String> diagnosticId=ThreadLocal.withInitial(Diagnostics::id);
 private String discoveryId="discovery";
 private LinearLayout panel;
 private EditText hostInput;
 private TextView status;
 private TextView pairSummary;private Button bridgePairButton,appPairButton;
 private Spinner bridgePicker;private ArrayAdapter<String> bridgeAdapter;
 private final List<String> discoveredHosts=new ArrayList<>(),discoveredIds=new ArrayList<>();
 private final ArrayDeque<NsdServiceInfo> resolveQueue=new ArrayDeque<>();
 private String bridgeName="";
 private String host,token,bridgeMac,clientKey;
 private boolean showingLights;
 private static final long BRIDGE_WAIT_MS=120000;
 private int countdownGeneration;
 private int discoveryGeneration;
 private void stopCountdown(){countdownGeneration++;}
 private void countdown(String key,long deadline,Runnable expired){
  final int generation=++countdownGeneration;
  Runnable tick=new Runnable(){public void run(){if(closing||generation!=countdownGeneration)return;long remaining=Math.max(0,(deadline-android.os.SystemClock.elapsedRealtime()+999)/1000);status.setText(String.format(getResources().getConfiguration().locale,SetupStrings.get(LightsActivity.this,key),remaining));if(remaining==0){stopCountdown();expired.run();}else panel.postDelayed(this,Math.min(1000,Math.max(1,deadline-android.os.SystemClock.elapsedRealtime())));}};
  tick.run();
 }
 private boolean busy,closing,resolving;
 private NsdManager nsd;
 private NsdManager.DiscoveryListener discovery;
 private interface Task {void run() throws Exception;}
 private static class HueError extends IOException {final int type;HueError(int type,String text){super(text);this.type=type;}}
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);Diagnostics.init(this);Diagnostics.event("lifecycle","lights.create",""); prefs=getSharedPreferences("hue_bridge",MODE_PRIVATE);
  connectionScreen();
 }
 private void base(String title) {
  stopCountdown();
  choices.clear(); positions.clear();brightness.clear();controls.clear();
  panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
  panel.setPadding(72,32,72,40); panel.setBackgroundColor(Color.rgb(25,38,65));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(panel);setContentView(scroll);
  text(title,27);
  status=text("",17);
 }
 private TextView text(String value,int size) {
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.WHITE);
  t.setPadding(0,8,0,12);panel.addView(t);return t;
 }
 private Button button(String label,Runnable action) {
  Button b=new Button(this);b.setText(label);b.setTextSize(20);b.setAllCaps(false);
  b.setPadding(24,0,24,0);b.setGravity(Gravity.CENTER);b.setMinHeight(128);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,128);lp.setMargins(0,8,0,8);
  panel.addView(b,lp);controls.add(b);b.setOnClickListener(v -> {if(!busy) action.run();});return b;
 }
 private void connectionScreen() {
  persistDraft();showingLights=false;
  base(SetupStrings.get(this,"setup"));
  text(SetupStrings.get(this,"help"),18);
  if(getSharedPreferences("native_setup_recovery",0).contains("snapshot")){
   status.setText(SetupStrings.get(this,"recovery"));button(SetupStrings.get(this,"recover"),this::recover);button(Strings.get(this,"back"),this::finish);return;
  }
  hostInput=new EditText(this);hostInput.setSingleLine(true);hostInput.setTextColor(Color.WHITE);
  hostInput.setTextSize(20);hostInput.setHint(Strings.get(this,"ip_hint"));hostInput.setHintTextColor(Color.LTGRAY);
  hostInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
  hostInput.setText(prefs.getString("host",""));panel.addView(hostInput,new LinearLayout.LayoutParams(-1,110));
  discoveredHosts.clear();discoveredIds.clear();bridgePicker=new Spinner(this);bridgeAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new ArrayList<String>(Arrays.asList(FeatureStrings.get(this,"select_bridge"))));bridgePicker.setAdapter(bridgeAdapter);panel.addView(bridgePicker,new LinearLayout.LayoutParams(-1,96));
  bridgePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,android.view.View v,int position,long id){if(position>0&&position<=discoveredHosts.size()&&hostInput!=null)hostInput.setText(discoveredHosts.get(position-1));}public void onNothingSelected(AdapterView<?> p){}});
  pairSummary=text("",17);
  button(Strings.get(this,"discover"),this::discover);
  bridgePairButton=button(SetupStrings.get(this,"pair"),() -> load(true));
  appPairButton=button(AppPairing.text(this,0),this::pairApp);
  button(Strings.get(this,"load"),() -> load(false));
  button(Strings.get(this,"back"),this::back);
  status.setText(Strings.get(this,"network_required"));
  refreshPairStatus();
 }
 private void refreshPairStatus(){if(showingLights||pairSummary==null)return;boolean bridge=!prefs.getString("token","").isEmpty()&&!prefs.getString("clientkey","").isEmpty(),app=UiStatus.paired(this);bridgePairButton.setText(UiColors.marks((bridge?"✓ ":"✕ ")+SetupStrings.get(this,"pair")));appPairButton.setText(UiColors.marks((app?"✓ ":"✕ ")+AppPairing.text(this,0)));pairSummary.setText(UiColors.summary(this,FeatureStrings.get(this,"bridge")+": "+prefs.getString("name",prefs.getString("configured_name",prefs.getString("mac",FeatureStrings.get(this,"not_ready"))))+" · "+prefs.getString("host","")+"\n"+SetupStrings.get(this,"pair")+": "+(bridge?"✓ "+FeatureStrings.get(this,"stored"):"✕ "+FeatureStrings.get(this,"not_ready"))+"\n"+FeatureStrings.get(this,"permission")+": "+(PairingAccessibility.ready()?"✓ "+FeatureStrings.get(this,"ready"):"✕ "+FeatureStrings.get(this,"not_ready"))+"\n"+AppPairing.text(this,0)+": "+(app?"✓ "+FeatureStrings.get(this,"stored"):"✕ "+FeatureStrings.get(this,"not_ready"))));}
 private void pairApp(){
  if(busy)return;stopDiscovery();getSharedPreferences("tv_pair_flow",0).edit().putBoolean("requested",true).apply();
  if(AppPairing.prepare(this,message->status.setText(message)))status.setText(SetupStrings.get(this,"pair_ready"));refreshPairStatus();
 }
 private static String normalize(String s) {return s==null?"":s.replaceAll("[^a-fA-F0-9]", "").toLowerCase(Locale.ROOT);}
 private String tvMac() throws Exception {
  try(Cursor c=getContentResolver().query(BRIDGE,new String[]{"MacAddr"},null,null,null)) {
   if(c!=null && c.moveToFirst()) {String mac=normalize(c.getString(0));if(mac.length()==12)return mac;}
  }
  return "";
 }
 private Map<Integer,ContentValues> readSelection() throws Exception {
  Map<Integer,ContentValues> rows=new LinkedHashMap<>();
  try(Cursor c=getContentResolver().query(LAMPS,COLUMNS,null,null,"LampID ASC")) {
   if(c==null)throw new IOException("Databáze světel není dostupná.");
   while(c.moveToNext()) {ContentValues v=new ContentValues();for(int i=0;i<COLUMNS.length;i++)v.put(COLUMNS[i],c.getInt(i)); rows.put(c.getInt(0),v);}
  }
  return rows;
 }
 private Object request(String host,String path,String body) throws Exception {
  if(!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))throw new IOException("Zadejte IPv4 adresu můstku, například 192.168.2.11.");
  HttpURLConnection c=(HttpURLConnection)new URL("http://"+host+path).openConnection();
  String id=diagnosticId.get();long began=android.os.SystemClock.elapsedRealtime();
  String kind=path.equals("/api/config")?"config":path.endsWith("/lights")?"lights":"pair";
  Diagnostics.event(id,"bridge.request.begin","kind="+kind+" method="+(body==null?"GET":"POST"));
  c.setConnectTimeout(6000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);
  try {
   if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.getBytes("UTF-8"));}}
   int http=c.getResponseCode();Diagnostics.event(id,"bridge.response","http="+http+" duration_ms="+(android.os.SystemClock.elapsedRealtime()-began));
   if(http!=200)throw new IOException("Můstek odpověděl HTTP "+http+".");
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>2097152)throw new IOException("Příliš velká odpověď můstku.");}}
   Object data=new JSONTokener(out.toString("UTF-8")).nextValue();
   if(data instanceof JSONArray){JSONObject first=((JSONArray)data).optJSONObject(0);JSONObject error=first==null?null:first.optJSONObject("error");if(error!=null){int type=error.optInt("type");Diagnostics.event(id,"bridge.api.error","type="+type);if(type==101)throw new HueError(type,"Stiskněte tlačítko na Hue můstku a zkuste Spárovat znovu.");if(type==1)throw new HueError(type,"Přístup vypršel. Stiskněte tlačítko můstku a použijte Spárovat.");throw new HueError(type,"Můstek odmítl požadavek (chyba "+type+").");}}
   return data;
  }catch(Exception e){Diagnostics.error(id,"bridge.request.error",e);throw e;}finally {c.disconnect();Diagnostics.event(id,"bridge.request.end","duration_ms="+(android.os.SystemClock.elapsedRealtime()-began));}
 }
 private String checkBridge(String candidate,String expected) throws Exception {
  Object obj=request(candidate,"/api/config",null);
  if(!(obj instanceof JSONObject))throw new IOException("Adresa neodpovídá Hue můstku.");
  String actual=normalize(((JSONObject)obj).optString("mac"));
  if(actual.length()!=12)throw new IOException("Invalid Bridge identity");
  if(expected.isEmpty())return actual;
  String bridgeid=normalize(((JSONObject)obj).optString("bridgeid"));
  String expectedId=expected.substring(0,6)+"fffe"+expected.substring(6);
  if(!expected.equals(actual)&&!expectedId.equals(bridgeid))throw new IOException("Tento můstek není napárovaný s TV. Vyberte správnou IP adresu.");
  return actual;
 }
 private void task(String message,Task work) {
  if(busy)return;busy=true;for(Button b:controls)b.setEnabled(false);if(hostInput!=null)hostInput.setEnabled(false);status.setText(message);
  worker.execute(() -> {
   String id=Diagnostics.id();diagnosticId.set(id);Diagnostics.event(id,"lights.task.begin","");Diagnostics.network(this,id);
   try {work.run();Diagnostics.event(id,"lights.task.ok","");}catch(Exception e){Diagnostics.error(id,"lights.task.error",e);ui(() -> status.setText(Strings.get(this,"operation_failed",id)));}
   finally {Diagnostics.event(id,"lights.task.end","");diagnosticId.remove();ui(() -> {stopCountdown();busy=false;for(Button b:controls)b.setEnabled(true);if(hostInput!=null)hostInput.setEnabled(true);});}
  });
 }
 private void ui(Runnable r){runOnUiThread(() -> {if(!closing&&!isFinishing())r.run();});}
 private void load(boolean pair) {
  String candidate=hostInput.getText().toString().trim();
  stopDiscovery();final long deadline=android.os.SystemClock.elapsedRealtime()+BRIDGE_WAIT_MS;
  status.setTextColor(pair?Color.rgb(255,217,92):Color.WHITE);status.setTypeface(null,pair?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);
  task(Strings.get(this,pair?"pair_press":"connecting"),() -> {
   String mac=checkBridge(candidate,"");
   bridgeName=((JSONObject)request(candidate,"/api/config",null)).optString("name",mac);
   prefs.edit().putString("host",candidate).apply();
   String key=prefs.getString("mac","").equals(mac)?prefs.getString("token",""):"";
   String streamKey=prefs.getString("mac","").equals(mac)?prefs.getString("clientkey",""):"";
   if(!pair&&streamKey.isEmpty()&&mac.equals(tvMac())){try(NativeLink link=new NativeLink(this)){String existingUser=link.call(4),existingKey=link.call(7);if(existingUser!=null&&existingKey!=null&&!existingKey.isEmpty()){key=existingUser;streamKey=existingKey;}}}
   if(pair){
    JSONArray result=null;
    while(result==null){
     if(closing)throw new IOException("Párování zrušeno.");
     if(android.os.SystemClock.elapsedRealtime()>=deadline)throw new SocketTimeoutException("Bridge pairing deadline");
     try{result=(JSONArray)request(candidate,"/api",new JSONObject().put("devicetype","ambilight_hue#tv_setup").put("generateclientkey",true).toString());}
     catch(HueError error){if(error.type!=101||android.os.SystemClock.elapsedRealtime()>=deadline)throw error;Thread.sleep(Math.min(1200,Math.max(1,deadline-android.os.SystemClock.elapsedRealtime())));}
    }
    key=result.getJSONObject(0).getJSONObject("success").getString("username");
    streamKey=result.getJSONObject(0).getJSONObject("success").getString("clientkey");
   }
   if(key.isEmpty())throw new IOException("Stiskněte fyzické tlačítko můstku a zvolte Spárovat.");
   Object response=request(candidate,"/api/"+URLEncoder.encode(key,"UTF-8")+"/lights",null);
   if(!(response instanceof JSONObject))throw new IOException("Můstek nevrátil seznam světel.");
   JSONObject lamps=(JSONObject)response;
   ArrayList<Integer> ids=new ArrayList<>();for(Iterator<String> it=lamps.keys();it.hasNext();){String id=it.next();JSONObject lamp=lamps.getJSONObject(id);String type=lamp.optString("type");if("Color light".equals(type)||"Extended color light".equals(type)){int n=Integer.parseInt(id);if(n>0)ids.add(n);}}
   Collections.sort(ids);nativeRowsAtLoad=readSelection();nativeMacAtLoad=tvMac();Map<Integer,ContentValues> selected=mac.equals(nativeMacAtLoad)?nativeRowsAtLoad:new LinkedHashMap<>();
   prefs.edit().putString("host",candidate).putString("mac",mac).putString("name",bridgeName).putString("token",key).putString("clientkey",streamKey).apply();
   host=candidate;token=key;bridgeMac=mac;clientKey=streamKey;
   if(pair){ui(()->{status.setTextColor(Color.WHITE);status.setTypeface(null,android.graphics.Typeface.NORMAL);new AlertDialog.Builder(this).setTitle(SetupStrings.get(this,"pair_success")).setMessage(SetupStrings.get(this,"pair_next")).setCancelable(false).setPositiveButton("OK",(d,w)->connectionScreen()).show();});return;}
   selected=restoreDraft(mac,selected);
   final Map<Integer,ContentValues> displayed=selected;
   ui(() -> showLights(lamps,ids,displayed));
  });
  if(pair)countdown("pair_wait",deadline,()->{});
 }
 private void showLights(JSONObject lamps,List<Integer> ids,Map<Integer,ContentValues> selected) {
  stopDiscovery();hostInput=null;initial=selected;
  base(Strings.get(this,"select_title"));showingLights=true;
  text(SetupStrings.get(this,"select"),18);
  for(int id:ids) {
   JSONObject lamp=lamps.optJSONObject(String.valueOf(id));
   CheckBox box=new CheckBox(this);box.setText(lamp.optString("name",Strings.get(this,"light",id))+"  (ID "+id+")");
   box.setTextColor(Color.WHITE);box.setTextSize(21);box.setPadding(20,8,20,8);box.setMinHeight(128);box.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(90,220,240)));
   box.setChecked(selected.containsKey(id)&&(!selected.get(id).containsKey("DraftSelected")||selected.get(id).getAsInteger("DraftSelected")==1));box.setFocusable(true);panel.addView(box,new LinearLayout.LayoutParams(-1,128));choices.put(id,box);box.setOnCheckedChangeListener((b,checked)->persistDraft());
   ContentValues row=selected.get(id);
   int angle=row==null?5:Math.max(1,Math.min(10,row.getAsInteger("Angle")));positions.put(id,angle);Button position=button(SetupStrings.get(this,"position")+": "+SetupStrings.positions(this)[angle-1],()->{});position.setOnClickListener(v->{if(!busy)PositionPicker.show(this,lamp.optString("name",Strings.get(this,"light",id)),positions.get(id),value->{positions.put(id,value);position.setText(SetupStrings.get(this,"position")+": "+SetupStrings.positions(this)[value-1]);persistDraft();});});
   text(SetupStrings.get(this,"brightness"),17);Spinner bright=new Spinner(this);bright.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"1","2","3","4","5","6","7","8","9","10"}));bright.setSelection(row==null?5:Math.max(0,Math.min(9,row.getAsInteger("Brightness")-1)));panel.addView(bright,new LinearLayout.LayoutParams(-1,90));brightness.put(id,bright);bright.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,android.view.View v,int pos,long item){persistDraft();}public void onNothingSelected(AdapterView<?> p){}});
  }
  int hidden=0;for(int id:selected.keySet())if(!choices.containsKey(id))hidden++;
  status.setText(Strings.get(this,"compatible",ids.size())+(hidden>0?Strings.get(this,"hidden",hidden):""));
  if(ids.isEmpty())text(Strings.get(this,"no_lights"),18);
  Button save=button(SetupStrings.get(this,"save"),this::save);save.setEnabled(!ids.isEmpty());
  button(Strings.get(this,"back"),this::back);
  if(!choices.isEmpty())choices.values().iterator().next().requestFocus();
 }
 private void save() {
  persistDraft();
  Set<Integer> desired=new LinkedHashSet<>();for(Map.Entry<Integer,ContentValues> e:initial.entrySet())if(!e.getValue().containsKey("DraftSelected")||e.getValue().getAsInteger("DraftSelected")==1)desired.add(e.getKey());
  for(Map.Entry<Integer,CheckBox> entry:choices.entrySet()){if(entry.getValue().isChecked())desired.add(entry.getKey());else desired.remove(entry.getKey());}
  if(desired.size()>10){status.setText(Strings.get(this,"too_many"));return;}
  if(desired.isEmpty()){status.setText(SetupStrings.get(this,"choose"));return;}
  if(clientKey==null||clientKey.isEmpty()){status.setText(SetupStrings.get(this,"pair_needed"));return;}
  Map<Integer,ContentValues> rows=new LinkedHashMap<>();for(int id:desired){ContentValues row=initial.containsKey(id)?new ContentValues(initial.get(id)):new ContentValues();row.remove("DraftSelected");row.put("LampID",id);if(positions.containsKey(id)){row.put("Angle",positions.get(id));row.put("Brightness",brightness.get(id).getSelectedItemPosition()+1);}if(!row.containsKey("Placement"))row.put("Placement",1);row.put("ConfigureStatus",1);rows.put(id,row);}
  SharedPreferences api=getSharedPreferences(TvApiClient.PREFS,0);
  if(api.getString("user","").isEmpty()||api.getString("password","").isEmpty()||api.getString("certificate","").isEmpty()){
   if(!getSharedPreferences("tv_pair_flow",0).getBoolean("requested",false)||!PairingAccessibility.ready()){String message=SetupStrings.get(this,"app_pair_required");status.setText(message);new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK",null).show();return;}
   busy=true;for(Button b:controls)b.setEnabled(false);status.setText(AppPairing.text(this,1));
   AppPairing.start(this,message->{if(closing||isFinishing()||isDestroyed())return;status.setText(message);if(!message.equals(AppPairing.text(this,6))){busy=false;for(Button b:controls)b.setEnabled(true);}},()->{if(!closing&&!isFinishing()&&!isDestroyed())writeConfiguration(rows,true);});
   return;
  }
  writeConfiguration(rows,false);
 }
 private void recover(){task(Strings.get(this,"saving"),()->{SharedPreferences recovery=getSharedPreferences("native_setup_recovery",0);JSONObject snapshot=new JSONObject(recovery.getString("snapshot",""));if(snapshot.has("reset_tv_api")){JSONObject api=snapshot.getJSONObject("reset_tv_api");SharedPreferences.Editor restored=getSharedPreferences(TvApiClient.PREFS,0).edit();for(String field:new String[]{"user","password","certificate","host"})restored.putString(field,api.getString(field));if(!restored.commit())throw new IOException("TV API restoration failed");}try(NativeLink link=new NativeLink(this)){new TvApiClient(this).setPower(false,()->NativeLink.notifyService(this,false));link.awaitFree();link.restore(snapshot);boolean on="On".equals(snapshot.getString("power"));new TvApiClient(this).setPower(on,()->NativeLink.notifyService(this,on));if(!recovery.edit().clear().commit())throw new IOException("Recovery marker");}ui(()->{connectionScreen();status.setText(SetupStrings.get(this,"restored"));});});}
 private void writeConfiguration(Map<Integer,ContentValues> rows,boolean newlyPaired){
  for(CheckBox box:choices.values())box.setEnabled(false);
  task(Strings.get(this,"saving"),()->{
   SharedPreferences recovery=getSharedPreferences("native_setup_recovery",0);JSONObject snapshot=null;
   try(NativeLink link=new NativeLink(this)){
    if(recovery.contains("snapshot"))throw new IOException("Recovery required");
    checkBridge(host,bridgeMac);if(!tvMac().equals(nativeMacAtLoad)||!readSelection().equals(nativeRowsAtLoad))throw new IOException("Native configuration changed");
    TvApiClient api=new TvApiClient(this);String power=api.getPower();if(!"On".equals(power)&&!"Off".equals(power))throw new IOException("Unknown power state");
    snapshot=link.snapshot(power);if(!recovery.edit().putString("snapshot",snapshot.toString()).commit())throw new IOException("Backup failed");
    try{
     api.setPower(false,()->NativeLink.notifyService(this,false));
     link.awaitFree();
     link.clear("HueLampsInfo");link.clear("HueGroupInfo");link.clear("HueBridgeInfo");
     link.insert("HueBridgeInfo",new JSONObject().put("MacAddr",bridgeMac));link.credentials(bridgeMac,token,clientKey,host);
     for(ContentValues row:rows.values()){JSONObject value=new JSONObject();for(String key:row.keySet())value.put(key,row.getAsInteger(key));link.insert("HueLampsInfo",value);}
     if(!readSelection().equals(rows))throw new IOException("Configuration verification failed");
     api.setPower(true,()->NativeLink.notifyService(this,true));
     if(!recovery.edit().clear().commit())throw new IOException("Commit marker failed");
     initial=readSelection();nativeRowsAtLoad=initial;nativeMacAtLoad=bridgeMac;
     prefs.edit().putString("configured_name",bridgeName).putString("configured_mac",bridgeMac).putString("configured_host",host).commit();
     getSharedPreferences("light_drafts",0).edit().remove(bridgeMac).commit();
     Diagnostics.event(diagnosticId.get(),"setup.commit.ok","lamps="+rows.size());ui(()->{String message=(newlyPaired?AppPairing.text(this,4)+"\n":"")+SetupStrings.get(this,"saved");status.setText(message);new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK",null).show();});
    }catch(Exception e){
     Diagnostics.error(diagnosticId.get(),"setup.commit.failed",e);
     try{api.setPower(false,()->NativeLink.notifyService(this,false));link.awaitFree();link.restore(snapshot);boolean on="On".equals(snapshot.getString("power"));api.setPower(on,()->NativeLink.notifyService(this,on));if(!recovery.edit().clear().commit())throw new IOException("Rollback marker");String id=diagnosticId.get();ui(()->status.setText(SetupStrings.get(this,"failed_restored")+id));}
     catch(Exception rollback){Diagnostics.error(diagnosticId.get(),"setup.rollback.failed",rollback);String id=diagnosticId.get();ui(()->{connectionScreen();status.setText(SetupStrings.get(this,"recovery")+id);});}
    }
   }finally{ui(()->{for(CheckBox box:choices.values())box.setEnabled(true);});}
  });
 }
 private void discover() {
  if(discovery!=null){status.setText(Strings.get(this,"search_running"));return;}
  discoveredHosts.clear();discoveredIds.clear();bridgeAdapter.clear();bridgeAdapter.add(FeatureStrings.get(this,"select_bridge"));
  discoveryId=Diagnostics.id();Diagnostics.event(discoveryId,"bridge.discovery.begin","");
  final int searchGeneration=++discoveryGeneration;
  status.setText(Strings.get(this,"searching"));nsd=(NsdManager)getSystemService(NSD_SERVICE);
  discovery=new NsdManager.DiscoveryListener(){
   public void onDiscoveryStarted(String s){}
   public void onDiscoveryStopped(String s){}
   public void onServiceLost(NsdServiceInfo s){}
   public void onStartDiscoveryFailed(String s,int e){Diagnostics.event(discoveryId,"bridge.discovery.start.error","code="+e);ui(()->{stopDiscovery();status.setText(Strings.get(LightsActivity.this,"search_unavailable"));});}
   public void onStopDiscoveryFailed(String s,int e){Diagnostics.event(discoveryId,"bridge.discovery.stop.error","code="+e);}
   public void onServiceFound(NsdServiceInfo s){
    ui(()->{if(searchGeneration==discoveryGeneration){resolveQueue.add(s);resolveNext(searchGeneration);}});
   }
  };
  try{nsd.discoverServices("_hue._tcp.",NsdManager.PROTOCOL_DNS_SD,discovery);countdown("discovery_wait",android.os.SystemClock.elapsedRealtime()+BRIDGE_WAIT_MS,()->{if(discovery!=null){stopDiscovery();if(!busy)status.setText(Strings.get(this,"search_done"));}});}catch(RuntimeException e){Diagnostics.error(discoveryId,"bridge.discovery.error",e);stopDiscovery();status.setText(Strings.get(this,"search_unavailable"));}
 }
 private void resolveNext(int generation){if(resolving||resolveQueue.isEmpty()||generation!=discoveryGeneration||discovery==null)return;resolving=true;NsdServiceInfo service=resolveQueue.remove();try{nsd.resolveService(service,new NsdManager.ResolveListener(){
  public void onResolveFailed(NsdServiceInfo s,int code){ui(()->{if(generation!=discoveryGeneration)return;resolving=false;resolveNext(generation);});}
  public void onServiceResolved(NsdServiceInfo s){ui(()->{if(generation!=discoveryGeneration)return;resolving=false;resolveNext(generation);if(s.getHost()==null)return;String ip=s.getHost().getHostAddress();String advertisedId=s.getServiceName();byte[] bridgeId=s.getAttributes().get("bridgeid");if(bridgeId!=null)advertisedId=new String(bridgeId,java.nio.charset.StandardCharsets.UTF_8);addDiscoveredBridge(generation,s.getServiceName(),ip,advertisedId);worker.execute(()->{try{JSONObject info=(JSONObject)request(ip,"/api/config",null);String mac=normalize(info.optString("mac"));if(mac.length()!=12)return;String name=info.optString("name",s.getServiceName());ui(()->addDiscoveredBridge(generation,name,ip,mac));}catch(Exception e){Diagnostics.error(discoveryId,"bridge.discovery.candidate.error",e);}});});}
 });}catch(RuntimeException e){resolving=false;Diagnostics.error(discoveryId,"bridge.discovery.resolve.error",e);resolveNext(generation);}}
 private void addDiscoveredBridge(int generation,String name,String ip,String id){if(generation!=discoveryGeneration||bridgeAdapter==null||hostInput==null)return;int index=discoveredIds.indexOf(id);if(index<0)index=discoveredHosts.indexOf(ip);String label=name+" · "+ip+" · "+id;if(index<0){discoveredIds.add(id);discoveredHosts.add(ip);bridgeAdapter.add(label);}else{discoveredIds.set(index,id);discoveredHosts.set(index,ip);String old=bridgeAdapter.getItem(index+1);if(!label.equals(old)){bridgeAdapter.remove(old);bridgeAdapter.insert(label,index+1);}}}
 private void stopDiscovery(){discoveryGeneration++;resolveQueue.clear();resolving=false;stopCountdown();if(discovery!=null){try{nsd.stopServiceDiscovery(discovery);}catch(RuntimeException ignored){Diagnostics.error(discoveryId,"bridge.discovery.stop.error",ignored);}discovery=null;}}
 private String nativeSignature(){return nativeMacAtLoad+":"+String.valueOf(nativeRowsAtLoad);}
 private void persistDraft(){
  if(!showingLights||bridgeMac==null||initial==null)return;
  try{JSONObject draft=new JSONObject().put("native",nativeSignature());JSONArray rows=new JSONArray();for(Map.Entry<Integer,CheckBox> e:choices.entrySet()){int id=e.getKey();if(!positions.containsKey(id)||!brightness.containsKey(id))continue;ContentValues original=initial.get(id);JSONObject row=new JSONObject().put("LampID",id).put("Angle",positions.get(id)).put("Brightness",brightness.get(id).getSelectedItemPosition()+1).put("Placement",original==null?1:original.getAsInteger("Placement")).put("ConfigureStatus",1).put("DraftSelected",e.getValue().isChecked()?1:0);rows.put(row);}draft.put("rows",rows);getSharedPreferences("light_drafts",0).edit().putString(bridgeMac,draft.toString()).apply();}catch(Exception e){Diagnostics.error("draft","draft.save.failed",e);}
 }
 private Map<Integer,ContentValues> restoreDraft(String mac,Map<Integer,ContentValues> saved){
  try{String value=getSharedPreferences("light_drafts",0).getString(mac,"");if(value.isEmpty())return saved;JSONObject draft=new JSONObject(value);if(!nativeSignature().equals(draft.getString("native")))return saved;Map<Integer,ContentValues> restored=new LinkedHashMap<>(saved);JSONArray rows=draft.getJSONArray("rows");for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);ContentValues v=new ContentValues();for(String key:new String[]{"LampID","Angle","Brightness","Placement","ConfigureStatus","DraftSelected"})v.put(key,row.getInt(key));restored.put(row.getInt("LampID"),v);}return restored;}catch(Exception e){Diagnostics.error("draft","draft.load.failed",e);return saved;}
 }
 @Override protected void onPause(){persistDraft();super.onPause();}
 @Override protected void onResume(){super.onResume();refreshPairStatus();if(!showingLights&&!busy&&!UiStatus.paired(this)&&getSharedPreferences("tv_pair_flow",0).getBoolean("requested",false)&&PairingAccessibility.ready())status.setText(SetupStrings.get(this,"pair_ready"));}
 private void back(){persistDraft();if(showingLights)connectionScreen();else finish();}
 @Override public void onBackPressed(){if(busy){status.setText(Strings.get(this,"wait"));return;}back();}
 @Override protected void onDestroy(){closing=true;stopDiscovery();worker.shutdown();super.onDestroy();}
}

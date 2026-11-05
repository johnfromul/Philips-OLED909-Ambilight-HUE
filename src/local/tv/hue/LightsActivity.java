package local.tv.hue;

import android.app.Activity;
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
 private String host,token,bridgeMac,clientKey;
 private boolean busy,closing,resolving;
 private NsdManager nsd;
 private NsdManager.DiscoveryListener discovery;
 private interface Task {void run() throws Exception;}
 private static class HueError extends IOException {final int type;HueError(int type,String text){super(text);this.type=type;}}
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);Diagnostics.init(this);Diagnostics.event("lifecycle","lights.create",""); prefs=getSharedPreferences("hue_bridge",MODE_PRIVATE);
  connectionScreen();
  if(!getSharedPreferences("native_setup_recovery",0).contains("snapshot")&&!prefs.getString("token","").isEmpty())load(false);
 }
 private void base(String title) {
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
  base(SetupStrings.get(this,"setup"));
  text(SetupStrings.get(this,"help"),18);
  if(getSharedPreferences("native_setup_recovery",0).contains("snapshot")){
   status.setText(SetupStrings.get(this,"recovery"));button(SetupStrings.get(this,"recover"),this::recover);button(Strings.get(this,"back"),this::finish);return;
  }
  hostInput=new EditText(this);hostInput.setSingleLine(true);hostInput.setTextColor(Color.WHITE);
  hostInput.setTextSize(20);hostInput.setHint(Strings.get(this,"ip_hint"));hostInput.setHintTextColor(Color.LTGRAY);
  hostInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
  hostInput.setText(prefs.getString("host",""));panel.addView(hostInput,new LinearLayout.LayoutParams(-1,110));
  button(Strings.get(this,"discover"),this::discover);
  button(Strings.get(this,"load"),() -> load(false));
  button(Strings.get(this,"pair"),() -> load(true));
  button(Strings.get(this,"back"),this::finish);
  status.setText(Strings.get(this,"network_required"));
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
   finally {Diagnostics.event(id,"lights.task.end","");diagnosticId.remove();ui(() -> {busy=false;for(Button b:controls)b.setEnabled(true);if(hostInput!=null)hostInput.setEnabled(true);});}
  });
 }
 private void ui(Runnable r){runOnUiThread(() -> {if(!closing&&!isFinishing())r.run();});}
 private void load(boolean pair) {
  String candidate=hostInput.getText().toString().trim();
  task(Strings.get(this,"connecting"),() -> {
   String mac=checkBridge(candidate,"");
   prefs.edit().putString("host",candidate).apply();
   String key=prefs.getString("mac","").equals(mac)?prefs.getString("token",""):"";
   String streamKey=prefs.getString("mac","").equals(mac)?prefs.getString("clientkey",""):"";
   if(!pair&&streamKey.isEmpty()&&mac.equals(tvMac())){try(NativeLink link=new NativeLink(this)){String existingUser=link.call(4),existingKey=link.call(7);if(existingUser!=null&&existingKey!=null&&!existingKey.isEmpty()){key=existingUser;streamKey=existingKey;}}}
   if(pair){
    JSONArray result=null;
    long deadline=android.os.SystemClock.elapsedRealtime()+45000;
    while(result==null){
     if(closing)throw new IOException("Párování zrušeno.");
     try{result=(JSONArray)request(candidate,"/api",new JSONObject().put("devicetype","ambilight_hue#tv_setup").put("generateclientkey",true).toString());}
     catch(HueError error){if(error.type!=101||android.os.SystemClock.elapsedRealtime()>=deadline)throw error;ui(()->status.setText(Strings.get(this,"pair_press")));Thread.sleep(1200);}
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
   prefs.edit().putString("host",candidate).putString("mac",mac).putString("token",key).putString("clientkey",streamKey).apply();
   host=candidate;token=key;bridgeMac=mac;clientKey=streamKey;
   ui(() -> showLights(lamps,ids,selected));
  });
 }
 private void showLights(JSONObject lamps,List<Integer> ids,Map<Integer,ContentValues> selected) {
  stopDiscovery();hostInput=null;initial=selected;
  base(Strings.get(this,"select_title"));
  text(SetupStrings.get(this,"select"),18);
  for(int id:ids) {
   JSONObject lamp=lamps.optJSONObject(String.valueOf(id));
   CheckBox box=new CheckBox(this);box.setText(lamp.optString("name",Strings.get(this,"light",id))+"  (ID "+id+")");
   box.setTextColor(Color.WHITE);box.setTextSize(21);box.setPadding(20,8,20,8);box.setMinHeight(128);box.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(90,220,240)));
   box.setChecked(selected.containsKey(id));box.setFocusable(true);panel.addView(box,new LinearLayout.LayoutParams(-1,128));choices.put(id,box);
   ContentValues row=selected.get(id);
   int angle=row==null?5:Math.max(1,Math.min(10,row.getAsInteger("Angle")));positions.put(id,angle);Button position=button(SetupStrings.get(this,"position")+": "+SetupStrings.positions(this)[angle-1],()->{});position.setOnClickListener(v->{if(!busy)PositionPicker.show(this,lamp.optString("name",Strings.get(this,"light",id)),positions.get(id),value->{positions.put(id,value);position.setText(SetupStrings.get(this,"position")+": "+SetupStrings.positions(this)[value-1]);});});
   text(SetupStrings.get(this,"brightness"),17);Spinner bright=new Spinner(this);bright.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"1","2","3","4","5","6","7","8","9","10"}));bright.setSelection(row==null?5:Math.max(0,Math.min(9,row.getAsInteger("Brightness")-1)));panel.addView(bright,new LinearLayout.LayoutParams(-1,90));brightness.put(id,bright);
  }
  int hidden=0;for(int id:selected.keySet())if(!choices.containsKey(id))hidden++;
  status.setText(Strings.get(this,"compatible",ids.size())+(hidden>0?Strings.get(this,"hidden",hidden):""));
  if(ids.isEmpty())text(Strings.get(this,"no_lights"),18);
  Button save=button(SetupStrings.get(this,"save"),this::save);save.setEnabled(!ids.isEmpty());
  button(Strings.get(this,"connection"),this::connectionScreen);button(Strings.get(this,"back"),this::finish);
  if(!choices.isEmpty())choices.values().iterator().next().requestFocus();
 }
 private void save() {
  Set<Integer> desired=new LinkedHashSet<>(initial.keySet());
  for(Map.Entry<Integer,CheckBox> entry:choices.entrySet()){if(entry.getValue().isChecked())desired.add(entry.getKey());else desired.remove(entry.getKey());}
  if(desired.size()>10){status.setText(Strings.get(this,"too_many"));return;}
  if(desired.isEmpty()){status.setText(SetupStrings.get(this,"choose"));return;}
  if(clientKey==null||clientKey.isEmpty()){status.setText(SetupStrings.get(this,"pair_needed"));return;}
  Map<Integer,ContentValues> rows=new LinkedHashMap<>();for(int id:desired){ContentValues row=initial.containsKey(id)?new ContentValues(initial.get(id)):new ContentValues();row.put("LampID",id);if(positions.containsKey(id)){row.put("Angle",positions.get(id));row.put("Brightness",brightness.get(id).getSelectedItemPosition()+1);}if(!row.containsKey("Placement"))row.put("Placement",1);row.put("ConfigureStatus",1);rows.put(id,row);}
  writeConfiguration(rows);
 }
 private void recover(){task(Strings.get(this,"saving"),()->{SharedPreferences recovery=getSharedPreferences("native_setup_recovery",0);JSONObject snapshot=new JSONObject(recovery.getString("snapshot",""));try(NativeLink link=new NativeLink(this)){new TvApiClient(this).setPower(false,()->NativeLink.notifyService(this,false));link.awaitFree();link.restore(snapshot);boolean on="On".equals(snapshot.getString("power"));new TvApiClient(this).setPower(on,()->NativeLink.notifyService(this,on));if(!recovery.edit().clear().commit())throw new IOException("Recovery marker");}ui(()->{connectionScreen();status.setText(SetupStrings.get(this,"restored"));});});}
 private void writeConfiguration(Map<Integer,ContentValues> rows){
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
     Diagnostics.event(diagnosticId.get(),"setup.commit.ok","lamps="+rows.size());ui(()->status.setText(SetupStrings.get(this,"saved")));
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
  discoveryId=Diagnostics.id();Diagnostics.event(discoveryId,"bridge.discovery.begin","");
  status.setText(Strings.get(this,"searching"));nsd=(NsdManager)getSystemService(NSD_SERVICE);
  discovery=new NsdManager.DiscoveryListener(){
   public void onDiscoveryStarted(String s){}
   public void onDiscoveryStopped(String s){}
   public void onServiceLost(NsdServiceInfo s){}
   public void onStartDiscoveryFailed(String s,int e){Diagnostics.event(discoveryId,"bridge.discovery.start.error","code="+e);ui(()->{stopDiscovery();status.setText(Strings.get(LightsActivity.this,"search_unavailable"));});}
   public void onStopDiscoveryFailed(String s,int e){Diagnostics.event(discoveryId,"bridge.discovery.stop.error","code="+e);}
   public void onServiceFound(NsdServiceInfo s){
    if(resolving||closing)return;resolving=true;
    nsd.resolveService(s,new NsdManager.ResolveListener(){
     public void onResolveFailed(NsdServiceInfo i,int e){Diagnostics.event(discoveryId,"bridge.discovery.resolve.error","code="+e);resolving=false;}
     public void onServiceResolved(NsdServiceInfo i){
      resolving=false;if(i.getHost()==null)return;String candidate=i.getHost().getHostAddress();
      worker.execute(()->{try{checkBridge(candidate,"");ui(()->{if(hostInput!=null){hostInput.setText(candidate);status.setText(SetupStrings.get(LightsActivity.this,"found"));}stopDiscovery();});}catch(Exception ignored){Diagnostics.error(discoveryId,"bridge.discovery.candidate.error",ignored);}finally{diagnosticId.remove();}});
     }
    });
   }
  };
  try{nsd.discoverServices("_hue._tcp.",NsdManager.PROTOCOL_DNS_SD,discovery);}catch(RuntimeException e){Diagnostics.error(discoveryId,"bridge.discovery.error",e);stopDiscovery();status.setText(Strings.get(this,"search_unavailable"));}
  panel.postDelayed(()->{if(discovery!=null){stopDiscovery();if(!busy)status.setText(Strings.get(this,"search_done"));}},12000);
 }
 private void stopDiscovery(){if(discovery!=null){try{nsd.stopServiceDiscovery(discovery);}catch(RuntimeException ignored){Diagnostics.error(discoveryId,"bridge.discovery.stop.error",ignored);}discovery=null;}}
 @Override public void onBackPressed(){if(busy){status.setText(Strings.get(this,"wait"));return;}super.onBackPressed();}
 @Override protected void onDestroy(){closing=true;stopDiscovery();worker.shutdown();super.onDestroy();}
}

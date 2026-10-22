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
 private final ArrayList<Button> controls=new ArrayList<>();
 private Map<Integer,ContentValues> initial;
 private SharedPreferences prefs;
 private LinearLayout panel;
 private EditText hostInput;
 private TextView status;
 private String host,token,bridgeMac;
 private boolean busy,closing,resolving;
 private NsdManager nsd;
 private NsdManager.DiscoveryListener discovery;
 private interface Task {void run() throws Exception;}
 private static class HueError extends IOException {final int type;HueError(int type,String text){super(text);this.type=type;}}
 @Override public void onCreate(Bundle state) {
  super.onCreate(state); prefs=getSharedPreferences("hue_bridge",MODE_PRIVATE);
  connectionScreen();
  if(!prefs.getString("token","").isEmpty())load(false);
 }
 private void base(String title) {
  choices.clear(); controls.clear();
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
  base("Přidání HUE světel do DB");
  text("Světla se načtou z můstku napárovaného s TV. Při prvním použití stiskněte fyzické tlačítko můstku a zvolte Spárovat.",18);
  hostInput=new EditText(this);hostInput.setSingleLine(true);hostInput.setTextColor(Color.WHITE);
  hostInput.setTextSize(20);hostInput.setHint("IP adresa Hue můstku");hostInput.setHintTextColor(Color.LTGRAY);
  hostInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
  hostInput.setText(prefs.getString("host",""));panel.addView(hostInput,new LinearLayout.LayoutParams(-1,110));
  button("Vyhledat můstek v síti",this::discover);
  button("Načíst kompatibilní světla",() -> load(false));
  button("Spárovat – tlačítko můstku je stisknuté",() -> load(true));
  button("Zpět",this::finish);
  status.setText("TV musí být s můstkem síťově propojená. Adresu lze zadat i ručně.");
 }
 private static String normalize(String s) {return s==null?"":s.replaceAll("[^a-fA-F0-9]", "").toLowerCase(Locale.ROOT);}
 private String tvMac() throws Exception {
  try(Cursor c=getContentResolver().query(BRIDGE,new String[]{"MacAddr"},null,null,null)) {
   if(c!=null && c.moveToFirst()) {String mac=normalize(c.getString(0));if(mac.length()==12)return mac;}
  }
  throw new IOException("Nejprve napárujte můstek v původním průvodci TV.");
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
  c.setConnectTimeout(6000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);
  try {
   if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.getBytes("UTF-8"));}}
   if(c.getResponseCode()!=200)throw new IOException("Můstek odpověděl HTTP "+c.getResponseCode()+".");
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>2097152)throw new IOException("Příliš velká odpověď můstku.");}}
   Object data=new JSONTokener(out.toString("UTF-8")).nextValue();
   if(data instanceof JSONArray){JSONObject first=((JSONArray)data).optJSONObject(0);JSONObject error=first==null?null:first.optJSONObject("error");if(error!=null){int type=error.optInt("type");if(type==101)throw new HueError(type,"Stiskněte tlačítko na Hue můstku a zkuste Spárovat znovu.");if(type==1)throw new HueError(type,"Přístup vypršel. Stiskněte tlačítko můstku a použijte Spárovat.");throw new HueError(type,"Můstek odmítl požadavek (chyba "+type+").");}}
   return data;
  } finally {c.disconnect();}
 }
 private void checkBridge(String candidate,String expected) throws Exception {
  Object obj=request(candidate,"/api/config",null);
  if(!(obj instanceof JSONObject))throw new IOException("Adresa neodpovídá Hue můstku.");
  String actual=normalize(((JSONObject)obj).optString("mac"));
  String bridgeid=normalize(((JSONObject)obj).optString("bridgeid"));
  String expectedId=expected.substring(0,6)+"fffe"+expected.substring(6);
  if(!expected.equals(actual)&&!expectedId.equals(bridgeid))throw new IOException("Tento můstek není napárovaný s TV. Vyberte správnou IP adresu.");
 }
 private void task(String message,Task work) {
  if(busy)return;busy=true;for(Button b:controls)b.setEnabled(false);if(hostInput!=null)hostInput.setEnabled(false);status.setText(message);
  worker.execute(() -> {
   try {work.run();}catch(Exception e){ui(() -> status.setText(e.getMessage()==null?"Operace se nezdařila.":e.getMessage()));}
   finally {ui(() -> {busy=false;for(Button b:controls)b.setEnabled(true);if(hostInput!=null)hostInput.setEnabled(true);});}
  });
 }
 private void ui(Runnable r){runOnUiThread(() -> {if(!closing&&!isFinishing())r.run();});}
 private void load(boolean pair) {
  String candidate=hostInput.getText().toString().trim();
  task("Připojuji se k můstku…",() -> {
   String mac=tvMac();checkBridge(candidate,mac);
   prefs.edit().putString("host",candidate).apply();
   String key=prefs.getString("mac","").equals(mac)?prefs.getString("token",""):"";
   if(pair){
    JSONArray result=null;
    long deadline=android.os.SystemClock.elapsedRealtime()+45000;
    while(result==null){
     if(closing)throw new IOException("Párování zrušeno.");
     try{result=(JSONArray)request(candidate,"/api",new JSONObject().put("devicetype","ambilight_hue#tv_picker").toString());}
     catch(HueError error){if(error.type!=101||android.os.SystemClock.elapsedRealtime()>=deadline)throw error;ui(()->status.setText("Nyní stiskněte fyzické tlačítko na Hue můstku. Čekám až 45 sekund…"));Thread.sleep(1200);}
    }
    key=result.getJSONObject(0).getJSONObject("success").getString("username");
   }
   if(key.isEmpty())throw new IOException("Stiskněte fyzické tlačítko můstku a zvolte Spárovat.");
   Object response=request(candidate,"/api/"+URLEncoder.encode(key,"UTF-8")+"/lights",null);
   if(!(response instanceof JSONObject))throw new IOException("Můstek nevrátil seznam světel.");
   JSONObject lamps=(JSONObject)response;
   ArrayList<Integer> ids=new ArrayList<>();for(Iterator<String> it=lamps.keys();it.hasNext();){String id=it.next();JSONObject lamp=lamps.getJSONObject(id);String type=lamp.optString("type");if("Color light".equals(type)||"Extended color light".equals(type)){int n=Integer.parseInt(id);if(n>0)ids.add(n);}}
   Collections.sort(ids);Map<Integer,ContentValues> selected=readSelection();
   prefs.edit().putString("host",candidate).putString("mac",mac).putString("token",key).apply();
   host=candidate;token=key;bridgeMac=mac;
   ui(() -> showLights(lamps,ids,selected));
  });
 }
 private void showLights(JSONObject lamps,List<Integer> ids,Map<Integer,ContentValues> selected) {
  stopDiscovery();hostInput=null;initial=selected;
  base("Výběr Hue světel pro Ambilight");
  text("Zaškrtněte požadovaná světla a zvolte Uložit výběr. Nejvýše 10 světel. Již uloženým světlům zůstane poloha i jas.",18);
  for(int id:ids) {
   JSONObject lamp=lamps.optJSONObject(String.valueOf(id));
   CheckBox box=new CheckBox(this);box.setText(lamp.optString("name","Světlo "+id)+"  (ID "+id+")");
   box.setTextColor(Color.WHITE);box.setTextSize(21);box.setPadding(20,8,20,8);box.setMinHeight(128);box.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(90,220,240)));
   box.setChecked(selected.containsKey(id));box.setFocusable(true);panel.addView(box,new LinearLayout.LayoutParams(-1,128));choices.put(id,box);
  }
  int hidden=0;for(int id:selected.keySet())if(!choices.containsKey(id))hidden++;
  status.setText("Kompatibilní světla: "+ids.size()+(hidden>0?". Dalších "+hidden+" uložených světel není v seznamu; zůstanou zachovaná.":""));
  if(ids.isEmpty())text("Můstek neobsahuje světla typu Color light nebo Extended color light.",18);
  Button save=button("Uložit výběr",this::save);save.setEnabled(!ids.isEmpty());
  button("Připojení k můstku",this::connectionScreen);button("Zpět",this::finish);
  if(!choices.isEmpty())choices.values().iterator().next().requestFocus();
 }
 private void save() {
  Set<Integer> desired=new LinkedHashSet<>(initial.keySet());
  for(Map.Entry<Integer,CheckBox> entry:choices.entrySet()){if(entry.getValue().isChecked())desired.add(entry.getKey());else desired.remove(entry.getKey());}
  if(desired.size()>10){status.setText("TV podporuje nejvýše 10 vybraných světel.");return;}
  if(desired.isEmpty())new AlertDialog.Builder(this).setMessage("Nezůstane vybrané žádné světlo pro Ambilight. Uložit prázdný výběr?").setPositiveButton("Uložit",(d,w)->writeSelection(desired)).setNegativeButton("Zrušit",null).show();
  else writeSelection(desired);
 }
 private void writeSelection(Set<Integer> desired) {
  for(CheckBox box:choices.values())box.setEnabled(false);
  task("Ukládám výběr…",() -> {
   try {
    if(!bridgeMac.equals(tvMac()))throw new IOException("Párování TV se změnilo. Načtěte světla znovu.");
    checkBridge(host,bridgeMac);
    Map<Integer,ContentValues> before=readSelection();
    if(!before.equals(initial))throw new IOException("Konfigurace TV se mezitím změnila. Načtěte seznam znovu.");
    Set<Integer> added=new LinkedHashSet<>();Set<Integer> removed=new LinkedHashSet<>();
    try {
     for(int id:desired)if(!before.containsKey(id)) {ContentValues row=new ContentValues();row.put("LampID",id);row.put("ConfigureStatus",0);if(getContentResolver().insert(LAMPS,row)==null)throw new IOException("Světlo se nepodařilo uložit.");added.add(id);}
     for(int id:before.keySet())if(!desired.contains(id)){if(getContentResolver().delete(LAMPS,"LampID=?",new String[]{String.valueOf(id)})!=1)throw new IOException("Výběr se mezitím změnil.");removed.add(id);}
     Map<Integer,ContentValues> after=readSelection();
     if(!after.keySet().equals(desired))throw new IOException("Kontrola uloženého výběru nesouhlasí.");
     for(int id:desired)if(before.containsKey(id)&&!before.get(id).equals(after.get(id)))throw new IOException("Nastavení existujícího světla se změnilo.");
     initial=after;
     ui(() -> status.setText("Uloženo a ověřeno: "+desired.size()+" světel. "+(added.isEmpty()?"Poloha a jas zůstaly zachované.":"Novým světlům nastavte polohu v původním průvodci přes Konfigurovat žárovky.")));
    } catch(Exception e) {
     try {for(int id:added)getContentResolver().delete(LAMPS,"LampID=?",new String[]{String.valueOf(id)});for(int id:removed)getContentResolver().insert(LAMPS,before.get(id));if(!readSelection().equals(before))throw new IOException();}
     catch(Exception rollback){throw new IOException("Uložení selhalo a návrat původního výběru nelze potvrdit. Otevřete původní průvodce TV.");}
     throw e;
    }
   } finally {ui(() -> {for(CheckBox box:choices.values())box.setEnabled(true);});}
  });
 }
 private void discover() {
  if(discovery!=null){status.setText("Vyhledávání už běží. Případně zadejte IP ručně.");return;}
  status.setText("Vyhledávám Hue můstek v místní síti…");nsd=(NsdManager)getSystemService(NSD_SERVICE);
  discovery=new NsdManager.DiscoveryListener(){
   public void onDiscoveryStarted(String s){}
   public void onDiscoveryStopped(String s){}
   public void onServiceLost(NsdServiceInfo s){}
   public void onStartDiscoveryFailed(String s,int e){ui(()->{stopDiscovery();status.setText("Vyhledání není dostupné. Zadejte IP adresu ručně.");});}
   public void onStopDiscoveryFailed(String s,int e){}
   public void onServiceFound(NsdServiceInfo s){
    if(resolving||closing)return;resolving=true;
    nsd.resolveService(s,new NsdManager.ResolveListener(){
     public void onResolveFailed(NsdServiceInfo i,int e){resolving=false;}
     public void onServiceResolved(NsdServiceInfo i){
      resolving=false;if(i.getHost()==null)return;String candidate=i.getHost().getHostAddress();
      worker.execute(()->{try{checkBridge(candidate,tvMac());ui(()->{if(hostInput!=null){hostInput.setText(candidate);status.setText("Nalezen napárovaný můstek. Zvolte Načíst světla nebo Spárovat.");}stopDiscovery();});}catch(Exception ignored){}});
     }
    });
   }
  };
  try{nsd.discoverServices("_hue._tcp.",NsdManager.PROTOCOL_DNS_SD,discovery);}catch(RuntimeException e){stopDiscovery();status.setText("Zadejte IP můstku ručně.");}
  panel.postDelayed(()->{if(discovery!=null){stopDiscovery();if(!busy)status.setText("Vyhledávání skončilo. Pokud adresa chybí, zadejte ji ručně.");}},12000);
 }
 private void stopDiscovery(){if(discovery!=null){try{nsd.stopServiceDiscovery(discovery);}catch(RuntimeException ignored){}discovery=null;}}
 @Override public void onBackPressed(){if(busy){status.setText("Počkejte na dokončení operace.");return;}super.onBackPressed();}
 @Override protected void onDestroy(){closing=true;stopDiscovery();worker.shutdown();super.onDestroy();}
}

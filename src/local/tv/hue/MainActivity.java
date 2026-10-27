package local.tv.hue;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
 private TextView status;
 private boolean busy;
 private final java.util.ArrayList<Button> buttons=new java.util.ArrayList<>();
 private final java.util.concurrent.ExecutorService apiWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  Diagnostics.init(this);Diagnostics.event("lifecycle","create","version=1.8 sdk="+android.os.Build.VERSION.SDK_INT);
  LinearLayout panel=new LinearLayout(this);
  panel.setOrientation(LinearLayout.VERTICAL);
  panel.setGravity(android.view.Gravity.CENTER);
  panel.setPadding(60,24,60,24);
  panel.setBackgroundColor(Color.rgb(25,38,65));
  TextView title=new TextView(this);
  title.setText("Ambilight + Hue"); title.setTextSize(30); title.setTextColor(Color.WHITE);
  panel.addView(title);
  TextView description=new TextView(this);
  description.setText("Vypnutí synchronizace obnoví předchozí osvětlení.");
  description.setTextColor(Color.LTGRAY); description.setTextSize(18);
  description.setPadding(0,12,0,24); panel.addView(description);
  Button on=button(panel,"Zapnout synchronizaci");
  on.setOnClickListener(v -> sync(true));
  Button off=button(panel,"Vypnout synchronizaci");
  off.setOnClickListener(v -> sync(false));
  Button configure=button(panel,"Konfigurovat žárovky");
  configure.setOnClickListener(v -> {
   try {
    Diagnostics.event(Diagnostics.id(),"wizard.open","requested=true");
    startActivity(new Intent().setComponent(new ComponentName("org.droidtv.alhuewizard","org.droidtv.alhuewizard.ALHueWizardActivity")));
   } catch(RuntimeException e) {Diagnostics.error("wizard","wizard.open.error",e);status.setText("Průvodce není dostupný.");}
  });
  status=new TextView(this); status.setTextColor(Color.WHITE); status.setTextSize(16);
  status.setPadding(0,20,0,0); panel.addView(status);
  Button lights=button(panel,"Přidání HUE světel do DB");
  lights.setOnClickListener(v -> startActivity(new Intent(this,LightsActivity.class)));
  android.widget.ScrollView scroll=new android.widget.ScrollView(this);
  scroll.setFillViewport(true); scroll.addView(panel);
  setContentView(scroll); on.requestFocus();
  command(getIntent());
 }
 private Button button(LinearLayout panel,String text) {
  Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(20);
  b.setSingleLine(true);
  b.setGravity(android.view.Gravity.CENTER);
  b.setPadding(24,0,24,0);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(1200,128);lp.setMargins(0,8,0,8);
  panel.addView(b,lp);buttons.add(b);return b;
 }
 private static final java.util.concurrent.ScheduledExecutorService observer=java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
 private static final java.util.concurrent.atomic.AtomicLong generation=new java.util.concurrent.atomic.AtomicLong();
 private void observe(String id,boolean desired,long token) {
  final android.content.Context app=getApplicationContext();
  for(int seconds:new int[]{2,10,30,60})observer.schedule(() -> {
   if(generation.get()!=token){Diagnostics.event(id,"observation.skipped","newer_request=true");return;}
   Diagnostics.event(id,"observation.begin","after_seconds="+seconds);
   Diagnostics.network(app,id);
   try {String saved=new TvApiClient(app,id).getPower();Diagnostics.event(id,"observation.result","after_seconds="+seconds+" saved="+saved+" matches_request="+((desired?"On":"Off").equals(saved)));}
   catch(Exception e){Diagnostics.error(id,"observation.error",e);}
  },seconds,java.util.concurrent.TimeUnit.SECONDS);
 }
 private void sync(boolean on) {
  if(busy){Diagnostics.event("ui","request.ignored","busy=true");return;}
  final String id=Diagnostics.id();final long token=generation.incrementAndGet();
  final long began=android.os.SystemClock.elapsedRealtime();
  Diagnostics.event(id,"request.begin","desired="+(on?"On":"Off"));Diagnostics.network(this,id);
  busy=true;for(Button b:buttons)b.setEnabled(false);
  status.setText(on?"Zapínám synchronizaci…":"Vypínám synchronizaci a obnovuji osvětlení…");
  apiWorker.execute(() -> {
   String message;
   try {
    new TvApiClient(this,id).setPower(on,() -> notifyNative(on,id));
    observe(id,on,token);
    message=on?"Požadavek na zapnutí přijat. Přepínač Hue je zapnutý; reakce světel zatím není ověřena.":"Nativní přepínač Hue je vypnutý. Předchozí osvětlení se obnovuje.";
   } catch(Exception e) {Diagnostics.error(id,"request.error",e);message="Požadavek selhal ("+e.getClass().getSimpleName()+"). Diagnostika: "+id;observe(id,on,token);}
   Diagnostics.event(id,"request.end","duration_ms="+(android.os.SystemClock.elapsedRealtime()-began));
   final String result=message;
   runOnUiThread(() -> {if(!isFinishing()&&!isDestroyed()){status.setText(result);busy=false;for(Button b:buttons)b.setEnabled(true);}});
  });
 }
 private void notifyNative(boolean on,String id){
  Diagnostics.event(id,"native_service.notify.begin","desired="+(on?"On":"Off"));
  Intent service=new Intent().setComponent(new ComponentName("org.droidtv.alhuewizard","org.droidtv.ambihue.HueService"));
  service.setAction("org.droidtv.ambihue.HueService."+(on?"ALHUEON":"ALHUEOFF"));
  ComponentName component=android.os.Build.VERSION.SDK_INT>=26?startForegroundService(service):startService(service);
  if(component==null)throw new IllegalStateException("Native Hue service unavailable");
  Diagnostics.event(id,"native_service.notify.accepted","actual_light_sync=unverified");
 }
 private void command(Intent i) {
  String value=i.getStringExtra("sync");
  if("on".equals(value)) sync(true);
  if("off".equals(value)) sync(false);
 }
 @Override protected void onNewIntent(Intent i) {super.onNewIntent(i);setIntent(i);command(i);}
 @Override protected void onStart(){super.onStart();Diagnostics.event("lifecycle","start","");}
 @Override protected void onStop(){Diagnostics.event("lifecycle","stop","");super.onStop();}
 @Override protected void onDestroy(){Diagnostics.event("lifecycle","destroy","");apiWorker.shutdown();super.onDestroy();}
}

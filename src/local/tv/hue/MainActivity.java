package local.tv.hue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
 private TextView status;
 private TextView stateSummary;private Button onButton,offButton;private long statusGeneration;
 private boolean statusReading;
 private final android.os.Handler refreshHandler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable refreshTick=new Runnable(){public void run(){refreshStatus();refreshHandler.postDelayed(this,5000);}};
 private boolean busy;
 private final java.util.ArrayList<Button> buttons=new java.util.ArrayList<>();
 private final java.util.concurrent.ExecutorService apiWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
 private final java.util.concurrent.ExecutorService statusWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  Diagnostics.init(this);Diagnostics.event("lifecycle","create","version=1.13 sdk="+android.os.Build.VERSION.SDK_INT);
  LinearLayout panel=new LinearLayout(this);
  panel.setOrientation(LinearLayout.VERTICAL);
  panel.setGravity(android.view.Gravity.CENTER);
  panel.setPadding(60,24,60,24);
  panel.setBackgroundColor(Color.rgb(25,38,65));
  TextView title=new TextView(this);
  title.setText("Ambilight + Hue"); title.setTextSize(30); title.setTextColor(Color.WHITE);
  panel.addView(title);
  stateSummary=new TextView(this);stateSummary.setTextColor(Color.WHITE);stateSummary.setTextSize(17);panel.addView(stateSummary);
  TextView description=new TextView(this);
  description.setText(Strings.get(this,"restore"));
  description.setTextColor(Color.LTGRAY); description.setTextSize(18);
  description.setPadding(0,12,0,24); panel.addView(description);
  Button on=button(panel,Strings.get(this,"on"));
  onButton=on;
  on.setOnClickListener(v -> sync(true));
  Button off=button(panel,Strings.get(this,"off"));
  offButton=off;
  off.setOnClickListener(v -> sync(false));
  Button setup=button(panel,SetupStrings.get(this,"setup"));
  setup.setOnClickListener(v -> startActivity(new Intent(this,LightsActivity.class)));
  button(panel,FeatureStrings.get(this,"advanced")).setOnClickListener(v->advanced());
  status=new TextView(this); status.setTextColor(Color.WHITE); status.setTextSize(16);
  status.setPadding(0,20,0,0); panel.addView(status);
  android.widget.ScrollView scroll=new android.widget.ScrollView(this);
  scroll.setFillViewport(true); scroll.addView(panel);
  setContentView(scroll); on.requestFocus();
  command(getIntent());
 }
 private void advanced(){new AlertDialog.Builder(this).setTitle(FeatureStrings.get(this,"advanced")).setItems(new String[]{SetupStrings.get(this,"native_wizard"),FeatureStrings.get(this,"reset"),FeatureStrings.get(this,"identity_title")},(d,item)->{
  if(item==0){try{startActivity(new Intent().setComponent(new ComponentName("org.droidtv.alhuewizard","org.droidtv.alhuewizard.ALHueWizardActivity")));}catch(RuntimeException e){status.setText(Strings.get(this,"wizard_missing"));}}
  else if(item==1)new AlertDialog.Builder(this).setTitle(FeatureStrings.get(this,"reset")).setMessage(FeatureStrings.get(this,"reset_help")).setNegativeButton(Strings.get(this,"cancel"),null).setPositiveButton(FeatureStrings.get(this,"reset_confirm"),(confirm,w)->reset()).show();
  else new AlertDialog.Builder(this).setMessage(FeatureStrings.get(this,"identity")).setPositiveButton("OK",null).show();
 }).setNegativeButton(Strings.get(this,"back"),null).show();}
 private void reset(){if(busy)return;generation.incrementAndGet();statusGeneration++;busy=true;for(Button b:buttons)b.setEnabled(false);apiWorker.execute(()->{String message;try{SettingsReset.run(this);message=FeatureStrings.get(this,"reset_done");}catch(Exception e){String id=Diagnostics.id();Diagnostics.error(id,"settings.reset.failed",e);message=Strings.get(this,"request_failed",e.getClass().getSimpleName(),id);}String result=message;runOnUiThread(()->{if(isDestroyed())return;busy=false;for(Button b:buttons)b.setEnabled(true);status.setText(result);refreshStatus();});});}
 private void refreshStatus(){if(busy||statusReading||isDestroyed())return;statusReading=true;long token=++statusGeneration;statusWorker.execute(()->{String power=FeatureStrings.get(this,"unknown");if(UiStatus.paired(this))try{power=new TvApiClient(this).getPower();}catch(Exception e){Diagnostics.error("status","power.read.failed",e);}String value=power;String summary=FeatureStrings.get(this,"bridge")+": "+UiStatus.configuredBridge(this)+"\n"+AppPairing.text(this,0)+": "+(UiStatus.paired(this)?"✓ "+FeatureStrings.get(this,"stored"):"✕ "+FeatureStrings.get(this,"not_ready"))+"\n"+FeatureStrings.get(this,"switch")+": "+("On".equals(value)?FeatureStrings.get(this,"state_on"):"Off".equals(value)?FeatureStrings.get(this,"state_off"):value)+"\n"+UiStatus.nativeActive(this);runOnUiThread(()->{statusReading=false;if(isDestroyed()||busy||token!=statusGeneration)return;stateSummary.setText(UiColors.summary(this,summary));onButton.setText(UiColors.marks(("On".equals(value)?"✓ ":"")+Strings.get(this,"on")));offButton.setText(UiColors.marks(("Off".equals(value)?"✓ ":"")+Strings.get(this,"off")));onButton.setTextColor("On".equals(value)?UiColors.GREEN:Color.WHITE);offButton.setTextColor("Off".equals(value)?UiColors.RED:Color.WHITE);});});}
 @Override protected void onResume(){super.onResume();refreshStatus();refreshHandler.removeCallbacks(refreshTick);refreshHandler.postDelayed(refreshTick,5000);}
 @Override protected void onPause(){refreshHandler.removeCallbacks(refreshTick);super.onPause();}
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
  if(on&&getSharedPreferences("native_setup_recovery",0).contains("snapshot")){status.setText(SetupStrings.get(this,"recovery"));startActivity(new Intent(this,LightsActivity.class));return;}
  if(busy){Diagnostics.event("ui","request.ignored","busy=true");return;}
  final String id=Diagnostics.id();final long token=generation.incrementAndGet();
  final long began=android.os.SystemClock.elapsedRealtime();
  Diagnostics.event(id,"request.begin","desired="+(on?"On":"Off"));Diagnostics.network(this,id);
  busy=true;for(Button b:buttons)b.setEnabled(false);
  statusGeneration++;
  status.setText(Strings.get(this,on?"starting":"stopping"));
  apiWorker.execute(() -> {
   String message;
   try {
    new TvApiClient(this,id).setPower(on,() -> notifyNative(on,id));
    observe(id,on,token);
    message=Strings.get(this,on?"on_ok":"off_ok");
   } catch(Exception e) {Diagnostics.error(id,"request.error",e);message=Strings.get(this,"request_failed",e.getClass().getSimpleName(),id);observe(id,on,token);}
   Diagnostics.event(id,"request.end","duration_ms="+(android.os.SystemClock.elapsedRealtime()-began));
   final String result=message;
   runOnUiThread(() -> {if(!isFinishing()&&!isDestroyed()){status.setText(result);busy=false;for(Button b:buttons)b.setEnabled(true);refreshStatus();}});
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
 @Override protected void onDestroy(){Diagnostics.event("lifecycle","destroy","");refreshHandler.removeCallbacks(refreshTick);statusWorker.shutdown();apiWorker.shutdown();super.onDestroy();}
}

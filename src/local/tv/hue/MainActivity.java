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
    startActivity(new Intent().setComponent(new ComponentName("org.droidtv.alhuewizard","org.droidtv.alhuewizard.ALHueWizardActivity")));
   } catch(RuntimeException e) {status.setText("Průvodce není dostupný.");}
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
 private void sync(boolean on) {
  if(busy)return;
  busy=true;for(Button b:buttons)b.setEnabled(false);
  status.setText(on?"Zapínám synchronizaci…":"Vypínám synchronizaci a obnovuji osvětlení…");
  apiWorker.execute(() -> {
   String message;
   try {
    new TvApiClient(this).setPower(on);
    Intent service=new Intent().setComponent(new ComponentName("org.droidtv.alhuewizard","org.droidtv.ambihue.HueService"));
    service.setAction("org.droidtv.ambihue.HueService."+(on?"ALHUEON":"ALHUEOFF"));
    if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(service);else startService(service);
    message=on?"Nativní přepínač Hue je zapnutý.":"Nativní přepínač Hue je vypnutý. Předchozí osvětlení se obnovuje.";
   } catch(Exception e) {message=e.getMessage()==null?"TV API neodpovídá. Zkuste to znovu.":e.getMessage();}
   final String result=message;
   runOnUiThread(() -> {if(!isFinishing()&&!isDestroyed()){status.setText(result);busy=false;for(Button b:buttons)b.setEnabled(true);}});
  });
 }
 private void command(Intent i) {
  String value=i.getStringExtra("sync");
  if("on".equals(value)) sync(true);
  if("off".equals(value)) sync(false);
 }
 @Override protected void onNewIntent(Intent i) {super.onNewIntent(i);setIntent(i);command(i);}
 @Override protected void onDestroy(){apiWorker.shutdown();super.onDestroy();}
}

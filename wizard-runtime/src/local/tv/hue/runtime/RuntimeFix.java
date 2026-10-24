package local.tv.hue.runtime;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;

/** Restores remote confirmation in the installed Philips Hue wizard only. */
public final class RuntimeFix {
 private static final String TAG="HueWizardFix";
 private static final String ACTIVITY="org.droidtv.alhuewizard.ALHueWizardActivity";
 private static final String LIST_SCREEN="org.droidtv.ui.tvwidget2k15.pagewizarddata.ListLoaderDataScreen";
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private static final Map<Activity,FixCallback> PATCHED=new WeakHashMap<>();
 private static Application application;
 private static boolean running;
 private static int confirmations,errors;
 private static String lastError="none";
 private static String lastState="starting";
 private static final Runnable TICK=new Runnable(){public void run(){
  if(!running)return;
  try {
   if(new File(application.getFilesDir(),"hue-runtime.disabled").exists()) {stopOnMain();return;}
   Object thread=Class.forName("android.app.ActivityThread").getMethod("currentActivityThread").invoke(null);
   Map<?,?> records=(Map<?,?>)get(thread,"mActivities");
   Iterator<Map.Entry<Activity,FixCallback>> old=PATCHED.entrySet().iterator();
   while(old.hasNext()) {
    Map.Entry<Activity,FixCallback> e=old.next();Activity a=e.getKey();
    if(a.isDestroyed() || a.isFinishing()) {
     Window w=a.getWindow();if(w!=null && w.getCallback()==e.getValue().proxy)w.setCallback(e.getValue().original);
     old.remove();
    }
   }
   for(Object record:records.values()) {
    Activity a=(Activity)get(record,"activity");
    if(a==null || !ACTIVITY.equals(a.getClass().getName()) || a.isDestroyed() || a.isFinishing())continue;
    Window w=a.getWindow();
    if(w==null || w.getCallback()==null)continue;
    FixCallback existing=PATCHED.get(a);
    if(existing==null || w.getCallback()!=existing.proxy) {
     FixCallback callback=new FixCallback(a,w.getCallback());
     callback.proxy=(Window.Callback)Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class<?>[]{Window.Callback.class},callback);
     w.setCallback(callback.proxy);PATCHED.put(a,callback);
     Log.i(TAG,"Installed confirmation handler for native Hue wizard");
    }
    restoreAuthorization(a);
    String state=describe(a);
    if(!state.equals(lastState)){lastState=state;Log.i(TAG,"State "+state);}
   }
  }catch(Throwable e){report(e);}
  if(running)MAIN.postDelayed(this,500);
 }};
 public static synchronized String install(Application app) {
  if(!"org.droidtv.alhuewizard".equals(app.getPackageName()))return "refused: wrong package";
  if(running)return "already installed";
  application=app;running=true;MAIN.post(TICK);return "scheduled";
 }
 public static String status(){return "running="+running+" confirmations="+confirmations+" errors="+errors+" lastError="+lastError+" state="+lastState;}
 private static void stopOnMain(){
  running=false;MAIN.removeCallbacks(TICK);
  for(Map.Entry<Activity,FixCallback> e:PATCHED.entrySet()) {
   Window w=e.getKey().getWindow();if(w!=null && w.getCallback()==e.getValue().proxy)w.setCallback(e.getValue().original);
  }
  PATCHED.clear();Log.i(TAG,"Restored original Hue window callbacks");
 }
 private static void report(Throwable e){
  Throwable cause=e instanceof InvocationTargetException?((InvocationTargetException)e).getCause():e;
  errors++;lastError=cause.getClass().getSimpleName();
  if(errors<=5)Log.w(TAG,"Runtime handler: "+lastError);
 }
 private static Object get(Object o,String field)throws Exception {
  for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(field);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException ignored){}
  throw new NoSuchFieldException(field);
 }
 private static Method method(Class<?> c,String name,Class<?>...types)throws Exception {
  for(Class<?> t=c;t!=null;t=t.getSuperclass())try{Method m=t.getDeclaredMethod(name,types);m.setAccessible(true);return m;}catch(NoSuchMethodException ignored){}
  throw new NoSuchMethodException(name);
 }
 private static Object invoke(Object o,String name,Class<?>[]types,Object...args)throws Exception{return method(o.getClass(),name,types).invoke(o,args);}
 private static Object screen(Activity a)throws Exception {
  Object manager=get(a,"mManger");
  return invoke(manager,"getmCurrentScreen",new Class<?>[0]);
 }
 private static void restoreAuthorization(Activity a) {
  try {
   Class<?> control=Class.forName("org.droidtv.ambihue.bridgecommunication.HueControl",true,a.getClassLoader());
   Field username=control.getDeclaredField("mUsername");username.setAccessible(true);
   String current=(String)username.get(null);
   Object wizard=get(a,"mHueWizard");
   String wizardUsername=(String)get(wizard,"mUsername");
   if(current!=null && !current.isEmpty() && wizardUsername!=null && !wizardUsername.isEmpty())return;
   // Only restore credentials belonging to the currently persisted bridge.
   Object selected=get(wizard,"mBridgeInfo");
   if(selected!=null) {
    Object provider=invoke(a,"getContentProvider",new Class<?>[0]);
    String savedMac=(String)invoke(provider,"getMac",new Class<?>[0]);
    String selectedMac=(String)get(selected,"mMacAddr");
    if(savedMac==null || selectedMac==null || !savedMac.replace(":", "").equalsIgnoreCase(selectedMac.replace(":", "")))return;
   }
   Object link=get(wizard,"mLinkService");if(link==null)return;
   String stored=(String)invoke(link,"getUserName",new Class<?>[0]);
   if(stored==null || stored.isEmpty())return;
   if(current==null || current.isEmpty())username.set(null,stored);
   if(wizardUsername==null || wizardUsername.isEmpty()) {
    Field member=wizard.getClass().getDeclaredField("mUsername");member.setAccessible(true);member.set(wizard,stored);
   }
   Log.i(TAG,"Initialized existing native bridge pairing context");
  }catch(Throwable ignored){} // New or changed bridge pairing remains under native user control.
 }
 private static Candidate candidate(Activity a)throws Exception {
  Object page=screen(a);if(page==null)return null;
  Class<?> base=Class.forName(LIST_SCREEN,false,a.getClassLoader());
  if(!base.isInstance(page))return null;
  ListView list=(ListView)invoke(page,"getmScrollAdapterView",new Class<?>[0]);
  if(list==null || !list.isShown() || !list.isEnabled() || list.getAdapter()==null)return null;
  int position=list.getSelectedItemPosition();
  View focus=a.getCurrentFocus();
  if(position<0 && focus!=null)try{int focused=list.getPositionForView(focus);if(focused>=0)position=focused;}catch(Exception ignored){}
  ListAdapter adapter=list.getAdapter();
  if(position<0 || position>=adapter.getCount() || !adapter.isEnabled(position))return null;
  View row=list.getChildAt(position-list.getFirstVisiblePosition());
  if(row==null || !row.isEnabled())return null;
  return new Candidate(page,list,row,position,adapter.getItemId(position));
 }
 private static String describe(Activity a) {
  try {
   Object page=screen(a);Candidate c=candidate(a);View v=a.getCurrentFocus();
   return "screen="+(page==null?"none":page.getClass().getSimpleName())+" row="+(c==null?-1:c.position)+" count="+(c==null?-1:c.list.getCount())+" focus="+(v==null?"none":v.getClass().getSimpleName());
  }catch(Throwable e){return "unavailable:"+e.getClass().getSimpleName();}
 }
 private static boolean confirmationKey(int code){return code==KeyEvent.KEYCODE_DPAD_CENTER || code==KeyEvent.KEYCODE_ENTER || code==KeyEvent.KEYCODE_NUMPAD_ENTER || code==KeyEvent.KEYCODE_BUTTON_A;}
 private static final class Candidate {
  final Object page;final ListView list;final View row;final int position;final long id;
  Candidate(Object p,ListView l,View r,int n,long i){page=p;list=l;row=r;position=n;id=i;}
  void confirm()throws Exception {invoke(page,"itemClicked",new Class<?>[]{AdapterView.class,View.class,int.class,long.class},list,row,position,id);}
 }
 private static final class FixCallback implements InvocationHandler {
  final Activity activity;final Window.Callback original;Window.Callback proxy;
  Candidate pressed;int pressedKey=-1;long pressedTime;
  FixCallback(Activity a,Window.Callback o){activity=a;original=o;}
  public Object invoke(Object self,Method m,Object[]args)throws Throwable {
   if(running && m.getName().equals("dispatchKeyEvent") && args!=null && args.length==1 && args[0] instanceof KeyEvent) {
    KeyEvent event=(KeyEvent)args[0];int code=event.getKeyCode();
    if(confirmationKey(code))try {
     Candidate now=candidate(activity);
     if(event.getAction()==KeyEvent.ACTION_DOWN && now!=null) {
      if(event.getRepeatCount()==0){pressed=now;pressedKey=code;pressedTime=event.getDownTime();}
      return true;
     }
     if(event.getAction()==KeyEvent.ACTION_UP && pressed!=null && pressedKey==code) {
      Candidate down=pressed;pressed=null;pressedKey=-1;
      if(!event.isCanceled() && event.getDownTime()==pressedTime && now!=null && now.page==down.page && now.position==down.position) {
       down.confirm();confirmations++;Log.i(TAG,"Confirmed "+down.page.getClass().getSimpleName()+" row="+down.position);
      }
      return true;
     }
    }catch(Throwable e){pressed=null;pressedKey=-1;report(e);return true;}
   }
   try{return m.invoke(original,args);}catch(InvocationTargetException e){throw e.getCause();}
  }
 }
}

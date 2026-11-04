package local.tv.hue;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.concurrent.CompletableFuture;

/** Reads only our named Philips PIN prompt, only while the user requests pairing. */
public final class PairingAccessibility extends AccessibilityService {
 private static volatile PairingAccessibility instance;
 private static CompletableFuture<String> pending;
 private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 static boolean ready(){return instance!=null;}
 static synchronized CompletableFuture<String> arm(){if(pending!=null)pending.cancel(false);pending=new CompletableFuture<>();return pending;}
 static synchronized void disarm(CompletableFuture<String> request){if(pending==request){pending=null;request.cancel(false);}}
 @Override protected void onServiceConnected(){instance=this;}
 @Override public void onAccessibilityEvent(AccessibilityEvent event){
  CompletableFuture<String> request; synchronized(PairingAccessibility.class){request=pending;}
  if(request==null||request.isDone())return;
  Diagnostics.event("pairing","pin.window","package="+event.getPackageName()+" event="+event.getEventType());
  StringBuilder eventText=new StringBuilder();for(CharSequence value:event.getText())eventText.append(value).append('\n');capture(request,eventText);
  scan(request);handler.postDelayed(()->scan(request),200);
 }
 private void scan(CompletableFuture<String> request){
  synchronized(PairingAccessibility.class){if(pending!=request||request.isDone())return;}
  AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
  try{String pkg=String.valueOf(root.getPackageName());if(!("org.droidtv.tvsystemui".equals(pkg)||"android".equals(pkg)||"org.droidtv.xtv".equals(pkg)||"org.droidtv.tv.tptvservice".equals(pkg)))return;StringBuilder text=new StringBuilder();collect(root,text,0);
   capture(request,text);
  }finally{root.recycle();}
 }
 private void capture(CompletableFuture<String> request,CharSequence text){java.util.regex.Matcher match=java.util.regex.Pattern.compile("Ambilight Hue control\\s*:\\s*([0-9]{1,4})(?![0-9])").matcher(text);if(match.find())request.complete(match.group(1));}
 private void collect(AccessibilityNodeInfo node,StringBuilder text,int depth){if(depth>25||text.length()>8192)return;if(node.getText()!=null)text.append(node.getText()).append('\n');for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null){try{collect(child,text,depth+1);}finally{child.recycle();}}}}
 @Override public void onInterrupt(){}
 @Override public void onDestroy(){instance=null;handler.removeCallbacksAndMessages(null);synchronized(PairingAccessibility.class){if(pending!=null){pending.cancel(false);pending=null;}}super.onDestroy();}
}

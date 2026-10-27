package local.tv.hue;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.SystemClock;
import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.util.UUID;

/** Only fixed event fields, exception types and stack frames: never raw API data. */
final class Diagnostics {
 private static final String TAG="HueDiagnostics";
 private static final int LIMIT=65536;
 private static File directory;
 static synchronized void init(Context c) {directory=new File(c.getFilesDir(),"diagnostics");directory.mkdirs();}
 static String id(){return UUID.randomUUID().toString().substring(0,8);}
 static synchronized void event(String id,String stage,String detail) {
  try {
   JSONObject row=new JSONObject();
   row.put("time_ms",System.currentTimeMillis());row.put("elapsed_ms",SystemClock.elapsedRealtime());
   row.put("id",id);row.put("stage",stage);row.put("detail",detail);
   String line=row.toString();Log.i(TAG,line);
   if(directory==null)return;
   File current=new File(directory,"current.jsonl");
   if(current.length()+line.length()*4+1>LIMIT){
    File older=new File(directory,"previous-2.jsonl"),previous=new File(directory,"previous-1.jsonl");
    if(older.exists()&&!older.delete())return;
    if(previous.exists()&&!previous.renameTo(older))return;
    if(current.exists()&&!current.renameTo(previous))return;
   }
   try(FileOutputStream out=new FileOutputStream(current,true)){out.write((line+"\n").getBytes("UTF-8"));}
  }catch(Exception ignored){Log.w(TAG,"diagnostic_storage_failed");}
 }
 static void error(String id,String stage,Throwable error) {
  // Throwable messages can contain credentials, URLs or response bodies; omit them.
  java.util.IdentityHashMap<Throwable,Boolean> seen=new java.util.IdentityHashMap<>();
  for(int depth=0;error!=null&&depth<8&&!seen.containsKey(error);depth++,error=error.getCause()){
   seen.put(error,true);StringBuilder frames=new StringBuilder();
   StackTraceElement[] stack=error.getStackTrace();
   for(int i=0;i<Math.min(stack.length,24);i++)frames.append(stack[i].toString()).append("; ");
   String kind=error instanceof java.net.SocketTimeoutException?"timeout":error instanceof javax.net.ssl.SSLException?"tls":error instanceof java.net.ConnectException?"connect":error instanceof java.io.EOFException?"connection_closed":error instanceof SecurityException?"permission":"other";
   event(id,stage,"cause="+depth+" type="+error.getClass().getName()+" category="+kind+" errno="+(error instanceof android.system.ErrnoException?((android.system.ErrnoException)error).errno:0)+" frames="+frames);
  }
 }
 static void network(Context c,String id) {
  try {
   ConnectivityManager cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
   NetworkCapabilities n=cm.getNetworkCapabilities(cm.getActiveNetwork());
   event(id,"network",n==null?"active=false":"active=true ethernet="+n.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)+" wifi="+n.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)+" vpn="+n.hasTransport(NetworkCapabilities.TRANSPORT_VPN)+" validated="+n.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
  }catch(Exception e){error(id,"network.error",e);}
 }
 static synchronized String export() throws IOException {
  if(directory==null)return "";
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  for(String name:new String[]{"previous-2.jsonl","previous-1.jsonl","current.jsonl"}){
   File f=new File(directory,name);if(!f.exists())continue;
   try(FileInputStream in=new FileInputStream(f)){
    byte[] buffer=new byte[4096];int n;int remaining=LIMIT;
    while(remaining>0&&(n=in.read(buffer,0,Math.min(buffer.length,remaining)))!=-1){out.write(buffer,0,n);remaining-=n;}
   }
  }
  return out.toString("UTF-8");
 }
}

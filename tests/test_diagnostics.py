import importlib.util
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("collector",ROOT/"tools/collect_diagnostics.py")
collector=importlib.util.module_from_spec(spec);spec.loader.exec_module(collector)

class CollectorTests(unittest.TestCase):
 def test_native_export_excludes_secret_urls_bodies_and_exception_messages(self):
  secret="PRIVATE_HUE_KEY_0123456789"
  raw=("1792870000.123 123 I ambilighthue: http://bridge/api/"+secret+"/lights/1/state {password: "+secret+"}\n"
       "1792870001.123 123 I ambilighthue: java.net.SocketTimeoutException: "+secret+"\n"
       "1792870002.123 123 I ambilighthue: Current state:StateIdle Event state:EvTurnOnHue Next State:StateFindBdg\n"
       "1792870003.123 123 I ambilighthue: INSTSETTINGSALPLUSHUEONOFF: 0\n")
  result=collector.native_events(raw)
  self.assertNotIn(secret,result);self.assertNotIn("http://",result)
  self.assertIn("java.net.SocketTimeoutException",result)
  self.assertIn("StateFindBdg",result);self.assertIn("ALPLUSHUEONOFF: 0",result)

@unittest.skipUnless(shutil.which("javac") and shutil.which("java"),"JDK required")
class LoggerTests(unittest.TestCase):
 def test_exception_redaction_rotation_and_reopen(self):
  # Run the actual logger on the JVM with minimal Android/JSON adapters.
  stubs={
   "android/content/Context.java":'package android.content; public class Context {public static final String CONNECTIVITY_SERVICE="connectivity";public java.io.File getFilesDir(){return new java.io.File(System.getProperty("test.dir"));}public Object getSystemService(String s){return null;}}',
   "android/net/ConnectivityManager.java":'package android.net; public class ConnectivityManager {public Object getActiveNetwork(){return null;}public NetworkCapabilities getNetworkCapabilities(Object n){return null;}}',
   "android/net/NetworkCapabilities.java":'package android.net; public class NetworkCapabilities {public static final int TRANSPORT_ETHERNET=3,TRANSPORT_WIFI=1,TRANSPORT_VPN=4,NET_CAPABILITY_VALIDATED=16;public boolean hasTransport(int v){return false;}public boolean hasCapability(int v){return false;}}',
   "android/os/SystemClock.java":'package android.os; public class SystemClock {public static long elapsedRealtime(){return System.nanoTime()/1000000;}}',
   "android/system/ErrnoException.java":'package android.system; public class ErrnoException extends Exception {public int errno=113;}',
   "android/util/Log.java":'package android.util; public class Log {public static int i(String t,String s){return 0;}public static int w(String t,String s){return 0;}}',
   "org/json/JSONObject.java":'package org.json; public class JSONObject {private final java.util.Map<String,Object> m=new java.util.LinkedHashMap<>();public JSONObject put(String k,Object v){m.put(k,v);return this;}public String toString(){return m.toString();}}',
   "local/tv/hue/Harness.java":r'''package local.tv.hue;
import java.nio.file.*;import java.io.*;
public class Harness {
 public static void main(String[] args)throws Exception {
  Diagnostics.init(new android.content.Context());
  if(args[0].equals("write")){
   Exception root=new java.net.SocketTimeoutException("PRIVATE_PASSWORD_URL");
   Exception top=new IOException("PRIVATE_API_KEY",root);
   Diagnostics.error("test-id","GET.body.read.error",top);
   String first=Diagnostics.export();
   if(first.contains("PRIVATE_")||!first.contains("SocketTimeoutException")||!first.contains("GET.body.read.error")||!first.contains("category=timeout"))throw new AssertionError(first);
   for(int i=0;i<1600;i++)Diagnostics.event("rotate","event","entry="+i+" "+"x".repeat(200));
   File dir=new File(System.getProperty("test.dir"),"diagnostics");
   if(dir.listFiles().length!=3)throw new AssertionError("rotation");
   for(File f:dir.listFiles())if(f.length()>65536)throw new AssertionError("size");
   if(!Diagnostics.export().contains("entry=1599"))throw new AssertionError("latest missing");
  }else if(!Diagnostics.export().contains("entry=1599"))throw new AssertionError("reopen lost logs");
 }
}'''
  }
  with tempfile.TemporaryDirectory() as tmp:
   d=Path(tmp)
   for name,source in stubs.items():
    f=d/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(source)
   shutil.copy(ROOT/"src/local/tv/hue/Diagnostics.java",d/"local/tv/hue/Diagnostics.java")
   subprocess.run(["javac","-d",str(d/"classes"),*map(str,d.rglob("*.java"))],check=True,capture_output=True)
   for mode in ("write","reopen"):
    subprocess.run(["java","-Dtest.dir="+str(d),"-cp",str(d/"classes"),"local.tv.hue.Harness",mode],check=True,capture_output=True)

if __name__=="__main__":unittest.main()

from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]

@unittest.skipUnless(shutil.which("javac") and shutil.which("java"),"JDK required")
class HttpPacketTests(unittest.TestCase):
 def test_post_body_shares_one_write_with_headers_including_digest_retry(self):
  # Execute the production request writer, not a duplicate packet builder.
  stubs={
   "android/content/Context.java":'package android.content;public class Context {public static final int MODE_PRIVATE=0;public SharedPreferences getSharedPreferences(String n,int m){return new SharedPreferences();}}',
   "android/content/SharedPreferences.java":'package android.content;public class SharedPreferences {public String getString(String k,String d){if(k.equals("certificate"))return "a".repeat(64);return "dummy";}}',
   "android/os/SystemClock.java":'package android.os;public class SystemClock {public static long elapsedRealtime(){return System.nanoTime()/1000000;}}',
   "org/json/JSONException.java":'package org.json;public class JSONException extends Exception {}',
   "org/json/JSONObject.java":'package org.json;public class JSONObject {public JSONObject(){}public JSONObject(String s)throws JSONException {}public JSONObject put(String k,Object v){return this;}public String optString(String k){return "On";}public String toString(){return "{}";}}',
   "local/tv/hue/Diagnostics.java":'package local.tv.hue;class Diagnostics {static String id(){return "test";}static void init(android.content.Context c){}static void event(String a,String b,String c){}static void error(String a,String b,Throwable c){}}',
   "local/tv/hue/PacketHarness.java":r'''package local.tv.hue;
import javax.net.ssl.*;import java.io.*;import java.lang.reflect.*;import java.util.*;
public class PacketHarness {
 static class RecordingOutput extends ByteArrayOutputStream {
  List<byte[]> records=new ArrayList<>();
  public synchronized void write(byte[] b,int off,int len){records.add(Arrays.copyOfRange(b,off,off+len));super.write(b,off,len);}
 }
 static class FakeSocket extends SSLSocket {
  RecordingOutput output=new RecordingOutput();
  InputStream input=new ByteArrayInputStream(("HTTP/1.1 401 Unauthorized\r\nContent-Length: 0\r\nWWW-Authenticate: Digest realm=\"test\", nonce=\"nonce\", qop=\"auth\"\r\n\r\nHTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
  public OutputStream getOutputStream(){return output;}public InputStream getInputStream(){return input;}
  public String[] getSupportedCipherSuites(){return new String[0];}public String[] getEnabledCipherSuites(){return new String[0];}public void setEnabledCipherSuites(String[] x){}
  public String[] getSupportedProtocols(){return new String[0];}public String[] getEnabledProtocols(){return new String[0];}public void setEnabledProtocols(String[] x){}
  public SSLSession getSession(){return null;}public void addHandshakeCompletedListener(HandshakeCompletedListener x){}public void removeHandshakeCompletedListener(HandshakeCompletedListener x){}public void startHandshake(){}
  public void setUseClientMode(boolean x){}public boolean getUseClientMode(){return true;}public void setNeedClientAuth(boolean x){}public boolean getNeedClientAuth(){return false;}public void setWantClientAuth(boolean x){}public boolean getWantClientAuth(){return false;}public void setEnableSessionCreation(boolean x){}public boolean getEnableSessionCreation(){return true;}
 }
 public static void main(String[] args)throws Exception {
  TvApiClient client=new TvApiClient(new android.content.Context());FakeSocket socket=new FakeSocket();
  Field session=TvApiClient.class.getDeclaredField("session");session.setAccessible(true);session.set(client,socket);
  Method request=TvApiClient.class.getDeclaredMethod("request",String.class,String.class);request.setAccessible(true);
  String body="{\"power\":\"On\"}";request.invoke(client,"POST",body);
  if(socket.output.records.size()!=2)throw new AssertionError("Headers/body split into separate writes");
  for(byte[] bytes:socket.output.records){
   String record=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
   if(!record.endsWith("\r\n\r\n"+body)||!record.contains("Content-Length: "+body.getBytes("UTF-8").length+"\r\n"))throw new AssertionError("Incomplete POST record");
  }
  String retry=new String(socket.output.records.get(1),"UTF-8");
  if(!retry.contains("Authorization: Digest "))throw new AssertionError("Digest retry missing");
 }
}'''
  }
  with tempfile.TemporaryDirectory() as tmp:
   d=Path(tmp)
   for name,source in stubs.items():
    f=d/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(source)
   for name in ("TvApiClient.java","PowerController.java"):
    shutil.copy(ROOT/"src/local/tv/hue"/name,d/"local/tv/hue"/name)
   subprocess.run(["javac","-d",str(d/"classes"),*map(str,d.rglob("*.java"))],check=True,capture_output=True)
   subprocess.run(["java","-cp",str(d/"classes"),"local.tv.hue.PacketHarness"],check=True,capture_output=True)

if __name__=="__main__":unittest.main()

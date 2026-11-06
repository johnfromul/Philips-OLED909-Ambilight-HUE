from pathlib import Path
import shutil, subprocess, tempfile, unittest
ROOT=Path(__file__).resolve().parents[1]

@unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
class ResetTests(unittest.TestCase):
 def test_success_rollback_and_interrupted_private_clear(self):
  sources={
   'android/content/SharedPreferences.java': '''package android.content; public class SharedPreferences {
public java.util.Map<String,String> values=new java.util.HashMap<>();public boolean failClear;
public String getString(String k,String d){return values.getOrDefault(k,d);}public boolean contains(String k){return values.containsKey(k);}public Editor edit(){return new Editor();}
public class Editor {boolean clear;java.util.Map<String,String> put=new java.util.HashMap<>();public Editor clear(){clear=true;return this;}public Editor putString(String k,String v){put.put(k,v);return this;}public boolean commit(){if(clear&&failClear)return false;if(clear)values.clear();values.putAll(put);return true;}}
}''',
   'android/content/Context.java': '''package android.content; public class Context {public java.util.Map<String,SharedPreferences> prefs=new java.util.HashMap<>();public SharedPreferences getSharedPreferences(String n,int mode){return prefs.computeIfAbsent(n,k->new SharedPreferences());}}''',
   'org/json/JSONObject.java': '''package org.json; public class JSONObject {public static JSONObject snapshot;public java.util.Map<String,Object> fields=new java.util.HashMap<>();public JSONObject put(String k,Object v){fields.put(k,v);return this;}public String toString(){if(fields.containsKey("native"))snapshot=this;return "PRIVATE_SNAPSHOT";}}''',
   'org/json/JSONArray.java': '''package org.json;public class JSONArray {int count;public JSONArray(int n){count=n;}public int length(){return count;}}''',
   'local/tv/hue/UiStatus.java': '''package local.tv.hue;import android.content.*;final class UiStatus {static boolean paired(Context c){return !c.getSharedPreferences("api",0).getString("user","").isEmpty();}}''',
   'local/tv/hue/TvApiClient.java': '''package local.tv.hue;import android.content.*;final class TvApiClient {static final String PREFS="api";static String power="On";TvApiClient(Context c){if(!UiStatus.paired(c))throw new IllegalStateException("No auth");}String getPower(){return power;}void setPower(boolean on,Runnable notify){power=on?"On":"Off";notify.run();}}''',
   'local/tv/hue/NativeLink.java': '''package local.tv.hue;import android.content.*;import org.json.*;final class NativeLink implements AutoCloseable {
static int bridges,lamps,groups;static String user,key,ip;static boolean failGroup;
NativeLink(Context c){}static void notifyService(Context c,boolean on){}void awaitFree(){}
JSONArray rows(String t){return new JSONArray(t.equals("HueBridgeInfo")?bridges:t.equals("HueLampsInfo")?lamps:groups);}
JSONObject snapshot(String p){return new JSONObject().put("native",true).put("power",p);}
void clear(String t)throws Exception{if(t.equals("HueBridgeInfo"))bridges=0;else if(t.equals("HueLampsInfo"))lamps=0;else{if(failGroup){failGroup=false;throw new Exception("injected");}groups=0;}}
String call(int c){if(c==6)user=null;if(c==9)key=null;if(c==12)ip=null;return c==4?user:c==7?key:c==10?ip:null;}
void restore(JSONObject s){bridges=1;lamps=2;groups=1;user="old-user";key="old-key";ip="old-ip";}public void close(){}
}''',
   'local/tv/hue/ResetHarness.java': '''package local.tv.hue;import android.content.*;import org.json.*;
public class ResetHarness {
 static Context prepared(){Context c=new Context();for(String name:new String[]{"api","hue_bridge","light_drafts","tv_pair_flow","tv_pair_pending"})c.getSharedPreferences(name,0).values.put("user","old-user");NativeLink.bridges=1;NativeLink.lamps=2;NativeLink.groups=1;NativeLink.user="old-user";NativeLink.key="old-key";NativeLink.ip="old-ip";TvApiClient.power="On";return c;}
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[]args)throws Exception{
 Context c=prepared();SettingsReset.run(c);check(NativeLink.bridges==0&&NativeLink.lamps==0&&NativeLink.groups==0);check(NativeLink.user==null&&NativeLink.key==null&&NativeLink.ip==null);check(TvApiClient.power.equals("Off"));check(c.getSharedPreferences("api",0).values.isEmpty());check(c.getSharedPreferences("native_setup_recovery",0).values.isEmpty());
 c=prepared();NativeLink.failGroup=true;try{SettingsReset.run(c);throw new AssertionError();}catch(Exception expected){}check(NativeLink.bridges==1&&NativeLink.lamps==2&&NativeLink.groups==1);check(TvApiClient.power.equals("On"));check(c.getSharedPreferences("api",0).getString("user","").equals("old-user"));check(!c.getSharedPreferences("native_setup_recovery",0).contains("snapshot"));
 c=prepared();c.getSharedPreferences("api",0).failClear=true;try{SettingsReset.run(c);throw new AssertionError();}catch(Exception expected){}check(c.getSharedPreferences("native_setup_recovery",0).contains("snapshot"));check(JSONObject.snapshot.fields.containsKey("reset_tv_api"));
 c=new Context();c.getSharedPreferences("native_setup_recovery",0).values.put("snapshot","pending");NativeLink.bridges=0;NativeLink.lamps=0;try{SettingsReset.run(c);throw new AssertionError();}catch(Exception expected){}check(c.getSharedPreferences("native_setup_recovery",0).contains("snapshot"));
 c=new Context();NativeLink.bridges=0;NativeLink.lamps=0;NativeLink.groups=0;SettingsReset.run(c);check(c.getSharedPreferences("api",0).values.isEmpty());
 }
}'''
  }
  with tempfile.TemporaryDirectory() as tmp:
   p=Path(tmp)
   for name,text in sources.items():
    f=p/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text)
   shutil.copy(ROOT/'src/local/tv/hue/SettingsReset.java',p/'local/tv/hue/SettingsReset.java')
   subprocess.run(['javac','-d',str(p/'classes'),*map(str,p.rglob('*.java'))],check=True,capture_output=True)
   subprocess.run(['java','-cp',str(p/'classes'),'local.tv.hue.ResetHarness'],check=True,capture_output=True)

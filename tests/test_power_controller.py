from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]

@unittest.skipUnless(shutil.which("javac") and shutil.which("java"),"JDK required")
class PowerRecoveryTests(unittest.TestCase):
 def test_recovery_scenarios(self):
  harness=r'''package local.tv.hue;
import java.net.*;import java.io.*;import java.util.*;
public class PowerHarness {
 static class Fake implements PowerController.Transport {
  int writes,reads,notifies,pauses;boolean desired;Exception writeError;Queue<Object> responses=new ArrayDeque<>();List<String> order=new ArrayList<>();
  Fake(Object...values){responses.addAll(Arrays.asList(values));}
  public void write(boolean on)throws Exception{writes++;desired=on;order.add("write");if(writeError!=null){Exception e=writeError;writeError=null;throw e;}}
  public String read()throws Exception{reads++;order.add("read");Object v=responses.isEmpty()?"Off":responses.remove();if(v instanceof Exception)throw (Exception)v;return (String)v;}
  public void notifyService(boolean on){if(on!=desired)throw new AssertionError("wrong intent");notifies++;order.add("notify");}
  public void pause(){pauses++;}
  public void event(String a,String b){}
  public void failure(String a,Exception b){}
 }
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args)throws Exception {
  Fake first=new Fake(new SocketTimeoutException(),"On");PowerController.apply(first,true);
  check(first.writes==1&&first.reads==2&&first.notifies==1);
  check(first.order.indexOf("notify")<first.order.indexOf("read"));
  Fake reverted=new Fake("Off","Off","On");PowerController.apply(reverted,true);
  check(reverted.writes==2&&reverted.notifies==2&&reverted.reads==3);
  Fake lostPost=new Fake("On");lostPost.writeError=new SocketTimeoutException();PowerController.apply(lostPost,true);
  check(lostPost.writes==1&&lostPost.notifies==1);
  Fake off=new Fake("Off");PowerController.apply(off,false);check(!off.desired&&off.writes==1);
  Fake denied=new Fake("On");denied.writeError=new IOException("HTTP 403");
  try{PowerController.apply(denied,true);throw new AssertionError("accepted denial");}catch(IOException expected){}
  check(denied.writes==1&&denied.notifies==0&&denied.reads==0);
  Fake tls=new Fake("On");tls.writeError=new javax.net.ssl.SSLHandshakeException("pin mismatch");
  try{PowerController.apply(tls,true);throw new AssertionError("accepted invalid TLS");}catch(javax.net.ssl.SSLHandshakeException expected){}
  check(tls.writes==1&&tls.notifies==0);
  Fake failed=new Fake("Off","Off","Off","Off");
  try{PowerController.apply(failed,true);throw new AssertionError("false success");}catch(IOException expected){}
  check(failed.writes==2&&failed.reads==4&&failed.notifies==2);
  Fake deniedRead=new Fake(new IOException("HTTP 403"));
  try{PowerController.apply(deniedRead,true);throw new AssertionError("accepted read denial");}catch(IOException expected){}
  check(deniedRead.writes==1&&deniedRead.reads==1);
 }
}'''
  with tempfile.TemporaryDirectory() as tmp:
   d=Path(tmp);src=d/"local/tv/hue";src.mkdir(parents=True)
   shutil.copy(ROOT/"src/local/tv/hue/PowerController.java",src/"PowerController.java")
   (src/"PowerHarness.java").write_text(harness)
   subprocess.run(["javac","-d",str(d/"classes"),*map(str,src.glob("*.java"))],check=True,capture_output=True)
   subprocess.run(["java","-cp",str(d/"classes"),"local.tv.hue.PowerHarness"],check=True,capture_output=True)

if __name__=="__main__":unittest.main()

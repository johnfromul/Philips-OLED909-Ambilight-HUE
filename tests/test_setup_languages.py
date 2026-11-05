from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

@unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK required')
class SetupLanguages(unittest.TestCase):
 def test_system_locale_and_fallback(self):
  sources = {
   'android/content/Context.java': 'package android.content; public class Context { public android.content.res.Resources getResources(){return new android.content.res.Resources();}}',
   'android/content/res/Resources.java': 'package android.content.res; public class Resources {public Configuration getConfiguration(){return new Configuration();}}',
   'android/content/res/Configuration.java': 'package android.content.res; public class Configuration {public java.util.Locale locale=java.util.Locale.getDefault();}',
   'local/tv/hue/LanguageHarness.java': '''package local.tv.hue;
public class LanguageHarness {
 public static void main(String[] args){
  android.content.Context c=new android.content.Context();
  String[] languages={"cs","en","de","fr","es"};
  for(String language:languages){java.util.Locale.setDefault(java.util.Locale.forLanguageTag(language));
   for(String key:SetupStrings.TEXT.keySet())if(SetupStrings.get(c,key)==null||SetupStrings.get(c,key).isEmpty())throw new AssertionError(language+key);
   if(SetupStrings.positions(c).length!=10)throw new AssertionError("native angle count");
  }
  java.util.Locale.setDefault(java.util.Locale.GERMAN);
  if(!SetupStrings.get(c,"save").startsWith("Speichern"))throw new AssertionError("German save");
  java.util.Locale.setDefault(java.util.Locale.FRENCH);
  if(!SetupStrings.get(c,"recovery").startsWith("L’enregistrement"))throw new AssertionError("French recovery");
  java.util.Locale.setDefault(java.util.Locale.forLanguageTag("it"));
  if(!SetupStrings.get(c,"setup").equals("Set up Bridge and bulbs"))throw new AssertionError("fallback");
 }
}'''
  }
  with tempfile.TemporaryDirectory() as tmp:
   d=Path(tmp)
   for name, text in sources.items():
    p=d/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
   shutil.copy(ROOT/'src/local/tv/hue/SetupStrings.java', d/'local/tv/hue/SetupStrings.java')
   subprocess.run(['javac','-d',str(d/'classes'),*map(str,d.rglob('*.java'))],check=True,capture_output=True)
   subprocess.run(['java','-cp',str(d/'classes'),'local.tv.hue.LanguageHarness'],check=True,capture_output=True)

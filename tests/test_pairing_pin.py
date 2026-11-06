from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]

@unittest.skipUnless(shutil.which('javac') and shutil.which('java'),'JDK required')
class PairingPinTests(unittest.TestCase):
 def test_localized_firmware_messages_and_rejection(self):
  harness=r'''package local.tv.hue;
public class PinHarness {
 static void yes(String text,String pin){if(!pin.equals(PairingPin.extract(text)))throw new AssertionError("PIN not detected");}
 static void no(String text){if(PairingPin.extract(text)!=null)throw new AssertionError("unrelated text accepted");}
 public static void main(String[]args){
  yes("Zadejte tento kód v aplikaci Ambilight Hue control: 1234","1234");
  yes("Enter this code on the Ambilight Hue control app: 2345","2345");
  yes("Geben Sie diesen Code über die Ambilight Hue control App ein: 3456","3456");
  yes("Saisissez ce code sur l'application Ambilight Hue control: 4567","4567");
  yes("Introduzca este código en la aplicación Ambilight Hue control: 5678","5678");
  yes("Inserire questo codice su Ambilight Hue control app: 6789","6789");
  yes("Ange den här koden i Ambilight Hue control-appen: 7890","7890");
  yes("Ambilight Hue control\napp:\n0123","0123");
  yes("Ambilight Hue control: 123","123");
  no("Other application: 1234");no("Ambilight Hue controlFake app: 1234");
  no("Ambilight Hue control app: 12345");no("Ambilight Hue control app: expired");no(null);
 }
}'''
  with tempfile.TemporaryDirectory() as tmp:
   p=Path(tmp);shutil.copy(ROOT/'src/local/tv/hue/PairingPin.java',p/'PairingPin.java');(p/'PinHarness.java').write_text(harness)
   subprocess.run(['javac','-d',str(p/'classes'),str(p/'PairingPin.java'),str(p/'PinHarness.java')],check=True,capture_output=True)
   subprocess.run(['java','-cp',str(p/'classes'),'local.tv.hue.PinHarness'],check=True,capture_output=True)

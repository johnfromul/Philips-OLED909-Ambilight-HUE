package local.tv.hue;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.io.EOFException;

/** Bounded recovery of an explicit On/Off command, never a toggle. */
final class PowerController {
 interface Transport {
  void write(boolean on) throws Exception;
  String read() throws Exception;
  void notifyService(boolean on) throws Exception;
  void pause() throws Exception;
  void event(String stage,String detail);
  void failure(String stage,Exception error);
 }
 static boolean transientFailure(Exception e){
  return e instanceof SocketTimeoutException || e instanceof SocketException || e instanceof EOFException;
 }
 static void apply(Transport t,boolean on) throws Exception {
  String desired=on?"On":"Off";
  for(int write=0;write<2;write++){
   t.event("power.write.attempt","attempt="+write+" desired="+desired);
   boolean notified=false;
   try {t.write(on);}
   catch(Exception e){
    t.failure("power.write.error",e);
    if(!transientFailure(e))throw e;
    // A lost response does not prove the command failed. Read before rewriting.
   }
   // Do not let verification failure prevent delivery of the user's explicit intent.
   // A permanent/authentication error above exits before this notification.
   t.notifyService(on);notified=true;
   for(int read=0;read<2;read++){
    try {
     String saved=t.read();
     t.event("power.verify","write_attempt="+write+" read_attempt="+read+" desired="+desired+" saved="+saved);
     if(desired.equals(saved)){t.event("power.set.ok","native_notification="+notified);return;}
     if(!"On".equals(saved)&&!"Off".equals(saved))throw new IOException("Neplatný stav přepínače Hue.");
    }catch(Exception e){t.failure("power.verify.error",e);if(!transientFailure(e))throw e;}
    t.pause();
   }
  }
  throw new IOException("TV nepotvrdila přepínač Hue ani po obnovení spojení.");
 }
}

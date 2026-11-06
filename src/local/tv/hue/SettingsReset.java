package local.tv.hue;
import android.content.*;import java.io.IOException;import org.json.*;
final class SettingsReset {
 static void run(Context c)throws Exception{
  android.content.SharedPreferences recovery=c.getSharedPreferences("native_setup_recovery",0);
  try(NativeLink link=new NativeLink(c)){
   if(recovery.contains("snapshot"))throw new IOException("Recover pending setup before reset");
   if(!UiStatus.paired(c)&&link.rows("HueBridgeInfo").length()==0&&link.rows("HueLampsInfo").length()==0){link.call(6);link.call(9);link.call(12);link.clear("HueGroupInfo");clearPrivate(c);return;}
   TvApiClient api=new TvApiClient(c);String power=api.getPower();if(!"On".equals(power)&&!"Off".equals(power))throw new IOException("Unknown power");
   JSONObject snapshot=link.snapshot(power);JSONObject apiBackup=new JSONObject();android.content.SharedPreferences credentials=c.getSharedPreferences(TvApiClient.PREFS,0);for(String field:new String[]{"user","password","certificate","host"})apiBackup.put(field,credentials.getString(field,""));snapshot.put("reset_tv_api",apiBackup);if(!recovery.edit().putString("snapshot",snapshot.toString()).commit())throw new IOException("Backup failed");
   try{
    api.setPower(false,()->NativeLink.notifyService(c,false));link.awaitFree();
    link.clear("HueLampsInfo");link.clear("HueGroupInfo");link.clear("HueBridgeInfo");link.call(6);link.call(9);link.call(12);
    if(link.rows("HueLampsInfo").length()!=0||link.rows("HueBridgeInfo").length()!=0||link.rows("HueGroupInfo").length()!=0||link.call(4)!=null||link.call(7)!=null||link.call(10)!=null)throw new IOException("Reset verification failed");
   }catch(Exception e){link.restore(snapshot);boolean on="On".equals(power);api.setPower(on,()->NativeLink.notifyService(c,on));recovery.edit().clear().commit();throw e;}
   // Keep the recovery snapshot until native clearing has been verified.
   clearPrivate(c);
  }
 }
 private static void clearPrivate(Context c)throws Exception{for(String name:new String[]{"hue_bridge","light_drafts","tv_pair_flow","tv_pair_pending",TvApiClient.PREFS,"native_setup_recovery"})if(!c.getSharedPreferences(name,0).edit().clear().commit())throw new IOException("Private settings reset incomplete");}
}

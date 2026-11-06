package local.tv.hue;
import android.content.*;import android.database.Cursor;import android.net.Uri;import java.util.concurrent.TimeUnit;
final class UiStatus {
 static boolean paired(Context c){android.content.SharedPreferences p=c.getSharedPreferences(TvApiClient.PREFS,0);return !p.getString("user","").isEmpty()&&!p.getString("password","").isEmpty()&&!p.getString("certificate","").isEmpty();}
 static String configuredBridge(Context c){
  try(Cursor cursor=c.getContentResolver().query(Uri.withAppendedPath(NativeLink.BASE,"HueBridgeInfo"),new String[]{"MacAddr"},null,null,null)){
   if(cursor!=null&&cursor.moveToFirst()){String mac=cursor.getString(0).replace(":","").toLowerCase(java.util.Locale.ROOT);android.content.SharedPreferences p=c.getSharedPreferences("hue_bridge",0);
    if(mac.equals(p.getString("configured_mac","")))return p.getString("configured_name",mac)+" · "+p.getString("configured_host","")+" · "+mac;
    if(mac.equals(p.getString("mac",""))){String host=p.getString("host","");String name=p.getString("name",mac);if(name.equals(mac)&&System.currentTimeMillis()-p.getLong("name_attempt",0)>60000){p.edit().putLong("name_attempt",System.currentTimeMillis()).apply();String found=lookupName(host,mac);if(found!=null){name=found;p.edit().putString("configured_name",name).putString("configured_host",host).putString("configured_mac",mac).apply();}}return name+" · "+host+" · "+mac;}
    return mac;
   }
  }catch(Exception e){Diagnostics.error("status","bridge.read.failed",e);return FeatureStrings.get(c,"unknown");}
  return FeatureStrings.get(c,"not_ready");
 }
 private static String lookupName(String host,String mac){java.net.HttpURLConnection c=null;try{if(!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))return null;c=(java.net.HttpURLConnection)new java.net.URL("http://"+host+"/api/config").openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(2000);c.setReadTimeout(2000);try(java.io.InputStream in=c.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>65536)return null;}org.json.JSONObject info=new org.json.JSONObject(out.toString("UTF-8"));if(!mac.equals(info.optString("mac").replace(":","").toLowerCase(java.util.Locale.ROOT)))return null;return info.optString("name",mac);}}catch(Exception e){return null;}finally{if(c!=null)c.disconnect();}}
 static String nativeActive(Context c){
  Process p=null;try{p=new ProcessBuilder("/system/bin/getprop","odm.tpv.sys.HueIsActive").start();if(!p.waitFor(2,TimeUnit.SECONDS))return FeatureStrings.get(c,"service_unknown");byte[] b=new byte[64];int n=p.getInputStream().read(b);String v=n<0?"":new String(b,0,n,"UTF-8").trim();return "true".equals(v)?FeatureStrings.get(c,"active"):"false".equals(v)?FeatureStrings.get(c,"inactive"):FeatureStrings.get(c,"service_unknown");}catch(Exception e){return FeatureStrings.get(c,"service_unknown");}finally{if(p!=null)p.destroy();}
 }
}

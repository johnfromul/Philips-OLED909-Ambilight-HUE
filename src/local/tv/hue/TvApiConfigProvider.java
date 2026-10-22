package local.tv.hue;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONObject;

/** Provisioning requires Android DUMP permission (ADB shell), never an ordinary app. */
public class TvApiConfigProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 @Override public Bundle call(String method,String arg,Bundle extras){
  getContext().enforceCallingOrSelfPermission("android.permission.DUMP","ADB provisioning only");
  if("status".equals(method)){Bundle status=new Bundle();status.putString("certificate",getContext().getSharedPreferences(TvApiClient.PREFS,Context.MODE_PRIVATE).getString("certificate",""));return status;}
  if(!"configure".equals(method))throw new IllegalArgumentException("Unknown provisioning operation");
  try {
   JSONObject value=new JSONObject(arg);String user=value.getString("user"),password=value.getString("password"),certificate=value.getString("certificate"),host=value.getString("host");
   if(!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))throw new IllegalArgumentException("Invalid host");
   if(user.isEmpty()||password.isEmpty()||!certificate.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid configuration");
   boolean ok=getContext().getSharedPreferences(TvApiClient.PREFS,Context.MODE_PRIVATE).edit().putString("user",user).putString("password",password).putString("certificate",certificate).putString("host",host).commit();
   Bundle result=new Bundle();result.putBoolean("configured",ok);return result;
  }catch(org.json.JSONException e){throw new IllegalArgumentException("Invalid configuration");}
 }
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException();}
 public String getType(Uri u){return null;}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
 public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}

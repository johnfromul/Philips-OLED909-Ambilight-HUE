package local.tv.hue;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.io.IOException;
import java.util.concurrent.*;

/** Native Binder bridge. Call from a worker; bind callbacks use the main looper. */
final class NativeLink implements AutoCloseable {
 static final Uri BASE=Uri.parse("content://org.droidtv.ambihue.provider");
 final Context context; final CompletableFuture<IBinder> ready=new CompletableFuture<>(); boolean bound;
 final ServiceConnection connection=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder b){ready.complete(b);}public void onServiceDisconnected(ComponentName n){}};
 NativeLink(Context c)throws Exception{
  context=c.getApplicationContext();new Handler(Looper.getMainLooper()).post(()->{try{bound=context.bindService(new Intent().setComponent(new ComponentName("org.droidtv.ambihuecommon","org.droidtv.ambihue.LinkService")),connection,Context.BIND_AUTO_CREATE);if(!bound)ready.completeExceptionally(new IOException("Native link unavailable"));}catch(Exception e){ready.completeExceptionally(e);}});
  try{IBinder binder=ready.get(8,TimeUnit.SECONDS);if(!"org.droidtv.ambihue.LinkServiceAccess".equals(binder.getInterfaceDescriptor()))throw new IOException("Unexpected native interface");}catch(Exception e){close();throw e;}
 }
 String call(int code,String...args)throws Exception{
  Parcel in=Parcel.obtain(),out=Parcel.obtain();try{in.writeInterfaceToken("org.droidtv.ambihue.LinkServiceAccess");for(String s:args)in.writeString(s);if(!ready.get().transact(code,in,out,0))throw new IOException("Native transaction unavailable");out.readException();return code==4||code==7||code==10?out.readString():null;}finally{in.recycle();out.recycle();}
 }
 static void notifyService(Context c,boolean on){ComponentName n=c.startForegroundService(new Intent().setComponent(new ComponentName("org.droidtv.alhuewizard","org.droidtv.ambihue.HueService")).setAction("org.droidtv.ambihue.HueService."+(on?"ALHUEON":"ALHUEOFF")));if(n==null)throw new IllegalStateException("Native Hue service unavailable");}
 void awaitFree()throws Exception{long until=SystemClock.elapsedRealtime()+8000;while(true){Parcel in=Parcel.obtain(),out=Parcel.obtain();int status;try{in.writeInterfaceToken("org.droidtv.ambihue.LinkServiceAccess");if(!ready.get().transact(3,in,out,0))throw new IOException("Bridge status unavailable");out.readException();status=out.readInt();}finally{in.recycle();out.recycle();}if(status==2)return;if(SystemClock.elapsedRealtime()>=until)throw new IOException("Bridge is busy");Thread.sleep(150);}}
 JSONArray rows(String table)throws Exception{
  JSONArray result=new JSONArray();try(Cursor c=context.getContentResolver().query(Uri.withAppendedPath(BASE,table),null,null,null,null)){if(c==null)throw new IOException("Native provider unavailable");while(c.moveToNext()){JSONObject row=new JSONObject();for(int i=0;i<c.getColumnCount();i++)row.put(c.getColumnName(i),c.isNull(i)?JSONObject.NULL:c.getString(i));result.put(row);}}return result;
 }
 JSONObject snapshot(String power)throws Exception{return new JSONObject().put("bridge",rows("HueBridgeInfo")).put("lamps",rows("HueLampsInfo")).put("groups",rows("HueGroupInfo")).put("user",call(4)).put("key",call(7)).put("ip",call(10)).put("power",power);}
 void credentials(String mac,String user,String key,String ip)throws Exception{
  call(5,mac,user);call(8,mac,key);call(11,ip);
  if(!user.equals(call(4))||!key.equals(call(7))||!ip.equals(call(10)))throw new IOException("Native credential verification failed");
 }
 void clear(String table){context.getContentResolver().delete(Uri.withAppendedPath(BASE,table),null,null);}
 void insert(String table,JSONObject row)throws Exception{ContentValues v=new ContentValues();java.util.Iterator<String> keys=row.keys();while(keys.hasNext()){String key=keys.next();if(!row.isNull(key))v.put(key,row.getString(key));}if(context.getContentResolver().insert(Uri.withAppendedPath(BASE,table),v)==null)throw new IOException("Native insert failed");}
 void restore(JSONObject s)throws Exception{
  clear("HueLampsInfo");clear("HueGroupInfo");clear("HueBridgeInfo");
  JSONArray bridge=s.getJSONArray("bridge");for(int i=0;i<bridge.length();i++)insert("HueBridgeInfo",bridge.getJSONObject(i));
  if(bridge.length()>0&&!s.isNull("user")){String mac=bridge.getJSONObject(0).getString("MacAddr");call(5,mac,s.getString("user"));if(s.isNull("key"))call(9);else call(8,mac,s.getString("key"));if(s.isNull("ip"))call(12);else call(11,s.getString("ip"));}
  else{call(6);call(9);call(12);}
  for(String table:new String[]{"HueLampsInfo","HueGroupInfo"}){JSONArray rows=s.getJSONArray(table.equals("HueLampsInfo")?"lamps":"groups");for(int i=0;i<rows.length();i++)insert(table,rows.getJSONObject(i));}
  if(!rows("HueLampsInfo").toString().equals(s.getJSONArray("lamps").toString()))throw new IOException("Restoration verification failed");
  for(int code:new int[]{4,7,10}){String key=code==4?"user":code==7?"key":"ip";if(!java.util.Objects.equals(s.isNull(key)?null:s.getString(key),call(code)))throw new IOException("Restored credential verification failed");}
 }
 public void close(){new Handler(Looper.getMainLooper()).post(()->{if(bound){context.unbindService(connection);bound=false;}});}
}

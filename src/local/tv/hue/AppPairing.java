package local.tv.hue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.EditText;
import org.json.JSONObject;
import org.json.JSONArray;
import javax.net.ssl.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.cert.X509Certificate;
import java.io.*;

/** Local TV pairing. Initial certificate trust is restricted to loopback. */
final class AppPairing {
 static String text(Context c,int key){
  String l=c.getResources().getConfiguration().locale.getLanguage();
  String[][] words={
   {"App pairing","Requesting TV PIN…","Enter the PIN displayed by the TV, then confirm before it expires.","Confirm","App paired successfully.","Pairing failed. Try again. Diagnostics: ","Verifying pairing…"},
   {"Spárovat aplikaci","Žádám TV o PIN…","Zadejte PIN z obrazovky TV a potvrďte před vypršením platnosti.","Potvrdit","Aplikace byla úspěšně spárována.","Párování selhalo. Zkuste to znovu. Diagnostika: ","Ověřuji párování…"},
   {"App koppeln","TV-PIN wird angefordert…","Geben Sie die PIN vom TV ein und bestätigen Sie vor Ablauf.","Bestätigen","App erfolgreich gekoppelt.","Kopplung fehlgeschlagen. Erneut versuchen. Diagnose: ","Kopplung wird geprüft…"},
   {"Associer l’application","Demande du PIN au téléviseur…","Saisissez le PIN affiché sur le téléviseur et confirmez avant son expiration.","Confirmer","Application associée.","Échec de l’association. Réessayez. Diagnostic : ","Vérification de l’association…"},
   {"Vincular aplicación","Solicitando PIN al televisor…","Introduzca el PIN del televisor y confirme antes de que caduque.","Confirmar","Aplicación vinculada correctamente.","Error de vinculación. Inténtelo de nuevo. Diagnóstico: ","Verificando vinculación…"}};
  int i="cs".equals(l)?1:"de".equals(l)?2:"fr".equals(l)?3:"es".equals(l)?4:0;return words[i][key];
 }
 interface Result {void done(String message);}
 static boolean prepare(Activity a,Result result){
  if(!PairingAccessibility.ready()){
   String l=a.getResources().getConfiguration().locale.getLanguage();
   String help=SetupStrings.get(a,"pair_permission");
   result.done(help);
   new AlertDialog.Builder(a).setTitle(text(a,0)).setMessage(help).setPositiveButton(text(a,3),(d,w)->{try{a.startActivity(new android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(RuntimeException e){failure(a,result,e);}}).setNegativeButton(Strings.get(a,"cancel"),null).show();return false;
  }
  return true;
 }
 static void start(Activity a,Result result){start(a,result,null);}
 static void start(Activity a,Result result,Runnable success){
  if(!prepare(a,result))return;
  java.util.concurrent.CompletableFuture<String> pinResult=PairingAccessibility.arm();
  new Thread(()->{
   SharedPreferences pending=a.getSharedPreferences("tv_pair_pending",0);
   try {
    pending.edit().clear().commit();
    JSONObject device=new JSONObject().put("device_name","Ambilight Hue Launcher").put("device_os","Android").put("app_name","Ambilight Hue control").put("type","native").put("app_id","local.tv.hue").put("id",java.util.UUID.randomUUID().toString().replace("-",""));
    final String[] fingerprint={null};
    SSLContext ssl=SSLContext.getInstance("TLS");
    ssl.init(null,new TrustManager[]{new X509TrustManager(){
     public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
     public void checkClientTrusted(X509Certificate[] chain,String auth)throws java.security.cert.CertificateException{throw new java.security.cert.CertificateException();}
     public void checkServerTrusted(X509Certificate[] chain,String auth)throws java.security.cert.CertificateException{try{if(chain==null||chain.length==0)throw new java.security.cert.CertificateException();fingerprint[0]=TvApiClient.hex(MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded()));}catch(GeneralSecurityException e){throw new java.security.cert.CertificateException(e);}}
    }},new SecureRandom());
    HttpsURLConnection connection=(HttpsURLConnection)new java.net.URL("https://127.0.0.1:1926/6/pair/request").openConnection();
    JSONObject reply;
    try {
     connection.setSSLSocketFactory(ssl.getSocketFactory());connection.setHostnameVerifier((host,session)->"127.0.0.1".equals(host));
     connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(7000);connection.setReadTimeout(12000);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");
     byte[] body=new JSONObject().put("scope",new JSONArray().put("read").put("write").put("control")).put("device",device).toString().getBytes("UTF-8");
     connection.setFixedLengthStreamingMode(body.length);try(OutputStream out=connection.getOutputStream()){out.write(body);}
     try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[2048];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>65536)throw new IOException();}reply=new JSONObject(out.toString("UTF-8"));}
    }finally{connection.disconnect();}
    if(!"SUCCESS".equals(reply.optString("error_id"))||fingerprint[0]==null)throw new IOException();
    if(!pending.edit().putString("user",device.getString("id")).putString("password",reply.getString("auth_key")).putString("certificate",fingerprint[0]).commit())throw new IOException();
    String code=pinResult.get(35,java.util.concurrent.TimeUnit.SECONDS);
    a.runOnUiThread(()->result.done(text(a,6)));
       byte[] key=android.util.Base64.decode("JCqdN5AcnAHgJYseUn7ER5k3qgtemfUvMRghQpTfTZq7Cvv8EPQPqfz6dDxPQPSu4gKFPWkJGw32zyASgJkHwCjU",0);
       Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(key,"HmacSHA1"));
       String signature=android.util.Base64.encodeToString(TvApiClient.hex(mac.doFinal((reply.get("timestamp").toString()+code).getBytes("UTF-8"))).getBytes("UTF-8"),android.util.Base64.NO_WRAP);
       JSONObject auth=new JSONObject().put("auth_AppId","1").put("pin",code).put("auth_timestamp",reply.get("timestamp")).put("auth_signature",signature);
       TvApiClient client=TvApiClient.pairingClient(a,pending);
       JSONObject grant=new JSONObject(client.pairingGrant(new JSONObject().put("auth",auth).put("device",device).toString()));
       String outcome=grant.optString("error_id");Diagnostics.event("pairing","grant.result","success="+"SUCCESS".equals(outcome)+" code="+(outcome.matches("[A-Z_0-9]{1,60}")?outcome:"unknown"));
       if(!"SUCCESS".equals(outcome))throw new IOException();
       TvApiClient.pairingClient(a,pending).getPower();
       SharedPreferences.Editor saved=a.getSharedPreferences(TvApiClient.PREFS,0).edit();
       for(String field:new String[]{"user","password","certificate"})saved.putString(field,pending.getString(field,""));
       if(!saved.putString("host","127.0.0.1").commit())throw new IOException();
       a.runOnUiThread(()->{result.done(text(a,4));if(success!=null)success.run();});
   }catch(Exception e){failure(a,result,e);}finally{PairingAccessibility.disarm(pinResult);pending.edit().clear().commit();}
  },"HuePairRequest").start();
 }
 private static void failure(Activity a,Result result,Exception e){String id=Diagnostics.id();Diagnostics.error(id,"pairing.error",e);a.runOnUiThread(()->result.done(text(a,5)+id));}
}

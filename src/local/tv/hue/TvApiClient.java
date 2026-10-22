package local.tv.hue;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import javax.net.ssl.*;
import java.net.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.regex.*;

final class TvApiClient {
 static final String PREFS="tv_native_api";
 private final String user,password,pin,host;
 private final SSLSocketFactory tls;
 TvApiClient(Context context) throws Exception {
  SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
  user=p.getString("user","");password=p.getString("password","");pin=p.getString("certificate","");
  host=p.getString("host","");
  if(!host.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))throw new IOException("Síťová adresa TV API není nastavená.");
  if(user.isEmpty()||password.isEmpty()||!pin.matches("[a-f0-9]{64}"))throw new IOException("Přístup k TV API není nastavený. Obnovte párování TV API podle návodu k APK.");
  SSLContext ssl=SSLContext.getInstance("TLS");
  ssl.init(null,new TrustManager[]{new X509TrustManager(){
   public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
   public void checkClientTrusted(X509Certificate[] c,String auth) throws CertificateException {throw new CertificateException("Client certificate unsupported");}
   public void checkServerTrusted(X509Certificate[] c,String auth) throws CertificateException {
    try {if(c==null||c.length==0||!pin.equals(hex(MessageDigest.getInstance("SHA-256").digest(c[0].getEncoded()))))throw new CertificateException("TV certificate changed");}
    catch(GeneralSecurityException e){throw new CertificateException(e);}
   }
  }},new SecureRandom());tls=ssl.getSocketFactory();
 }
 private SSLSocket session;
 private String challenge;
 private void connect() throws Exception {
  Socket raw=new Socket();
  try {raw.connect(new InetSocketAddress(host,1926),7000);raw.setSoTimeout(9000);
   session=(SSLSocket)tls.createSocket(raw,host,1926,true);
   session.setSoTimeout(9000);session.setEnabledProtocols(new String[]{"TLSv1.2"});session.startHandshake();
  }catch(Exception e){raw.close();throw e;}
 }
 void setPower(boolean on) throws Exception {
  String desired=on?"On":"Off";
  try {
   connect();
   request("POST",new JSONObject().put("power",desired).toString());
   JSONObject saved=new JSONObject(request("GET",null));
   if(!desired.equals(saved.optString("power")))throw new IOException("TV nepotvrdila uložení přepínače Hue. Zkuste to znovu.");
  }finally {if(session!=null)session.close();}
 }
 private String request(String method,String body) throws Exception {
  String uri="/6/HueLamp/power";
  for(int attempt=0;attempt<3;attempt++) {
   android.util.Log.i("HueNativeAPI",method+" attempt="+attempt);
   byte[] bytes=body==null?new byte[0]:body.getBytes("UTF-8");
   String headers=method+" "+uri+" HTTP/1.1\r\nHost: "+host+":1926\r\nAccept: application/json\r\nConnection: keep-alive\r\nUser-Agent: AmbilightHue/1.5\r\n";
   if(challenge!=null)headers+="Authorization: "+digest(challenge,method,uri)+"\r\n";
   if(body!=null)headers+="Content-Type: application/json\r\nContent-Length: "+bytes.length+"\r\n";
   OutputStream output=session.getOutputStream();output.write((headers+"\r\n").getBytes("UTF-8"));output.write(bytes);output.flush();
   InputStream input=session.getInputStream();String[] parts=line(input).split(" ",3);
   if(parts.length<2)throw new IOException("Neplatná odpověď TV API.");
   int code=Integer.parseInt(parts[1]);Map<String,String> responseHeaders=new HashMap<>();int headerBytes=0;
   for(String h=line(input);!h.isEmpty();h=line(input)){headerBytes+=h.length();if(headerBytes>32768)throw new IOException("Příliš velká hlavička TV API.");int colon=h.indexOf(':');if(colon>0)responseHeaders.put(h.substring(0,colon).toLowerCase(Locale.ROOT),h.substring(colon+1).trim());}
   android.util.Log.i("HueNativeAPI",method+" HTTP="+code+" length="+responseHeaders.get("content-length")+" framing="+responseHeaders.get("transfer-encoding"));
   String response="";
   if("chunked".equalsIgnoreCase(responseHeaders.get("transfer-encoding"))){
    ByteArrayOutputStream data=new ByteArrayOutputStream();
    for(;;){String size=line(input).split(";",2)[0];int n=Integer.parseInt(size.trim(),16);if(n<0||n>65536-data.size())throw new IOException("Příliš velká odpověď TV API.");if(n==0){while(!line(input).isEmpty()){}break;}for(int i=0;i<n;i++){int v=input.read();if(v<0)throw new EOFException();data.write(v);}if(!line(input).isEmpty())throw new IOException("Neplatná odpověď TV API.");}
    response=data.toString("UTF-8");
   }else if(responseHeaders.containsKey("content-length")){
    int n=Integer.parseInt(responseHeaders.get("content-length"));if(n<0||n>65536)throw new IOException("Příliš velká odpověď TV API.");byte[] data=new byte[n];for(int pos=0;pos<n;){int count=input.read(data,pos,n-pos);if(count<0)throw new EOFException();pos+=count;}response=new String(data,"UTF-8");
   }else if(code==200&&"GET".equals(method))response=read(input);
   if(code==401){challenge=responseHeaders.get("www-authenticate");if(challenge==null)throw new IOException("TV API odmítlo přihlášení.");continue;}
   if(code!=200&&code!=204)throw new IOException("TV API odmítlo příkaz (HTTP "+code+").");
   return response;
  }
  throw new IOException("TV nepřijala přihlašovací údaje. Obnovte párování TV API.");
 }
 private static String line(InputStream input)throws IOException {
  ByteArrayOutputStream b=new ByteArrayOutputStream();int c;
  while((c=input.read())!=-1){if(c==10)return b.toString("ISO-8859-1").replace("\r","");b.write(c);if(b.size()>8192)throw new IOException("Neplatná hlavička TV API.");}
  throw new EOFException("TV uzavřela spojení před odpovědí.");
 }
 private static String read(InputStream stream) throws IOException {
  if(stream==null)return "";
  try(ByteArrayOutputStream out=new ByteArrayOutputStream()) {
   InputStream in=stream;
   int n;while((n=in.read())!=-1){out.write(n);if(out.size()>65536)throw new IOException("Neočekávaná odpověď TV API.");if(n=='}'){String value=out.toString("UTF-8");try{return new JSONObject(value).toString();}catch(org.json.JSONException incomplete){}}}return out.toString("UTF-8");
  }
 }
 private String digest(String challenge,String method,String uri) throws Exception {
  if(challenge==null||!challenge.regionMatches(true,0,"Digest ",0,7))throw new IOException("TV API nepodporuje očekávané přihlášení.");
  Map<String,String> a=new HashMap<>();
  Matcher m=Pattern.compile("(\\w+)\\s*=\\s*(?:\"([^\"]*)\"|([^,\\s]+))").matcher(challenge.substring(7));
  while(m.find())a.put(m.group(1).toLowerCase(Locale.ROOT),m.group(2)!=null?m.group(2):m.group(3));
  String realm=a.get("realm"),nonce=a.get("nonce"),algorithm=a.containsKey("algorithm")?a.get("algorithm"):"MD5";
  if(realm==null||nonce==null||!("MD5".equalsIgnoreCase(algorithm)||"MD5-sess".equalsIgnoreCase(algorithm)))throw new IOException("Nepodporovaná výzva TV API.");
  String qop=a.get("qop"),cnonce=UUID.randomUUID().toString().replace("-",""),nc="00000001";
  boolean auth=false;if(qop!=null){for(String q:qop.split(","))if("auth".equals(q.trim()))auth=true;if(!auth)throw new IOException("Nepodporovaný režim ověření TV API.");}
  String ha1=md5(user+":"+realm+":"+password);
  if("MD5-sess".equalsIgnoreCase(algorithm))ha1=md5(ha1+":"+nonce+":"+cnonce);
  String ha2=md5(method+":"+uri);
  String response=md5(ha1+":"+nonce+":"+(auth?nc+":"+cnonce+":auth:":"")+ha2);
  String result="Digest username="+quote(user)+", realm="+quote(realm)+", nonce="+quote(nonce)+", uri="+quote(uri)+", response="+quote(response)+", algorithm="+algorithm;
  if(auth)result+=", qop=auth, nc="+nc+", cnonce="+quote(cnonce);
  else if("MD5-sess".equalsIgnoreCase(algorithm))result+=", cnonce="+quote(cnonce);
  if(a.containsKey("opaque"))result+=", opaque="+quote(a.get("opaque"));return result;
 }
 private static String quote(String s)throws IOException{if(s.indexOf('\r')>=0||s.indexOf('\n')>=0)throw new IOException("Neplatná výzva TV API.");return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
 private static String md5(String s)throws Exception{return hex(MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8")));}
 static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte n:b)s.append(String.format(Locale.ROOT,"%02x",n&255));return s.toString();}
}

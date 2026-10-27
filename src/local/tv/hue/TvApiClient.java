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
 private final String operation;
 private String stage="config";
 private long started;
 private long deadline=Long.MAX_VALUE;
 private int readTimeout=9000;
 private int budget(int maximum) throws SocketTimeoutException {long left=deadline-android.os.SystemClock.elapsedRealtime();if(left<=0)throw new SocketTimeoutException("Power operation deadline");return (int)Math.min(maximum,left);}
 private void trace(String value){stage=value;Diagnostics.event(operation,value,"duration_ms="+(android.os.SystemClock.elapsedRealtime()-started));}
 TvApiClient(Context context) throws Exception {this(context,Diagnostics.id());}
 TvApiClient(Context context,String operation) throws Exception {
  this.operation=operation;started=android.os.SystemClock.elapsedRealtime();Diagnostics.init(context);trace("config.begin");
  SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
  user=p.getString("user","");password=p.getString("password","");pin=p.getString("certificate","");
  // The launcher controls the TV on which it runs; never depend on its LAN/VPN IP.
  host="127.0.0.1";
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
  trace("tcp.connect.begin");
  Socket raw=new Socket();
  try {raw.connect(new InetSocketAddress(host,1926),budget(7000));raw.setSoTimeout(budget(readTimeout));trace("tcp.connect.ok");
   session=(SSLSocket)tls.createSocket(raw,host,1926,true);
   session.setSoTimeout(budget(readTimeout));session.setEnabledProtocols(new String[]{"TLSv1.2"});trace("tls.handshake.begin");session.startHandshake();trace("tls.handshake.ok");
  }catch(Exception e){raw.close();throw e;}
 }
 void setPower(boolean on,Runnable notifyService) throws Exception {
  deadline=android.os.SystemClock.elapsedRealtime()+30000;
  try {
   PowerController.apply(new PowerController.Transport(){
    public void write(boolean value)throws Exception {exchange("POST",new JSONObject().put("power",value?"On":"Off").toString());}
    public String read()throws Exception {return getPower();}
    public void notifyService(boolean value){notifyService.run();}
    public void pause()throws Exception {budget(1000);Thread.sleep(Math.min(500,budget(500)));}
    public void event(String stage,String detail){Diagnostics.event(operation,stage,detail);}
    public void failure(String stage,Exception e){Diagnostics.error(operation,stage,e);}
   },on);
  }finally {close();deadline=Long.MAX_VALUE;}
 }
 private String exchange(String method,String body)throws Exception {
  close();challenge=null;readTimeout="GET".equals(method)?3000:9000;
  try {connect();return request(method,body);}
  catch(Exception e){Diagnostics.error(operation,stage+".error",e);throw e;}
  finally {close();}
 }
 private void close(){if(session!=null){try{session.close();}catch(Exception e){Diagnostics.error(operation,"socket.close.error",e);}finally{session=null;}}}
 private static String safePower(String power){return "On".equals(power)||"Off".equals(power)?power:"unknown";}
 String getPower() throws Exception {
  String power=safePower(new JSONObject(exchange("GET",null)).optString("power"));Diagnostics.event(operation,"power.observed","saved="+power);return power;
 }
 private String request(String method,String body) throws Exception {
  String uri="/6/HueLamp/power";
  for(int attempt=0;attempt<3;attempt++) {
   trace(method+".attempt."+attempt+".write");
   byte[] bytes=body==null?new byte[0]:body.getBytes("UTF-8");
   String headers=method+" "+uri+" HTTP/1.1\r\nHost: "+host+":1926\r\nAccept: application/json\r\nConnection: keep-alive\r\nUser-Agent: AmbilightHue/1.8\r\n";
   if(challenge!=null)headers+="Authorization: "+digest(challenge,method,uri)+"\r\n";
   if(body!=null)headers+="Content-Type: application/json\r\nContent-Length: "+bytes.length+"\r\n";
   ByteArrayOutputStream packet=new ByteArrayOutputStream();
   packet.write((headers+"\r\n").getBytes("UTF-8"));packet.write(bytes);
   // Philips can acknowledge an empty command if headers/body arrive in separate TLS records.
   OutputStream output=session.getOutputStream();output.write(packet.toByteArray());output.flush();
   trace(method+".status.read");
   session.setSoTimeout(budget(readTimeout));
   InputStream input=session.getInputStream();String[] parts=line(input).split(" ",3);
   if(parts.length<2)throw new IOException("Neplatná odpověď TV API.");
   int code=Integer.parseInt(parts[1]);Map<String,String> responseHeaders=new HashMap<>();int headerBytes=0;
   trace(method+".headers.read");
   for(String h=line(input);!h.isEmpty();h=line(input)){headerBytes+=h.length();if(headerBytes>32768)throw new IOException("Příliš velká hlavička TV API.");int colon=h.indexOf(':');if(colon>0)responseHeaders.put(h.substring(0,colon).toLowerCase(Locale.ROOT),h.substring(colon+1).trim());}
   Diagnostics.event(operation,method+".response","http="+code+" has_length="+responseHeaders.containsKey("content-length")+" chunked="+"chunked".equalsIgnoreCase(responseHeaders.get("transfer-encoding"))+" closes="+"close".equalsIgnoreCase(responseHeaders.get("connection"))+" duration_ms="+(android.os.SystemClock.elapsedRealtime()-started));
   trace(method+".body.read");
   String response="";
   if("chunked".equalsIgnoreCase(responseHeaders.get("transfer-encoding"))){
    ByteArrayOutputStream data=new ByteArrayOutputStream();
    for(;;){String size=line(input).split(";",2)[0];int n=Integer.parseInt(size.trim(),16);if(n<0||n>65536-data.size())throw new IOException("Příliš velká odpověď TV API.");if(n==0){while(!line(input).isEmpty()){}break;}for(int i=0;i<n;i++){int v=input.read();if(v<0)throw new EOFException();data.write(v);}if(!line(input).isEmpty())throw new IOException("Neplatná odpověď TV API.");}
    response=data.toString("UTF-8");
   }else if(responseHeaders.containsKey("content-length")){
    int n=Integer.parseInt(responseHeaders.get("content-length"));if(n<0||n>65536)throw new IOException("Příliš velká odpověď TV API.");byte[] data=new byte[n];for(int pos=0;pos<n;){int count=input.read(data,pos,n-pos);if(count<0)throw new EOFException();pos+=count;}response=new String(data,"UTF-8");
   }else if(code==200&&"GET".equals(method))response=read(input);
   if(code==401){trace(method+".digest.challenge");challenge=responseHeaders.get("www-authenticate");if(challenge==null)throw new IOException("TV API odmítlo přihlášení.");if("close".equalsIgnoreCase(responseHeaders.get("connection"))){close();connect();}continue;}
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

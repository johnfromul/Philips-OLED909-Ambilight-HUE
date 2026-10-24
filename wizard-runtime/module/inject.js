// Bootstrap through JNI only: the high-level Java bridge is incompatible with this TV's ART.
Java.vm.perform(function(){
 const env=Java.vm.getEnv().handle,table=env.readPointer();
 const f=(i,r,a)=>new NativeFunction(table.add(i*Process.pointerSize).readPointer(),r,['pointer',...a]);
 const find=f(6,'pointer',['pointer']),objcls=f(31,'pointer',['pointer']);
 const mid=f(33,'pointer',['pointer','pointer','pointer']),smid=f(113,'pointer',['pointer','pointer','pointer']);
 const call=f(36,'pointer',['pointer','pointer','pointer']),scall=f(116,'pointer',['pointer','pointer','pointer']);
 const create=f(30,'pointer',['pointer','pointer','pointer']);
 const str=f(167,'pointer',['pointer']),chars=f(169,'pointer',['pointer','pointer']),release=f(170,'void',['pointer','pointer']);
 const exception=f(228,'uchar',[]),clear=f(17,'void',[]);
 const utf=s=>Memory.allocUtf8String(s),s=s=>str(env,utf(s));
 function check(stage){if(exception(env)){clear(env);throw Error('JNI failed: '+stage);}}
 function args(values){if(!values.length)return ptr(0);const p=Memory.alloc(values.length*8);values.forEach((v,i)=>{p.add(i*8).writeU64(0);if(typeof v==='number')p.add(i*8).writeS32(v);else p.add(i*8).writePointer(v);});return p;}
 function invoke(o,name,sig,values){const id=mid(env,objcls(env,o),utf(name),utf(sig));check(name);if(id.isNull())throw Error('Method unavailable: '+name);const out=call(env,o,id,args(values));check(name);return out;}
 const at=find(env,utf('android/app/ActivityThread'));check('ActivityThread');
 const app=scall(env,at,smid(env,at,utf('currentApplication'),utf('()Landroid/app/Application;')),ptr(0));check('currentApplication');
 if(app.isNull())throw Error('Application is not ready');
 const parent=invoke(app,'getClassLoader','()Ljava/lang/ClassLoader;',[]);
 const files=invoke(app,'getFilesDir','()Ljava/io/File;',[]);
 const pathString=invoke(files,'getAbsolutePath','()Ljava/lang/String;',[]);
 const pchars=chars(env,pathString,ptr(0)),path=pchars.readUtf8String();release(env,pathString,pchars);
 const dcl=find(env,utf('dalvik/system/DexClassLoader'));check('DexClassLoader');
 const ctor=mid(env,dcl,utf('<init>'),utf('(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/ClassLoader;)V'));check('constructor');
 const loader=create(env,dcl,ctor,args([s(path+'/hue-runtime-fix.dex'),ptr(0),ptr(0),parent]));check('create loader');
 const cls=invoke(loader,'loadClass','(Ljava/lang/String;)Ljava/lang/Class;',[s('local.tv.hue.runtime.RuntimeFix')]);
 const install=smid(env,cls,utf('install'),utf('(Landroid/app/Application;)Ljava/lang/String;'));check('install method');
 const result=scall(env,cls,install,args([app]));check('install');
 const text=chars(env,result,ptr(0));send({runtimeFix:text.readUtf8String()});release(env,result,text);
});

from pathlib import Path
import urllib.request, ssl, json, secrets, sys, base64, hmac, hashlib, os, subprocess

ROOT=Path(__file__).resolve().parent
STATE=ROOT/'jointspace-private.json'
BASE='https://'+os.environ.get('TV_HOST','192.0.2.10')+':1926/6/'
context=ssl._create_unverified_context()  # This TV exposes a self-signed local certificate.
def call(path,body=None,auth=False):
    handlers=[urllib.request.HTTPSHandler(context=context)]
    if auth:
        state=json.loads(STATE.read_text())
        config='user = '+json.dumps(state['device']['id']+':'+state['auth_key'])+'\n'
        args=['curl','--silent','--show-error','--insecure','--digest','--config','-','--max-time','12',BASE+path]
        if body is not None:args+=['--header','Content-Type: application/json','--data',json.dumps(body)]
        result=subprocess.run(args,input=config,text=True,capture_output=True,check=True)
        return json.loads(result.stdout) if result.stdout else {'empty_response':True}
    request=urllib.request.Request(BASE+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json'})
    with urllib.request.build_opener(*handlers).open(request,timeout=15) as response:
        raw=response.read()
        return json.loads(raw) if raw else {'http_status':response.status}

def save(state):
    fd=os.open(STATE,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
    with os.fdopen(fd,'w') as f: json.dump(state,f)

def main():
    action=sys.argv[1]
    if action in ('pair','renew'):
        if STATE.exists() and action=='pair':raise RuntimeError('Pair state already exists; inspect it before replacing.')
        device={'device_name':'Ambilight Hue manual control','device_os':'Android','app_name':'Ambilight Hue control','type':'native','app_id':'local.tv.hue','id':secrets.token_hex(8)}
        result=call('pair/request',{'scope':['read','write','control'],'device':device})
        if result.get('error_id')!='SUCCESS':raise RuntimeError('Pair request refused: '+str(result.get('error_id')))
        save({'device':device,**result})
        print('Pairing requested. Read the PIN displayed by the TV.')
    elif action=='grant':
        state=json.loads(STATE.read_text())
        key=base64.b64decode('JCqdN5AcnAHgJYseUn7ER5k3qgtemfUvMRghQpTfTZq7Cvv8EPQPqfz6dDxPQPSu4gKFPWkJGw32zyASgJkHwCjU')
        pin=sys.argv[2]
        signature=base64.b64encode(hmac.new(key,(str(state['timestamp'])+pin).encode(),hashlib.sha1).hexdigest().encode()).decode()
        body={'auth':{'auth_AppId':'1','pin':pin,'auth_timestamp':state['timestamp'],'auth_signature':signature},'device':state['device']}
        result=call('pair/grant',body,True)
        print(json.dumps(result))
    elif action in ('get','post'):
        result=call(sys.argv[2],None if action=='get' else json.loads(sys.argv[3]),True)
        target=ROOT/('api-'+sys.argv[2].replace('/','-')+'-'+action+'.json')
        target.write_text(json.dumps(result,indent=2,ensure_ascii=False))
        if sys.argv[2]=='HueLamp/power': print(json.dumps(result))
        else: print('Response saved:',target.name)
if __name__=='__main__':main()

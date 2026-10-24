from pathlib import Path
import argparse,json,hashlib,socket,ssl,subprocess,shlex,re,http.client

parser=argparse.ArgumentParser(description='Provision paired TV API credentials to the launcher using ADB shell permission.')
parser.add_argument('--serial',required=True)
parser.add_argument('--host',required=True,help='Reachable TV LAN IP from this computer, used only for first-time certificate discovery.')
parser.add_argument('--replace-certificate',action='store_true',help='Explicitly trust a changed TV certificate; use only after checking the target TV.')
parser.add_argument('--state',type=Path,default=Path(__file__).parent/'jointspace-private.json')
args=parser.parse_args()
state=json.loads(args.state.read_text())
status=subprocess.run(['adb','-s',args.serial,'shell','content call --uri content://local.tv.hue.tvapi --method status'],capture_output=True,text=True)
match=re.search(r'certificate=([a-f0-9]{64})',status.stdout)
if match and not args.replace_certificate:
    certificate=match.group(1)
else:
    client=http.client.HTTPSConnection(args.host,1926,timeout=10,context=ssl._create_unverified_context())
    try:
        client.connect();cert=client.sock.getpeercert(binary_form=True)
        client.request('GET','/6/system',headers={'Connection':'close'})
        response=client.getresponse();response.read()
        certificate=hashlib.sha256(cert).hexdigest()
    finally:client.close()
config={'user':state['device']['id'],'password':state['auth_key'],'certificate':certificate,'host':'127.0.0.1'}
command=['content','call','--uri','content://local.tv.hue.tvapi','--method','configure','--arg',json.dumps(config)]
result=subprocess.run(['adb','-s',args.serial,'shell',shlex.join(command)],capture_output=True,text=True)
if result.returncode or 'configured=true' not in result.stdout:
    raise SystemExit('Launcher provisioning failed. No credentials printed.')
print('Launcher credentials provisioned in private TV storage; certificate pinned; TV API host is 127.0.0.1.')

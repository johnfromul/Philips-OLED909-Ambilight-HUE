#!/usr/bin/env python3
"""Export private launcher diagnostics and allowlisted native events via ADB.
No power requests, service notifications, root or configuration changes.
Raw native logs are held in memory only: Hue URLs can contain secret keys.
"""
import argparse
from pathlib import Path
import re
import subprocess
from datetime import datetime

PATTERNS = [
    r"Current state:State[A-Za-z]+ Event state:Ev[A-Za-z]+ Next State:State[A-Za-z]+",
    r"INSTSETTINGSALPLUSHUEONOFF:\s*[01]\b",
    r"AMBICONFIG:\s*[0-9]+\b",
    r"no hue bridge found in mDNS discovery",
    r"start upnp hue bridge discovery with timeout of [0-9]+s",
    r"no hue bridge found in upnp discovery with timeout of [0-9]+s",
    r"check nupnp hue bridge discovery",
    r"Existing Mac Found in nupnp",
    r"(?<!no hue bridge )found in (?:mDNS|upnp|nupnp) discovery",
    r"Bridge is not free, force to relinquish Bridge to start new bridge discovery",
    r"fetchLastStatus ALPLUSHUEONOFF 0",
    r"arePrecoditionsMet\(\) (?:true|false) areAmbilightPropertiesConditionsMet\(\) (?:true|false) isConfigurationValid\(\) (?:true|false)",
    r"mPowerManager.isInteractive\(\) (?:true|false) INSTSETTINGSALPLUSHUEONOFF [01] mAlloungeplushue: [01]",
    r"(?:[a-zA-Z_$][a-zA-Z0-9_$]*\.)+[a-zA-Z_$][a-zA-Z0-9_$]*(?:Exception|Error)\b",
]

def native_events(raw):
    rows=[]
    for line in raw.splitlines():
        stamp=re.match(r"\s*(\d{10}\.\d+)",line)
        if not stamp: continue
        events=[]
        for pattern in PATTERNS:
            events.extend(m.group(0) for m in re.finditer(pattern,line))
        if events: rows.append(stamp.group(1)+" "+" | ".join(events))
    return "\n".join(rows)+"\n"

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--adb",default="adb")
    p.add_argument("--serial",required=True)
    p.add_argument("--output",type=Path,default=Path("hue-diagnostics-"+datetime.now().strftime("%Y%m%d-%H%M%S")))
    a=p.parse_args();a.output.mkdir(parents=True,exist_ok=False)
    def run(*args):
        value=subprocess.run([a.adb,"-s",a.serial,*args],capture_output=True,text=True,timeout=45)
        if value.returncode: raise RuntimeError("ADB command failed; check connection and authorization")
        return value.stdout
    # DUMP-protected provider: shell can export; ordinary apps cannot.
    launcher=run("shell","content","call","--uri","content://local.tv.hue.tvapi","--method","diagnostics")
    if "log=" not in launcher: raise RuntimeError("Diagnostics unavailable; install launcher 1.7 or later")
    (a.output/"launcher.txt").write_text(launcher)
    raw=run("shell","logcat","-d","-v","epoch","-s","ambilighthue:V")
    (a.output/"native-events.txt").write_text(native_events(raw))
    (a.output/"collection.txt").write_text("Collected "+datetime.now().astimezone().isoformat()+"\nNative log buffer is finite; missing events do not prove absence.\nNo power state was changed.\n")
    print("Saved diagnostics to",a.output.resolve())

if __name__=="__main__": main()

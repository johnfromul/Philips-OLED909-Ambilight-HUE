#!/system/bin/sh
MODDIR=${0%/*}
STATE=/data/user/0/org.droidtv.alhuewizard/files
APK=/system_ext/app/ambilighthue/ambilighthue.apk
EXPECTED=0e865164982b371e24496a543c3c79670a51e96f2d1cbc41c0e8011b2ff197f4
LOG="$MODDIR/runtime.log"
exec </dev/null
if [ -f "$MODDIR/watcher.pid" ]; then
 old_pid=$(cat "$MODDIR/watcher.pid")
 if [ -n "$old_pid" ] && kill -0 "$old_pid" 2>/dev/null; then
  old_command=$(tr '\000' ' ' < "/proc/$old_pid/cmdline" 2>/dev/null)
  case "$old_command" in *"$MODDIR/service.sh"*) exit 0;; esac
 fi
fi
echo $$ > "$MODDIR/watcher.pid"
trap 'rm -f "$MODDIR/watcher.pid"' EXIT
last_pid=
while [ ! -f "$MODDIR/disable" ] && [ ! -f "$MODDIR/remove" ]; do
 if [ -f "$STATE/hue-runtime.disabled" ]; then sleep 2; continue; fi
 target_pid=$(pidof org.droidtv.ambihue)
 target_pid=${target_pid%% *}
 if [ -n "$target_pid" ] && [ "$target_pid" != "$last_pid" ]; then
  checksum=$(sha256sum "$APK" 2>/dev/null)
  checksum=${checksum%% *}
  if [ "$checksum" != "$EXPECTED" ]; then echo "APK mismatch: injection refused" >> "$LOG"; exit 1; fi
  target_uid=$(stat -c %u /data/user/0/org.droidtv.alhuewizard)
  if [ -n "$target_uid" ] && [ -d /data/user/0/org.droidtv.alhuewizard ]; then
   if [ ! -d "$STATE" ]; then
    mkdir -p "$STATE"
    chown "$target_uid:$target_uid" "$STATE"
    chmod 700 "$STATE"
    restorecon "$STATE" >/dev/null 2>&1
   fi
   cp "$MODDIR/hue-runtime-fix.dex" "$STATE/hue-runtime-fix.dex"
   chown "$target_uid:$target_uid" "$STATE/hue-runtime-fix.dex"
   chmod 444 "$STATE/hue-runtime-fix.dex"
   restorecon "$STATE/hue-runtime-fix.dex" >/dev/null 2>&1
   sleep 1
   echo "Attaching Hue runtime fix to PID $target_pid" >> "$LOG"
   if timeout 15 "$MODDIR/bin/frida-inject" -p "$target_pid" -s "$MODDIR/inject.js" -e >> "$LOG" 2>&1; then
    last_pid=$target_pid
   else
    echo "Injection failed; backing off" >> "$LOG";sleep 10
   fi
  fi
 fi
 sleep 2
done

#!/system/bin/sh
MODDIR=${0%/*}
STATE=/data/user/0/org.droidtv.alhuewizard/files
# The in-process helper observes this marker and restores the original callbacks.
touch "$STATE/hue-runtime.disabled"
chmod 644 "$STATE/hue-runtime.disabled"
restorecon "$STATE/hue-runtime.disabled" >/dev/null 2>&1
touch "$MODDIR/disable"
echo "Hue runtime fix disabled; original callbacks restored within one second."

# Sourced by the standard Magisk app installer.
ui_print "- Philips OLED909 Hue Wizard Runtime Fix 1.1"
APK=/system_ext/app/ambilighthue/ambilighthue.apk
EXPECTED=0e865164982b371e24496a543c3c79670a51e96f2d1cbc41c0e8011b2ff197f4
[ -f "$APK" ] || abort "Native Philips Hue APK not found. Unsupported TV."
ACTUAL=$(sha256sum "$APK")
ACTUAL=${ACTUAL%% *}
[ "$ACTUAL" = "$EXPECTED" ] || abort "Native Hue APK differs from tested version. Installation stopped."
[ -x /system/bin/app_process32 ] || abort "Required ARM32 Android runtime not found."
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/bin/frida-inject" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/stop.sh" 0 0 0755
set_perm "$MODPATH/hue-runtime-fix.dex" 0 0 0444
# Explicitly reinstalling the module re-enables a previously stopped helper.
rm -f /data/user/0/org.droidtv.alhuewizard/files/hue-runtime.disabled
ui_print "- Original APK and Philips signature will remain unchanged."
ui_print "- Root / Magisk are required."
ui_print "- Reboot after installation to activate the service."
ui_print "- Pair your bridge and configure lights again after a wipe."

# Ambilight + Hue 1.6 — local TV API connection

Fixes On/Off failing with EHOSTUNREACH / No route to host after a TV LAN or VPN address changes.

- The Android launcher always uses `127.0.0.1:1926`, including upgrades with obsolete saved host values.
- Keeps certificate pinning, Digest authentication, native persistent On/Off and native light restoration.
- Updated provisioning distinguishes the external TV address used by the computer from loopback used by the Android app.
- Adds explicit certificate replacement for deliberate recovery and documents fresh API pairing after a factory reset.
- Uses the original signing identity; install with `adb install -r` to preserve valid current launcher data.

**After a wipe:** fresh TV API pairing is required if old credentials were invalidated. This is separate from Hue Bridge pairing. Turn on the TV's own Ambilight before enabling Hue synchronization.

**Optional rooted-TV component:** the separate Magisk ZIP repairs remote OK/Enter and completion in the native Philips wizard without replacing its APK. The launcher APK does not install that module and does not require root. See `docs/WIZARD-RUNTIME.md` for compatibility, installation and untested boot/wipe limitations.

Tested TV: Philips 65OLED909, firmware TPM231WW_R.201.100.146.220 / build 3000428, kernel 5.15.180-android14-11-00042-g766efc243647-dirty. App UI: Czech; repository documentation: English.

Downloads include the signed APK, an English distribution bundle, a source archive, checksums and the optional Magisk module. No personal pairing credentials or private signing key are included. Version 1.5 remains available in its earlier release.

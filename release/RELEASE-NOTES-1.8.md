# Ambilight + Hue 1.8 — reliable command delivery and verification

Fixes the reproduced SocketTimeoutException during verification after enabling Hue. The TV acknowledged separate header/body writes without applying the command and then stopped answering the next request on the reused connection. This version writes the complete request together and performs verification on a fresh pinned TLS connection. Native service notification no longer depends on successful verification. Bounded retries cover transient failures and an unapplied state; permanent authentication/certificate failures remain errors.

Retains the 1.7 private diagnostic logs and safe ADB export. Install with `adb install -r` to preserve current pairing. The TV's own Ambilight must be On. A confirmed saved switch is not an independent optical check of lamp response.

Validated on the existing Philips OLED909: Off 187 ms, On 166 ms, saved values verified and native HueIsActive true. Eight test methods passed. No reboot or wipe performed. The optional Magisk wizard repair is unchanged and is not part of the APK.

# Version 1.8 validation — October 27, 2026

- Captured incident 400d6aa2: POST returned 200 after about 2.18 s, same-session verification GET timed out after 9 s; fresh-session follow-up checks consistently returned Off.
- A first recovery build removed same-session verification and delivered native intents, but two POSTs still left the setting Off. Philips prerequisites for Ambilight and bridge configuration were true; the saved switch was the missing condition.
- A direct alternate-client request changed the saved switch to On. Changing the production Java writer from separate header/body writes to one combined write then fixed actual Off and On transitions: 187 ms and 166 ms respectively. Both verified the desired value on the first fresh-session GET.
- Native service entered StatePostLamp and HueIsActive was true after the final On. This is native telemetry; no independent optical lamp measurement was performed. Native bridge discovery/reconnection delays remain outside the launcher's switch-setting fix.
- A later unrelated pair of API POSTs returned the switch to Off 36 seconds after the first successful On; the launcher recorded no Off request then. The sender is not established. Home Assistant is another known API client, but discovery or a stale socket alone does not prove it sent those commands.
- Repeated On using the final APK completed in 157 ms. Follow-up checks at 2, 10, 30 and 60 seconds all remained On; native HueIsActive was true at the final check.
- Eight test methods passed. Recovery scenarios cover a verification timeout, an unapplied command, ambiguous POST timeout, Off, permanent rejection, certificate rejection, bounded failure and read rejection. A JVM regression test executes the production HTTP writer and checks complete header/body writes on both the initial request and Digest retry. Existing diagnostic redaction/rotation and provisioning tests remain passing.
- Built and signature-verified versionName 1.8 / versionCode 9 using the original signing identity. Installed using install -r, preserving pairing. No reboot, data clear, native APK replacement or wizard configuration change.

# Version 1.6 validation — October 24, 2026

- Four focused provisioning tests passed: preserve an existing pin without network discovery, discover a certificate at the remote TV address while provisioning loopback, deliberately replace a pin, and report rejected configuration without exposing credentials.
- The APK built successfully against platform 36 with build-tools 36.1.0. Its signing certificate matches the original 1.5 APK; versionName is 1.6 and versionCode is 7.
- Upgraded the tested OLED909 using install -r, preserving its current valid API pairing.
- Upgrade regression test deliberately retained the obsolete saved LAN/VPN host. Version 1.6 ignored it and successfully switched native Hue Off over 127.0.0.1. POST and verification GET returned HTTP 200, the app displayed native switch Off, and HueIsActive became false.
- The revised provisioner was also run on the real TV using its externally reachable address; app status then confirmed effective host 127.0.0.1 and the preserved certificate pin.
- Synchronization was not switched On for release validation. No TV reboot or wipe was performed. No fresh throughput or long-duration stability test was run.
- The optional module retains the tested runtime Java DEX from the October 24 wizard repair. Its Magisk ZIP packaging was checked; actual installation through Magisk after a wipe and full-TV boot remain untested.
- The separate earlier API recovery confirmed that pre-wipe credentials can be invalid even after correcting an address. Fresh PIN pairing resolved that issue; APK 1.6 does not bypass authentication.

# Publication validation — October 23, 2026

- All four Java files match the verified v1.5 source files byte for byte.
- The published APK matches the original v1.5 build artifact byte for byte.
- Android apksigner verified the release APK (v1, v2 and v3 signatures).
- The portable build script successfully compiled, packaged, signed and verified a local build with SDK platform 36 and build-tools 36.1.0.
- Python helpers passed syntax checks; provisioning help exposes explicit host and ADB serial arguments.
- The distribution ZIP passed its integrity check. Private TV pairing values were checked against publication contents and nested archives without disclosing them; none were found.
- The existing October 21 device validation and October 22 user-confirmed Ambilight requirement are documented separately. No TV installation, lighting change or restart was performed for publication.

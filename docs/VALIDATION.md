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

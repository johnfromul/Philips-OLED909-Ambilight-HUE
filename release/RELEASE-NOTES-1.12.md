# Ambilight + Hue 1.12

This release refines the rootless setup flow introduced in 1.11 and preserves unfinished light configuration.

Open **Set up Bridge and bulbs** and follow:

1. **Find bridge on network** — or enter its IPv4 address.
2. **Pair Hue Bridge** — press its physical button when the yellow, bold prompt appears. Press OK on the success dialog to return to the menu.
3. **App pairing** — grant the Ambilight + Hue pairing Accessibility helper permission, then return. You do not need to press App pairing again after granting permission.
4. **Load compatible lights** — select lights, choose positions on the graphical TV diagram, and set brightness.
5. **Save and start synchronization** — the app automatically completes TV PIN pairing if needed, then saves native configuration and starts the Philips service. The final dialog reports successful pairing when newly performed, plus successful configuration. Keep TV Ambilight on and verify the light response.

Both network discovery and waiting for the physical Bridge button now show a countdown of up to 120 seconds. Discovery stops early when a reachable candidate is found. Editing drafts preserve selected lights, positions and brightness between visits; native configuration changes supersede stale drafts. Back returns from light settings to the setup menu. The redundant Bridge connection button was removed.

All new labels and messages support Czech, English, German, French and Spanish using the TV language, with English fallback. The helper can be disabled after successful TV pairing; normal synchronization does not require it. Original Philips wizard access and its optional separate root repair remain available. The in-app setup does not require the original wizard or root on the tested TV.

Validation: the owner confirmed the complete clean setup and automatic TV pairing. Native logs recorded successful grant, two-lamp configuration commit and On verification. Earlier red/blue screen tests confirmed matching lamp changes. Nine focused tests pass, including localization and transport recovery checks. All rollback failure modes have not been exhaustively tested.

Tested: Philips 65OLED909, firmware TPM231WW_R.201.100.146.220 (build 3000428), kernel 5.15.180-android14-11-00042-g766efc243647-dirty, Hue Bridge V2. OLED808, other firmware and Hue Bridge Pro are unverified. Android API 26 or newer and the original compatible Philips services are required.

VersionCode 13. The original signing identity is retained for in-place updates. Attached: APK, source archive, documentation bundle and SHA256 checksums. No private credentials, device backups or signing keys are included.

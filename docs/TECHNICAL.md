# Technical notes and verification history

## Tested TV software

- TV: Philips **65OLED909**, Android 14.
- Firmware: **`TPM231WW_R.201.100.146.220`**, build **`3000428`**.
- Kernel: **`5.15.180-android14-11-00042-g766efc243647-dirty`**.

These versions come from the saved TV diagnostics for the tested setup. Compatibility with other firmware or kernel versions has not been verified.

## Native integration

The OLED909 firmware contains `org.droidtv.alhuewizard`, although the regular settings menu does not expose the original Ambilight + Hue integration. The launcher delegates to the installed service; it does not embed a replacement synchronization engine.

Version 1.5 sends an authenticated Digest request to `https://TV_HOST:1926/6/HueLamp/power` with `{"power":"On"}` or `{"power":"Off"}`. A GET to the same endpoint verifies the saved value. The Android client uses one TLS 1.2 connection for challenge handling, POST and verification GET, and validates the leaf certificate's SHA-256 fingerprint.

It then starts the native service with `org.droidtv.ambihue.HueService.ALHUEON` or `org.droidtv.ambihue.HueService.ALHUEOFF`. On the tested TV, changing the saved setting alone did not wake the native service after restart. Service-only On failed while the saved setting remained Off.

Native power corresponds to ITvSettingsManager ID 120 / ACFG 16385. The runtime indicator is `odm.tpv.sys.HueIsActive`. The native status activity is `org.droidtv.alhuewizard/org.droidtv.ambihue.AmbiHueDialogActivity`.

## Light selection

The launcher reads the TV's paired bridge identity, pairs its own local Hue API access using the physical bridge button, and offers `Color light` / `Extended color light` devices. It writes the chosen IDs to `content://org.droidtv.ambihue.provider/HueLampsInfo`, retaining existing position and brightness settings. Native bridge pairing and the native configuration wizard remain in use.

## Verified on October 21, 2026

- Signed v1.5 APK installed successfully, versionCode 6.
- Consecutive Off → On → Off → On → Off operations completed using the final client.
- POST and verification GET returned HTTP 200 during those checks.
- Runtime state changed to true after On and false after Off.
- The native status dialog independently reported `Pixel enable: 0 alhueonoff 0` after the final Off.
- A prior authorized standby/wake test preserved manual Off. This does not establish automatic Off when entering standby while On.
- The final build was left with native Off. A new full restart after final verification and long-duration stability were not tested.

## October 22, 2026 incident

The launcher reported the native switch enabled, but `HueIsActive` was false. Native logs reported prerequisites true, Ambilight conditions false, and configuration valid. The user confirmed that the TV's own Ambilight was off; switching it on restored Hue synchronization. A USB LAN/VPN setup was present, but did not prevent successful operation after that change. No reliable event showing when or why Ambilight had turned off was recovered.

## Historical limitations

An experimental `hue-acfg` tool printed successful writes but actually failed to connect to the settings IPC service under SELinux. Its output is not evidence of a successful settings write. Do not use that method. No SELinux or firmware changes were made.

The original wizard did not reliably react to OK on selection checkboxes or Done. Direct database selection and the native position configuration were used. The exact action that originally exposed the native configuration menu was not established reliably; there is no verified universal command for bypassing its initial Done screen.

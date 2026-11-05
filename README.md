# Philips OLED909 Ambilight + HUE

Manual Ambilight + Hue control for the **Philips 65OLED909**, using the original Philips Hue service already included in the TV firmware.

[Download version 1.11](https://github.com/johnfromul/Philips-OLED909-Ambilight-HUE/releases/tag/v1.11) · [Installation and pairing](docs/INSTALL.md) · [Technical notes](docs/TECHNICAL.md) · [Build from source](docs/BUILD.md)

![Ambilight + Hue artwork](artwork/ambilight-hue-approved.png)

## What it does

- Turns synchronization on and off from a TV remote friendly launcher.
- Pairs the launcher with the TV directly on the TV, without computer-side scripts or root. First enable the optional pairing helper in Accessibility settings.
- Saves the native Hue On/Off setting through the paired JointSPACE API, then notifies the native Philips service.
- Restores the previous light state when synchronization is turned off, using Philips' own restoration mechanism.
- Pairs the Hue Bridge, selects up to ten color lights and sets their position and brightness directly in the app, without root or the native wizard.
- Passes both Bridge credentials to the native Philips service and starts synchronization. Keeps a private recovery snapshot during configuration changes.
- Retains the original Philips wizard as an optional separate control.

**The TV's own Ambilight must be switched on.** On October 22, 2026, Hue synchronization appeared enabled but did not run while Ambilight was off. Turning Ambilight on restored operation. The USB LAN setup did not prevent operation in that test; the reason Ambilight had switched off could not be determined.

## Tested setup

| Component | Verified value |
| --- | --- |
| TV | Philips 65OLED909 |
| Firmware | `TPM231WW_R.201.100.146.220` (build `3000428`) |
| Kernel | `5.15.180-android14-11-00042-g766efc243647-dirty` |
| Release | 1.11, Android versionCode 12 |
| Android package | `local.tv.hue` |
| Native service | `org.droidtv.alhuewizard/org.droidtv.ambihue.HueService` |
| TV API | HTTPS, port 1926, JointSPACE v6 |
| Compatible lights | `Color light` and `Extended color light`, up to 10 |

Other models and firmware versions have not been verified. White ambiance lights are intentionally excluded, matching the native application's color-light requirement. Two color lights were configured and tested.

## App controls

The launcher follows the TV system language for Czech, English, German, French and Spanish. Other system languages fall back to English. The native Philips wizard opened by the launcher remains a separate TV component and uses its own language support.

| App label | Meaning |
| --- | --- |
| Turn on synchronization | Turn synchronization on |
| Turn off synchronization | Turn synchronization off and restore previous lighting |
| Set up Bridge and bulbs | Pair Bridge, select lights, set positions and brightness, save and start |
| Original Philips wizard | Open the optional native wizard |
| App pairing | Authorize the launcher with the TV API using the on-screen PIN helper |

The success message confirms the saved native switch and service request. It does **not** independently confirm that lights are responding.

## Added in 1.10

- Choose **App pairing** to authorize the launcher directly on the TV. On first use, enable **Ambilight + Hue pairing** in Accessibility settings, return to the launcher, and choose App pairing again.
- Philips displays a system PIN prompt that blocks ordinary app input. The helper reads that specific prompt and completes authorization while it remains open. Do not close the prompt yourself.
- The helper processes the PIN only during an explicit pairing attempt. PINs and credentials are not logged. After pairing, the helper can be disabled; normal On/Off control does not require Accessibility.
- The app verifies authenticated TV API access before replacing existing credentials. Updating from 1.9 preserves existing pairing; already paired users do not need to pair again.
- Verified on the owner's OLED909 on November 4, 2026: grant returned `SUCCESS`, followed by an authenticated `HueLamp/power` read with HTTP 200. This test verifies API authorization, not a new full native-wizard or optical synchronization test.

## Added in 1.9

- Localize every launcher label, action, progress and selection-screen message from the TV system language: Czech, English, German, French and Spanish; other languages use English.
- The previous hard-coded Czech label **Konfigurovat žárovky** is now **Configure bulbs** on an English TV.
- The native Philips wizard is not part of this APK. Hue Bridge Pro compatibility has not been verified: the launcher and native wizard were tested only with Hue Bridge V2. Do not claim Bridge Pro support until it has been tested on a compatible TV firmware.
- Version 1.9 retains the original Hue launcher signing identity and can be installed as an in-place update over release 1.8 with `adb install -r`. Existing TV API pairing is preserved.

## Fixed in 1.8

Send HTTP headers and the command body in one socket write. On the tested TV, separate writes caused HTTP 200 acknowledgements without applying the command. Verify the switch on a new authenticated TLS connection, notify the native service before verification can time out, and retry a transient read or one unapplied command within a bounded recovery sequence. Authentication and certificate failures remain errors. Actual light synchronization is still independently controlled by Philips' service.

## Added in 1.7

Persistent, rotating diagnostics correlate button requests, API stages, failures and read-only follow-up checks. Success messages distinguish the saved switch from actual light synchronization. See [Diagnostics](docs/DIAGNOSTICS.md) for safe ADB export and limitations.

## Fixed in 1.6

The Android launcher now always connects to its own TV through **`127.0.0.1:1926`**. It ignores obsolete saved LAN/VPN addresses, so changing the TV IP no longer breaks the On/Off buttons. Certificate pinning, Digest authentication and native service notifications remain in place.

A factory reset can invalidate old API credentials independently of the IP address. After a wipe, complete fresh **TV API pairing and provisioning** as described in the installation guide. Updating the APK preserves current private credentials; it cannot make invalid pre-wipe credentials valid again.

An optional **root-only Magisk module** repairs remote confirmation in the original Philips wizard. It is separate from the launcher APK; see [Native wizard runtime fix](docs/WIZARD-RUNTIME.md). The launcher itself does not require root.

## Installation overview

1. Enable Android developer options and ADB debugging on the TV.
2. Install the latest APK.
3. Pair the native Philips application with your Hue Bridge.
4. Choose **App pairing**, enable the pairing helper in Accessibility when prompted, return and choose **App pairing** again. The original computer-side tools remain available as an alternative.
5. Select color lights, configure their positions, enable Ambilight, and start synchronization.

See [the full installation guide](docs/INSTALL.md) for commands, recovery steps and certificate handling.

## Limitations

- Automatic shutdown of synchronization when the TV enters standby is not implemented. If synchronization is On at standby, it may resume on wake.
- The most recent manual Off persisted through a tested standby/wake cycle.
- The saved switch and native active indicator do not independently prove optical lamp response. Version 1.8 passed real Off/On checks and a one-minute follow-up; long-term stability and a subsequent full reboot were not verified.
- No firmware or SELinux modifications are required by this launcher.
- TV API pairing does not repair the native Philips wizard. Some TVs have non-working bulb selection/confirmation; the separate runtime repair requires root. Bridge Pro remains unverified. A reported multi-bridge search stalled while reading the second bridge, even though mDNS had found both; disconnecting that bridge allowed V2 discovery to complete.

## Repository contents

`src/` contains the Android Java sources; `res/`, `artwork/` and `AndroidManifest.xml` contain the app assets and metadata. `tools/` provides pairing and provisioning helpers. `docs/` includes installation, build instructions, technical findings and historical ADB recovery notes. `release/` contains the signed APKs, English distribution bundles and checksums.

No personal TV/Hue credentials, private signing keys, raw device logs, extracted Philips applications or decompiled vendor sources are included.

## Project status and attribution

This is an unofficial personal compatibility project, not a Philips or Signify product. Philips, Ambilight and Hue names and associated marks belong to their respective owners. No open-source license has been selected for this publication; public availability alone is not a grant of reuse rights. The private signing key is not distributed.

## Version 1.11: setup without the native wizard

Complete **App pairing**, then **Set up Bridge and bulbs**. Discover the Bridge or enter its IPv4 address, press its physical button and choose Pair. Select lights and configure positions/brightness, then choose **Save and start synchronization**. The original native wizard and Magisk repair are not needed for this setup path on the tested TV.

A clean-state prototype was tested on the owner's OLED909 with native Bridge/lamp records cleared, both Hue flags reset and the root wizard repair disabled. The ordinary APK obtained fresh Bridge credentials, configured two lamps and started the original Philips service. The service set its own configuration flag; red and blue screen changes produced matching changes in both lights, confirmed visually and through Bridge reads. Root was used only to prepare/backup/reset the test and read resulting system flags. This does not establish compatibility with OLED808, different firmware or Hue Bridge Pro. The final 1.11 interface loaded existing native credentials, displayed positions/brightness, saved two lamps and started the native service on the TV; recovery from every possible partial failure has not been exhaustively verified.

Android 8 (API 26) or later is required. The Philips native service and its exported interfaces must exist. No private credentials, TV data archives or signing key are included.

The position control shows a TV diagram with ten selectable locations, matching the native zone order. Remote arrows select a location; OK confirms and Back cancels. Labels and guidance follow the TV language.

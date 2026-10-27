# Philips OLED909 Ambilight + HUE

Manual Ambilight + Hue control for the **Philips 65OLED909**, using the original Philips Hue service already included in the TV firmware.

[Download version 1.8](https://github.com/johnfromul/Philips-OLED909-Ambilight-HUE/releases/tag/v1.8) · [Installation and pairing](docs/INSTALL.md) · [Technical notes](docs/TECHNICAL.md) · [Build from source](docs/BUILD.md)

![Ambilight + Hue artwork](artwork/ambilight-hue-approved.png)

## What it does

- Turns synchronization on and off from a TV remote friendly launcher.
- Saves the native Hue On/Off setting through the paired JointSPACE API, then notifies the native Philips service.
- Restores the previous light state when synchronization is turned off, using Philips' own restoration mechanism.
- Opens the native bulb position and brightness configuration wizard.
- Lists compatible color lights from the paired Hue Bridge and saves the selected lights in the native TV database, preserving existing position and brightness values.

**The TV's own Ambilight must be switched on.** On October 22, 2026, Hue synchronization appeared enabled but did not run while Ambilight was off. Turning Ambilight on restored operation. The USB LAN setup did not prevent operation in that test; the reason Ambilight had switched off could not be determined.

## Tested setup

| Component | Verified value |
| --- | --- |
| TV | Philips 65OLED909 |
| Firmware | `TPM231WW_R.201.100.146.220` (build `3000428`) |
| Kernel | `5.15.180-android14-11-00042-g766efc243647-dirty` |
| Release | 1.8, Android versionCode 9 |
| Android package | `local.tv.hue` |
| Native service | `org.droidtv.alhuewizard/org.droidtv.ambihue.HueService` |
| TV API | HTTPS, port 1926, JointSPACE v6 |
| Compatible lights | `Color light` and `Extended color light`, up to 10 |

Other models and firmware versions have not been verified. White ambiance lights are intentionally excluded, matching the native application's color-light requirement. Two color lights were configured and tested.

## App controls

The v1.8 APK has a **Czech interface**. This repository's documentation is in English.

| App label | Meaning |
| --- | --- |
| Zapnout synchronizaci | Turn synchronization on |
| Vypnout synchronizaci | Turn synchronization off and restore previous lighting |
| Konfigurovat žárovky | Open the native bulb configuration wizard |
| Přidání HUE světel do DB | Select compatible lights for the TV database |

The success message confirms the saved native switch and service request. It does **not** independently confirm that lights are responding.

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
4. Pair the launcher with the TV API using the included computer-side tools.
5. Select color lights, configure their positions, enable Ambilight, and start synchronization.

See [the full installation guide](docs/INSTALL.md) for commands, recovery steps and certificate handling.

## Limitations

- Automatic shutdown of synchronization when the TV enters standby is not implemented. If synchronization is On at standby, it may resume on wake.
- The most recent manual Off persisted through a tested standby/wake cycle.
- The saved switch and native active indicator do not independently prove optical lamp response. Version 1.8 passed real Off/On checks and a one-minute follow-up; long-term stability and a subsequent full reboot were not verified.
- No firmware or SELinux modifications are required by this launcher.

## Repository contents

`src/` contains the Android Java sources; `res/`, `artwork/` and `AndroidManifest.xml` contain the app assets and metadata. `tools/` provides pairing and provisioning helpers. `docs/` includes installation, build instructions, technical findings and historical ADB recovery notes. `release/` contains the signed APKs, English distribution bundles and checksums.

No personal TV/Hue credentials, private signing keys, raw device logs, extracted Philips applications or decompiled vendor sources are included.

## Project status and attribution

This is an unofficial personal compatibility project, not a Philips or Signify product. Philips, Ambilight and Hue names and associated marks belong to their respective owners. No open-source license has been selected for this publication; public availability alone is not a grant of reuse rights. The prebuilt APK retains the original local signing identity; the private signing key is not distributed.

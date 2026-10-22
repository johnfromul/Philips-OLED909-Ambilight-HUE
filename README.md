# Philips OLED909 Ambilight + HUE

Manual Ambilight + Hue control for the **Philips 65OLED909**, using the original Philips Hue service already included in the TV firmware.

[Download version 1.5](https://github.com/johnfromul/Philips-OLED909-Ambilight-HUE/releases/tag/v1.5) · [Installation and pairing](docs/INSTALL.md) · [Technical notes](docs/TECHNICAL.md) · [Build from source](docs/BUILD.md)

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
| Release | 1.5, Android versionCode 6 |
| Android package | `local.tv.hue` |
| Native service | `org.droidtv.alhuewizard/org.droidtv.ambihue.HueService` |
| TV API | HTTPS, port 1926, JointSPACE v6 |
| Compatible lights | `Color light` and `Extended color light`, up to 10 |

Other models and firmware versions have not been verified. White ambiance lights are intentionally excluded, matching the native application's color-light requirement. Two color lights were configured and tested.

## App controls

The verified v1.5 APK has a **Czech interface**. This repository's documentation is in English.

| App label | Meaning |
| --- | --- |
| Zapnout synchronizaci | Turn synchronization on |
| Vypnout synchronizaci | Turn synchronization off and restore previous lighting |
| Konfigurovat žárovky | Open the native bulb configuration wizard |
| Přidání HUE světel do DB | Select compatible lights for the TV database |

The success message confirms the saved native switch and service request. It does **not** independently confirm that lights are responding.

## Installation overview

1. Enable Android developer options and ADB debugging on the TV.
2. Install the APK from the v1.5 release.
3. Pair the native Philips application with your Hue Bridge.
4. Pair the launcher with the TV API using the included computer-side tools.
5. Select color lights, configure their positions, enable Ambilight, and start synchronization.

See [the full installation guide](docs/INSTALL.md) for commands, recovery steps and certificate handling.

## Limitations

- Automatic shutdown of synchronization when the TV enters standby is not implemented. If synchronization is On at standby, it may resume on wake.
- The most recent manual Off persisted through a tested standby/wake cycle.
- Earlier experimental clients using multiple TLS connections left the TV API unresponsive. The final client uses one TLS connection for the Digest challenge, POST and verification GET. Several consecutive changes passed, but long-term stability and a subsequent full reboot were not verified.
- No firmware or SELinux modifications are required by this launcher.

## Repository contents

`src/` contains the four Java classes; `res/`, `artwork/` and `AndroidManifest.xml` contain the app assets and metadata. `tools/` provides pairing and provisioning helpers. `docs/` includes installation, build instructions, technical findings and historical ADB recovery notes. `release/` contains the original signed APK, an English distribution bundle and checksums.

No personal TV/Hue credentials, private signing keys, raw device logs, extracted Philips applications or decompiled vendor sources are included.

## Project status and attribution

This is an unofficial personal compatibility project, not a Philips or Signify product. Philips, Ambilight and Hue names and associated marks belong to their respective owners. No open-source license has been selected for this publication; public availability alone is not a grant of reuse rights. The prebuilt APK retains the original local signing identity; the private signing key is not distributed.

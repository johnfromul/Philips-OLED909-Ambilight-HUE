# Philips TPM231WW Ambilight + Hue — Android TV / Google TV

Ambilight + Hue setup and control for compatible **Philips TPM231WW TVs running Android TV / Google TV**, using the original Philips service included in the firmware. **Titan OS is not supported.** The platform name and model year alone do not establish compatibility.

[Download version 1.12](https://github.com/johnfromul/Philips-TPM231WW-Ambilight-HUE-Android-TV/releases/tag/v1.12) · [Installation and pairing](docs/INSTALL.md) · [Technical notes](docs/TECHNICAL.md) · [Build from source](docs/BUILD.md)

![Ambilight + Hue artwork](artwork/ambilight-hue-approved.png)

## Current development build: 1.13

The latest published stable release remains 1.12. The 1.13 development build includes the multilingual PIN fix confirmed by the OLED808 tester, multi-Bridge selection, saved-status indicators, Advanced options and a confirmed local configuration reset. It is installed on the owner’s TV for evaluation; a 1.13 release has not yet been published.

## What it does

- Turns synchronization on and off from a TV remote friendly launcher.
- Prepares TV API pairing through an explicitly enabled Accessibility helper, then completes pairing automatically when saving light configuration. No computer or root is needed.
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
| Release | 1.12, Android versionCode 13 |
| Android package | `local.tv.hue` |
| Native service | `org.droidtv.alhuewizard/org.droidtv.ambihue.HueService` |
| TV API | HTTPS, port 1926, JointSPACE v6 |
| Compatible lights | `Color light` and `Extended color light`, up to 10 |

The OLED808 tester has additionally confirmed the rootless setup and synchronization on TPM231WW_R.201.100.228.212, including restore on standby and resume on wake. See [Compatibility](docs/COMPATIBILITY.md). Other models and firmware versions require verification. White ambiance lights are intentionally excluded, matching the native application's color-light requirement. Two color lights were configured and tested.

## App controls

The launcher follows the TV system language for Czech, English, German, French and Spanish. Other system languages fall back to English. The native Philips wizard opened by the launcher remains a separate TV component and uses its own language support.

| App label | Meaning |
| --- | --- |
| Turn on synchronization | Turn synchronization on |
| Turn off synchronization | Turn synchronization off and restore previous lighting |
| Set up Bridge and bulbs | Pair Bridge, select lights, set positions and brightness, save and start |
| Advanced options | Original Philips wizard, confirmed settings reset and authorization identity information |
| App pairing (inside setup) | Prepare Accessibility permission; TV authorization completes when saving lights |

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

## Version 1.12: chronological setup on the TV

Open **Set up Bridge and bulbs** and follow **Find bridge → Pair Hue Bridge → App pairing → Load compatible lights**. Grant the pairing helper's Accessibility permission, return, select lights and set their position/brightness, then choose **Save and start synchronization**. The app completes TV PIN pairing automatically at this final step if needed. The original wizard is optional. Discovery and Bridge-button waiting show 120-second countdowns; the button prompt is yellow and bold.

The graphical position selector mirrors the native ten-zone order using an original TV drawing. Remote arrows select, OK confirms and Back cancels. Private drafts preserve unfinished light selection, position and brightness between visits. Confirmed native saves and a private interrupted-save recovery mechanism are retained.

The owner successfully tested the complete clean setup and automatic TV pairing on the OLED909. Initial clean testing reset native Bridge/lamp records and both Hue settings with the root wizard repair disabled. An ordinary app obtained fresh Bridge credentials and the native service set its configuration flag. Red/blue screen changes produced matching changes in both lights, confirmed visually and through Bridge reads. Root was used only for test preparation, backup/reset and reading resulting system flags. Production setup does not require root. Recovery from every possible partial failure is not exhaustively verified.

Android API 26 or newer and compatible original Philips components are required. OLED808, different firmware and Hue Bridge Pro remain unverified. No private credentials, device backups or signing key are distributed.

## Development 1.13 controls

- Discovery collects Bridges for up to 120 seconds. Choose a Bridge by advertised/configured name, IP and identifier in the dropdown, or enter an address manually. mDNS-only candidates can be listed even when their local API is not yet verified; listing a Bridge Pro does not establish pairing or synchronization support.
- Bridge identity is yellow; pairing checkmarks are green and missing authorizations have a red cross. The verified native On state is green, Off red, and unknown states remain neutral. Main-screen checkmarks reflect the authenticated native On/Off switch. Separate status text shows saved TV authorization, configured Bridge identity and the native service indicator where readable. The service indicator may be unavailable to ordinary apps; unverified is shown instead of assuming activity. Optical synchronization still requires visual confirmation.
- Setup separately indicates stored Bridge authorization, Accessibility helper readiness and stored TV API authorization. These saved indicators do not guarantee current network reachability.
- Advanced options contains the original Philips wizard, a reset confirmation, and information about the local authorization name ambilight_hue#tv_setup. The cloud account label alone cannot establish which authorization is safe to remove.
- Reset clears native Bridge credentials/records, lamp configuration, drafts and this app’s TV API credentials after stopping synchronization. Existing authorizations on the Bridge remain. It does not factory-reset the Bridge, revoke unrelated credentials or reset other TV features. The Accessibility helper can be disabled separately. A native configuration reset requires working TV API authorization; an already-empty native setup can clear its local drafts without it.

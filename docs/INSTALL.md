# Installation and pairing

## Requirements

A compatible Philips TV with its original Ambilight + Hue service and a reachable Hue Bridge with color lights. The tested firmware and release are listed in the README. Version 1.10 can pair with the TV entirely on the TV, using its Accessibility pairing helper. Python 3 and curl are needed only for the optional computer-side pairing method. Android platform-tools (`adb`) are needed for the example installation commands below; an APK may also be installed using your usual TV sideloading method.

Use your own TV IP and current ADB port in all examples. `TV_SERIAL` is the full ADB device identifier; the pairing port and connection port may differ.

```sh
export TV_HOST=192.0.2.10   # Replace with your TV's reachable IPv4 address
export TV_SERIAL="$TV_HOST:5555"  # Replace with its current ADB connection port
adb pair "$TV_HOST:PAIRING_PORT"  # Only if wireless debugging requires pairing
adb connect "$TV_SERIAL"
adb devices -l
adb -s "$TV_SERIAL" install -r release/Ambilight-Hue-1.10.apk
adb -s "$TV_SERIAL" shell am start -W -n local.tv.hue/.MainActivity
```

`192.0.2.10` is a documentation placeholder. Use a trusted local network. Updating with `install -r` and the same signing certificate preserves launcher data. Uninstalling or clearing app data requires provisioning again.

## Native Hue Bridge pairing

Open the native wizard from the launcher, or run:

```sh
adb -s "$TV_SERIAL" shell am start -W -n org.droidtv.alhuewizard/.ALHueWizardActivity
```

Follow the native pairing flow and press the physical Hue Bridge button when requested. The launcher preserves this original Philips pairing. TV API pairing below is a separate step.

## Pair the TV API

Starting with version 1.10, choose **App pairing** in the launcher. On first use, follow the prompt to enable **Ambilight + Hue pairing** in the TV's Accessibility settings, then return and choose App pairing again. The helper reads the PIN from the Philips system prompt and completes pairing while that prompt remains open. Do not close the TV's PIN prompt: closing it cancels authorization. No computer or root is required for this step. The launcher verifies API access before replacing existing credentials. If pairing fails or expires, choose App pairing again. This is separate from pairing the native wizard with the Hue Bridge.

The accessibility helper listens only to selected Philips/system packages and reads the specifically named Ambilight Hue control PIN prompt only during a user-initiated pairing attempt. It does not click controls or log the PIN. You may disable the helper after successful pairing; normal synchronization does not need it. Tested on the owner's OLED909: native grant returned SUCCESS and authenticated HueLamp/power returned HTTP 200 on 4 November 2026.

Pairing connects only to the TV's own loopback address. The initial local certificate is pinned for the grant and subsequent API requests. PINs and credentials are not written to diagnostic logs.

The computer-side method below remains available for older versions and recovery.

Run these commands from the repository root. Keep `TV_HOST` exported for each invocation of `tv_api.py`.

```sh
python3 tools/tv_api.py pair
python3 tools/tv_api.py grant PIN_FROM_TV
python3 tools/tv_api.py get HueLamp/power
python3 tools/provision_launcher.py --serial "$TV_SERIAL" --host "$TV_HOST"
```

The TV displays a short-lived PIN after `pair`. Complete `grant` before it expires. The script stores credentials in `tools/jointspace-private.json` with mode 0600 when creating the file. This file is ignored by Git and must remain private. `pair` refuses to overwrite existing state; `renew` deliberately replaces it and should only be used when renewing pairing.

The Python pairing helper accepts the TV's self-signed HTTPS certificate. Provisioning obtains a certificate fingerprint on first setup, or preserves the launcher's existing fingerprint when available. First setup therefore trusts the local network and the TV address you provide. The Android app subsequently pins the TV certificate's SHA-256 fingerprint. The helper's generic curl requests use `--insecure`; they do not implement the app's certificate pinning.

The provisioning provider requires Android's `android.permission.DUMP`, available to ADB shell. Credentials are saved in private app storage and Android backup is disabled. Never publish the state file, provisioning command payloads or private app data.

From version 1.6, the Android client always uses **`127.0.0.1:1926`**. A LAN/VPN IP change does not require re-provisioning. `TV_HOST` and `--host` above mean the reachable TV address from your computer; do not set them to your computer's loopback address. The provisioner discovers the TV certificate remotely when needed, but always stores `127.0.0.1` for the app running on the TV.

The existing certificate pin is preserved unless you deliberately pass `--replace-certificate`. Only use this option after checking that the specified host is your TV and you intend to trust its changed certificate. The Android client's certificate validation is not disabled.

## Recovery after a wipe

A wipe can erase the launcher data and invalidate previously paired TV API credentials. Installing an old APK or re-provisioning an old state file does not restore valid authorization. In version 1.10, use **App pairing** again to create and verify new credentials directly on the TV. The alternative computer-side recovery procedure follows.

1. Keep the previous private state file outside the repository if you need an archive; otherwise use `renew` deliberately to replace only your pairing helper's current state.
2. Request fresh pairing and read the new short-lived PIN on the TV.
3. Complete the grant and confirm that it reports `SUCCESS`.
4. Provision those new credentials into the installed launcher.

```sh
python3 tools/tv_api.py renew
python3 tools/tv_api.py grant NEW_PIN_FROM_TV
python3 tools/tv_api.py get HueLamp/power
python3 tools/provision_launcher.py --serial "$TV_SERIAL" --host "$TV_HOST"
```

If the certificate changed, use the explicit `--replace-certificate` option on the last command. Never share `jointspace-private.json`. Native Hue Bridge pairing and bulb positions may also need restoring after a wipe; they are separate from TV API pairing.

For the optional root-only native wizard repair, see [WIZARD-RUNTIME.md](WIZARD-RUNTIME.md).

## Select and configure lights

1. Open **Add Hue lights to TV** (labels follow your TV language).
2. Discover the bridge or enter its IPv4 address.
3. On first use, press the bridge's physical button and choose **Pair**.
4. Select up to ten compatible color lights and save.
5. Open **Configure bulbs** and set their positions and brightness in the native wizard. If its confirmation does not work, TV API pairing alone will not fix it; see the separate root-only runtime repair notes.
6. Turn on the TV's own Ambilight, then choose **Turn on synchronization**.

Existing position and brightness configuration is retained for lights that remain selected. White ambiance lights are not offered.

## Verify and troubleshoot

```sh
adb -s "$TV_SERIAL" shell getprop odm.tpv.sys.HueIsActive
```

`true` indicates the native service reports synchronization active; it is not proof that every light received an update. Check visually as well. If the app reports On but lights do not respond, first confirm that the TV's own Ambilight is enabled. Then check TV-to-bridge reachability and pairing.

Use **Vypnout synchronizaci** to save native Off and restore the previous lighting state. It does not select a named Hue scene. Leaving On before standby may allow synchronization to resume on wake.

The native `ambilighthue` log can contain Hue API keys, including in request URLs. Do not publish raw logcat output. The launcher's `HueNativeAPI` tag is designed not to log credentials.

Do not repeatedly retry an unresponsive TV API. Earlier experimental connection handling caused hangs; the exact server-side cause was not established. A restart is a separate disruptive troubleshooting action, not an installation requirement.

For detailed error IDs, persistent logs and safe ADB export, see [DIAGNOSTICS.md](DIAGNOSTICS.md).

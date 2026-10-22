# Installation and pairing

## Requirements

A compatible Philips TV with its original Ambilight + Hue service, a reachable Hue Bridge with color lights, Python 3, curl and Android platform-tools (`adb`). The tested firmware and release are listed in the README.

Use your own TV IP and current ADB port in all examples. `TV_SERIAL` is the full ADB device identifier; the pairing port and connection port may differ.

```sh
export TV_HOST=192.0.2.10   # Replace with your TV's reachable IPv4 address
export TV_SERIAL="$TV_HOST:5555"  # Replace with its current ADB connection port
adb pair "$TV_HOST:PAIRING_PORT"  # Only if wireless debugging requires pairing
adb connect "$TV_SERIAL"
adb devices -l
adb -s "$TV_SERIAL" install -r release/Ambilight-Hue-1.5.apk
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

When the TV IP changes, run `provision_launcher.py` again with the new host and serial. A TV certificate change may require deliberate re-provisioning; the helper normally preserves an existing pin, so do not assume rerunning it replaces that pin.

## Select and configure lights

1. Open **Přidání HUE světel do DB**.
2. Discover the bridge or enter its IPv4 address.
3. On first use, press the bridge's physical button and choose **Spárovat**.
4. Select up to ten compatible color lights and save.
5. Open **Konfigurovat žárovky** and set their positions and brightness in the native wizard.
6. Turn on the TV's own Ambilight, then choose **Zapnout synchronizaci**.

Existing position and brightness configuration is retained for lights that remain selected. White ambiance lights are not offered.

## Verify and troubleshoot

```sh
adb -s "$TV_SERIAL" shell getprop odm.tpv.sys.HueIsActive
```

`true` indicates the native service reports synchronization active; it is not proof that every light received an update. Check visually as well. If the app reports On but lights do not respond, first confirm that the TV's own Ambilight is enabled. Then check TV-to-bridge reachability and pairing.

Use **Vypnout synchronizaci** to save native Off and restore the previous lighting state. It does not select a named Hue scene. Leaving On before standby may allow synchronization to resume on wake.

The native `ambilighthue` log can contain Hue API keys, including in request URLs. Do not publish raw logcat output. The launcher's `HueNativeAPI` tag is designed not to log credentials.

Do not repeatedly retry an unresponsive TV API. Earlier experimental connection handling caused hangs; the exact server-side cause was not established. A restart is a separate disruptive troubleshooting action, not an installation requirement.

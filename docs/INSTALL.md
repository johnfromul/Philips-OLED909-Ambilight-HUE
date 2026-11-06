# Installation and setup

Use a compatible Philips TV with its original Ambilight + Hue components and a reachable Hue Bridge with color lights. The verified model, firmware and kernel are in the README. Version 1.12 completes setup on the TV without a computer or root. Android API 26 or newer is required.

## Install or update

Download the APK from the latest GitHub release and sideload it using your normal TV installer. An ADB example follows; substitute your TV address and actual connection port:

```sh
export TV_HOST=192.0.2.10
export TV_SERIAL="$TV_HOST:5555"
adb connect "$TV_SERIAL"
adb -s "$TV_SERIAL" install -r release/Ambilight-Hue-1.12.apk
adb -s "$TV_SERIAL" shell am start -W -n local.tv.hue/.MainActivity
```

192.0.2.10 is a documentation placeholder. If wireless debugging requires a pairing port, use `adb pair` first. Updating with the same signing certificate preserves data. Uninstalling or clearing app data erases launcher credentials, drafts and recovery snapshots.

## First setup, entirely on the TV

Open **Set up Bridge and bulbs**. Follow its buttons in this order:

1. **Find bridge on network**. In stable 1.12 the search stops at the first reachable candidate. In development 1.13 it collects candidates for up to 120 seconds; choose one from the dropdown showing name, IP and identifier. You can also enter its IPv4 address manually. With multiple Bridges, check the selected address; discovery is not a multi-Bridge chooser.
2. **Pair Hue Bridge**. Press the physical Bridge button when the yellow, bold prompt appears. The app waits up to 120 seconds with a countdown and requests both a username and client key. A success dialog returns to the setup menu when you press OK.
3. **App pairing**. Enable **Ambilight + Hue pairing** in the TV's Accessibility settings when prompted, then return. This step prepares authorization; you do not need to press App pairing again after granting permission. The actual TV pairing completes when you save the lights.
4. **Load compatible lights**. Select one to ten color lights. Open each position control to use the graphical TV diagram. Use remote arrows and OK to confirm a position, or Back to cancel. L1 is at the lower left, L4 at the upper left, then Top, R4 through R1 on the right, and Bottom. This is viewed facing the TV. Set brightness from 1 to 10.
5. Enable the TV's own Ambilight, then choose **Save and start synchronization**. If TV API credentials are absent and you completed the permission step, the app automatically requests and reads the Philips TV PIN, verifies pairing, saves the configuration and starts the native Philips Hue service. Keep the system PIN prompt open; closing it cancels authorization. A final dialog reports the successful pairing (when newly performed) and configuration save. Verify actual light response visually.

TV API pairing and Bridge pairing are separate authorizations. The short-lived Philips PIN prompt may flash briefly because the explicitly enabled helper reads it automatically. No manual PIN entry or computer is needed. The helper listens only to selected system packages and reads the specifically named pairing prompt during an active pairing attempt; it does not click controls or log the PIN. You may disable it after successful pairing. Normal synchronization does not require it.

The native service receives both Bridge credentials and the configured lamp records. It sets its own configuration flag during startup; the app does not need direct access to the restricted Philips settings library. Root and the optional native wizard repair are not required for this path on the tested TV. The **Original Philips wizard** is available under **Advanced options** in development version 1.13.

## Saved settings and drafts

Existing native positions, brightness and placement are loaded for the paired Bridge. New lights use the native default placement. Compatible types are Color light and Extended color light; White ambiance is not offered. Existing selected native lights absent from the compatible list are retained.

Selections, positions and brightness are also saved as a private per-Bridge draft while editing, so leaving and reopening the screen does not lose unfinished work. The draft is reused only while the underlying native configuration matches its baseline. A confirmed native save takes precedence over an older draft. Back returns from light configuration to the setup menu.

Before changing native records, the app saves a private recovery snapshot. Failed saves attempt to restore the previous credentials, records and On/Off setting. An interrupted save exposes **Restore previous configuration** and blocks another setup until recovery. Do not clear app data until recovery succeeds. This is a configuration snapshot, not a full TV backup; recovery cannot be guaranteed across firmware changes or hardware/network failures. Native Philips code can log Bridge credentials, so do not publish raw native logs.

## Verify and troubleshoot

If Bridge pairing succeeds but TV authorization is missing, complete the App pairing permission step in the same setup menu and save the lights again. Previously valid TV API credentials are reused. If credentials were invalidated by a firmware reset rather than simply erased, further recovery may be required; do not assume a saved token is still valid.

A native On result verifies the switch and service notification, not optical synchronization. Confirm that Ambilight is enabled and the lights follow screen colors. **Turn off synchronization** asks the Philips service to restore previous lighting; it does not choose a named Hue scene.

The optional root-only original-wizard repair is documented in [WIZARD-RUNTIME.md](WIZARD-RUNTIME.md). It is separate from the rootless in-app setup.

## Optional computer-side TV API recovery

Python 3, curl and ADB are needed only for this alternative. Run from the repository root with TV_HOST and TV_SERIAL set to your actual TV:

```sh
python3 tools/tv_api.py pair
python3 tools/tv_api.py grant PIN_FROM_TV
python3 tools/tv_api.py get HueLamp/power
python3 tools/provision_launcher.py --serial "$TV_SERIAL" --host "$TV_HOST"
```

The TV displays a short-lived PIN. The pairing helper stores private state in ignored `tools/jointspace-private.json`; do not share it. `pair` refuses to replace existing state. After a wipe use `renew` deliberately to request fresh credentials, then grant, verify and provision again. Use `--replace-certificate` only after verifying an intended TV certificate change.

The computer helper accepts the TV's self-signed certificate. The Android app pins its certificate fingerprint after initial local pairing and always uses 127.0.0.1:1926. The provisioner requires Android DUMP permission through ADB shell; ordinary apps cannot use it. Never publish provisioning payloads or private app data.

## Advanced options in development 1.13

The original Philips wizard is under Advanced options. Reset requires explicit confirmation. It clears this app’s native Hue configuration, keys, drafts and TV API credentials, while leaving authorizations on the Bridge intact. It is not a Bridge factory reset or a general TV reset. The native records are backed up before reset and verified after clearing; if a native write fails, restoration is attempted. An interrupted reset keeps recovery data, including the TV API credentials required for restoration. A reset of existing native setup requires working TV API authorization. Without TV API credentials, reset is permitted only when native Bridge and lamp records are already empty. The helper’s Accessibility permission is managed separately in TV settings.

Indicators distinguish saved authorizations, Accessibility readiness, authenticated native On/Off and the native service’s active flag (when readable). None is a direct optical measurement. Public Bridge names may be cached. mDNS discovery is not evidence that a Bridge Pro supports the native TV protocol.

# Optional root-only native wizard repair

The optional **Hue-Wizard-Runtime-Fix-1.1-Magisk-EN.zip** repairs remote confirmation in the original Philips Hue wizard. It is separate from launcher APK 1.6. The launcher itself does not require root.

## Tested behavior

- Native light checkboxes respond to OK/Enter and update the Philips database.
- Confirmation on the Bridge linked screen proceeds to light selection.
- Done proceeds from light selection to the native configuration hub; Finish closes the wizard.
- Long confirmation presses do not repeatedly toggle a light, and rapid navigation uses the current selected row.
- A controlled A/B check reproduced failure with the helper disabled and successful selection with it enabled.
- The helper automatically reloaded after several native Hue process restarts.

The verified flow reused the existing authorized bridge. **A real factory wipe and a full TV reboot with the module have not been tested.** Startup via Magisk is prepared, but it must not be presented as already verified after a reboot. Module 1.1 keeps the tested Java helper and adds an app-installable ZIP, creation of missing private storage after a wipe, and protection against an unrelated process reusing a stale watcher PID.

## Compatibility

Philips 65OLED909, Android 14, firmware TPM231WW_R.201.100.146.220 / build 3000428; kernel 5.15.180-android14-11-00042-g766efc243647-dirty.

The installer and watcher require this exact original native APK:

- `/system_ext/app/ambilighthue/ambilighthue.apk`
- SHA-256: `0e865164982b371e24496a543c3c79670a51e96f2d1cbc41c0e8011b2ff197f4`

A mismatch stops installation or injection. Do not bypass the check on unverified firmware. Root and Magisk are prerequisites; this package does not root the TV. It does not replace the original APK, alter its signature, change SELinux policy or start a network-facing diagnostic server. Zygisk is not required by this module.

## Installation after a wipe

1. Complete TV setup and confirm root / Magisk still work. A wipe or firmware flash may remove them and existing modules.
2. Copy the module ZIP to the TV without extracting it.
3. In Magisk, choose Modules → Install from storage and select the module ZIP.
4. Confirm installation succeeded, then reboot yourself when appropriate so Magisk starts the service.
5. Open the original Hue wizard and allow a few seconds for the runtime helper to attach.
6. Pair the Hue Bridge using its physical button if the reset erased pairing. Select color lights, confirm Done, and configure positions and brightness again.

The module does **not** restore bridge keys, selected light IDs, bulb positions or launcher API authorization. Keep its installation ZIP outside the TV so a wipe cannot erase your only copy. Fresh launcher API pairing after a wipe is covered in [INSTALL.md](INSTALL.md).

## Runtime and removal

The service watches only `org.droidtv.ambihue` and uses the official Frida 16.7.19 ARM injector to load the helper into that process. The helper wraps the native Hue activity's window callback and forwards confirmation to its existing list-screen handler. Other input delegates to the original callback. Existing valid bridge context can be restored from the native link service; no credentials are embedded.

Magisk can disable or remove the module using its usual reboot lifecycle. For immediate disable without a TV restart:

```sh
adb -s "$TV_SERIAL" shell su -c '/system/bin/sh /data/adb/modules/hue_wizard_runtime/stop.sh'
```

The helper observes a private disable marker and restores original window callbacks. The watcher exits. Reinstalling the module clears that private marker. The installed module path is `/data/adb/modules/hue_wizard_runtime/`.

Use `adb shell logcat -d -s HueWizardFix:V` for credential-free helper messages. Do not publish raw native Hue logs; their request URLs can contain bridge keys.

## Build / packaging

The module's Java source and service/bootstrap scripts are in `wizard-runtime/`. Compile the Java source against Android platform 36, convert all resulting classes with D8, and package the DEX plus those scripts and the official ARM injector at ZIP root. `module.prop` must be at root, and `bin/frida-inject`, `service.sh` and `stop.sh` must be executable. `customize.sh` also sets permissions during installation.

The prebuilt Java helper's SHA-256 is `12cef824edf8183d1c7dca674d7864efb99d7bc4237adfa175f028745c5b2ea7`.

Frida is third-party software. The unmodified injector comes from [Frida 16.7.19](https://github.com/frida/frida/releases/tag/16.7.19); upstream licensing notices and source references are in `wizard-runtime/third-party/`. No extracted Philips APK, private pairing data or private signing key is included.

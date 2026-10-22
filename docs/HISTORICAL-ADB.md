# Historical ADB recovery notes

Prefer the launcher's light selection screen. The following database operations are historical diagnostics for the tested firmware, not required for an ordinary APK update. Substitute your current `TV_SERIAL` and actual bridge light IDs.

```sh
adb -s "$TV_SERIAL" shell content query --uri content://org.droidtv.ambihue.provider/HueLampsInfo
```

For a light that is absent from the query, an example insertion is:

```sh
adb -s "$TV_SERIAL" shell content insert --uri content://org.droidtv.ambihue.provider/HueLampsInfo --bind LampID:i:1 --bind ConfigureStatus:i:0
```

`LampID` is the Hue Bridge's actual numeric light identifier, not the screen row number. It is unique: do not insert an existing ID. `ConfigureStatus=0` marks an unconfigured selected light; use the native wizard to configure its position and brightness. Do not copy another home's light IDs or overwrite existing configuration blindly.

```sh
adb -s "$TV_SERIAL" shell am start -W -n org.droidtv.alhuewizard/.ALHueWizardActivity
```

For diagnostic reference, native service actions are:

```sh
adb -s "$TV_SERIAL" shell am start-foreground-service -n org.droidtv.alhuewizard/org.droidtv.ambihue.HueService -a org.droidtv.ambihue.HueService.ALHUEON
adb -s "$TV_SERIAL" shell am start-foreground-service -n org.droidtv.alhuewizard/org.droidtv.ambihue.HueService -a org.droidtv.ambihue.HueService.ALHUEOFF
```

These service-only actions do not replace v1.5's persistent native API switch. In particular, On can fail when the saved switch is Off, and service-only Off is not proof of persistent Off.

The earlier patched `tpvacfg` experiment is intentionally not distributed: its IPC connection failed under SELinux, and apparent success output was misleading. The wizard completion workaround was not proven reproducible. This repository contains no proprietary diagnostic binaries or extracted Philips packages.

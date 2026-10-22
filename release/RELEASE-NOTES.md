# Ambilight + Hue 1.5

Manual control of the Philips OLED909's built-in Ambilight + Hue integration.

This release saves the native Hue On/Off setting through paired JointSPACE v6, verifies it with GET and notifies the original Philips service. Turning synchronization off restores previous lighting using the native mechanism. Includes compatible color-light selection and access to native bulb position configuration.

**Requirements:** the original Philips Hue service, a paired Hue Bridge with color lights, TV API provisioning, and the TV's own Ambilight switched on. Tested on Philips 65OLED909 firmware TPM231WW_R.201.100.146.220 / 3000428. App interface: Czech. Documentation: English.

Download `Ambilight-Hue-1.5.apk` for installation or `Ambilight-Hue-1.5-bundle.zip` for the APK plus English documentation and pairing helpers. Verify downloads using `SHA256SUMS.txt`.

Existing installations signed by the same key can use `adb install -r`. A clean installation needs TV API pairing and provisioning. See `docs/INSTALL.md`.

Automatic Off on standby is not included; On may resume after wake. Long-term API stability and a new full reboot after final verification were not tested. No private credentials or signing keys are included.

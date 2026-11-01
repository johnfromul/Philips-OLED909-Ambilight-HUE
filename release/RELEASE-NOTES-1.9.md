# Ambilight + Hue 1.9 — system-language launcher

Version 1.9 localizes the launcher according to the TV system language. Czech, English, German, French and Spanish are included; other locales use English. This fixes the previous hard-coded Czech **Konfigurovat žárovky** label: on an English TV it now reads **Configure bulbs**.

The native Philips configuration wizard is opened by the launcher but is not included or modified by this APK. Hue Bridge Pro compatibility has not been verified. The tested setup remains Hue Bridge V2 with the firmware listed in the README.

This APK retains the original Hue launcher signing identity. Install with `adb install -r` to update release 1.8 in place and preserve existing TV API pairing.

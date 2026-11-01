# Ambilight + Hue 1.9 — system-language launcher

Version 1.9 localizes the launcher according to the TV system language. Czech, English, German, French and Spanish are included; other locales use English. This fixes the previous hard-coded Czech **Konfigurovat žárovky** label: on an English TV it now reads **Configure bulbs**.

The native Philips configuration wizard is opened by the launcher but is not included or modified by this APK. Hue Bridge Pro compatibility has not been verified. The tested setup remains Hue Bridge V2 with the firmware listed in the README.

This APK is signed with a new signing identity because the former key could not be recovered. It cannot update releases 1.8 or earlier in place. Uninstall the previous launcher, install version 1.9, and perform TV API pairing again.

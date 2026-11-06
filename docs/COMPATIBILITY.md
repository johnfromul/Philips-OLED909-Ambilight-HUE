# Compatibility

This project targets compatible Philips TPM231WW TVs running **Android TV / Google TV** and containing the original Philips Hue provider, LinkService, native HueService and TV API. Titan OS is excluded: this Android APK is not a Titan OS application. A model year, shared product branding or platform name alone is not enough to declare support.

| TV | Firmware | Evidence |
| --- | --- | --- |
| Philips 65OLED909 | TPM231WW_R.201.100.146.220, build 3000428 | Owner’s TV; rootless clean setup, local TV pairing, two-lamp red/blue sync |
| Philips OLED808 | TPM231WW_R.201.100.228.212 | Alex’s report: fixed multilingual PIN capture works; Bridge pairing, lamp setup and synchronization work |

OLED808 and OLED909 are verified configurations, not a claim covering every firmware revision. Other TPM231WW Android TV / Google TV models are candidates requiring testing. No blanket exclusion or inclusion is made by model year.

Alex also confirmed on his OLED808 that the original lamp state returns when the TV is switched off and synchronization resumes when it is switched on. These observations depend on his saved native settings and firmware; they are not guaranteed for every model or every standby/power-loss scenario.

Hue Bridge V2 is verified. Hue Bridge Pro remains unverified. Discovery, local API pairing, lamp configuration and actual native synchronization must be tested separately. The current app uses the legacy local Bridge API over HTTP for setup; the new selection control does not change that transport or the protocol used by the native Philips service.

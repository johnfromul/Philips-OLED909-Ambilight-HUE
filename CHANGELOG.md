# Changelog

## 1.11 — 2026-11-05

- Added rootless Hue Bridge setup directly in the app: fresh username/client key, native service handoff, light positions and brightness.
- Added a private recovery snapshot, rollback attempts and interrupted-save recovery.
- Explicit native service startup completes the setup; no original wizard is needed on the tested OLED909.
- Added CZ/EN/DE/FR/ES setup strings and retained the original wizard as an optional control.
- Requires Android API 26 or newer.
- Verified a clean-state prototype with red/blue light response; final APK saved two lamps and started the native service on the TV.

## 1.10 — November 4, 2026

- Add TV-local App pairing with Czech, English, German, French and Spanish instructions.
- Add an explicitly enabled Accessibility helper for the Philips PIN prompt, scoped to selected system packages and an active pairing request. Closing the native prompt cancels pairing, so authorization completes while it stays open.
- Pin the local TV certificate, reuse Digest authentication for the grant, and verify authenticated API access before saving new credentials. Keep existing credentials on failure.
- Verify native grant SUCCESS and authenticated power read HTTP 200 on the owner's OLED909. Root and computer-side provisioning are not used by the pairing flow.

## 1.9 — November 1, 2026

- Localize the launcher using the TV system language: Czech, English, German, French and Spanish. Other locales fall back to English.
- Localize the button that opens the native Philips wizard and the launcher-managed light-selection flow.
- Clarify the unverified status of Hue Bridge Pro compatibility. The native wizard and launcher workflow have only been tested with Hue Bridge V2.

## 1.8 — October 27, 2026

- Send headers and POST body in one socket write to avoid the observed Philips empty-command acknowledgement.
- Verify each command over a fresh TLS connection with a fresh Digest challenge.
- Deliver the native service intent independently of verification read success; bounded recovery permits two explicit state writes and two verification reads per write.
- Keep certificate pinning and fail on authentication, certificate and protocol errors.
- Tested real Off and On transitions, with On completing in 166 ms and native HueIsActive true.

## 1.7 — October 24, 2026

- Add private rotating diagnostics, correlated API stages, sanitized exceptions and read-only follow-up switch checks.
- Provide DUMP-protected export and an ADB collector that filters native service events.
- Clarify that the saved switch does not independently confirm lamp response.

## 1.6 — October 24, 2026

- Always connect the Android launcher to the TV API over `127.0.0.1`, including upgrades with obsolete saved LAN/VPN addresses.
- Provision local API routing independently of the computer-side address used for certificate discovery.
- Preserve the existing certificate pin by default and offer explicit certificate replacement during recovery.
- Document fresh TV API pairing after a wipe and the separate optional root-only native wizard runtime module.
- Keep the original signing identity, native On/Off persistence and light restoration behavior.


## 1.5 — October 21, 2026

- Persist native Hue On/Off using paired JointSPACE v6 `HueLamp/power` and verify with GET.
- Notify the installed Philips service after the saved setting changes.
- Use a single TLS connection with certificate pinning and HTTP Digest authentication.
- Keep large remote-friendly buttons, native position configuration and compatible color-light selection.
- Store credentials privately behind an ADB-permission-protected provisioning provider.

## Earlier development

Versions 1.3 and 1.4 established manual service control and the launcher interface. Service-only control did not reliably persist Off or re-enable synchronization when the saved switch was Off. These earlier APKs are not part of this initial GitHub release.

## Documentation update — October 23, 2026

Initial GitHub publication with English installation, build and technical notes. Documented the confirmed requirement that the TV's own Ambilight be On. The published APK remains the verified v1.5 binary.

The position control shows a TV diagram with ten selectable locations, matching the native zone order. Remote arrows select a location; OK confirms and Back cancels. Labels and guidance follow the TV language.

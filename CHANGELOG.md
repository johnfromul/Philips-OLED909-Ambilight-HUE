# Changelog

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

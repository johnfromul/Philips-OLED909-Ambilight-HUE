# Ambilight + Hue 1.10 — pair directly on the TV

The new **App pairing** button authorizes the launcher with the TV API without a computer or root.

1. Install the APK as an update over your existing version.
2. Select **App pairing**. On first use, enable **Ambilight + Hue pairing** in the TV's Accessibility settings.
3. Return to the launcher and select **App pairing** again.
4. Leave the Philips PIN prompt open. The helper reads its PIN and completes authorization automatically.
5. Wait for **App paired successfully**. The helper may then be disabled; normal synchronization does not need it.

Existing users with working pairing do not need to pair again. The APK uses the original signing identity and preserves app data during an in-place update. Instructions follow the TV language: CZ, EN, DE, FR and ES.

The helper handles only the specifically named Philips pairing prompt during a user-initiated attempt. PINs and credentials are not logged. The local certificate is pinned, and authenticated API access is verified before existing credentials are replaced.

Validated on the owner's OLED909 on November 4, 2026: grant SUCCESS and authenticated HueLamp/power HTTP 200. ADB was used to install and operate the test and inspect results; pairing itself ran inside the app without root or computer-side provisioning.

TV API pairing is separate from Hue Bridge pairing and bulb configuration. This release does not repair a broken native Philips wizard or establish Hue Bridge Pro compatibility. See [installation instructions](https://github.com/johnfromul/Philips-OLED909-Ambilight-HUE/blob/main/docs/INSTALL.md).

Assets include the signed APK, SHA-256 checksum, source archive, and documentation bundle. Private signing keys and TV/Hue credentials are not distributed.

# Ambilight + Hue 1.11

Set up a Hue Bridge and configure lights directly in the app, without root or the original Philips wizard on the tested OLED909.

1. Complete **App pairing** with the TV. Enable the Accessibility pairing helper when requested; it may be disabled after successful TV pairing.
2. Open **Set up Bridge and bulbs**. Discover the Bridge or enter its IPv4 address.
3. Press its physical button and choose **Pair**.
4. Select up to ten color lights, their native positions and brightness.
5. Choose **Save and start synchronization**. Keep the TV's own Ambilight enabled.

The app obtains both Bridge credentials, writes native configuration and explicitly starts the Philips Hue service. No computer is required for this setup. Existing TV API pairing is preserved by an in-place update. Old app-only Bridge credentials without a client key require pairing again, unless the existing native pairing can be reused.

Setup uses a private snapshot before native writes. Failed saves attempt rollback; interrupted saves expose a recovery control. Keep app data until recovery succeeds. The original wizard remains an optional separate control. New UI and messages support Czech, English, German, French and Spanish using the TV language.

Validation: a clean-state prototype, with native pairing/lights erased, Hue flags reset and root wizard repair disabled, obtained new Bridge credentials and synchronized two lamps to red/blue test screens. Final app 1.11 loaded existing native credentials, edited/displayed positions and brightness, saved two lamps and started the native service on the owner's TV. Nine focused tests pass, including system-locale selection. All rollback failures and firmware variants have not been exhaustively tested.

Tested TV: Philips 65OLED909, firmware TPM231WW_R.201.100.146.220 (build 3000428), kernel 5.15.180-android14-11-00042-g766efc243647-dirty. OLED808, other firmware and Hue Bridge Pro remain unverified. Android API 26 or newer and compatible original Philips services are required.

VersionCode 12. Signed with the original Hue certificate for in-place updates. APK, source archive, documentation bundle and SHA256 checksums are attached. No private credentials or signing key are included.

The position control shows a TV diagram with ten selectable locations, matching the native zone order. Remote arrows select a location; OK confirms and Back cancels. Labels and guidance follow the TV language.

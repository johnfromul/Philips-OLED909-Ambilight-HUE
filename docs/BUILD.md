# Build from source

Use Python 3, a JDK with `javac`, Android SDK platform 36 and build-tools 36.1.0. No Gradle dependencies are required. `JAVA_HOME`/PATH must provide a suitable JDK for the installed Android tools.

```sh
export ANDROID_SDK_ROOT=/path/to/Android/sdk
export HUE_KEYSTORE=/private/path/to/your-signing.keystore
export HUE_KEYSTORE_PASSWORD='your-keystore-password'
# Optional if your key password differs:
export HUE_KEY_PASSWORD='your-key-password'
python3 build.py
```

For a new signing identity, create your own keystore interactively using `keytool`; keep it outside this repository. `HUE_KEY_ALIAS` can select an alias if needed. Optional `ANDROID_BUILD_TOOLS_VERSION` and `ANDROID_PLATFORM` override defaults 36.1.0 and 36.

The output is `build/ambilight-hue-launcher.apk`. The script creates its output directories, compiles Java and resources, adds DEX, aligns, signs and verifies the APK. Passwords are supplied to apksigner through environment references rather than literal command arguments.

The current release is `release/Ambilight-Hue-1.12.apk`, Android versionCode 13, minimum Android API 26. It is signed with the original local Hue identity so it updates earlier releases in place. No private signing material is distributed.

Run the focused diagnostic, HTTP request, recovery and provisioning tests with:

```sh
python3 -m unittest discover -s tests -v
```

Your build will only update an installed app in place if it uses the same signing identity. The original private signing key is not published. Uninstalling to install a differently signed build removes launcher data and requires API provisioning again.

The interface follows the TV language: Czech, English, German, French and Spanish, with English fallback.

The development sources are now version 1.13, versionCode 14. The published stable release remains 1.12. The 1.13 TV test build preserves the same signing identity; reset logic has failure-injection tests, but a destructive reset has not been exercised on the owner’s restored live configuration.

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

The published `release/Ambilight-Hue-1.6.apk` is built from the current version 1.6 sources with the original local signing identity. The earlier 1.5 APK is retained as a historical artifact. No private keystore or signing password is distributed. Pairing tools use `TV_HOST` / `--host` for the computer-to-TV connection and always provision loopback for the app on the TV.

Run the focused computer-side provisioning tests with:

```sh
python3 -m unittest discover -s tests -v
```

Your build will only update an installed app in place if it uses the same signing identity. The original private signing key is not published. Uninstalling to install a differently signed build removes launcher data and requires API provisioning again.

The v1.6 interface remains Czech. English documentation does not imply an English-language APK.

# Publication validation — October 23, 2026

- All four Java files match the verified v1.5 source files byte for byte.
- The published APK matches the original v1.5 build artifact byte for byte.
- Android apksigner verified the release APK (v1, v2 and v3 signatures).
- The portable build script successfully compiled, packaged, signed and verified a local build with SDK platform 36 and build-tools 36.1.0.
- Python helpers passed syntax checks; provisioning help exposes explicit host and ADB serial arguments.
- The distribution ZIP passed its integrity check. Private TV pairing values were checked against publication contents and nested archives without disclosing them; none were found.
- The existing October 21 device validation and October 22 user-confirmed Ambilight requirement are documented separately. No TV installation, lighting change or restart was performed for publication.

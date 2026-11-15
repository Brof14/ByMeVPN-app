# Native VPN runtime provenance

The Android build includes `app/libs/libv2ray.aar` as a checked-in binary.

- Archive SHA-256: `e56ec3b06485fe7b37af26948673951edf719c50d6bbc618b8539045952adde4`.
- The archive contains JNI libraries for `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64`.
- The most recent commit changing the archive in the repository history is `d7715aed411add344ab9110c8da938f431a0f156`.
- The repository history identifies an earlier restore in `a419a78aa5983096ae055398cd82dc8fa27049f0`.

The available repository history does not establish the upstream source revision, build inputs, toolchain, or a complete dependency inventory for this binary. Embedded version-like strings are not proof of provenance. This record is an integrity fingerprint, not a verified SBOM or a reproducible-build attestation. Those require the original build source and dependency metadata.

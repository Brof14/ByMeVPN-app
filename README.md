# ByMeVPN for Android

An Android client for managing ByMeVPN subscriptions and VPN connections. The app is written in Kotlin with Jetpack Compose and integrates Xray through the `libv2ray.aar` runtime.

> **Status:** Android client with automated signed releases. Unit tests, a successful APK build, and release signing do not prove end-to-end account, payment, or VPN traffic behavior on every device and network.

## Current capabilities

- Import a compatible subscription and connect through Android `VpnService`.
- VLESS Reality, VLESS TLS, Hysteria 2, and VLESS XHTTP configuration support. Unit tests cover parsing and generated configuration; real server interoperability depends on the deployed node and network.
- Connection checks distinguish proxy reachability from proof that every Android app's traffic, DNS, and IPv6 are protected.
- Select the welcome-screen language from the device language.

Support depends on server configuration, availability, and the user's network. Device-level checks are still needed for DNS, IPv6, network changes, reconnects, and background operation.

## Build

The production application is the native Kotlin/Compose module at `app/`.
The superseded Flutter prototype (`android/`, `lib/`, `web/`, and Dart build files)
was removed from the working tree to avoid accidentally building a different client.
Its history remains available in Git.


Requirements: JDK 17, Android SDK 36, and Gradle 8.11.1 (the version used by GitHub Actions).

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

For a side-by-side debug build:

```sh
gradle :app:assembleDebug -PsideBySideDebug=true
```

The side-by-side build uses a separate application ID and app data. Debug builds use a local debug signing key and are not suitable for release distribution.

Direct releases include architecture-specific APKs and a universal fallback. The in-app updater selects the matching Android ABI to avoid downloading Xray binaries for unrelated CPU architectures.

GitHub Actions runs unit tests and creates debug artifacts. A version change merged to `main` triggers the signed Android release workflow, which checks the release certificate and publishes the APKs, Play-format AAB, checksums, and update manifest to GitHub Releases. The Play build excludes external YooKassa checkout; Google Play Billing is not yet integrated, so this AAB is not ready to sell subscriptions through Play. See [`docs/DIRECT_DISTRIBUTION.md`](docs/DIRECT_DISTRIBUTION.md).

## Known release blockers

- Payment, subscription renewal, and device-limit changes require provider-backed end-to-end verification. Do not infer correct live billing from unit tests or mock providers.
- Google Play Billing and server-side purchase-token verification are not implemented. Play builds deliberately do not open the external YooKassa checkout.
- Direct APK updates require a signed upgrade from an existing release APK and a verified HTTPS update manifest.
- VPN release acceptance requires real traffic checks for DNS, IPv4/IPv6, reconnects, and clean shutdown on supported devices; source checks alone cannot establish leak-free routing.
- The release build currently keeps R8/minification disabled. Enable it only after validating native Xray integration and upgrade behavior.
- The native VPN runtime is committed as a binary dependency. Its provenance and a release SBOM should be documented before formal Play publication.
- Referral data shown by the bot is not yet connected to an app-side server API.

Do not present local UI state as proof of an authenticated account, completed payment, or active entitlement.

## Security

Do not commit passwords, signing keys, subscription links, or VPN credentials. Historical commits contained test VPN configuration; treat any real values previously committed as exposed and rotate them at the server. This warning is retained because changing current files does not remove values from Git history.

## Server implementation notes

See [`docs/VPS_IMPLEMENTATION_PROMPT.md`](docs/VPS_IMPLEMENTATION_PROMPT.md) for the current server-side implementation brief. See [GitHub Actions](https://github.com/Brof14/ByMeVPN-app/actions) for build workflows.

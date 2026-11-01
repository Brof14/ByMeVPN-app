# ByMeVPN for Android

An Android client for managing ByMeVPN subscriptions and VPN connections. The app is written in Kotlin with Jetpack Compose and integrates Xray through the `libv2ray.aar` runtime.

> **Status:** In active development and testing. This repository is not yet a production-ready consumer release. Production API/payment integration, release signing, direct-update hosting, and full network lifecycle validation still need completion.

## Current capabilities

- Import a compatible subscription and connect through Android `VpnService`.
- Tested connection flows for VLESS Reality, VLESS TLS, Hysteria 2, and VLESS XHTTP on a test device.
- Display connection-check results while distinguishing a TCP reachability check from proof that VPN traffic is routed through the tunnel.
- Select the welcome-screen language from the device language.

Support depends on server configuration, availability, and the user's network. Longer tests are still needed for DNS, IPv6, network changes, reconnects, and background operation.

## Build

Requirements: JDK 17, Android SDK 35, and Gradle 8.9.

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

For a side-by-side debug build:

```sh
gradle :app:assembleDebug -PsideBySideDebug=true
```

The side-by-side build uses a separate application ID and app data. Debug builds use a local debug signing key and are not suitable for release distribution.

GitHub Actions runs unit tests and creates debug artifacts. A tagged direct-download release (`vX.Y.Z`) uses the separate `Publish direct Android APK` workflow. It requires a protected release signing key. Publishing to the ByMeVPN update site also requires its restricted upload credentials; until those are configured, the signed APK and update manifest remain a private Actions artifact. See [`docs/DIRECT_DISTRIBUTION.md`](docs/DIRECT_DISTRIBUTION.md).

## Known release blockers

- The production API must be rechecked for the mobile V2 plans, subscription, payment, and device contracts before release. Prices and device limits must come from the server; checkout stays disabled unless the server explicitly enables it.
- Google Sign-In stays unavailable until the server validates ID-token signatures, audience, expiry, and nonce. Email access must remain usable independently of Telegram.
- Payment and subscription renewal flows need provider-backed staging verification. Do not enable live checkout based only on unit tests or mock providers.
- Direct APK updates need a verified signed upgrade from an existing release APK and a tested HTTPS update manifest on the ByMeVPN site.
- VPN release acceptance still requires real tunnel traffic checks for DNS, IPv4/IPv6, reconnects, and clean shutdown on supported devices.
- Referral data shown by the bot is not yet connected to an app-side server API.

Do not present local UI state as proof of an authenticated account, completed payment, or active entitlement.

## Security

Do not commit passwords, signing keys, subscription links, or VPN credentials. Historical commits contained test VPN configuration; treat any real values previously committed as exposed and rotate them at the server. This warning is retained because changing current files does not remove values from Git history.

## Server implementation notes

See [`docs/VPS_IMPLEMENTATION_PROMPT.md`](docs/VPS_IMPLEMENTATION_PROMPT.md) for the current server-side implementation brief. See [GitHub Actions](https://github.com/Brof14/ByMeVPN-app/actions) for build workflows.

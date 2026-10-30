# ByMeVPN for Android

An Android client for managing ByMeVPN subscriptions and VPN connections. The app is written in Kotlin with Jetpack Compose and integrates Xray through the `libv2ray.aar` runtime.

> **Status:** In active development and testing. This repository is not a production-ready consumer release. Authentication, payments, release signing, and full network lifecycle validation still need completion.

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

GitHub Actions runs unit tests and creates a debug artifact. Release publishing is intentionally not automatic; a protected signing key and a verified release process are required.

## Known release blockers

- The configured API currently uses HTTP. The client blocks sending credentials over that connection; verified HTTPS and server-side authentication are required.
- Google Sign-In token verification must happen on the server.
- Payment and subscription renewal flows need verified server-side handlers and a policy-compliant Play distribution plan.
- Referral data shown by the bot is not yet connected to an app-side server API.

Do not present local UI state as proof of an authenticated account, completed payment, or active entitlement.

## Security

Do not commit passwords, signing keys, subscription links, or VPN credentials. Historical commits contained test VPN configuration; treat any real values previously committed as exposed and rotate them at the server. This warning is retained because changing current files does not remove values from Git history.

## Server implementation notes

See [`docs/VPS_IMPLEMENTATION_PROMPT.md`](docs/VPS_IMPLEMENTATION_PROMPT.md) for the current server-side implementation brief. See [GitHub Actions](https://github.com/Brof14/ByMeVPN-app/actions) for build workflows.

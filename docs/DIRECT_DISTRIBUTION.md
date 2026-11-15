# Direct Android distribution

The direct APK checks `update.json` in the latest GitHub Release and accepts the corresponding GitHub release APK. Before handing an APK to Android's installer, it checks the checksum, package ID, signing certificate, and that the installed version is older.

## First release setup

1. Confirm that `applicationId` is `com.bymevpn.app`, and keep it unchanged for updates.
2. Create the release signing keystore on a trusted local computer. Keep an encrypted offline backup. Never commit the keystore or its passwords.
3. Add GitHub Actions repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. `KEYSTORE_BASE64` is the base64-encoded keystore file.
   If the backend returns ES256-signed offline entitlements, also set the GitHub Actions repository variable `SERVER_ES256_PUBLIC_KEY` to the matching PEM public key. The app fails closed for a signed entitlement when this value is absent; never substitute a generated or unrelated key.
4. Increase Android `versionCode` and `versionName` for each release. The version-bump workflow on `main` signs and verifies the APK, then publishes it with `SHA256SUMS.txt` and `update.json` to GitHub Releases.

## What users see

Android may ask the user to allow ByMeVPN as an install source. Later updates are checked when the app returns to the foreground, downloaded from GitHub Releases, validated, and handed to Android's package installer. Android may still show a confirmation screen; ordinary third-party apps cannot promise silent installation on personal phones. The separate Play build excludes the direct APK updater and removes `REQUEST_INSTALL_PACKAGES`.

## Release safety

Never rotate the release signing key without an Android signing-key migration plan. Verify update installation from an existing signed APK before announcing a release. A successful build does not prove the upgrade path or real VPN traffic.

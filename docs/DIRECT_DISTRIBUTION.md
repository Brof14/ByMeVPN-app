# Direct Android distribution

ByMeVPN can be distributed from `https://bymevpn.duckdns.org/app/` without Google Play. The app checks the public `app/update.json` manifest and accepts APKs only from the same HTTPS host. Before handing an APK to Android's installer, it checks the checksum, package ID, signing certificate, and that the installed version is older.

## First release setup

1. Confirm that `applicationId` is `com.bymevpn.app`, and keep it unchanged for updates.
2. Create the release signing keystore on a trusted local computer. Keep an encrypted offline backup. Never commit the keystore or its passwords.
3. Add GitHub Actions repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. `KEYSTORE_BASE64` is the base64-encoded keystore file.
   If the backend returns ES256-signed offline entitlements, also set the GitHub Actions repository variable `SERVER_ES256_PUBLIC_KEY` to the matching PEM public key. The app fails closed for a signed entitlement when this value is absent; never substitute a generated or unrelated key.
4. The VPS must serve `/var/www/bymevpn/app/update.json` and `/var/www/bymevpn/app/downloads/` over HTTPS on `bymevpn.duckdns.org`. The manifest fields are `version_name`, `version_code`, `download_url`, `sha256`, and `release_notes`.
5. For automatic publishing from tagged builds, configure a chrooted SFTP-only upload identity as GitHub secrets `UPDATE_SSH_HOST`, `UPDATE_SSH_USER`, `UPDATE_SSH_KEY`, and `UPDATE_SSH_KNOWN_HOSTS`; set repository variable `UPDATE_DEPLOY_ENABLED=true`. The SSH host key must be pinned from the hosting provider's trusted console. The upload user should be confined to the update site's directory and must not have a shell or access to bot, database, or VPN files.
6. Increase Android `versionCode` for every release and set `versionName` to the release version. Push a matching tag such as `v1.0.0`. Actions signs the APK, verifies it, creates `update.json`, saves a build artifact, then publishes the APK before atomically replacing the manifest.

## What users see

The first install is a normal APK install from the website. Android may ask the user to allow ByMeVPN as an install source. Later updates are checked when the app returns to the foreground, downloaded from the ByMeVPN HTTPS site, validated, and handed to Android's package installer. Android may still show a confirmation screen; ordinary third-party apps cannot promise silent installation on personal phones. Do not ask users to install updates from GitHub Actions or GitHub Releases. Before any future Google Play build, remove or separate `REQUEST_INSTALL_PACKAGES` and this direct-download updater according to Play's current rules.

## Release safety

Do not enable the update deployment job until the HTTPS manifest and downloads directory have been tested with a signed release APK. Never rotate the release signing key without an Android signing-key migration plan. Verify update installation from an existing signed APK before announcing a release. The GitHub Actions artifact is for release review and is not the user download channel.

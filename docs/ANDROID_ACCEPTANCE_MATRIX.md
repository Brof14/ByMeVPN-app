# ByMeVPN Android — release acceptance matrix

This is a **manual release gate**, not a report that tests have passed. Run against a *signed* APK on physical Android devices with the current staging/production server configuration and a dedicated test account. Never use a real customer's payment method or export subscription URLs, UUIDs, Reality keys, bearer tokens or full logcat into issues.

## Build and supply chain

- [ ] `gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` succeeds.
- [ ] Signed release GitHub Action verifies SHA-256 certificate fingerprint against the last working APK.
- [ ] `SHA256SUMS.txt` matches uploaded APK bytes and `update.json` points to this exact release.
- [ ] Upgrade in place from previous signed release preserves encrypted account credentials, VPN preferences and the active node.
- [ ] Record the `sha256sum app/libs/libv2ray.aar` output and obtain provenance/version/ABI evidence from the native core builder. A checksum alone is **not** provenance.

## Functional matrix — mark each case pass/fail with device details

| Case | Required check | Pass condition |
| --- | --- | --- |
| New account | Google / email / Telegram flow + trial | Server confirms correct identity, entitlement and device, no purchase prompt for an existing paid account |
| Device limit | 2, 5, 10 slots; remove; re-register | Correct confirmed server limit, no accidental removal, cannot bypass or charge the wrong account |
| External import | Single `vless://` and HTTPS subscription | No ByMeVPN account endpoints, data survives 401 and app restart, user can delete/replace profile |
| Tunnel connected | Enable app, open **another** application/browser | Real IPv4 public IP changes to expected VPN egress; local SOCKS health check alone is insufficient |
| IPv6 | Both native IPv6 and IPv6-only destination | IPv6 uses VPN egress or is blocked as designed; never bypasses in a claimed protected mode |
| DNS | Native resolver, IPv4/IPv6, multiple providers | Queries use intended tunnel DNS; check and document split-tunnel/Russian-bypass exceptions |
| Wi-Fi handover | Wi-Fi → cellular → Wi-Fi | No unprotected gap when Android lockdown is enabled; eventual VPN recovery or explicit failure |
| Offline | Airplane mode 2 minutes, restore | No false permanent connected state, no hanging notification, no data loss |
| Process death | Kill app process while VPN on | System Always-on restores or fails cleanly; no permanently stuck CONNECTING |
| Permission revoked | Revoke system VPN permission | TUN and foreground service end; UI reports permission error |
| Compact screens | 320–360dp width and 500dp height | No clipped payment CTA, login, server selection or disconnect action |
| Accessibility | 200% font, TalkBack, reduced motion | Critical labels readable; focus and buttons usable |
| Backend down | 401 with refresh 503/timeout; normal 503 | Cached identity not erased on transient errors, no cross-account retry |
| Billing | YooKassa test invoice, duplicate webhook, refund | Exactly one fulfillment, no false confirmation, correct tariff/device tier |
| Auto-renew | Existing bot-managed recurring profile | Shows only verified state; cancellation preserves paid access; no invented next charge |
| Prorated upgrade | 2→5 and 5→10 on existing key | Price confirmed by server, one charge, remainder preserved |
| Support report | User explicitly consents to diagnostics | Only allowed standardized stage/error/version; no raw logs or HWID header |

## Operator procedure (PowerShell / Windows)

1. Enable Android USB debugging on your own test phone, install the signed APK, and run `powershell -File scripts/Verify-AndroidVpn.ps1`. This prints package and high-level VPN transport diagnostics only.
2. Record the device, Android version, connection protocol, Wi-Fi/mobile transport, and release hash. Do not record subscriber identities.
3. Press **Connect** in ByMeVPN, then run `powershell -File scripts/Verify-AndroidVpn.ps1 -OpenLeakTests` to open IP/DNS/IPv6 browser test sites in another app. Compare outcomes while connected and disconnected.
4. Perform handover, process death, Always-on + lockdown, network loss and permission-revoke cases above. Write down **actual** pass/fail outcomes, not code-based predictions.
5. For payments use a dedicated sandbox/staging environment and test account. Compare Android payment intent, backend invoice, webhook idempotency, entitlement and panel device count; never create a live charge without a controlled test procedure.

## Release rule

Passing GitHub Actions is necessary but **not sufficient** to assert a leak-free tunnel, correct VPN routing, payment safety or 100/100 UX. Leave untested rows unchecked. Fail a release if a previously supported protocol stops routing real app traffic, a live VPN cannot be disconnected, credentials are lost after a network timeout, or a billing mutation may duplicate charges.

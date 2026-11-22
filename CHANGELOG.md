# Changelog: ByMeVPN Android — Master Project Improvement

All notable changes to the ByMeVPN Android project are documented in this file.
This release follows the **PARTIAL Redesign / HIGH Freedom / BALANCED-PLUS Priority** directive, significantly elevating UI/UX, animations, tactile feedback, settings architecture, real-time telemetry, and test coverage while strictly preserving backend contracts and core VPN protocol integrity.

---

## [2.1.0-enhanced] - 2026-11-22

### 🎨 Visual Design & UI System
- **Unified Design Tokens (`AppColors`)**:
  - Replaced hardcoded hex values in `HomeScreen`, `SupportScreen`, `PricingScreen`, and `LiveSpeedWave` with standardized semantic tokens (`AppColors.BgCanvas`, `AppColors.CardSurface`, `AppColors.Teal`, `AppColors.NeonTeal`, `AppColors.TextPrimary`, `AppColors.TextSecondary`, `AppColors.TextMuted`).
  - Standardized elevation, card borders (`0.8.dp`), and corner radii across all screens (22dp on containers, 14–16dp on interactive tiles).
- **Extracted `HomeBottomDock` Component**:
  - Eliminated ~200 lines of duplicated location selector and subscription status cards from `HomeScreen.kt`.
  - Consolidated into a modular, highly readable, and maintainable bottom dock layout with verified minimum 48dp touch targets.
- **Redesigned Status Banners**:
  - Polished `HomeConnectedContent`, `HomeDisconnectedContent`, `HomeReconnectingContent`, and `HomeConnectionErrorContent`.
  - Added subtle glowing ambient indicator circles and readable typographical hierarchy matching `DESIGN.md`.

---

### ✨ Animations & Tactile Interaction (`ByMeHaptics`)
- **Physics-Based VPN Power Button (`VpnPowerButton.kt`)**:
  - Implemented `MutableInteractionSource` + `collectIsPressedAsState()` for instantaneous pointer-down feedback (scales to `0.95f` instantly without lag, following `emil-design-eng` / `apple-design` principles).
  - Tuned spring and cubic-bezier transition curves (`280ms`, `FastOutSlowInEasing`) for state changes (`DISCONNECTED`, `CONNECTING`, `CONNECTED`, `RECONNECTING`).
  - Added distinct tactile haptics on press, connection established, and disconnection.
- **Dedicated Tactile Haptic Engine (`ByMeHaptics.kt`)**:
  - Created a robust haptic helper utilizing Jetpack Compose's `LocalHapticFeedback` with graceful fallback to system `Vibrator` / `VibratorManager` on Android API 26–34+.
  - Applied tactile feedback across key touchpoints:
    - Server item selection and automatic server toggle in `ServerSelectionSheet`.
    - Manual ping refresh trigger.
    - Plan and device count selectors in `PricingScreen`.
    - Primary action and dialog buttons in `SupportScreen`.
    - Toggles and dialog selectors in `AdvancedSettingsScreen`.

---

### 📊 Real-Time Telemetry & Performance
- **Live Network Speed & Traffic Wave (`LiveSpeedWave.kt`)**:
  - Activated previously dormant telemetry stream from `VpnManager`: connected `bytesIn`, `bytesOut`, `downloadSpeedBps`, and `uploadSpeedBps` into `HomeScreen`.
  - Replaced ad-hoc path allocation in `Canvas` draw loop with `remember { Path() }` to eliminate garbage collection pressure during continuous 60fps wave rendering.
  - Implemented bilingual formatters (`formatBitrate`, `formatVolume`) supporting `Kbps`/`Mbps`/`Gbps` and `Кбит/с`/`Мбит/с`/`Гбит/с`, as well as `MB`/`GB` / `МБ`/`ГБ`.
  - Restrained animation to fire only when `isConnected = true`, pausing phase iteration when disconnected to conserve battery.

---

### ⚙️ Architecture & Advanced Settings
- **Full-Featured Advanced Settings (`AdvancedSettingsScreen.kt`)**:
  - Transformed the placeholder screen into a comprehensive settings dashboard.
  - **DNS Provider Selection**: Integrated interactive selection modal supporting Cloudflare DNS (`1.1.1.1`), AdGuard DNS (`94.140.14.14` with ad-blocking), and Google DNS (`8.8.8.8`), persisting directly via `SettingsRepository`.
  - **Auto-Reconnect Switch**: User toggle for automatic VPN tunnel restoration upon network transitions or unexpected disconnection.
  - **Status & Speed Notifications**: Granular control over foreground notification detail (live speed readout toggle and connection alert switches).
  - **System Kill Switch / Always-On Link**: Quick-launch shortcut to Android's native system VPN settings (`android.net.VpnService.SERVICE_INTERFACE` / `Settings.ACTION_VPN_SETTINGS`).
- **Settings Navigation Wiring**:
  - Added "Дополнительно / Advanced" entry in `SettingsScreen.kt` with icon and navigation callback.
  - Wired route in `MainActivity.kt` with smooth navigation transitions.

---

### 🛡️ Core Stability & Backend Compatibility
- **Zero Breaking API Changes**:
  - Preserved all existing backend contracts (`ByMeApiClient`): `/auth/telegram/*`, `/auth/google`, `/subscription/devices`, `/subscription/status`, `/servers`, `/payment/yookassa/*`.
  - Preserved Telegram deep-linking (`tg://resolve?domain=...`) and external browser fallback.
- **Xray Engine & VpnService Integrity**:
  - Tuned `XrayConfigGenerator` DNS server array generation to dynamically match user-selected DNS mode (`cloudflare`, `adguard`, `google`).
  - Ensured routing rules for Russian traffic bypass (`geosite:category-ru`, `geoip:ru`) and reality stream settings remain strictly compliant.

---

### 🧪 Automated Testing & QA
- **New Unit Test Suites**:
  - `DnsModePolicyTest.kt`: Validates Xray JSON generation for Cloudflare, AdGuard, and Google DNS configurations, checking server IP lists, IPv4 query strategy, and TUN sniffing settings.
  - `LiveSpeedWaveTelemetryTest.kt`: Unit tests for telemetry formatters covering boundary conditions (0 bps, sub-megabit, high speed, gigabyte threshold, Russian and English localization).
- **Code Hygiene**:
  - Stripped unintended UTF-8 BOM characters across all source and test files.
  - Ensured strict null-safety and exception resilience across UI event handlers.

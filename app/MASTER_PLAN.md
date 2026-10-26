# Master Plan: ByMeVPN Luxury UI/UX Redesign

## 1. Architectural Alignment
- Align theme, layouts, and typography directly with the Figma Desktop UI design system adapted for mobile.
- Theme:
  - Deep carbon/emerald canvas: `#080C0E` / `#0B1015`
  - Cards & containers: `#101A1F` with borders `#16282E`
  - Neon lime/mint accents: `#26E875` -> `#6EFFA6`
  - Brand shield: Exact gradient matching logo (Royal Blue `#0062FF` -> Cyan `#00ACFF` -> Mint `#00FFA2` -> Neon Green `#26E875`)
  - Typography: Pure White `#FFFFFF`, Slate Grey `#8B9FA8`, Neon Green `#26E875`

## 2. Screens Implementation
1. `WelcomeScreen.kt` (01 Добро пожаловать):
   - ByMeVPN Shield & infinity logo with subtle pulsating ambient glow
   - "Создайте подписку в Telegram" primary green button
   - "Подключить аккаунт" button & paste subscription input field
2. `SignInScreen.kt` & `SignUpScreen.kt` (02 Вход / 03 Регистрация):
   - Fast Telegram Auth button ("Войти через Telegram-бота")
   - Email & Password input fields with dark container styling
   - Google Sign-In with Credential Manager
3. `HomeScreen.kt` (04 / 05 Подключение):
   - Giant central shield button with neon trace pulse and haptic feedback
   - Status text: "VPN выключен" / "VPN подключён • Соединение защищено • 00:18:42"
   - Real-time animated speed graph / telemetry: ↓ Mbit/s, ↑ Mbit/s
   - Selected server card with ping badge, country flag, and "Изменить →"
   - Active subscription status card: "Доступ активен • Подписка до 24 ноября 2026 • 2 из 5 устройств • Продлить в Telegram →"
4. `ServerSelectionModal` (06 Выбор сервера):
   - Search bar "Найти сервер"
   - "Автоматически (Рекомендуется • лучший доступный пинг)" with checkmark
   - "Оптимальный сервер"
   - "Текущий сервер" + Full list of servers from 3x-ui subscription
   - Real-time TCP latency measurement
5. `AccountScreen.kt` & `TariffModal` (07 Аккаунт / 08 Тарифы):
   - Telegram account card with @username & active badge
   - Current subscription period card
   - Connected devices indicator (dots 2 из 5)
   - "Продлить или изменить тариф" -> Opens Bot or shows Tariff selector (2, 5, 10 devices / 1, 3, 6, 12 months with "ВЫГОДНЕЕ")
   - Support button -> @ByMeVPNSupportBot
6. `SettingsScreen.kt` (09 Настройки):
   - "Запускать вместе с системой"
   - "Подключаться автоматически"
   - "Маршрутизация (Российские сервисы открываются напрямую)"
   - "Язык интерфейса"
   - "Помощь и поддержка"

# SKILLS AUDIT — BYMEVPN ANDROID

**Дата проведения аудита:** 22 ноября 2026 г.  
**Проект:** ByMeVPN Android Client (Kotlin / Jetpack Compose / Material3 / VpnService / Xray Core)  
**Репозиторий:** `ByMeVPN-app-main`

---

## 1. Инвентаризация доступных локальных скиллов

В локальной среде Antigravity (каталоги `C:\Users\User\.gemini\config\skills`, `C:\Users\User\.gemini\config\plugins\egm\skills`, встроенные системные скиллы) обнаружен полный каталог инструментов.

### 1.1. Классификация и категоризация

| Категория | Количество | Примеры | Статус применимости | Причина |
|---|---|---|---|---|
| **Игровые движки и механики** | 35+ | `godot-*`, `unity-*`, `unreal-*`, `roblox-*`, `phaser-*`, `pixijs-*`, `bevy-*`, `defold-*`, `love2d-*`, `pygame-*`, `platformer`, `level-design`, `rpg`, `card-game` | **Исключены** | Не применимы к нативной разработке системного VPN-клиента под Android |
| **Несовместимые платформы и стеки** | 5 | `write-swift` (iOS/Swift), `animate-expo` (React Native/Expo), `ask-sonner` (React web toast), `defold-core` (Lua), `ebitengine-game` (Go) | **Исключены** | Проект реализован на 100% нативном Kotlin + Jetpack Compose |
| **Медиа и видеообработка** | 2 | `ffmpeg-skill`, `video-use` | **Исключены** | В приложении нет задач локального видеомонтажа или кодирования дорожек |
| **Дисциплина разработки** | 1 | `ub-mode` | **Применён** | Принципы «First Make it Work, Then Make it Right, Follow Practice», запрет на невозможные состояния, защита границ |
| **Дизайн, UX и анимации** | 8 | `emil-design-eng`, `apple-design`, `mobile-native`, `break-ui`, `animation-performance`, `choreograph`, `animation-vocabulary`, `remove-animations` | **Применены** | Проектирование тактильного отклика кнопок (scale-on-press), пружинных физических переходов, Compose graphicsLayer, исключение визуального мусора |
| **Экосистемное выравнивание** | 1 | `bymevpn-windows` | **Применён** | Синхронизация протокольных контрактов, логики серверов, статусных формулировок и архитектуры ByMeVPN |
| **Рецензирование и верификация** | 4 | `egm-review`, `egm-codebase-details`, `egm-synthesizer`, `break-ui` | **Применены** | Многофакторный аудит архитектуры, устойчивости граничных случаев и проверки качества |

---

## 2. Применимые скиллы и обоснование их использования

### 2.1. `ub-mode` (Engineering Discipline)
- **Файл:** `C:\Users\User\.gemini\config\skills\ub-mode\SKILL.md`
- **Зачем используется:** 
  - Формирование структур данных, делающих невозможные состояния невозможными (`illegal states impossible`).
  - Проверка данных на внешних границах (парсинг backend-ответов и deep link `bymevpn://*`) и доверие внутри.
  - Идемпотентность всех операций подключения, отключения и смены серверов.
  - Устранение корневых причин сбоев, а не навешивание «защитных заплаток».
  - Разделение разделяемого состояния перед сериализацией.

### 2.2. `emil-design-eng` (Design Engineering & Polish)
- **Файл:** `C:\Users\User\.gemini\config\skills\emil-design-eng\SKILL.md`
- **Зачем используется:**
  - Принцип «Unseen details compound»: тактильный отклик компонентов при нажатии (`graphicsLayer` scale 0.96f на pointer down).
  - Ограничение длительности переходов интерфейса до 300 мс (за исключением модальных шторок).
  - Использование кастомных сглаживающих кривых (Custom `CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` вместо дефолтных смазанных кривых).
  - Повышение субъективно воспринимаемой скорости (perceived speed).

### 2.3. `apple-design` (Fluid Physical Motion & Interruptibility)
- **Файл:** `C:\Users\User\.gemini\config\skills\apple-design\SKILL.md`
- **Зачем используется:**
  - Непрерывный отклик на нажатие (feedback на pointer-down, а не при отпускании).
  - Прерываемость анимаций: переход от текущего экранного значения (presentation value), а не сброс в ноль.
  - Использование физических пружин (Spring spec: `dampingRatio = 0.85f`, `stiffness = Spring.StiffnessMediumLow`).

### 2.4. `mobile-native` (Native Android Feel & Safe Areas)
- **Файл:** `C:\Users\User\.gemini\config\skills\mobile-native\SKILL.md`
- **Зачем используется:**
  - Интеграция Edge-to-Edge (`enableEdgeToEdge()`) с правильной обработкой `WindowInsets` (status bars, navigation bars, display cutout/notch).
  - Минимальные размеры интерактивных областей (touch targets) не менее 48×48 dp.
  - Тактильная вибрация (Haptic feedback) при переключении VPN и подтверждении действий.
  - Исключение задержек и артефактов при быстрых повторных тапах.

### 2.5. `break-ui` (Adversarial Data & Stress Testing)
- **Файл:** `C:\Users\User\.gemini\config\skills\break-ui\SKILL.md`
- **Зачем используется:**
  - Проверка поведения экранов на стрессовых граничных данных: отсутствие серверов, нулевой пинг, отрицательный пинг (`-1`, `-2`, `-3`, `-4`, `-5`), огромные значения трафика (терабайты/петабайты), экстремально длинные имена локаций, отсутствие интернет-подключения, неподтверждённый статус подписки.
  - Тестирование локализации на русском и английском языках (переполнение контейнеров и разрыв строк).

### 2.6. `animation-performance` (Hardware Acceleration & Zero-Allocations)
- **Файл:** `C:\Users\User\.gemini\config\skills\animation-performance\SKILL.md`
- **Зачем используется:**
  - Анимации только по композитным свойствам (`scale`, `alpha`, `translationX/Y` через `Modifier.graphicsLayer`).
  - Недопущение реаллокаций объектов (`Path`, `Paint`, `Brush`) внутри циклов отрисовки `Canvas.DrawScope`.
  - Устранение лишних recomposition в Jetpack Compose.

### 2.7. `remove-animations` (Subtractive Motion Polish)
- **Файл:** `C:\Users\User\.gemini\config\skills\remove-animations\SKILL.md`
- **Зачем используется:**
  - Удаление избыточных декоративных анимаций, отвлекающих пользователя или замедляющих доступ к функциям.
  - Сохранение только тех движений, которые несут функциональный смысл и отображают реальное физическое состояние туннеля.

### 2.8. `bymevpn-windows` (Cross-Platform Alignment)
- **Файл:** `C:\Users\User\.gemini\config\skills\bymevpn-windows\SKILL.md`
- **Зачем используется:**
  - Соблюдение единых соглашений по протоколам (VLESS Reality over TCP/gRPC/XHTTP, Hysteria 2).
  - Идентичность отображения статусов, времени жизни сессий, валидации конфигураций.
  - Недопущение подмены реального состояния VPN фиктивными индикаторами.

---

## 3. Исключённые инструменты и причины

1. **Все игровые движки (`godot-*`, `unity-*`, `unreal-*`, `roblox-*`, `phaser-*`, `pixijs-*`, `bevy-*`):**  
   *Причина:* Проект является Android-приложением на Kotlin / Jetpack Compose.
2. **`write-swift`:**  
   *Причина:* Стек iOS/macOS; Android проект не использует Swift.
3. **`animate-expo`:**  
   *Причина:* Стек React Native/Expo; проект написан на чистом нативном Compose.
4. **`ask-sonner`:**  
   *Причина:* Библиотека всплывающих уведомлений для Web React; на Android используются нативные `Toast` и Compose `SnackbarHost`.
5. **`ffmpeg-skill`:**  
   *Причина:* В VPN-клиенте нет видеофайлов, транскодирования и аудиодорожек.

---

## 4. Сводный итог инвентаризации

Все релевантные скиллы подключены к работе на соответствующих этапах:
- `ub-mode` руководит проектированием архитектуры и обработкой состояний.
- `emil-design-eng` и `apple-design` задают стандарты микро-взаимодействий, кинетики кнопок и типографики.
- `mobile-native` обеспечивает адаптивность, safe areas и тактильный отклик.
- `break-ui` управляет проверкой граничных условий и обработкой сбоев.
- `bymevpn-windows` гарантирует экосистемную целостность ByMeVPN.

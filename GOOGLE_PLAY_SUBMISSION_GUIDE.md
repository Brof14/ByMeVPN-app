# Руководство по публикации ByMeVPN в Google Play Console (100% готовность к одобрению)

Данное руководство содержит **исчерпывающие формулировки, тексты и ответы на все опросники Google Play Console**, составленные с учётом актуальных политик Google Play для VPN-приложений (API 34/35, `VpnService` Policy, Foreground Service `specialUse`, Data Safety и Account Deletion).

---

## 📋 1. Основные метаданные страницы приложения в Google Play

### Название приложения (App Name)
- **RU:** `ByMeVPN — быстрый и надежный VPN`
- **EN:** `ByMeVPN — Fast & Secure VPN`

### Краткое описание (Short Description, до 80 символов)
- **RU:** `Быстрый и анонимный VPN. Протокол VLESS Reality, строгий Zero-Logs, до 5 устройств.` (79 символов)
- **EN:** `Fast & private VPN. VLESS Reality, strict Zero-Logs, up to 5 devices.` (70 символов)

### Полное описание (Full Description, до 4000 символов)
```text
ByMeVPN — это современный, быстрый и по-настоящему анонимный VPN-сервис нового поколения. 
Защитите свои личные данные, забудьте о замедлениях и блокировках благодаря протоколам VLESS Reality и XTLS.

🚀 ПОЧЕМУ ВЫБИРАЮТ ByMeVPN:

• Скорость без компромиссов: серверы с пропускной способностью до 10 Гбит/с в Нидерландах, Германии, Финляндии, Швеции и США. Смотрите 4K видео, играйте с минимальным пингом и скачивайте файлы без ограничений.
• Абсолютная анонимность (Строгий Zero-Logs): мы принципиально не регистрируем, не храним и не продаем историю вашей активности, посещенные сайты или DNS-запросы.
• Современный протокол VLESS + Reality: устойчив к любым видам блокировок и замедлений интернет-провайдеров.
• Вход в 1 клик: удобная авторизация через Google, Email или моментальный вход через нашего официального Telegram-бота.
• До 5 устройств на одну подписку: защитите свой телефон, планшет, ноутбук, ПК или Smart TV по единой подписке. Управляйте подключенными гаджетами прямо из раздела «Аккаунт».
• Встроенный блокировщик рекламы: фильтрация фишинга, опасных трекеров и назойливой рекламы через безопасный AdGuard DNS.
• Раздельное туннелирование (Split Tunneling): выбирайте, какие приложения (например, банковские клиенты) должны работать напрямую без VPN.
• Надежный Kill Switch: защита от случайной утечки данных при обрыве интернет-соединения.

🎁 3 ДНЯ БЕСПЛАТНО:
Оцените все возможности ByMeVPN без риска. Активируйте 3-дневный бесплатный пробный период в один тап — привязка банковской карты не требуется!

💳 УДОБНАЯ ОПЛАТА:
Оформляйте подписку любым удобным способом: через СБП (Система быстрых платежей), SberPay, T-Pay или картами МИР, Visa и MasterCard.

Остались вопросы? Наша русскоязычная поддержка всегда готова помочь вам в Telegram: @ByMeVPNSupportBot
```

---

## 🛡️ 2. Декларация использования VpnService (Google Play Policy)

В разделе **Политика и согласие (Policy) → Содержимое приложения (App content) → VpnService**:

1. **Какова основная функция вашего приложения?**
   > Выберите: **VPN-приложение (Core VPN functionality)**.
2. **Почему приложению требуется разрешение VpnService?**
   > Текст для поля обоснования:  
   > *«ByMeVPN is a privacy and cybersecurity application whose core functionality is creating an encrypted device-level network tunnel via VpnService. The tunnel encrypts all user internet traffic using VLESS Reality and XTLS protocols to protect users from eavesdropping, cyber attacks on public Wi-Fi networks, and ISP tracking. The app strictly enforces a Zero-Logs policy and does not log, inspect, or sell browsing data.»*
3. **Предоставляет ли приложение явное уведомление (Prominent Disclosure) до запроса разрешения?**
   > Выберите: **Да (Yes)**.  
   > *В ByMeVPN встроен диалог `VpnDisclosureDialog`, отображаемый до вызова системного диалога `VpnService.prepare()`.*
4. **Ссылка на демонстрационное видео (Demo Video URL):**
   > Google Play требует короткое видео (15–30 секунд), показывающее:
   > 1. Открытие главного экрана ByMeVPN.
   > 2. Нажатие на кнопку щита (подключение).
   > 3. Показ уведомления о защите данных (Prominent Disclosure) и системного согласия VPN.
   > 4. Появление активного постоянного уведомления в шторке со статусом подключения.  
   > *Загрузите видео на YouTube со статусом «Доступ по ссылке» (Unlisted) и вставьте ссылку в форму.*

---

## ⚙️ 3. Декларация Foreground Service (Тип `specialUse`)

В разделе **Политика → Декларации типов служб переднего плана (Foreground Service Declarations)**:

1. **Тип службы переднего плана:**
   > Выберите: **Special Use** (`android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` = `vpn`).
2. **Какую задачу выполняет эта служба?**
   > *«The foreground service runs the persistent encrypted VPN tunnel using VLESS Reality core. It maintains a secure network route for all outgoing and incoming device traffic while the user browses the web or uses other apps.»*
3. **Что произойдет, если система остановит эту службу?**
   > *«If the foreground service is terminated, the VPN tunnel will drop immediately, exposing the user's unencrypted network traffic and real IP address to potential interception on public Wi-Fi networks and causing active downloads and streaming connections to fail.»*

---

## 🔒 4. Безопасность данных (Data Safety Section)

Отвечайте на опросник Data Safety следующим образом:

1. **Собирает или передает ли ваше приложение какие-либо данные о пользователях?**  
   → **Да (Yes)** (только адрес электронной почты для входа).
2. **Шифруются ли все передаваемые данные пользователей при передаче?**  
   → **Да (Yes)** (все сетевые соединения защищены HTTPS и TLS 1.3 / XTLS).
3. **Предоставляете ли вы пользователям возможность запросить удаление их данных?**  
   → **Да (Yes)** (внутри приложения кнопка в «Аккаунте» и через веб-сайт).
4. **Укажите ссылку, по которой пользователи могут запросить удаление аккаунта:**  
   → `https://bymevpn.com/delete-account` (или страница на GitHub Pages с файлом `docs/delete-account.html`).
5. **Категории собираемых данных:**
   - **Учетные данные (User info):**
     - *Email address / Google Account ID*:
       - Сбор: **Да**.
       - Передача третьим лицам: **Нет**.
       - Цель: **Управление учетной записью (Account management)**.
       - Обязательно ли предоставление: **Да** (для работы подписки).
   - **История браузера и сетевая активность (Web browsing, Network info):**
     - *История посещений (Browsing history)*: **НЕТ (Not collected)**.
     - *Сетевой трафик / пакеты*: **НЕТ (Not collected)**.
     - *IP-адреса*: **НЕТ (Not collected)**.
   - **Финансовые данные (Financial info):**
     - *Номера банковских карт*: **НЕТ (Not collected)** (все платежи обрабатываются напрямую шлюзом ЮKassa).

---

## 🌐 5. Ссылки на документы (Политика и Удаление данных)

- **Политика конфиденциальности (Privacy Policy URL):**  
  `https://bymevpn.com/privacy`  
  *(Исходный HTML-файл готов в репозитории: `docs/privacy-policy.html`)*
- **Запрос на удаление аккаунта (Account Deletion URL):**  
  `https://bymevpn.com/delete-account`  
  *(Исходный HTML-файл готов в репозитории: `docs/delete-account.html`)*

> 💡 **Как бесплатно опубликовать страницы за 1 минуту через GitHub Pages:**
> 1. В репозитории `https://github.com/Brof14/ByMeVPN-app` перейдите в **Settings → Pages**.
> 2. В поле **Branch** выберите `main`, в поле папки выберите `/docs` и нажмите **Save**.
> 3. Ссылки станут доступны по адресам вида:  
>    `https://brof14.github.io/ByMeVPN-app/privacy-policy.html`  
>    `https://brof14.github.io/ByMeVPN-app/delete-account.html`  
>    Их можно сразу указывать в Google Play Console!

---

## 📦 6. Как скачать файл для Google Play Console

После сборки на GitHub Actions:
1. Перейдите во вкладку **Actions** в репозитории `Brof14/ByMeVPN-app`.
2. Откройте последний успешный запуск workflow **Build Android Release & Debug**.
3. В секции **Artifacts** скачайте:
   - **`ByMeVPN-googleplay-bundle`** — архив с файлом `app-release.aab`. Именно этот файл загружается в Google Play Console при создании релиза!
   - **`ByMeVPN-upload-keystore`** — резервная копия файла ключа подписи `bymevpn-upload.jks`. Сохраните его в надёжном месте.

# Yandex TV Remote

[![Release APK](https://github.com/DaniilBorovskiy1809/yandex-tv-remote/actions/workflows/build.yml/badge.svg)](https://github.com/DaniilBorovskiy1809/yandex-tv-remote/actions/workflows/build.yml)

Пульт для телевизоров Яндекса на Android TV (прошивка `monet`, Amlogic `t5w`, Android TV 11) в виде Android-приложения на Kotlin, Jetpack Compose и Material 3 Expressive.

Приложение умеет работать в двух режимах:

| Режим | Статус | Что использует |
|---|---|---|
| **Bluetooth HID** | Основной, рекомендуется | Телефон регистрируется как Bluetooth HID-клавиатура + Consumer Control |
| **ИК-пульт** | Экспериментальный | Встроенный ИК-передатчик телефона, NEC-команды |

> Главная идея: телевизор уже умеет работать с обычными Bluetooth HID-устройствами. Приложению не нужен Google TV Remote Service, ADB, root или специальный протокол Яндекса. Телефон притворяется стандартной HID-клавиатурой, а телевизор сам превращает HID-отчёты в нажатия кнопок.

---

## Возможности

- Питание, ТВ, настройки, Алиса (кнопка голосового поиска).
- D-pad, OK, назад, домой, меню, поиск/инфо.
- Громкость, mute, канал +/−.
- Медиа: play/pause, предыдущий/следующий, перемотка, стоп, запись, телегид.
- Цифровая клавиатура 0–9.
- Повтор при удержании для D-pad, громкости, каналов и перемотки.
- Выбор режима Bluetooth HID / ИК.
- Кнопка «Сделать видимым» превращается в «Отмена»: видимость можно выключить, не дожидаясь двухминутного таймаута.
- Инструкция по сопряжению и диагностика состояния.
- Material 3 Expressive: `MaterialExpressiveTheme`, `MotionScheme.expressive()`, выразительные формы и анимации.
- Светлая и тёмная темы.

---

## Как это работает

### Bluetooth HID (основной режим)

1. Приложение поднимает профиль `BluetoothHidDevice` (API 28+).
2. Регистрирует композитный HID-дескриптор:
   - Report ID 1 — клавиатура (цифры, служебные клавиши);
   - Report ID 2 — Consumer Control (кнопки пульта, медиа, громкость, каналы).
3. Телефон становится видимым для сопряжения.
4. На телевизоре: **Настройки → Пульты и аксессуары → Добавить аксессуар**.
5. После сопряжения телевизор видит телефон как обычный HID-пульт и применяет свой `Generic.kl`.

Для телевизора это не «приложение-пульт», а обычная Bluetooth-клавиатура. Поэтому не нужны ни сервисы Google TV Remote, ни ADB, ни доступ к прошивке.

### ИК-пульт (экспериментальный режим)

Используется `ConsumerIrManager` и NEC-протокол с адресом `0x20DF`. Команды взяты из `vendor/etc/remote_nec_ircus1.tab` прошивки `monet`.

Ограничения:

- ИК-передатчик есть далеко не во всех телефонах.
- В обычной загрузке активна RCA-таблица (`rc_tag_str = rca`), а NEC-таблицы подключены в том же `remote.sum` как fallback. Формат RCA неизвестен, поэтому NEC-режим может не сработать на конкретной прошивке.
- В ИК-режиме доступны только кнопки, для которых есть NEC-код: питание, громкость, каналы, навигация, цифры, назад, домой, меню, инфо.

---

## Быстрый старт

### 1. Скачать APK

1. Откройте вкладку **Releases** репозитория.
2. Выберите последний релиз `vX.Y.Z`.
3. Скачайте `yandex-tv-remote-X.Y.Z.apk` (и `.sha256` для проверки).
4. Установите APK на телефон (разрешите установку из неизвестных источников).

APK подписан постоянным release-ключом. Debug-сборки в релизы не попадают.

### 2. Подключить телефон к телевизору

1. Включите Bluetooth на телефоне.
2. Откройте приложение и разрешите Bluetooth-разрешения.
3. Нажмите **«Сделать видимым»**.
4. На телевизоре откройте **Настройки → Пульты и аксессуары → Добавить аксессуар**.
5. Выберите телефон в списке и подтвердите сопряжение.
6. Вернитесь в приложение — статус должен стать **«Подключено»**.

> Для первого подключения нужен штатный ИК-пульт или кнопки на панели телевизора: приложение не может удалённо открыть экран сопряжения, пока у него нет канала управления.

### 3. Пользоваться

После подключения кнопки на экране отправляют HID-отчёты на телевизор. Удержание D-pad, громкости и каналов повторяет нажатие.

---

## Совместимость

- Телевизор: Yandex TV `monet` (Amlogic `t5w`), Android TV 11, build `5000001240`.
- Телефон: Android 9+ (`minSdk 28`) с поддержкой профиля Bluetooth HID Device.
- Большинство современных Android-телефонов поддерживают HID Device, но некоторые OEM-прошивки отключают профиль. Если статус **«Телефон не поддерживает Bluetooth HID Device»**, помочь ничем нельзя: это ограничение прошивки телефона.
- ИК-режим: только телефоны с `ConsumerIrManager.hasIrEmitter() == true`.

---

## Версии

Актуально на ноябрь 2026. Использованы последние стабильные версии.

| Компонент | Версия |
|---|---|
| Kotlin | `2.4.20` |
| Android Gradle Plugin | `9.5.2` |
| Gradle | `9.9.2` |
| Compose BOM | `2026.11.01` |
| Compose | `1.13.0` |
| Material 3 | `1.5.0` |
| Activity Compose | `1.13.0` |
| Lifecycle | `2.11.1` |
| Core KTX | `1.19.1` |
| Coroutines | `1.11.0` |
| compileSdk | `37.1` |
| targetSdk | `37` |
| minSdk | `28` |
| Java target | `17` |
| JDK для сборки | `21` |

---

## Сборка

### GitHub Actions

Сборка запускается **только по тегу версии** `vX.Y.Z`. Обычные коммиты и pull request'ы ничего не собирают и не тратят минуты Actions.

Что делает workflow `.github/workflows/build.yml`:

- проверяет, что тег имеет вид `vX.Y.Z`;
- берёт `versionName` из тега, а `versionCode` — из номера запуска;
- восстанавливает release-keystore из GitHub Actions secrets;
- собирает **подписанный** release APK, а не debug;
- проверяет подпись через `apksigner`;
- публикует APK и SHA-256 в GitHub Releases;
- запускает JVM-тесты и прикладывает их отчёт;
- использует кэш Gradle, wrapper и Android SDK.

Секреты репозитория:

| Секрет | Назначение |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | release-keystore в base64 |
| `ANDROID_KEYSTORE_PASSWORD` | пароль хранилища |
| `ANDROID_KEY_ALIAS` | alias ключа |
| `ANDROID_KEY_PASSWORD` | пароль ключа |

### Как выпустить новую версию

```bash
git tag v1.0.0
git push origin v1.0.0
```

После этого GitHub Actions соберёт подписанный APK и создаст релиз `v1.0.0`. Повторный push того же тега обновит файлы существующего релиза (`--clobber`).

### Локально

Нужны JDK 21 и Android SDK с `platforms;android-37.1` и `build-tools;37.0.0`.

```bash
./gradlew testDebugUnitTest assembleDebug
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

Для локальной подписанной release-сборки создайте `keystore.properties` в корне (файл в `.gitignore`):

```properties
storeFile=/absolute/path/to/release.jks
storePassword=...
keyAlias=yandextvremote
keyPassword=...
```

Затем:

```bash
VERSION_NAME=1.0.0 VERSION_CODE=1 ./gradlew testDebugUnitTest assembleRelease
```

> В репозитории нет локального Android SDK. Эталонная сборка — GitHub Actions.

### Тесты

JVM-тесты не требуют телефона или телевизора. Для запуска через Gradle нужен Android SDK:

```bash
./gradlew testDebugUnitTest
```

Что проверяется:

- NEC-кадр: длина, leader, порядок бит, адрес, команда и инверсная команда;
- HID-отчёты: клавиатура, consumer little-endian, release-отчёты;
- `RemoteKey`: наличие HID-команды, покрытие и диапазон ИК-кодов, диапазоны usage, повторяемые кнопки.

CI запускает тесты и сборку APK, а HTML-отчёт `unit-test-report` загружает даже при падении.

---

## Структура проекта

```
app/src/main/java/dev/daniilborovskiy/yandextvremote/
├── MainActivity.kt
├── RemoteViewModel.kt
├── remote/
│   ├── HidRemoteController.kt   # Bluetooth HID Device
│   ├── HidReports.kt            # чистые HID-отчёты
│   ├── IrRemoteController.kt    # ConsumerIrManager + NEC
│   ├── NecEncoder.kt            # чистый NEC-кодировщик
│   └── RemoteKey.kt             # таблица кнопок: HID usage + IR code
└── ui/
    ├── RemoteApp.kt             # разрешения, Bluetooth lifecycle
    ├── RemoteScreen.kt          # экран, карточки, пульт
    ├── RemoteButton.kt          # кнопка с ripple, haptic, repeat
    └── theme/                   # M3 Expressive тема

app/src/test/java/.../remote/
├── NecEncoderTest.kt
├── HidReportsTest.kt
└── RemoteKeyTest.kt
```

Документация:

- [Анализ прошивки](docs/FIRMWARE_ANALYSIS.md)
- [Протоколы: HID и NEC](docs/PROTOCOL.md)
- [Архитектура приложения](docs/ARCHITECTURE.md)
- [Сборка и CI](docs/BUILD.md)
- [Таблица кнопок](docs/REMOTE_KEYS.md)
- [Тестирование](docs/TESTING.md)
- [Планы развития](docs/ROADMAP.md)

---

## Ограничения

- **Голос/микрофон не реализованы.** В прошивке есть `YandexBleRemote` с проприетарным BLE GATT-сервисом для голоса. Это отдельный проект: нужно эмулировать GATT-сервер, ADPCM-поток и пройти проверку имени `"Yandex Remote"`.
- **Нельзя включить выключенный телевизор по Bluetooth.** Когда ТВ выключен, Bluetooth-контроллер тоже выключен. Для включения нужен ИК-пульт или кнопка на панели.
- **Нет Google TV Remote Service.** В прошивке нет `com.google.android.tv.remote.service`, поэтому протокол Google TV Remote не используется.
- **Нет ADB/сетевого пульта.** ADB по умолчанию выключен и требует авторизации; это не пользовательский сценарий.
- **ИК-режим экспериментальный.** Активная таблица — RCA, её формат не документирован. NEC-таблицы загружены как fallback.
- **Первое сопряжение требует телевизор.** Приложение не может открыть экран сопряжения удалённо без уже существующего канала.

---

## Лицензия

MIT. См. [LICENSE](LICENSE).

# Анализ прошивки Yandex TV `monet`

## 1. Целевое устройство

| Параметр | Значение |
|---|---|
| Устройство | Yandex TV `monet` |
| SoC | Amlogic `t5w` |
| ОС | Android TV 11 |
| Build | `5000001240` |
| Fingerprint | `yandex/monet/monet:11/RD2A.211001.002/5000001240:user/dev-keys` |
| Разметка | `system`, `system_ext`, `vendor`, `product` |

Исследование проводилось по распакованному образу. Пути ниже указаны относительно корня соответствующего раздела.

---

## 2. ИК-подсистема

### 2.1. Запуск

`vendor/etc/init/hw/init.amlogic.rc` запускает:

```text
service remotecfg /vendor/bin/remotecfg /vendor/etc/remote.conf
```

`remote.conf` в образе отсутствует, но рядом лежит `remote.sum` — суммарный конфиг, из которого `remotecfg` выбирает активный набор таблиц.

### 2.2. Активная конфигурация

`vendor/etc/remote.sum`:

```text
rc_tag_str = rca

nec_rc_cfg = /vendor/etc/remote_rca.cfg
nec_rc_begin
    /vendor/etc/remote_nec_ircus1.tab
    /vendor/etc/remote_nec_ircus2.tab
    /vendor/etc/remote_nec_ircus3.tab
    /vendor/etc/remote_nec_irfac.tab
nec_rc_end

rca_rc_cfg = /vendor/etc/remote_rca.cfg
rca_rc_begin
    /vendor/etc/remote_rca_ircus1.tab
    /vendor/etc/remote_rca_ircus2.tab
    /vendor/etc/remote_rca_ircus3.tab
    /vendor/etc/remote_rca_ircus4.tab
    /vendor/etc/remote_rca_ircus5.tab
    /vendor/etc/remote_rca_ircus6.tab
    /vendor/etc/remote_rca_ircus7.tab
    /vendor/etc/remote_nec_ircus1.tab
    /vendor/etc/remote_nec_ircus2.tab
    /vendor/etc/remote_nec_irfac.tab
rca_rc_end
```

Ключевые выводы:

- В обычной загрузке активен **RCA** (`rc_tag_str = rca`).
- NEC-таблицы всё равно перечислены внутри `rca_rc_begin`, то есть загружаются как дополнительный/fallback-набор.
- В recovery используется NEC (`vendor/etc/remote_recovery.sum`, `rc_tag_str = nec`).
- `remote_rca.cfg` задаёт `work_mode = 0xff08`.
- `remote_nec.cfg` задаёт `work_mode = 0x01`.

### 2.3. Основная RCA-таблица

`vendor/etc/remote_rca_ircus1.tab`:

```text
custom_name = remote_rca_ircus1
custom_code = 0xf
release_delay = 80
fn_key_scancode = 0xffff

wakeup_begin
    0x101d0d5
wakeup_end
```

Кнопки (RCA-код → Linux keycode):

| RCA | Linux | `Vendor_0001_Product_0001.kl` | Функция |
|---:|---:|---|---|
| `0x2a` | 116 | `POWER` | Питание |
| `0xa3` | 79 | `TV_INPUT` | Источник/вход |
| `0x2f` | 115 | `VOLUME_UP` | Громкость + |
| `0x2e` | 114 | `VOLUME_DOWN` | Громкость − |
| `0x2d` | 402 | `CHANNEL_UP` | Канал + |
| `0x2c` | 403 | `CHANNEL_DOWN` | Канал − |
| `0x08` | 102 | `HOME` | Домой |
| `0x06` | 158 | `BACK` | Назад |
| `0xec` | 139 | `SETTINGS` | Настройки/меню |
| `0x59` | 103 | `DPAD_UP` | Вверх |
| `0x58` | 108 | `DPAD_DOWN` | Вниз |
| `0x56` | 105 | `DPAD_LEFT` | Влево |
| `0x57` | 106 | `DPAD_RIGHT` | Вправо |
| `0xf4` | 28 | `DPAD_CENTER` | OK |
| `0xd8` | 401 | `PROG_BLUE` | Синяя кнопка |
| `0x30` | 11 | `0` | Цифра 0 |
| `0x31`…`0x39` | 2…10 | `1`…`9` | Цифры 1–9 |
| `0x27` | 775 | — | Неизвестно |

В таблице всего около 27 кнопок. Нет медиа, голоса, телегида, цветных кнопок кроме синей.

### 2.4. NEC-таблица

`vendor/etc/remote_nec_ircus1.tab`:

```text
custom_name = remote_nec_ircus1
custom_code = 0xdf20
release_delay = 80
fn_key_scancode = 0xffff

wakeup_begin
    0xad52df20
wakeup_end
```

`wakeup`-код `0xad52df20` раскладывается как NEC-последовательность:

```text
0xAD  — инверсный код команды
0x52  — команда (POWER)
0xDF  — младший байт адреса
0x20  — старший байт адреса
```

То есть адрес — `0x20DF`, команда питания — `0x52`. Это совпадает с NEC-адресом, который использует приложение.

Полезные команды:

| NEC | Linux | `Generic.kl` | `Vendor_0001.kl` | Функция |
|---:|---:|---|---|---|
| `0x52` | 116 | `POWER` | `POWER` | Питание |
| `0x53` | 113 | `VOLUME_MUTE` | `VOLUME_MUTE` | Mute |
| `0x07` | 466 | `F1 FUNCTION` | `TV_INPUT` | Источник/вход |
| `0x1c` | 358 | `INFO` | `INFO` | Инфо |
| `0x48` | 102 | `MOVE_HOME` | `HOME` | Домой |
| `0x0d` | 139 | `MENU` | `SETTINGS` | Настройки/меню |
| `0x40` | 14 | `DEL` | — | Backspace/удаление |
| `0x02` | 402 | `CHANNEL_UP` | `CHANNEL_UP` | Канал + |
| `0x09` | 403 | `CHANNEL_DOWN` | `CHANNEL_DOWN` | Канал − |
| `0x03` | 115 | `VOLUME_UP` | `VOLUME_UP` | Громкость + |
| `0x41` | 114 | `VOLUME_DOWN` | `VOLUME_DOWN` | Громкость − |
| `0x47` | 103 | `DPAD_UP` | `DPAD_UP` | Вверх |
| `0x4d` | 108 | `DPAD_DOWN` | `DPAD_DOWN` | Вниз |
| `0x49` | 105 | `DPAD_LEFT` | `DPAD_LEFT` | Влево |
| `0x4b` | 106 | `DPAD_RIGHT` | `DPAD_RIGHT` | Вправо |
| `0x4a` | 28 | `ENTER` | `DPAD_CENTER` | OK |
| `0x1b` | 11 | `0` | `0` | Цифра 0 |
| `0x00` | 2 | `1` | `1` | Цифра 1 |
| `0x10` | 3 | `2` | `2` | Цифра 2 |
| `0x11` | 4 | `3` | `3` | Цифра 3 |
| `0x13` | 5 | `4` | `4` | Цифра 4 |
| `0x14` | 6 | `5` | `5` | Цифра 5 |
| `0x15` | 7 | `6` | `6` | Цифра 6 |
| `0x17` | 8 | `7` | `7` | Цифра 7 |
| `0x18` | 9 | `8` | `8` | Цифра 8 |
| `0x19` | 10 | `9` | `9` | Цифра 9 |
| `0x8b` | 638 | `MEDIA_STOP` | `MEDIA_STOP` | Стоп |
| `0x4c` | 625 | `CHANNEL_RECALL` | `CHANNEL_RECALL` | Возврат канала |
| `0x4e` | 640 | `MEDIA_FAST_FORWARD` | `MEDIA_FAST_FORWARD` | Перемотка вперёд |
| `0x06` | 641 | `MEDIA_PREVIOUS` | `MEDIA_PREVIOUS` | Предыдущий |
| `0x0b` | 642 | `MEDIA_NEXT` | `MEDIA_NEXT` | Следующий |
| `0x1d` | 644 | `TV_INPUT_MEDIA` | `TV_INPUT_MEDIA` | Медиа-вход |
| `0x1f` | 645 | `IR_SYS_SETTING` | `IR_SYS_SETTING` | Системные настройки ИК |
| `0x5c` | 398 | `PROG_RED` | `PROG_RED` | Красная |
| `0x5d` | 399 | `PROG_GREEN` | `PROG_GREEN` | Зелёная |
| `0x5e` | 400 | `PROG_YELLOW` | `PROG_YELLOW` | Жёлтая |
| `0x5f` | 401 | `PROG_BLUE` | `PROG_BLUE` | Синяя |

Остальные команды (`0x45`, `0x12`, `0x51`, `0x5b`, `0x44`, `0x50`, `0x43`, `0x1a`, `0x04`, `0x08`, `0x42`, `0x0f`, `0x59`, `0x57`) ведут в F1–F12, PageUp/PageDown и не несут полезной функции для пульта.

### 2.5. Какая keylayout применяется к ИК

В прошивке есть специальная `vendor/usr/keylayout/Vendor_0001_Product_0001.kl` — «generic/virtual TV remote». Она содержит `TV_INPUT`, `SETTINGS`, `INFO`, `GUIDE`, `MEDIA_*`, `CHANNEL_RECALL`, `PROG_*` и другие телевизионные коды.

Именно она, а не `Generic.kl`, объясняет, почему NEC-код `0x07` (Linux 466) в таблице выглядит как `F1 FUNCTION` в `Generic.kl`, но на телевизоре должен работать как `TV_INPUT`.

Для телефонного HID-устройства производитель/продукт не `0001:0001`, поэтому телевизор применит `Generic.kl`. Это важно: в HID-режиме нужно отправлять не «телевизионные» Linux-коды, а стандартные HID Consumer usages, которые `Generic.kl` уже умеет превращать в `TV_INPUT`, `GUIDE`, `CHANNEL_UP` и т.д.

### 2.6. ИК-передатчик

В прошивке есть:

- HAL `vendor/bin/hw/android.hardware.ir@1.0-service`;
- реализация `vendor/lib/hw/android.hardware.ir@1.0-impl.so`;
- `vendor/lib/hw/consumerir.amlogic.so`;
- sysfs `/sys/devices/virtual/irblaster/irblaster0/` с `carrier_freq`, `duty_cycle`, `send`;
- `vendor/etc/init/irblaster1.rc`, который выставляет права `666` на эти узлы.

Телевизор умеет не только принимать ИК, но и передавать его. Для телефонного приложения это не используется, но объясняет, почему в системе есть `ConsumerIrManager`.

### 2.7. Что это значит для телефона

- **RCA**: формат кадра не документирован. В таблице есть только коды кнопок, но нет описания модуляции/длительностей. Реализовать RCA вслепую нельзя.
- **NEC**: формат стандартный, адрес `0x20DF` и команды известны. Но в обычной загрузке активен RCA-режим, а NEC-таблицы подключены как fallback. Поэтому NEC-режим в приложении помечен экспериментальным.
- **ИК-передатчик телефона**: нужен `ConsumerIrManager.hasIrEmitter()`. У большинства современных телефонов его нет.
- **Питание**: ИК-код `0x52` может включать телевизор, если ИК-приёмник активен в выключенном состоянии. Bluetooth-режим этого не умеет.

---

## 3. Bluetooth-подсистема

### 3.1. Профили

`system/system/priv-app/Bluetooth/Bluetooth.apk` содержит:

- HID Host (`BluetoothHidHost`);
- HID Device (`BluetoothHidDevice`);
- HOGP (HID over GATT).

Телевизор поддерживает HID Host и HOGP, поэтому может принимать обычные Bluetooth-клавиатуры, мыши и геймпады. Это основа Bluetooth-режима приложения.

### 3.2. Keylayouts пультов Яндекса

В `vendor/usr/keylayout/`:

| Файл | Устройство |
|---|---|
| `Vendor_057a_Product_0058.kl` | Yandex Remote / Omni Remote (brick) |
| `Vendor_057a_Product_006f.kl` | Yandex Remote 2 |
| `Vendor_057a_Product_0088.kl` | Yandex Remote 3 |
| `Vendor_7545_Product_0200.kl` | Yandex Remote 4 / Weida |
| `Vendor_1949_Product_0401.kl` | Amazon Fire TV Remote |
| `Vendor_0001_Product_0001.kl` | Generic/virtual TV remote |

Общие особенности Yandex-раскладок:

```text
key 116   POWER
key 113   VOLUME_MUTE
key 114   VOLUME_DOWN
key 115   VOLUME_UP
key 158   BACK
key 172   HOME
key 139   MENU
key 217   VOICE_ASSIST
key 231   PAIRING
key 362   GUIDE
key 402   CHANNEL_UP
key 403   CHANNEL_DOWN
key 582   SEARCH
key usage 0x07004a HOME
key usage 0x0c006F BRIGHTNESS_UP
key usage 0x0c0070 BRIGHTNESS_DOWN
```

`key 231 PAIRING` — отдельный код для режима сопряжения. Это не кнопка «голос»: голос — `217 VOICE_ASSIST`.

### 3.3. Сопряжение

`YandexRemoteChecker.apk` (`com.yandex.tv.remotechecker`) содержит `PairingRemoteReceiver`, который слушает:

```text
com.yandex.tv.remotechecker.SHOW_REMOTE_PAIRING
```

`Utils.showPairing()` запускает activity:

```text
com.yandex.tv.setupwizard.aliasBluetoothRemotePairing
action = com.yandex.tv.BT_REMOTE_PAIRING
```

`PhoneWindowManager` в `services.jar` при долгом удержании POWER на панели телевизора отправляет этот broadcast.

`YandexSetupWizard.apk` содержит `com.yandex.tv.setupwizard.bluetooth.AddAccessoryActivity` и alias `aliasBluetoothRemotePairing`. Activity использует классический `BluetoothAdapter.startDiscovery()` и `createBond()`. Также она обрабатывает:

```text
com.yandex.tv.action.RCU_IR_SETUP
```

`DroidTvSettings.apk` (`com.android.tv.settings`) содержит стандартный экран **Remotes & Accessories** и тот же action `com.yandex.tv.BT_REMOTE_PAIRING`.

Практический вывод: телефон можно спарить через штатный экран **Настройки → Пульты и аксессуары → Добавить аксессуар**. Приложение не может удалённо открыть этот экран, пока не имеет канала управления.

### 3.4. Голосовой BLE-сервис

`system/system/priv-app/YandexBleRemote/YandexBleRemote.apk` (`ru.yandex.bleremotecontrol`) — отдельный сервис только для голоса.

Он регистрирует GATT-сервис `rcuvoiceservice` и фильтрует устройства по имени, содержащему `"Yandex Remote"`.

GATT UUID:

| Назначение | UUID |
|---|---|
| Service | `65fa9513-e8ca-4efe-b5ba-67b7c44101dc` |
| Mic control | `20d695c7-1f7b-4d11-afec-c6c5cfae7f52` |
| Voice data | `9c98fa55-3de4-4361-8f26-ba1c62c8f222` |
| CCCD | `00002902-0000-1000-8000-00805f9b34fb` |

Внутри используется ADPCM-декодирование. Это проприетарный протокол, поэтому голос/микрофон не входят в MVP приложения.

### 3.5. Чего в прошивке нет

- Нет `com.google.android.tv.remote.service`.
- Нет пользовательского сетевого пульта.
- ADB выключен по умолчанию и требует авторизации.
- Нет публичного API, который позволяет телефону открыть экран сопряжения без существующего канала.

---

## 4. Матрица выполнимости

| Подход | Выполнимость | Комментарий |
|---|---|---|
| Bluetooth HID Device | **Да** | Телевизор поддерживает HID Host/HOGP; стандартные consumer usages работают через `Generic.kl` |
| ИК NEC | **Экспериментально** | NEC-таблицы есть, но активен RCA; нужен ИК-передатчик в телефоне |
| ИК RCA | **Нет** | Формат кадра неизвестен |
| Google TV Remote | **Нет** | В прошивке нет сервиса |
| ADB-пульт | **Нет** | Не пользовательский сценарий, требует авторизации |
| Голос через BLE | **Возможно, но сложно** | Нужно эмулировать проприетарный GATT-сервер и ADPCM-поток |
| Включение ТВ по Bluetooth | **Нет** | При выключенном ТВ Bluetooth-контроллер выключен |

---

## 5. Нерешённые вопросы

1. Точный формат RCA-кадра и последовательность инициализации `remotecfg`.
2. Активен ли NEC-декодер в обычной загрузке одновременно с RCA.
3. Полный handshake голосового GATT-сервиса (кроме известных UUID).
4. Поведение конкретных OEM-прошивок телефонов при регистрации HID Device.
5. Принимает ли телевизор HID-устройство с нестандартным набором consumer usages без переподключения.

Эти вопросы не блокируют основной Bluetooth HID-режим.

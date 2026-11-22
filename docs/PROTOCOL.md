# Протоколы

## 1. Bluetooth HID Device

### 1.1. Роль

Телефон регистрируется как **HID Device** (не Host). Телевизор — HID Host. Обмен идёт по классическому Bluetooth HID (L2CAP) и/или HOGP, в зависимости от того, какой транспорт выберет стек.

Приложение использует Android API:

```kotlin
BluetoothAdapter.getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)
BluetoothHidDevice.registerApp(sdp, null, null, executor, callback)
BluetoothHidDevice.sendReport(device, reportId, data)
```

`BluetoothHidDevice` доступен с API 28, поэтому `minSdk = 28`.

### 1.2. SDP

```kotlin
BluetoothHidDeviceAppSdpSettings(
    name = "Yandex TV Remote",
    description = "Bluetooth HID remote for Yandex TV",
    provider = "Daniil Borovskiy",
    subclass = 0xC0, // keyboard + consumer control
    descriptors = HID_REPORT_DESCRIPTOR,
)
```

`subclass = 0xC0` — комбинированное устройство (клавиатура + pointing/consumer). Телевизор не требует Yandex-специфичного subclass.

### 1.3. HID Report Descriptor

Используется композитный дескриптор с двумя report ID.

```text
05 01        Usage Page (Generic Desktop)
09 06        Usage (Keyboard)
A1 01        Collection (Application)
85 01          Report ID (1)
05 07          Usage Page (Keyboard/Keypad)
19 E0          Usage Minimum (224)
29 E7          Usage Maximum (231)
15 00          Logical Minimum (0)
25 01          Logical Maximum (1)
75 01          Report Size (1)
95 08          Report Count (8)
81 02          Input (Data,Var,Abs)      ; modifiers
75 08          Report Size (8)
95 01          Report Count (1)
81 01          Input (Const)             ; reserved
05 08          Usage Page (LEDs)
19 01          Usage Minimum (1)
29 05          Usage Maximum (5)
75 01          Report Size (1)
95 05          Report Count (5)
91 02          Output (Data,Var,Abs)
75 03          Report Size (3)
95 01          Report Count (1)
91 01          Output (Const)
05 07          Usage Page (Keyboard/Keypad)
19 00          Usage Minimum (0)
29 65          Usage Maximum (101)
15 00          Logical Minimum (0)
25 65          Logical Maximum (101)
75 08          Report Size (8)
95 06          Report Count (6)
81 00          Input (Data,Array,Abs)    ; 6-key rollover
C0           End Collection

05 0C        Usage Page (Consumer)
09 01        Usage (Consumer Control)
A1 01        Collection (Application)
85 02          Report ID (2)
15 00          Logical Minimum (0)
26 FF 03       Logical Maximum (1023)
19 00          Usage Minimum (0)
2A FF 03       Usage Maximum (1023)
75 10          Report Size (16)
95 01          Report Count (1)
81 00          Input (Data,Array,Abs)
C0           End Collection
```

### 1.4. Формат отчётов

**Клавиатура, Report ID 1, 8 байт:**

```text
byte 0: modifiers
byte 1: reserved (0)
byte 2: key 1
byte 3: key 2
byte 4: key 3
byte 5: key 4
byte 6: key 5
byte 7: key 6
```

Нажатие цифры:

```text
00 00 1E 00 00 00 00 00   // "1" (HID usage 0x1E)
```

Отпускание:

```text
00 00 00 00 00 00 00 00
```

**Consumer Control, Report ID 2, 2 байта (little-endian usage):**

```text
30 00   // Power (0x0030)
24 02   // AC Back (0x0224)
23 02   // AC Home (0x0223)
```

Отпускание:

```text
00 00
```

Между нажатием и отпусканием приложение держит паузу `30 мс`. Для повторяемых кнопок UI вызывает отправку каждые `110 мс` после начальной задержки `400 мс`.

### 1.5. Consumer usages

Приложение использует только стандартные usages из HID Consumer Page (0x0C). Телевизор применяет `Generic.kl`, потому что телефон не совпадает с vendor/product пультов Яндекса.

| Кнопка | HID usage | Linux key | `Generic.kl` |
|---|---:|---:|---|
| Power | `0x0030` | 116 | `POWER` |
| TV / источник | `0x0089` | 377 | `TV` |
| Menu / настройки | `0x0040` | 139 | `MENU` |
| Voice / Алиса | `0x00CF` | 582 | `VOICE_ASSIST` |
| D-pad Up | `0x0042` | 103 | `DPAD_UP` |
| D-pad Down | `0x0043` | 108 | `DPAD_DOWN` |
| D-pad Left | `0x0044` | 105 | `DPAD_LEFT` |
| D-pad Right | `0x0045` | 106 | `DPAD_RIGHT` |
| OK | `0x0041` | 353 | `DPAD_CENTER` |
| Back | `0x0224` | 158 | `BACK` |
| Home | `0x0223` | 172 | `HOME` |
| Volume Up | `0x00E9` | 115 | `VOLUME_UP` |
| Volume Down | `0x00EA` | 114 | `VOLUME_DOWN` |
| Mute | `0x00E2` | 113 | `VOLUME_MUTE` |
| Channel Up | `0x009C` | 402 | `CHANNEL_UP` |
| Channel Down | `0x009D` | 403 | `CHANNEL_DOWN` |
| Play/Pause | `0x00CD` | 164 | `MEDIA_PLAY_PAUSE` |
| Previous | `0x00B6` | 165 | `MEDIA_PREVIOUS` |
| Next | `0x00B5` | 163 | `MEDIA_NEXT` |
| Rewind | `0x00B4` | 168 | `MEDIA_REWIND` |
| Fast Forward | `0x00B3` | 208 | `MEDIA_FAST_FORWARD` |
| Stop | `0x00B7` | 166 | `MEDIA_STOP` |
| Record | `0x00B2` | 167 | `MEDIA_RECORD` |
| Guide | `0x008D` | 362 | `GUIDE` |
| Search / Info | `0x0221` | 217 | `SEARCH` |

Почему именно эти usages:

- `0x40` (Menu) и `0x41` (Menu Pick) — стандартная навигация consumer-пульта.
- `0x42`–`0x45` — directional pad.
- `0x223`/`0x224` — AC Home/Back, самые совместимые аналоги Android Home/Back.
- `0x89` — TV, `0x8D` — Program Guide, `0x9C`/`0x9D` — Channel Increment/Decrement.
- `0xB2`–`0xB7` — media transport.
- `0xCD` — Play/Pause.
- `0xCF` — Voice Command.
- `0xE2`, `0xE9`, `0xEA` — Mute/Volume.
- `0x221` — AC Search.

### 1.6. Цифры

Цифры отправляются как клавиатурные usage, потому что consumer page не имеет стабильных цифровых usages, а `Generic.kl` уже содержит маппинг клавиатуры.

| Цифра | HID keyboard usage |
|---|---:|
| 0 | `0x27` |
| 1 | `0x1E` |
| 2 | `0x1F` |
| 3 | `0x20` |
| 4 | `0x21` |
| 5 | `0x22` |
| 6 | `0x23` |
| 7 | `0x24` |
| 8 | `0x25` |
| 9 | `0x26` |

### 1.7. Почему не vendor keylayout

Yandex-раскладки (`Vendor_057a_*`, `Vendor_7545_*`) применяются только к конкретным VID/PID пультов. Телефон имеет другой VID/PID, поэтому телевизор использует `Generic.kl`.

Следовательно:

- нельзя рассчитывать на `TV_INPUT`, `SETTINGS`, `PAIRING` из vendor-раскладок;
- нужно использовать стандартные consumer usages, которые `Generic.kl` умеет маппить;
- `0x07004a HOME` из vendor-раскладки не нужен: `0x223 AC Home` надёжнее.

---

## 2. NEC IR

### 2.1. Параметры

| Параметр | Значение |
|---|---|
| Несущая | 38 кГц |
| Адрес | `0x20DF` |
| Передача | LSB first |
| Leader mark | 9000 мкс |
| Leader space | 4500 мкс |
| Bit mark | 560 мкс |
| Zero space | 560 мкс |
| One space | 1690 мкс |
| Финальный mark | 560 мкс |

### 2.2. Кадр

NEC передаёт 4 байта:

```text
address_low
address_high
command
~command
```

Для адреса `0x20DF` и команды `0x52` (Power):

```text
DF 20 52 AD
```

Каждый байт передаётся LSB first. `ConsumerIrManager.transmit(38000, pattern)` получает массив длительностей в микросекундах, чередующих mark/space, начиная с mark.

### 2.3. Команды

Команды берутся из `vendor/etc/remote_nec_ircus1.tab`:

| Функция | NEC command |
|---|---:|
| Power | `0x52` |
| Mute | `0x53` |
| Info/Search | `0x1C` |
| Home | `0x48` |
| Menu/Settings | `0x0D` |
| Backspace | `0x40` |
| Channel + | `0x02` |
| Channel − | `0x09` |
| Volume + | `0x03` |
| Volume − | `0x41` |
| D-pad Up | `0x47` |
| D-pad Down | `0x4D` |
| D-pad Left | `0x49` |
| D-pad Right | `0x4B` |
| OK | `0x4A` |
| 0 | `0x1B` |
| 1 | `0x00` |
| 2 | `0x10` |
| 3 | `0x11` |
| 4 | `0x13` |
| 5 | `0x14` |
| 6 | `0x15` |
| 7 | `0x17` |
| 8 | `0x18` |
| 9 | `0x19` |

### 2.4. Ограничения

- В обычной загрузке активна RCA-таблица. NEC-таблицы подключены в `remote.sum` как fallback, но не гарантируется, что NEC-декодер слушает эфир одновременно с RCA.
- `0x40` в NEC-таблице ведёт в Linux keycode 14 (`DEL`), а не в `BACK`. В приложении ИК-кнопка «Назад» отправляет `0x40`, но на телевизоре это может быть удаление символа, а не навигационный Back. В HID-режиме Back работает корректно.
- Нет медиа-команд в таблице, кроме Stop/Next/Previous/Fast Forward; в приложении они отключены в ИК-режиме, если кода нет.
- Нет голоса, телегида, настроек в ИК-таблице.

---

## 3. BLE voice (не реализовано)

`YandexBleRemote` использует отдельный GATT-сервис:

```text
Service:      65fa9513-e8ca-4efe-b5ba-67b7c44101dc
Mic control:  20d695c7-1f7b-4d11-afec-c6c5cfae7f52
Voice data:   9c98fa55-3de4-4361-8f26-ba1c62c8f222
CCCD:         00002902-0000-1000-8000-00805f9b34fb
```

Сервис фильтрует устройства по имени `"Yandex Remote"` и использует ADPCM. Для эмуляции нужно:

1. Зарегистрировать GATT-сервер с этими UUID.
2. Пройти проверку имени/сопряжения.
3. Реализовать mic control handshake.
4. Кодировать голос в ADPCM и отправлять пакетами.

Это отдельный большой проект. В текущей версии кнопка «Алиса» отправляет только HID consumer usage `0xCF` (Voice Command), то есть открывает голосовой ввод на телевизоре, но не передаёт звук с телефона.

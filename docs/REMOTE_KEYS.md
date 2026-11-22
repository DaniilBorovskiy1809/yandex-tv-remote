# Таблица кнопок

`RemoteKey` — единый источник правды для UI, HID и ИК.

Поля:

- `hid` — `HidCommand.Consumer(usage)` или `HidCommand.Keyboard(usage)`;
- `ir` — NEC command byte из `remote_nec_ircus1.tab`;
- `repeatable` — повтор при удержании;
- `tone` — визуальный акцент (`Normal`, `Accent`, `Danger`).

## 1. Основные кнопки

| Enum | Подпись | HID | IR NEC | Повтор | Примечание |
|---|---|---|---:|---|---|
| `POWER` | Питание | Consumer `0x30` | `0x52` | Нет | В BT не включает выключенный ТВ |
| `TV` | ТВ | Consumer `0x89` | — | Нет | Live TV / источник |
| `SETTINGS` | Настройки | Consumer `0x40` | `0x0D` | Нет | MENU/настройки |
| `VOICE` | Алиса | Consumer `0xCF` | — | Нет | Только голосовой ввод ТВ, без микрофона телефона |
| `DPAD_UP` | Вверх | Consumer `0x42` | `0x47` | Да | |
| `DPAD_DOWN` | Вниз | Consumer `0x43` | `0x4D` | Да | |
| `DPAD_LEFT` | Влево | Consumer `0x44` | `0x49` | Да | |
| `DPAD_RIGHT` | Вправо | Consumer `0x45` | `0x4B` | Да | |
| `OK` | OK | Consumer `0x41` | `0x4A` | Нет | `Menu Pick` / `DPAD_CENTER` |
| `BACK` | Назад | Consumer `0x224` | `0x40` | Нет | В IR может быть DEL |
| `HOME` | Домой | Consumer `0x223` | `0x48` | Нет | |
| `MENU` | Меню | Consumer `0x40` | `0x0D` | Нет | Дублирует SETTINGS |
| `VOLUME_UP` | Громкость + | Consumer `0xE9` | `0x03` | Да | |
| `VOLUME_DOWN` | Громкость − | Consumer `0xEA` | `0x41` | Да | |
| `MUTE` | Без звука | Consumer `0xE2` | `0x53` | Нет | |
| `CHANNEL_UP` | Канал + | Consumer `0x9C` | `0x02` | Да | |
| `CHANNEL_DOWN` | Канал − | Consumer `0x9D` | `0x09` | Да | |
| `SEARCH` | Поиск / инфо | Consumer `0x221` | `0x1C` | Нет | AC Search / INFO |

## 2. Медиа

| Enum | Подпись | HID | IR NEC | Повтор |
|---|---|---|---:|---|
| `PLAY_PAUSE` | Пауза / воспроизведение | Consumer `0xCD` | — | Нет |
| `PREVIOUS` | Предыдущий | Consumer `0xB6` | — | Да |
| `NEXT` | Следующий | Consumer `0xB5` | — | Да |
| `REWIND` | Перемотка назад | Consumer `0xB4` | — | Да |
| `FORWARD` | Перемотка вперёд | Consumer `0xB3` | — | Да |
| `STOP` | Стоп | Consumer `0xB7` | — | Нет |
| `RECORD` | Запись | Consumer `0xB2` | — | Нет |
| `GUIDE` | Телегид | Consumer `0x8D` | — | Нет |

В NEC-таблице есть `MEDIA_STOP` (`0x8B`), `MEDIA_PREVIOUS` (`0x06`), `MEDIA_NEXT` (`0x0B`), `MEDIA_FAST_FORWARD` (`0x4E`), но приложение не использует их в ИК-режиме, потому что маппинг зависит от keylayout и может отличаться. При необходимости их можно добавить в `RemoteKey`.

## 3. Цифры

| Enum | Подпись | HID keyboard | IR NEC |
|---|---|---|---:|
| `DIGIT_0` | 0 | `0x27` | `0x1B` |
| `DIGIT_1` | 1 | `0x1E` | `0x00` |
| `DIGIT_2` | 2 | `0x1F` | `0x10` |
| `DIGIT_3` | 3 | `0x20` | `0x11` |
| `DIGIT_4` | 4 | `0x21` | `0x13` |
| `DIGIT_5` | 5 | `0x22` | `0x14` |
| `DIGIT_6` | 6 | `0x23` | `0x15` |
| `DIGIT_7` | 7 | `0x24` | `0x17` |
| `DIGIT_8` | 8 | `0x25` | `0x18` |
| `DIGIT_9` | 9 | `0x26` | `0x19` |

## 4. Группировка в UI

```text
┌──────────────────────────────────────────────┐
│  POWER     TV     SETTINGS     VOICE         │
├──────────────────────────────────────────────┤
│  VOL +     ↑        CH +                     │
│  MUTE    ← OK →     CH −                     │
│  VOL −     ↓                                 │
├──────────────────────────────────────────────┤
│  BACK   HOME   MENU   SEARCH                 │
├──────────────────────────────────────────────┤
│  ⏪  ⏮  ▶/⏸  ⏭  ⏩                           │
│        ⏹  ⏺  GUIDE                           │
├──────────────────────────────────────────────┤
│        1   2   3                             │
│        4   5   6                             │
│        7   8   9                             │
│            0                                 │
└──────────────────────────────────────────────┘
```

## 5. Поведение в ИК-режиме

Кнопки без `ir`-кода disabled:

- `TV`;
- `VOICE`;
- все медиа;
- `GUIDE`.

Кнопки с ИК-кодом остаются активными. Для `BACK` используется NEC `0x40`, который в таблице ведёт в Linux keycode 14 (`DEL`), поэтому фактическое поведение может отличаться от HID-режима.

## 6. Как добавить кнопку

1. Добавить enum-значение в `RemoteKey`.
2. Указать `labelRes`, иконку или текст.
3. Указать `hid` (consumer или keyboard usage).
4. Указать `ir`, если есть NEC-код.
5. Указать `repeatable`, если нужно удержание.
6. Добавить кнопку в `RemotePad`.
7. Добавить строку в `strings.xml`.
8. Обновить этот документ.

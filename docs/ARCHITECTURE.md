# Архитектура приложения

## 1. Общая схема

```text
┌──────────────────────────────────────────────────────────────┐
│ MainActivity (ComponentActivity)                             │
│  enableEdgeToEdge()                                          │
│  setContent { YandexTvRemoteTheme { RemoteApp(vm) } }        │
└──────────────────────────────┬───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│ RemoteApp (Compose)                                          │
│  - Bluetooth permissions                                     │
│  - ACTION_REQUEST_ENABLE                                     │
│  - lifecycle ON_RESUME                                       │
│  - collectAsStateWithLifecycle(hidState)                     │
└──────────────────────────────┬───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│ RemoteViewModel (AndroidViewModel)                           │
│  - mode: BLUETOOTH / INFRARED                                │
│  - lastMessage                                               │
│  - hidState                                                  │
│  - send(RemoteKey)                                           │
└───────────────┬──────────────────────────────┬───────────────┘
                │                              │
┌───────────────▼──────────────┐  ┌────────────▼───────────────┐
│ HidRemoteController          │  │ IrRemoteController         │
│  BluetoothHidDevice          │  │  ConsumerIrManager         │
│  registerApp / sendReport    │  │  transmit(38kHz, NEC)      │
│  StateFlow<HidState>         │  │  hasIrEmitter()            │
└──────────────────────────────┘  └────────────────────────────┘
```

Один Activity, один ViewModel, без Navigation Compose и DI. Это осознанно: приложение — один экран с несколькими карточками.

---

## 2. Слои

### 2.1. UI

`ui/RemoteScreen.kt`:

- `RemoteScreen` — Scaffold, `LargeTopAppBar`, `LazyColumn`, Snackbar.
- `StatusCard` — состояние HID/IR и основная кнопка действия.
- `ModeCard` — `SingleChoiceSegmentedButtonRow` для выбора режима.
- `RemotePad` — все кнопки, сгруппированные по секциям.
- `HelpCard` — раскрывающаяся инструкция.

`ui/RemoteButton.kt`:

- `Surface` с формой, цветом и тенью.
- `pointerInput` + `awaitEachGesture` для собственной обработки нажатия.
- `PressInteraction` для ripple.
- `HapticFeedbackType.LongPress` для тактильного отклика.
- `graphicsLayer` + `animateFloatAsState` для пружинного уменьшения.
- `semantics` с `Role.Button`, `contentDescription` и `onClick` для TalkBack.
- `repeatJob` для удержания.

`ui/theme/`:

- `Color.kt` — светлая/тёмная схема в брендинге Яндекса (красный, тёплый фон).
- `Type.kt` — выразительная типографика.
- `Shapes.kt` — увеличенные скругления.
- `Theme.kt` — `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())`.

### 2.2. ViewModel

`RemoteViewModel` — единственный источник состояния для UI:

```kotlin
var mode by mutableStateOf(RemoteMode.BLUETOOTH)
var lastMessage by mutableStateOf<String?>(null)
val hidState: StateFlow<HidState>
val irAvailable: Boolean
```

Методы:

| Метод | Что делает |
|---|---|
| `onBluetoothPermissionChanged(granted)` | Запускает/останавливает HID-контроллер |
| `setMode(mode)` | Переключает Bluetooth/IR, останавливает ненужный контроллер |
| `onDiscoverableResult(granted)` | Обновляет состояние после системного диалога видимости |
| `retryBluetooth()` | Перезапускает HID-контроллер |
| `send(key)` | Отправляет HID-отчёт или ИК-команду |
| `clearMessage()` | Убирает snackbar |

`send()` в Bluetooth-режиме проверяет, что есть `HidState.Connected`. Если нет — показывает сообщение. В ИК-режиме проверяет наличие `key.ir`.

### 2.3. HID-контроллер

`HidRemoteController` инкапсулирует:

- получение `BluetoothAdapter` через `BluetoothManager`;
- проверку разрешений (`BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`);
- `getProfileProxy` (если профиль не поддерживается, proxy не создаётся);
- `registerApp` с SDP и дескриптором;
- `onDiscoverableResult` после `ACTION_REQUEST_DISCOVERABLE`;
- `sendReport` для consumer/keyboard;
- обработку `Callback` (подключение, отключение, get/set report);
- `unregisterApp` и `closeProfileProxy`.

Состояние:

```kotlin
sealed interface HidState {
    data object Idle
    data object BluetoothOff
    data object Unsupported
    data object PermissionRequired
    data object Registering
    data object Ready
    data object Discoverable
    data class Connecting(val name: String)
    data class Connected(val name: String)
    data class Error(val message: String)
}
```

Поток:

```text
start()
  ├─ нет разрешений → PermissionRequired
  ├─ адаптера нет → Unsupported
  ├─ Bluetooth выключен → BluetoothOff
  ├─ HID Device не поддерживается → Unsupported
  └─ getProfileProxy → Registering
        └─ onServiceConnected → registerApp
              └─ onAppStatusChanged(true) → Ready
                    └─ пользователь нажимает «Сделать видимым»
                          └─ ACTION_REQUEST_DISCOVERABLE → onDiscoverableResult(true)
                                └─ Discoverable
                                      └─ onConnectionStateChanged(CONNECTED) → Connected(name)
```

### 2.4. IR-контроллер

`IrRemoteController`:

- получает `ConsumerIrManager`;
- проверяет `hasIrEmitter()`;
- вызывает `NecEncoder.encode(command)`;
- вызывает `transmit(38000, pattern)`.

Кодирование NEC вынесено в чистый объект `NecEncoder`. Его легко протестировать без Android-устройства. HID-отчёты также вынесены в `HidReports`, поэтому оба протокола покрыты JVM-тестами.

---

## 3. Разрешения и lifecycle

### 3.1. Разрешения

| Разрешение | Зачем |
|---|---|
| `BLUETOOTH`, `BLUETOOTH_ADMIN` (`maxSdk 30`) | Старые Android |
| `BLUETOOTH_CONNECT` | Подключение, регистрация HID, имя устройства |
| `BLUETOOTH_ADVERTISE` | `ACTION_REQUEST_DISCOVERABLE` для видимости |
| `TRANSMIT_IR` | ИК-передача (normal permission) |

На Android 12+ `BLUETOOTH_CONNECT` и `BLUETOOTH_ADVERTISE` запрашиваются через `RequestMultiplePermissions`. На Android 9–11 они не нужны.

### 3.2. Lifecycle

- `LaunchedEffect(Unit)` — первичный запрос разрешений.
- `LifecycleEventEffect(ON_RESUME)` — обновляет состояние Bluetooth и вызывает `onBluetoothPermissionChanged(permissionGranted)`.
- `onBluetoothPermissionChanged` вызывает `hid.start()`, который идемпотентен: если контроллер уже запущен, ничего не происходит.
- `onCleared()` — `hid.stop()`.

Важно: `retryBluetooth()` не вызывается на каждом `ON_RESUME`, иначе HID-профиль перерегистрировался бы при каждом возврате в приложение.

---

## 4. Потоки и конкурентность

- UI — main thread.
- `HidRemoteController` использует `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`.
- `send()` сериализуется `Mutex`, чтобы два быстрых нажатия не перемешали down/up.
- Каждый отчёт: down → `delay(30)` → up.
- Повтор в UI: `repeatJob` в `rememberCoroutineScope`; отменяется при отпускании.
- `IrRemoteController.send()` вызывается из `Dispatchers.IO` через `withContext`, потому что `transmit()` может блокировать.

---

## 5. Повтор кнопок

`RemoteKey.repeatable`:

- D-pad;
- Volume Up/Down;
- Channel Up/Down;
- Previous/Next/Rewind/Forward.

Логика:

```text
нажатие → send()
        → delay(400)
        → while (палец внизу) { send(); delay(110) }
отпускание → cancel job
```

Для HID это последовательность коротких нажатий. Для ИК — последовательность NEC-кадров. Для Power, цифр, OK и медиа-паузы повтор выключен.

---

## 6. Обработка ошибок

- Нет разрешений → `PermissionRequired`, кнопка «Разрешить Bluetooth».
- Bluetooth выключен → `BluetoothOff`, кнопка «Включить Bluetooth».
- Профиль не поддерживается → `Unsupported`, без кнопки.
- Ошибка регистрации/видимости/отправки → `Error(message)`, кнопка «Повторить».
- Нет соединения при нажатии кнопки → snackbar «Сначала подключите телефон…».
- ИК-кнопка без кода → snackbar «Эта кнопка не поддерживается…».
- ИК-передатчик недоступен → ИК-режим disabled, предупреждение.

---

## 7. Почему нет DI и Navigation

- Один экран.
- Один ViewModel.
- Контроллеры создаются в ViewModel.
- Нет фоновых сервисов, WorkManager, Room, сети.

Добавление Hilt/Koin/Navigation увеличило бы размер и сложность без пользы. Если появятся экраны настроек/логирования, DI можно добавить позже.

---

## 8. Точки расширения

| Задача | Куда добавлять |
|---|---|
| Новая HID-кнопка | `RemoteKey` + `RemotePad` |
| Новый ИК-код | `RemoteKey.ir` |
| Голосовой BLE | Новый `VoiceGattController` + разрешение `RECORD_AUDIO` |
| Макросы | Новый слой `MacroRepository` + последовательность `RemoteKey` |
| Настройки | `DataStore` + отдельный экран |
| Логирование HID | `HidRemoteController` + in-memory ring buffer |

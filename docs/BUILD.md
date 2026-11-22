# Сборка и CI

## 1. Версии

| Компонент | Версия | Комментарий |
|---|---|---|
| Kotlin | `2.4.20` | Последняя стабильная на ноябрь 2026 |
| AGP | `9.5.2` | Требует Gradle ≥ 9.6.0, поддерживает API 37 |
| Gradle | `9.9.2` | Wrapper с проверкой SHA-256 |
| Compose BOM | `2026.11.01` | Compose `1.13.0` |
| Material 3 | `1.5.0` | BOM-managed, `MaterialExpressiveTheme` |
| Activity Compose | `1.13.0` | |
| Lifecycle | `2.11.1` | |
| Core KTX | `1.19.1` | |
| Coroutines | `1.11.0` | |
| compileSdk | `37.1` | Compose 1.13.0 requires 37.1+ |
| targetSdk | `37` | |
| minSdk | `28` | `BluetoothHidDevice` |
| build-tools | `37.0.0` | |
| Java source/target | `17` | |
| JDK для CI | `21` | Temurin |

## 2. Особенности AGP 9

AGP 9 включает встроенную поддержку Kotlin. Поэтому в `app/build.gradle.kts` **не** применяется:

```kotlin
id("org.jetbrains.kotlin.android")
```

Вместо этого применяется только Compose-компилятор:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
```

AGP 9.5.2 поставляется с KGP `2.2.10`. Чтобы использовать Kotlin `2.4.20`, в корневом `build.gradle.kts` добавлен buildscript classpath:

```kotlin
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}
```

Это переопределяет версию KGP, которую использует встроенная Kotlin-поддержка AGP.

## 3. GitHub Actions

Файл: `.github/workflows/build.yml`.

Шаги:

1. `actions/checkout@v4`.
2. `actions/setup-java@v4` с Temurin JDK 21.
3. Кэш Android SDK:
   - `actions/cache@v4` кэширует `platforms/android-37.1` и `build-tools/37.0.0`;
   - при попадании в кэш `sdkmanager` не запускается.
4. Установка Android SDK (только при cache miss):
   - используется предустановленный SDK на runner;
   - `sdkmanager --install "platforms;android-37.1" "build-tools;37.0.0"`.
5. `gradle/actions/setup-gradle@v4`:
   - кэширует `~/.gradle/caches`;
   - кэширует wrapper distribution;
   - кэширует build-cache.
6. `./gradlew --stacktrace testDebugUnitTest assembleDebug`.
7. `actions/upload-artifact@v4`:
   - APK: `app/build/outputs/apk/debug/*.apk`;
   - HTML-отчёт тестов: `app/build/reports/tests/testDebugUnitTest`.

Дополнительно:

- `concurrency.cancel-in-progress: true` отменяет предыдущий запуск при новом push в ту же ветку;
- `paths-ignore` не запускает сборку на изменения только в `**.md`, `docs/**` и `LICENSE`.

Почему не `android-actions/setup-android@v3`: в runner с cmdline-tools 16.0 action пытается установить пакет `tools`, которого больше нет, и падает. Поэтому SDK ставится напрямую через `sdkmanager` из предустановленного каталога.

### Артефакт

После успешного запуска:

```text
Actions → Build APK → последний run → Artifacts → yandex-tv-remote-debug
```

Внутри — `app-debug.apk`.

## 4. Локальная сборка

Требования:

- JDK 21;
- Android SDK;
- `platforms;android-37.1`;
- `build-tools;37.0.0`;
- переменная `ANDROID_HOME` или `local.properties` с `sdk.dir`.

Команды:

```bash
./gradlew testDebugUnitTest assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Для release:

```bash
./gradlew assembleRelease
```

Подпись release не настроена: нужен keystore и `signingConfigs`.

## 5. Wrapper

`gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.9.2-bin.zip
distributionSha256Sum=971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b
```

Wrapper jar проверен по SHA-256:

```text
621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8
```

## 6. Частые проблемы

### `Unsupported class file major version`

CI использует JDK 21. Локально нужен JDK 21, не 17.

### `compileSdk 37.1 not found`

Установите:

```bash
sdkmanager --install "platforms;android-37.1"
```

В AGP 9 минорная версия задаётся явно:

```kotlin
compileSdk {
    version = release(37) { minorApiLevel = 1 }
}
```

### `Failed to find package 'tools'`

Это ошибка `android-actions/setup-android@v3` на новых cmdline-tools. Workflow уже обходит её, вызывая `sdkmanager` напрямую.

### `BluetoothHidDevice` не поддерживается

Это runtime-ограничение телефона, не сборки. Приложение покажет `Unsupported`.

### Ошибка Compose compiler

Убедитесь, что версия `org.jetbrains.kotlin.plugin.compose` совпадает с версией Kotlin (`2.4.20`).

### `MaterialExpressiveTheme` не найден

Нужна Material3 `1.5.0` и `compileSdk 37.1`. Версия Material3 управляется Compose BOM `2026.11.01`.

## 7. Воспроизводимость

- Все версии зафиксированы в `gradle/libs.versions.toml`.
- Wrapper проверяет SHA-256 дистрибутива.
- CI не использует локальный кеш.
- `gradle/actions/setup-gradle` кеширует зависимости между запусками, но не влияет на версии.

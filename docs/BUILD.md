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
| compileSdk / targetSdk | `37` | |
| minSdk | `28` | `BluetoothHidDevice` |
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
3. Установка Android SDK:
   - используется предустановленный SDK на runner;
   - `sdkmanager --install "platforms;android-37" "build-tools;36.0.0"`.
4. `gradle/actions/setup-gradle@v4`.
5. `./gradlew --stacktrace assembleDebug`.
6. `actions/upload-artifact@v4` с `app/build/outputs/apk/debug/*.apk`.

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
- `platforms;android-37`;
- `build-tools;36.0.0`;
- переменная `ANDROID_HOME` или `local.properties` с `sdk.dir`.

Команды:

```bash
./gradlew assembleDebug
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

### `compileSdk 37 not found`

Установите:

```bash
sdkmanager --install "platforms;android-37"
```

### `Failed to find package 'tools'`

Это ошибка `android-actions/setup-android@v3` на новых cmdline-tools. Workflow уже обходит её, вызывая `sdkmanager` напрямую.

### `BluetoothHidDevice` не поддерживается

Это runtime-ограничение телефона, не сборки. Приложение покажет `Unsupported`.

### Ошибка Compose compiler

Убедитесь, что версия `org.jetbrains.kotlin.plugin.compose` совпадает с версией Kotlin (`2.4.20`).

### `MaterialExpressiveTheme` не найден

Нужна Material3 `1.5.0` и `compileSdk 37`. Версия Material3 управляется Compose BOM `2026.11.01`.

## 7. Воспроизводимость

- Все версии зафиксированы в `gradle/libs.versions.toml`.
- Wrapper проверяет SHA-256 дистрибутива.
- CI не использует локальный кеш.
- `gradle/actions/setup-gradle` кеширует зависимости между запусками, но не влияет на версии.

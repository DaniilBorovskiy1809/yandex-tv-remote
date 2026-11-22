import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Версия задаётся CI из тега vX.Y.Z. Локально остаются значения по умолчанию.
val buildVersionCode = providers.environmentVariable("VERSION_CODE").orNull
    ?.toIntOrNull()
    ?: 1
val buildVersionName = providers.environmentVariable("VERSION_NAME").orNull
    ?.takeIf { it.isNotBlank() }
    ?: "1.0.0"

// Постоянная release-подпись из GitHub Actions secrets.
val signingStorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val signingStorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val signingKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val signingKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasEnvironmentSigning = listOf(
    signingStorePath,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "dev.daniilborovskiy.yandextvremote"
    compileSdk {
        // API 37 is published as android-37.1 on current SDK runners.
        // Compose 1.13.0 (BOM 2026.11.01) requires compileSdk 37.1+.
        version = release(37) { minorApiLevel = 1 }
    }
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "dev.daniilborovskiy.yandextvremote"
        minSdk = 28
        targetSdk = 37
        versionCode = buildVersionCode
        versionName = buildVersionName
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (hasEnvironmentSigning) {
            create("release") {
                storeFile = file(signingStorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        } else {
            // Локальная подпись: keystore.properties не коммитится.
            val keystorePropsFile = rootProject.file("keystore.properties")
            if (keystorePropsFile.exists()) {
                val props = Properties().apply {
                    keystorePropsFile.inputStream().use { load(it) }
                }
                create("release") {
                    storeFile = file(
                        props.getProperty("storeFile")
                            ?: error("keystore.properties: storeFile is missing"),
                    )
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}

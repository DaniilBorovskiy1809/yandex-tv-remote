buildscript {
    dependencies {
        // AGP 9.5.2 bundles KGP 2.2.10. This upgrades the built-in Kotlin
        // compiler to the latest stable Kotlin release (2.4.20).
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

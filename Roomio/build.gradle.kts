buildscript {
    repositories {
        maven(url = "https://storage.googleapis.com/r8-releases/raw") {
            content { includeModule("com.android.tools", "r8") }
        }
        maven(url = "https://maven.myket.ir")
        google()
    }
    dependencies {
        // AGP 9.0's bundled shrinker predates Kotlin 2.4 metadata support.
        classpath(libs.android.r8)
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.kmp.library).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.metro).apply(false)
}

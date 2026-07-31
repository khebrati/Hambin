import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "top.roomio.data"
        compileSdk = 36
        minSdk = 23
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.multiplatformSettings)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.multiplatformSettings.test)
        }
    }
}

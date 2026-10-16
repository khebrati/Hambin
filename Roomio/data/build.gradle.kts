import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.metro)
}

// Keep Firebase client configuration out of tracked source. Generated output stays under build/.
val localFirebaseProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { load(it) }
}
val firebaseApiKey = providers.gradleProperty("roomio.firebaseApiKey")
    .orElse(providers.environmentVariable("ROOMIO_FIREBASE_API_KEY"))
    .orElse(localFirebaseProperties.getProperty("firebase.apiKey", ""))
val generatedFirebaseDirectory = layout.buildDirectory.dir("generated/firebase/commonMain")
val generateFirebaseConfig = tasks.register("generateFirebaseConfig") {
    val configuredKey = firebaseApiKey
    val outputDirectory = generatedFirebaseDirectory
    inputs.property("firebaseApiKey", configuredKey)
    outputs.dir(outputDirectory)
    doLast {
        val key = configuredKey.get()
        require(key.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            "Firebase API key contains unsupported characters"
        }
        val output = outputDirectory.get().file("top/roomio/data/party/BuildFirebaseConfig.kt").asFile
        output.parentFile.mkdirs()
        output.writeText("package top.roomio.data.party\n\ninternal object BuildFirebaseConfig {\n    const val apiKey = \"$key\"\n}\n")
    }
}

kotlin {
    android {
        namespace = "top.roomio.data"
        compileSdk = 36
        minSdk = 23
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
        androidResources {
            enable = true
        }
    }

    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain { kotlin.srcDir(generateFirebaseConfig) }
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.multiplatformSettings)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.stream.webrtc.android)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.multiplatformSettings.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

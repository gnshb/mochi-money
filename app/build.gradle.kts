import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val keystoreProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use(::load)
}

fun envOrProp(envName: String, propName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propName)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.mochimoney.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mochimoney.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations += listOf("en")

        // The MediaPipe LLM engine ships a large native .so per ABI. x86/x86_64 are
        // emulator-only, so we drop them to keep the APK small. Play delivers only the
        // device's ABI from the App Bundle anyway; this just trims the universal APK.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = envOrProp("MOCHI_RELEASE_KEYSTORE", "storeFile")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = envOrProp("MOCHI_RELEASE_STORE_PASSWORD", "storePassword")
                keyAlias = envOrProp("MOCHI_RELEASE_KEY_ALIAS", "keyAlias")
                keyPassword = envOrProp("MOCHI_RELEASE_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Sign release builds only when a real release keystore is configured (via env
            // vars or keystore.properties). Otherwise the output is an unsigned APK that
            // cannot be installed — by design, so we never ship a debug-signed release.
            if (envOrProp("MOCHI_RELEASE_KEYSTORE", "storeFile") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.activity.compose)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.datetime)

    // On-device LLM counterparty detection.
    // MediaPipe runs user-downloaded small models (e.g. Gemma 3 1B) on all devices.
    // AICore drives Gemini Nano on supported Pixel/Samsung devices (API 31+, gated at runtime).
    implementation(libs.mediapipe.tasks.genai)
    implementation(libs.aicore)

    ksp(libs.room.compiler)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
}

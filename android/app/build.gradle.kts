plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.malletcrafts.sitephotos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.malletcrafts.sitephotos"
        // 29 = Android 10: MediaStore RELATIVE_PATH exists, and app-owned
        // inserts into Pictures/ need no storage permission at all. A site
        // phone bought for this job will not be older.
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        getByName("debug") {
            // A COMMITTED keystore, deliberately. Debug keystores are not
            // secrets (storepass "android" is the platform convention); what
            // matters is that every CI build carries the SAME signature, so a
            // new APK installs OVER the old one instead of demanding an
            // uninstall that would wipe the offline queue. This key never
            // touches Play — the Play upload key will be a real secret.
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":pano"))

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

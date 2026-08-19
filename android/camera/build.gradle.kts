// The Insta360 X3 bridge. This module exists so the PROPRIETARY SDK never
// touches the public repo: the AAR(s) are dropped into libs/ (gitignored)
// by the developer or by CI (fetched from the private Drive parking via the
// MCFT_GDRIVE_SA_JSON service account), and everything that links against
// them lives here, behind -PwithCamera.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.malletcrafts.sitephotos.camera"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

// X3Bridge calls real SDK classes now, so the AARs are a hard requirement:
// fail at configuration with instructions instead of at compile with 200
// unresolved references. CI fetches them from the private Drive parking;
// local dev unzips the SDK into libs/ (gitignored — see README.md).
val sdkAars = fileTree("libs") { include("*.aar") }
if (sdkAars.isEmpty) {
    error(
        "camera: no Insta360 SDK AAR in android/camera/libs/. Building with " +
        "-PwithCamera needs the SDK — see android/camera/README.md for where " +
        "it is parked and how CI fetches it."
    )
}

dependencies {
    implementation(project(":pano"))
    implementation(sdkAars)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}

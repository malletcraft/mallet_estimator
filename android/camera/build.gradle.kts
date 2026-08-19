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

// The SDK resolves from Insta360's private Maven (the zip they ship is a
// demo project, not a bag of AARs). CI parses the coordinates + credentials
// out of the privately parked zip into env vars; local dev can either set
// the same four INSTA_* vars or drop AARs into libs/ (gitignored).
val camCoord: String? = System.getenv("INSTA_SDKCAMERA_COORD")
val medCoord: String? = System.getenv("INSTA_SDKMEDIA_COORD") ?: camCoord
val sdkAars = fileTree("libs") { include("*.aar") }
if (camCoord == null && sdkAars.isEmpty) {
    error(
        "camera: no Insta360 SDK available. Building with -PwithCamera needs " +
        "either the INSTA_MVN_* + INSTA_SDK*_COORD env vars (the CI path) " +
        "or AARs in android/camera/libs/ — see android/camera/README.md."
    )
}

dependencies {
    implementation(project(":pano"))
    if (camCoord != null) {
        implementation(camCoord)
        if (medCoord != camCoord) implementation(medCoord!!)
    } else {
        implementation(sdkAars)
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}

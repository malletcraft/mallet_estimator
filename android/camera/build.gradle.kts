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
// Coordinates match the parked Android-SDK-V1.10.1.zip (its demo's version
// catalog names exactly these). The repo needs credentials — see settings —
// so building -PwithCamera without INSTA_MVN_USER/PASS cannot resolve them.
val instaVersion = "1.10.1"

if (System.getenv("INSTA_MVN_USER").isNullOrBlank()) {
    error(
        "camera: INSTA_MVN_USER/INSTA_MVN_PASS not set. The SDK resolves from " +
        "Insta360's credentialed maven; CI parses the credentials out of the " +
        "privately parked demo zip — see android/camera/README.md."
    )
}

dependencies {
    implementation(project(":pano"))
    implementation("com.arashivision.sdk:sdkcamera:$instaVersion")
    implementation("com.arashivision.sdk:sdkmedia:$instaVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}

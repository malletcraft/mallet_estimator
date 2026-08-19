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

// Soft check while the scaffold carries no SDK calls yet: warn, don't fail.
// The moment X3Bridge wires real Insta360 classes this flips to error(),
// because compiling without the AAR would then be impossible anyway.
val sdkAars = fileTree("libs") { include("*.aar") }
if (sdkAars.isEmpty) {
    logger.warn(
        "camera: no Insta360 SDK AAR in android/camera/libs/ — building the " +
        "stub only. Park the SDK privately (see android/camera/README.md) " +
        "and unzip its AARs here to wire the real camera."
    )
}

dependencies {
    implementation(sdkAars)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}

// Manifest-only library: its entire job is to contribute one permission to
// the merged manifest of builds that include it (see settings.gradle.kts).
plugins {
    id("com.android.library")
}

android {
    namespace = "com.malletcrafts.sitephotos.selfupdate"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
    }
}

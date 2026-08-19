// Two modules with very different needs:
//
//   :pano — pure JVM. The projection + naming contract, buildable and
//           TESTABLE anywhere a JDK exists. This is what CI's projection
//           job and the no-SDK dev container run.
//   :app  — the Android app. Needs the Android SDK, so it is included only
//           when explicitly asked for (-PwithApp). Gating on ANDROID_HOME
//           instead would silently flip behaviour between machines — GitHub
//           runners HAVE the SDK preinstalled, so the projection job would
//           start configuring AGP for tests that never touch Android.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Versions DECLARED here are only RESOLVED by a project that applies
    // them. The Android plugins therefore live here, not in the root build:
    // a root declaration would force AGP resolution on every build — and the
    // pure-JVM machines that run :pano:test cannot reach google()'s maven.
    // ALL plugin versions live here and NONE in the root build script.
    // A root declaration loads that plugin into the root classloader; the
    // Android plugin then loads into :app's child loader, and Kotlin —
    // resolved from the parent — cannot see AGP's classes (the BaseVariant
    // crash). Declared here, each module assembles one coherent classpath.
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.0.21"
        id("com.android.application") version "8.7.3"
        id("com.android.library") version "8.7.3"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Insta360's private Maven — the SDK's home. Coordinates and
        // credentials are parsed out of the (privately parked) demo zip by
        // CI and arrive as env vars; they are never committed. Absent env =
        // repo not added = every non-camera build unaffected.
        // The URL and coordinates are public facts (they appear in
        // Insta360's own docs); only the credentials are licensed, so they
        // arrive via env — parsed out of the privately parked demo zip by
        // CI, never committed. No creds = repo not added = every non-camera
        // build unaffected.
        if (!System.getenv("INSTA_MVN_USER").isNullOrBlank()) {
            maven {
                url = uri("https://androidsdk.insta360.com/repository/maven-public/")
                credentials {
                    username = System.getenv("INSTA_MVN_USER") ?: ""
                    password = System.getenv("INSTA_MVN_PASS") ?: ""
                }
            }
        }
    }
}

rootProject.name = "mcft-site-photos"
include(":pano")
if (providers.gradleProperty("withApp").isPresent) {
    include(":app")
}
// :camera wraps the proprietary Insta360 SDK (direct X3 connection). The
// AAR is licensed, so it lives OUTSIDE this public repo — parked privately
// and dropped into android/camera/libs/ (gitignored) before building with
// -PwithCamera. Gated separately from -PwithApp so every existing build
// keeps working with no SDK on disk.
if (providers.gradleProperty("withCamera").isPresent) {
    include(":camera")
}

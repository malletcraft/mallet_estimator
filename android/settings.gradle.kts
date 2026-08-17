// The Android app is not here yet — this builds the one piece that must be
// right before anything else is written: the on-device projection, held to
// the goldens the server publishes. It is pure JVM Kotlin with no Android
// dependency, so it can be built and TESTED without the Android SDK.
rootProject.name = "mcft-site-photos"
include(":pano")

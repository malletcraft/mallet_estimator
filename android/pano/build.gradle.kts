import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { kotlin("jvm") }

repositories { mavenCentral() }

dependencies {
    // The stamp burned into every caption strip, and read back off whatever
    // ImageMeter hands us. Pure Java, no Android — which is the whole reason
    // it lives in THIS module: the encode/decode contract is unit-tested in
    // CI, on a machine with no device attached.
    implementation("com.google.zxing:core:3.5.3")
    testImplementation(kotlin("test"))
}

// 17-bytecode, whatever JDK happens to run the build. NOT jvmToolchain: that
// would demand a JDK 17 download on machines that only have 21 (this dev
// container, the projection CI job), and the app consumes this jar through
// Android's dexer, which is happiest at or below 17.
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}

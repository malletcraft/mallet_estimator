import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { kotlin("jvm") }

repositories { mavenCentral() }

dependencies { testImplementation(kotlin("test")) }

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

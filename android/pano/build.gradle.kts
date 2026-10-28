import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.security.MessageDigest

plugins { kotlin("jvm") }

repositories { mavenCentral() }

// THE i18n GENERATOR'S OWN SOURCE SET (design-marathi §3.1, slice 1a). GenTx and the
// literal scanner live here, never in main: they depend only on MiniJson, so a hand-damaged
// or stale generated Tx.kt can never stop the generator that repairs it from compiling. It
// ships in no jar -- :app consumes main only.
val i18nGen: SourceSet by sourceSets.creating

dependencies {
    // The stamp burned into every caption strip, and read back off whatever
    // ImageMeter hands us. Pure Java, no Android — which is the whole reason
    // it lives in THIS module: the encode/decode contract is unit-tested in
    // CI, on a machine with no device attached.
    implementation("com.google.zxing:core:3.5.3")
    testImplementation(kotlin("test"))
    // The tests regenerate the table in memory (check 6) and scan the sources with the same
    // tokenizer the ratchet task runs, so both live once, in i18nGen.
    testImplementation(i18nGen.output)
}

// 17-bytecode, whatever JDK happens to run the build. NOT jvmToolchain: that
// would demand a JDK 17 download on machines that only have 21 (this dev
// container, the projection CI job), and the app consumes this jar through
// Android's dexer, which is happiest at or below 17.
kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    // `internal object MiniJson` moved into i18nGen (one copy, not two); the projection and
    // prep-file tests still read their goldens with it, so test is associated with i18nGen.
    target.compilations.getByName("test").associateWith(target.compilations.getByName("i18nGen"))
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// ---- i18n (android/i18n is the ONE home of the two tables; Amit, 27 Oct 2026) ----
// Since port 61c slice L2 (28 Oct 2026) nothing imports a prototype's tables: a new key is a reviewed
// commit to android/i18n, and the prototype builder is to read android/i18n at a pinned commit from its
// next build on. i18nImport and i18nCheckProto are gone with that, so no copy is imported over the card.
val i18nDir: File = rootDir.resolve("i18n")
val i18nOut: File = projectDir.resolve("src/main/kotlin/com/malletcrafts/interiorestimate/pano/i18n")
val i18nReportDir: File = layout.buildDirectory.dir("i18n").get().asFile
// A CommandLineArgumentProvider here (none is left since L2) reads ONLY locals of its own register block:
// a script-level val or fun is a member of the build script object, which the configuration cache cannot
// store. I18nBuildTest still holds every future provider to that.

tasks.register<JavaExec>("i18nGen") {
    group = "i18n"
    description = "Regenerates Tx.kt, TxEn.kt, TxMr.kt, Masters.kt and android/i18n/index.tsv from android/i18n."
    classpath = i18nGen.runtimeClasspath
    mainClass.set("com.malletcrafts.interiorestimate.pano.i18n.GenTx")
    args("gen", i18nDir.path, i18nOut.path)
}

tasks.register<JavaExec>("i18nRatchet") {
    group = "i18n"
    description = "Lowers android/i18n/literal-counts.tsv to the counts in the sources; never raises one."
    classpath = i18nGen.runtimeClasspath
    mainClass.set("com.malletcrafts.interiorestimate.pano.i18n.LiteralScan")
    args("ratchet", rootDir.path)
}

// Prints what the i18n tests left for CI: one GitHub annotation per failed check, at the
// start of a line where the runner reads it (a test's own stdout arrives indented), and
// the unused-accessor count. A finalizer runs when the tests fail too.
val i18nReport = tasks.register("i18nReport") {
    group = "i18n"
    val dir = i18nReportDir
    doLast {
        val notes = dir.resolve("annotations.txt")
        if (notes.isFile) notes.readLines().filter { it.isNotBlank() }.forEach { println(it) }
        val unused = dir.resolve("unused.txt")
        if (unused.isFile) println("i18n: ${unused.readLines().count { it.isNotBlank() }} accessors not yet called (${unused.path})")
    }
}

fun sha256(s: String): String =
    MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        // The i18n checks put their whole worklist in ONE assertion message; FULL shows it.
        exceptionFormat = TestExceptionFormat.FULL
    }
    // Where the i18n tests find the tables and the sources they scan (design §3.3).
    systemProperty("mcft.android", rootDir.path)
    systemProperty("mcft.i18n.out", i18nReportDir.path)
    // The tests read SOURCE TEXT, not only classes: the tables and worklists, every scanned module's
    // sources (LiteralScan.ROOTS: :app, :camera and :pano main, comments and `// text:value` markers
    // included), the generated files check 6 compares byte for byte, and :pano's own tests (the hygiene
    // check). A comment-only edit changes no bytecode, so each is declared, or :pano:test would stay
    // UP-TO-DATE locally on a tree CI fails.
    inputs.dir(i18nDir).withPropertyName("i18nTables").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(rootDir.resolve("app/src/main/kotlin")).withPropertyName("appSources").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(rootDir.resolve("camera/src/main/kotlin")).withPropertyName("cameraSources").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(projectDir.resolve("src/main/kotlin")).withPropertyName("panoSources").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(projectDir.resolve("src/test/kotlin")).withPropertyName("panoTestSources").withPathSensitivity(PathSensitivity.RELATIVE)
    // I18nBuildTest reads this script itself.
    inputs.file(projectDir.resolve("build.gradle.kts")).withPropertyName("panoBuildScript").withPathSensitivity(PathSensitivity.RELATIVE)
    // The OPTIONAL client-name list: a file (-Pi18n.deny=<file>, resolved against android/pano) or the
    // I18N_DENY_NAMES secret. Only its hash is an input, so the names never reach Gradle's task history.
    // A path that is not a readable file is passed on as given, and check 8 FAILS on it, naming it.
    providers.gradleProperty("i18n.deny").orNull?.let { f ->
        val deny = file(f)
        systemProperty("mcft.i18n.deny", deny.path)
        inputs.property("i18nDenyFile", sha256(if (deny.isFile) deny.readText() else "missing:$f"))
    }
    inputs.property("i18nDenyEnv", sha256(System.getenv("I18N_DENY_NAMES") ?: ""))
    val dir = i18nReportDir
    doFirst { dir.resolve("annotations.txt").delete() }
    finalizedBy(i18nReport)
}

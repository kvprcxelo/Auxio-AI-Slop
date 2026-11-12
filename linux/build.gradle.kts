import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.2"
    id("com.diffplug.spotless") version "8.10.1"
}

kotlin {
    jvmToolchain(21)
}

// Mirrors the root project's formatting rules so `./gradlew spotlessCheck` from the repository
// root accepts these sources too.
spotless {
    kotlin {
        target("src/**/*.kt")
        ktfmt().kotlinlangStyle()
        licenseHeaderFile(file("../NOTICE"))
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")

    // libmpv is called through JNA rather than a hand-written JNI shim: same C API,
    // no extra native build step to keep in sync with the JDK.
    implementation("net.java.dev.jna:jna:5.19.1")

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "org.oxycblt.auxio.linux.MainKt"

        // AppImage assembly is done with appimagetool around the jpackage image,
        // which Compose does not emit directly.
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "auxio"
            packageVersion = "0.1.0"
            description = "A simple, rational music player"
            vendor = "Auxio-AI-Slop"
        }
    }
}

// The tag reader is a small JNI shim over the taglib build that lives in the musikr submodule.
val tagsLibrary =
    layout.buildDirectory.get().file("native/libauxiotags.so").asFile

val configureTagsNative by tasks.registering(Exec::class) {
    val sourceDir = file("src/main/cpp")
    val outputDir = layout.buildDirectory.get().dir("native").asFile
    inputs.dir(sourceDir)
    outputs.file(outputDir.resolve("CMakeCache.txt"))
    commandLine(
        "cmake",
        "-S",
        sourceDir.absolutePath,
        "-B",
        outputDir.absolutePath,
        "-G",
        "Ninja",
        "-DCMAKE_BUILD_TYPE=Release",
    )
}

val buildTagsNative by tasks.registering(Exec::class) {
    group = "build"
    description = "Builds the taglib JNI shim for Linux."
    dependsOn(configureTagsNative)
    val outputDir = layout.buildDirectory.get().dir("native").asFile
    outputs.file(tagsLibrary)
    commandLine("cmake", "--build", outputDir.absolutePath)
}

tasks.test {
    useJUnitPlatform()
    dependsOn(buildTagsNative)
    systemProperty("auxio.tags.library", tagsLibrary.absolutePath)
}

// `run` and the distributable need the shim too; the packaged app finds it through
// AUXIO_TAGS_LIB, which the AppImage sets.
tasks.matching { it.name == "run" }.configureEach {
    dependsOn(buildTagsNative)
    doFirst { (this as? JavaExec)?.systemProperty("auxio.tags.library", tagsLibrary.absolutePath) }
}

tasks.matching { it.name == "createDistributable" }.configureEach { dependsOn(buildTagsNative) }

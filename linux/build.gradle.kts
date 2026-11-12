import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.2"
}

kotlin {
    jvmToolchain(21)
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

tasks.test {
    useJUnitPlatform()
}

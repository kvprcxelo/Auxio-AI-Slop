# Auxio for Linux

An unofficial desktop port of [Auxio](https://github.com/OxygenCobalt/Auxio), packaged as an
AppImage. It is a separate Gradle build from the Android app so that neither side needs the
other's toolchain: the Android build needs the SDK and NDK, this one needs a JDK and libmpv.

## Status

Early. It scans a folder and plays files, with a minimal Compose window on top. Tags are not
read yet, so the list shows file names. No playlists, play queue, MPRIS, search ranking,
ReplayGain or settings.

## Requirements

- Linux x86_64
- **JDK 21**
- **libmpv** — any of `libmpv2` (Debian/Ubuntu), `mpv-libs` (Fedora), `mpv` (Arch)
- Optional for packaging: `appimagetool` (the script downloads it if absent)

The player loads `libmpv` at runtime through JNA, so the library must be on the loader path.
Missing it is reported as a normal error rather than a crash:

```
Could not start playback: libmpv was not found. Install it with your package manager
(for example `mpv-libs` or `libmpv2`).
```

## Build

```sh
cd linux
JAVA_HOME=/path/to/jdk-21 ../gradlew build
```

`build` compiles and runs the tests. Tests that drive libmpv are skipped when it is not
installed, so a machine without a media stack still gets a green build.

## Run

```sh
cd linux
JAVA_HOME=/path/to/jdk-21 ../gradlew run
```

With no arguments it reads `$XDG_MUSIC_DIR`, then `~/Music`. To play a specific folder, build a
distributable and pass paths to its launcher:

```sh
cd linux
../gradlew createDistributable
./build/compose/binaries/main/app/auxio/bin/auxio ~/Music/SomeAlbum
```

## Test

```sh
cd linux
../gradlew test
```

Reports land in `build/reports/tests/test/`. If libmpv is installed somewhere unusual, point
the loader at it: `LD_LIBRARY_PATH=/opt/mpv/lib ../gradlew test`.

## AppImage

```sh
./linux/packaging/build-appimage.sh
```

The script builds the jpackage image, wraps it in an AppDir, and runs `appimagetool`. The result
is `linux/dist/auxio-<version>-x86_64.AppImage`; `--version` output and release tags come from
`packageVersion` in `linux/build.gradle.kts`.

`.github/workflows/linux-appimage.yml` builds the same AppImage, uploads it as a workflow
artifact, and attaches it to the GitHub release when a `v*` tag is pushed.

The AppImage bundles the JVM but **not** libmpv, so the target machine needs libmpv installed.
Bundling it (and its dependency closure) is still to do.

## Layout

```
linux/
  build.gradle.kts              Compose Desktop, JNA, jpackage metadata
  src/main/kotlin/.../Main.kt   entry point, library roots from arguments
  src/main/kotlin/.../ui/       Compose UI: track list, transport bar
  src/main/kotlin/.../music/    filesystem scanner and Track model
  src/main/kotlin/.../playback/ libmpv binding and player wrapper
  src/test/kotlin/              scanner and playback tests
  packaging/                    AppImage script and icon
```

## Notes on the port

- **MediaStore → filesystem walk.** Android's library is provided by the OS; here it is a
  `java.nio` walk plus an extension filter, cached later in SQLite.
- **Media3 → libmpv.** libmpv covers gapless playback, ReplayGain tags and the formats we need
  without vendoring a decoder stack. It is called through JNA, which is the same C API a
  hand-written JNI shim would use, with no native build step to keep in sync.
- **taglib** is built from the `musikr` submodule for Linux; metadata wiring is in progress. The
  `linux/` module does not depend on the Android `musikr` module yet, since most of it imports
  Android APIs.

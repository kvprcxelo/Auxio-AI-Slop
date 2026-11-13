# Auxio for Linux

An unofficial desktop port of [Auxio](https://github.com/OxygenCobalt/Auxio), packaged as an
AppImage. It is a separate Gradle build from the Android app so that neither side needs the
other's toolchain: the Android build needs the SDK and NDK, this one needs a JDK and libmpv.

## Status

It scans folders, reads tags through taglib, and browses the result as **songs, albums, artists,
genres and playlists**, each with a detail view. Playlists are `.m3u`/`.m3u8` files found next to
your music. Playing a song fills a queue from the list you started it in,
with previous/next, automatic advance when a track ends, **shuffle** and **repeat all / repeat
one**. The progress bar is Material 3's expressive wavy progress indicator with a thumb, inside a
taller touch target so it still seeks on click or drag; the wave freezes when playback pauses.
Cover art is shown for the current album, artist and track: the picture embedded in the file, or
`cover.jpg`/`folder.jpg` (and similar names) from its folder. An artist page shows a large
circular collage of their album covers with play and shuffle for the whole artist, and the album
rows underneath. Artists are grouped
case-insensitively, so "x" and "X" are one artist shown with whichever spelling occurs most, and
repeated names inside one tag are dropped. Album dates are read from the date tag and shown in a
format chosen in settings, defaulting to your system's short date. A settings page also offers a
library rescan, the metadata separators used to split multi-artist tags, and a switch that sorts
`*Song` or `(Song)` under its first letter instead of the symbol. Titles fall back to file names
when a file carries no tags. Album artist and disc numbers are not read yet, and there are no
playlists, MPRIS, search ranking or ReplayGain.

## Requirements

- Linux x86_64
- **JDK 21**
- **libmpv** — any of `libmpv2` (Debian/Ubuntu), `mpv-libs` (Fedora), `mpv` (Arch)
- **cmake** and **ninja** — to build the taglib tag reader
- Optional for packaging: `appimagetool` (the script downloads it if absent)

The player loads `libmpv` at runtime through JNA, so the library must be on the loader path.
Missing it is reported as a normal error rather than a crash:

```
Could not start playback: libmpv was not found. Install it with your package manager
(for example `mpv-libs` or `libmpv2`).
```

## Settings

Settings live in `$XDG_CONFIG_HOME/auxio/settings.properties` (defaulting to
`~/.config/auxio/settings.properties`):

- `metadata.separators` — any artist tag containing one of those characters is split into
  separate artists, so `A; B` lists under both. Albums keep their raw tag, so compilations stay
  one album.
- `metadata.dateFormat` — a pattern of `YYYY`, `YY`, `MM` and `DD` tokens with anything else kept
  literally (`YYYY-MM-DD`, `YYYYMMDD`, `DD/MM/YY`, …), or `system` (the default) for the locale's
  short date. Dates come from the album's date tag; a tag with only a year keeps showing just the
  year.
- `sorting.ignoreLeadingPunctuation` — `true` (the default) sorts by the first letter or digit,
  so `*Song` and `(Song)` file under S.
- `artists.collageSeed` — seeds the shuffle that arranges an artist's album covers into the
  collage on artist rows and headers. The same seed always gives the same arrangement.

Changes apply immediately.

## Logs

Playback problems are hard to diagnose without libmpv's output, so logging is behind one
environment variable:

```sh
AUXIO_LOG=1 ./auxio-0.1.2-x86_64.AppImage            # to the terminal
AUXIO_LOG=~/auxio.log ./auxio-0.1.2-x86_64.AppImage  # to a file, mpv's own log included
```

With a path, the app appends its own lines (the `loadfile` command, the resolved audio output,
durations, errors) to that file and asks mpv for a verbose log at `<path>.mpv`, so launching from
a desktop menu still produces something you can attach to a report. Send both: `auxio.log` has the
app's view, `auxio.log.mpv` has everything the playback core did. Errors from a failed play are
also shown in the window, above the list.

## Build

Build taglib once, then the module:

```sh
./linux/packaging/build-taglib.sh   # initialises submodules, builds taglib for x86_64
cd linux
JAVA_HOME=/path/to/jdk-21 ../gradlew build
```

The tag reader is a JNI shim linking the taglib build from the `musikr` submodule, so
`build-taglib.sh` has to run before Gradle; without it the native task fails with a message
pointing back at the script. `build` then compiles, builds the shim and runs the tests. Tests
that drive libmpv are skipped when it is not installed, so a machine without a media stack
still gets a green build.

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
  src/main/kotlin/.../music/    filesystem scanner, Track model, taglib bridge
  src/main/kotlin/.../playback/ libmpv binding and player wrapper
  src/main/cpp/                 JNI tag reader over taglib, plus its CMake build
  src/test/kotlin/              scanner, tag and playback tests
  packaging/                    taglib and AppImage scripts, icon
```

## Notes on the port

- **MediaStore → filesystem walk.** Android's library is provided by the OS; here it is a
  `java.nio` walk plus an extension filter, cached later in SQLite.
- **Media3 → libmpv.** libmpv covers gapless playback, ReplayGain tags and the formats we need
  without vendoring a decoder stack. It is called through JNA, which is the same C API a
  hand-written JNI shim would use, with no native build step to keep in sync.
- **libmpv needs `LC_NUMERIC=C`** — it returns no playback context otherwise. GTK installs the
  desktop's locale process-wide, so the player resets that one category through libc just before
  creating the context. Without it, every UTF-8 desktop session fails with "Non-C locale
  detected" on the first play.
- **Material 3 Expressive** is only shipped in the alpha Material3 line, so `linux/build.gradle.kts`
  pins `org.jetbrains.compose.material3:material3:1.12.0-alpha03`, the version Compose Multiplatform
  1.12.2 itself is built against, instead of the `compose.material3` alias. That is what provides
  `LinearWavyProgressIndicator`; the stable 1.9.0 artifact does not contain it.
- **taglib** is built from the `musikr` submodule and read through a small JNI shim, so both
  targets parse tags with the same library version. The shim does not reuse musikr's
  `JMetadataBuilder`, which constructs Android metadata objects, and taglib 2.x keeps album
  artist and disc off `Tag`; those need the format-specific tag classes. The `linux/` module
  does not depend on the Android `musikr` module yet, since most of it imports Android APIs.

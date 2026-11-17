<p align="center"><img src="fastlane/metadata/android/en-US/images/icon.png" width="150"></p>
<h1 align="center"><b>Auxio-AI-Slop</b></h1>
<h4 align="center">An unofficial Linux desktop port of <a href="https://github.com/OxygenCobalt/Auxio">Auxio</a>.</h4>

Auxio-AI-Slop is a local music player for Linux. It keeps Auxio's simple, rational approach: a
fast library, a queue that behaves itself, and no accounts, telemetry or network access. The
desktop app is built with Compose Multiplatform, plays through **libmpv**, and reads tags through
**taglib**.

This is an unofficial fork of Auxio's `dev` branch and is largely AI-written. The Linux port is
the release target; the upstream Android app still lives in the repository but is not what the
releases here build. Expect rough edges, and please report them.

## Features

- Local playback through **libmpv**, including gapless queue playback, previous/next, automatic
  advance, shuffle, and repeat off/all/one.
- Library scanning with tags read through **taglib**, the same library build the Android app uses.
- Browsing by **songs, albums, artists, genres, and playlists**, with search across all of them.
- Artist pages with a seeded collage of their album covers, plus their albums and songs.
- Cover art from embedded pictures or `cover.jpg`/`folder.jpg` and similar files next to a track.
- Playlists from `.m3u`/`.m3u8` files next to your music or in `~/auxio/playlists`, with create,
  import, delete, add/remove songs, drag-to-reorder, and undo for removals.
- A sort control for songs and albums by name or release date, and by name for the other lists.
  Dragging songs in a sorted playlist writes the visible order back to the playlist file.
- Material 3 UI with the expressive wavy seek bar, which flattens while paused.
- Theming from your desktop accent, your wallpaper through **matugen**, or a custom seed colour,
  with system, light, and dark brightness.
- Metadata settings for artist separators, album date formats, punctuation-aware sorting, and the
  artist collage seed.
- Completely private and offline: no accounts, no telemetry, no network access.

The upstream Android app keeps its own feature set (**Media3/ExoPlayer**, Android Auto, widgets,
ReplayGain, equalizer support). Those features are Android-only and are not part of the desktop
build.

## Requirements

- Linux **x86_64**.
- **libmpv** at runtime: `libmpv2` on Debian/Ubuntu, `mpv-libs` on Fedora, `mpv` on Arch.
- **JDK 21** to build and run from source.
- **cmake** and **ninja** to build the small JNI tag reader against taglib.
- **taglib 2.x**, built from the `musikr` submodule by the packaging script.
- Optional: **matugen** on your `PATH` for wallpaper-based theming.
- Optional: **zenity** or **kdialog** for the playlist import file picker; AWT is used as a
  fallback.
- Optional for packaging: **appimagetool**; the build script downloads it if it is missing.

The AppImage bundles the JVM but **not** libmpv, so the target machine still needs libmpv
installed. The player loads it at runtime through JNA; if it is missing, playback reports:

```text
Could not start playback: libmpv was not found. Install it with your package manager
(for example `mpv-libs` or `libmpv2`).
```

## Install

Download the latest AppImage from the [releases page](https://github.com/kvprcxelo/Auxio-AI-Slop/releases/latest),
make it executable, and run it:

```sh
chmod +x auxio-<version>-x86_64.AppImage
./auxio-<version>-x86_64.AppImage
```

You can pass one or more music folders as arguments:

```sh
./auxio-<version>-x86_64.AppImage ~/Music/SomeAlbum
```

With no arguments, the player uses `$XDG_MUSIC_DIR`, then `~/Music`.

## Files and settings

Auxio keeps its files in the usual XDG locations:

| What | Where |
| --- | --- |
| Settings | `$XDG_CONFIG_HOME/auxio/config` (`~/.config/auxio/config`) |
| Cover cache | `$XDG_CACHE_HOME/auxio/` (`~/.cache/auxio/`) |
| Library cache | `$XDG_CACHE_HOME/auxio/library` (`~/.cache/auxio/library`) |
| Playlists | `~/auxio/playlists` |

The cover cache stores pictures pulled from tags and folders so a launch does not re-read every
file. It is safe to delete and is rebuilt as covers are needed. It is capped at 500 MB by
default, measured in allocated disk blocks; the least recently stored covers are removed when it
passes that size. Empty "no artwork" markers are capped separately at 2,000 entries.

The library cache stores the tags from the last scan, keyed by path, size, and modification time.
**Reload** reuses those tags and only parses new or changed files; **Full reload** ignores the
cache and reads every file again. Playlists in `~/auxio/playlists` are read on every library scan,
alongside `.m3u`/`.m3u8` files next to your music. Importing a playlist copies the file, makes its
entries absolute, and skips files that are already loaded or live in the managed folder.

The settings file starts empty and is written as settings change:

- `metadata.separators` — characters that split multi-artist tags into separate artists. Albums
  keep their raw tag, so compilations stay one album.
- `metadata.dateFormat` — a pattern of `YYYY`, `YY`, `MM`, and `DD` tokens, or `system` (the
  default) for the locale's short date. Month and day are shown when the date tag has them.
- `sorting.ignoreLeadingPunctuation` — `true` by default, so `*Song` and `(Song)` sort under S.
- `artists.collageSeed` — seeds the arrangement of album covers in artist collages.
- `cache.maxSizeMb` — maximum cover-cache size in MB (500 by default).
- `theme.mode` — `DESKTOP` (default), `MATUGEN`, or `CUSTOM`.
- `theme.brightness` — `SYSTEM` (default), `LIGHT`, or `DARK`.
- `theme.seed` — the custom scheme's seed colour as `#rrggbb`.
- `theme.wallpaper` — a wallpaper path for matugen; empty means whatever the desktop reports.
- `theme.schemeType` — the matugen palette, such as `TONAL_SPOT` or `VIBRANT`.

Changes apply immediately. Older builds wrote `auxio/settings.properties`; that file is moved to
the current location on first launch.

## Theming

The window uses a Material 3 scheme whose source colour is chosen in **Settings → Appearance**.
**Desktop colours** is the default: it reads the XDG appearance portal's `accent-color`, then
GNOME's `gsettings`, the KDE `kdeglobals` selection colour, and a GTK `accent_bg_color` from
`gtk.css`, falling back to Auxio's purple.

**Matugen** builds the scheme from your wallpaper using [matugen](https://github.com/InioX/matugen).
It asks matugen for the colour dump and takes the scheme for the current brightness. The wallpaper
comes from GNOME, KDE, or hyprpaper settings unless a path is entered. matugen is optional; without
it the desktop accent is used instead.

**Deep customization** builds the scheme from a seed colour entered as `#rrggbb` or set with the
red, green, and blue sliders, plus a palette type (tonal spot, content, vibrant, expressive,
neutral, or monochrome). With matugen installed it is the colour engine for every mode; without
it, a built-in approximation keeps text readable for any seed.

The scheme is built before the first frame, so launching does not flash a different palette. The
current roles are cached in `$XDG_CACHE_HOME/auxio/theme-roles` and keyed by wallpaper metadata,
palette, and brightness.

## Logs

Playback problems are much easier to diagnose with libmpv's output, so logging is behind one
environment variable:

```sh
AUXIO_LOG=1 ./auxio-<version>-x86_64.AppImage            # to the terminal
AUXIO_LOG=~/auxio.log ./auxio-<version>-x86_64.AppImage  # to a file, mpv's log included
```

With a path, the app appends its own lines to that file and asks mpv for a verbose log at
`<path>.mpv`. Attach both files to a bug report: `auxio.log` has the app's view, and
`auxio.log.mpv` has everything the playback core did.

## Build from source

Clone with submodules, then build taglib once before the desktop module:

```sh
git clone --recursive https://github.com/kvprcxelo/Auxio-AI-Slop.git
cd Auxio-AI-Slop
./linux/packaging/build-taglib.sh          # initialises submodules, builds taglib for x86_64
cd linux
JAVA_HOME=/path/to/jdk-21 ../gradlew build  # compiles, builds the native reader, runs tests
```

The tag reader is a JNI shim linking the taglib build from the `musikr` submodule, so
`build-taglib.sh` has to run before Gradle. If it has not, the native task fails with a message
pointing back at the script.

Run it during development:

```sh
cd linux
JAVA_HOME=/path/to/jdk-21 ../gradlew run
JAVA_HOME=/path/to/jdk-21 ../gradlew run --args="$HOME/Music/SomeAlbum"
```

To run a distributable with specific folders:

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

Reports land in `build/reports/tests/test/`. Tests that drive libmpv are skipped when it is not
installed, so a machine without a media stack still gets a green build. If libmpv is installed
somewhere unusual, point the loader at it with `LD_LIBRARY_PATH=/opt/mpv/lib`.

Formatting is checked with Spotless:

```sh
cd linux
../gradlew spotlessCheck
```

The repository-wide check from the root also covers the C++ sources. See
[CONTRIBUTING.md](.github/CONTRIBUTING.md) for the full contributor workflow.

## AppImage

```sh
./linux/packaging/build-appimage.sh
```

The script builds the jpackage image, wraps it in an AppDir, and runs `appimagetool`. The result is
`linux/dist/auxio-<version>-x86_64.AppImage`; `--version` output and release tags come from
`packageVersion` in `linux/build.gradle.kts`.

[`.github/workflows/linux-appimage.yml`](.github/workflows/linux-appimage.yml) builds the same
AppImage, uploads it as a workflow artifact, and attaches it to the GitHub release when a `v*` tag
is pushed.

## Project layout

```text
linux/
  build.gradle.kts              Compose Desktop, JNA, jpackage metadata
  src/main/kotlin/.../Main.kt   entry point, library roots from arguments
  src/main/kotlin/.../ui/       Compose UI: lists, transport bar, settings
  src/main/kotlin/.../music/    filesystem scanner, Track model, taglib bridge
  src/main/kotlin/.../playback/ libmpv binding and player wrapper
  src/main/cpp/                 JNI tag reader over taglib, plus its CMake build
  src/test/kotlin/              scanner, tag, and playback tests
  packaging/                    taglib and AppImage scripts, icon
```

The Android app (`app/`) and its indexing library (`musikr/`) are inherited from upstream Auxio.
They need the Android SDK and NDK and are a separate build from `linux/`.

## Port notes and limitations

- **MediaStore → filesystem walk.** The desktop library is a `java.nio` walk plus an extension
  filter, with tags cached between scans.
- **Media3 → libmpv.** libmpv provides playback and the formats we need without vendoring a
  decoder stack. It is called through JNA, the same C API a hand-written JNI shim would use.
- **taglib.** The desktop shim reads the common `Tag` fields and embedded artwork. Album artist
  and disc numbers are not read yet.
- **Material 3 Expressive** only ships in the alpha Material 3 line, so
  `linux/build.gradle.kts` pins the alpha artifact that Compose Multiplatform 1.12.2 is built
  against.
- There is no MPRIS integration, search ranking, or ReplayGain. The AppImage does not bundle
  libmpv, and only x86_64 is supported right now.
- The Android-only features listed above are not available in the Linux build.

## Contributing

Bug reports and feature requests go to [GitHub Issues](https://github.com/kvprcxelo/Auxio-AI-Slop/issues).
Please read [CONTRIBUTING.md](.github/CONTRIBUTING.md) before opening an issue or pull request.
It covers the information a useful report needs, the development setup, the test commands, and
what a pull request should include.

## Donate

You can support the creator of Auxio through
[OxygenCobalt's GitHub Sponsors page](https://github.com/sponsors/OxygenCobalt).

## License and attribution

[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](https://www.gnu.org/licenses/gpl-3.0.en.html)

Auxio-AI-Slop is Free Software: you can use, study, share, and improve it under the terms of the
[GNU General Public License](LICENSE), version 3 or (at your option) any later version.

Auxio is created by [OxygenCobalt](https://github.com/OxygenCobalt/Auxio). This is an unofficial
fork and is not affiliated with or endorsed by the upstream project. The upstream Android code
retains its original copyright and license notices.

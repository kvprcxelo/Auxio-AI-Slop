# Auxio for Linux

An unofficial desktop port of [Auxio](https://github.com/OxygenCobalt/Auxio), packaged as an
AppImage. It is a separate Gradle build from the Android app so that neither side needs the
other's toolchain: the Android build needs the SDK and NDK, this one needs a JDK and libmpv.

## Status

It scans folders, reads tags through taglib, and browses the result as **songs, albums, artists,
genres and playlists**, each with a detail view. Playlists are `.m3u`/`.m3u8` files found in
`~/auxio/playlists` or next to your music. Playing a song fills a queue from the list you started it in,
with previous/next, automatic advance when a track ends, **shuffle** and **repeat all / repeat
one**. Every tab root has a **shuffle** button in the corner that shuffles that whole list and
starts playing, and the shuffle buttons elsewhere (artist, genre and playlist pages) do the same.
The transport bar shows cover art, title and artist, the five transport buttons, and
Material 3's progress indicator underneath the row, inside a taller touch target so it seeks on
click or drag.
Cover art is shown for the current album, artist and track: the picture embedded in the file, or
`cover.jpg`/`folder.jpg` (and similar names) from its folder. An artist page shows a large
circular collage of their album covers with play and shuffle for the whole artist, and the album
rows underneath. Artists are grouped
case-insensitively, so "x" and "X" are one artist shown with whichever spelling occurs most, and
repeated names inside one tag are dropped. Album dates are read from the date tag and shown in a
format chosen in settings, defaulting to your system's short date. A settings page also offers
Reload and Full reload (see [Files](#files) for what each reads), the metadata separators used to
split multi-artist tags, and a switch that sorts
`*Song` or `(Song)` under its first letter instead of the symbol. Titles fall back to file names
when a file carries no tags. The window's colour scheme follows your desktop's accent, or your
wallpaper through matugen, or a seed colour you pick — see [Theming](#theming). Album artist and disc
numbers are not read yet, and playlists can be created and imported from the playlists tab and
have songs added to them, though songs cannot yet be removed or reordered. There is no MPRIS,
search ranking or ReplayGain.

## Requirements

- Linux x86_64
- **JDK 21**
- **libmpv** — any of `libmpv2` (Debian/Ubuntu), `mpv-libs` (Fedora), `mpv` (Arch)
- **cmake** and **ninja** — to build the taglib tag reader
- Optional: **matugen** on your `PATH`, to draw the window in your wallpaper's colours
- Optional for packaging: `appimagetool` (the script downloads it if absent)

The player loads `libmpv` at runtime through JNA, so the library must be on the loader path.
Missing it is reported as a normal error rather than a crash:

```
Could not start playback: libmpv was not found. Install it with your package manager
(for example `mpv-libs` or `libmpv2`).
```

## Files

Auxio keeps its own files in the usual places:

| What | Where |
| --- | --- |
| Settings | `$XDG_CONFIG_HOME/auxio/config` (`~/.config/auxio/config`) |
| Cover cache | `$XDG_CACHE_HOME/auxio/` (`~/.cache/auxio/`) |
| Library cache | `$XDG_CACHE_HOME/auxio/library` (`~/.cache/auxio/library`) |
| Playlists | `~/auxio/playlists` |

The cover cache holds the pictures pulled out of tags and folders, so a launch does not re-read
every file. It is safe to delete; it is rebuilt as covers are needed. It is capped at 500 MB by
default and measured in blocks allocated on disk rather than in file sizes; once it passes that
size the least recently stored covers are deleted. Empty "no artwork" markers are capped at 2 000
entries as well, since a library without covers would otherwise leave one per song. The library
cache stores the tags read by the last scan, keyed by each file's path, size and modification time.
**Reload** reuses those tags and only parses new or changed files; **Full reload** ignores the
cache and re-reads every file. Either kind of scan rewrites it for the next reload. Playlists in
`~/auxio/playlists` are read on every scan of the library, alongside any `.m3u`/`.m3u8` files next
to your music. Playlists created or imported in the playlists tab are written here; an import
copies the file and makes its entries absolute, so relative paths still resolve after the move.
**Add songs** on a playlist page appends the chosen songs' paths to that file and skips any it
already lists, so playlists stay editable in other players.

## Theming

The window is drawn in a Material 3 scheme whose source colour you choose in
Settings → Appearance. Brightness is System (follow the desktop), Light or Dark, and System is the
default. There are three colour sources:

- **Desktop colours** (the default) reads the accent your desktop reports: the XDG appearance
  portal's `accent-color` first, then GNOME's `gsettings`, then the KDE `kdeglobals` selection
  colour, then a GTK `accent_bg_color` from `gtk.css`. When nothing answers, Auxio's own purple is
  used.
- **Matugen** builds the scheme from your wallpaper with [matugen](https://github.com/InioX/matugen).
  It reads `matugen image <wallpaper> --json hex` and takes the chosen brightness' scheme out of
  the dump, in either shape matugen has used for it (4.x keys each role with a colour per mode,
  earlier versions nest the schemes). The wallpaper comes from the GNOME, KDE or hyprpaper
  settings unless you type a path in the field. matugen is optional: without it the desktop accent
  is used instead, and the settings say so. Its `config.toml` is created under
  `~/.config/matugen` when missing, because a fresh install otherwise fails on its first run.
- **Deep customization** builds the scheme from a seed colour you type as `#rrggbb` or set with
  the red, green and blue sliders, plus a palette type (tonal spot, content, vibrant,
  expressive, neutral, monochrome). The type is not decoration: with matugen it picks matugen's
  palette, and without it the built-in generator shifts the hue and scales the chroma the same
  way, so Vibrant is louder and Monochrome is grey either way.

With matugen installed it is the colour engine for every mode — desktop accents and custom seeds
are passed to `matugen color hex` — so all three look like the same Material You scheme. Without
it, a built-in approximation takes over: the seed's hue and chroma carry the roles and every
on-colour is chosen by luminance, so text stays readable whatever seed is picked.

The scheme is built before the first frame, so launching never flashes a different palette: the
desktop and custom modes run their one colour lookup up front, and a wallpaper reuses the roles
matugen last produced for it, kept in `$XDG_CACHE_HOME/auxio/theme-roles` and keyed by the
wallpaper's size, modification time, palette and brightness. Editing the wallpaper or switching
palette misses that cache, and the window draws the accent scheme for the moment matugen takes to
run.

Brightness follows the setting: System asks the desktop's appearance portal first, then GNOME's
`color-scheme`, then the KDE scheme name, and falls back to dark.

## Settings

The settings file is `$XDG_CONFIG_HOME/auxio/config` (defaulting to `~/.config/auxio/config`).
Older builds wrote `auxio/settings.properties`; that file is moved over on the first launch:

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
- `cache.maxSizeMb` — how large the cover cache may grow, in megabytes (500 by default). Past
  that size the least recently stored covers are deleted; a deleted cover is read from its file
  again the next time it is shown.
- `theme.mode` — `DESKTOP` (the default), `MATUGEN` or `CUSTOM`.
- `theme.brightness` — `SYSTEM` (the default), `LIGHT` or `DARK`.
- `theme.seed` — the custom scheme's seed as `#rrggbb`. A build that wrote `theme.hue` and
  `theme.chroma` is migrated to the colour those sliders were showing.
- `theme.wallpaper` — a wallpaper path for matugen; empty means whatever the desktop reports.
- `theme.schemeType` — the matugen palette to build, e.g. `TONAL_SPOT` or `VIBRANT`.

Changes apply immediately.

## Logs

Playback problems are hard to diagnose without libmpv's output, so logging is behind one
environment variable:

```sh
AUXIO_LOG=1 ./auxio-0.11.3-x86_64.AppImage            # to the terminal
AUXIO_LOG=~/auxio.log ./auxio-0.11.3-x86_64.AppImage  # to a file, mpv's own log included
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
  `java.nio` walk plus an extension filter, with the tags it reads cached between scans.
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

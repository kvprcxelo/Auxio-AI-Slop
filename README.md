<p align="center"><img src="fastlane/metadata/android/en-US/images/icon.png" width="150"></p>
<h1 align="center"><b>Auxio-AI-Slop</b></h1>
<h4 align="center"><a href="https://github.com/OxygenCobalt/Auxio">A simple, rational music player</a>, unofficially ported to Linux.</h4>


## About

**WARNING: This fork is in development and is NOT ready to be used.**

Auxio, and this fork, are local music players with fast, reliable UI/UX without the many useless features present in other music players. Built off of modern media playback libraries, they have superior library support and listening quality compared to other apps that use outdated functionality. In short, **they play music.**

**This is a fork of the dev branch of Auxio; a fully AI-made port. Expect bugs.**

## Bugs & Feature Requests

Found a bug, a crash, or something that looks wrong? Want a feature added? Open an issue on the
[Issues](https://github.com/kvprcxelo/Auxio-AI-Slop/issues) tab. A useful report has:

- what you expected and what actually happened,
- the steps that reproduce it,
- the build you tried (the release tag or AppImage name),
- your logs if you have them. On Linux, run the AppImage with
  `AUXIO_LOG=~/auxio.log ./auxio-<version>-x86_64.AppImage` and attach `auxio.log` and
  `auxio.log.mpv`; see [linux/README.md](linux/README.md#logs) for details.

Feature requests go to the same place. Please describe the problem the feature solves, not just the
feature, so it can be judged on its own.

## Screenshots

Replace these later...

<!--
<p align="center">
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot0.png" width=250>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot1.png" width=250>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot2.png" width=250>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot3.png" width=250>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot4.png" width=250>
    <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/shot5.png" width=250> 
</p>
-->

## Features

The Linux port is what this repository builds and releases. Its feature set is still growing; see
[linux/README.md](linux/README.md) for the current state and limitations.

- Local playback through **libmpv**, with gapless hand-off between tracks in a queue
- Library scanning of your music folders, reading tags through **taglib** (the same build the
  Android app uses)
- Browsing by **songs, albums, artists, genres and playlists** (`.m3u`/`.m3u8` files next to your
  music)
- Artist pages with a seeded collage of their album covers, plus their albums and songs
- Queue with previous/next, automatic advance, shuffle and repeat off/all/one
- Material 3 UI, including the expressive wavy seek bar, which flattens while paused
- Cover art from embedded pictures or `cover.jpg`/`folder.jpg` in the track's folder
- Search across songs, albums, artists, genres and playlists
- Settings for metadata separators, album date patterns, punctuation-aware sorting and the artist
  collage seed
- Completely private and offline: no accounts, no telemetry, no network access

The Android app from upstream keeps its own feature list (**Media3/ExoPlayer**, Android Auto,
widgets, ReplayGain, equalizer support); those are Android-only and are not part of the desktop
build.

## Donate

You can support the creator of Auxio through [OxygenCobalt's Github Sponsors page](https://github.com/sponsors/OxygenCobalt). 

## Dependencies

Building and running the Linux port needs:

- **JDK 21** (the desktop build targets Java 21)
- **libmpv** at runtime — `libmpv2` on Debian/Ubuntu, `mpv-libs` on Fedora, `mpv` on Arch
- **cmake** and **ninja** to build the small JNI tag reader against taglib
- **taglib 2.x**, built from the `musikr` submodule by the packaging script
- **appimagetool**, only for packaging the AppImage (the script downloads it if missing)

Kotlin dependencies are resolved by Gradle: Compose Multiplatform (Desktop), the Material 3 alpha
line for the expressive components, JNA for libmpv and kotlinx-coroutines.

Building the Android app additionally needs the Android SDK and NDK; the Linux build is a separate
Gradle project under `linux/` and does not touch them.

## Building

Clone with submodules, then build taglib once before the desktop module:

```sh
git clone --recursive https://github.com/kvprcxelo/Auxio-AI-Slop.git
cd Auxio-AI-Slop
./linux/packaging/build-taglib.sh          # initialises submodules, builds taglib for x86_64
cd linux
JAVA_HOME=/path/to/jdk-21 ../gradlew build  # compiles, builds the native reader, runs tests
```

Run it against a folder:

```sh
cd linux
JAVA_HOME=/path/to/jdk-21 ../gradlew run --args="$HOME/Music"
```

Build the AppImage:

```sh
./linux/packaging/build-appimage.sh
# -> linux/dist/auxio-<version>-x86_64.AppImage
```

The AppImage bundles the JVM but not libmpv, so the target machine needs libmpv installed.
Fuller instructions, including logs and the settings file, are in [linux/README.md](linux/README.md).

## License

[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](http://www.gnu.org/licenses/gpl-3.0.en.html)

Auxio and this fork are Free Software: You can use, study share and improve it at your
will. Specifically you can redistribute and/or modify it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html) as
published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.


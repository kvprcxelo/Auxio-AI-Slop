# Contributing to Auxio-AI-Slop

Auxio-AI-Slop is an unofficial Linux desktop port of
[Auxio](https://github.com/OxygenCobalt/Auxio). The Linux player in `linux/` is the release
target; the Android app (`app/`) and its indexing library (`musikr/`) are inherited from upstream.
This guide is for the Linux port unless a change explicitly touches the Android code.

By participating, you agree to follow the [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.html)
that covers the project and to keep discussion respectful and on topic.

## Reporting bugs and crashes

Open a [GitHub issue](https://github.com/kvprcxelo/Auxio-AI-Slop/issues) using the bug report
form. Before filing:

- Search existing issues and pull requests to avoid duplicates.
- Check that the problem still happens in the
  [latest release](https://github.com/kvprcxelo/Auxio-AI-Slop/releases/latest) or on the latest
  `dev` build.
- If you can, reproduce it with the smallest music folder or playlist that shows the problem.

A useful report includes:

- What you expected and what actually happened.
- Exact steps to reproduce the problem.
- The release tag or AppImage filename, and whether you built it from source.
- Your Linux distribution, desktop environment, session type (Wayland or X11), and libmpv
  version.
- Logs. Run the AppImage with logging enabled and attach both files:

  ```sh
  AUXIO_LOG=~/auxio.log ./auxio-<version>-x86_64.AppImage
  # attach ~/auxio.log and ~/auxio.log.mpv
  ```

  If the app does not start, run it from a terminal and include the terminal output.
- A sample file only when the problem is specific to loading or playing that file. Do not upload
  copyrighted music you cannot share.

## Requesting features

Feature requests also go to [GitHub Issues](https://github.com/kvprcxelo/Auxio-AI-Slop/issues)
using the feature request form. Explain the problem the feature solves, how other players handle
it, and why it benefits other users. Check that it is not already implemented in the latest
release or already requested.

## Development setup

The Linux build is a separate Gradle project under `linux/` and does not need the Android SDK or
NDK. You need:

- JDK 21.
- libmpv installed (the runtime library; package names vary by distribution).
- CMake, Ninja, and a C++ toolchain.
- Taglib built from the `musikr` submodule by the packaging script.

Clone with submodules and build taglib once:

```sh
git clone --recursive https://github.com/kvprcxelo/Auxio-AI-Slop.git
cd Auxio-AI-Slop
./linux/packaging/build-taglib.sh
```

Then build, test, and run the desktop module:

```sh
cd linux
JAVA_HOME=/path/to/jdk-21 ../gradlew build
JAVA_HOME=/path/to/jdk-21 ../gradlew test
JAVA_HOME=/path/to/jdk-21 ../gradlew run --args="$HOME/Music"
```

Tests that drive libmpv are skipped when it is not installed. Test reports are written to
`linux/build/reports/tests/test/`.

To build the release artifact exactly as CI does, go back to the repository root:

```sh
cd ..
./linux/packaging/build-appimage.sh
```

The result is `linux/dist/auxio-<version>-x86_64.AppImage`. The AppImage bundles the JVM but not
libmpv, so test it on a machine with libmpv installed.

## Code style

Kotlin is formatted with Spotless and ktfmt:

```sh
cd linux
../gradlew spotlessCheck
../gradlew spotlessApply   # apply the fixes
```

C++ under `linux/src/main/cpp` uses the repository-wide Eclipse CDT formatter configuration in
`eclipse-cdt.xml`. CI runs `./gradlew spotlessCheck` from the repository root, which covers both
the Linux sources and the Android modules.

Keep the Linux and Android builds separate. Do not add Android-only dependencies or APIs to
`linux/`, and do not change the Android build just to make a Linux change compile. Do not add
third-party dependencies without a clear need; the project intentionally has a small dependency
set.

## Pull requests

- Open pull requests against `dev`.
- Keep each pull request focused on one bug or feature. Split unrelated changes.
- Include a short explanation, the issue it fixes, and the checks you ran.
- Add or update tests when the behavior changes.
- Include a screenshot or short recording for visible UI changes.
- Update the documentation and templates when behavior or setup instructions change.
- Do not bump `packageVersion` or push release tags in a pull request; releases are cut by a
  maintainer.

The pull request template lists the expected Linux checks. At minimum, run the Gradle tests and
Spotless for the code you changed, and verify the AppImage if your change affects packaging or
startup.

## Releases

Maintainers update `packageVersion` in `linux/build.gradle.kts`, commit the change on `dev`, and
push an annotated `v*` tag. The `Linux AppImage` workflow builds the AppImage and attaches it to
the GitHub release. Android workflows are inherited from upstream and do not publish Linux
releases.

#!/usr/bin/env bash
# Builds a self-contained AppImage of the Linux port from the Compose desktop app image.
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
module=$(dirname "$here")
root=$(dirname "$module")
dist="$module/dist"

version=$(sed -n 's/.*packageVersion *= *"\([^"]*\)".*/\1/p' "$module/build.gradle.kts" | head -1)
: "${version:=0.0.0}"

gradlew="$root/gradlew"
if [[ ! -x "$gradlew" ]]; then
    echo "error: Gradle wrapper not found at $gradlew" >&2
    exit 1
fi

echo "==> Building taglib"
"$here/build-taglib.sh"

echo "==> Building distributable (version $version)"
"$gradlew" -p "$module" --console=plain createDistributable

appimage_dir="$module/build/compose/binaries/main/app/auxio"
if [[ ! -d "$appimage_dir" ]]; then
    echo "error: no desktop distributable at $appimage_dir" >&2
    exit 1
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

appdir="$work/Auxio.AppDir"
mkdir -p "$appdir/usr"

echo "==> Assembling AppDir"
cp -a "$appimage_dir"/* "$appdir/usr/"
cp "$here/auxio.png" "$appdir/auxio.png"
install -Dm755 "$module/build/native/libauxiotags.so" "$appdir/usr/lib/libauxiotags.so"

cat > "$appdir/AppRun" <<'EOF'
#!/bin/sh
HERE=$(dirname "$(readlink -f "$0")")
AUXIO_TAGS_LIB="$HERE/usr/lib/libauxiotags.so"
export AUXIO_TAGS_LIB
exec "$HERE/usr/bin/auxio" "$@"
EOF
chmod +x "$appdir/AppRun"

cat > "$appdir/auxio.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=Auxio
Comment=A simple, rational music player
Exec=auxio %F
Icon=auxio
Terminal=false
Categories=AudioVideo;Audio;Player;
MimeType=audio/flac;audio/mpeg;audio/ogg;audio/opus;audio/mp4;
EOF

if ! ldconfig -p 2>/dev/null | grep -q libmpv; then
    echo "warning: libmpv is not installed here; the AppImage will only run where it is" >&2
fi

tool="${APPIMAGETOOL:-}"
if [[ -z "$tool" ]]; then
    cache="${XDG_CACHE_HOME:-$HOME/.cache}/auxio"
    tool="$cache/appimagetool"
    if [[ ! -x "$tool" ]]; then
        echo "==> Downloading appimagetool"
        mkdir -p "$cache"
        curl -fsSL -o "$tool" \
            "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage"
        chmod +x "$tool"
    fi
fi

mkdir -p "$dist"
out="$dist/auxio-$version-x86_64.AppImage"

echo "==> Running appimagetool"
# FUSE is often unavailable in containers and CI; extract-and-run avoids needing it.
ARCH=x86_64 "$tool" --appimage-extract-and-run "$appdir" "$out"

echo "==> Built $out"

#!/usr/bin/env bash
# Builds taglib for the host Linux architecture from the musikr submodule, matching the options
# the Android build uses. The result is installed inside the submodule so both consumers share it.
set -euo pipefail

here=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
module=$(dirname "$here")
root=$(dirname "$module")

src="$root/musikr/src/main/cpp/taglib"
pkg="$src/pkg/linux-x86_64"
build="$src/build/linux-x86_64"

arch=$(uname -m)
if [[ "$arch" != "x86_64" ]]; then
    echo "error: only x86_64 is supported right now, this machine is $arch" >&2
    exit 1
fi

echo "==> Fetching taglib sources"
# The submodule checkout can be owned by another user in containers and CI sandboxes, which git
# refuses to read without this. It is scoped to these two commands, not written to any config.
git -c safe.directory='*' -C "$root" submodule update --init --depth 1 musikr/src/main/cpp/taglib
# taglib vendors utfcpp as a submodule of its own.
git -c safe.directory='*' -C "$src" submodule update --init --depth 1 --recursive

echo "==> Building taglib"
cmake -S "$src" -B "$build" -G Ninja \
    -DBUILD_SHARED_LIBS=OFF \
    -DVISIBILITY_HIDDEN=ON \
    -DBUILD_TESTING=OFF \
    -DBUILD_EXAMPLES=OFF \
    -DBUILD_BINDINGS=OFF \
    -DWITH_ZLIB=OFF \
    -DWITH_APE=OFF \
    -DWITH_ASF=OFF \
    -DWITH_MOD=OFF \
    -DWITH_SHORTEN=OFF \
    -DWITH_TRUEAUDIO=OFF \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_CXX_FLAGS=-fPIC \
    -DCMAKE_INSTALL_PREFIX="$pkg"

cmake --build "$build" -j"$(nproc)"
cmake --install "$build"

echo "==> Installed taglib to $pkg"

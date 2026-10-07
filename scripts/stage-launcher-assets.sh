#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LAUNCHER="$REPO_ROOT/launcher/app/src/main"
ARCH="${LWJGL_BUILD_ARCH:-arm64}"
case "$ARCH" in
    arm64) ABI=arm64-v8a ;;
    arm32) ABI=armeabi-v7a ;;
    *) echo "ERROR: unsupported LWJGL_BUILD_ARCH=$ARCH" >&2; exit 1 ;;
esac

stage() {
    local src="$1" dst="$2"
    if [[ ! -f "$src" ]]; then
        echo "  MISS: $src" >&2
        return 1
    fi
    mkdir -p "$(dirname "$dst")"
    cp -v "$src" "$dst"
}

echo "=== libgl4es.so ==="
stage "$REPO_ROOT/out/gl4es/jniLibs/$ABI/libgl4es.so" \
      "$LAUNCHER/jniLibs/$ABI/libgl4es.so"

echo
echo "=== libopenal.so ==="
stage "$REPO_ROOT/out/openal/jniLibs/$ABI/libopenal.so" \
      "$LAUNCHER/jniLibs/$ABI/libopenal.so"
stage "$REPO_ROOT/out/openal/jniLibs/$ABI/libc++_shared.so" \
      "$LAUNCHER/jniLibs/$ABI/libc++_shared.so"

echo
echo "=== LWJGL 3.4.1 assets ==="
stage "$REPO_ROOT/out/lwjgl3/lwjgl-3.4.1-android-natives-$ARCH.zip" \
      "$LAUNCHER/assets/lwjgl/lwjgl-3.4.1-android-natives-$ARCH.zip"
stage "$REPO_ROOT/out/lwjgl3/lwjgl-3.4.1-android-modules.zip" \
      "$LAUNCHER/assets/lwjgl/lwjgl-3.4.1-android-modules.zip"

echo
echo "=== caciocavallo (AWT bridge) ==="
stage "$REPO_ROOT/out/cacio/cacio-shared.jar" \
      "$LAUNCHER/assets/cacio/cacio-shared.jar"
stage "$REPO_ROOT/out/cacio/cacio-tta.jar" \
      "$LAUNCHER/assets/cacio/cacio-tta.jar"

echo
echo "=== frenchpress (froth-foamy/Steam) ==="
stage "$REPO_ROOT/out/frenchpress/frenchpress.jar" \
      "$LAUNCHER/assets/frenchpress/frenchpress.jar"

echo
echo "done."

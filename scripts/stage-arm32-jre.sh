#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEST="$REPO_ROOT/launcher/app/src/main/assets/jre25-arm32"
COMMIT=1847aa953e869e3f780818bccc3e5170cc75c135
BASE="https://raw.githubusercontent.com/FCL-Team/FoldCraftLauncher/$COMMIT/FCL/src/main/jreAssets/app_runtime/java/jre25"
mkdir -p "$DEST"

# The universal image and ARM natives must come from the same runtime build.
curl --fail --location --retry 3 "$BASE/universal.tar.xz" -o "$DEST/universal.tar.xz"
curl --fail --location --retry 3 "$BASE/bin-arm.tar.xz" -o "$DEST/bin-arm.tar.xz"
if command -v sha256sum >/dev/null 2>&1; then
    SHA256=(sha256sum)
else
    SHA256=(shasum -a 256)
fi
(
    cd "$DEST"
    "${SHA256[@]}" --check <<'CHECKSUMS'
df2d62a7842fb10f001aed10423846a3e919f202d01889fb40f1007703d4a873  universal.tar.xz
73d86e11eb7653a11f1a433322aaa3898aa5fe66647b10a342232012c3842901  bin-arm.tar.xz
CHECKSUMS
)
printf '25.0.3-fcl-%s\n' "$COMMIT" > "$DEST/version"

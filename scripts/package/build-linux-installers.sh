#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DISTRIBUTION_ROOT="${MECHANIST_DISTRIBUTION_ROOT:-}"
PACKAGE_TYPES="app-image,deb,rpm"
DIST_DIR="$PROJECT_ROOT/dist/installers/linux"
APP_NAME="The Mechanist"
REMOTE_LAUNCHER_NAME="The Mechanist Remote Lobby"
VENDOR="The Mechanist Project"

usage() {
    cat <<'EOF'
Usage: build-linux-installers.sh [options]

Options:
  --distribution PATH    Verified linux-x64 canonical distribution.
  --package-types LIST   Comma-separated: app-image,deb,rpm.
  --output PATH          Installer output directory.
  --help                 Show this help.

Without --distribution, the script builds a release-hardened canonical
distribution through ROOT_build/ci/build_runnable_distribution.py.
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --distribution) DISTRIBUTION_ROOT="$2"; shift 2 ;;
        --package-types) PACKAGE_TYPES="$2"; shift 2 ;;
        --output) DIST_DIR="$2"; shift 2 ;;
        --help|-h) usage; exit 0 ;;
        *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
    esac
done

require_tool() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Required tool '$1' was not found on PATH." >&2
        exit 2
    }
}

require_tool python3
require_tool jpackage
require_tool tar
require_tool sha256sum

BUILDER="$PROJECT_ROOT/ROOT_build/ci/build_runnable_distribution.py"
VERIFIER="$PROJECT_ROOT/ROOT_build/ci/verify_runnable_distribution.py"
STAGER="$PROJECT_ROOT/ROOT_build/ci/stage_native_installer_payload.py"
IMAGE_VERIFIER="$PROJECT_ROOT/ROOT_build/ci/verify_native_installer_image.py"

for required in "$BUILDER" "$VERIFIER" "$STAGER" "$IMAGE_VERIFIER"; do
    [[ -f "$required" ]] || {
        echo "Required release tool is missing: $required" >&2
        exit 3
    }
done

if [[ -z "$DISTRIBUTION_ROOT" ]]; then
    require_tool mvn
    require_tool java
    BUILD_OUTPUT="$PROJECT_ROOT/dist/releases-native"
    python3 "$BUILDER" \
        --repo "$PROJECT_ROOT" \
        --release-hardened \
        --output "$BUILD_OUTPUT"
    DISTRIBUTION_ROOT="$(find "$BUILD_OUTPUT" -mindepth 1 -maxdepth 1 -type d -name 'TheMechanist-*-linux-*' | sort | tail -n 1)"
fi

[[ -n "$DISTRIBUTION_ROOT" && -d "$DISTRIBUTION_ROOT" ]] || {
    echo "Canonical Linux distribution was not found: $DISTRIBUTION_ROOT" >&2
    exit 4
}
DISTRIBUTION_ROOT="$(cd "$DISTRIBUTION_ROOT" && pwd)"

MANIFEST="$DISTRIBUTION_ROOT/manifests/runtime-manifest.json"
[[ -f "$MANIFEST" ]] || {
    echo "Canonical runtime manifest is missing: $MANIFEST" >&2
    exit 4
}

readarray -t IDENTITY < <(python3 - "$MANIFEST" <<'PY'
import json, re, sys
data=json.load(open(sys.argv[1], encoding="utf-8"))
version=str(data["version"])
match=re.search(r"\d+(?:\.\d+){0,2}", version)
native=match.group(0) if match else "0.0.0"
print(version)
print(native)
print(data["platform"])
print(data["commit"])
print(data.get("remoteClientEntryPoint", ""))
PY
)
VERSION="${IDENTITY[0]}"
NATIVE_VERSION="${IDENTITY[1]}"
PLATFORM="${IDENTITY[2]}"
COMMIT="${IDENTITY[3]}"
REMOTE_ENTRY="${IDENTITY[4]}"
[[ "$PLATFORM" == "linux-x64" ]] || {
    echo "Linux native packaging requires linux-x64, found $PLATFORM." >&2
    exit 4
}
[[ "$REMOTE_ENTRY" == "mechanist.RemoteClientMain" ]] || {
    echo "Canonical distribution does not declare the governed remote-client entry." >&2
    exit 4
}

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"
PAYLOAD_DIR="$PROJECT_ROOT/build/native-installer/linux/payload"
STAGE_REPORT="$DIST_DIR/staging-linux-x64.json"

python3 "$VERIFIER" "$DISTRIBUTION_ROOT" \
    --require-release-hardened \
    --report "$DIST_DIR/source-verification-linux-x64.json"
python3 "$STAGER" "$DISTRIBUTION_ROOT" \
    --output "$PAYLOAD_DIR" \
    --expected-platform linux-x64 \
    --report "$STAGE_REPORT"

RUNTIME_IMAGE="$DISTRIBUTION_ROOT/runtime"
[[ -x "$RUNTIME_IMAGE/bin/java" ]] || {
    echo "Canonical Linux runtime image is incomplete: $RUNTIME_IMAGE" >&2
    exit 5
}

ICON_ARGS=()
for candidate in \
    "$PROJECT_ROOT/PACKAGE_client/assets/app/icons/the-mechanist-256.png" \
    "$PROJECT_ROOT/client/assets/app/icons/the-mechanist-256.png"; do
    if [[ -f "$candidate" ]]; then
        ICON_ARGS=(--icon "$candidate")
        break
    fi
done

RESOURCE_ARGS=()
for candidate in \
    "$PROJECT_ROOT/PACKAGE_installer/linux/resources" \
    "$PROJECT_ROOT/packaging/linux/resources"; do
    if [[ -d "$candidate" ]]; then
        RESOURCE_ARGS=(--resource-dir "$candidate")
        break
    fi
done

LAUNCHER_CONFIG_DIR="$PROJECT_ROOT/build/native-installer/linux/launchers"
REMOTE_LAUNCHER_CONFIG="$LAUNCHER_CONFIG_DIR/remote-client.properties"
rm -rf "$LAUNCHER_CONFIG_DIR"
mkdir -p "$LAUNCHER_CONFIG_DIR"
cat > "$REMOTE_LAUNCHER_CONFIG" <<EOF
main-jar=packages/client/TheMechanist.jar
main-class=mechanist.RemoteClientMain
app-version=$NATIVE_VERSION
linux-app-category=Game
EOF

IFS=',' read -r -a REQUESTED_TYPES <<< "$PACKAGE_TYPES"
NORMALIZED_TYPES=()
for raw in "${REQUESTED_TYPES[@]}"; do
    value="$(printf '%s' "$raw" | tr '[:upper:]' '[:lower:]' | xargs)"
    case "$value" in
        app-image|deb|rpm) NORMALIZED_TYPES+=("$value") ;;
        "") ;;
        *) echo "Unsupported Linux package type: $raw" >&2; exit 2 ;;
    esac
done
[[ ${#NORMALIZED_TYPES[@]} -gt 0 ]] || NORMALIZED_TYPES=(app-image)

COMMON_ARGS=(
    --name "$APP_NAME"
    --app-version "$NATIVE_VERSION"
    --vendor "$VENDOR"
    --description "The Mechanist limited-alpha launcher and verified package runtime"
    --runtime-image "$RUNTIME_IMAGE"
    --input "$PAYLOAD_DIR"
    --main-jar "launcher/MechanistLauncher.jar"
    --main-class "mechanist.launcher.MechanistLauncherApp"
    --add-launcher "$REMOTE_LAUNCHER_NAME=$REMOTE_LAUNCHER_CONFIG"
)

APP_IMAGE_DEST="$DIST_DIR/app-image"
APP_IMAGE_ROOT="$APP_IMAGE_DEST/$APP_NAME"
APP_IMAGE_BUILT=0

build_app_image() {
    rm -rf "$APP_IMAGE_DEST"
    mkdir -p "$APP_IMAGE_DEST"
    jpackage --type app-image \
        "${COMMON_ARGS[@]}" \
        --dest "$APP_IMAGE_DEST" \
        "${ICON_ARGS[@]}" \
        "${RESOURCE_ARGS[@]}"
    [[ -d "$APP_IMAGE_ROOT" ]] || {
        echo "jpackage did not produce expected image: $APP_IMAGE_ROOT" >&2
        exit 6
    }
    [[ -x "$APP_IMAGE_ROOT/bin/$APP_NAME" ]] || {
        echo "Native main launcher is missing: $APP_IMAGE_ROOT/bin/$APP_NAME" >&2
        exit 6
    }
    [[ -x "$APP_IMAGE_ROOT/bin/$REMOTE_LAUNCHER_NAME" ]] || {
        echo "Native remote-lobby launcher is missing: $APP_IMAGE_ROOT/bin/$REMOTE_LAUNCHER_NAME" >&2
        exit 6
    }
    python3 "$IMAGE_VERIFIER" "$APP_IMAGE_ROOT" \
        --expected-platform linux-x64 \
        --report "$DIST_DIR/native-image-verification-linux-x64.json"
    tar -C "$APP_IMAGE_DEST" -czf \
        "$DIST_DIR/TheMechanist-${VERSION}-linux-x64-native-app-image.tar.gz" \
        "$APP_NAME"
    APP_IMAGE_BUILT=1
}

for type in "${NORMALIZED_TYPES[@]}"; do
    case "$type" in
        app-image)
            build_app_image
            ;;
        deb)
            jpackage --type deb \
                "${COMMON_ARGS[@]}" \
                --dest "$DIST_DIR" \
                --install-dir "/opt/the-mechanist" \
                --linux-shortcut \
                --linux-menu-group "Game" \
                "${ICON_ARGS[@]}" \
                "${RESOURCE_ARGS[@]}"
            ;;
        rpm)
            if command -v rpmbuild >/dev/null 2>&1; then
                jpackage --type rpm \
                    "${COMMON_ARGS[@]}" \
                    --dest "$DIST_DIR" \
                    --install-dir "/opt/the-mechanist" \
                    --linux-shortcut \
                    --linux-menu-group "Game" \
                    "${ICON_ARGS[@]}" \
                    "${RESOURCE_ARGS[@]}"
            else
                echo "rpmbuild is unavailable; RPM packaging was skipped." >&2
            fi
            ;;
    esac
done

if [[ "$APP_IMAGE_BUILT" -eq 0 ]]; then
    build_app_image
fi

find "$DIST_DIR" -type f ! -name SHA256SUMS.txt -print0 |
    sort -z |
    xargs -0 sha256sum > "$DIST_DIR/SHA256SUMS.txt"

cat > "$DIST_DIR/LINUX_INSTALLERS_README.txt" <<EOF
The Mechanist limited-alpha Linux outputs
==========================================

Version: $VERSION
Native package version: $NATIVE_VERSION
Commit: $COMMIT
Platform: $PLATFORM
Release hardened: true
Canonical source distribution: $DISTRIBUTION_ROOT
Distribution model: installer -> thin launcher -> client -> server
Native launchers: $APP_NAME; $REMOTE_LAUNCHER_NAME

Every native output in this directory was composed from the verified canonical
release staging tree. The native app image was reopened and checked against the
launcher manifest after jpackage completed. Mutable saves, profiles, settings,
logs, cache, mods, exports, and resume-token custody remain outside the installed
application payload.

Recommended validation order:
1. Verify SHA256SUMS.txt.
2. Extract the native app-image archive and start The Mechanist.
3. Confirm launcher package verification succeeds.
4. Start The Mechanist Remote Lobby and verify the lobby-only boundary.
5. Run a single-player save/resume test.
6. Run the independent host bind and two-client connection tests.
EOF

echo "Linux native package convergence complete: $DIST_DIR"

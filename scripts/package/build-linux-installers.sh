#!/usr/bin/env bash
set -Eeuo pipefail

APP_NAME="The Mechanist Launcher"
APP_ID="the-mechanist-launcher"
APP_VERSION="0.9.10ix"
VENDOR="The Mechanist Project"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TARGET_DIR="$PROJECT_ROOT/target"
DIST_DIR="$PROJECT_ROOT/dist/installers/linux"
APP_IMAGE_DIR="$DIST_DIR/app-image"
RUNTIME_DIR="$PROJECT_ROOT/build/jlink/runtime-launcher-linux"
INPUT_DIR="$PROJECT_ROOT/build/package-input/linux-launcher"
ICON_PATH="$PROJECT_ROOT/assets/app/icons/the-mechanist-256.png"
CLIENT_MODULES="$(tr -d '\r\n ' < "$PROJECT_ROOT/packaging/jlink/client-modules.txt")"
JLINK_OPTIONS=(--strip-debug --no-man-pages --no-header-files --strip-native-commands --compress=2)
DEPENDENCY_STAGE_DIR="$TARGET_DIR/package-runtime-deps"
MANIFEST_DIR="$INPUT_DIR/manifests"
PACKAGE_CACHE_DIR="$INPUT_DIR/packages"
CLIENT_PACKAGE_DIR="$PACKAGE_CACHE_DIR/client"
SERVER_PACKAGE_DIR="$PACKAGE_CACHE_DIR/server"
SUPPORT_LIB_DIR="$PACKAGE_CACHE_DIR/support/lib"
LAUNCHER_PACKAGE_DIR="$PACKAGE_CACHE_DIR/launcher"
LAUNCHER_PROFILE_SOURCE_DIR="$PROJECT_ROOT/launcher/profile-packages"
LWJGL_VERSION="3.4.1"
REQUIRED_LWJGL_FILES=(
  "lwjgl-$LWJGL_VERSION.jar"
  "lwjgl-glfw-$LWJGL_VERSION.jar"
  "lwjgl-opengl-$LWJGL_VERSION.jar"
  "lwjgl-stb-$LWJGL_VERSION.jar"
  "lwjgl-$LWJGL_VERSION-natives-linux.jar"
  "lwjgl-glfw-$LWJGL_VERSION-natives-linux.jar"
  "lwjgl-opengl-$LWJGL_VERSION-natives-linux.jar"
  "lwjgl-stb-$LWJGL_VERSION-natives-linux.jar"
)

require_tool() {
    local tool="$1"
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "Required tool '$tool' was not found on PATH." >&2
        exit 2
    fi
}

valid_jar() {
    local file="$1"
    [[ -f "$file" ]] || return 1
    [[ "$(wc -c < "$file" 2>/dev/null || echo 0)" -ge 128 ]] || return 1
    python3 - "$file" <<'PY' >/dev/null 2>&1
import sys
from pathlib import Path
p=Path(sys.argv[1])
sys.exit(0 if p.read_bytes()[:4] == b"PK\x03\x04" else 1)
PY
}

sha256_file() {
    sha256sum "$1" | awk '{print tolower($1)}'
}

relative_path() {
    python3 - "$1" "$2" <<'PY'
import os, sys
print(os.path.relpath(sys.argv[2], sys.argv[1]).replace(os.sep, '/'))
PY
}

stage_runtime_dependencies() {
    rm -rf "$DEPENDENCY_STAGE_DIR"
    mkdir -p "$DEPENDENCY_STAGE_DIR" "$SUPPORT_LIB_DIR"
    mvn -B -DincludeScope=runtime -Dmdep.copyPom=false -DoutputDirectory="$DEPENDENCY_STAGE_DIR" dependency:copy-dependencies
    cp -a "$DEPENDENCY_STAGE_DIR"/. "$SUPPORT_LIB_DIR"/

    local missing=()
    local required hit found
    for required in "${REQUIRED_LWJGL_FILES[@]}"; do
        found=0
        while IFS= read -r -d '' hit; do
            if valid_jar "$hit"; then
                found=1
                break
            fi
        done < <(find "$SUPPORT_LIB_DIR" -type f -name "$required" -print0 2>/dev/null)
        if [[ "$found" -eq 0 ]]; then
            missing+=("$required")
        fi
    done
    if [[ "${#missing[@]}" -gt 0 ]]; then
        echo "Packaged Linux LWJGL runtime is incomplete. Missing or invalid:" >&2
        printf '  %s\n' "${missing[@]}" >&2
        exit 23
    fi
    echo "Launcher-managed Linux support libraries staged in $SUPPORT_LIB_DIR"
}

stage_launcher_profile_packages() {
    if [[ ! -d "$LAUNCHER_PROFILE_SOURCE_DIR" ]]; then
        echo "Missing launcher profile package source directory: $LAUNCHER_PROFILE_SOURCE_DIR" >&2
        exit 24
    fi
    local dest="$LAUNCHER_PACKAGE_DIR/profile-packages"
    rm -rf "$dest"
    mkdir -p "$LAUNCHER_PACKAGE_DIR"
    cp -a "$LAUNCHER_PROFILE_SOURCE_DIR" "$dest"
    echo "Launcher profile packages staged in $dest"
}

write_thin_launcher_manifest() {
    local client_jar="$1"
    local server_jar="$2"
    mkdir -p "$MANIFEST_DIR"
    local client_name server_name support_json entry rel
    client_name="$(basename "$client_jar")"
    server_name="$(basename "$server_jar")"
    support_json=""
    while IFS= read -r -d '' entry; do
        rel="$(relative_path "$PACKAGE_CACHE_DIR" "$entry")"
        local line="      {\"path\": \"$rel\", \"sha256\": \"$(sha256_file "$entry")\", \"size\": $(wc -c < "$entry")}"
        if [[ -z "$support_json" ]]; then
            support_json="$line"
        else
            support_json="$support_json,
$line"
        fi
    done < <(find "$SUPPORT_LIB_DIR" -type f -name '*.jar' -print0 | sort -z)
    cat > "$MANIFEST_DIR/linux-runtime-manifest.json" <<EOF
{
  "schema": 1,
  "distribution_model": "installer-thin-launcher-client-server",
  "version": "$APP_VERSION",
  "platform": "linux-x64",
  "launcher": {
    "role": "installed-orchestrator",
    "main_class": "mechanist.launcher.ThinLauncherMain",
    "profile_packages": "packages/launcher/profile-packages",
    "owns": ["wrapper-detection", "fallback-profile-generation", "manifest-verification", "package-acquisition", "update", "rollback", "launch"]
  },
  "client": {
    "path": "packages/client/$client_name",
    "sha256": "$(sha256_file "$client_jar")",
    "size": $(wc -c < "$client_jar"),
    "main_class": "mechanist.TheMechanist"
  },
  "server": {
    "path": "packages/server/$server_name",
    "sha256": "$(sha256_file "$server_jar")",
    "size": $(wc -c < "$server_jar"),
    "main_class": "mechanist.MechanistServerMain"
  },
  "launcher_profile": {
    "fallback_human_portraits": "launcher-human-8x8-v1",
    "celebrity_portraits": "launcher-celebrity-portraits-v1",
    "celebrity_name_detection": "launcher-celebrity-name-detection-v1",
    "wrapper_detection": ["steam", "gog", "none"]
  },
  "support_libraries": [
$support_json
  ]
}
EOF
}

require_tool java
require_tool javac
require_tool jlink
require_tool jpackage
require_tool mvn
require_tool python3

if [[ -z "${JAVA_HOME:-}" ]]; then
    echo "JAVA_HOME is not set. Set JAVA_HOME to a Java 17 JDK before packaging." >&2
    exit 2
fi

if [[ ! -d "$JAVA_HOME/jmods" ]]; then
    echo "JAVA_HOME does not point to a full JDK with jmods: $JAVA_HOME" >&2
    exit 2
fi

cd "$PROJECT_ROOT"
rm -rf "$DIST_DIR" "$RUNTIME_DIR" "$INPUT_DIR"
mkdir -p "$DIST_DIR" "$INPUT_DIR" "$APP_IMAGE_DIR" "$CLIENT_PACKAGE_DIR" "$SERVER_PACKAGE_DIR" "$SUPPORT_LIB_DIR" "$LAUNCHER_PACKAGE_DIR"

"$PROJECT_ROOT/scripts/security/generate-sensitive-strings.sh"
mvn -B -DskipTests package

CLIENT_SOURCE="$TARGET_DIR/TheMechanist-all.jar"
SERVER_SOURCE="$TARGET_DIR/TheMechanistServer-all.jar"
if [[ ! -s "$CLIENT_SOURCE" ]]; then
    echo "Required development client jar was not produced: $CLIENT_SOURCE" >&2
    exit 4
fi
if [[ ! -s "$SERVER_SOURCE" ]]; then
    echo "Required development server jar was not produced: $SERVER_SOURCE" >&2
    exit 4
fi
CLIENT_JAR="$CLIENT_PACKAGE_DIR/TheMechanist.jar"
SERVER_JAR="$SERVER_PACKAGE_DIR/TheMechanistServer.jar"
cp "$CLIENT_SOURCE" "$CLIENT_JAR"
cp "$SERVER_SOURCE" "$SERVER_JAR"
stage_runtime_dependencies
stage_launcher_profile_packages
write_thin_launcher_manifest "$CLIENT_JAR" "$SERVER_JAR"

jlink \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules "$CLIENT_MODULES" \
  --output "$RUNTIME_DIR" \
  "${JLINK_OPTIONS[@]}"

build_package_type() {
    local package_type="$1"
    local extra_args=()
    if [[ -f "$ICON_PATH" ]]; then
        extra_args+=(--icon "$ICON_PATH")
    fi
    jpackage \
      --type "$package_type" \
      --name "$APP_NAME" \
      --app-version "$APP_VERSION" \
      --vendor "$VENDOR" \
      --description "The Mechanist thin launcher and package orchestrator" \
      --runtime-image "$RUNTIME_DIR" \
      --input "$INPUT_DIR" \
      --main-jar "packages/client/TheMechanist.jar" \
      --main-class "mechanist.launcher.ThinLauncherMain" \
      --dest "$DIST_DIR" \
      --install-dir "/opt/the-mechanist-launcher" \
      --linux-shortcut \
      --linux-menu-group "Game" \
      --resource-dir "$PROJECT_ROOT/packaging/linux/resources" \
      "${extra_args[@]}"
}

build_app_image() {
    local extra_args=()
    if [[ -f "$ICON_PATH" ]]; then
        extra_args+=(--icon "$ICON_PATH")
    fi
    jpackage \
      --type app-image \
      --name "$APP_NAME" \
      --app-version "$APP_VERSION" \
      --vendor "$VENDOR" \
      --description "The Mechanist thin launcher and package orchestrator" \
      --runtime-image "$RUNTIME_DIR" \
      --input "$INPUT_DIR" \
      --main-jar "packages/client/TheMechanist.jar" \
      --main-class "mechanist.launcher.ThinLauncherMain" \
      --dest "$APP_IMAGE_DIR" \
      "${extra_args[@]}"
    if [[ -d "$APP_IMAGE_DIR/$APP_NAME" ]]; then
        tar -C "$APP_IMAGE_DIR" -czf "$DIST_DIR/TheMechanist_launcher_linux_portable_${APP_VERSION}.tar.gz" "$APP_NAME"
    else
        echo "jpackage app-image did not produce $APP_IMAGE_DIR/$APP_NAME" >&2
        exit 5
    fi
}

build_package_type deb

if command -v rpmbuild >/dev/null 2>&1; then
    build_package_type rpm
else
    echo "rpmbuild was not found; DEB was produced and RPM packaging was skipped on this runner." >&2
fi

build_app_image
find "$DIST_DIR" -type f ! -name SHA256SUMS.txt -print0 | sort -z | xargs -0 sha256sum > "$DIST_DIR/SHA256SUMS.txt"
cat > "$DIST_DIR/LINUX_INSTALLERS_README.txt" <<EOF
The Mechanist Linux installer outputs
====================================

Version: $APP_VERSION
Distribution model: installer -> thin launcher -> client -> server
Launcher entrypoint: mechanist.launcher.ThinLauncherMain
Package identity manifest: manifests/linux-runtime-manifest.json
Launcher-managed packages: packages/client, packages/server, packages/support/lib, packages/launcher/profile-packages
Wrapper detection: Steam/GOG/none, evaluated by thin launcher before client start
Fallback profile generation: launcher-owned, hash-based
Launcher portrait/name packages: human 8x8, celebrity portrait manifest, celebrity name detection manifest
LWJGL/support libraries: staged into packages/support/lib at package-build time
Game-launch dependency downloads: forbidden

Recommended Linux testing order:
1. Extract the portable launcher tarball and run the launcher image.
2. Confirm manifests/linux-runtime-manifest.json exists inside the installed image.
3. Confirm packages/client/TheMechanist.jar and packages/server/TheMechanistServer.jar exist.
4. Confirm packages/support/lib contains LWJGL core/native jars and runtime dependency jars.
5. Confirm packages/launcher/profile-packages contains the launcher profile packages.
6. Test DEB/RPM installer behavior after portable app-image verification.
EOF
echo "Linux installers written to $DIST_DIR"

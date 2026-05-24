#!/usr/bin/env bash
set -Eeuo pipefail

APP_NAME="The Mechanist"
APP_ID="the-mechanist"
APP_VERSION="0.9.10ix"
VENDOR="The Mechanist Project"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TARGET_DIR="$PROJECT_ROOT/target"
DIST_DIR="$PROJECT_ROOT/dist/installers/linux"
APP_IMAGE_DIR="$DIST_DIR/app-image"
RUNTIME_DIR="$PROJECT_ROOT/build/jlink/runtime-client-linux"
INPUT_DIR="$PROJECT_ROOT/build/package-input/linux"
ICON_PATH="$PROJECT_ROOT/assets/app/icons/the-mechanist-256.png"
CLIENT_MODULES="$(tr -d '\r\n ' < "$PROJECT_ROOT/packaging/jlink/client-modules.txt")"
JLINK_OPTIONS=(--strip-debug --no-man-pages --no-header-files --strip-native-commands --compress=2)

require_tool() {
    local tool="$1"
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "Required tool '$tool' was not found on PATH." >&2
        exit 2
    fi
}

require_tool java
require_tool javac
require_tool jlink
require_tool jpackage
require_tool mvn

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
mkdir -p "$DIST_DIR" "$INPUT_DIR" "$APP_IMAGE_DIR"

"$PROJECT_ROOT/scripts/security/generate-sensitive-strings.sh"
mvn -B -DskipTests package
"$PROJECT_ROOT/scripts/security/encrypt-obfuscation-mappings.sh"
if [[ ! -s "$TARGET_DIR/TheMechanist-obfuscated.jar" ]]; then
    echo "Required obfuscated client jar was not produced: $TARGET_DIR/TheMechanist-obfuscated.jar" >&2
    exit 4
fi
cp "$TARGET_DIR/TheMechanist-obfuscated.jar" "$INPUT_DIR/TheMechanist-obfuscated.jar"
if [[ -d "$PROJECT_ROOT/lib" ]]; then
    cp -a "$PROJECT_ROOT/lib" "$INPUT_DIR/lib"
fi

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
      --description "The Mechanist Java 17 desktop simulation client" \
      --runtime-image "$RUNTIME_DIR" \
      --input "$INPUT_DIR" \
      --main-jar "TheMechanist-obfuscated.jar" \
      --main-class "mechanist.TheMechanist" \
      --dest "$DIST_DIR" \
      --install-dir "/opt/the-mechanist" \
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
      --description "The Mechanist Java 17 desktop simulation client" \
      --runtime-image "$RUNTIME_DIR" \
      --input "$INPUT_DIR" \
      --main-jar "TheMechanist-obfuscated.jar" \
      --main-class "mechanist.TheMechanist" \
      --dest "$APP_IMAGE_DIR" \
      "${extra_args[@]}"
    if [[ -d "$APP_IMAGE_DIR/$APP_NAME" ]]; then
        tar -C "$APP_IMAGE_DIR" -czf "$DIST_DIR/TheMechanist_linux_portable_${APP_VERSION}.tar.gz" "$APP_NAME"
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
echo "Linux installers written to $DIST_DIR"

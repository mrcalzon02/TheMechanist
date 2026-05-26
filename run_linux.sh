#!/usr/bin/env bash
set -u

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR" || exit 1
LOG_FILE="$APP_DIR/launch_linux.log"
LWJGL_VERSION="3.4.1"
LWJGL_REQUIRED_FILES=(
  "lwjgl-$LWJGL_VERSION.jar"
  "lwjgl-glfw-$LWJGL_VERSION.jar"
  "lwjgl-opengl-$LWJGL_VERSION.jar"
  "lwjgl-stb-$LWJGL_VERSION.jar"
  "lwjgl-$LWJGL_VERSION-natives-linux.jar"
  "lwjgl-glfw-$LWJGL_VERSION-natives-linux.jar"
  "lwjgl-opengl-$LWJGL_VERSION-natives-linux.jar"
  "lwjgl-stb-$LWJGL_VERSION-natives-linux.jar"
)

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

assert_packaged_lwjgl_runtime() {
  local lib_dir="$APP_DIR/lib"
  local missing=()
  local required hit found
  for required in "${LWJGL_REQUIRED_FILES[@]}"; do
    found=0
    if [[ -d "$lib_dir" ]]; then
      while IFS= read -r -d '' hit; do
        if valid_jar "$hit"; then
          found=1
          break
        fi
      done < <(find "$lib_dir" -type f -name "$required" -print0 2>/dev/null)
    fi
    if [[ "$found" -eq 0 ]]; then
      missing+=("$required")
    fi
  done
  if [[ "${#missing[@]}" -gt 0 ]]; then
    echo "ERROR: packaged LWJGL runtime is incomplete. Missing or invalid files:"
    printf '  %s\n' "${missing[@]}"
    echo "LWJGL must be bundled by the installer/package build, not downloaded at game launch."
    return 23
  fi
  echo "Packaged LWJGL runtime verified for Linux: version=$LWJGL_VERSION"
}

build_classpath() {
  local cp="$APP_DIR/TheMechanist.jar"
  if [[ -d "$APP_DIR/lib" ]]; then
    while IFS= read -r -d '' jar; do
      cp="$cp:$jar"
    done < <(find "$APP_DIR/lib" -type f -name '*.jar' -print0 | sort -z)
  fi
  printf '%s' "$cp"
}

{
  echo "=== The Mechanist Linux launcher ==="
  date
  echo "App directory: $APP_DIR"

  if [[ ! -f "$APP_DIR/TheMechanist.jar" ]]; then
    echo "ERROR: TheMechanist.jar was not found beside run_linux.sh."
    echo "Extract the whole zip into one folder and launch from inside that folder."
    exit 2
  fi

  if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java was not found. Install Java 17 or newer, then run again."
    echo "Linux Mint example: sudo apt install openjdk-17-jre"
    exit 3
  fi

  echo "Java version:"
  java -version 2>&1
  assert_packaged_lwjgl_runtime || exit $?
  if [[ -d "$APP_DIR/lib" ]]; then
    echo "Runtime dependency jars under lib/:"
    find "$APP_DIR/lib" -type f -name '*.jar' -print | sort || true
  else
    echo "Runtime dependency lib directory: missing"
  fi
  CP="$(build_classpath)"
  echo "Classpath: $CP"
  echo "Startup preflight:"
  java -Dmechanist.requireLwjgl=true -cp "$CP" mechanist.WindowsLaunchHealthCheck
  PREFLIGHT_RC=$?
  if [[ "$PREFLIGHT_RC" -ne 0 ]]; then
    echo "ERROR: startup preflight failed with code $PREFLIGHT_RC."
    exit "$PREFLIGHT_RC"
  fi
  echo "Launching through explicit classpath so bundled libraries in lib/ are visible..."
} > "$LOG_FILE" 2>&1
preflight_status=$?
if [[ "$preflight_status" -ne 0 ]]; then
  echo "The Mechanist launcher failed during preflight. See $LOG_FILE" >&2
  exit "$preflight_status"
fi

CP="$(build_classpath)"
exec java -Dmechanist.requireLwjgl=true -cp "$CP" mechanist.TheMechanist "$@" >> "$LOG_FILE" 2>&1

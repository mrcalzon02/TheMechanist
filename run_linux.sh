#!/usr/bin/env bash
set -u

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR" || exit 1
LOG_FILE="$APP_DIR/launch_linux.log"
LWJGL_VERSION="3.4.1"
LWJGL_REPO="https://repo1.maven.org/maven2"
LWJGL_MODULES=("lwjgl" "lwjgl-glfw" "lwjgl-opengl" "lwjgl-stb")

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

download_file() {
  local url="$1"
  local target="$2"
  local tmp="${target}.tmp"
  rm -f "$tmp"
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 --connect-timeout 20 --max-time 120 -o "$tmp" "$url" || return 1
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$tmp" "$url" || return 1
  elif command -v python3 >/dev/null 2>&1; then
    python3 - "$url" "$tmp" <<'PY' || return 1
import sys, urllib.request
url, out = sys.argv[1], sys.argv[2]
req = urllib.request.Request(url, headers={"User-Agent":"TheMechanist-LWJGL-Bootstrap/0.9.10iy"})
with urllib.request.urlopen(req, timeout=120) as r, open(out, 'wb') as fh:
    while True:
        chunk = r.read(1024*256)
        if not chunk: break
        fh.write(chunk)
PY
  else
    echo "ERROR: no curl, wget, or python3 available for LWJGL bootstrap."
    return 1
  fi
  valid_jar "$tmp" || return 1
  mv -f "$tmp" "$target"
}

ensure_lwjgl_runtime() {
  if [[ "${MECHANIST_DISABLE_LWJGL_BOOTSTRAP:-}" == "true" ]]; then
    echo "LWJGL bootstrap disabled by MECHANIST_DISABLE_LWJGL_BOOTSTRAP=true."
    return 0
  fi
  local lib_dir="$APP_DIR/lib/lwjgl"
  mkdir -p "$lib_dir"
  echo "LWJGL bootstrap: version=$LWJGL_VERSION platform=linux target=$lib_dir"
  local module file url target
  for module in "${LWJGL_MODULES[@]}"; do
    file="${module}-${LWJGL_VERSION}.jar"
    url="${LWJGL_REPO}/org/lwjgl/${module}/${LWJGL_VERSION}/${file}"
    target="$lib_dir/$file"
    if valid_jar "$target"; then
      echo "LWJGL present: lib/lwjgl/$file"
    else
      echo "Installing LWJGL runtime: $file"
      download_file "$url" "$target" || { echo "ERROR: failed to install LWJGL jar: $file"; return 23; }
    fi
  done
  for module in "${LWJGL_MODULES[@]}"; do
    file="${module}-${LWJGL_VERSION}-natives-linux.jar"
    url="${LWJGL_REPO}/org/lwjgl/${module}/${LWJGL_VERSION}/${file}"
    target="$lib_dir/$file"
    if valid_jar "$target"; then
      echo "LWJGL present: lib/lwjgl/$file"
    else
      echo "Installing LWJGL runtime: $file"
      download_file "$url" "$target" || { echo "ERROR: failed to install LWJGL native jar: $file"; return 23; }
    fi
  done
  echo "LWJGL bootstrap complete."
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
  ensure_lwjgl_runtime || exit $?
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

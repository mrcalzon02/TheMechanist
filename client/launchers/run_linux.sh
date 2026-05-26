#!/usr/bin/env bash
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$RUNTIME_ROOT" || exit 1
LOG_FILE="$RUNTIME_ROOT/launch_linux.log"
JAR_PATH="$RUNTIME_ROOT/TheMechanist.jar"

build_classpath() {
  local cp="$JAR_PATH"
  if [[ -d "$RUNTIME_ROOT/lib" ]]; then
    while IFS= read -r -d '' jar; do
      cp="$cp:$jar"
    done < <(find "$RUNTIME_ROOT/lib" -type f -name '*.jar' -print0 | sort -z)
  fi
  printf '%s' "$cp"
}

{
  echo "=== The Mechanist segmented Linux client launcher ==="
  date
  echo "Script dir: $SCRIPT_DIR"
  echo "Runtime root: $RUNTIME_ROOT"

  if [[ ! -f "$JAR_PATH" ]]; then
    echo "ERROR: TheMechanist.jar was not found at $JAR_PATH."
    exit 2
  fi

  if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java was not found. Install Java 17 or newer, then run again."
    exit 17
  fi

  echo "Java version:"
  java -version 2>&1
  CP="$(build_classpath)"
  echo "Classpath: $CP"
  echo "Startup preflight:"
  java -cp "$CP" mechanist.WindowsLaunchHealthCheck
  PREFLIGHT_RC=$?
  if [[ "$PREFLIGHT_RC" -ne 0 ]]; then
    echo "ERROR: startup preflight failed with code $PREFLIGHT_RC."
    exit "$PREFLIGHT_RC"
  fi
  echo "Launching client..."
} > "$LOG_FILE" 2>&1
preflight_status=$?
if [[ "$preflight_status" -ne 0 ]]; then
  echo "The Mechanist client launcher failed during preflight. See $LOG_FILE" >&2
  exit "$preflight_status"
fi

CP="$(build_classpath)"
exec java -Xms512m -Xmx4096m -cp "$CP" mechanist.TheMechanist "$@" >> "$LOG_FILE" 2>&1

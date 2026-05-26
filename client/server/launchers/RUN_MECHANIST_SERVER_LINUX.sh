#!/usr/bin/env bash
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$SERVER_ROOT" || exit 1

APP_NAME="The Mechanist Server"
JAR_NAME="TheMechanistServer.jar"
JAR_PATH="$SERVER_ROOT/$JAR_NAME"
LOG_FILE="$SERVER_ROOT/launch-server-linux.log"

java_major_version() {
  local line version major
  line="$(java -version 2>&1 | head -n 1 || true)"
  version="$(printf '%s' "$line" | sed -n 's/.*version "\([^"]*\)".*/\1/p')"
  if [[ -z "$version" ]]; then
    printf '%s' "0"
    return
  fi
  if [[ "$version" == 1.* ]]; then
    major="$(printf '%s' "$version" | cut -d. -f2 | sed 's/[^0-9].*$//')"
  else
    major="$(printf '%s' "$version" | sed 's/[^0-9].*$//')"
  fi
  if [[ -z "$major" ]]; then
    printf '%s' "0"
  else
    printf '%s' "$major"
  fi
}

if [[ ! -f "$JAR_PATH" ]]; then
  echo "ERROR: $JAR_NAME was not found at $JAR_PATH." >&2
  echo "The server package may not have extracted correctly." >&2
  exit 2
fi

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java was not found. Install Java 17 or newer, then run again." >&2
  exit 17
fi

JAVA_MAJOR="$(java_major_version)"
if [[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] && [[ "$JAVA_MAJOR" -gt 0 ]] && [[ "$JAVA_MAJOR" -lt 17 ]]; then
  echo "ERROR: Java 17 or newer is required. Detected Java major version: $JAVA_MAJOR" >&2
  java -version >&2
  exit 17
fi

if [[ "$#" -eq 0 ]]; then
  SERVER_ARGS=(--status)
else
  SERVER_ARGS=("$@")
fi

{
  echo "=================================================="
  echo "$APP_NAME Linux launcher started at $(date '+%Y-%m-%d %H:%M:%S')"
  echo "Script dir: $SCRIPT_DIR"
  echo "Server root: $SERVER_ROOT"
  echo "Jar path: $JAR_PATH"
  echo "Java version:"
  java -version 2>&1
  echo "Launching $APP_NAME..."
  echo "Server args: ${SERVER_ARGS[*]}"
} > "$LOG_FILE" 2>&1

exec java -Xms256m -Xmx2048m -jar "$JAR_PATH" "${SERVER_ARGS[@]}" >> "$LOG_FILE" 2>&1

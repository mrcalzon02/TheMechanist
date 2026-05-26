#!/usr/bin/env bash
set -u

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR" || exit 1

APP_NAME="The Mechanist Server"
JAR_NAME="TheMechanistServer.jar"
JAR_PATH="$APP_DIR/$JAR_NAME"
LOG_FILE="$APP_DIR/launch-server-linux.log"

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
  echo "ERROR: $JAR_NAME was not found beside RUN_MECHANIST_SERVER_LINUX.sh." >&2
  echo "Extract the whole zip into one folder and launch from inside that folder." >&2
  exit 2
fi

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java was not found. Install Java 17 or newer, then run again." >&2
  echo "Linux Mint example: sudo apt install openjdk-17-jre" >&2
  exit 3
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
  echo "Working directory: $APP_DIR"
  echo "Launcher path: ${BASH_SOURCE[0]}"
  echo "Jar path: $JAR_PATH"
  echo "Java version:"
  java -version 2>&1
  echo "Startup preflight:"
  java -cp "$JAR_PATH" mechanist.WindowsLaunchHealthCheck
  PREFLIGHT_RC=$?
  if [[ "$PREFLIGHT_RC" -ne 0 ]]; then
    echo "ERROR: startup preflight failed with code $PREFLIGHT_RC."
    exit "$PREFLIGHT_RC"
  fi
  echo "Preflight OK. Launching $APP_NAME..."
  echo "Server args: ${SERVER_ARGS[*]}"
} > "$LOG_FILE" 2>&1
preflight_status=$?
if [[ "$preflight_status" -ne 0 ]]; then
  echo "$APP_NAME launcher failed during preflight. See $LOG_FILE" >&2
  exit "$preflight_status"
fi

echo "Starting $APP_NAME..."
echo "Launch log: $LOG_FILE"
exec java -Xms256m -Xmx2048m -jar "$JAR_PATH" "${SERVER_ARGS[@]}" >> "$LOG_FILE" 2>&1

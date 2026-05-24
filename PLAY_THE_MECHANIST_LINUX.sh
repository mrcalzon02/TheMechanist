#!/usr/bin/env bash
# Double-click friendly launcher for Linux/XFCE file managers.
# It delegates to run_linux.sh so there is only one real launch path.
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR" || exit 1
exec "$APP_DIR/run_linux.sh" "$@"

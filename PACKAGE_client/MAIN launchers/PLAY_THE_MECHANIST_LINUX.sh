#!/usr/bin/env bash
# Double-click friendly launcher for Linux/XFCE file managers.
# Delegates to the segmented client launcher so there is only one real client launch path.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$SCRIPT_DIR/run_linux.sh" "$@"

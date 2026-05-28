#!/usr/bin/env bash
set -euo pipefail

cat <<'EOF'

============================================================
The Mechanist legacy Git launcher installer is retired
============================================================

Gate 2 requires a native installer/app-image that carries the thin launcher,
runtime manifests, and manifest-verified package seeds. This legacy installer
no longer installs a Git clone updater or performs an initial repository clone.

Build the current Linux launcher package with:
  ./scripts/package/build-linux-installers.sh

Then test the portable app-image before DEB/RPM installer testing.
EOF

exit 2

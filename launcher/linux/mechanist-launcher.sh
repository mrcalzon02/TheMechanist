#!/usr/bin/env bash
set -euo pipefail

cat <<'EOF'

============================================================
The Mechanist legacy Git launcher is retired
============================================================

Gate 2 requires installer -> thin launcher -> manifest-verified client/server packages.
This script no longer clones or updates the full development repository.

Use the native packaging pipeline to build the manifest launcher:
  ./scripts/package/build-linux-installers.sh

Then run the produced launcher app-image or installer.
EOF

exit 2

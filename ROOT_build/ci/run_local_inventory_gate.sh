#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."
exec python3 ROOT_build/ci/run_local_inventory_gate.py "$@"

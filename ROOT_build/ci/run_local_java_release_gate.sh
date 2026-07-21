#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

PYTHON_BIN="${PYTHON_BIN:-python3}"
OUTPUT="${MECHANIST_LOCAL_GATE_OUTPUT:-$ROOT/dist/local-java-gate}"
REPORT="${MECHANIST_LOCAL_GATE_REPORT:-$ROOT/dist/local-java-gate-report.json}"

exec "$PYTHON_BIN" ROOT_build/ci/run_local_java_release_gate.py \
  --release-hardened \
  --output "$OUTPUT" \
  --report "$REPORT" \
  "$@"

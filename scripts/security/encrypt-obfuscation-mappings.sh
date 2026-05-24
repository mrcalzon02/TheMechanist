#!/usr/bin/env bash
set -Eeuo pipefail
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BUILD_DIR="$PROJECT_ROOT/build/tools/obfuscation"
TOOL_SRC="$PROJECT_ROOT/tools/obfuscation/MappingEncryptionTool.java"
KEY_FILE="${MECHANIST_MAPPING_KEY_FILE:-$PROJECT_ROOT/build/secure-local/mapping.key}"
mkdir -p "$BUILD_DIR" "$PROJECT_ROOT/dist/secure-maps"
javac --release 17 -d "$BUILD_DIR" "$TOOL_SRC"
for target_name in client server; do
  raw="$PROJECT_ROOT/target/proguard/$target_name/mapping.raw.txt"
  encrypted="$PROJECT_ROOT/dist/secure-maps/$target_name-mapping.txt"
  if [[ ! -s "$raw" ]]; then
    echo "Expected ProGuard raw mapping was not found or is empty: $raw" >&2
    exit 3
  fi
  java -cp "$BUILD_DIR" MappingEncryptionTool \
    --encrypt \
    --in "$raw" \
    --out "$encrypted" \
    --key-file "$KEY_FILE" \
    --delete-input
  printf 'Encrypted ProGuard mapping: %s\n' "$encrypted"
done

#!/usr/bin/env bash
set -Eeuo pipefail
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BUILD_DIR="$PROJECT_ROOT/build/tools/obfuscation"
GENERATOR_SRC="$PROJECT_ROOT/tools/obfuscation/SensitiveStringTableGenerator.java"
INPUT="$PROJECT_ROOT/config/obfuscation/sensitive-strings.properties"
OUTPUT="$PROJECT_ROOT/src/mechanist/ObfuscatedStringTable.java"
mkdir -p "$BUILD_DIR" "$(dirname "$OUTPUT")"
javac --release 17 -d "$BUILD_DIR" "$GENERATOR_SRC"
java -cp "$BUILD_DIR" SensitiveStringTableGenerator \
  --input "$INPUT" \
  --output "$OUTPUT" \
  --package mechanist \
  --class ObfuscatedStringTable
printf 'Generated %s\n' "$OUTPUT"

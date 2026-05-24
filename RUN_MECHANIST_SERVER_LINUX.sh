#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
java -jar TheMechanistServer.jar "${@:---status}"

#!/usr/bin/env bash
set -euo pipefail

REPO_URL="https://github.com/mrcalzon02/TheMechanist.git"
BRANCH="main"
INSTALL_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/TheMechanist/repo"
NO_UPDATE=0
NO_LAUNCH=0
GAME_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --branch)
      BRANCH="$2"
      shift 2
      ;;
    --install-dir)
      INSTALL_DIR="$2"
      shift 2
      ;;
    --no-update)
      NO_UPDATE=1
      shift
      ;;
    --no-launch)
      NO_LAUNCH=1
      shift
      ;;
    --)
      shift
      GAME_ARGS+=("$@")
      break
      ;;
    *)
      GAME_ARGS+=("$1")
      shift
      ;;
  esac
done

step() {
  echo
  echo "============================================================"
  echo "$1"
  echo "============================================================"
}

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 10
  fi
}

step "The Mechanist GitHub Launcher"
echo "Repository: $REPO_URL"
echo "Branch:     $BRANCH"
echo "Install:    $INSTALL_DIR"

need_cmd git
mkdir -p "$(dirname "$INSTALL_DIR")"

if [[ ! -d "$INSTALL_DIR" ]]; then
  step "Cloning The Mechanist"
  git clone --branch "$BRANCH" "$REPO_URL" "$INSTALL_DIR"
elif [[ ! -d "$INSTALL_DIR/.git" ]]; then
  echo "Install directory exists but is not a Git repository: $INSTALL_DIR" >&2
  exit 11
else
  pushd "$INSTALL_DIR" >/dev/null
  if [[ "$NO_UPDATE" -eq 0 ]]; then
    step "Updating The Mechanist from GitHub"
    git fetch origin "$BRANCH"
    git checkout "$BRANCH"
    git pull --ff-only origin "$BRANCH"
  else
    step "Skipping update by request"
  fi
  popd >/dev/null
fi

if [[ "$NO_LAUNCH" -eq 1 ]]; then
  step "Update complete. Launch skipped by request."
  exit 0
fi

pushd "$INSTALL_DIR" >/dev/null
if [[ -x "./PLAY_THE_MECHANIST_LINUX.sh" ]]; then
  step "Launching The Mechanist"
  ./PLAY_THE_MECHANIST_LINUX.sh "${GAME_ARGS[@]}"
elif [[ -f "./PLAY_THE_MECHANIST_LINUX.sh" ]]; then
  chmod +x ./PLAY_THE_MECHANIST_LINUX.sh
  step "Launching The Mechanist"
  ./PLAY_THE_MECHANIST_LINUX.sh "${GAME_ARGS[@]}"
else
  echo "Could not find PLAY_THE_MECHANIST_LINUX.sh in installed repository." >&2
  exit 12
fi
popd >/dev/null

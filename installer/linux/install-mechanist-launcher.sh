#!/usr/bin/env bash
set -euo pipefail

APP_NAME="The Mechanist"
BRANCH="main"
NO_INITIAL_UPDATE=0
INSTALL_ROOT="${XDG_DATA_HOME:-$HOME/.local/share}/TheMechanist"
BIN_DIR="$HOME/.local/bin"
APPLICATIONS_DIR="$HOME/.local/share/applications"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --branch)
      BRANCH="$2"
      shift 2
      ;;
    --install-root)
      INSTALL_ROOT="$2"
      shift 2
      ;;
    --no-initial-update)
      NO_INITIAL_UPDATE=1
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SOURCE_LAUNCHER="$REPO_ROOT/launcher/linux/mechanist-launcher.sh"

step "$APP_NAME Launcher Installer"
need_cmd git

if [[ ! -f "$SOURCE_LAUNCHER" ]]; then
  echo "Could not locate launcher source: $SOURCE_LAUNCHER" >&2
  exit 11
fi

LAUNCHER_DIR="$INSTALL_ROOT/launcher"
REPO_DIR="$INSTALL_ROOT/repo"
mkdir -p "$LAUNCHER_DIR" "$BIN_DIR" "$APPLICATIONS_DIR"

step "Installing launcher files"
cp "$SOURCE_LAUNCHER" "$LAUNCHER_DIR/mechanist-launcher.sh"
chmod +x "$LAUNCHER_DIR/mechanist-launcher.sh"

cat > "$LAUNCHER_DIR/launcher-config.properties" <<EOF
repo=https://github.com/mrcalzon02/TheMechanist.git
branch=$BRANCH
repoDir=$REPO_DIR
createdBy=The Mechanist Phase O installer
EOF

cat > "$BIN_DIR/the-mechanist" <<EOF
#!/usr/bin/env bash
exec "$LAUNCHER_DIR/mechanist-launcher.sh" --branch "$BRANCH" --install-dir "$REPO_DIR" "\$@"
EOF
chmod +x "$BIN_DIR/the-mechanist"

cat > "$APPLICATIONS_DIR/the-mechanist.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=The Mechanist
Comment=Update and launch The Mechanist
Exec=$BIN_DIR/the-mechanist
Terminal=true
Categories=Game;
EOF

step "Installed launcher"
echo "Launcher:     $LAUNCHER_DIR/mechanist-launcher.sh"
echo "Command:      $BIN_DIR/the-mechanist"
echo "Desktop file: $APPLICATIONS_DIR/the-mechanist.desktop"
echo "Game repo:    $REPO_DIR"
echo "Branch:       $BRANCH"

if [[ "$NO_INITIAL_UPDATE" -eq 0 ]]; then
  step "Performing first update/clone without launching"
  "$LAUNCHER_DIR/mechanist-launcher.sh" --branch "$BRANCH" --install-dir "$REPO_DIR" --no-launch
fi

step "Installer complete"
echo "Run: the-mechanist"
echo "If your shell cannot find it yet, add this to PATH: $BIN_DIR"

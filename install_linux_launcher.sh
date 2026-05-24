#!/usr/bin/env bash
set -u
APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$APP_DIR" || exit 1
chmod +x run_linux.sh PLAY_THE_MECHANIST_LINUX.sh install_linux_launcher.sh "The Mechanist.desktop" 2>/dev/null || true
mkdir -p "$HOME/Desktop" "$HOME/.local/share/applications"

# Install the application icon into the user's hicolor theme so .desktop launchers
# can resolve Icon=the-mechanist without needing a brittle absolute path.
for size in 16 32 48 64 128 256 512; do
  src="$APP_DIR/assets/app/icons/the-mechanist-${size}.png"
  dst_dir="$HOME/.local/share/icons/hicolor/${size}x${size}/apps"
  if [ -f "$src" ]; then
    mkdir -p "$dst_dir"
    cp "$src" "$dst_dir/the-mechanist.png" 2>/dev/null || true
  fi
done
if command -v gtk-update-icon-cache >/dev/null 2>&1; then
  gtk-update-icon-cache -q "$HOME/.local/share/icons/hicolor" 2>/dev/null || true
fi

cp "The Mechanist.desktop" "$HOME/Desktop/The Mechanist.desktop" 2>/dev/null || true
cp "The Mechanist.desktop" "$HOME/.local/share/applications/the-mechanist.desktop" 2>/dev/null || true
chmod +x "$HOME/Desktop/The Mechanist.desktop" "$HOME/.local/share/applications/the-mechanist.desktop" 2>/dev/null || true
if command -v gio >/dev/null 2>&1; then
  gio set "$HOME/Desktop/The Mechanist.desktop" metadata::trusted true 2>/dev/null || true
fi
echo "Linux launcher permissions and The Mechanist icon refreshed. Try double-clicking PLAY_THE_MECHANIST_LINUX.sh or The Mechanist.desktop."
echo "If your file manager still blocks launchers, run: ./run_linux.sh"

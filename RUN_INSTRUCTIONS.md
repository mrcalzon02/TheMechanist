# Run Instructions — The Mechanist

Requires Java 17 or newer.

## Linux Mint / XFCE recommended path

1. Extract the full zip into one folder.
2. Double-click `PLAY_THE_MECHANIST_LINUX.sh`.
3. If your desktop accepts `.desktop` launchers, you may also double-click `The Mechanist.desktop`.

If Linux blocks the launcher because it came from a downloaded archive, run this once from inside the extracted folder:

```bash
chmod +x run_linux.sh PLAY_THE_MECHANIST_LINUX.sh "The Mechanist.desktop" install_linux_launcher.sh
./run_linux.sh
```

You can also refresh/install the desktop launcher with:

```bash
./install_linux_launcher.sh
```

Direct jar fallback:

```bash
java -jar TheMechanist.jar
```

`run_linux.sh` writes startup details to `launch_linux.log` if Java or package layout problems occur.

Keep `assets/` beside `TheMechanist.jar`. Optional art-pack ZIPs belong in `assets/artpacks/`; optional audio/music-pack ZIPs belong in `assets/audiopacks/`. Do not manually extract optional packs; the game manages cache extraction on launch.


## Server runtime

For the separate local/headless server namespace initializer, run `java -jar TheMechanistServer.jar --status` or use `RUN_MECHANIST_SERVER_LINUX.sh` / `RUN_MECHANIST_SERVER_WINDOWS.bat`. Server state is written under `saves/server/`; desktop single-player saves remain under `saves/singleplayer/`.

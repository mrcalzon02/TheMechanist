# Run Instructions — The Mechanist Client

Requires Java 17 or newer.

## Intended delivery path

The intended user path is:

```text
installer → thin launcher → client → server
```

The installer installs the thin launcher. The thin launcher verifies manifests and acquires or updates the client package, server package, and support libraries before launching the client. The client may verify package completeness, but it must not download support libraries during game startup.

## Direct development package path

For direct extracted-package testing before the true thin launcher entrypoint is complete, use the platform launcher in the extracted package folder.

Linux:

```bash
chmod +x run_linux.sh PLAY_THE_MECHANIST_LINUX.sh "The Mechanist.desktop"
./run_linux.sh
```

Windows:

```bat
RUN_THE_MECHANIST_WINDOWS.bat
```

Direct jar fallback:

```bash
java -jar TheMechanist.jar
```

## Package layout expectation

A client runtime package should include or be given by the launcher:

```text
TheMechanist.jar
TheMechanistServer.jar
lib/ or packages/support/lib/
assets/
settings/ packaged defaults
profiles/ packaged defaults
```

`run_linux.sh` writes startup details to `launch_linux.log`. The Windows launcher writes diagnostics to `%LOCALAPPDATA%\\TheMechanist\\logs\\launch-client.log` when possible.

## Server runtime

For the separate local/headless server namespace initializer, run:

```bash
java -jar TheMechanistServer.jar --status
```

or use:

```text
RUN_MECHANIST_SERVER_LINUX.sh
RUN_MECHANIST_SERVER_WINDOWS.bat
```

Server state is written under `saves/server/`; desktop single-player saves remain under `saves/singleplayer/`.

# Phase Z — Native Executable and Runtime Packaging Pipeline

## Purpose

Phase Z transitions The Mechanist from script-first execution toward professional installable artifacts.

The target user path is:

```text
Installer -> Launcher executable -> Game menu executable -> Client -> Local/internal server
```

The scripts in this phase intentionally produce app-image / portable outputs before installer outputs. This follows the project standard that bundled runtime behavior must be testable before a setup wizard is trusted.

## Current outputs

Windows packaging scripts:

```text
tools/packaging/windows/build_launcher_app_image.ps1
tools/packaging/windows/build_game_app_image.ps1
tools/packaging/windows/build_server_app_image.ps1
tools/packaging/windows/build_launcher_installer_exe.ps1
```

Release/verification utilities:

```text
tools/packaging/scan_java17_classfiles.py
tools/packaging/generate_release_manifest.py
```

## Launcher app-image

Run from repository root or directly from its folder:

```powershell
powershell -ExecutionPolicy Bypass -File tools/packaging/windows/build_launcher_app_image.ps1
```

Expected output:

```text
dist/native/windows/app-image/
```

This builds the Java 17 Swing launcher module and packages it with `jpackage --type app-image`.

## Game app-image

Run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/packaging/windows/build_game_app_image.ps1
```

Expected output:

```text
dist/native/windows/game-app-image/
```

This builds the main Maven project, chooses obfuscated jars when available, falls back to shaded jars when needed, scans classfile major versions, then creates a client app-image.

## Server app-image

Run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/packaging/windows/build_server_app_image.ps1
```

Expected output:

```text
dist/native/windows/server-app-image/
```

This creates a bundled executable image for the internal/headless server entrypoint. This does not claim public multiplayer readiness; it is a packaging/runtime artifact for the existing server-authority line.

## Launcher EXE installer

Run only after the launcher app-image has been smoke-tested:

```powershell
powershell -ExecutionPolicy Bypass -File tools/packaging/windows/build_launcher_installer_exe.ps1
```

Expected output:

```text
dist/native/windows/launcher-installer/
```

Windows installer generation requires the WiX Toolset on PATH. The script detects `wix.exe` or the WiX v3 `candle.exe` / `light.exe` pair and fails with an explicit message if installer prerequisites are missing. If icon authority has not yet produced `assets/app/icons/the-mechanist.ico`, the installer is created without a custom icon and prints that fact.

## Release manifest

After producing app-images or installers, generate a manifest:

```powershell
python tools/packaging/generate_release_manifest.py `
  --repo-root . `
  --output dist/native/windows/release-manifest.json `
  --channel dev `
  --artifact dist/native/windows/app-image `
  --artifact dist/native/windows/game-app-image `
  --artifact dist/native/windows/server-app-image
```

The manifest records build time, repository branch/commit/dirty state, Java/Python/builder platform details, artifact sizes, file counts, and SHA-256 hashes or tree hashes.

## Java 17 verification

Java 17 classfile major version is 61. No shipped class may exceed that.

Manual scan example:

```powershell
python tools/packaging/scan_java17_classfiles.py target/TheMechanist-all.jar target/TheMechanistServer-all.jar launcher/java/target/mechanist-launcher-0.1.0.jar
```

## Current limitations

- These scripts are packaging scaffolds and must be run on Windows for Windows-native outputs.
- EXE installer output is not yet signed.
- Windows icon wiring is optional and still needs a dedicated asset authority pass.
- The game client and server app-images are separate package outputs; final installer composition is still a later pass.
- Native installer artifacts must be produced on the target operating system.

## Next packaging pass

- Add icon authority wiring for EXE/window/taskbar/installer.
- Add runtime information panel in the launcher.
- Add a single orchestrator script that builds launcher, game, server, manifests, and optional installer in one verified sequence.
- Add CI/build-action hooks once local Windows packaging behavior is proven.

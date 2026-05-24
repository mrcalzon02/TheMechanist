# Phase Z — Native Executable and Runtime Packaging Pipeline

## Purpose

Phase Z transitions The Mechanist from script-first execution toward professional installable artifacts.

The target user path is:

```text
Installer -> Launcher executable -> Game menu executable -> Client -> Local/internal server
```

The scripts in this phase intentionally produce app-image / portable outputs before installer outputs. This follows the project standard that bundled runtime behavior must be testable before a setup wizard is trusted.

## Current outputs

Initial Windows packaging scripts:

```text
tools/packaging/windows/build_launcher_app_image.ps1
tools/packaging/windows/build_game_app_image.ps1
```

Java 17 classfile scanner:

```text
tools/packaging/scan_java17_classfiles.py
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

## Java 17 verification

Java 17 classfile major version is 61. No shipped class may exceed that.

Manual scan example:

```powershell
python tools/packaging/scan_java17_classfiles.py target/TheMechanist-all.jar target/TheMechanistServer-all.jar launcher/java/target/mechanist-launcher-0.1.0.jar
```

## Current limitations

- These scripts create app-images, not final signed installers.
- EXE/MSI installer packaging still needs a follow-up pass.
- Windows icon wiring is not complete yet.
- Server executable packaging still needs its own jpackage entrypoint.
- WiX detection is not yet used because this pass does not build MSI/EXE installers.
- Native installer artifacts must be produced on the target operating system.

## Next packaging pass

- Add launcher EXE/MSI installer script.
- Add server executable/app-image script.
- Add Windows `.ico` and installer icon authority wiring.
- Add runtime information panel in the launcher.
- Add release manifest generation containing git commit, build time, Java version, package selections, and artifact hashes.

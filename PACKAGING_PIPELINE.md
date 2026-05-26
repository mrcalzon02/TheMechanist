# The Mechanist — Native Packaging Pipeline

This guide describes the Java 17 packaging pipeline for producing platform packages for The Mechanist. The distribution model is now explicitly:

```text
installer → thin launcher → client → server
```

The installer should install the smallest durable launcher/orchestrator layer and its own runtime support. The thin launcher then owns package manifest verification, acquisition, update, rollback, and launch of the client package, server package, and required support libraries. The player should not need to download the full development repository to run the launcher.

## Stage 1 — Source build and runtime artifacts

The build is driven by `pom.xml` and compiles Java source from `src/`. Maven may declare runtime dependencies such as networking libraries, controller bridges, and rendering libraries, but dependency declaration is not runtime delivery by itself.

The normal development package path produces executable shaded jars:

```text
target/TheMechanist-all.jar
target/TheMechanistServer-all.jar
```

Build command:

```bash
mvn -B -DskipTests package
```

ProGuard is isolated behind the explicit `release-obfuscation` profile while release hardening is still under development. Do not require ProGuard configuration or obfuscated jars for the ordinary development package path.

## Stage 2 — Launcher-managed package layout

Packaging scripts stage a launcher-owned runtime layout rather than relying on the repository root:

```text
manifests/
packages/client/TheMechanist.jar
packages/server/TheMechanistServer.jar
packages/support/lib/*.jar
```

The platform runtime manifest records package identity, version, platform, paths, sizes, hashes, and main classes. The launcher must treat that manifest as the package identity source of truth.

Required support libraries such as LWJGL, platform native LWJGL jars, controller bridges such as Jamepad or its successor, Netty when used by a launched package, and future support libraries must be staged into `packages/support/lib/` by the packaging/update/acquisition stage. Game launchers and the client runtime must not download support libraries opportunistically during game startup.

The client may verify that required support libraries are present and fail loudly with a visible log if the installed package is incomplete. Verification is allowed; launch-time acquisition is not.

## Stage 3 — jlink runtime trimming

Package scripts may create custom Java 17 runtime images with `jlink`. Runtime images contain JDK modules only. Application jars and support-library jars live in the launcher-managed package layout.

The graphical launcher/client runtime requires `java.desktop` for Swing/AWT. Server runtimes should avoid desktop-rendering modules unless they are required by a verified server path. TLS, crypto, management, logging, and unsupported/internal-access modules should be included only when required by packaged dependencies and verified runtime behavior.

A typical jlink call has this shape:

```bash
jlink \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules "$CLIENT_MODULES" \
  --output build/jlink/runtime-launcher-linux \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --strip-native-commands \
  --compress=2
```

Runtime image size depends on the JDK vendor, platform, compression, included modules, fonts, and native libraries.

## Stage 4 — Native packages

`jpackage` cannot produce every native package from one operating system. Linux packages must be built on Linux; Windows MSI/EXE packages must be built on Windows.

Linux script:

```bash
./scripts/package/build-linux-installers.sh
```

Windows script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1
```

A quick Windows local-testing path may use existing client/server jars when the script supports it:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1 -UseExistingJar
```

The Windows script should emit a portable launcher app-image output in addition to installer outputs when possible. The portable output is the first test target because it verifies the staged package layout before installer wizard behavior is diagnosed.

Installer scripts must print clear diagnostics for missing Java, Maven, jpackage, WiX, icon files, runtime images, jars, dependency staging, LWJGL support libraries, or expected output paths. They must not fail as a silent one-frame console flicker.

## Stage 5 — CI artifact bundle

The GitHub Actions workflow may run platform-specific packaging jobs on native platform runners and then assemble the resulting artifacts into a unified download bundle.

The workflow should keep platform artifacts separately inspectable before final bundling. Checksums should be written beside installer outputs and preserved in the final artifact.

## User data directory separation

Installer targets are read-only or administrator-controlled locations. Mutable player data must route through `GameStorageManager`, not the application install path.

Default roots:

```text
Windows: %USERPROFILE%\Documents\TheMechanist\
Linux:   $XDG_DATA_HOME/TheMechanist/ or $HOME/.local/share/TheMechanist/
```

Strict subdirectories:

```text
TheMechanist/saves/
TheMechanist/saves/data/profiles/
TheMechanist/export/
TheMechanist/mods/
TheMechanist/modsarchived/
```

The storage manager must reject absolute child paths, `..` traversal, and file names containing directory separators before moving mods, reading runtime files, writing exports, or installing content.

## Release checklist

1. Ensure Java 17 JDK is installed and `JAVA_HOME` points to it.
2. Read the active durable governing documents before code/package work.
3. Run source compile or Maven package with Java 17 compatibility.
4. Run targeted smoke tests for touched systems.
5. Rebuild client and server jars from the corrected source tree.
6. Stage client, server, support libraries, and manifests into the launcher-managed package layout.
7. Verify manifest hashes and required support-library presence, including platform LWJGL native jars when required.
8. Scan shipped jars/class directories for Java 17 classfile compatibility.
9. Build platform packages on their native operating systems.
10. Verify checksums and zip/archive integrity.
11. State honestly what was not manually tested.

## Crash log de-obfuscation workflow

Release builds should keep encrypted mapping artifacts in a developer-only location. When an obfuscated crash trace is submitted, the developer should select the matching build token, the encrypted mapping payload, and the external mapping key through the developer/admin de-obfuscation tool. Missing mappings should be reported inline rather than crashing the tool or destroying the original report structure.

## Rendering and optional adapters

The desktop client remains responsible for honest runtime reporting. Declaring a native rendering, networking, controller, platform, or workshop dependency is not the same as initializing that adapter. Startup and packaging diagnostics must report whether the adapter is actually present and active.

Headless server profiles must not force desktop rendering pipeline flags. Client graphics profiles may request platform Java2D acceleration before Swing startup where supported, while preserving safe fallback behavior.

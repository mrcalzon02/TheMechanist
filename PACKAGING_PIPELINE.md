# The Mechanist — Native Packaging Pipeline

This guide describes the Java 17 packaging pipeline for producing native platform packages for The Mechanist. The pipeline is intentionally separated into dependency assembly, runtime trimming, native package production, artifact bundling, and verification so each step can be inspected independently.

## Stage 1 — Dependency isolation

The build is driven by `pom.xml` and compiles Java source from `src/`. Maven may declare runtime dependencies such as networking libraries, controller bridges, and optional rendering libraries, but the package must distinguish between declared dependencies, compiled adapter seams, and actually initialized runtime behavior.

The Maven package path produces executable application jars, normally including:

```text
target/TheMechanist-all.jar
target/TheMechanistServer-all.jar
```

Build command:

```bash
mvn -B -DskipTests package
```

The shaded jars are classpath artifacts. They are separate from any custom Java runtime image: the runtime image contains JDK modules, while the shaded jars contain application bytecode, resources, assets, and library bytecode.

## Stage 1B — Obfuscation and secure mapping retention

Release packaging may run ProGuard over the shaded jars and produce obfuscated client/server artifacts. Public modding API classes, launch entrypoints, Java 17 record/sealed metadata, and any verified reflection paths must remain stable through explicit keep rules.

Raw mapping files must not be distributed with player-facing packages. They should be encrypted or otherwise stored in a developer-only location immediately after obfuscation. Developer de-obfuscation keys must not ship inside the player runtime.

Sensitive string-table generation is an obfuscation barrier only. It may reduce obvious constant-pool search anchors, but it must not be used to store real secrets in client or server bytecode.

## Stage 2 — jlink runtime trimming

Package scripts may create custom Java 17 runtime images with `jlink`. Runtime images contain JDK modules only; application and dependency bytecode remains in the packaged jars.

The desktop runtime requires `java.desktop` for Swing/AWT. Server runtimes should avoid desktop-rendering modules unless they are required by a verified server path. TLS, crypto, management, logging, and unsupported/internal-access modules should be included only when required by packaged dependencies and verified runtime behavior.

A typical jlink call has this shape:

```bash
jlink \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules "$CLIENT_MODULES" \
  --output build/jlink/runtime-client-linux \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --strip-native-commands \
  --compress=2
```

Runtime image size depends on the JDK vendor, platform, compression, included modules, fonts, and native libraries.

## Stage 3 — Native packages

`jpackage` cannot produce every native package from one operating system. Linux packages must be built on Linux; Windows MSI/EXE packages must be built on Windows.

Linux script:

```bash
./scripts/package/build-linux-installers.sh
```

Windows script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1
```

A quick Windows local-testing path may use an existing jar when the script supports it:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1 -UseExistingJar
```

The Windows script should emit a portable app-image output in addition to installer outputs when possible. The portable output is the first test target because it verifies the packaged runtime before installer wizard behavior is diagnosed.

Installer scripts must print clear diagnostics for missing Java, Maven, jpackage, WiX, icon files, runtime images, jars, or expected output paths. They must not fail as a silent one-frame console flicker.

## Stage 4 — CI artifact bundle

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
6. Scan shipped jars/class directories for Java 17 classfile compatibility.
7. Build platform packages on their native operating systems.
8. Verify checksums and zip/archive integrity.
9. State honestly what was not manually tested.

## Crash log de-obfuscation workflow

Release builds should keep encrypted mapping artifacts in a developer-only location. When an obfuscated crash trace is submitted, the developer should select the matching build token, the encrypted mapping payload, and the external mapping key through the developer/admin de-obfuscation tool. Missing mappings should be reported inline rather than crashing the tool or destroying the original report structure.

## Rendering and optional adapters

The desktop client remains responsible for honest runtime reporting. Declaring an optional native rendering, networking, controller, platform, or workshop dependency is not the same as initializing that adapter. Startup and packaging diagnostics must report whether the adapter is actually present and active.

Headless server profiles must not force desktop rendering pipeline flags. Client graphics profiles may request platform Java2D acceleration before Swing startup where supported, while preserving safe fallback behavior.

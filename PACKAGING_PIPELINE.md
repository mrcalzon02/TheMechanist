# The Mechanist Native Packaging Pipeline

This guide describes the Java 17 packaging pipeline for producing native platform installers for The Mechanist. It is intentionally split into four stages so dependency isolation, runtime trimming, installer production, and CI artifact assembly can be reviewed independently.

## Stage 1 — Netty and dependency isolation

The build is driven by `pom.xml` and keeps the legacy project layout by compiling Java source directly from `src/`. The Maven configuration declares `io.netty:netty-all`, the optional Jamepad controller bridge, and the default LWJGL desktop rendering dependency set (`lwjgl`, `lwjgl-glfw`, `lwjgl-opengl`, plus Linux/Windows native runtime classifiers). It then uses `maven-shade-plugin` to emit two executable fat jars:

- `target/TheMechanist-all.jar` with `mechanist.TheMechanist` as its main class.
- `target/TheMechanistServer-all.jar` with `mechanist.MechanistServerMain` as its main class.

Build command:

```bash
mvn -B -DskipTests package
```

The LWJGL dependencies are bundled by default so the experimental first-person renderer can move to a native OpenGL backend later without forcing the user to acquire libraries after enabling the QoL toggle. The current `doom` mode viewport still uses the Java2D software backend unless a later runtime adapter selects LWJGL explicitly.

The fat jars are dependency-isolated classpath artifacts. They are deliberately separate from the custom Java runtime image: the runtime image contains only JDK modules, while the fat jar contains application bytecode, resources, assets, and library bytecode.

### Stage 1B — ProGuard obfuscation and secure mapping retention

After the shaded jars are produced, the Maven package lifecycle now runs `proguard-maven-plugin` over both fat jars:

- `target/TheMechanist-all.jar` → `target/TheMechanist-obfuscated.jar`
- `target/TheMechanistServer-all.jar` → `target/TheMechanistServer-obfuscated.jar`

The active rule files are:

- `config/proguard/proguard-client.conf`
- `config/proguard/proguard-server.conf`
- `config/proguard/obfuscation-dictionary.txt`
- `config/proguard/class-dictionary.txt`
- `config/proguard/package-dictionary.txt`

The rules preserve the public `mechanist.modapi.**` namespace, mod payload records, launch entrypoints, Java 17 `Record` and `PermittedSubclasses` attributes, and stack trace line-number/source metadata renamed to `MechanistObfuscated`. Internal server, security, packet, transaction, anti-macro, and networking classes are not kept by name unless another runtime rule requires it, so they are aggressively renamed through short dictionary identifiers.

ProGuard writes raw mappings to `target/proguard/*/mapping.raw.txt`. Raw mappings must not be shipped. The packaging scripts immediately run:

```bash
./scripts/security/encrypt-obfuscation-mappings.sh
```

or on Windows:

```powershell
.\scripts\security\encrypt-obfuscation-mappings.ps1
```

The encryption helper compresses the raw mapping with GZIP, encrypts it with AES-256-GCM, writes encrypted local developer artifacts under `dist/secure-maps/`, and deletes the raw mapping file. Provide `MECHANIST_MAPPING_KEY_BASE64` with a 32-byte Base64 key in CI for stable developer-controlled de-obfuscation. If that variable is absent, the helper generates `build/secure-local/mapping.key` for local testing; do not publish that key.

### Stage 1C — Sensitive internal string table

Some high-signal security strings are deliberately kept out of raw bytecode literals. The string manifest is `config/obfuscation/sensitive-strings.properties`, and the generator is:

```bash
./scripts/security/generate-sensitive-strings.sh
```

or on Windows:

```powershell
.\scripts\security\generate-sensitive-strings.ps1
```

The generator compiles `tools/obfuscation/SensitiveStringTableGenerator.java` with the local Java 17 JDK and emits `src/mechanist/ObfuscatedStringTable.java`, where selected strings are AES-GCM encrypted byte arrays decoded only at call time. This is an obfuscation barrier, not a cryptographic trust boundary; the key necessarily ships with the client/server bytecode. Use it to remove easy string-search anchors such as security disconnect reasons, not to store actual secrets.

## Stage 2 — jlink runtime trimming

The package scripts create a custom Java 17 runtime image with `jlink`. The current client module set is stored in `packaging/jlink/client-modules.txt`:

```text
java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.security.sasl,java.xml,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.unsupported
```

The current server module set is stored in `packaging/jlink/server-modules.txt`:

```text
java.base,java.logging,java.management,java.naming,java.net.http,java.security.sasl,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.unsupported
```

The desktop runtime requires `java.desktop` for Swing/AWT. Both runtime profiles retain TLS and crypto providers for HTTPS, secure handshakes, and signed packet/authentication systems. `jdk.unsupported` is retained because Netty commonly probes low-level JDK access paths for performance and compatibility.

The scripts use the following jlink flags:

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

Runtime image size depends on the JDK vendor, platform, compression, included modules, fonts, and native libraries. A small headless runtime can approach the tens-of-megabytes range, but the desktop Swing runtime is normally larger because it must carry desktop, font, image, and AWT/Swing native support.

## Stage 3 — jpackage native installers

`jpackage` cannot produce every native package from one operating system. Linux installers must be built on Linux; Windows MSI installers must be built on Windows.

Linux script:

```bash
./scripts/package/build-linux-installers.sh
```

The Linux script:

- Builds the fat jars with Maven.
- Creates a trimmed Java 17 runtime image.
- Produces a `.deb` installer with `jpackage`.
- Attempts `.rpm` generation when `rpmbuild` is available.
- Installs under `/opt/the-mechanist`.
- Requests a Linux desktop shortcut and Game menu integration.
- Writes `SHA256SUMS.txt` beside the produced installers.

Windows script:

```powershell
.\scripts\package\build-windows-msi.ps1
```

The Windows script:

- Builds the fat jars with Maven.
- Creates a trimmed Java 17 runtime image.
- Produces an `.msi` installer using `jpackage` and WiX Toolset 3.x.
- Enables Start Menu integration, desktop shortcut creation, directory selection, and normal MSI uninstaller registration.
- Writes `SHA256SUMS.txt` beside the produced installer.

## Stage 4 — GitHub Actions multi-runner bundle

The workflow lives at `.github/workflows/package.yml`. It runs Linux and Windows packaging jobs concurrently through a matrix strategy, uploads each platform installer as a separate artifact, then downloads both artifacts in a final Linux job and compresses them into:

```text
TheMechanist_native_installers.zip
```

This produces one player-facing download containing the platform-specific installers.

## User data directory separation

Installer targets are read-only or administrator-controlled locations. Mutable player data is routed through `GameStorageManager`, not the application install path.

Default roots:

- Windows: `%USERPROFILE%\Documents\TheMechanist\`
- Linux: `$XDG_DATA_HOME/TheMechanist/` when `XDG_DATA_HOME` is set, otherwise `$HOME/.local/share/TheMechanist/`
- macOS/other fallback paths are implemented for safety even though this packaging pass targets Linux and Windows.

Strict subdirectories:

```text
TheMechanist/saves/
TheMechanist/saves/data/profiles/
TheMechanist/export/
TheMechanist/mods/
TheMechanist/modsarchived/
```

The storage manager rejects absolute child paths, `..` traversal, and file names containing directory separators before moving mods or writing exported archives.

## Release checklist

1. Ensure Java 17 JDK is installed and `JAVA_HOME` points to it.
2. Run `mvn -B -DskipTests package`.
3. Run the local smoke tests for the touched storage/build path.
4. On Linux, run `./scripts/package/build-linux-installers.sh`.
5. On Windows with WiX Toolset installed, run `./scripts/package/build-windows-msi.ps1`.
6. Verify checksums in `dist/installers/**/SHA256SUMS.txt`.
7. Trigger `.github/workflows/package.yml` for the unified installer artifact.


## Crash log de-obfuscation workflow

Release builds should keep encrypted mapping artifacts in a developer-only location such as `dist/secure-maps/` or the restricted CI artifact named `secure-obfuscation-maps`. When a player submits an obfuscated crash trace, open the Simulation Editor Suite, launch the Crash De-Obfuscator, select the matching build token, choose the encrypted mapping payload, and provide the external mapping key file. The tool reconstructs class/method names and line numbers when ProGuard emitted the required line metadata. Missing tokens are preserved with an inline `MappingMissingAnomalie` marker so the original report structure is not destroyed.


## 0.9.10ie visual effects packaging note

The shared HUD, Java2D raycaster juice systems, primitive particles, and lightmap overlays remain Java standard-library/Swing code paths. LWJGL is still declared for future hardware rendering, but the current visual-juice pass does not add a new native dependency beyond the already declared rendering library set.


## Java2D acceleration defaults

Client JVM profiles now emit OS-specific Java2D acceleration properties before Swing startup: Direct3D and VRAM surfaces on Windows, OpenGL on Linux, and Metal plus OpenGL fallback on macOS. Headless server profiles intentionally do not set desktop rendering pipeline flags.


## Windows-first testing path

The primary Windows packaging script is now:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1
```

For quick local testing without rerunning Maven, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1 -UseExistingJar
```

A double-clickable wrapper is also available at `scripts/package/build-windows-installers.cmd`. The script emits three Windows-friendly outputs under `dist/installers/windows/`:

1. `TheMechanist_windows_portable_<version>.zip` — portable app-image containing `The Mechanist.exe`; use this first when testing whether the packaged runtime launches at all.
2. `.exe` installer — preferred human-facing installer with a visible wizard, install-directory chooser, shortcut prompt, Start Menu entry, and desktop shortcut request.
3. `.msi` installer — Windows Installer package for enterprise-style install/uninstall flows.

The EXE/MSI outputs require WiX Toolset 3.x on PATH. If WiX is missing, the script keeps the portable app-image available and prints a clear warning instead of appearing to do nothing. Both Windows installer types pass `--win-dir-chooser`, `--win-shortcut`, `--win-shortcut-prompt`, `--win-menu`, `--win-menu-group`, and `--icon` to `jpackage`.


## 0.9.10in launcher note

Windows direct launch scripts must detect Java through process stdout/stderr capture, not through raw `2>&1` native-command redirection under `$ErrorActionPreference = Stop`. Java version output is commonly written to stderr; treating that as an exception can cause valid Java 17+ installs to be rejected as unreadable.

# The Mechanist — Installer Packaging Pipeline

This guide belongs to the installer workspace. It describes how platform packages should be assembled around the current delivery model:

```text
installer → thin launcher → client → server
```

The installer should install the smallest durable launcher/orchestrator layer and its own runtime support. The launcher then owns package manifest verification, acquisition, update, rollback, and launch of the client package, server package, and required support libraries. The player should not need to download the full development repository to run the launcher.

## Source build and runtime artifacts

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

## Launcher-managed package layout

Packaging scripts stage a launcher-owned runtime layout rather than relying on the repository root:

```text
manifests/
packages/client/TheMechanist.jar
packages/server/TheMechanistServer.jar
packages/support/lib/*.jar
```

The platform runtime manifest records package identity, version, platform, paths, sizes, hashes, and main classes. The launcher must treat that manifest as the package identity source of truth.

Required support libraries such as LWJGL, platform native LWJGL jars, controller bridges such as Jamepad or its successor, Netty when used by a launched package, and future support libraries must be staged into `packages/support/lib/` by the packaging/update/acquisition stage. Game launchers and the client runtime must not download support libraries opportunistically during game startup.

## Native package scripts

Linux:

```bash
./scripts/package/build-linux-installers.sh
```

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1
```

A quick Windows local-testing path may use existing client/server jars when the script supports it:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1 -UseExistingJar
```

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

## Current segmentation note

This guide has been moved under `installer/` as part of the workspace segmentation pass. The root-level `PACKAGING_PIPELINE.md` remains temporarily as a compatibility bridge until script/resource references and external notes are updated, then it should be removed or replaced with a pointer.

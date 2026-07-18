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
launcher/MechanistLauncher.jar
packages/client/TheMechanist.jar
packages/server/TheMechanistServer.jar
packages/support/lib/*.jar
runtime/
```

The platform runtime manifest records package identity, version, platform, paths, sizes, hashes, and main classes. The launcher must treat that manifest as the package identity source of truth.

The same layout may be used as a local package seed for acquisition testing. The launcher reads `MECHANIST_LAUNCHER_PACKAGE_SEED_ROOT` or `mechanist.launcher.packageSeedRoot`, verifies the seed manifest and listed artifacts, copies them into the install root, and stores rollback backups under the launcher cache before replacement.

Manifest compatibility is part of package identity. Current launcher verification accepts schema `2`, distribution model `installer-thin-launcher-client-server`, and the platform matching the running launcher, such as `windows-x64` or `linux-x64`.

Required support libraries such as LWJGL, platform native LWJGL jars, controller bridges such as Jamepad or its successor, Netty when used by a launched package, and future support libraries must be staged into `packages/support/lib/` by the packaging/update/acquisition stage. Game launchers and the client runtime must not download support libraries opportunistically during game startup.

## Remote Java 17 verification and GitHub Releases

The remote development and release path is owned by:

```text
.github/workflows/java17-verify-and-release.yml
ROOT_build/ci/build_runnable_distribution.py
ROOT_build/ci/verify_runnable_distribution.py
```

Every push to `main` and every pull request targeting `main` runs the cross-platform Java 17 verification matrix on Linux and Windows. The workflow:

1. Checks out the exact commit.
2. Installs Temurin Java 17 and records the active toolchain.
3. Rebuilds client and server shaded jars from source.
4. Executes `Gate3PlayerFacingTextSmokeSuite`.
5. Executes the focused packaged startup smoke.
6. Starts the packaged headless server with `--help` under an isolated user-storage root.
7. Builds the launcher.
8. Stages the client, server, launcher, platform-matching runtime libraries, documentation, launch scripts, and a bundled Java 17 `jlink` runtime.
9. Generates a schema-2 manifest covering every staged file with size and SHA-256 identity.
10. Verifies all declared files, rejects undeclared files, validates JAR entry points, rejects classfiles newer than Java 17 major version 61, checks platform-native support libraries, and checks ZIP integrity.
11. Runs Gate 3 and the server smoke again from the staged distribution using the bundled runtime.
12. Uploads the runnable ZIP and machine-readable verification report as workflow artifacts.

Ordinary `main` and pull-request runs build non-obfuscated development distributions for exact-head verification. Release publication uses the `release-obfuscation` Maven profile and must pass the same matrix before publication.

A GitHub Release is created only by one of these explicit conditions:

- Push a `v*` tag, such as `v0.9.10iz`.
- Manually dispatch **Java 17 Verify and Release**, enable `publish_release`, and provide a valid `v`-prefixed release tag.

The release job does not run until both Linux and Windows verification jobs succeed. It attaches both portable runnable ZIPs, both verification reports, and `SHA256SUMS.txt`. Existing releases with the same tag receive replacement assets through an explicit clobbering upload; ordinary `main` pushes never publish a release.

Workflow publication permission is limited to the release job. Verification jobs have read-only repository contents permission.

## Local package seed builder

When Maven or private dependency authentication is unavailable, use the local Gate 4 seed builder to verify the manifest package chain from source:

```powershell
powershell -ExecutionPolicy Bypass -File .\ROOT_tools\packaging\stage_local_package_seed.ps1 -Version gate4-local
```

The builder compiles client/server and launcher sources with `javac --release 17`, creates executable client, server, and launcher jars, stages them under `build/local-package-seed/`, writes the platform runtime manifest, and scans the staged jars for Java 17 classfile compatibility. Add `-IncludeAssets` when the local check needs a copied `packages/client/assets` tree.

## Gate 2 open authentication dependency

Gate 2 remains unfinished until publish-safe artifact authentication is defined and verified. The central project repository and any private Maven/GitHub Packages artifacts require explicit credentials outside source control, such as a local Maven `settings.xml` server entry or environment-backed token wiring. Do not hard-code credentials in this tree.

Until that is in place, local package-seed verification and rollback smoke tests are valid, but remote acquisition/update readiness and full native package release readiness must remain marked open.

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
6. Stage client, server, launcher, support libraries, manifests, and the platform Java runtime into the launcher-managed package layout.
7. Verify manifest hashes and complete file coverage, including platform LWJGL native jars when required.
8. Scan shipped jars/class directories for Java 17 classfile compatibility.
9. Run Gate 3 and client/server startup checks from the staged distribution.
10. Build native installers on their native operating systems when installer publication is required.
11. Verify checksums and zip/archive integrity.
12. State honestly what was not manually tested.

## Current segmentation note

This guide has been moved under `installer/` as part of the workspace segmentation pass. The root-level `PACKAGING_PIPELINE.md` remains temporarily as a compatibility bridge until script/resource references and external notes are updated, then it should be removed or replaced with a pointer.

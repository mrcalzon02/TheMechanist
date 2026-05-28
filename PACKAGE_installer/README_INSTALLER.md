# The Mechanist Installer Layer

This folder contains the installer and native-package wrapper surface for the thin launcher.

## Purpose

The installer should place the smallest durable launcher/orchestrator layer needed to start The Mechanist. It should not ask players to download the full development repository, and it should not treat the repository root as the runtime layout.

The active delivery shape is:

```text
installer -> thin launcher -> client package -> server package
```

## Current Package Model

The installer and native package scripts are moving toward this launcher-managed layout:

```text
manifests/
packages/launcher/MechanistLauncher.jar
packages/client/TheMechanist.jar
packages/server/TheMechanistServer.jar
packages/support/lib/
```

The launcher owns manifest verification, local package-seed install/update, rollback repair, and launch handoff. Legacy Git-backed installer scripts now stop with an explanatory message instead of cloning or updating the full development repository.

## Gate 2 Status

Gate 2 is not complete yet.

Local package identity and manifest verification are in place, but publish-safe remote package acquisition still needs an authenticated artifact policy for the private central repository or a public-safe artifact channel. Private Maven or package-host access must be supplied through local credentials outside source control.

## Native Package Builds

Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1
```

Linux:

```bash
./scripts/package/build-linux-installers.sh
```

Full native verification still requires the appropriate local toolchain: Java 17, Maven dependency access, jpackage, Bash for Linux script checks, and platform installer tools where applicable.

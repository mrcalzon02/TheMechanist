# The Mechanist Launcher Status

The old Git-backed launcher scripts in this folder are retired as active bootstrap paths.

Gate 2 requires:

- installer -> thin launcher -> client/server payloads,
- manifest-described package identity,
- client, server, and support-library verification before launch,
- no full development repository clone just to run the launcher.

The active launcher entrypoint is the Java launcher app packaged as:

```text
packages/launcher/MechanistLauncher.jar
```

Native package scripts stage that launcher jar beside:

```text
manifests/*-runtime-manifest.json
packages/client/TheMechanist.jar
packages/server/TheMechanistServer.jar
packages/support/lib/
packages/launcher/profile-packages/
```

Build platform launcher packages through:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package\build-windows-installers.ps1
```

or:

```bash
./scripts/package/build-linux-installers.sh
```

The legacy `launcher/windows/MechanistLauncher.ps1` and `launcher/linux/mechanist-launcher.sh` now stop with an explanatory message instead of cloning or updating the full repository.

## Local Package Seed Acquisition

The launcher can acquire a verified local package seed without using the network. A seed uses the same layout as the installed package root:

```text
manifests/*-runtime-manifest.json
packages/client/TheMechanist.jar
packages/server/TheMechanistServer.jar
packages/support/lib/
```

Set one of these before running the launcher to point at the seed:

```text
MECHANIST_LAUNCHER_PACKAGE_SEED_ROOT
mechanist.launcher.packageSeedRoot
```

Staged runs and smoke checks may also override launcher-managed state roots:

```text
MECHANIST_LAUNCHER_USER_DATA_ROOT
MECHANIST_LAUNCHER_ROAMING_CONFIG_ROOT
MECHANIST_LAUNCHER_LOCAL_STATE_ROOT
mechanist.launcher.userDataRoot
mechanist.launcher.roamingConfigRoot
mechanist.launcher.localStateRoot
```

The seed manifest is verified before copy. Existing installed package files are backed up under the launcher cache before replacement, and the repair path can restore the latest rollback if verification later fails.

Runtime manifests must use schema `2`, distribution model `installer-thin-launcher-client-server`, and the current platform identifier such as `windows-x64` or `linux-x64`. Wrong-schema, wrong-model, and wrong-platform seeds are rejected before package files are trusted.

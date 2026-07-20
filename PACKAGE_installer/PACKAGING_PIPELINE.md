# The Mechanist — Installer Packaging Pipeline

This guide belongs to the installer workspace. It defines the current package path:

```text
installer → thin launcher → client → server
```

The full development repository is never a player runtime. Limited-alpha delivery begins with a verified canonical platform distribution, then native installers consume that distribution without rebuilding a competing client, server, dependency, runtime, or manifest tree.

## Canonical release authority

The canonical build and verification tools are:

```text
ROOT_build/ci/build_runnable_distribution.py
ROOT_build/ci/verify_runnable_distribution.py
ROOT_build/ci/run_synthetic_distribution_tests.py
```

The canonical distribution contains:

```text
manifests/runtime-manifest.json
manifests/launcher-runtime-manifest.json
launcher/MechanistLauncher.jar
packages/client/TheMechanist.jar
packages/server/TheMechanistServer.jar
packages/support/lib/*.jar
runtime/
docs/
platform launch scripts
```

`runtime-manifest.json` is the complete integrity ledger. It records the exact source commit, project version, platform, Java release, release-hardening state, every staged path, file size, role, and SHA-256 identity.

`launcher-runtime-manifest.json` is the compatibility view consumed by the current thin launcher. It contains the verified client, server, and support-library package identities. The compatibility manifest is itself covered by the canonical integrity ledger.

The launcher may verify, install, repair, roll back, and launch a package set. It must not clone the development repository or opportunistically download libraries while the game starts.

## Development and distributable artifacts

The ordinary Maven development path produces:

```text
target/TheMechanist-all.jar
target/TheMechanistServer-all.jar
```

The distributable limited-alpha path requires the explicit `release-obfuscation` profile and produces:

```text
target/TheMechanist-obfuscated.jar
target/TheMechanistServer-obfuscated.jar
```

A native installer or published prerelease must fail unless the canonical distribution reports:

```text
javaRelease = 17
releaseHardened = true
```

Ordinary exact-head CI distributions may remain non-obfuscated development artifacts. They are verification outputs, not playtest releases.

## Canonical portable build

Linux or macOS shell:

```bash
python3 ROOT_build/ci/build_runnable_distribution.py \
  --repo . \
  --release-hardened \
  --output dist/releases
```

Windows PowerShell:

```powershell
python .\ROOT_build\ci\build_runnable_distribution.py `
  --repo . `
  --release-hardened `
  --output dist\releases
```

Verify the resulting platform directory and archive:

```bash
python3 ROOT_build/ci/verify_runnable_distribution.py \
  dist/releases/TheMechanist-<version>-<platform> \
  --archive dist/releases/TheMechanist-<version>-<platform>.zip \
  --require-release-hardened
```

The verifier checks complete manifest coverage, hashes, JAR entry points, Java 17 classfile major version 61, required native support libraries, launcher-manifest agreement, bundled runtime presence, unsafe paths, undeclared files, and ZIP integrity.

## Native installer convergence

Native packaging no longer recompiles the game or constructs its own package manifest. These shared tools stage and verify the native application payload:

```text
ROOT_build/ci/stage_native_installer_payload.py
ROOT_build/ci/verify_native_installer_image.py
```

The staging tool:

1. Re-verifies the canonical distribution with release hardening required.
2. Copies the verified launcher, client, server, support libraries, and documentation.
3. Preserves the launcher-compatible runtime manifest.
4. Records the source distribution, version, commit, platform, hardening state, canonical manifest hash, and artifact count.
5. Supplies the canonical `runtime/` image to `jpackage` separately instead of duplicating it inside the application payload.

The native-image verifier reopens the produced `jpackage` app image and checks the launcher, client, server, support-library hashes, Java 17 classfiles, bundled runtime, source-certification record, hardening identity, and absence of mutable user-storage directories inside the installed payload.

### Linux

Build from a new canonical release-hardened distribution:

```bash
./scripts/package/build-linux-installers.sh
```

Build from an already verified canonical distribution:

```bash
./scripts/package/build-linux-installers.sh \
  --distribution dist/releases/TheMechanist-<version>-linux-x64
```

Limit the requested package types when needed:

```bash
./scripts/package/build-linux-installers.sh \
  --distribution dist/releases/TheMechanist-<version>-linux-x64 \
  --package-types app-image,deb
```

The Linux path always produces and verifies a portable app image. DEB is produced when requested. RPM is produced only when `rpmbuild` is available.

### Windows

Build from a new canonical release-hardened distribution:

```powershell
powershell -ExecutionPolicy Bypass -File `
  .\scripts\package\build-windows-installers.ps1
```

Build from an already verified canonical distribution:

```powershell
powershell -ExecutionPolicy Bypass -File `
  .\scripts\package\build-windows-installers.ps1 `
  -DistributionRoot .\dist\releases\TheMechanist-<version>-windows-x64
```

Build only the directly runnable app image:

```powershell
powershell -ExecutionPolicy Bypass -File `
  .\scripts\package\build-windows-installers.ps1 `
  -DistributionRoot .\dist\releases\TheMechanist-<version>-windows-x64 `
  -PackageTypes app-image
```

The Windows path always produces and verifies a portable app image. EXE and MSI require WiX Toolset 3.x. Use `-RequireNativeInstallers` when missing WiX must be treated as a hard failure rather than allowing app-image-only output.

The project version may contain an alphabetic development suffix. Native package metadata uses the numeric version portion accepted by `jpackage`, while the full project version and exact commit remain preserved in filenames, manifests, verification reports, and installer README output.

## User-storage boundary

Installer-controlled application directories must remain immutable during ordinary operation. Saves, settings, profiles, logs, cache, active mods, archived mods, and exports belong under the governed user-storage roots, never inside Program Files, `/opt`, or the app-image payload.

The native-image verifier rejects installer payloads that contain top-level mutable user-storage directories.

## Remote verification and prerelease publication

`.github/workflows/java17-verify-and-release.yml` owns the cross-platform Java 17 package matrix, Gate 3, synthetic extracted-install checks, final certification, and guarded prerelease publication.

An ordinary push or pull request verifies development distributions. A limited-alpha prerelease is created only through an explicit manual workflow dispatch with `publish_prerelease=true` and a version-matching `v` tag value.

Before any playtest link is distributed, the exact candidate run must show successful Linux x64 and Windows x64 build, package verification, packaged Gate 3, synthetic environment, standalone host bind, final certification, checksums, and prerelease publication.

## Gate 2 authentication boundary

Remote authenticated package acquisition remains open. The current launcher can verify installed packages, install from a verified local package seed, and restore a rollback. It does not yet provide a production-ready authenticated remote update service.

For limited alpha, use either:

- the complete verified portable distribution; or
- a native installer composed from that complete verified distribution.

Do not present inactive `main`, `testing`, or `dev` channel hooks as operational remote update services.

## Release checklist

1. Freeze the exact candidate commit and project version.
2. Build client and server from source with Java 17.
3. Run the relevant focused smokes.
4. Build the canonical release-hardened platform distribution.
5. Verify complete manifest coverage, hashes, entry points, classfile major version 61, native support libraries, runtime, and archive integrity.
6. Run packaged Gate 3, synthetic clean-profile tests, read-only-install checks, tamper rejection, and standalone host binding.
7. Build native app images and installers only on their target operating systems.
8. Reopen and verify each native app image.
9. Verify SHA-256 checksum files.
10. Test install, launch, repair, rollback, upgrade, uninstall, and preservation of user data.
11. Test single-player internal-server lifecycle and independent-host client connection before claiming those capabilities.
12. Attach known limitations, tester instructions, diagnostics guidance, verification reports, and checksums.
13. State exactly what was not manually tested.

## Current readiness limitation

The canonical portable and native composition paths are now aligned in source, but this does not by itself certify an alpha. The exact Windows and Linux native scripts still require successful execution on their target operating systems, and the internal single-player server lifecycle plus packaged independent-host client session remain release blockers.

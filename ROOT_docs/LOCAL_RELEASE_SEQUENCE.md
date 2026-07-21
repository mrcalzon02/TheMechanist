# Codex Local Alpha Release Build Protocol

This document is the authoritative operator protocol for creating local limited-alpha packages when GitHub Actions cannot be used. It is written for a Codex development session operating on a full local checkout of `mrcalzon02/TheMechanist`.

It coordinates the repository's existing build, verification, inventory, native-image, and installer authorities. It does not replace them with a second build path.

A local build is target-platform evidence only. Windows installers must be built and tested on Windows. Linux installers must be built and tested on Linux. A successful run on one operating system does not certify the other operating system.

## 1. Governing authority

Before starting any local alpha build, Codex must read these files in order:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md`
3. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`
4. `ROOT_docs/DEVELOPMENT_HISTORY.md`
5. `ROOT_docs/MILESTONE_INDEX.md`
6. `ROOT_docs/MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md`
7. this file

The following rules are release law:

- Work only on the authoritative `main` branch.
- Build from a complete checkout, not a sparse export, copied source folder, old archive, or previously staged package tree.
- Compile from source with Java 17 compatibility.
- Rebuild launcher, client, server, runtime image, support libraries, and native staging from the same exact commit.
- Release packaging must consume the release-hardened/obfuscated client artifact.
- Do not patch JARs, native images, installers, reports, manifests, or checksum ledgers by hand.
- Do not reuse evidence from a different commit, version, operating system, architecture, or hardening mode.
- Do not change `HEAD` during a candidate run.
- Do not call an app-image an installer-lifecycle-certified EXE, MSI, DEB, or RPM.
- Do not tag, publish, upload, create a GitHub Release, or update release history unless the user separately authorizes publication after the evidence has been reviewed.

The intended package path remains:

```text
installer -> thin launcher -> verified client -> verified server/support payloads
```

## 2. Codex operating contract

Codex must treat a local alpha build as a fail-closed operation.

During the build, Codex must:

- record the exact 40-character commit before running a gate;
- record the active branch and require `main`;
- require a clean source worktree;
- allow generated, untracked `dist/` output only where the existing gate explicitly permits it;
- stop at the first failing stage;
- read the first failing report and its referenced log before editing anything;
- repair the authoritative source, script, manifest, or configuration that owns the failure;
- commit the repair to `main`;
- restore a clean worktree;
- rerun the full sequence from the beginning;
- preserve the generated reports and logs until the candidate is accepted or abandoned;
- state exactly which automatic and manual checks were not performed.

Codex must never:

- skip a failed gate and continue packaging;
- edit a JSON report so it says `passed` or `verified`;
- copy a report from an older run into the current evidence directory;
- build a native package from a portable distribution whose commit does not equal current `HEAD`;
- use a raw shaded development JAR in release installer staging;
- silently ignore a missing JDK, Maven, jpackage, WiX, RPM, DEB, shell, or checksum tool;
- delete or place mutable saves, profiles, settings, logs, cache, mods, exports, or resume tokens inside the installer-controlled application directory;
- claim both-platform readiness after testing only one platform.

## 3. Supported local build hosts

The current local release gates support:

```text
windows-x64
linux-x64
```

The detected machine architecture must be `x86_64` or `amd64`.

Native packages are not cross-compiled:

- Build Windows app-image, EXE, and MSI outputs on Windows x64.
- Build Linux app-image, DEB, and RPM outputs on Linux x64.
- Build RPM on a host with `rpmbuild` available.
- Build Windows EXE/MSI with WiX Toolset 3.x available through `candle.exe` and `light.exe`.
- If the package backend required by `jpackage` is unavailable, stop and install the missing target-platform tool. Do not substitute a package built on another operating system.

## 4. Required local tools

Every host requires:

- Git
- Python 3
- Maven
- a full Java 17 JDK providing `java`, `javac`, `jlink`, and `jpackage`
- enough disk space for Maven output, the canonical portable distribution, runtime image, native staging tree, app-image, installers, archives, reports, and checksums

Windows additionally requires:

- PowerShell 7 available as `pwsh`
- WiX Toolset 3.x for EXE and MSI output

Linux additionally requires:

- Bash
- `tar`
- `sha256sum`
- the target package backend required by `jpackage`
- `rpmbuild` when RPM output is requested

Before building, verify that Java tools resolve from the intended Java 17 installation:

### Windows PowerShell

```powershell
Get-Command git, python, mvn, java, javac, jpackage, pwsh
java -version
javac -version
jpackage --version
$env:JAVA_HOME
```

For EXE/MSI output also verify:

```powershell
Get-Command candle.exe, light.exe
```

### Linux shell

```bash
command -v git python3 mvn java javac jpackage bash tar sha256sum
java -version
javac -version
jpackage --version
printf 'JAVA_HOME=%s\n' "${JAVA_HOME:-unset}"
```

For RPM output also verify:

```bash
command -v rpmbuild
```

`java`, `javac`, and `jpackage` must belong to the same intended Java 17 JDK. A newer JDK successfully launching the program does not prove Java 17 compatibility.

## 5. Candidate preflight

From the repository root, Codex must capture:

```bash
git branch --show-current
git rev-parse HEAD
git status --short --branch
```

Required state:

- branch is exactly `main`;
- `git rev-parse HEAD` returns one 40-character commit;
- no tracked, staged, or untracked source changes exist outside generated `dist/` output;
- the intended code and documentation changes have already been committed;
- no second development process is modifying the checkout;
- the game, launcher, local server, IDE build, Maven process, or prior packaging script is not still holding output files open.

Codex must record the candidate identity in its operator report:

```text
commit
branch
host operating system
host architecture
Java version
javac version
jpackage version
Maven version
Python version
requested package types
```

Do not begin with an uncommitted "almost final" worktree. The clean commit is part of the package identity.

## 6. Governed repository-manifest review boundary

Any committed file change can make `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` stale. The release sequence intentionally refuses to hide that fact.

First run the normal sequence:

```bash
python ROOT_build/ci/run_local_release_sequence.py
```

If the inventory stage reports that the governed manifest is stale, run the explicit update form:

```bash
python ROOT_build/ci/run_local_release_sequence.py --update-committed-manifest
```

This command intentionally stops after inventory with exit code `2` and top-level status `review-required`. It must not continue into native packaging under the old commit identity.

Review:

```text
dist/local-inventory-gate/manifest-diff.txt
dist/local-inventory-gate/generated.tsv
ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv
```

Codex must inspect the diff for:

- accidental build output outside ignored/generated roots;
- missing or unexpected source files;
- duplicate protected runtime material;
- newly added binary assets that lack intended ownership;
- files that should not be release candidates;
- unexpected deletions or renames;
- clearance-registry mismatches.

After review:

1. commit the governed manifest directly to `main`;
2. confirm the worktree is clean;
3. record the new commit identity;
4. rerun the sequence without `--update-committed-manifest`.

Never combine manifest replacement and final release evidence in one candidate run.

## 7. Authoritative local release sequence

Run this command from the repository root:

```bash
python ROOT_build/ci/run_local_release_sequence.py
```

The coordinator executes these existing authorities in order:

1. source limited-alpha operating-document verification;
2. release-hardened Java 17 build and portable-distribution gate;
3. reopened packaged limited-alpha operating-document verification;
4. governed repository inventory generation and audit;
5. target-platform native app-image construction and verification;
6. cross-stage release-evidence coherence verification.

The Java stage includes:

- Python release-tool syntax compilation;
- ProGuard policy verification;
- platform packaging-script syntax verification;
- Maven `clean package` from source;
- headless boot smoke;
- server `--help` operation smoke with isolated server home;
- release-hardened canonical portable-distribution construction;
- portable distribution and ZIP verification;
- packaged Gate 3 execution through the bundled runtime;
- synthetic extracted-distribution tests;
- synthetic release-contract verification;
- exact manifest/report/commit/platform/version/Java/hardening coherence checks.

The coordinator stops at the first failed stage and writes:

```text
dist/local-release-sequence-report.json
dist/local-release-sequence/logs/
```

The underlying stages retain detailed evidence at:

```text
dist/local-java-gate-report.json
dist/local-java-gate/
dist/local-inventory-gate-report.json
dist/local-inventory-gate/
dist/local-native-gate-report.json
dist/local-native-gate/
dist/local-release-evidence-report.json
```

A passing sequence must end with:

```text
dist/local-release-sequence-report.json -> status: passed
dist/local-java-gate-report.json -> status: passed
dist/local-inventory-gate-report.json -> status: passed
dist/local-native-gate-report.json -> status: passed
dist/local-release-evidence-report.json -> status: verified
```

The native gate proves the app-image composition and archive. It deliberately records:

```text
installerCertificationClaimed: false
```

That field must remain false until actual EXE/MSI/DEB/RPM installation and removal are tested on the target operating system.

## 8. Release-clearance mode

Release-clearance mode is stricter than an ordinary development-alpha package pass.

Use it only after:

- the governed manifest is committed and clean;
- `ROOT_docs/RELEASE_CLEARANCE.tsv` has been reviewed;
- every release-relevant entry requiring clearance has an exact accepted status;
- no stale registry entries or clearance blockers remain.

Run:

```bash
python ROOT_build/ci/run_local_release_sequence.py --require-clearance
```

Do not use `--require-clearance` merely to force a package through. A failure means the clearance registry or inventory ownership must be repaired and reviewed.

## 9. Locate the verified canonical distribution

After the local release sequence passes, use the unique target-platform distribution under:

```text
dist/local-java-gate/releases/
```

Expected forms:

```text
dist/local-java-gate/releases/TheMechanist-<version>-windows-x64/
dist/local-java-gate/releases/TheMechanist-<version>-linux-x64/
```

The directory must contain its canonical runtime manifest under:

```text
manifests/runtime-manifest.json
```

The manifest commit must equal current `git rev-parse HEAD`, and `releaseHardened` must be `true`.

Codex must not choose a distribution by modification time when multiple candidates exist. Remove or archive stale generated candidates, rerun the Java gate, and require exactly one matching target-platform directory.

## 10. Build full Windows alpha outputs

Run this section only on Windows x64 after the authoritative local sequence has passed.

Open PowerShell 7 in the repository root:

```powershell
$distribution = Get-ChildItem `
    -LiteralPath "dist\local-java-gate\releases" `
    -Directory `
    -Filter "TheMechanist-*-windows-x64"

if ($distribution.Count -ne 1) {
    throw "Expected exactly one verified windows-x64 canonical distribution."
}

& .\scripts\package\build-windows-installers.ps1 `
    -DistributionRoot $distribution[0].FullName `
    -PackageTypes @("app-image", "exe", "msi") `
    -OutputDir (Join-Path $PWD "dist\installers\windows") `
    -RequireNativeInstallers
```

The script:

- reopens and verifies the canonical distribution;
- requires `releaseHardened=true`;
- stages the governed installer payload;
- builds a directly runnable app-image;
- builds EXE and MSI installers when WiX 3.x is available;
- adds the governed main launcher and Remote Lobby launcher;
- applies Start Menu, desktop shortcut, shortcut prompt, directory chooser, upgrade UUID, icon, and per-user installation settings;
- reopens and verifies the app-image;
- produces a portable app-image ZIP;
- writes `SHA256SUMS.txt` and `WINDOWS_INSTALLERS_README.txt`.

Expected output root:

```text
dist/installers/windows/
```

Required alpha artifacts include:

- the native app-image directory;
- `TheMechanist-<version>-windows-x64-native-app-image.zip`;
- an EXE installer;
- an MSI installer;
- `source-verification-windows-x64.json`;
- `staging-windows-x64.json`;
- `native-image-verification-windows-x64.json`;
- `SHA256SUMS.txt`;
- `WINDOWS_INSTALLERS_README.txt`.

If `-RequireNativeInstallers` fails because WiX is missing, install WiX 3.x and rerun. Do not distribute only the app-image while describing it as a completed EXE/MSI release.

## 11. Build full Linux alpha outputs

Run this section only on Linux x64 after the authoritative local sequence has passed.

From the repository root:

```bash
mapfile -t distributions < <(
  find dist/local-java-gate/releases \
    -mindepth 1 -maxdepth 1 -type d \
    -name 'TheMechanist-*-linux-x64' \
    -print | sort
)

if [[ ${#distributions[@]} -ne 1 ]]; then
  printf 'Expected exactly one verified linux-x64 canonical distribution; found %s.\n' \
    "${#distributions[@]}" >&2
  exit 1
fi

bash scripts/package/build-linux-installers.sh \
  --distribution "${distributions[0]}" \
  --package-types app-image,deb,rpm \
  --output dist/installers/linux
```

The script:

- reopens and verifies the canonical distribution;
- requires `releaseHardened=true`;
- stages the governed installer payload;
- builds and verifies a directly runnable app-image;
- builds DEB output through the host's jpackage package backend;
- builds RPM output when `rpmbuild` is available;
- adds the governed main launcher and Remote Lobby launcher;
- writes a portable app-image `tar.gz` archive;
- writes `SHA256SUMS.txt` and `LINUX_INSTALLERS_README.txt`.

Expected output root:

```text
dist/installers/linux/
```

Required alpha artifacts include:

- the native app-image directory;
- `TheMechanist-<version>-linux-x64-native-app-image.tar.gz`;
- a DEB package when the DEB backend is available;
- an RPM package when `rpmbuild` is available and RPM was requested;
- `source-verification-linux-x64.json`;
- `staging-linux-x64.json`;
- `native-image-verification-linux-x64.json`;
- `SHA256SUMS.txt`;
- `LINUX_INSTALLERS_README.txt`.

The current Linux script reports when RPM is skipped because `rpmbuild` is unavailable. A skipped RPM must not be listed as produced or tested.

## 12. Automatic artifact verification

The platform packaging scripts automatically verify the source distribution, staged payload, native app-image, launcher identities, remote-lobby entry point, mutable-storage exclusion, and generated checksums.

Codex must verify the checksum ledger before moving or uploading artifacts.

### Windows PowerShell

```powershell
$root = Resolve-Path "dist\installers\windows"
Get-Content (Join-Path $root "SHA256SUMS.txt") | ForEach-Object {
    if ($_ -notmatch '^([0-9a-f]{64})  (.+)$') {
        throw "Malformed SHA256SUMS entry: $_"
    }
    $expected = $Matches[1]
    $path = Join-Path $root $Matches[2]
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing checksum target: $path"
    }
    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
        throw "SHA256 mismatch: $path"
    }
}
```

### Linux shell

```bash
(
  cd dist/installers/linux
  sha256sum --check SHA256SUMS.txt
)
```

Do not rename, recompress, modify, sign, or move individual files and continue using the old checksum ledger. Any artifact mutation requires a new checksum ledger and another verification pass.

## 13. Mandatory manual app-image test

Test the app-image before testing an installer wizard or package manager.

The app-image test must use the packaged native executable, not Maven, an IDE, or `java -jar` from the repository.

Verify:

1. The main launcher starts.
2. The launcher displays usable diagnostics when a package check fails.
3. Launcher package verification succeeds for the included candidate.
4. The client starts through the launcher.
5. A new single-player run can be created.
6. A save can be written.
7. The application can be closed and the save resumed.
8. Mutable data is written under the user-storage authority, not under the app-image directory.
9. The Remote Lobby native launcher starts independently.
10. The Remote Lobby remains a lobby/client entry point and does not silently become a local authoritative host.
11. The packaged headless/internal server help or startup path remains available.
12. Uninstalling or deleting the app-image does not delete user saves or profiles.

Run audible GUI launch tests only as often as needed. One complete app-image lifecycle pass is preferable to repeated uncontrolled launcher starts.

## 14. Mandatory Windows installer-lifecycle test

Test EXE and MSI separately on Windows. A clean Windows VM or clean disposable test account is preferred.

For each installer type:

1. Verify its SHA-256 against `SHA256SUMS.txt`.
2. Install using the normal user-facing flow.
3. Confirm the directory chooser appears.
4. Confirm the intended per-user installation behavior.
5. Confirm Start Menu integration.
6. Confirm the desktop-shortcut choice/prompt.
7. Confirm the installed application icon.
8. Launch The Mechanist from the installed shortcut.
9. Launch The Mechanist Remote Lobby from its installed entry point.
10. Complete the app-image functional checks against the installed copy.
11. Confirm saves, profiles, settings, logs, cache, mods, and exports are outside the installation directory.
12. Close all application processes.
13. Uninstall through the normal Windows uninstall path.
14. Confirm installer-owned files are removed.
15. Confirm user saves and profiles remain intact unless the user explicitly removed them.
16. Reinstall and verify the prior save can still be resumed.
17. Record installer type, file hash, Windows version, account/install mode, install location, result, and any warning.

A passing app-image does not substitute for these EXE/MSI lifecycle checks.

## 15. Mandatory Linux package-lifecycle test

Test DEB and RPM on their native package families. Use clean VMs or disposable test systems where practical.

For each produced package:

1. Verify its SHA-256 against `SHA256SUMS.txt`.
2. Install through the platform package manager.
3. Confirm the intended `/opt/the-mechanist` application location or package-reported equivalent.
4. Confirm desktop/menu integration.
5. Confirm the installed icon.
6. Launch The Mechanist from the installed entry.
7. Launch The Mechanist Remote Lobby from its installed entry.
8. Complete the app-image functional checks against the installed copy.
9. Confirm mutable user data is outside `/opt/the-mechanist`.
10. Close all application processes.
11. Remove the package through the platform package manager.
12. Confirm package-owned files are removed.
13. Confirm user saves and profiles remain intact.
14. Reinstall and verify the prior save can still be resumed.
15. Record package type, file hash, distribution/version, desktop environment, install result, removal result, and any warning.

Do not claim RPM verification from a DEB-family host or DEB verification from an RPM-family host merely because the file was generated.

## 16. Multiplayer and host alpha checks

Current release evidence does not certify fully authoritative remote gameplay. Preserve that limitation in release notes and operator reports.

Where the active candidate is intended for limited multiplayer transport testing, record separate results for:

- supervised single-player/internal hosting;
- independent host process startup;
- configured host bind address and port;
- one client connection;
- two concurrent client connections;
- client disconnect and reconnect;
- host restart and reconnect continuity;
- Remote Lobby entry-point behavior;
- rejection of unauthenticated or replayed control requests where the existing synthetic gate covers them.

Do not broaden a transport/session proof into a claim that remote inventory, movement, world mutation, combat, or all gameplay commands are authoritative unless the exact current runtime evidence proves those capabilities.

## 17. Artifact custody and upload set

Do not distribute the development repository, Maven `target/`, raw ProGuard mapping, de-obfuscation key material, temporary native staging directories, test profiles, server homes, or unreviewed logs as player packages.

A target-platform alpha upload set should contain only the intended outputs from the platform installer directory and their supporting verification material, normally:

```text
native app-image archive
native installer package(s)
SHA256SUMS.txt
platform installer README
source-verification report
staging report
native-image verification report
approved limited-alpha operating documents/release notes required by the candidate
```

Keep the following evidence with the internal release record even when it is not all uploaded to players:

```text
local-release-sequence-report.json
local-java-gate-report.json
local-inventory-gate-report.json
local-native-gate-report.json
local-release-evidence-report.json
stage logs
manual app-image test record
manual installer-lifecycle test record
exact commit and version identity
```

Never ship raw ProGuard mappings or private mapping keys.

## 18. Candidate status vocabulary

Use these terms precisely:

### Local development package

A package was generated, but one or more authoritative gates or manual checks have not passed.

### Locally verified app-image candidate

The full local release sequence passed on one target operating system and the native app-image passed its manual functional test. This does not certify EXE/MSI/DEB/RPM lifecycle behavior.

### Locally verified installer candidate

The full local release sequence passed on the target operating system, the app-image passed, and each claimed installer type passed installation, launch, storage, uninstall, and reinstall testing on that operating-system family.

### Release-clearance candidate

The locally verified installer candidate also passed `--require-clearance` with exact inventory/clearance evidence.

### Limited-alpha release candidate

Required Windows and Linux candidates, claimed installer types, clean-machine checks, operating documents, checksums, identity evidence, and manual lifecycle results have been reviewed and accepted for the intended alpha scope.

Do not use `release-ready`, `certified`, `published`, or `available` before the corresponding evidence and explicit publication action exist.

## 19. Failure and repair protocol

When a stage fails:

1. Open `dist/local-release-sequence-report.json`.
2. Read `failedStage` and `failedStageResult`.
3. Open the referenced stage log.
4. Open the underlying gate report for that stage.
5. Identify the first concrete failure, not later cascade noise.
6. Locate the authoritative source owner.
7. Repair that owner directly.
8. Run the narrowest safe syntax or focused test required to validate the repair.
9. Commit the repair to `main`.
10. Confirm a clean worktree and record the new commit.
11. Rerun the entire local release sequence.
12. Discard old candidate status; reports from the prior commit are historical diagnostics only.

Common stop conditions include:

- wrong branch;
- dirty source worktree;
- stale governed repository manifest;
- pending or rejected clearance;
- missing Java 17/Maven/Python/native package tool;
- classfile major version above 61;
- launcher/client/server version disagreement;
- missing or mismatched runtime manifest identity;
- raw rather than release-hardened client payload;
- Gate 3 failure;
- synthetic contract failure;
- packaged operating-document mismatch;
- native staging mismatch;
- mutable storage found inside the package;
- missing main or Remote Lobby native launcher;
- checksum failure;
- installer backend failure;
- install/uninstall lifecycle failure.

Do not repair evidence by weakening a verifier unless the verifier demonstrably contradicts the governing package contract. If a verifier is wrong, repair the canonical verifier and add focused proof for the corrected rule.

## 20. Codex execution prompt

The following prompt may be supplied to a local Codex session after the repository has been cloned and the required target-platform tools are installed:

```text
Operate directly on mrcalzon02/TheMechanist, branch main only.

Read, in order:
1. ROOT_docs/MASTER_DEVELOPMENT_PLAN.md
2. ROOT_docs/STANDARDS_AND_PRACTICES.md
3. ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md
4. ROOT_docs/DEVELOPMENT_HISTORY.md
5. ROOT_docs/MILESTONE_INDEX.md
6. ROOT_docs/MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md
7. ROOT_docs/LOCAL_RELEASE_SEQUENCE.md

Create a target-platform limited-alpha release candidate locally without using GitHub Actions.

Before executing work, report the active branch, exact HEAD commit, worktree state, host OS/architecture, Java, javac, jpackage, Maven, and Python versions. Require branch main and a clean source worktree.

Run the authoritative command:
python ROOT_build/ci/run_local_release_sequence.py

Stop at the first failure. Read the top-level report, the failed stage report, and its log. Repair the authoritative implementation directly; do not add wrappers, bypass gates, edit evidence, reuse stale build output, or continue packaging after failure. Commit any repair to main, restore a clean worktree, and rerun the complete sequence.

If the repository manifest is stale, run:
python ROOT_build/ci/run_local_release_sequence.py --update-committed-manifest
Review the generated manifest and diff, commit the governed manifest to main, then rerun the normal sequence from the new clean commit.

After the sequence passes, build the full installer types for the current operating system using the exact commands in ROOT_docs/LOCAL_RELEASE_SEQUENCE.md. Verify SHA256SUMS.txt. Test the native app-image before testing installers. Perform the target-platform install, launch, mutable-storage, uninstall, and reinstall protocol for every installer type claimed.

Do not tag, publish, upload, create a GitHub Release, update development history, or claim cross-platform readiness. Return the exact commit, version, platform, produced artifact paths, SHA-256 values, automatic report statuses, manual test results, and every untested or blocked requirement.
```

## 21. Required Codex completion report

At the end of a local build attempt, Codex must report:

```text
branch
commit
canonical version
platform
release-hardened status
worktree-clean status
local release sequence status
Java gate status
inventory gate status
native gate status
final evidence status
clearance required/passed status
portable distribution path
portable distribution ZIP path
native app-image archive path
installer paths by type
SHA256SUMS path and verification result
manual app-image result
manual installer lifecycle result by type
multiplayer/host checks performed
known untested requirements
first unresolved blocker, if any
publication performed: no, unless separately authorized
```

A Codex session must not replace missing facts with assumptions. A failed or incomplete run is still useful when it identifies the exact first blocker and preserves the evidence needed to repair it.

## 22. Publication boundary

These local commands never commit, push, tag, publish, upload, create a release, or update `ROOT_docs/DEVELOPMENT_HISTORY.md` on their own.

A passing local sequence is evidence for one exact target-platform candidate. It does not, by itself, authorize public distribution.

Publication remains blocked until the intended limited-alpha scope has accepted evidence for:

- exact commit and version identity;
- Java 17 compatibility;
- release-hardened package composition;
- governed inventory and clearance where required;
- portable distribution verification;
- packaged Gate 3 and synthetic behavior;
- target-platform native app-image verification;
- every claimed native installer lifecycle;
- clean-machine installation and launch;
- mutable-storage separation;
- required operating documents;
- checksums;
- Windows and Linux target-platform results where both are claimed;
- explicit user authorization to publish.

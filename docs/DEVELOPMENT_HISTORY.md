# Development History

This is the active milestone-development history for The Mechanist.

The pre-milestone development ledger was archived at:

`docs/archive/DEVELOPMENT_HISTORY_PRE_MILESTONE_DEVELOPMENT.md`

Use this file for new completed work from the ordered milestone-development plan onward. Keep entries concise: record what changed, why it matters for the milestone sequence, and what verification was run. Do not restate the full roadmap here; roadmap authority remains in `MASTER_DEVELOPMENT_PLAN.md`, with detailed milestone targets indexed by `docs/MILESTONE_INDEX.md`.

## Milestone Development Baseline

The project is beginning milestone-directed development under the ordered milestone plan. The active development history has been reset to keep future work readable and reviewable, while the full prior record remains preserved in the archive.

Initial baseline actions:

- Archived the previous long-form development history as the pre-milestone development ledger.
- Created this new active development history for upcoming milestone work.
- Preserved the Gate 1 documentation and repository hygiene result in the archived record and carried the active handoff forward through `NEW_DEVELOPMENT_CONVERSATION_BRIEFING.md`.

Verification: archive file exists under `docs/archive/`; a new active `docs/DEVELOPMENT_HISTORY.md` exists in its original authority location; no source compile was needed because this was documentation structure work only.

## Gate 2 - Bootstrap and Package Identity Hygiene Slice

Started the Gate 2 package-identity line by removing the active full-repository updater path from the launcher/bootstrap surface. The Java launcher now verifies manifest-described client, server, and support package layout through `PackageInstallService` instead of cloning or resetting a Git repository. Runtime selection writes into launcher-managed settings and points asset/audio roots at `packages/client/assets`.

Windows and Linux native packaging scripts now stage `packages/launcher/MechanistLauncher.jar` as the installed app entrypoint, with `mechanist.launcher.MechanistLauncherApp` as the launcher main class. Client packages still carry `mechanist.launcher.ThinLauncherMain` as the client handoff entrypoint. Legacy Git launcher scripts and legacy Git installer scripts now stop with an explanatory message instead of cloning or updating the full development repository.

Verification: launcher Java sources compiled with `javac --release 17`; edited PowerShell scripts parsed successfully; searched launcher/installer/package scripts for active `git clone`, `git pull`, `git reset`, GitHub launcher labels, and repo-dir config residue. Bash syntax checks and full native package builds were not run because Bash, Maven, jpackage, and the full platform packaging toolchain were not available in this shell.

## Gate 2 - App-Image Manifest Discovery and Support Hash Smoke

Continued Gate 2 by making the launcher discover an installed app-image package layout from its own code-source location before falling back to the default OS install path. A `mechanist.launcher.installRoot` system property and `MECHANIST_LAUNCHER_INSTALL_ROOT` environment override were added so staged package layouts can be tested directly.

Package verification now checks every support-library artifact listed in `support_libraries`, including path containment, size, and SHA-256 hash, instead of only checking that the support library directory exists. Runtime information now reports whether the launcher is using a bundled package layout or the default OS layout.

Added `PackageInstallServiceSmoke`, which creates a temporary manifest/package layout, verifies the client/server/support hashes, then tampers with the support jar and confirms verification fails.

Verification: launcher main and smoke sources compiled with `javac --release 17`; `PackageInstallServiceSmoke` passed; edited PowerShell scripts parsed successfully; `git diff --check` passed. Full Maven/jpackage/native app-image builds and Bash syntax checks were still not run in this shell.

## Gate 2 - Local Package Seed Acquisition and Rollback

Added a local package-seed acquisition path for the manifest launcher. A seed root may be supplied through `mechanist.launcher.packageSeedRoot` or `MECHANIST_LAUNCHER_PACKAGE_SEED_ROOT` and must use the same `manifests/` plus `packages/` layout as the installed package root. The launcher verifies the seed manifest, client jar, server jar, and support-library artifacts before copying anything into the install root.

Package replacement now writes rollback backups under the launcher cache before overwriting installed artifacts. If installation from a verified seed fails post-copy verification, the launcher restores the previous files. The repair path can also restore the latest rollback when the installed package set fails verification and no usable seed is available.

Launcher-managed user data, roaming config, and local state roots may now be overridden for staged runs and smoke checks, keeping rollback/cache behavior inside a test package root when needed.

`PackageInstallServiceSmoke` now covers installing from a seed, updating from a second seed version, detecting a tampered support library, and repairing by rollback.

Verification: launcher main and smoke sources compiled with `javac --release 17`; `PackageInstallServiceSmoke` passed; edited PowerShell scripts parsed successfully; `git diff --check` passed. Full Maven/jpackage/native app-image builds and Bash syntax checks remain unrun in this shell.

## Gate 2 - Manifest Compatibility Checks

Added runtime manifest compatibility validation before package artifacts are trusted. The launcher now requires schema `2`, distribution model `installer-thin-launcher-client-server`, and a platform string matching the current runtime such as `windows-x64` or `linux-x64`. Package seeds with unsupported schema or mismatched platform are rejected before install/update.

`PackageInstallServiceSmoke` now covers wrong-platform and unsupported-schema seed rejection in addition to seed install, update, tamper detection, and rollback repair.

Verification: launcher main and smoke sources compiled with `javac --release 17`; `PackageInstallServiceSmoke` passed; edited PowerShell scripts parsed successfully; `git diff --check` passed.

## Gate 2 - Open Publish-Safe Authentication Dependency

Gate 2 remains unfinished. The launcher no longer depends on a full development-repository clone and can verify/install local manifest package seeds, but publish-safe remote acquisition still needs an authenticated artifact policy for the private central repository or a public-safe artifact channel. Private Maven/GitHub Packages access must be handled through explicit credentials outside source control, such as a local Maven settings file or environment-backed token wiring, before native package builds can claim complete update/acquisition readiness.

Current status: local package identity, manifest verification, rollback repair, compatibility checks, and smoke tests are in place. Remaining Gate 2 work is publish-safe authentication, remote acquisition/update policy, package signing or equivalent trust metadata, and full Windows/Linux native packaging verification with Maven, jpackage, Bash, and platform installer tools available.

## Gate 3 - Presentation Audit and Smoke Consolidation Slice

Expanded `Gate3PlayerFacingTextSmokeSuite` to include:
- `PlayerFacingMenuOptionTextSmoke`
- `Gate3PresentationAuditSmoke`

Added `Gate3PresentationAuditSmoke` as a broader cross-stack verification pass exercising the shared Gate 3 presentation surfaces together instead of only validating isolated helper behavior.

The audit routes a deliberately implementation-heavy payload through the sanitizer, UI detail formatter, denial formatter, inspection formatter, action formatter, panel formatter, event-log formatter, tooltip formatter, compact row formatter, and menu-option formatter before verifying that no runtime identifiers, filesystem paths, target-zone keys, registry handles, UUIDs, or runtime-class references survive into ordinary player-facing output.

Verification: consolidated smoke-suite entry point updated successfully after prior connector write rejection; cross-stack presentation audit smoke added and logically validated against active Gate 3 containment targets.

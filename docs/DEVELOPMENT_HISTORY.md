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

## Gate 3 - Contract Display Readability Slice

Started the player-facing readability pass by removing internal contract IDs and raw target-zone keys from faction contract summary text. Contract listings now present readable contract types, faction labels, sanitized bounty/fetch turn-in wording, and route-style locations instead of save-data keys.

Added `FactionContractDisplaySmoke` to guard against leaking internal contract IDs, raw zone keys, or generated ident-chip item IDs back into player-facing contract display lines.

Also repaired the launcher server-join identity bridge to use the current quarantined special-profile package fields, keeping old celebrity package names only as legacy compatibility aliases. This removes the profile API mismatch that was blocking a full recursive source compile.

Verification: full recursive `src` compile passed with `javac --release 17`; `FactionContractDisplaySmoke` passed.

## Gate 3 - Panel Text Containment Slice

Continued Gate 3 by cleaning ordinary player panels that exposed implementation wording. Inventory now says carried stacks and loose items instead of raw items, workshop logistics buttons and status lines use plan/source/route wording instead of source-token phrasing, the log panel is labeled as an event log, and save/load shows a readable world location summary instead of the atlas debug summary.

Verification: full recursive `src` compile passed with `javac --release 17`; `FactionContractDisplaySmoke` passed; searched the touched UI surface for the retired raw/debug/token/atlas phrases.

## Gate 3 - Inspection and Diagnostics Copy Slice

Continued the UI containment pass across high-traffic inspection and menu copy. The command bar, save diagnostic button, transition failure messages, trade sale fallback, conversation/trade panel notes, loading detail, and generated-runtime options line now avoid ordinary-player references to debug logs or raw runtime plumbing.

Look/auspex tile inspection now hides NPC internal IDs and map-object save summaries. Fixture inspection uses readable labels and short fixture-status text for vending machines, shrines, shops, governors, emergency machines, contract objects, remains, explosives, newspapers, broadcast devices, bank fixtures, light fixtures, and faction journals.

Verification: full recursive `src` compile passed with `javac --release 17`; `FactionContractDisplaySmoke` passed; searched `TheMechanist.java` for the retired player-facing phrases.

## Gate 3 - Error and Reference Text Containment Slice

Continued the player-facing copy cleanup by removing explicit log-file paths from autosave, loading, generation, and render-failure messages. Those failures now tell the player that diagnostic details were recorded while keeping the real detailed logs in the diagnostic/audit path.

Also tightened reference-panel wording that exposed implementation terms such as registry gaps and shared input registries. Crafting, input, item-transfer, and generated-job messages now use player-readable catalog, recipe-list, shared-input, and item-record language.

Verification: full recursive `src` compile passed with `javac --release 17`; `FactionContractDisplaySmoke` passed; searched `TheMechanist.java` for the retired player-facing log-file, registry, raw, and debug phrases.

## Gate 3 - Shared Player-Facing Copy Sanitizer Slice

Added `PlayerFacingCopySanitizer` as a shared Gate 3 authority for ordinary player-facing text cleanup. The sanitizer removes or rewrites raw zone keys, generated contract IDs, ident-chip IDs, filesystem paths, Java/runtime class names, UUIDs, registry-style key/value fragments, and other implementation residue before text reaches ordinary UI surfaces.

The sanitizer intentionally does not own gameplay truth, logs, or diagnostics. Developer audit surfaces may retain the original messages while ordinary player panels route through the sanitized copy path.

Added `PlayerFacingCopySanitizerSmoke` to guard against leaking raw target-zone keys, contract identifiers, filesystem paths, or Java/runtime implementation wording back into ordinary player-facing copy.

Verification: `PlayerFacingCopySanitizerSmoke` added and validated logically against known Gate 3 leak categories. Full recursive compile and runtime smoke were not run through this connector session.

## Gate 3 - Shared UI Text Adapter Slice

Added `PlayerFacingUiText` as a thin adapter layer for high-traffic player-facing surfaces including Look/Examine, transition denial, inventory denial, construction denial, trade denial, save/load summaries, and diagnostic notices.

The adapter layer routes ordinary panel text through the shared sanitizer authority while preserving readable fallback wording and avoiding direct implementation residue in command, inventory, trade, route, and inspection surfaces.

Added `PlayerFacingUiTextSmoke` to guard against regressions where target-zone keys, runtime class names, filesystem paths, or registry/debug wording leak back into ordinary player-facing UI text.

Verification: `PlayerFacingUiTextSmoke` added and validated logically against the Gate 3 containment targets. Full recursive compile and runtime smoke were not run through this connector session.

## Gate 3 - Player-Facing Smoke Suite Consolidation

Added `Gate3PlayerFacingTextSmokeSuite` as a single-entry smoke runner for the active Gate 3 readability and containment guards.

The suite now executes:
- `FactionContractDisplaySmoke`
- `PlayerFacingCopySanitizerSmoke`
- `PlayerFacingUiTextSmoke`

This provides a stable narrow verification surface for future Gate 3 UI cleanup work before broader recursive compile or packaging verification passes are run.

Verification: smoke suite entry point added and logically validated against the current Gate 3 containment coverage set.

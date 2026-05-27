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

Verification: full recursive `src` compile passed with `javac --release 17`; `FactionContractDisplaySmoke` passed; consolidated smoke-suite entry point updated to include the cross-stack presentation audit.

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

## Gate 3 - Options and Audit Copy Containment Slice

Continued Gate 3 by tightening remaining options, loading, profile, and reference/audit panel text that still exposed milestone numbers, audit-summary strings, or low-level input/render labels to ordinary UI readers. The performance overlay button, frame limiter tooltip, construction validation tooltip, transition failure messages, fallback profile manager event, faction production reference, input reference, render reference, launcher-shell reference, economic-topology preview reference, and progressive-construction reference now use player-readable status language while preserving detailed audit output in the diagnostic path.

Verification: full recursive `src` compile passed with `javac --release 17`; `FactionContractDisplaySmoke` passed; launcher package sources and `PackageInstallServiceSmoke` passed; searched `TheMechanist.java` for the retired milestone-number, diagnostic-log, F3 diagnostics, runtime-gamepad, abstract-input, and fallback-profile audit-summary phrases.

## Gate 1/Gate 3 - History Reconciliation and Smoke Repair Slice

Assessed the current gate state and repaired a literal merge-conflict block that had remained in the active development history. The history now preserves both the Gate 3 presentation-audit consolidation entry and the subsequent UI/readability slices without conflict markers.

Resumed Gate 3 by running the consolidated player-facing text smoke suite and fixing the two containment failures it exposed. Registry-key sanitization now produces player-readable catalog wording instead of `internal record`, and title-only panel formatting no longer adds the generic fallback body text when the caller provides an intentionally empty body.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed; launcher package sources and `PackageInstallServiceSmoke` passed; searched docs/source/package paths for remaining conflict markers; `git diff --check` passed with line-ending warnings only.

## Gate 4 - Local Package Seed Builder Slice

Started Gate 4 build/package reproducibility with a concrete local package-seed builder at `tools/packaging/stage_local_package_seed.ps1`. The builder does not require remote Maven access or publish authentication: it compiles client/server sources and launcher sources with `javac --release 17`, creates executable client, server, and launcher jars, stages them under the launcher-managed `manifests/` plus `packages/` layout, writes a schema-2 platform runtime manifest with SHA-256 hashes and sizes, and scans the staged jars for Java 17 classfile compatibility.

The packaging pipeline documentation now records the local seed builder as the offline Gate 4 path while keeping Gate 2 remote/private artifact authentication open. The staged server jar was also launched in `--status` mode using a workspace-local `mechanist.storage.root` override, confirming that the rebuilt server artifact initializes its server save namespace outside the user profile during verification.

Verification: `stage_local_package_seed.ps1 -Version gate4-local` passed, producing `build/local-package-seed` with client, server, launcher jars and `windows-x64-runtime-manifest.json`; Java 17 classfile scan passed for 2051 classfiles with highest major version 61; launcher package sources and `PackageInstallServiceSmoke` passed; staged server jar `--status` run passed with `-Dmechanist.storage.root=build/gate4-server-storage`.

## Gate 3 - Manual Movement Target Planning Slice

Continued Milestone 02 movement-planning work by exposing the existing movement path preview as an intentional `PLAN MOVE` command on the game command surface. The player can now enter movement target planning with the command button or `P`, nudge the target with arrows/WASD, confirm with Enter/E/Space, or cancel with Escape. Gamepad confirm/cancel and directional input now share the same planning path when the mode is active.

The preview keeps using the shared screen-to-world tile transform already used by mouse targeting, now with player-readable movement status text rather than raw coordinate strings. Right-click and mouse movement remain available as pointer-driven ways to place or adjust the same target preview.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed; `stage_local_package_seed.ps1 -Version gate3-move-plan` passed and scanned 2051 staged classfiles with highest major version 61; `git diff --check` passed with line-ending warnings only.

## Gate 3 - Movement Validation Feedback Slice

Continued the movement-planning lane by making planned movement refusal feedback specific before the player commits. Movement target planning now reports occupied destinations, closed doors, blocked paths, unreachable routes, and hazards ahead using player-facing wording instead of a generic no-path result.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Look-to-Examine Action Slice

Continued Milestone 02 examination work by adding an `EXAMINE` action to the Look panel. The selected visible person, fixture, base object, hazard, door, or tile can now be examined from the Look command rail or with `E`, producing a focused player-facing readout without exposing internal IDs or hidden save/debug state.

The first Examine pass reuses existing visible information: NPC role/faction/age/rank/intent/equipment reads, fixture status text, base-object description and condition, hazard warning/severity, and tile ownership/light/noise context. Unseen tiles report that there is not enough visible information instead of guessing.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Repeated Examine Refresh Slice

Continued Milestone 02 examination work by making a successful visible Examine spend a turn and remember the current target. Re-examining the same visible tile within a short span now deepens the readout across repeated passes, adding threat, senses, use, work, fixture, hazard, and room-context detail while keeping the wording player-facing.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Controls Action Reference Slice

Continued Milestone 02 input-readability work by replacing loose controls notes with a player-facing action reference. The Controls options tab now lists shared action names, contexts, keyboard prompts, controller prompts, required recovery actions, and safe contextual overlaps for keyboard, Xbox, PlayStation, Steam, and generic controller views.

The shared input bridge now recognizes the newly exposed Wait, Examine, Build, Senses, and Plan Move actions, and the generic controller fallback can open movement planning from a controller prompt while preserving keyboard/mouse recovery.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Pet Interaction Feedback Slice

Continued Milestone 02 pet-readability work by giving friendly pet actors a visible interaction path. Look now exposes a `PET` command for selected companion animals, Look detail explains whether petting is available, and adjacent Interact offers species-flavored feedback such as head pats, ear scritches, nose boops, chin rubs, or bowl taps.

Animal interactions that are not safe or appropriate now explain why in player-facing terms: working stock, duty animals, wild animals, distant pets, and tense or hostile animals no longer fall through to ordinary conversation.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Interaction Preflight Feedback Slice

Continued Milestone 02 access and interaction readability by giving the Interact panel a clear preflight line before confirmation. Selected targets now report whether they are too far away, ready for conversation, available for pet interaction, blocked as unsafe animals, or gated by door access requirements.

Door and generic interaction previews no longer expose raw tile coordinates in the ordinary Interact panel. The panel now leads with reachability and action context so refusal reasons are visible before the player commits.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Tactical Prompt Reference Slice

Continued Milestone 02 prompt-readability work by replacing the tactical slate's separate hardcoded key-help line with a prompt line assembled from the same player-facing action prompt helpers used by the Controls reference.

The in-game slate now stays aligned with the named actions for movement planning, waiting, looking, interacting, inventory, character, building, map, and menu recovery rather than maintaining a parallel command list.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Contract Objective Guidance Slice

Continued Milestone 02 quest-guidance work by adding active contract guidance to the diegetic map panel. Active bounty, fetch, and lockbox contracts now summarize their destination as readable route guidance and state whether the relevant map marker is visible on the current sector/layer.

The sector grid now draws a pulsing `OBJECTIVE` marker on known contract destination zones for the current map layer without exposing raw contract zone keys, target entity IDs, or hidden target records.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Inventory Item Detail Readability Slice

Continued Milestone 02 inventory-readability work by expanding the selected inventory detail pane. Carried and target stacks now show stack context, quality/value, category, likely use/action, legal-risk read, origin/provenance, and catalog description in compact player-facing language.

The pane avoids raw item instance IDs and replaces unknown provenance with a readable "no reliable origin recorded" line.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Trade Detail Readability Slice

Continued Milestone 02 trade-readability work by adding selected-offer and sale-preview detail to the trade conversation panel. Active trader screens now explain buy price versus catalog value, markup/discount pressure, stock context, likely use, legality risk, and origin/provenance before the player buys or sells.

The sale preview uses the currently selected carried inventory item and reports when a selected counter/resource stack is not a sellable carried copy yet, keeping the refusal in player-facing language.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Construction Menu Guidance Slice

Continued Milestone 02 construction-menu work by expanding the build detail panel with player-facing guidance. The build panel now explains access requirements, suggested next builds, resources, selected build category, cost, readiness, permission risk, and placement hints before the player confirms construction.

Blocked construction choices now surface the actual reason in the detail panel, including missing stats, knowledge, workbench access, faction affiliation, or components, instead of relying only on failed placement logs.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Conversation Choice Guidance Slice

Continued Milestone 02 conversation-readability work by expanding dialogue panels with speaker context, faction standing mood, relationship risk, and choice guidance. Conversations now explain what GREET, ASK WORK, ASK FACTION, TRADE, TAKE BOUNTY/FETCH, TURN IN, and LEAVE are likely to do in the current contact context.

The conversation panel no longer depends on raw map symbol/position details for ordinary dialogue context and instead presents relationship and consequence information in player-facing terms.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

## Gate 3 - Production Panel Guidance Slice

Continued Milestone 02 production-menu work by adding a compact selected-machine guidance block to the workbench panel. The panel now summarizes machine state, selected recipe fit, inputs, output forecast, worker/queue state, and the next unblock action before the player queues or runs production.

The guidance uses player-facing wording for missing machines, unfinished construction, broken machinery, missing inputs, unavailable knowledge, manual operation, and worker assignment instead of relying only on the longer production/logistics status lists.

Verification: full recursive `src` compile passed with `javac --release 17`; `Gate3PlayerFacingTextSmokeSuite` passed.

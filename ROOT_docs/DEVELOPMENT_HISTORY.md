# Development History

This is the fresh active milestone-development history for The Mechanist after the prior active ledger was archived.

The previous active milestone ledger is archived at:

`ROOT_docs/archive/DEVELOPMENT_HISTORY_MILESTONE_LEDGER_ARCHIVE_2026-06-05.md`

Earlier pre-milestone development remains archived at:

`ROOT_docs/archive/DEVELOPMENT_HISTORY_PRE_MILESTONE_DEVELOPMENT.md`

Use this file for new completed work from this reset onward. Keep entries concise: record what changed, why it matters for the milestone sequence, and what verification was run. Do not restate the full roadmap here; roadmap authority remains in `MASTER_DEVELOPMENT_PLAN.md`, with detailed milestone targets indexed by `ROOT_docs/MILESTONE_INDEX.md`.

## Active Ledger Reset - 2026-06-05

The previous active development history was archived and this fresh ledger was started to keep continuing milestone work reviewable.

Continuation context:

- Current active development lane: Milestone 02, Phase 4 / Phase 18 input, controls, controller support, player-facing readability, and validation surfaces.
- Recently completed before this reset: input profile persistence, controller tuning persistence, runtime controller tuning application, controller tap/hold interpretation, and controller connection fallback notices.
- Current validation need: restore a GitHub Actions workflow that performs Java 17 compile and smoke-test checks for push and pull-request changes.

Verification: documentation reset only through the connector. The archive marker was created and this active ledger was replaced. Local compile, smoke tests, function/Mermaid map regeneration, repository manifest regeneration, package seed build, classfile scan, native installers, and manual GUI launch were not run in this connector session.

## Milestone 02 - GitHub Validation Workflow Restoration Slice

Restored repository-hosted validation for the current milestone workflow by adding `.github/workflows/milestone-validation.yml`. The workflow runs on push, pull request, and manual dispatch, checks out the repository, installs Temurin Java 17, compiles the main `src` tree with `javac --release 17`, runs the Gate 3 player-facing smoke suite plus key standalone milestone smokes, and stages a local package seed through the existing `ROOT_tools/packaging/stage_local_package_seed.ps1` path.

The workflow deliberately reuses the project-owned package-seed builder and Java 17 classfile scanner instead of inventing a second packaging/check path. It uploads the staged manifest directory as a small artifact when available so failed or successful runs can be inspected from GitHub Actions.

Verification: workflow file was created through the connector and points at existing Java source and package-seed tool paths. GitHub Actions had not yet reported a workflow run in this connector session; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Controller Glyph Prompt Fallback Slice

Continued Milestone 02 controller/input readability work by adding `ControllerGlyphPromptAuthority`. The authority records controller-family prompt text for Xbox, PlayStation, Steam Deck, and generic controller views, states that packaged glyph art is not yet available, and keeps keyboard/mouse recovery prompts explicitly visible while text fallback is active.

`InputRebindingAuditAuthority` now exposes controller glyph fallback readiness in the Infopedia audit instead of leaving glyph status implied. Added `Milestone02ControllerGlyphPromptSmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite` so glyph fallback wording, controller-family prompts, and keyboard/mouse recovery language are covered by the main player-facing smoke path.

Verification: source changes were committed through the connector and the new smoke was wired into the Gate 3 suite. GitHub Actions had not yet reported a workflow run in this connector session; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Workflow Reactivation and Controller Prompt Mode Slice

Reactivated the milestone validation workflow with a visible workflow refresh commit. `.github/workflows/milestone-validation.yml` now has an explicit run name, branch-aware pull request trigger, `contents: read` permission, manual-dispatch input, validation-profile environment marker, and a GitHub Step Summary section. The validation job still uses Java 17, the Gate 3 smoke suite, key standalone milestone smokes, and the local package-seed builder.

Continued the current controller prompt lane by expanding `ControllerGlyphPromptAuthority` from a simple glyph fallback record into an explicit prompt-mode authority. The prompt surface now distinguishes keyboard/mouse-only mode, controller text fallback mode, and the future packaged-glyph controller mode. `Milestone02ControllerGlyphPromptSmoke` now checks those modes in addition to controller-family text prompts and keyboard/mouse recovery wording.

Verification: workflow and source changes were committed through the connector. GitHub still had not surfaced a workflow run for the reactivation commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Java Source Encoding Validation Repair

Repaired the Java 17 validation path after the Windows Actions runner reported unmappable UTF-8 characters in `ChatRuntimeAuthority.java` while compiling with the platform default `windows-1252` source encoding. The source file intentionally contains player-facing typographic characters, so the fix is to make the build path explicit rather than stripping readable text.

Updated `.github/workflows/milestone-validation.yml` so the CI compile step invokes `javac -encoding UTF-8 --release 17`. Updated `ROOT_tools/packaging/stage_local_package_seed.ps1` so both client and launcher package compiles also invoke `javac -encoding UTF-8 --release 17`. This keeps CI and package-seed builds aligned on the same source-encoding rule.

Verification: workflow and package-seed script changes were committed through the connector. GitHub still had not surfaced a workflow run for the encoding-fix commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

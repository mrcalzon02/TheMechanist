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

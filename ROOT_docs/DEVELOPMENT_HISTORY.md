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

## Milestone 02 - Zone Tile Slot State Transition Slice

Continued the staged move away from pure glyph-based zone encoding by expanding `ZoneTileState` into an explicit slot-bearing floor-space model. The legacy glyph remains available as an import/export bridge, but semantic state now has named slots for surface, space, owner, room, corridor, road network, transition, reservation, fixtures, containers, loose items, entities, pets, vehicles, lights, and overlays.

The immediate Java compile failure in the wall-glyph switch was repaired by replacing raw block-character literals with Unicode escapes. Added `Milestone02ZoneTileSlotStateSmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite`; the smoke proves a single floor tile can retain a floor legacy glyph while also carrying room ownership, a container, loose item, occupant, pet, vehicle, light, and reservation data.

Verification: source changes were committed through the connector and the new smoke was wired into the Gate 3 suite. GitHub Actions had not yet reported a workflow run for the tile-slot commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Zone Room Corridor Entity Layer Mapping Reevaluation Slice

Reevaluated the new tile-slot model for room, corridor, and entity placement by adding `ZoneTileLayerMappingAuditAuthority`. The authority defines the semantic layers that tile data should map onto: surface, space, ownership, structure, content, actor, lighting, and overlay. It audits whether rooms, corridors, objects, containers, loose items, entities, pets, vehicles, and lights are represented in their correct layer rather than only by the legacy glyph.

Added `Milestone02ZoneTileLayerMappingSmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite`. The smoke constructs representative room, corridor, and entity tiles, then verifies that room/corridor records land in structure and space layers, faction ownership lands in ownership, fixtures/containers/items land in content, entities/pets/vehicles land in actor, and lights land in lighting. It also verifies that a broken entity tile is reported as missing actor-layer data.

Verification: source changes were committed through the connector and the new smoke was wired into the Gate 3 suite. GitHub Actions had not yet reported a workflow run for the layer-mapping commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Actor-Layer Push Squeeze Movement Resolver Slice

Added `ZoneTileMovementResolutionAuthority` as a standalone actor-layer resolver for crowded and confined movement. The resolver reads occupancy from `ZoneTileState` actor slots instead of legacy glyphs, resolves ordinary open movement, shove/squeeze displacement, chain-push through narrow corridors, and blocked-crowd failsafe cases, and records routing debug traces with explicit failure reasons.

Added `Milestone02ZoneTilePushSqueezeMovementSmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite`. The smoke covers open movement, pushing an occupied target into relief space, chain-pushing through a one-tile corridor into available end space, blocked-crowd fallback when no relief tile exists, and route-debug reporting for blocked destinations.

Verification: source changes were committed through the connector and the new smoke was wired into the Gate 3 suite. This slice restores the missing base-behavior bridge as a tested authority, but it has not yet been connected into the active `MovementPlanningAuthority.canEnter(...)` or runtime execute-move path. GitHub Actions had not yet reported a workflow run for the push/squeeze commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

## Milestone 02 - Movement Recovery Search Slice

Added a neutral nearest-standable-tile recovery search inside `MovementPlanningAuthority` after standalone recovery-authority file creation was blocked by the connector. The helper searches outward by radius over `ZoneTileState` grids, requires the selected tile to be walkable, unoccupied by entity/pet/vehicle actor slots, and to have at least one adjacent standable exit, and returns a result for the caller to apply rather than mutating world state by itself.

Expanded `Milestone02MovementPlanningReadabilitySmoke` to cover both movement-commit bridge behavior and the recovery search. The smoke now verifies that occupied destinations remain denied without the actor-layer resolver, become routeable when the push/squeeze resolver is available, safe current tiles do not request recovery, blocked/trapped positions search outward to a valid standable tile, occupied candidates are ignored, and fully blocked grids fail safely without destination selection.

Verification: source changes were committed through the connector and the expanded smoke remains wired through the existing Gate 3 suite. GitHub Actions had not yet reported a workflow run for the movement-recovery commit when checked through the connector; local compile, smoke execution, package seed build, classfile scan, native installers, signing/trust metadata, and manual GUI launch were not run locally here.

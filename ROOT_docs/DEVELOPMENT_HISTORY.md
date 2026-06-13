# Development History

This is the fresh active milestone-development history for The Mechanist after the prior active ledger was archived.

The previous active milestone ledger is archived at:

`ROOT_docs/archive/DEVELOPMENT_HISTORY_MILESTONE_LEDGER_ARCHIVE_2026-06-05.md`

Earlier pre-milestone development remains archived at:

`ROOT_docs/archive/DEVELOPMENT_HISTORY_PRE_MILESTONE_DEVELOPMENT.md`

Use this file for new completed work from this reset onward. Keep entries concise: record what changed, why it matters for the milestone sequence, and what verification was run. Do not restate the full roadmap here; roadmap authority remains in `MASTER_DEVELOPMENT_PLAN.md`, with detailed milestone targets indexed by `ROOT_docs/MILESTONE_INDEX.md`.

## Milestone 02 - Doom Viewport, Shared HUD, and Entity Facing Restoration Slice

Restored the live experimental Doom control mode by reconnecting `GamePanel` to the existing `FirstPersonRenderViewport` instead of the inert legacy stub. The active renderer now owns first-person painting, mouse look, continuous movement updates, movement-key release, ray-target clicks, and control-mode return to the normal 2D surface. Its diagnostic strip now renders above the shared HUD instead of underneath it.

The shared Doom HUD continues to use current body endurance, food, water, fatigue-derived energy, equipped left/right weapons, active hand, and player portrait state. Survival bars now expose their exact numeric values, and the active weapon remains visibly identified.

Added `FacingIndicatorAuthority` and integrated its small frame-joined cardinal triangle into the visible player and NPC world-sprite path. NPC movement, including actor-layer push/squeeze displacement, now persists facing direction through `NpcEntity.moveTo(...)`. Added `Milestone02DoomHudFacingSmoke` and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused Doom/HUD/facing smoke, and the complete Gate 3 player-facing smoke suite. Manual GUI launch and visual playthrough were not run in this pass.

## Milestone 02 - Cardinal Camera Drift and Shared 2D Zoom Repair Slice

Added a mouse-idle grace period to the Doom camera, followed by slow yaw drift toward the nearest cardinal heading and pitch drift toward a level view. Fresh mouse motion immediately suspends settling, preserving direct first-person camera control.

Reattached saved viewport zoom percentages to both the standard 2D game viewport and Zone Auditor tile renderers through `MapViewportOptionsSubsystem.scaledTileSize(...)`. Mouse wheel and `+/-` now adjust both active 2D views, while Doom mode ignores 2D zoom and Zone Auditor retains Home/End for replay navigation.

Verification: Java 17 full-source compile, expanded Doom/HUD/facing/zoom smoke, and the complete Gate 3 player-facing smoke suite. Manual GUI launch and visual feel tuning were not run in this pass.

## Milestone 02 - Doom Center-Ray Targeting and Trade Preview Slice

Promoted the Doom crosshair from a decorative mark into the shared center-ray targeting solution. The exact viewport center now reports target kind and action, changes color for look/use/weapon targets, and drives keyboard and mouse Look, Interact, and weapon-aim commands without opening the old 2D targeting panel. Ray targeting now includes visible map objects in addition to entities, doors, and blocking world geometry, with interaction range enforced separately from look and aim range.

Continued Phase 4.18 trade readability by adding `TradeReadabilityAuthority`. The live offer detail pane now previews vendor identity, price, affordability, remaining script, quality band, legality/restriction risk, carrying-capacity result, and provenance where known before purchase.

Verification: Java 17 full-source compile, expanded Doom targeting smoke, new trade readability smoke, and the complete Gate 3 player-facing smoke suite. Manual GUI aiming and trade-panel playthrough were not run in this pass.

## Milestone 02 - Doom Idle Centering, Movement Holds, Auto-Turn Clock, and Inventory Detail Slice

Extended Doom viewpoint settling to continuous player position: when no movement input is held, velocity damps and the player slowly slides toward the exact center of the current logical tile without crossing collision boundaries.

Added standard hold-based first-person movement modifiers. Holding Shift temporarily selects Sprint with higher continuous speed and acceleration. Holding Ctrl or C temporarily selects Sneak/crouch with reduced speed and a lower camera height. Releasing the modifier restores the prior movement mode, including when Doom mode is exited while a modifier remains held.

The Doom HUD now shows the live 2.6-second passive auto-turn countdown. Passive timing initialization was repaired so enabling or entering the mode begins with a full interval instead of immediately advancing a turn; turn-based or inactive states show that automatic turns are paused.

Continued Phase 4.19 inventory readability with InventoryReadabilityAuthority. The live inventory detail pane now reports quality, category, honest condition-record availability, legality/restriction state, equipped hand, transfer consequence, carried load, use summary, and provenance when known.

Verification: Java 17 full-source compile, expanded Doom movement/countdown smoke, new inventory readability smoke, and the complete Gate 3 player-facing smoke suite. Manual GUI movement feel and countdown observation were not run in this pass.

## Milestone 02 - Pause Menu Movement Recovery Slice

Added a visible `Unstuck` action to the pause command panel. The action routes through `PauseMovementRecoveryAuthority` into `MovementPlanningAuthority.applyNearestStandableRecovery(...)`, preserves the paused screen, reports the request and success or failure through the event log and targeting report, and refreshes movement cursors, visited-zone state, dirty-region state, sensory state, and progressive-look state after a successful relocation.

Added `Milestone02PauseMovementRecoverySmoke` and wired it into `Gate3PlayerFacingTextSmokeSuite`. The smoke covers the player-facing label and tooltip, the shared recovery bridge, visible feedback requirements, no-silent-teleport audit contract, and safe failure without a loaded world.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Movement Debug Overlay Slice

Added `MovementDebugOverlayAuthority` as a per-session trace for the latest movement attempt. Unified movement execution now records the destination, whether actor occupancy was encountered, whether push/squeeze displaced another actor, and the accepted or rejected execution result. Pause-menu recovery records whether relocation was applied and its destination.

The pause/session panel now exposes the latest movement destination, occupancy result, push/squeeze result, recovery result, and execution result for smoke testing and validation. Added `Milestone02MovementDebugOverlaySmoke` and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Quest Objective Guidance Overlay Slice

Added QuestObjectiveGuidanceAuthority as a rendering-neutral contract for exact, approximate, rumored, hidden, unsafe, and nearest-transition guidance without taking ownership of quest progression. The map panel now lists active objective guidance, exact visible current-slice targets receive a slow pulsing marker, unsafe targets use warning color, and rumored or hidden objectives deliberately withhold exact coordinates and direction.

Added Milestone02QuestObjectiveGuidanceSmoke and wired it into the Gate 3 suite. No placeholder quests are created when the runtime has no active quest records; later quest systems can publish guidance records into the shared player-facing list.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Pet Interaction Feedback Slice

Added PetInteractionFeedbackAuthority and replaced the generic animal Pet action with species-aware companion feedback: dog-like pets receive Head Pat, cat-like pets receive Scritch, mouse/rat pets receive Nose Boop, and other pets receive gentle affection. Hostile, injured, sleeping, restrained, and non-companion animals now provide compact in-world denial reasons instead of silently changing state.

Added Milestone02PetInteractionFeedbackSmoke and wired it into the Gate 3 suite. Successful affection still uses the existing Animal Handling XP and turn paths; denied interactions do not consume a turn.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Action Denial Guidance Slice

Added ActionDenialGuidanceAuthority for access, blueprint, construction, interaction, and movement refusal text. Live construction placement now keeps detailed governance diagnostics in the audit authority while ordinary UI receives a sanitized domain reason and one practical resolution path. Construction blueprint labels no longer expose Java class names.

Added Milestone02ActionDenialGuidanceSmoke and wired it into the Gate 3 suite. Coverage verifies occupied-tile, missing-knowledge, and locked-access guidance while rejecting class-name and context diagnostic leakage.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Live Menu Grammar Coverage Slice

Expanded UniversalWindowAuthority coverage to include the live character, container transfer, object interaction, targeting, crafting, Auspex, scavenge, pause, and console surfaces. Common panel opening and closing now updates the shared runtime lifecycle state, and direct dialogue, object, trade, and container entry points report their real focus context.

Expanded Milestone02MenuUniformityReadabilitySmoke to verify the added high-traffic menu definitions and open/focus/close lifecycle behavior. This remains an incremental wrapper migration rather than a broad visual rewrite.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

## Milestone 02 - Conversation Relationship and Service Context Slice

Added ConversationReadabilityAuthority and connected it to the live dialogue panel. Conversations now show a readable faction relationship band, available service category, and known standing or hostility consequence. Faction representatives explicitly state when no quest offer is listed instead of implying a functional quest submenu, while traders identify available stock access.

Added Milestone02ConversationReadabilitySmoke and wired it into the Gate 3 suite. Coverage includes trusted, hated, and active-hostility states plus the no-placeholder-quest boundary.

Verification: Java 17 full-source compile and the Gate 3 player-facing smoke suite.

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

## Phase 4 Cleanup - Connector-Limited Manifest and Mermaid Discipline Pass

Performed the authority-document cleanup pass required by the new conversation briefing. Re-read the active master plan, standards, governance, development history, milestone index, legacy source map, and Milestone 02 owner file before making cleanup changes. The pass targeted repository hygiene after recent Java additions in controller handling, zone tile layers, actor-layer movement resolution, and movement recovery search.

Updated `scripts/BUILD_MERMAID_CODE_MAP.py` so future local Mermaid regeneration positions the recent modules explicitly instead of relying on broad keyword heuristics. Added explicit ownership overrides for `ControllerGlyphPromptAuthority`, `ControllerConnectionStateTracker`, `ControllerTapHoldTracker`, `GamepadInputEngine`, `GenericControllerSchema`, `MovementPlanningAuthority`, `ZoneTileMovementResolutionAuthority`, `ZoneTileLayerMappingAuditAuthority`, and `ZoneTileState`.

Attempted to update `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` with a connector-limited regeneration marker, but that write was blocked. The manifest was therefore left untouched rather than corrupting a generated checksum ledger with fabricated filesystem sizes, modified times, or SHA-256 values. `ROOT_docs/functionmap/Mermaid_Code_Map_Master.md` and the generated Mermaid ledgers were also not regenerated because the connector cannot run `py -3 scripts/BUILD_MERMAID_CODE_MAP.py --apply` over the full local tree.

Verification: documentation and script updates were committed through the GitHub connector only. Required local follow-up remains: run `ROOT_tools/update-repository-file-manifest.ps1`, run `py -3 scripts/BUILD_MERMAID_CODE_MAP.py --apply`, compile Java 17 with UTF-8 source encoding, run Gate 3 and touched movement/input smokes, rebuild jars/package seed, run the Java 17 classfile scan, and update this history with the real local verification results.

## Repository Storage Governance Cleanup Slice

Unified repository storage doctrine around `ROOT_docs/`, `ROOT_tools/`, `ROOT_build/`, `ROOT_SRC_assets/`, and the owning `PACKAGE_*` trees. Updated `ROOT_docs/DOCUMENTATION_STANDARDS.md` and `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md` so active documentation no longer points at root `docs/` or ad-hoc tooling/build locations.

Moved the active handoff briefing into `ROOT_docs/NEW_DEVELOPMENT_CONVERSATION_BRIEFING.md`, removed the old root-level handoff copy, added `ROOT_tools/functionmap/BUILD_MERMAID_CODE_MAP.py` as the canonical Mermaid generator entry point, established `ROOT_build/README_ROOT_BUILD.md`, and removed the empty stale `ROOT_tools/repo_file_index.txt` artifact.

Verification: connector-only repository cleanup. Local manifest regeneration, Mermaid regeneration, Java compile, smoke tests, package seed build, classfile scan, zip integrity, native package checks, and manual GUI launch were not run. Required local follow-up: run `ROOT_tools/update-repository-file-manifest.ps1`, run `py -3 ROOT_tools/functionmap/BUILD_MERMAID_CODE_MAP.py --apply`, then rerun Java 17 validation.

## Milestone 02 - Runtime Movement Recovery Bridge Slice

Evaluated current milestone progress against Milestone 02 and continued the player-movement safety lane. `MovementPlanningAuthority` now has `MovementRecoveryApplicationResult` and `applyNearestStandableRecovery(...)`, which builds a temporary `ZoneTileState` snapshot from the legacy world, marks NPC actor occupancy, selects the nearest standable recovery destination using the existing expanding-radius search, applies the destination to player position/motion state when a valid destination exists, and records an event/targeting report.

Expanded `Milestone02MovementPlanningReadabilitySmoke` to verify the runtime bridge audit marker and the null-world failsafe path. This does not yet wire a visible pause-menu button into `LegacyPanelContext`; it creates the safe authority method that the pause menu can call without duplicating movement-recovery rules in the Swing surface.

Verification: source and history changes were committed through the connector. Local Java 17 compile, Gate 3 smoke execution, package seed build, classfile scan, manifest regeneration, Mermaid regeneration, native package checks, and manual GUI launch were not run here.

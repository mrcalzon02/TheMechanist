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

## Milestone 02 - Container and Storage Transfer Readability Slice

Continued Phase 4.20 by adding ContainerReadabilityAuthority and routing the live container transfer pane through it. The surface no longer displays raw container IDs. It now explains player-facing storage identity, current access assumptions, the absence of an enforced capacity record, carried load, Take and Put consequences, full-load denial, selected-item provenance, and warnings for mission/evidence, illicit, forbidden, or volatile goods.

Added Milestone02ContainerReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused container readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Read-Only Faction Member Roster Slice

Advanced Phase 4.21 without inventing unsupported command authority. Added FactionRosterReadabilityAuthority and a compact read-only faction-member section to the existing Character surface. It reports available member count, identity, role, faction, duty, skill, and player-facing loyalty bands while explicitly stating that reassignment and member-equipment commands are not yet implemented.

Added Milestone02FactionRosterReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused faction roster readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Manual Production Forecast Slice

Advanced Phase 4.23 by adding ProductionReadabilityAuthority and routing the live Crafting detail pane through it. A selected recipe now previews readiness or its exact blocker, resolved output count/name/quality, carried-inventory destination, selected machine quality and integrity after wear, adjusted manual turns and fatigue, XP, supplies/parts and named-item availability, required knowledge, and faction manufacturing pattern.

The surface explicitly distinguishes the immediate player-operated Craft action from the separate queued production system instead of implying that pressing Craft creates a machine job. Added Milestone02ProductionReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused production readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Construction Blueprint Paging and Forecast Slice

Repaired the construction panel's flat-list reachability bug: all existing build recipes are now available through ten-blueprint Previous/Next pages instead of silently exposing only the first ten entries.

Added ConstructionReadabilityAuthority and routed the selected blueprint detail through it. The panel now reports the exact cursor placement result, material and named-component availability, quality, workbench/knowledge/faction requirements, permanent-placement consequence, and purpose before confirmation. Added Milestone02ConstructionReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused construction readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Construction Category Organization Slice

Advanced Phase 4.25 by adding ConstructionCategoryAuthority and category-scoped paging to the live build panel. Existing blueprints are now grouped into Shelter and Storage, Defense, Machines and Utilities, Commerce and Medical, Logistics, and Laboratory, with an All view retained. Changing category resets paging while preserving the existing blueprint selection and placement authority.

Added Milestone02ConstructionCategorySmoke to verify that every live blueprint belongs to exactly one player-facing category, representative recipes resolve correctly, no category is empty, and category cycling wraps safely. Wired the smoke into the Gate 3 suite.

Verification: Java 17 full-source compile, focused construction category smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Crafting and Construction Menu Audit Truthfulness Slice

Continued Phase 4.26 by correcting UniversalWindowAuthority records that claimed tabs and progress bars for the live Crafting and Construction panels. Their player-facing audit definitions now describe the implemented manual crafting forecast, explicit separation from queued machine jobs, full-catalog construction category paging, and live placement/material feedback. Construction retains its real search/filter capability through category filtering.

Expanded Milestone02MenuUniformityReadabilitySmoke to guard the updated contracts and reject future fictional capability claims.

Verification: Java 17 full-source compile, focused menu uniformity smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Body Condition and Combat Readiness Slice

Advanced Phase 14.1 by replacing the Character panel's raw per-body-part Endurance and Agility dump with BodyConditionReadabilityAuthority. The live Body / Loadout pane now derives overall condition, combat readiness, trauma, bleeding/infection urgency, stamina and supply impairment, the worst affected body regions, clothing protection, and equipped hands from current runtime state.

The summary uses decision-oriented condition bands rather than raw body-stat leakage. Added Milestone02BodyConditionReadabilitySmoke for healthy and injured states and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused body-condition readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Medical Treatment Readiness Slice

Advanced Phase 14.2 with MedicalTreatmentReadabilityAuthority in the Character Body / Loadout pane. Current injuries now produce carried-treatment guidance based on the actual ordinary Use path: medkits, bandages, splints, and antiseptics are recognized, while unavailable treatment directs the player toward supplies or a clinic.

The panel explicitly states that cataloged named drugs and stimulants do not yet receive specialized runtime effects through ordinary Use, preventing descriptive catalog content from being mistaken for implemented simulation. Added Milestone02MedicalTreatmentReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused medical-treatment readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Entity Identity and Social Context Slice

Advanced Phase 15.1 and 15.2 with EntityIdentityReadabilityAuthority. Ordinary NPC interaction and conversation portrait panes now expose readable name, role, faction, rank title and authority scope, current activity, relationship to the recorded home/duty position, condition approximation, visible equipment threat, age, and known provenance while explicitly preserving uncertainty about private motives.

Removed raw NPC HP from ordinary interaction and progressive examination text. Added Milestone02EntityIdentityReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused entity-identity readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Personnel Roster Status Hygiene Slice

Advanced Phase 15.3 by expanding FactionRosterReadabilityAuthority with compact assignment, skill-band, availability, and loyalty-warning language. The roster now distinguishes general-labor availability from assigned members while preserving the existing read-only command boundary.

The panel explicitly states that recruit rank and current world location are not present in the recruit save record, avoiding fabricated command tier or workplace claims. Expanded Milestone02FactionRosterReadabilitySmoke to guard the new status and limitation wording.

Verification: Java 17 full-source compile, focused faction-roster readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Player Expansion Heat Readability Slice

Advanced Phase 17.4 with ExpansionHeatReadabilityAuthority in the Auspex Signals pane. The readout now translates suspicion and gang attention into readable bands, counts open commerce, defenses, production assets, laboratories/clinics, and restricted or military assets, totals recorded per-business heat, and explains likely attention drivers and available relief paths.

The UI explicitly states the current simulation boundary: asset exposure is advisory and is not yet automatically aggregated into the global suspicion or gang-attention meters. Added Milestone02ExpansionHeatReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused expansion-heat readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Interaction Approach Planning Slice

Advanced Phase 17.6 with InteractionApproachAuthority. NPC, animal, vendor, machine, container-style object, and base-object interaction surfaces can now request an Approach plan. The authority evaluates the four adjacent tiles, selects the shortest reachable destination within the current movement mode, and opens the existing manual movement ghost/path for explicit confirmation.

Approach never teleports or auto-commits movement, and unavailable adjacency reports a clear range/path failure. Added Milestone02InteractionApproachSmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused interaction-approach smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Contract Objective and Evidence Readability Slice

Advanced Phase 17.1 with ContractObjectiveReadabilityAuthority in the Map / Objectives pane. Active faction contracts now show a sanitized objective, readable route location and certainty, required proof or delivery item, whether that item is carried, stored, or missing, and the script/standing reward.

Internal contract IDs and target entity IDs remain hidden, and unconfirmed local identities or exact target positions are explicitly withheld until the route records them. Added Milestone02ContractObjectiveReadabilitySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused contract-objective readability smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Shared Transfer Workflow Grammar Slice

Advanced Phase 17.5 with TransferWorkflowReadabilityAuthority. Inventory/base storage, container transfer, and vendor purchase previews now share source, destination, one-item quantity, permission, destination capacity, mission/evidence protection, reversibility, and confirmation/cancel language while retaining each surface's real execution rules.

Purchases are explicitly marked as not automatically reversible, while storage/container moves identify the paired return action where access remains available. Added Milestone02TransferWorkflowConsistencySmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused transfer-workflow consistency smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Live Context Binding Prompt Slice

Advanced Phase 17.7 by replacing the gameplay panel header's hardcoded Esc/Tab/Enter text with live context prompts from ControlReferenceTextSubsystem. Inventory, character, trade, container, conversation, object interaction, Look, Interact, combat, construction, crafting, Auspex, scavenge, map, Infopedia, pause, and movement planning now select appropriate named actions.

Prompts consume the current remappable keyboard profile. Controller views show their selected controller-family text while retaining current keyboard recovery bindings, and long prompts are fitted to the panel header. Added Milestone02LivePanelPromptSmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused live-panel prompt smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Exposed Mechanics Infopedia Coverage Slice

Advanced Phase 18 Infopedia coverage by registering player-facing mechanic references for body condition, medical treatment readiness, production forecasts, construction blueprints, expansion heat, interaction approach planning, contract objectives and evidence, and shared transfer workflows. Each entry explains the implemented behavior, states important non-automatic or unsupported boundaries, names its validation guard, and links to related mechanics through the existing navigable Related action.

Expanded Milestone02InfopediaMechanicsReadabilitySmoke to cover every new row, searchable detail text, cross-system health filtering, leak prevention, and the corresponding focused smoke guard names.

Verification: Java 17 full-source compile, expanded Infopedia mechanics smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Gameplay-to-Infopedia Mechanic Hot-Link Slice

Advanced Phase 18.1 with InfopediaHotLinkAuthority. Gameplay panels can now resolve a stable mechanic key, open the existing game-owned InfoPedia mechanics tab, select the exact reference row, and reset list/detail scrolling without duplicating encyclopedia UI.

Added direct Body Condition, Contract Objectives and Evidence, and Expansion Heat reference buttons to the Character, Map, and Auspex panels. Added Milestone02InfopediaHotLinkSmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused Infopedia hot-link smoke, expanded Infopedia mechanics smoke, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Structured Menu Definition Audit Slice

Advanced Phase 18.4 with MenuDefinitionAuditAuthority. Every UniversalWindowAuthority registration now produces a player-readable audit definition covering menu ID/title, owning authority, purpose, data source, panes, actions, back behavior, declared capabilities, world-input behavior, text containment, and domain-owned permission/failure boundaries.

The Menu Uniformity Infopedia reference now contains the structured audit, while the tactical slate and pause session surface show a compact readiness summary and provide a direct Menu Audit hot-link. Added Milestone02MenuDefinitionAuditSmoke and wired it into the Gate 3 suite.

Verification: Java 17 full-source compile, focused structured-menu audit smoke, expanded menu/Infopedia smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Movement Planning Definition Audit Slice

Advanced Phase 18.5 with MovementPlanningDefinitionAuditAuthority. The Movement Planning Infopedia entry now audits movement modes, ghost visuals, placement inputs, live bindings, valid and invalid target rules, hazard limitations, interaction adjacency, controller verification status, overlay priority, and reset/persistence expectations.

The audit deliberately records that hazard exposure does not yet alter route acceptance, end-to-end gamepad ghost nudging still needs live verification, and save/load focus reset needs a dedicated persistence audit. Added a tactical-slate Move Audit hot-link plus Milestone02MovementPlanningDefinitionAuditSmoke in the Gate 3 suite.

Verification: Java 17 full-source compile, focused movement-definition audit smoke, expanded movement/Infopedia smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Accurate Current-versus-Default Input Audit Slice

Repaired Phase 18.6 input audit accuracy. Input rows no longer label a dynamically rebound key as the default or claim every profile is still using default mappings. The shared prompt subsystem now exposes explicit baseline keyboard/controller prompts separately from live current-profile prompts, and the audit displays both values for each action.

The tactical slate and pause session surface now include an Input Audit readiness summary and direct hot-link. Expanded Milestone02InputRebindingAuditSmoke to rebind Confirm, verify the live key changes while the baseline remains stable, and restore defaults before the remaining Gate 3 suite runs.

Verification: Java 17 full-source compile, focused input audit/current-binding/profile smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Movement Route Hazard Warning Slice

Advanced the Phase 19 movement-readiness checklist by connecting movement previews to the existing environmental hazard records. Manual, directional, Approach, and mouse preview routes now inspect crossed tiles, report the number of hazardous tiles and highest visible concern, and render valid hazardous paths in amber rather than presenting them as ordinary safe routes or invalid red routes.

Hazard warnings remain advisory: they do not yet block movement or calculate exposure cost. Cancellation, execution, recovery, and main-menu reset clear hazard-preview state with the route. Expanded movement readability and definition-audit smokes to cover severity selection, warning language, truthful non-blocking behavior, and the updated audit contract.

Verification: Java 17 full-source compile, focused movement readability/definition smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Hazardous Quick-Movement Confirmation Slice

Closed a Phase 19 quick-versus-planned movement inconsistency. Walk and Sneak inputs still execute ordinary safe steps immediately, but a step onto a recorded environmental hazard now opens the same one-tile amber movement ghost and warning used by deliberate planning. The player must confirm the risky step or can cancel it without moving.

Run and Sprint already route through multi-tile planning, so all movement modes now receive pre-commit warning when their known route crosses a recorded hazard. Expanded movement readability and definition-audit guards for direct-step hazard detection and safe-tile immediacy.

Verification: Java 17 full-source compile, focused movement readability/definition smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Movement Planning Focus Reset Slice

Closed the movement planning save/load focus-reset gap with MovementPlanningFocusResetAuthority. Entering save/load, successfully loading another game state, or returning to the main menu now clears manual ghosts, mouse previews, route lists, hazard flags, look-cursor ownership, and stale preview targets through one shared bridge.

Updated the movement definition audit to claim only the now-wired reset paths. Added Milestone02MovementPlanningFocusResetSmoke and wired it into Gate 3 alongside the existing movement and persistence guards.

Verification: Java 17 full-source compile, focused movement focus-reset/definition/persistence smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Faction Personnel Authority and Staffing Readability Slice

Advanced the Phase 19 faction-management checklist by repairing the Character roster's overly broad claim that all reassignment was unavailable. The roster now distinguishes player command membership from the separate NPC worker track, shows actual recorded machine/defense station assignments, and points to the existing validated station-management staffing route.

The surface also states the current privacy and authority boundary: the compact recruit record has no rank, current location, or personal item ledger, so direct duty editing, member inventory transfer, and member equipment commands remain unavailable. Added a Faction Personnel and Staffing Infopedia entry, Character-panel hot-link, and expanded roster/Infopedia smoke coverage.

Verification: Java 17 full-source compile, focused faction-roster and Infopedia mechanics smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Manual Craft and Machine Queue Context Slice

Advanced the Phase 19 production-management checklist by extending the Crafting detail forecast with the selected machine's recorded worker, assigned recipe, queue remaining/target count, and shared operation queue totals. These are shown as separate machine state rather than being falsely attributed to the immediate Craft command.

The panel now states that manual Craft remains player-operated, currently has no separate power/fuel gate beyond its existing readiness checks, routes output to carried inventory, and does not control queued-machine output routing. Expanded production and Infopedia guard coverage for staffing, utility, queue, and routing boundaries.

Verification: Java 17 full-source compile, focused production readability and Infopedia mechanics smokes, and the complete Gate 3 player-facing smoke suite.

## Milestone 02 - Vendor Context and Protected-Sale Enforcement Slice

Advanced the Phase 19 trade checklist with readable market affiliation, faction standing band, accessible-stock count, shipment/scarcity context, and an explicit service boundary stating that the trade panel supports item buying and selling rather than unrelated repairs, treatment, lodging, banking, or training.

Repaired a transaction safety mismatch: mission, evidence, and intelligence items previously displayed a protection warning but could still be sold. The sale preview and execution path now share one protection rule and refuse ordinary vendor sale until a dedicated hand-in or explicit release flow exists. Expanded trade and transfer Infopedia smoke coverage.

Verification: Java 17 full-source compile, focused trade/transfer readability smokes, and the complete Gate 3 player-facing smoke suite.

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

## Milestone 02 - Inventory Mixed-Quality Stack Clarity Slice

Continued inventory readability by distinguishing duplicate units of the exact selected quality from related units in the same item family. The live Inventory / Storage detail pane now reports the exact-selection count, total related-family count, number of represented quality grades, and explicitly states that Use, Equip, Store, and Take affect one selected unit at a time.

Expanded `Milestone02InventoryReadabilitySmoke` with a mixed Common/Fine weapon list so exact count, family count, quality-grade count, and one-unit action wording remain covered. Verification follows this entry through the local Java 17 compile and smoke path.

## Milestone 02 - Inventory Equipment Infopedia Route Slice

Added an `Inventory and Equipment` mechanic reference covering individual-unit rows, mixed-quality item families, equipment matching, condition boundaries, protected goods, and one-unit action scope. The live Inventory / Storage panel now exposes an `Item Info` hot-link to that entry, and Transfer Workflows links back to it for durable navigation between item and movement rules.

Expanded `Milestone02InfopediaMechanicsReadabilitySmoke` to require the new row and its inventory smoke guard. Verification follows through a fresh Java 17 compile, focused Infopedia and hot-link smokes, and the full Gate 3 player-facing suite.

## Milestone 03 - Production Quality Cap and Trace Slice

Started the first bounded Milestone 03 implementation by repairing `cappedProductionQuality(...)`, which previously returned only the selected machine quality and bypassed the central doctrine and recipe ceilings. Immediate manual crafting now resolves output quality through `QualityAuthorityApi` using known doctrine, recipe requirement, and machine quality, while material, facility, and worker quality remain explicit open hooks.

Added `ProductionQualityTraceAuthority` so the live crafting forecast reports expected quality, each active cap, the main limiting input, and the inactive quality-ledger boundary. Added `Milestone03ProductionQualityTraceSmoke` and wired it into the Gate 3 player-facing suite to cover recipe-limited and machine-limited production outcomes.

Verification: fresh Java 17 full-tree compile passed. `Milestone03ProductionQualityTraceSmoke`, `Milestone02ProductionReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` passed. Manual GUI launch was not run.

## Milestone 03 - Quality Provenance Persistence Slice

Extended item provenance so newly crafted units retain their resolved output quality, recipe knowledge source, producing machine quality, and active quality limiter. These fields survive provenance save encoding, decoding, and later transfers while older seven-field provenance records remain readable.

The live inventory detail pane now exposes the recorded production-quality context when it exists. Added `Milestone03QualityProvenanceSmoke` to cover production recording, persistence round-trip, transfer preservation, and legacy decode compatibility.

## Milestone 03 - Quality-Sensitive Production Outcome Forecast Slice

Corrected production defect estimates so they account for both faction manufacturing style and the resolved output quality's reliability and defect multipliers. Value and usable-charge estimates already consumed output quality; the live crafting forecast now exposes all three estimates together.

The forecast explicitly states that defect risk is comparative guidance and that immediate manual crafting does not yet create a separate defect-state record. Added `Milestone03ProductionOutcomeForecastSmoke` to verify that better quality increases estimated value and charges, reduces defect risk, and still preserves faction-specific manufacturing differences.

## Milestone 03 - Live Production Reference Route Slice

Retargeted the crafting panel's generic InfoPedia button into a direct `Production Info` hot-link. The Production Forecast reference now documents the live doctrine/recipe/machine quality cap, open material/facility/worker hooks, quality provenance persistence, value and charge estimates, quality-sensitive defect risk, and the current absence of instantiated defect-state records.

Expanded the Infopedia mechanic smoke so the durable production reference must retain both Milestone 02 crafting behavior and the new Milestone 03 outcome-forecast guard.

Verification: fresh Java 17 full-tree compile passed. Focused inventory, production, quality trace, provenance, outcome forecast, Infopedia mechanics, and Infopedia hot-link smokes passed across this iteration. `Gate3PlayerFacingTextSmokeSuite` passed after every completed slice. Manual GUI launch was not run.

## Milestone 03 - Machine Condition Production Risk Slice

Connected the existing absolute machine-integrity value to manual production. Broken machines at integrity zero now refuse crafting with a repair requirement; critical and worn machines remain usable but add visible defect-risk surcharges. Because the legacy object model has no maximum-integrity field, this slice uses explicit absolute bands instead of inventing percentage condition.

The crafting forecast now reports machine condition and its defect adjustment. Crafted-item provenance records the producing machine condition through save/load and transfers. Added `Milestone03MachineConditionProductionSmoke` and expanded the provenance smoke for this context.

## Milestone 03 - Owned Machine Field Repair Workflow Slice

Added a supported repair path to the live base-object interaction panel. A damaged owned machine can spend one machine part and one turn to restore up to two integrity, stopping at the serviceable threshold rather than fabricating an unknown original maximum-integrity value. The panel previews resource cost, projected integrity, missing-part refusal, and the no-repair-needed state before mutation.

Added `MachineRepairAuthority` and `Milestone03MachineRepairWorkflowSmoke` to keep repair cost, restoration amount, serviceable cap, and refusal wording explicit.

## Milestone 03 - Manual Production Operator Skill Slice

Connected each crafting recipe's named XP skill to the existing 4-11 core-stat scale for manual production forecasts. Recipe skills map onto Mechanics, Firearms, Melee, Charm, Endurance, or Intellect and produce transparent novice, practiced, skilled, or expert bands. These bands adjust comparative defect risk while leaving the doctrine/recipe/machine quality cap unchanged.

The live forecast shows the mapped stat, value, band, and risk adjustment. Newly crafted item provenance retains the operator skill and band through save/load and transfers. Added `Milestone03ProductionOperatorSkillSmoke` for mapping and risk behavior.

## Milestone 03 - Production Condition Skill Reference Consolidation Slice

Updated the live Production Forecast Infopedia reference to include broken-machine blocking, worn-machine defect risk, bounded owned-machine repair, and manual operator skill mapping. Expanded the mechanic-reference smoke so these rules remain reachable from the crafting panel's Production Info route.

Strengthened `Milestone03QualityProvenanceSmoke` to prove operator skill and band survive provenance encoding, decoding, and transfers alongside machine condition and quality-cap context.

Verification: fresh Java 17 full-tree compile passed. Machine condition, machine repair, operator skill, quality trace, quality provenance, production outcome, production readability, Infopedia mechanics, Infopedia hot-link, and full Gate 3 smokes passed. Manual GUI launch was not run.

## Milestone 03 - Named Input Material Quality Slice

Activated the material-quality cap for manual recipes that consume named item units. Forecasting now follows the actual legacy consumption order, selecting matching carried units before base-storage units, and uses the lowest quality among the units that will be consumed as the material ceiling. Recipes that consume only abstract supplies or machine parts leave the material hook open.

Moved quality-trace capture before input removal so forecast and execution inspect the same units. Crafted-item provenance now preserves the consumed material-quality cap. Added `Milestone03ProductionMaterialQualitySmoke` for carried-versus-storage selection, mixed-quality limiting, and abstract-input boundaries.

## Milestone 03 - Material Quality Reference and Persistence Guard Slice

Updated the live Production Forecast reference with named-material selection order, lowest-consumed-unit limiting, and the abstract supply/part boundary. Expanded the quality provenance smoke to prove an active material cap survives encoding, decoding, and transfers, and expanded the Infopedia mechanic smoke to require the material rule and guard.

## Milestone 03 - Compact Recipe Material Cap Alignment Slice

Aligned crafting recipe-list status rows with the same material-aware quality authority used by the detail forecast and final execution. The compact `cap` label can no longer advertise machine-only quality when a lower-grade named component will cap the actual result. Expanded the material-quality smoke to cover this live list-row path.

Verification: fresh Java 17 full-tree compile passed. Material quality, quality trace, provenance, operator skill, machine condition, machine repair, production outcome, production readability, Infopedia mechanics, Infopedia hot-link, and full Gate 3 smokes passed. Headless display detection emitted safe-default warnings only. Manual GUI launch was not run.

## Milestone 03 - Assigned Worker Quality Boundary Slice

Added `ProductionWorkerQualityAuthority` to translate recruit skill 1-4 into Common, Serviceable, Fine, or Masterwork potential worker tiers. The live crafting forecast now shows the assigned worker's potential while explicitly preserving the current ownership rule: immediate Craft is player-operated, so an assigned recruit neither caps nor improves that manual result.

The authority also exposes the future staffed-run cap without activating dormant automation. Added `Milestone03ProductionWorkerQualitySmoke` and updated the durable Production Forecast reference with this boundary.

## Milestone 03 - Machine-Installed Production Knowledge Slice

Added one append-only installed-doctrine slot to each base machine with backward-compatible save parsing. The crafting panel now offers `Teach Machine`: the player must currently know the selected recipe doctrine, and installing it replaces the machine's prior doctrine. A matching installed doctrine then keeps the recipe visible, satisfies its knowledge execution gate, and contributes the doctrine tier to quality tracing even if the player no longer knows it.

Added `ProductionKnowledgeSourceAuthority` and `Milestone03MachineKnowledgeSourceSmoke` for player-versus-machine source resolution, teaching refusal/success, recipe visibility, execution access, quality contribution, and save-line placement.

## Milestone 03 - Knowledge Provider Provenance Slice

Extended crafted-item provenance with the provider of recipe knowledge, distinguishing player knowledge, installed machine doctrine, or both from the doctrine name itself. The provider is captured from the same authority used by the execution gate, survives save/load and transfers through an append-only provenance field, and appears in live inventory quality context.

Corrected the machine-knowledge smoke fixture so its doctrine-bearing machine participates in required-machine discovery. Focused machine-knowledge and quality-provenance smokes passed after a fresh Java 17 full-tree compile.

## Milestone 03 - Production Batch and Defect Disposition Slice

Added `ProductionBatchAuthority` so each immediate Craft action receives one shared batch identity and one inspection roll using the existing quality-, faction-, machine-condition-, and operator-sensitive defect forecast. Every output unit from that action records the same batch ID and either `passed inspection` or `defect flagged` in provenance; both fields survive save/load and transfers.

The live forecast and Production Forecast Infopedia entry now explain that batch disposition is instantiated, while flagged defects remain traceability data and do not yet reduce item statistics. Added `Milestone03ProductionBatchProvenanceSmoke`, expanded provenance and readability guards, and wired the new smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed. Machine knowledge, batch provenance, quality provenance, production readability, and the full Gate 3 player-facing smoke suite passed. Headless display detection emitted safe-default warnings only. Manual GUI launch was not run.

## Options UI - Standard Swing Editors and Multiplayer Privacy Slice

Set milestone feature work aside for an emergency options-menu modernization pass. Replaced every Text/UI, audio-volume, world-zoom, Doom-FOV, JVM-heap, screen-shake, and selected-color plus/minus command cluster with standard Java 17 Swing editors. Bounded numeric settings now open `JSlider`; custom colors open `JColorChooser`; On/Off settings route through grouped `JRadioButton` choices while preserving their existing save, audio, rendering, and restart side effects.

Added `SwingOptionsEditorAuthority` as the reusable bridge between the custom-painted menu shell and native Swing editors. Added `ROOT_docs/JAVA17_SWING_OPTIONS_UI_REFERENCE.md` with the Java 17 Swing package reference and recommended component ownership for named modes, exact numbers, paths, tabs, and validated text.

Hardened multiplayer privacy with `MultiplayerPrivacyAuthority`. The live custom multiplayer screen now redacts direct addresses, recent servers, favorites, statuses, and local-host binding details. Direct connection entry uses a guarded `JPasswordField` with deliberate temporary reveal. The standalone `MultiplayerJoinPanel` now also uses `JPasswordField` for server addresses and grouped On/Off `JRadioButton`s for stream-safe display instead of a checkbox.

Added `OptionsSwingComponentSmoke` for Java 17 component availability, guarded address entry, IPv4/IPv6 masking, and recent-server display privacy, and wired it into Gate 3.

Verification: fresh Java 17 full-tree compile passed. `OptionsSwingComponentSmoke` and the complete `Gate3PlayerFacingTextSmokeSuite` passed. Headless display detection emitted safe-default warnings only. Manual visual inspection of native dialogs was not run.

## Milestone 03 - Claimed Production Facility Quality Slice

Activated the facility-quality cap for immediate manual crafting without inventing a separate facility save ledger. `ProductionFacilityQualityAuthority` evaluates the selected machine's claimed production room and counts serviceable production stations in that room. One station supports Common output, two or three support Serviceable, four or five support Fine, and six or more support Masterwork; broken stations do not contribute. Unclaimed work areas leave the facility hook open.

The central production-quality trace now applies and names the facility cap alongside doctrine, recipe, machine, and named-material limits. Live recipe rows, detailed forecasts, and final execution share that authority. Crafted-item provenance records facility quality through append-only save encoding, decoding, and transfers. The Production Forecast reference documents the rule.

Added `Milestone03ProductionFacilityQualitySmoke`, expanded quality trace and provenance coverage, and wired the new smoke into Gate 3. Verification: fresh Java 17 full-tree compile passed; focused facility, quality-trace, and provenance smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Manual GUI inspection was not run.

## Milestone 03 - Defective Batch Market Appraisal Slice

Converted recorded production defects from provenance-only warnings into a bounded market consequence. `ProductionDefectAppraisalAuthority` applies a visible 40% ordinary-trader resale penalty to units whose preserved batch disposition is `defect flagged`; passed batches retain ordinary pricing. Inventory inspection explains the consequence, trade preview shows the adjusted value before confirmation, and final sale execution uses the same appraisal authority.

The slice deliberately does not invent hidden combat, durability, charge, or use penalties before a per-item condition owner exists. Updated batch feedback, Production Forecast guidance, and Infopedia coverage to state that boundary. Added `Milestone03ProductionDefectAppraisalSmoke`, updated the existing batch/readability guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused appraisal, batch provenance, production readability, and trade readability smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Manual GUI inspection was not run.

## Milestone 03 - Equipped Production Tool Quality Slice

Activated the manual-production tool-quality cap through `ProductionToolQualityAuthority`. Only a deliberately equipped fabrication or repair tool participates; unrelated carried items and ordinary weapons do not silently affect production. If both hands hold qualifying tools, the better-quality tool governs. Empty hands leave the hook open under the selected machine's integrated tooling.

The central production-quality trace now names equipped tool quality alongside doctrine, recipe, machine, material, and facility caps. Compact recipe rows, detailed forecasts, final execution, and append-only item provenance share the same result. The Production Forecast reference documents the equipped-tool rule and its no-silent-inventory-cap boundary.

Added `Milestone03ProductionToolQualitySmoke`, expanded quality provenance and Infopedia guards, and wired the smoke into Gate 3. Verification: fresh Java 17 full-tree compile passed; focused tool-quality, quality-trace, provenance, and production-readability smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Manual GUI inspection was not run.

## Milestone 02 - Character Paper Doll and Equipment Slot Restoration

Restored the player-character paper doll and exact limb hit-point/status display to the live character panel. The repair reuses `Candidate.body`, `BodyPart`, `BodyConditionReadabilityAuthority`, and existing equipment state rather than creating duplicate health data. The character panel now expands for the body display, colors each tracked region by condition, prints current/max hit points, and lists every tracked limb with its readable state.

Replaced the two blind hand-only unequip buttons with selectable Left Hand, Right Hand, and Body Protection slots. The selected equipped item receives a detail/icon surface and can be unequipped explicitly; hand slots route through the existing unequip implementation, while body protection is returned to carried inventory before the clothing slot is cleared. Added `CharacterPaperDollAuthority` and `Milestone02CharacterPaperDollSmoke`, with the smoke wired into Gate 3.

Verification: Java 17 UTF-8 full-source compile, focused paper-doll smoke, and complete Gate 3 smoke suite in GitHub Actions. Manual GUI review remains required for supported window sizes.

## Milestone 02 - Full Character Equipment and Medical Body Tabs

Expanded the restored character panel into Overview, Equipment, and Medical tabs. The Equipment tab now exposes Headgear, Underclothes, Clothes/Body, Gloves, Boots, Backpack, two Ring slots, two Accessory slots, and both Hand slots. It includes a live paper doll, selectable slots, carried-item navigation, compatibility feedback, selected-item icon/detail presentation, equip replacement, and explicit unequip-to-inventory behavior. Backpacks now contribute a visible additive carry-capacity bonus while preserving the existing Strength/world-settings capacity authority.

Added `CharacterEquipmentAndMedicalAuthority` as the narrow loadout and body-modification contract rather than embedding another subsystem in the legacy panel bridge. Existing hand and clothing fields remain authoritative for their established mechanics; new wearable slots live in an enum-keyed map. The Medical tab mirrors every tracked `Candidate.body` region and reserves independent Mutation, Modification, and Cybernetic records for each region. It intentionally does not invent surgery, rejection, power, maintenance, mutation, or wireless cybernetic mechanics; future systems can bind through `installCharacterMedicalRecord(...)` and `MedicalSlotKey.storageKey()` without redesigning the screen. Cybernetic presentation assumes isolated direct-interface hardware rather than wireless control.

Added property persistence hooks for the new wearable and medical maps where the existing `Persistence.writeCore/readCore` authority is available. Added focused authority and live GamePanel integration smokes, including equip/unequip transfer, backpack capacity, anatomical region mapping, and future medical-record binding. Verification uses Java 17 UTF-8 full-source compilation and the complete Gate 3 suite.

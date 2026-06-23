# Development History

This is the fresh active milestone-development history for The Mechanist after the prior active ledger was archived.

The previous active milestone ledger is archived at:

`ROOT_docs/archive/DEVELOPMENT_HISTORY_MILESTONE_LEDGER_ARCHIVE_2026-06-05.md`

Earlier pre-milestone development remains archived at:

`ROOT_docs/archive/DEVELOPMENT_HISTORY_PRE_MILESTONE_DEVELOPMENT.md`

Use this file for new completed work from this reset onward. Keep entries concise: record what changed, why it matters for the milestone sequence, and what verification was run. Do not restate the full roadmap here; roadmap authority remains in `MASTER_DEVELOPMENT_PLAN.md`, with detailed milestone targets indexed by `ROOT_docs/MILESTONE_INDEX.md`.

## Milestone 02 - Semantic Asset Audit Dev Room and Manual Tile Cycling

Added a curated asset smoke-test room to Tools / Zone Audit. The audit cursor can cycle backward or forward through compatible indexed semantic assets for the selected tile, immediately updating the rendered target and showing its asset ID, type, name, source, and candidate count.

Manual choices remain transient audit overrides, so visual diagnosis cannot silently rewrite production registry or world-generation data. The semantic runtime registry smoke now verifies that the room builds and that a floor tile can cycle to a different valid indexed asset.

Verification: fresh Java 17 full-tree compile passed; the focused semantic runtime registry smoke and complete Gate 3 player-facing suite passed. The package-seed gate staged 3,000 Java 17-compatible classfiles, and the function and Mermaid code maps were refreshed. Manual GUI inspection was not run.

## Milestone 02 - Streetlight Infrastructure Semantic Promotion Slice

Promoted the existing `RoadInfrastructureTileRules` meaning for `Road_infrastructure` row 5 columns 1-2 into the deep asset descriptor source. Those visually verified lamp-post cells now carry explicit streetlight, infrastructure, fixture, and sidewalk metadata instead of inheriting only the broad road-atlas category.

Runtime compiled-index classification now honors explicit fixture content types before broad source-group tags. The active semantic resolver therefore selects streetlight fixture art and rejects system inventory, item-icon, and UI-icon substitutions. Expanded the runtime registry smoke to guard that contract.

Verification: fresh Java 17 full-tree compile passed. Focused runtime registry, semantic resolver, migration coverage, and binding-doctrine smokes passed; the complete Gate 3 player-facing smoke suite passed. The active registry resolves `STREETLIGHT_FIXTURE` to `FIX-0151` and reports infrastructure coverage complete (`1/1`), raising total semantic render intent coverage to `17/44`. The package-seed gate passed with 2,998 Java 17-compatible classfiles, and the function and Mermaid code maps were refreshed. Manual GUI inspection was not run.

## Milestone 02 - Open and Closed Door Semantic Migration Slice

Preserved the `Doors-C` and `Doors-O` atlas state when compiled content-index rows become runtime asset metadata. Door alias scoring and `SemanticRenderAssetResolver` ranking now prefer dedicated fixture assets over mixed bulkhead wall sheets, so the live renderer receives distinct closed and open semantic assets for standard doors and archways.

Expanded the runtime registry smoke to require `FIXTURE`-typed closed and open door assets, reject opposite-state metadata, prove both states resolve to different IDs, and require the tile alias and general semantic resolver authorities to agree. Locked, security, vent-panel, and double-door aliases remain explicit missing-art cases until their specialized source art is semantically identified.

Verification: fresh Java 17 full-tree compile passed. Focused runtime registry, semantic resolver, migration coverage, and binding-doctrine smokes passed; the complete Gate 3 player-facing smoke suite passed. Active-registry migration coverage reports both door intents available (`2/2`) and `16/44` total semantic render intents available. The package-seed gate passed with 2,998 Java 17-compatible classfiles, and the function and Mermaid code maps were refreshed. Manual GUI inspection was not run.

## Milestone 02 - Themed Semantic Asset Registry Verification Slice

Corrected compiled-asset classification so structural atlas identity takes precedence over theme words such as `noble`. Noble floors, corridors, and walls now remain floor, corridor, and wall assets instead of being misclassified as portraits, allowing the live semantic tile authority to resolve them from the active registry.

Expanded `TileSemanticRuntimeRegistrySmoke` to verify generic, industrial, sewer, noble, road, and door representatives against their expected runtime asset type and theme while rejecting sewer/noble cross-theme substitution. Wired the smoke into the aggregate Gate 3 suite.

Verification: fresh Java 17 full-tree compile passed. Focused semantic binding, resolver, migration coverage, registry extension, and runtime registry smokes passed; the complete Gate 3 player-facing smoke suite passed. Runtime alias resolution increased from 98 of 210 aliases to 120 of 210. The package-seed gate passed with 2,998 Java 17-compatible classfiles, and the function and Mermaid code maps were refreshed. Manual GUI inspection was not run.

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

## Milestone 03 - Faction Production Mutation Traceability Slice

Made the existing faction manufacturing consequences explicit through `ProductionFactionMutationAuthority`. Live crafting forecasts now identify the faction profile and output prefix, then show the effective value, charge, and defect-pressure multipliers already used by `ProductionRecipe`. This does not add a second faction-stat model; it explains the established `FactionManufacturingProfile` math in player-facing terms.

Crafted-item provenance now preserves the faction production mutation through append-only field 23, legacy decoding, and inventory/storage transfers. The Production Forecast reference documents the rule. Added `Milestone03ProductionFactionMutationSmoke`, expanded provenance and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused faction-mutation, quality-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 2,988 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Repository manifest regeneration was attempted twice but exceeded two-minute and five-minute command ceilings, so that generated ledger still requires a successful refresh.

## Milestone 03 - Claimed Facility Knowledge Sharing Slice

Activated facility-provided production knowledge without creating a parallel room save ledger. `ProductionFacilityKnowledgeAuthority` allows another serviceable production station in the selected machine's claimed room to supply the required installed doctrine. The selected machine, broken stations, stations outside the room, and unclaimed work areas cannot provide this shared facility source.

`ProductionKnowledgeSourceAuthority` now reports player, selected-machine, and claimed-facility sources through one execution-shared result. Facility doctrine can reveal a recipe, satisfy the manual Craft execution gate, contribute its knowledge-quality tier, and flow into the existing knowledge-provider provenance field. Added `Milestone03ProductionFacilityKnowledgeSmoke`, expanded the Production Forecast reference and Infopedia guard, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused facility-knowledge, machine-knowledge, quality-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 2,994 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `1006F4A9E1B3E8166BD0218F3D88261AFBBB53554722D812B7395489261105E8`. The repository manifest remains pending after the prior generator timeouts.

## Milestone 03 - Manual Operator Quality Cap Slice

Activated manual operator skill as an execution-shared production-quality cap. `ProductionOperatorSkillAuthority` now maps the recipe's existing core-stat resolution into visible quality support: novice operators support Common output, practiced operators Serviceable, skilled operators Fine, and expert operators Masterwork. The existing defect-risk adjustments remain active alongside the new cap.

`ProductionQualityTraceAuthority` now carries and names the operator tier with doctrine, recipe, machine, material, facility, and equipped-tool limits. Live recipe forecasts, compact status quality, and final Craft execution resolve the operator before output creation, while provenance continues to preserve the operator skill and band. Queued assigned-worker behavior remains separate and dormant until its own execution owner is implemented.

Expanded `Milestone03ProductionOperatorSkillSmoke`, `Milestone03ProductionQualityTraceSmoke`, batch/provenance fixtures, Production Forecast guidance, and the Infopedia guard. Verification: fresh Java 17 full-tree compile passed; focused operator-skill, quality-trace, batch, provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 2,994 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `ABA7B73BEAA377B664B00D1B322E4AB781EDA5A82C5A981D57245964EB41EC82`. The repository manifest remains pending after the prior generator timeouts.

## Milestone 03 - Manual Production Fatigue Pressure Slice

Connected the existing body-readiness fatigue bands to immediate manual production through `ProductionFatiguePressureAuthority`. Ready operators receive no fatigue defect adjustment; slightly tired operators add two defect points; tired operators add five; and the established exhausted band at 75 fatigue blocks manual machinery operation until the player rests. The forecast shows current and projected fatigue before materials are consumed.

`ProductionBatchAuthority` now combines operator skill and live fatigue pressure into one batch defect risk. Final Craft execution resolves that pressure before the run, uses it for the shared batch inspection, and preserves it in item provenance through append-only field 24, legacy decoding, and transfers. Added `Milestone03ProductionFatiguePressureSmoke`, expanded Production Forecast guidance and its Infopedia guard, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused fatigue-pressure, batch, operator, provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,000 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `86F3B91FE38B31286FF9642E8271A38D7822955B1E0E5AEE70BFF0F63E1EEA7B`. The repository manifest remains pending after the prior generator timeouts.

## Milestone 03 - Producing Room and Facility Provenance Slice

Added `ProductionLocationAuthority` so immediate manual Craft resolves its producing room and facility before materials are consumed. When world room metadata exists, the forecast names the room profile and room ID; otherwise a selected machine inside the claimed base retains the claimed production-room identity. Work outside that boundary is recorded as an unclaimed world workspace rather than inheriting the player's base identity.

Crafted-item provenance now preserves producing room and producing facility separately through append-only fields 25 and 26, legacy decoding, and inventory/storage transfers. This advances the provenance ledger without inventing a blueprint ownership or facility-save subsystem. Added `Milestone03ProductionLocationProvenanceSmoke`, expanded Production Forecast guidance and its Infopedia guard, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused production-location, fatigue-pressure, quality-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,006 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `B4A1A8ADF3669A1787E22257FA50C53D2AC97634B35C09A16B37D7EC220E41BD`. The repository manifest remains pending after the prior generator timeouts.

## Milestone 03 - Producing Machine Identity Provenance Slice

Added `ProductionMachineIdentityAuthority` so immediate manual Craft identifies the exact selected station by its player-facing name, machine role, and coordinates. Forecasts now show that station identity alongside room and facility origin, allowing two same-quality machines to remain distinguishable without inventing a new machine-ID save subsystem.

Crafted-item provenance preserves the producing machine through append-only field 27, legacy decoding, and inventory/storage transfers. Added `Milestone03ProductionMachineProvenanceSmoke`, expanded quality-provenance and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused producing-machine, quality-provenance, production-location, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,012 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `E3FF92E11259C44E9012383E49EB8A3000FAEC2545D777A4C5EEC19110EEED7F`.

## Milestone 03 - Producing Operator Identity Provenance Slice

Added `ProductionOperatorIdentityAuthority` so immediate manual Craft forecasts and provenance identify the character who actually performed the run. The operator identity is separate from the existing skill and skill-band records, allowing later contracts, investigations, and item inspection to distinguish who made an item from how capable they were.

Crafted-item provenance preserves the producing operator through append-only field 28, legacy decoding, and inventory/storage transfers. Assigned workers and supervisors remain explicitly outside this manual-Craft slice until a queued staffed-production owner executes their work. Added `Milestone03ProductionOperatorProvenanceSmoke`, expanded quality-provenance and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused operator-provenance, machine-provenance, quality-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,018 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `2491FA6307BCE4D09BDDE44E8A0BF05DDFCA26A71839493B89A7AB8B78A410D0`.

## Milestone 03 - Manual Craft Operation History Slice

Connected successful immediate manual Craft actions to the existing conservative `ProductionQueueRecordBridge`. After the live Craft path has consumed inputs, created outputs, applied wear and fatigue, awarded experience, and advanced through its full turn cost, it now records one completed operation in shared `MachineOperationQueue` history with the operator, producing station, recipe, output count, duration, and final completion turn.

This does not transfer outcome authority to the queue. The operation history remains audit and status metadata only, preventing duplicate consumption or output creation while making completed production visible to shared operation diagnostics and persistence. Added `Milestone03ManualProductionOperationRecordSmoke`, expanded the Production Forecast reference, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused manual-operation-record, operator-provenance, production-readability, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,020 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `A073968EF7DFD95047CE794C957FCF5C50BB51AC64D92B95A4C05793B1AE913C`.

## Milestone 03 - Staffed Generated Production Execution Slice

Added `StaffedProductionExecutionAuthority` as the first bounded execution owner for queued staffed generated production. A machine with an existing generated assignment, assigned worker, claimed-room authorization, required knowledge, ready concrete inputs, and remaining queue count can complete one staffed run. The run consumes inputs through the existing production-container route, places output into base storage, preserves item provenance, applies bounded machine wear, decrements the machine queue, and records the completed operation through `ProductionQueueRecordBridge`.

This keeps outcome ownership narrow: it executes one validated run and does not create an open-ended background simulator or a second inventory system. Machine-operation status now reports whether the selected staffed assignment is ready or blocked. Added `Milestone03StaffedProductionExecutionSmoke`, expanded the Production Forecast reference, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused staffed-execution and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,026 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `087FA9F65D36B91D063A5626E0FF2A9A50B15C2AB1B1D11BB7D03A46A98D9C13`.

## Milestone 03 - Production Workforce Mode Provenance Slice

Added `ProductionWorkforceModeAuthority` and append-only provenance field 29 so produced items distinguish immediate manual Craft from staffed queued production. Manual Craft records the immediate operator mode by default; staffed generated-production output overrides the same field with the assigned-worker staffed mode.

The field survives save/load decoding and inventory/storage transfer, giving later contracts, investigations, and inspection text a stable workforce-mode hook without inventing supervisor mechanics. Added `Milestone03ProductionWorkforceModeProvenanceSmoke`, expanded quality-provenance, staffed-execution, and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused workforce-mode, staffed-execution, quality-provenance, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,030 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `BD5075BBC0D482ECF551E8BD1E7F209E5FADFA1374D5D1E5EF430573028D2E89`.

## Milestone 03 - Generated Production Legal Status Provenance Slice

Added `ProductionLegalStatusAuthority` and append-only provenance field 30 so staffed generated-production output preserves the variant law/status classification already used by production access rules. Lawful issue, restricted stock, gray-market, black-market, contraband, profaned, and hostile-identity labels now survive item inspection after save/load and transfer.

This is deliberately provenance only: it does not invent law-enforcement, seizure, corruption, or faction-reputation consequences before those owners exist. Staffed generated production writes the field from `FactionRecipeVariant.lawStatus`; manual Craft leaves it absent unless a future manual recipe law source is implemented. Added `Milestone03ProductionLegalStatusProvenanceSmoke`, expanded staffed-execution and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused legal-status, staffed-execution, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed after one transient master-map write retry. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,034 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `BB6B6BA3D79F3F68B25FD22DA092D2A5603AFAA6A1A287B2862B96BC0337D370`.

## Milestone 03 - Generated Production Source Provenance Slice

Added `ProductionSourceProvenanceAuthority` and append-only provenance field 31 so staffed generated-production output preserves the generated recipe source, base note, and variant note that produced the item. This gives later inspections and investigations a stable source hook without claiming blueprint ownership or supervisor authorship before those owners exist.

Staffed generated production writes this field from `FactionRecipeVariant.base.source`, `base.note`, and `productionNote`; manual Craft leaves it absent unless a future manual recipe-source owner is implemented. Added `Milestone03ProductionSourceProvenanceSmoke`, expanded staffed-execution and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused source-provenance, staffed-execution, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,038 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `CFB1EF8921A16DE07A1770D15E56C3502D37DE6CF0C493130776DC8EFE80B028`.

## Milestone 03 - Production Batch Issue Tags Slice

Added `ProductionBatchIssueAuthority` and append-only provenance field 32 so produced items can preserve Phase 9.3 batch issue tags. Manual production derives good or defective batch tags from the existing inspection disposition and adds evidence-backed contaminated, unstable, restricted, stolen-risk, counterfeit, or faction-certified tags from recipe and production metadata when those signals are present.

Staffed generated production records variant-level issue tags from law status, faction, source, and production notes when those signals are present, even though it does not yet roll a per-run manual batch inspection. This is provenance and readability only: the existing defect appraisal remains the only current gameplay consequence, so item statistics, law enforcement, contamination effects, recalls, and counterfeit enforcement remain reserved for later owners. Added `Milestone03ProductionBatchIssueTagsSmoke`, expanded batch, staffed-execution, quality-provenance, and Infopedia guards, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused batch-issue, batch-provenance, quality-provenance, staffed-execution, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,042 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `D63F0764ACD360172FA4CDD4B9FBE1883F77B66E4F07AEBDF5BF2732ED7BE154`.

## Milestone 03 - Production Repair History Provenance Slice

Added `MachineRepairHistoryAuthority` and append-only provenance field 33 so items produced on a repaired machine can preserve the field-repair note that existed on that machine at production time. The existing owned-machine repair action now records a compact machine repair history line with turn, actor, machine, integrity change, and machine-part cost.

`BaseObject` persistence now carries the machine repair note as an append-only save field, and produced-item provenance copies it through save/load and transfer. This intentionally records repaired-machine provenance only; item modification history remains empty until a real item-modification owner exists. Added `Milestone03ProductionRepairHistoryProvenanceSmoke`, expanded the Production Forecast reference, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused repair-history, machine-repair, quality-provenance, and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,046 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `40F483BF125B27ECAD60E7F0439566A85E5219C994D4F1ECED0432085E002C99`.

## Milestone 03 - Skill Tree Progression Readability Slice

Added `SkillTreeProgressionAuthority` as the first Phase 15 skill-tree ownership surface. It defines player-facing skill branches, capability-bearing nodes, XP costs, prerequisites, visible effects, and explicit skill-versus-knowledge boundaries. Initial branches include fabrication and repair, machine operation, trade and appraisal, investigation and examination, and leadership and faction command.

This is deliberately readable and auditable before it mutates saves: spending UI, permanent unlocked-skill persistence, trainer gating, and stat mutation remain future owners. Added `Milestone03SkillTreeProgressionReadabilitySmoke`, expanded the Infopedia with a Skill Progression entry, and wired the smoke into Gate 3.

Verification: fresh Java 17 full-tree compile passed; focused skill-tree progression and Infopedia smokes passed; the complete `Gate3PlayerFacingTextSmokeSuite` passed. Function and Mermaid maps refreshed. The maintained `PACKAGE_client` rebuilt successfully; the Java 17 scan passed for 3,054 classfiles at highest major version 61; the packaged client stayed alive for the full eight-second boot-smoke window. Final JAR SHA-256: `6F3D8911B07C1BE22E77E69AABBA03345DA736BAF70FD7A19CD549A1CC6B4BDA`.

## Milestone 03 - Skill Tree Spending Persistence Slice

Added the first durable skill-node spending path. `SkillTreeProgressionAuthority` now validates node lookup, XP cost, duplicate unlocks, and prerequisite nodes, then returns a bounded spend result. `GamePanel` now carries `unlockedSkillNodes` separately from `unlockedKnowledges`, and save/load persistence stores skill nodes in `run.skillNodes` without granting or consuming knowledge credits.

Added player-rank console routes `skill_status` and `skill_unlock <node id>` so XP can be spent on capability nodes before a full character-screen skill UI exists. This keeps Phase 15 moving while preserving the Knowledge Tree boundary: spending XP on a skill never teaches recipe doctrine. Added `Milestone03SkillTreeSpendingPersistenceSmoke`, expanded the Skill Progression Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeSpendingPersistenceSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar`, passed Java 17 classfile scan across 3058 classfiles, and passed the 8-second package boot smoke. Package jar SHA-256: `B00DC9076926C4053A8C12766117266353B591464DCFD5161920DC53450B722E`.

## Milestone 03 - Skill Tree Access Gate Slice

Added world-access validation to the skill-tree spending authority. Skill nodes can now declare access requirements for unlocked knowledge, faction standing, trainers, facilities, or equipment, and the player console route evaluates those gates from live game state before spending XP. Basic nodes remain spendable by XP and prerequisite alone; advanced nodes can now refuse with a player-facing missing-access reason instead of silently acting like detached menu upgrades.

Added gated example nodes for workshop fabrication, certified market appraisal, and supervisor authorization. The live game context derives facility access from owned base objects, faction access from faction standing, equipment access from carried/equipped/storage items, and knowledge access from the existing Knowledge Tree state. Trainer tokens remain explicit context entries until trainer NPC ownership exists. Added `Milestone03SkillTreeAccessGateSmoke`, expanded skill-tree readability and Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeAccessGateSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone03SkillTreeSpendingPersistenceSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar`, passed Java 17 classfile scan across 3062 classfiles, and passed the 8-second package boot smoke. Package jar SHA-256: `ED4BFDF379DF9D67F57AD03D57B01AEBF506D09A7E53BBABBE95559B041BEBC9`.

## Milestone 03 - Skill Tree Stat Gate and Effect Slice

Added stat requirements and bounded stat effects to the skill-tree authority. Skill nodes now carry separate stat requirement and stat effect fields, so a node can require a live character stat threshold before XP spending and can apply a clamped stat increase only after a successful unlock. The player console route now passes the active candidate's stats into skill spending and applies the returned stat effect in the same transaction that spends XP and records the unlocked node.

Added stat-gated machine-operation nodes, including Pressure Discipline, and upgraded the advanced workshop node to require both a fabrication facility and Mechanics 8. Stat effects mutate the existing `Candidate.stats` map, so persistence continues through the established `char.stats` save path instead of adding a parallel character progression ledger. Added `Milestone03SkillTreeStatGateSmoke`, expanded skill-tree readability and Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeStatGateSmoke`, `Milestone03SkillTreeAccessGateSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone03SkillTreeSpendingPersistenceSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar`, passed Java 17 classfile scan across 3064 classfiles, and passed the 8-second package boot smoke. Package jar SHA-256: `B3A2D3430D696E90A607970E48A97371E13E51F544EE2F453E4AA50147E4AD3B`.

## Boot Menu Studio Splash and Music Delay Slice

Added a nine-second boot-menu hold before main-menu music can start. The boot sequence now advertises placeholder stages for a studio intro and logo splash, keeps the visual boot/menu handoff under `BootMenuFlowAuthority`, and gates `MAIN_MENU` music through a timer-backed helper so skipping the boot screen does not start menu music before the nine-second mark.

Added `BootMenuMusicDelaySmoke` as a non-audio timing contract and wired it into Gate 3 for future aggregate runs. Also added a local-audio testing reminder to `STANDARDS_AND_PRACTICES.md`: prefer non-audio compile/focused smokes for boot timing, and do not run local audible boot/package smoke more than once unless explicitly requested.

Verification: one local Java 17 compile plus the non-audio `BootMenuMusicDelaySmoke` passed. No local GUI/package boot smoke was run for this slice.

## Milestone 03 - Skill Tree Mutual Specialization Slice

Added mutually exclusive specialization groups to the skill-tree authority. Skill nodes now carry an explicit exclusivity boundary, and XP spending refuses a node when a sibling specialization in the same group is already unlocked. The status surface includes the exclusive group so the player-facing console can show why related specializations are locked instead of hiding the rule behind a failed spend.

Added the first trade-appraisal specialization pair: Certified Market Appraisal for formal faction-facing certificates and Streetwise Appraisal for practical stolen-risk, counterfeit, and fence-market judgment. These share `trade-appraisal-specialization`, so the character can choose one lane after Batch Appraisal rather than stacking both appraisal identities. Added `Milestone03SkillTreeMutualExclusionSmoke`, expanded skill-tree readability and Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeMutualExclusionSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone03SkillTreeSpendingPersistenceSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed after one transient master-map write retry. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3068 classfiles. Package jar SHA-256: `2EA044737F67AD1639E5DD79C1D198EE7BACC5DB65D2DE6E519D6202579479B0`. Local GUI/package boot smoke was not run.

## Milestone 03 - Skill Tree Capability Hook Slice

Added queryable capability hooks to the skill-tree authority. Skill nodes now expose a durable capability key plus optional passive bonus and active ability labels, so future production, inspection, trade, machine, combat, or social systems can ask for trained capabilities directly instead of scraping player-facing prose.

Unlocked skill nodes now produce `capabilityKeys`, `passiveBonusLines`, `activeAbilityLines`, and a `hasCapability` helper. Initial hooks include `machine-readiness-preview` as an active ability, `production-limiter-readability:+1` as a passive production-inspection bonus, and `street-market-risk-appraisal` as an active street-trade appraisal ability. Added `Milestone03SkillTreeCapabilityHooksSmoke`, expanded skill-tree readability and Infopedia wording, and wired the smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeCapabilityHooksSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3070 classfiles. Package jar SHA-256: `CDD80D62DF85A9BD9A5F71AD9967CC6986F97A66AC06F7D34E6189DA84867B97`. Local GUI/package boot smoke was not run.

## Milestone 03 - Production Skill Capability Preview Slice

Connected unlocked skill-tree capability hooks to the manual production preview surface. `ProductionReadabilityAuthority` now adds bounded skill capability context from `GamePanel.unlockedSkillNodes`, including passive production-inspection hooks and active machine-operation abilities, so the player can see trained capabilities beside knowledge, operator, material, machine, and facility explanations.

This slice is intentionally read-only for production outcomes: the preview says the hooks are context only until a later consuming authority applies them to execution math. Added `Milestone03ProductionSkillCapabilityPreviewSmoke` and wired it into Gate 3 so skill capability hooks remain visible without silently changing output quality or defect calculations.

Verification: `javac --release 17`, `Milestone03ProductionSkillCapabilityPreviewSmoke`, `Milestone03SkillTreeCapabilityHooksSmoke`, `Milestone02ProductionReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3072 classfiles. Package jar SHA-256: `5FB25D19816F09C71ED811860E3C478551394CB1DD405AACB04D4716592E3630`. Local GUI/package boot smoke was not run.

## Milestone 03 - Trade Skill Appraisal Bridge Slice

Connected the skill-tree `Streetwise Appraisal` capability to visible trade appraisal text. Sale previews can now receive unlocked skill nodes and, when the street-appraisal specialization is trained, call out recorded defect, counterfeit, stolen-risk, restricted, or black-market provenance before the player confirms a sale.

The bridge is intentionally bounded: Streetwise Appraisal improves risk detection and buyer-facing explanation, but it does not override protected evidence hand-ins, invent law-enforcement consequences, or alter the existing defect resale math. The live trade panel now passes `unlockedSkillNodes` into sale preview, while the existing three-argument preview remains available for older tests and callers. Added `Milestone03TradeSkillAppraisalSmoke` and wired it into Gate 3.

Verification: `javac --release 17`, `Milestone03TradeSkillAppraisalSmoke`, `Milestone02TradeReadabilitySmoke`, `Milestone03ProductionDefectAppraisalSmoke`, `Milestone03SkillTreeCapabilityHooksSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3074 classfiles. Package jar SHA-256: `9F1960D7274E6C995C8DA70BC50127104FA2F4301034C1A996623696631643C4`. Local GUI/package boot smoke was not run.

## Milestone 03 - Certified Trade Appraisal Bridge Slice

Connected the formal side of trade appraisal to sale preview text. `Certified Market Appraisal` can now recognize faction-certified batch proof and recorded legal status as formal trade evidence, while untrained characters see that a certificate exists but cannot fully separate formal proof from ordinary item naming.

The bridge remains preview/readability only. It does not bypass faction access, protected hand-ins, buyer policy, law-enforcement ownership, or the existing defect resale appraisal. Added `Milestone03TradeCertifiedAppraisalSmoke` and wired it into Gate 3 beside the Streetwise Appraisal smoke, preserving the mutual-specialization split between institutional certification and informal street judgment.

Verification: `javac --release 17`, `Milestone03TradeCertifiedAppraisalSmoke`, `Milestone03TradeSkillAppraisalSmoke`, `Milestone02TradeReadabilitySmoke`, `Milestone03SkillTreeMutualExclusionSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3076 classfiles. Package jar SHA-256: `232BD430EC87C5E012EEE9F03B1E38CBDF60E90D1A1C46D509EEDE5C3D0EB4E0`. Local GUI/package boot smoke was not run.

## Milestone 03 - Contract Skill Proof Readability Slice

Connected skill and knowledge proof readiness to contract objective summaries. `ContractObjectiveReadabilityAuthority` now has a skill-aware overload that can explain whether a contract-relevant capability, such as Certified Market Appraisal, Investigation Trace Reading, Streetwise Appraisal, or fabrication inspection, is trained, and whether related knowledge such as Contract Negotiation is known.

The map/objective panel now passes live `unlockedSkillNodes` and `unlockedKnowledges` into the contract summary. This is deliberately readable proof only: contract completion, reward payout, and turn-in rules remain owned by the existing contract flow. Added `Milestone03ContractSkillProofSmoke` and wired it into Gate 3.

Verification: `javac --release 17`, `Milestone03ContractSkillProofSmoke`, `Milestone02ContractObjectiveReadabilitySmoke`, `Milestone03TradeCertifiedAppraisalSmoke`, `Milestone03SkillTreeCapabilityHooksSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3078 classfiles. Package jar SHA-256: `BF8CD5BF3DA5F3438B1AA9443153545C900443FA53F25F2036C7097D7264A29A`. Local GUI/package boot smoke was not run.

## Milestone 03 - Contract Skill Proof Infopedia Slice

Expanded the Contract Objectives and Evidence Infopedia entry so the new contract skill-proof lines are not hidden behavior. The entry now explains that contract summaries can show skill and knowledge proof readiness for Certified Market Appraisal, Investigation Trace Reading, Streetwise Appraisal, fabrication inspection, Contract Negotiation, and Scrap-Forging Doctrine where a contract implies those proof lanes.

The reference keeps the ownership boundary explicit: skill proof does not complete contracts, pay rewards, bypass hand-ins, or reveal hidden target identity. Added `Milestone03ContractSkillProofInfopediaSmoke`, strengthened `Milestone02InfopediaMechanicsReadabilitySmoke`, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ContractSkillProofInfopediaSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, `Milestone03ContractSkillProofSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3080 classfiles. Package jar SHA-256: `618EF12512795F5C26160F01FC86B3D16B8364D246693BE861D6AD6349A6117C`. Local GUI/package boot smoke was not run.

## Milestone 02 - Character Paper Doll and Equipment Slot Restoration

Restored the player-character paper doll and exact limb hit-point/status display to the live character panel. The repair reuses `Candidate.body`, `BodyPart`, `BodyConditionReadabilityAuthority`, and existing equipment state rather than creating duplicate health data. The character panel now expands for the body display, colors each tracked region by condition, prints current/max hit points, and lists every tracked limb with its readable state.

Replaced the two blind hand-only unequip buttons with selectable Left Hand, Right Hand, and Body Protection slots. The selected equipped item receives a detail/icon surface and can be unequipped explicitly; hand slots route through the existing unequip implementation, while body protection is returned to carried inventory before the clothing slot is cleared. Added `CharacterPaperDollAuthority` and `Milestone02CharacterPaperDollSmoke`, with the smoke wired into Gate 3.

Verification: Java 17 UTF-8 full-source compile, focused paper-doll smoke, and complete Gate 3 smoke suite in GitHub Actions. Manual GUI review remains required for supported window sizes.

## Milestone 02 - Full Character Equipment and Medical Body Tabs

Expanded the restored character panel into Overview, Equipment, and Medical tabs. The Equipment tab now exposes Headgear, Underclothes, Clothes/Body, Gloves, Boots, Backpack, two Ring slots, two Accessory slots, and both Hand slots. It includes a live paper doll, selectable slots, carried-item navigation, compatibility feedback, selected-item icon/detail presentation, equip replacement, and explicit unequip-to-inventory behavior. Backpacks now contribute a visible additive carry-capacity bonus while preserving the existing Strength/world-settings capacity authority.

Added `CharacterEquipmentAndMedicalAuthority` as the narrow loadout and body-modification contract rather than embedding another subsystem in the legacy panel bridge. Existing hand and clothing fields remain authoritative for their established mechanics; new wearable slots live in an enum-keyed map. The Medical tab mirrors every tracked `Candidate.body` region and reserves independent Mutation, Modification, and Cybernetic records for each region. It intentionally does not invent surgery, rejection, power, maintenance, mutation, or wireless cybernetic mechanics; future systems can bind through `installCharacterMedicalRecord(...)` and `MedicalSlotKey.storageKey()` without redesigning the screen. Cybernetic presentation assumes isolated direct-interface hardware rather than wireless control.

Added property persistence hooks for the new wearable and medical maps where the existing `Persistence.writeCore/readCore` authority is available. Added focused authority and live GamePanel integration smokes, including equip/unequip transfer, backpack capacity, anatomical region mapping, and future medical-record binding. Verification uses Java 17 UTF-8 full-source compilation and the complete Gate 3 suite.

## Milestone 03 - Contract Skill Proof Audit Slice

Advanced Phase 18.1 by giving contract skill and knowledge proof readiness a dedicated audit surface beside the player-facing objective summary. `ContractObjectiveReadabilityAuthority.auditLines(...)` reports the owning authority, active/shown contract counts, readiness states for inferred skill and knowledge proof, evidence route confidence, and the explicit boundaries that proof audit does not complete contracts or mutate rewards.

The audit remains safe for ordinary surfaces: this slice records `rawIdsHidden=true` and verifies that contract IDs and target entity IDs do not leak while still showing useful named proof states such as Certified Market Appraisal and Contract Negotiation. Added `Milestone03ContractSkillProofAuditSmoke`, referenced it from the Contract Objectives and Evidence Infopedia entry, and wired the focused smoke into Gate 3.

Manifest generation was repaired after the legacy PowerShell hasher stalled on the full 153,289-file, 5.6 GB workspace. `ROOT_tools/update-repository-file-manifest.ps1` now defaults to an incremental Python generator that preserves the existing seven-column `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` schema, reuses hashes when path, size, and UTC modified time match, and retains `-ForceHash` plus `-LegacyFullHash` escape hatches for full validation.

Verification: `javac --release 17`, `Milestone03ContractSkillProofAuditSmoke`, `Milestone03ContractSkillProofInfopediaSmoke`, `Milestone03ContractSkillProofSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3082 classfiles. Package jar SHA-256: `3AAB4F4201F11F1DE9B02514558D16565AF474FAFDA3B147E5A2E9762A57079F`. The incremental manifest refresh wrote 153,289 indexed rows, reused 150,165 existing hashes, hashed 3,124 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Skill Tree Definition Audit Slice

Advanced Phase 18.1 by expanding the skill-tree audit surface from a compact summary into structured branch and node definition lines. `SkillTreeProgressionAuthority.definitionAuditLines()` now reports the owning authority, branch and node counts, XP-cost coverage, dependency coverage, access-gate coverage, stat modifiers, exclusive groups, capability hooks, skill/knowledge separation, and an explicit ordinary-UI raw-ID boundary.

Each branch audit names its world-use purpose, and each node audit names its readable branch, XP cost, prerequisite, access requirement, stat requirement/effect, specialization group, capability hook, and knowledge boundary. The default audit keeps ordinary UI free of raw node IDs while still making Phase 18 editor/audit facts inspectable. Added `Milestone03SkillTreeDefinitionAuditSmoke`, expanded the Skill Progression Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03SkillTreeDefinitionAuditSmoke`, `Milestone03SkillTreeProgressionReadabilitySmoke`, `Milestone03SkillTreeCapabilityHooksSmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3084 classfiles. Package jar SHA-256: `1774E6489B2DBC92AE08EBD8DCB19E3B4E2ABEA7FD97758A23AB4BAAE8F4F53E`. The incremental manifest refresh wrote 154,842 indexed rows, reused 151,719 existing hashes, hashed 3,123 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Production Quality Definition Audit Slice

Advanced Phase 18.1 by adding a production quality definition audit to `ProductionQualityTraceAuthority`. The audit names the active quality cap inputs, limiter owner, batch owner, issue-tag owner, provenance owner, material/facility/tool/operator boundaries, and the worker-quality handoff boundary between immediate manual Craft and staffed queued production.

The audit also records the batch definition and consequence contract: one manual Craft action creates one batch ID and inspection disposition, defect appraisal can reduce ordinary resale value by 40%, and item statistics, law enforcement, contamination effects, recalls, and counterfeit enforcement remain future owners. Batch issue tags and provenance fields are listed as inspectable data definitions rather than hidden effects. Added `Milestone03ProductionQualityDefinitionAuditSmoke`, expanded the Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ProductionQualityDefinitionAuditSmoke`, `Milestone03ProductionQualityTraceSmoke`, `Milestone03ProductionBatchIssueTagsSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3086 classfiles. Package jar SHA-256: `8943DB2BF92F0FDB4A3E1FFD19352A47871646440D171D06938A2335A066D46E`. The incremental manifest refresh wrote 156,396 indexed rows, reused 153,271 existing hashes, hashed 3,125 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Production Knowledge Source Audit Slice

Advanced Phase 18.1 by adding a production knowledge-source definition audit to `ProductionKnowledgeSourceAuthority`. The audit names the player knowledge, selected-machine doctrine, and claimed-facility doctrine sources; records that the effective knowledge set is a union of those valid sources; and keeps ordinary UI free of raw IDs.

The audit also records ownership boundaries for Teach Machine and claimed-room doctrine sharing: machines preserve one installed recipe doctrine only after the player knows it, and facility doctrine can come only from another serviceable production station in the same claimed production room. Broken providers, stations outside the room, and unclaimed workspaces do not share doctrine. Added `Milestone03ProductionKnowledgeSourceAuditSmoke`, expanded the Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ProductionKnowledgeSourceAuditSmoke`, `Milestone03MachineKnowledgeSourceSmoke`, `Milestone03ProductionFacilityKnowledgeSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3088 classfiles. Package jar SHA-256: `F971ADEDAAF359BCB06422A758DADA460BE1D6FD22D1CF93920249FC992CF050`. The incremental manifest refresh wrote 157,951 indexed rows, reused 154,824 existing hashes, hashed 3,127 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Faction Production Mutation Audit Slice

Advanced Phase 18.1 by adding a faction production mutation definition audit to `ProductionFactionMutationAuthority`. The audit names `FactionManufacturingProfile` as the profile owner, `ItemProvenanceRecord` as the provenance owner, and records the visible formula inputs for value, charges, and defect pressure without creating a parallel faction-stat model.

The audit also records the effect boundary: faction mutation affects the existing output prefix, value, charges, and defect pressure, while law enforcement, reputation changes, seizure, corruption, and faction hostility remain future owners. Added `Milestone03ProductionFactionMutationAuditSmoke`, expanded the Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ProductionFactionMutationAuditSmoke`, `Milestone03ProductionFactionMutationSmoke`, `Milestone03QualityProvenanceSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3090 classfiles. Package jar SHA-256: `AAB0B3AC785AAC9DB66CBABD9A73EB267B332655C6D42EA567C3862AA08964C5`. The incremental manifest refresh wrote 159,507 indexed rows, reused 156,378 existing hashes, hashed 3,129 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Definition Audit Slice

Advanced Phase 18.1 by expanding `BlueprintConstructionAuthority` with a structured blueprint definition audit. The audit names the room-blueprint schema, relative cell offsets, anchors, object matrix, blueprint quality boundary, supported cell kinds, tile build recipe mapping, itemized cost and labor estimates, preflight rules, and the collisionless ghost-placement contract.

The audit is deliberately schema and preflight only: it does not place objects, consume materials, mutate room ownership, upgrade schematic quality, or bypass live placement validation. Added `Milestone03BlueprintDefinitionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02ConstructionCategorySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3092 classfiles. Package jar SHA-256: `D5D3A90BB85FBF711440C85B93FB0CC854C83AA6D0739779C20E1B9D1780DA1B`. The incremental manifest refresh wrote 161,064 indexed rows, reused 157,933 existing hashes, hashed 3,131 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Quality Band Definition Audit Slice

Advanced Phase 18.1 by adding a shared quality-band definition audit to `QualityAuthorityApi`. The audit names `QualityAuthorityApi` as the quality profile owner, `ItemQuality` as the item prefix/value/charge owner, the full item-quality order from Junk through Archeotech, and the doctrine-band order used by the Knowledge Tree.

The audit records the key boundary that Shoddy is a degradation quality rather than a target doctrine school, while Common remains the civic baseline for missing or ordinary doctrine. It also names item value/charge multipliers, production profile meanings, and the central production capping rule. Added `Milestone03QualityBandDefinitionAuditSmoke`, expanded Inventory and Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03QualityBandDefinitionAuditSmoke`, `Milestone02InventoryReadabilitySmoke`, `Milestone03ProductionQualityTraceSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3094 classfiles. Package jar SHA-256: `0EFC472C0841FF484B9CA0422ACE9DCB2AC1EF41E623F2E9C260F3056C916427`. The incremental manifest refresh wrote 162,622 indexed rows, reused 159,489 existing hashes, hashed 3,133 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Batch Issue Definition Audit Slice

Advanced Phase 18.1 by adding a batch issue definition audit to `ProductionBatchIssueAuthority`. The audit names the issue-tag owner, batch owner, and provenance owner; records the supported good, defective, contaminated, unstable, counterfeit, stolen-risk, restricted, and faction-certified batch tags; and reserves recall flags for a future owner.

The audit also records the effect boundary: issue tags preserve inspection and source-risk evidence, while only the existing defect appraisal changes ordinary resale value. Recall enforcement, seizure, law penalties, reputation effects, contamination damage, counterfeit penalties, item statistics, and ordinary use effects remain outside this owner. Added `Milestone03BatchIssueDefinitionAuditSmoke`, expanded the Production Forecast Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BatchIssueDefinitionAuditSmoke`, `Milestone03ProductionBatchIssueTagsSmoke`, `Milestone03ProductionQualityDefinitionAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3096 classfiles. Package jar SHA-256: `90727004D77EEE5FDDB42492BE193B61B7E20044C500A5534EBBE27E6AA36762`. The incremental manifest refresh wrote 164,181 indexed rows, reused 161,046 existing hashes, hashed 3,135 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Parity Audit Slice

Advanced Phase 18.1 and Phase 18.2 by adding a construction blueprint parity audit to `BlueprintConstructionAuthority`. The audit names the BuildRecipe catalog owner, construction category owner, room-blueprint owner, and future acquisition owner; counts player-facing names, descriptions, category coverage, faction restrictions, knowledge gates, and workbench gates; and verifies the sample room blueprint mapping.

The audit deliberately exposes current gaps instead of inventing hidden systems: faction vendor stock categories, reputation gates, permits, acquisition paths, faction construction capability, heat, suspicion, non-acquirable rooms, player-only exceptions, and faction-only exceptions remain future data owners. Added `Milestone03BlueprintParityAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintParityAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionCategorySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3098 classfiles. Package jar SHA-256: `415AC40E7502D9F1B46687DB77E084E9399B76BAFD0CC275D9697DE2D685348F`. The incremental manifest refresh wrote 165,741 indexed rows, reused 162,604 existing hashes, hashed 3,137 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Acquisition Path Audit Slice

Advanced Phase 18.2 by adding `BlueprintAcquisitionPathAuthority`, a data-owned acquisition audit for construction blueprints. The audit maps the existing BuildRecipe catalog to construction categories, representative/vendor archetypes, access labels, acquisition routes, legal labels, and player-facing explanations without adding live shop offers.

The acquisition audit distinguishes blueprint ownership from permission, reputation, license, permit, materials, workbench, knowledge, placement access, utilities, and construction labor. It also records future-owner boundaries for live vendor stock, reputation spending, permit purchases, theft resolution, heat and suspicion mutation, and faction construction execution. Added `Milestone03BlueprintAcquisitionPathAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone03BlueprintParityAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionCategorySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3104 classfiles. Package jar SHA-256: `11CE3C1DBF96F21BE7EBE43CF9FB9668A198AB4E9906B69B821D7F7BBE4E3F01`. The incremental manifest refresh wrote 167,307 indexed rows, reused 164,164 existing hashes, hashed 3,143 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Room Construction Parity Audit Slice

Advanced Phase 18.1 by adding `RoomConstructionParityAuthority`, an audit surface for room-stamp construction parity. The audit samples current zone room profiles, marks faction rooms with player acquisition status, marks player construction blueprints with faction-use status, maps common room functions to current BuildRecipe blueprints, and records documented exceptions for plazas, transitions, closets, and unsafe utility spaces.

The audit keeps consequences future-owned: it does not place rooms, unlock blueprints, mutate ownership, spend reputation, create faction construction jobs, or apply heat and suspicion. Added `Milestone03RoomConstructionParityAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03RoomConstructionParityAuditSmoke`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone03BlueprintParityAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionCategorySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3110 classfiles. Package jar SHA-256: `75378ECA3181C3B23D78C4F207987926340BA6F8CD8080FDDCE8EB8CAC60E710`. The incremental manifest refresh wrote 168,876 indexed rows, reused 165,727 existing hashes, hashed 3,149 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Expansion Heat Audit Slice

Advanced Phase 18.2 and Phase 19 release-audit readiness by adding `BlueprintExpansionHeatAuthority`, an audit-only heat and suspicion projection surface for construction blueprints. The audit scores current BuildRecipe blueprints through visible commerce, armed defenses, industrial footprint, laboratory or clinic footprint, access or legality risk, and faction-visible asset drivers while reusing the existing Expansion Heat readability bands.

The slice keeps gameplay mutation explicitly future-owned: projected heat and suspicion do not change `gangHeat`, suspicion, reputation, permits, law response, faction schemes, or construction completion. Added `Milestone03BlueprintExpansionHeatAuditSmoke`, expanded the Construction Blueprints and Expansion Heat Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ExpansionHeatReadabilitySmoke`, `Milestone03RoomConstructionParityAuditSmoke`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,116 classfiles. Package jar SHA-256: `34F17B3C08DE436EE18B507AB7EDD1917C01AAFD50F98FB23E0B667456CE9274`. The repository manifest refresh wrote 170,448 indexed rows, reused 167,293 existing hashes, hashed 3,155 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Definition Audit Slice

Advanced Phase 18.1, Phase 18.2, and the progressive construction standard by adding a definition audit to the existing `ProgressiveConstructionAuthority`. The audit names staged construction site ownership, saved BaseObject fields, required and inserted materials, labor progress, visual progress, final build symbol, quality, faction, held-tool timing, ghost-blue visual transition, and save/load restoration boundaries.

Live construction confirmation now creates a prepaid under-construction site instead of instantly completing the final base object. The existing placement checks and material consumption still run before placement, but labor completion now remains owned by staged construction; missing-material placement, worker dispatch, room ownership mutation, blueprint unlocks, and heat application remain future owners. Added `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the focused smoke into Gate 3.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `F041FA8D4DA5915B4E0019B672D81F395121BA9FE2C652D452F7AFBA4BFC0AFD`. The repository manifest refresh wrote 172,026 indexed rows, reused 168,868 existing hashes, hashed 3,158 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Visibility Slice

Advanced the staged construction bridge by making under-construction sites visibly and textually distinct after placement. The world renderer now overlays staged base objects with the existing ghost-blue construction tint and a compact progress bar, while object interaction text reports staged-site status, material progress, labor progress, missing materials, and the final build target before offering only inspection/approach actions.

This keeps unfinished construction from being mistaken for a completed facility: machine operation, crafting, repair, business returns, room ownership mutation, worker dispatch, and heat application remain outside this visual/readability owner. Expanded `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, updated the Construction Blueprints Infopedia wording, and kept the focused smoke wired into Gate 3.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `DE7908CCEF8E2B003642000784566906E61C2986C948280C91CF9D2265192B4A`. The repository manifest refresh wrote 173,594 indexed rows, reused 170,437 existing hashes, hashed 3,157 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Labor Action Slice

Advanced staged construction from visible state into a bounded player action. Under-construction sites now expose a Work button in the object interaction panel; each use contributes one turn of labor through `ProgressiveConstructionAuthority`, reports progress or completion, advances time, and lets the staged owner finalize the base object only when materials and labor are complete.

This still keeps scope narrow: the Work action does not dispatch workers, bypass missing materials, mutate room ownership, unlock blueprints, apply heat, or treat unfinished sites as machines, shops, repair targets, or craft stations. Expanded `Milestone03ProgressiveConstructionDefinitionAuditSmoke` and updated the Construction Blueprints Infopedia wording.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `1D970F297A8CDB1980D9BDE5EB047FF7CF03BBB8B4E5ABE49727CDBCEC79AEFE`. The repository manifest refresh wrote 175,162 indexed rows, reused 172,005 existing hashes, hashed 3,157 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Material Staging Slice

Extended the Work action so staged construction sites can pull available missing materials through the existing production-input consumption route before labor advances. A Work action can now stage construction supplies, machine parts, and named component units into the site's inserted-material ledger, then apply labor only once the staged materials satisfy the required-material ledger.

The slice still keeps first placement conservative: live blueprint placement continues to require available materials up front, while the reusable staged owner now supports partial-site material contribution for future missing-material placement and recovery flows. Worker dispatch, room ownership mutation, blueprint unlocks, and heat application remain outside this owner. Expanded `Milestone03ProgressiveConstructionDefinitionAuditSmoke` and updated the Construction Blueprints Infopedia wording.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `58BDD0B9E7167C352A74972449573777CCCCF541FC5B2D022886A0FE92C3EB03`. The repository manifest refresh wrote 176,730 indexed rows, reused 173,573 existing hashes, hashed 3,157 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Partial Placement Slice

Advanced live construction placement so material shortfalls no longer always block starting a staged site. If placement geometry, workbench, knowledge, and occupancy checks pass, and at least one required material unit is available, the build panel reports a STAGED START and confirmation creates a partial under-construction site with the available materials inserted. Additional materials and labor then continue through the existing Work action.

The slice keeps no-input and non-material failures honest: zero available construction inputs still block placement, while knowledge, workbench, occupied tile, map object, base object, and world-bound failures remain hard refusals. Worker dispatch, room ownership mutation, blueprint unlocks, and heat application remain outside this owner. Expanded construction readability and progressive construction smokes, and updated the Construction Blueprints Infopedia wording.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,118 classfiles. Package jar SHA-256: `95CB5C094C144A8C049C0966523B25307503C40C76C63FC3DFE9A035720FCD32`. The repository manifest refresh wrote 178,298 indexed rows, reused 175,139 existing hashes, hashed 3,159 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Persistence Guard Slice

Added a concrete save/load guard for staged construction sites. `Milestone03ProgressiveConstructionPersistenceSmoke` now writes a partial construction site through `Persistence.writeCore`, reloads it through `Persistence.readCore`, and verifies that under-construction status, placeholder symbol, final symbol, assigned recipe, required and inserted materials, labor progress, visual progress, quality, faction, inspection text, and later BaseObject fields survive the round trip.

The slice also checks that loaded staged sites are restored before completed-object configuration and keep the world tile on the construction placeholder rather than silently becoming finished furniture. Updated the progressive construction audit, Gate 3 suite, and Construction Blueprints Infopedia guard.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionPersistenceSmoke`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,120 classfiles. Package jar SHA-256: `5A39025C477A925396456E0575637451E64B8CF86A5179BADE11F522BD551E62`. The repository manifest refresh wrote 179,869 indexed rows, reused 176,709 existing hashes, hashed 3,160 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Dismantle Slice

Added a bounded Dismantle action for unfinished staged construction sites. Staged sites now expose Dismantle beside Work in the object interaction panel; the action removes the unfinished placeholder, recovers inserted construction supplies and machine parts to pooled resources, returns named components to base storage, clears the construction placeholder tile when it owns that tile, and advances one turn.

The slice keeps completion authority honest: dismantling does not configure the final facility, recover labor progress, mutate room ownership, dispatch workers, unlock blueprints, apply heat, or target already-completed base objects. Added `Milestone03ProgressiveConstructionDismantleSmoke`, expanded the progressive construction audit, and updated the Construction Blueprints Infopedia guard.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionDismantleSmoke`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03ProgressiveConstructionPersistenceSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,124 classfiles. Package jar SHA-256: `CF6A18B1EA8EB7DB2E4407E441538288A7230B4229CE5F2AAD89A4289B1B9BB3`. The repository manifest refresh wrote 181,443 indexed rows, reused 178,278 existing hashes, hashed 3,165 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Tile Sync Slice

Aligned live staged-construction placement with the save/load behavior by synchronizing staged sites back to the world tile grid. Live placement now reserves the target tile with the construction placeholder immediately after the BaseObject is added, and staged completion restores the final built symbol before normal built-object configuration runs.

This keeps map rendering, same-tile placement denial, save/load restoration, dismantle cleanup, and completed-facility behavior consistent without adding room ownership, worker dispatch, blueprint unlocks, heat, or faction construction side effects. Added `Milestone03ProgressiveConstructionTileSyncSmoke`, expanded the progressive construction audit, and updated the Construction Blueprints Infopedia guard.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionTileSyncSmoke`, `Milestone03ProgressiveConstructionDismantleSmoke`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03ProgressiveConstructionPersistenceSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,126 classfiles. Package jar SHA-256: `02902AD6BDC911F2BD5BC83D02F48A616F5A2DDF2EB35AFC1140BB434B668D5D`. The repository manifest refresh wrote 183,017 indexed rows, reused 179,850 existing hashes, hashed 3,167 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Progressive Construction Original Tile Restoration Slice

Preserved the walkable tile underneath unfinished staged construction. Live placement now records the original tile before writing the construction placeholder, staged-site save lines persist that original tile in an append-only BaseObject field, and dismantle restores the recorded tile instead of flattening every cancelled site to plain floor.

This keeps cancellation honest for roads, doors, floors, and other walkable construction surfaces while preserving the existing boundaries: completion still owns the final built symbol, dismantle still only targets unfinished staged sites, and no room ownership, worker dispatch, blueprint unlock, heat, or faction construction side effects are introduced. Added `Milestone03ProgressiveConstructionOriginalTileSmoke`, expanded persistence/tile-sync guards, and updated the Construction Blueprints Infopedia wording.

Verification: `javac --release 17`, `Milestone03ProgressiveConstructionOriginalTileSmoke`, `Milestone03ProgressiveConstructionTileSyncSmoke`, `Milestone03ProgressiveConstructionDismantleSmoke`, `Milestone03ProgressiveConstructionDefinitionAuditSmoke`, `Milestone03ProgressiveConstructionPersistenceSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 618 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,128 classfiles. Package jar SHA-256: `CCF8F81CCE960741BC9F7AE4D5E10B2E7C025E64483385A25699F33E868B8C92`. The repository manifest refresh wrote 184,592 indexed rows, reused 181,420 existing hashes, hashed 3,172 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Ownership Permission Readability Slice

Advanced the construction release-audit wording by making live blueprint previews distinguish blueprint ownership from actual build permission and readiness. Construction details now state that owning a blueprint is separate from permission, reputation, license, permit, materials, workbench, knowledge, placement access, utilities, and labor, so the player can understand why a known plan may still be blocked or only partially startable.

This keeps the slice strictly readable and audit-backed: it does not add vendors, reputation spending, permit purchase, theft resolution, utility simulation, heat mutation, suspicion mutation, or faction construction execution. Added `Milestone03BlueprintOwnershipPermissionReadabilitySmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintOwnershipPermissionReadabilitySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 619 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,130 classfiles. Package jar SHA-256: `C4CB6BB8B32ACED3B1FD689A9878368474FB62FD33E5224465283F236FED1913`. The repository manifest refresh wrote 186,168 indexed rows, reused 182,998 existing hashes, hashed 3,170 changed or new files, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Entombment Safety Guard Slice

Hardened construction placement against self-entombment. Room-blueprint preflight now warns when a stamp has no connection anchor or doorway, live placement now invokes the claimed-room exit-path guard before allowing OK or STAGED START placement, and the runtime guard rejects the player's current tile, NPC-occupied tiles, and claimed-room blocker placements that would leave no valid access path to a door or exit.

This keeps the slice safety-focused and bounded: it does not add vendor stock, permit purchase, utility simulation, worker dispatch, room ownership mutation, heat mutation, suspicion mutation, faction construction execution, or background path scans. Added `Milestone03BlueprintNoSelfEntombmentAuditSmoke` and `Milestone03SelfEntombmentRuntimeGuardSmoke`, expanded the Construction Blueprints Infopedia wording, and wired both guards into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintNoSelfEntombmentAuditSmoke`, `Milestone03SelfEntombmentRuntimeGuardSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 621 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,134 classfiles. Package jar SHA-256: `AA1E9C0C21D0FD87F0D076D64C2B1A4E557F2E918E4508F0F2243DBAC1A1D906`. The final repository manifest refresh wrote 189,323 indexed rows, reused 189,322 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Utility Readiness Audit Slice

Advanced the construction governance lane by adding a metadata-only utility readiness audit. `ConstructionGovernanceAuthority` now reports utility-bearing room and blueprint coverage, tracked hook families, fail-closed missing utility validation, ready metadata validation, and passability interaction without claiming that a live utility network exists.

This keeps the slice explicitly bounded: it does not create utility grids, consume fuel or water, schedule workers, mutate room ownership, apply heat or suspicion, run background scans, or complete construction. Added `Milestone03BlueprintUtilityReadinessAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintUtilityReadinessAuditSmoke`, `Milestone03BlueprintDefinitionAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 622 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,136 classfiles. Package jar SHA-256: `B10DFEB07313A84C68DA8FB2A5AAF0F3047006AE1C8A8E0D1E348775CD978C99`. The final repository manifest refresh wrote 190,902 indexed rows, reused 190,901 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Permission Readiness Audit Slice

Advanced the construction acquisition lane by adding `BlueprintPermissionReadinessAuthority`, a forecast-only permission readiness layer over existing blueprint acquisition metadata. The audit classifies blueprints into public-ready, permit-or-license, faction-standing, restricted legal-access, and illicit or stolen-risk gates, then reports concrete blockers such as unowned blueprint, missing permit, missing license, missing faction standing, or missing legal access.

This keeps blueprint ownership honest without inventing live commerce: the slice does not add vendor offers, spend reputation, buy permits, grant licenses, resolve theft, mutate heat or suspicion, bypass placement validation, or execute faction construction. Added `Milestone03BlueprintPermissionReadinessAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintPermissionReadinessAuditSmoke`, `Milestone03BlueprintAcquisitionPathAuditSmoke`, `Milestone03BlueprintOwnershipPermissionReadabilitySmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 624 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,142 classfiles. Package jar SHA-256: `84FA0AF1F5BB54E89F06E9F4834FA0DD9B8236ABDEBCCF8578B310D0DF34E723`. The final repository manifest refresh wrote 192,487 indexed rows, reused 192,486 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Capability Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionCapabilityAuthority`, an audit-only faction construction capability layer over existing blueprint, parity, and permission metadata. The audit marks plausible faction-construction candidates and reports planning blockers such as permission readiness, faction budget, construction crew, room claim, and construction materials.

This keeps faction construction strictly as planning metadata: the slice does not spawn faction construction jobs, mutate room ownership, reserve or consume materials, spend faction budget, grant permits, apply heat or suspicion, bypass placement validation, or complete construction. Added `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone03BlueprintPermissionReadinessAuditSmoke`, `Milestone03RoomConstructionParityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 626 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,148 classfiles. Package jar SHA-256: `38F7AA4BB3DA257D653F214BE33CDEB60779D420C55A16CA1F7474A8D2E69B1D`. The final repository manifest refresh wrote 194,075 indexed rows, reused 194,074 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Job Definition Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionJobDefinitionAuthority`, a definition-only contract for future faction construction jobs. The audit names lifecycle states, required fields, sample definitions, capability and permission readiness sources, and the handoff to staged construction before any execution owner exists.

This keeps faction construction non-mutating: the slice does not create a live job queue, reserve or consume materials, assign workers, mutate room ownership, apply heat or suspicion, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone03BlueprintPermissionReadinessAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 628 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,154 classfiles. Package jar SHA-256: `44BD9BBD89CB117FD2AA01DDA6E70DA0CFB7A36A28CC54DC7B92A2176E72E94D`. The final repository manifest refresh wrote 195,666 indexed rows, reused 195,665 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Material Reservation Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionMaterialReservationAuthority`, an audit-only material reservation contract for future faction construction jobs. The audit derives required Construction supplies, Machine parts, and named component costs from `BuildRecipe`, then reports reserved-preview and missing-material ledgers without touching inventory.

This keeps faction construction non-mutating: the slice does not remove supplies, remove machine parts, remove named components, write reservation rows, stage materials into a site, assign crew, mutate room ownership, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 630 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,160 classfiles. Package jar SHA-256: `EA46B26BAC290E703080127014FEF4FF93AD99B40A63BA1084373E17B5A64EF3`. The final repository manifest refresh wrote 198,838 indexed rows, reused 198,837 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Crew Assignment Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionCrewAssignmentAuthority`, an audit-only crew assignment contract for future faction construction jobs. The audit derives required crew profiles and labor turns from `BuildRecipe` workbench, faction restriction, attention, construction category, and base labor metadata before any execution owner can bind workers.

This keeps faction construction non-mutating: the slice does not assign recruits, move NPCs, reserve workers, create schedules, mutate room ownership, remove materials, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 632 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,166 classfiles. Package jar SHA-256: `C401B0573D79A093B0AD6ADA6DA233D46BF0C6444F53704F65FCB637F9F7ADF3`. The final repository manifest refresh wrote 200,435 indexed rows, reused 200,434 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Site Readiness Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionSiteReadinessAuthority`, an audit-only room claim and placement-readiness contract for future faction construction jobs. The audit names room-claim, placement, access-route, no-self-entombment, utility, heat-preview, and staged-construction handoff checks before any execution owner can reserve a target site.

This keeps faction construction non-mutating: the slice does not claim rooms, reserve tiles, write ownership, bypass placement validation, bypass no-self-entombment checks, create staged sites, apply utilities, assign crew, remove materials, or complete construction. Added `Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke`, `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 634 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,172 classfiles. Package jar SHA-256: `08C8E613F1A13400C1B1FDC4D60D5CD59ED33613E3C2522E0FAA3E7C03CC32AB`. The final repository manifest refresh wrote 202,035 indexed rows, reused 202,034 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Budget and Heat Authorization Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionBudgetHeatAuthorizationAuthority`, an audit-only budget and heat authorization contract for future faction construction jobs. The audit estimates faction budget from `BuildRecipe` supplies, parts, named components, workbench need, faction restriction, and base labor turns, then reuses `BlueprintExpansionHeatAuthority` projections for heat and suspicion.

This keeps faction construction non-mutating: the slice does not spend faction budget, mutate heat, mutate suspicion, trigger law response, schedule faction schemes, reserve sites, assign crew, remove materials, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke`, `Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke`, `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone03BlueprintExpansionHeatAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 636 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,178 classfiles. Package jar SHA-256: `D6AE6B509C118A90DBA45D098A66704727BE29D4FA07B1A1F05722E22DBAE538`. The final repository manifest refresh wrote 203,638 indexed rows, reused 203,637 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

## Milestone 03 - Blueprint Faction Construction Cancellation Release Audit Slice

Advanced the construction parity lane by adding `BlueprintFactionConstructionCancellationReleaseAuthority`, an audit-only cancellation and release contract for future faction construction jobs. The audit requires cancelled or failed jobs to record a reason and declare release of site, crew, materials, budget hold, and attention preview before any future execution owner can retire a job.

This keeps faction construction non-mutating: the slice does not cancel live jobs, release live reservations, refund budget, mutate heat, mutate suspicion, move crew, return materials, remove staged sites, place objects, advance labor, or complete construction. Added `Milestone03BlueprintFactionConstructionCancellationReleaseAuditSmoke`, expanded the Construction Blueprints Infopedia wording, and wired the guard into Gate 3.

Verification: `javac --release 17`, `Milestone03BlueprintFactionConstructionCancellationReleaseAuditSmoke`, `Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke`, `Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke`, `Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke`, `Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke`, `Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke`, `Milestone03BlueprintFactionConstructionCapabilityAuditSmoke`, `Milestone02ConstructionReadabilitySmoke`, `Milestone02InfopediaMechanicsReadabilitySmoke`, and `Gate3PlayerFacingTextSmokeSuite` pass. Function and Mermaid maps refreshed; Mermaid found 638 Java modules. Rebuilt `PACKAGE_client` with `scripts\PACKAGE_CLIENT_WINDOWS.ps1 -CleanClasses -BuildJar` and passed Java 17 classfile scan across 3,184 classfiles. Package jar SHA-256: `0C3A714FFE5AA3DCC8BFB64847183525D66D60FBBCF8F250F05474D8C0D78867`. The final repository manifest refresh wrote 205,244 indexed rows, reused 205,243 existing hashes, hashed 1 changed or new file, and reported 0 hash errors. Local GUI/package boot smoke was not run.

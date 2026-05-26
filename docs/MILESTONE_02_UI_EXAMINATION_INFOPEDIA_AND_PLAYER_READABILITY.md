# Milestone 02 - UI, Examination, Infopedia, Player Movement, Controls, and Player Readability

This ordered milestone document consolidates planning for player-facing readability, the `Look` and `Examine` interaction split, intelligence-based entity analysis, refreshed intent checks, entity examination screens, player movement usability, ghost movement target placement, keyboard/mouse rebinding, controller/gamepad rebinding, control profiles, quest objective guidance, pet interaction feedback, Infopedia depth, Infopedia hot linking, and the general rule that exposed mechanics must be explained in compact useful language.

This milestone also owns the broad user-interface uniformity lane: conversation, trade, inventory management, base inventory management, faction member management, faction member inventory management, production management, construction management, construction menu organization, options/control menus, and all other ordinary game menus that must be trimmed, checked, guideline-aligned, readable, uniform, and free of raw implementation debris before publish-safe handoff.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the UI/readability/examination/Infopedia/player-movement/input-rebinding/menu-management slice of that roadmap.

This document is not a changelog. Completed implementation still belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules still belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine still belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Source topical files absorbed or aligned

This milestone aligns and partially absorbs material from:

- `MILESTONE_EXAMINATION_AND_INFOPEDIA_SYSTEMS.md`
- `MILESTONE_WORLD_CONTENT_AND_PET_SYSTEMS.md` where pet interaction feedback touches UI/readability
- `MILESTONE_PARITY_BLUEPRINTS_AND_FACTION_MARKETS.md` where Infopedia acquisition references and blueprint restrictions need player-facing explanation
- `MILESTONE_KNOWLEDGE_SKILLS_MEDICAL_AND_FABRICATION_SYSTEMS.md` where Infopedia entries explain knowledge, skills, medicine, cybernetics, narcotics, and quality fabrication
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md` where economy/provenance/legality needs Infopedia and player-facing explanation
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md` where construction, access, heat, and blueprint systems need compact UI explanations
- `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md` where vehicle planned movement mirrors the player movement planning model and must share input/rebinding rules
- `MILESTONE_07_AGES_OF_CONTROL_SCHEMES_AND_QUEST_LIFECYCLES.md` where scheme quests, objective guidance, journal intelligence, evidence, and timing windows need clear UI
- `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md` where treatment, body state, cybernetics, prosthetics, narcotics, and clinic services need readable UI
- `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md` where generated rooms, vendors, workforce anchors, containers, and facility inspection need readable menus
- `MILESTONE_ROADMAP_REORGANIZATION_AND_PHASE_ALIGNMENT.md`

The older topical files should remain in place until their contents are fully absorbed into the ordered milestone sequence and the user explicitly approves archival, deprecation, or cleanup.

## Phase mapping

Primary phase groups:

- Phase 4 - UI, input, rendering, accessibility, display, graphics, options, control rebinding, player movement usability, menu containment, and presentation containment.
- Phase 14 - Combat, health, body condition, unconsciousness, death, saves, and feedback.
- Phase 15 - Character creation, names, ranks, rosters, social identity, faction member management, and personnel interfaces.
- Phase 17 - Economy, quests, contracts, faction reputation, pets, ownership, trade, vendor UI, inventory transfer, and player-facing mission clarity.
- Phase 18 - Editor, localization, modding, Infopedia, content pipeline, input-action audit, and menu-definition audit surfaces.
- Phase 19 - Polish, packaging, QA, and release audit.

Secondary dependencies:

- Phase 2 - Semantic asset IDs, player-facing labels, controller glyph assets, mouse/cursor assets, and prompt icons.
- Phase 5 - Operations, hauling, reservations, transfer intent, movement intent, route preview, and task management surfaces.
- Phase 6 - Production status, machine operation, recipe explanation, production management, and output routing.
- Phase 7 - Access/ownership denial messaging, containers, and permission checks.
- Phase 8 - Population/social identity exposed through examination, rosters, and faction member management.
- Phase 9 - Item provenance exposed through Infopedia, inspection, trade, and inventory management.
- Phase 10 - Vehicle inspection, cargo management, crew management, planned vehicle movement, driving controls, and large-entity readability.
- Phase 11 - Active schemes, quest lifecycle readability, and leadership-intelligence UI.
- Phase 12 - Construction feedback, blueprint restrictions, construction menu organization, base inventory management, and player expansion heat explanation.
- Phase 13 - Hazard warnings, movement safety warnings, visibility feedback, and ghost target safety feedback.

## Core readability doctrine

If a system is player-visible, the player should be able to understand what it is, why it matters, and what broad action paths are available without reading source code or seeing raw internal identifiers.

Player-facing UI should prefer compact truth over verbose noise. It should hide raw package IDs, Java class names, UUIDs, registry handles, implementation flags, manifest keys, raw asset IDs, and developer-only identifiers unless the player has explicitly opened a developer/audit surface.

Readable does not mean omniscient. The game may hide secrets, uncertain identities, vague rumors, unknown mechanisms, locked details, and failed checks. But uncertainty should be represented in player-facing terms such as `unknown`, `suspected`, `rumored`, `appears to be`, `too dark to tell`, `requires closer examination`, or `needs better tools` rather than leaking debug state.

## Player movement usability doctrine

Player movement must be deliberate, readable, and correctable. The planned movement system should not be limited to keypad double-auto ghost prediction. If the player wants fine-tuned movement, they should be able to manually invoke a move command, choose where the movement ghost is placed, and explicitly control the target location for the end of the movement chain.

The movement ghost is a user-facing planning object. It should show where the player intends to end movement, what path or direction is being attempted, and why the chosen target is valid or invalid. It must not feel like an invisible automation guess that the player must fight against.

The keypad or quick-direction input may remain as a fast control path, but manual move-command placement must exist for precision movement, cautious movement, path safety, tactical positioning, interaction alignment, room/door approach, cover approach, hazard avoidance, and movement around entities, vehicles, containers, machines, or tight corridors.

## Input rebinding and control profile doctrine

The game must treat input as a player-facing accessibility and usability system, not as hardcoded developer convenience. Keyboard, mouse, and controller/gamepad inputs should be represented through named actions, contexts, conflict rules, prompts, and saved control profiles.

Every major action should have a player-facing action name and belong to a context such as global, movement, targeting, conversation, trade, inventory, construction, production, vehicle driving, combat, map, quest log, Infopedia, save/load, or developer/audit. Rebinding one action must not silently break another context without warning.

Controller/gamepad support must not be treated as an afterthought. It needs rebinding, glyph prompts, deadzones, axis inversion where relevant, sensitivity, hold/tap behavior, focus navigation, ghost-target nudging, and safe recovery controls. Mouse and keyboard support also need full rebinding depth: keys, keypad, modifiers, mouse buttons, wheel actions, click/drag actions, alternate binds, and conflict detection.

The player must always have a safe way to recover from a bad binding configuration. There should be reset-to-default, restore-last-good, clear binding, detect input, cancel rebinding, and keyboard/mouse fallback behavior. A user should not be able to permanently strand themselves outside the UI because they rebound the confirm, cancel, menu, or movement controls poorly.

## Menu uniformity doctrine

All ordinary player-facing menus should feel like parts of one game rather than unrelated Swing fragments. Conversation, trade, inventory, base storage, faction member management, faction member inventory, production, construction, quests, Infopedia, options, controls, character, movement planning, and management surfaces should share menu grammar: title/header behavior, close/back behavior, selection lists, detail panes, action buttons, disabled-state explanations, hot links, scroll bounds, transfer controls, confirmation prompts, and compact failure messages.

Uniformity does not mean every menu is identical. It means each menu follows the same interaction laws, hides the same implementation debris, explains failure with the same clarity, and keeps text/buttons/lists contained in their owning frames. A trade interface, a faction roster, a construction menu, a controls menu, and a movement placement prompt may have different content, but the player should not need to relearn UI rules every time they open a new system.

## Dependency rule

The basic `Look` surface must precede the deeper `Examine` command. `Look` provides immediate visible observation. `Examine` builds on that observation and uses intelligence, relevant skill, visibility, distance, time spent, prior knowledge, and tools to refine estimates.

Input rebinding depends on an action-map abstraction. Game systems should consume named actions, not raw key codes or mouse-button numbers scattered across UI, movement, construction, vehicle, and inventory classes. The same action-map layer should support keyboard, mouse, keypad, controller/gamepad buttons, controller axes, modifier combinations, context-specific actions, and prompt rendering.

Movement planning depends on shared input, viewport, path validation, targeting transforms, hazard checks, and feedback grammar. The ghost target should consume the same map coordinates, viewport transforms, collision checks, tile accessibility checks, and movement-cost logic as the final movement execution. The preview must not lie about what movement will do.

Infopedia depth depends on stable semantic entries for items, entities, mechanics, rooms, machines, recipes, factions, statuses, blueprints, qualities, injuries, pets, vehicles, ownership states, movement rules, input rules, and provenance states.

Quest guidance depends on target identity and zone/path knowledge. If a target is exact and known, guidance may be precise. If a target is approximate, rumored, hidden, disguised, or unknown, guidance should communicate uncertainty instead of lying.

Management UI depends on shared container, transfer, permission, roster, production, construction, movement intent, input action, and ownership authorities. Menus should not invent parallel state or one-off transfer/movement/input rules merely because a surface needed a button quickly.

## Phase 4 - Look, Examine, player movement, controls, UI feedback, and menu uniformity

### Phase 4.1 - Look command readability pass

`Look` should answer what the player can immediately observe: basic entity/object label, visible faction cues, posture, surface intent, obvious injuries/equipment/hostility, ownership/access state, and whether deeper `Examine` is possible. It should be fast, compact, and readable.

Exit criteria:

Looking at an entity, object, container, room feature, pet, machine, or vehicle produces useful immediate information without raw IDs or hidden-state leakage.

### Phase 4.2 - Examine command and entity examination screen pass

When the player looks at a valid entity, the UI should expose an additional `Examine` command. `Examine` opens a character-screen-like view tuned for uncertainty and approximation rather than perfect internal truth.

The examination screen may show known or estimated name, type, faction, role, biography, state, intent, threat, health/injury, body plan, equipment, armor/weapon danger, mood, current activity, quest relevance, ownership/access relevance, pet temperament, and Infopedia links.

Exit criteria:

A player can look at an entity, choose `Examine`, and see a coherent analysis screen that provides useful information without pretending the player has perfect omniscience.

### Phase 4.3 - Examination refresh and time-investment pass

Intent and state checks should refresh every few turns while the player continues examining or repeatedly re-examines. Spending time examining can improve confidence, reveal additional details, correct uncertainty, or update stale intent information.

Refresh behavior should consider intelligence, relevant skills, distance, lighting, obstructions, movement, disguise, injury, hostility, prior knowledge, and available tools.

Exit criteria:

Spending extra time examining an entity can produce more reliable or more complete information, and stale intent estimates do not remain frozen forever.

### Phase 4.4 - Player movement ghost target placement pass

The player movement system should support explicit manual target placement for the movement ghost.

Required behavior:

- The player can invoke a move command or movement-planning mode.
- The player can manually choose the target location where the movement ghost is placed.
- The movement ghost marks the intended endpoint of the movement chain.
- The preview should show the intended path, route segment, or direction chain where feasible.
- The preview should explain invalid targets before movement commits.
- Quick keypad/directional ghost prediction may remain, but it must not be the only precision movement path.
- Manual placement should use the same coordinate and targeting transform rules as mouse interaction, combat targeting, look targeting, and construction placement.
- Cancel/confirm behavior should be clear.
- The ghost should not be confused with quest markers, selection markers, combat targeting, or debug overlays.

Use cases:

- Fine-tuned movement around doors, corners, furniture, and narrow corridors.
- Choosing exact cover/positioning tiles.
- Avoiding hazards.
- Approaching an interaction target without overshooting.
- Moving around NPCs, pets, vehicles, containers, or machines.
- Positioning for line of sight.
- Stopping at a particular tile rather than accepting the keypad double-auto prediction.

Exit criteria:

The player can intentionally place the movement ghost at the desired destination instead of relying only on automatic directional prediction.

### Phase 4.5 - Movement validation and feedback pass

Movement planning should explain why a target is valid, risky, blocked, or impossible.

Feedback examples:

- `Path blocked.`
- `Destination occupied.`
- `Door is closed.`
- `Hazard ahead.`
- `Too far for one movement chain.`
- `Cannot reach from here.`
- `Requires opening the door first.`
- `Movement would enter hostile reach.` where tactical reach exists.
- `Destination is outside the current room boundary.` where relevant.
- `Movement target selected.`

Exit criteria:

Invalid or dangerous movement is explained before the player commits where feasible, and movement refusal does not feel arbitrary.

### Phase 4.6 - Movement input unification pass

Keyboard, mouse, keypad, future controller/gamepad, look targeting, interaction targeting, construction targeting, combat targeting, and movement ghost placement should converge on shared viewport/targeting transforms.

Required behavior:

- Mouse click placement and keyboard movement preview consume the same world-coordinate conversion.
- Quick movement and planned movement share validation logic where practical.
- Movement preview and final movement execution should not disagree.
- Future controller/gamepad movement should be able to select or nudge the ghost target without a mouse.
- Input focus should not trap the player in movement planning.

Exit criteria:

Movement controls are predictable across input methods and do not fork into incompatible targeting systems.

### Phase 4.7 - Input action map and control profile pass

Create or preserve a unified action-map model for player input.

The action map should support:

- Named actions.
- Player-facing action labels.
- Action categories.
- Context-specific action scopes.
- Keyboard bindings.
- Mouse bindings.
- Keypad bindings.
- Controller/gamepad button bindings.
- Controller/gamepad axis bindings.
- Modifier combinations.
- Alternate bindings.
- Disabled/unbound actions where safe.
- Required actions that cannot be left unrecoverably unbound.
- Per-profile saved bindings.
- Reset-to-default behavior.
- Restore-last-good behavior.

Exit criteria:

Game systems consume named actions from a shared action map instead of scattered raw key/button checks.

### Phase 4.8 - Keyboard and mouse rebinding pass

Keyboard and mouse controls should be fully rebindable through a readable controls menu.

Required behavior:

- Rebind movement keys, keypad movement, wait/pass turn, look, examine, interact, inventory, character, map, quest log, Infopedia, construction, production, trade, conversation choices, confirm, cancel, back, menu, save/load, screenshot/debug-safe actions where applicable.
- Support modifier combinations where appropriate.
- Support mouse buttons, mouse wheel, click, drag, and context-click actions where appropriate.
- Detect conflicts before accepting a binding.
- Show whether a conflict is same-context, cross-context, harmless, or dangerous.
- Let the player replace, cancel, clear, or add alternate binding.
- Preserve a safe escape/cancel path during rebinding.
- Show readable action labels rather than raw key codes where possible.

Exit criteria:

Mouse and keyboard users can customize controls without breaking basic navigation, confirmation, cancellation, movement, or menu access.

### Phase 4.9 - Controller and gamepad rebinding pass

Controller/gamepad support should include real rebinding depth, not only a fixed default mapping.

Required behavior:

- Rebind buttons, triggers, sticks, D-pad, and axis actions where the input library supports them.
- Support movement, ghost target nudging, targeting, confirm, cancel, back, menu, look, examine, interact, inventory, map, quest log, Infopedia, construction, production, trade, conversation navigation, vehicle driving, and camera/viewport controls where implemented.
- Support deadzone configuration.
- Support axis sensitivity.
- Support axis inversion where relevant.
- Support hold/tap behavior where actions are overloaded.
- Support controller glyph prompts where prompt assets exist.
- Detect controller disconnect/reconnect where possible.
- Preserve keyboard/mouse fallback when a controller is missing, unsupported, or misconfigured.

Exit criteria:

Controller/gamepad users can navigate, move, target, manage menus, and recover from bad bindings without needing hardcoded controls.

### Phase 4.10 - Input conflict detection and recovery pass

The controls UI must prevent unrecoverable control states.

Conflict and recovery support should include:

- Same-context conflict warnings.
- Cross-context conflict warnings.
- Required-action warnings.
- Duplicate-binding display.
- Clear binding.
- Replace binding.
- Add alternate binding.
- Cancel rebinding.
- Reset current category.
- Reset all to defaults.
- Restore last working profile.
- Keyboard/mouse emergency fallback.
- Controller fallback notices.
- Confirmation before accepting dangerous changes.

Exit criteria:

The player cannot easily strand themselves without confirm/cancel/menu/movement, and conflicts are readable before they become frustrating.

### Phase 4.11 - Input prompt, glyph, and localization pass

Player-facing prompts should reflect the active binding profile.

Required behavior:

- UI prompts show the current keyboard/mouse/controller binding for relevant actions.
- Controller prompts use glyphs or readable button names where available.
- Prompts fall back to text when glyph assets are missing.
- Prompt strings should be localization-ready.
- Rebinding updates prompts without requiring restart where feasible.
- Tutorial/help/Infopedia references use action names and current bindings where feasible.

Exit criteria:

The UI teaches the player their actual configured controls rather than hardcoded defaults.

### Phase 4.12 - Quest objective guidance overlay pass

Quest UI must provide clear player-facing objective guidance: arrows toward known objectives or nearest zone transitions, slowly pulsing highlights on visible targets, and different feedback for exact, approximate, rumored, hidden, and unsafe objectives.

Exit criteria:

Active quests guide the player toward relevant objectives without deciding the solution path or flooding the screen with clutter.

### Phase 4.13 - Pet interaction feedback pass

Non-hostile pet entities must expose clear interaction feedback when the player can pet them. Dog-like pets support head pats, cat-like pets support scritches, and mouse/rat pets support nose boops. Blocked pet interactions explain the in-world reason.

Exit criteria:

If the player sees a non-hostile pet, the game acknowledges the obvious interaction path unless a real in-world state blocks it.

### Phase 4.14 - Access, construction, and ownership feedback pass

Access-denied, construction-denied, blueprint-denied, interaction-denied, and movement-denied states must explain themselves in useful language. The player should understand what blocked an action and what kind of path might resolve it.

Exit criteria:

Blocked actions produce truthful, compact, player-facing reasons rather than silent failure or debug leakage.

### Phase 4.15 - Universal menu trimming, checking, and guideline pass

All major menus should receive a deliberate trim/check/guideline pass before publish-safe claims. Check title, purpose, action grouping, disabled-state explanations, hidden raw IDs, compact rows, detail panes, back/close behavior, keyboard/controller viability, wrapping, scroll bounds, and stale/duplicate information.

Exit criteria:

Every major game menu has been checked against a shared readability guideline instead of being accepted because it technically opens.

### Phase 4.16 - Universal menu wrapper and navigation grammar pass

Major game menus should migrate toward a shared in-game window/menu wrapper where practical: header/title, back/close, main list, detail pane, action rail, confirmations, disabled-state explanations, search/filter/sort, scrollbars, Infopedia links, input focus, and escape/right-click/back behavior.

Exit criteria:

Menus are owned by a common in-game UI grammar and no longer look like unrelated detached utility windows.

### Phase 4.17 - Conversation and dialogue interface pass

Conversation UI should support readable dialogue, trade entry points, quest offers, faction standing changes, service menus, and player choices without becoming a text dump. It should show speaker identity, role/faction, relationship/standing context where relevant, clean body wrapping, choices, service/trade/quest buttons, consequence hints where known, timed-offer countdowns, and clear exit/back behavior.

Exit criteria:

Conversation menus are readable, compact, and able to guide the player into trade, quests, services, or faction interactions without burying choices in prose.

### Phase 4.18 - Trade, barter, vendor, and service interface pass

Trade UI should support buying, selling, services, restricted goods, price explanation, inventory transfer, faction standing, and legality without exposing raw internal records. It should show vendor identity/faction, funds, stock, player sell list, item details, price, quantity, quality, legality, restriction, provenance indicators, buy/sell/confirm controls, unavailable-goods explanations, service rows, search/filter/sort, and mixed-quality clarity.

Exit criteria:

Trade and service menus let the player understand what is for sale, why something is restricted, what the transaction will do, and what inventory or reputation consequence will result.

### Phase 4.19 - Inventory and equipment management interface pass

Inventory UI should manage carried items, equipment, item quality, weight/bulk, condition, provenance, use actions, drop actions, and transfer targets through one coherent grammar. It should show carried items, equipment slots, detail pane, quality/durability/condition, capacity, use/equip/drop/transfer controls, stack rules, quest protections, restricted/stolen/illicit indicators, Infopedia links, and useful sorting/filtering.

Exit criteria:

The player can understand and manage carried goods and equipment without losing track of quality, quest relevance, legality, or equipment authority.

### Phase 4.20 - Container, base inventory, and storage management interface pass

Base inventory and storage management should extend the shared inventory grammar. It should show player inventory, target container/base storage, transfer controls, take/deposit helpers where safe, storage filters, ownership/access state, capacity, routing/output preferences where relevant, quest protections, and warnings for stolen/restricted/volatile/faction-owned items.

Exit criteria:

Base inventory management is readable, safe, and consistent with ordinary inventory, trade, machine, and container transfer rules.

### Phase 4.21 - Faction member management interface pass

Faction member management UI should expose rosters, roles, command tiers, assignments, standing, status, and availability without collapsing NPC management into an unreadable list. It should support search/filter/sort by role, rank, status, assignment, location, injury, skill, loyalty, or availability where implemented.

Exit criteria:

Faction personnel can be reviewed and managed through a compact readable roster that respects player/NPC command parity and authority boundaries.

### Phase 4.22 - Faction member inventory and equipment management pass

Faction member inventory UI should support inspecting, assigning, transferring, equipping, and restricting goods for controlled or authorized members without bypassing ownership, access, rank, privacy, hostility, distance, or AI authority.

Exit criteria:

Faction member equipment and inventory management is useful for authorized command while respecting world ownership, command structure, and access rules.

### Phase 4.23 - Production and machine management interface pass

Production UI should explain recipes, inputs, outputs, workers, machine state, queue state, quality expectations, blockers, storage routing, and progress. It should show machine/facility identity, current recipe, recipe list, requirements, output expectations, quality forecast, worker/operator/utility requirements, progress, blockers, routing, queue controls, and Infopedia links.

Exit criteria:

The player can tell what a machine is doing, why it is blocked, what it needs, where output goes, and whether changing workers/materials/knowledge would improve production.

### Phase 4.24 - Construction and blueprint management interface pass

Construction UI should organize blueprints, rooms, machines, utilities, defenses, furniture, vehicles, farms, pet/animal rooms, faction rooms, and restricted assets into understandable menus. It should include category tree/tabs, search/filter, blueprint detail panes, required materials/tools/workforce/time, placement constraints, access/license/permit/reputation restrictions, heat/suspicion warnings, preview/ghost controls, invalid-placement reasons, confirm/cancel behavior, and Infopedia links.

Exit criteria:

Construction menus are organized enough that the player can find what they want to build, understand why they can or cannot build it, and place it without raw blueprint IDs or silent failures.

### Phase 4.25 - Construction menu category organization pass

Construction categories should scale to future content. Candidate categories include rooms, walls/doors/structural elements, floors/surfaces, utilities, machines, storage, furniture/fixtures, defenses, agriculture/gardens, animal/pet rooms, medical/clinic rooms, markets/vendors, vehicle facilities, faction/civic rooms, noble/estate rooms where unlocked, illicit/hidden rooms where unlocked, and blueprints/plans.

Exit criteria:

Construction menus scale to hundreds of future buildables without becoming a flat alphabetical wall.

### Phase 4.26 - Game menu uniformity and release-readiness audit pass

All ordinary game menus should be audited for uniformity: main menu, options, controls/rebinding, character creation, character dossier, knowledge/skill progression, conversation, trade/barter/services, inventory/equipment, container transfer, base inventory/storage, faction member roster, faction member inventory/equipment, production/machine management, construction/build/blueprint management, movement planning, quest log, map/intel, Infopedia, save/load, death/loss screen, and developer/audit surfaces where exposed.

Exit criteria:

No high-traffic menu remains an unreviewed island with inconsistent close behavior, uncontrolled scrolling, hidden failure reasons, placeholder labels, missing prompt updates, or raw implementation leakage.

## Phase 14 - Body-plan, injury, combat-readiness, and condition readability

### Phase 14.1 - Body-plan and condition approximation pass

Examination should show body-plan approximations and current condition estimates for humanoids, animals, creatures, machines, and vehicles without requiring perfect numbers. The player should be able to judge whether a target looks healthy, injured, dangerous, disabled, weak, heavily armored, or likely beyond their current ability.

Exit criteria:

Body and condition information supports decision-making without raw stat leakage.

### Phase 14.2 - Medical and narcotic readability pass

Medical states, treatments, drugs, narcotics, sedatives, stimulants, painkillers, prosthetics, and cybernetics should be explained through inspection and Infopedia entries where player-facing. The UI should distinguish injury, pain, bleeding, infection, poisoning, sedation, stimulation, addiction/dependency risk where implemented, missing treatment, and counterfeit/contaminated/restricted medicine where known.

Exit criteria:

Medical and drug systems are not hidden status-effect soup; they can be inspected and explained.

## Phase 15 - Biography, identity, social context, and roster readability

### Phase 15.1 - Entity biography and identity pass

The examination screen should consume character and social identity systems when information is known or discoverable. It may show name, alias, faction, rank, role, workplace, home room, relationship to local rooms/factions/contracts/pets/vehicles/schemes/noble houses/vendors/markets, reputation/rumors, quest relevance, ownership/access relevance, and confidence level.

Exit criteria:

The examination screen does not treat all entities as blank stat blocks.

### Phase 15.2 - Rank, faction, and role label hygiene pass

Rank, faction, role, and profession labels should be compact and player-facing. Ordinary UI should not show raw faction keys, enum names, package strings, debug labels, or unlocalized constants.

Exit criteria:

Social identity is readable and diegetic enough to support decisions without debug leakage.

### Phase 15.3 - Roster and personnel label hygiene pass

Roster-style UI should use readable personnel labels and compact status summaries: name, role, rank/command tier, assignment, warning status, location/workplace, and availability where known. It must not show raw actor IDs, UUIDs, package names, hidden AI state, or debug personality keys.

Exit criteria:

Faction, base, crew, worker, and personnel rosters are readable at a glance and safe to expose.

## Phase 17 - Quest, ownership, pet, faction, economy, trade, movement, input, and inventory readability

### Phase 17.1 - Quest target and evidence readability pass

Examination and quest UI should identify quest targets and proof-of-death evidence where appropriate without revealing hidden identities or outcomes unless a check, record, or investigation path supports it.

Exit criteria:

Quest guidance supports player action without collapsing mystery or hidden systems into free omniscience.

### Phase 17.2 - Economy, legality, and market readability pass

Vendors, markets, and trade goods should explain legal, restricted, illicit, stolen, counterfeit, contaminated, noble-owned, military-controlled, black-market, vault-only, not-for-sale, license-gated, reputation-gated, invitation-gated, scarce, or blockaded states in player-facing language.

Exit criteria:

The player can understand why a market has, lacks, restricts, or refuses goods.

### Phase 17.3 - Pet ownership and care readability pass

Pet entities and pet UI should explain ownership, sale/adoption status, hunger, thirst, shelter, neglect, fear, hostility, friendliness, accessibility, and whether petting is allowed or blocked.

Exit criteria:

Pet systems feel like visible living systems rather than hidden timers or inert sprites.

### Phase 17.4 - Player expansion heat readability pass

As the player grows from individual survivor to faction-scale actor, UI and Infopedia systems should explain heat and suspicion drivers such as rooms, vehicles, production machines, defenses, restricted stockpiles, shops, clinics, labs, armories, motor pools, and illegal/military blueprints.

Exit criteria:

Faction attention does not feel arbitrary when player construction and ownership start raising heat.

### Phase 17.5 - Transfer workflow consistency pass

Inventory, trade, container, machine, base storage, faction member inventory, vehicle cargo, and quest/evidence interfaces should use compatible transfer rules: source, target, quantity, permission check, capacity check, quest protection, ownership/stolen/restricted warning, confirm/cancel, reversible action where supported, and clear failure reason.

Exit criteria:

The player does not encounter a different invisible transfer system in every menu that moves items.

### Phase 17.6 - Movement-to-interaction usability pass

Movement planning should support interaction-driven positioning. If the player selects an entity, container, door, machine, pet, vendor, vehicle, or construction target that requires adjacency or exact range, the UI should help place the movement ghost near a valid interaction tile where possible.

Exit criteria:

The player can approach interaction targets through deliberate movement planning rather than trial-and-error bumping.

### Phase 17.7 - Context-sensitive input prompt pass

Quest, trade, inventory, movement, construction, vehicle, conversation, and combat surfaces should display prompts based on the current input profile and current context.

Prompt behavior should include:

- Current binding display for common actions.
- Controller glyph or text fallback.
- Keyboard/mouse fallback.
- Prompt updates after rebinding.
- Context-aware prompt suppression to reduce clutter.
- Disabled-action prompts with clear reason where useful.

Exit criteria:

The player sees prompts that match their actual bindings and current activity rather than hardcoded default controls.

## Phase 18 - Infopedia, hot links, editor, menu definitions, movement definitions, input definitions, and audit surfaces

### Phase 18.1 - Infopedia hot-link and deep reference pass

The Infopedia should support clickable or otherwise navigable references. Entries should explain what a thing is, what it does, important values/stat ranges/quality ranges, where it is made, what it is made from, what it can make, quest/contract/scheme references, warnings, access rules, ownership issues, and related entries.

Exit criteria:

The Infopedia becomes a true player reference system rather than a shallow glossary.

### Phase 18.2 - Mechanics and options Infopedia coverage pass

Every significant exposed mechanic should eventually have an Infopedia entry, including Look, Examine, movement planning, movement ghost target placement, input rebinding, keyboard/mouse controls, controller/gamepad controls, control profiles, intent checks, quest arrows/highlights, proof-of-death evidence, zero-weight quest items, pets, conversation, trade, services, inventory, equipment, transfer, storage, base inventory, faction member inventory, production, construction, heat, provenance, markets, knowledge quality, skills, medicine, vehicles, Ages of Control, and leadership schemes.

Exit criteria:

A player can look up the systems the UI exposes instead of guessing from scattered tooltips.

### Phase 18.3 - Examination and Infopedia editor/audit pass

Editor/audit surfaces should inspect examine sections, biography fields, body-plan templates, stat approximation bands, intent rules, refresh cadence, skill modifiers, visibility/distance modifiers, information confidence states, Infopedia entries, links/backlinks, categories, item values, recipe references, and mechanic explanation entries.

Exit criteria:

Examine and Infopedia behavior can be audited or extended through owned data/editor surfaces instead of hardcoded central-class sprawl.

### Phase 18.4 - Menu definition and uniformity audit pass

Editor/audit surfaces should define and inspect menu ID/title, owning authority, open/close/back behavior, primary purpose, data source, panes, actions, disabled-state rules, confirmations, links, search/filter/sort, transfer behavior, permission checks, failure strings, raw-ID leakage, text wrapping, and scroll containment.

Exit criteria:

Major game menus can be audited as owned UI definitions rather than only by visual inspection and memory.

### Phase 18.5 - Movement planning editor/audit pass

Editor/audit surfaces should define and inspect movement-planning UI behavior.

Audit fields should include:

- Movement mode name and player-facing label.
- Ghost target asset/overlay.
- Ghost placement input methods.
- Confirm/cancel bindings.
- Valid target rules.
- Invalid target feedback strings.
- Path preview behavior.
- Hazard warning behavior.
- Interaction-adjacency helper behavior.
- Controller/gamepad nudge behavior where implemented.
- Overlay priority against quest, combat, selection, ownership, and debug markers.
- Save/load and focus-reset expectations.

Exit criteria:

Movement ghost placement can be audited as a real UI/input system rather than hidden keypad behavior.

### Phase 18.6 - Input rebinding editor/audit pass

Editor/audit or diagnostics surfaces should inspect input definitions and player profiles.

Audit fields should include:

- Action ID and player-facing action label.
- Context category.
- Default keyboard binding.
- Default mouse binding.
- Default controller binding.
- Alternate bindings.
- Required/unrequired state.
- Current profile binding.
- Conflict set.
- Prompt text.
- Glyph asset or fallback label.
- Rebindability flag.
- Hold/tap behavior.
- Deadzone/sensitivity/inversion values.
- Save/load source.
- Last-good profile source.
- Reset behavior.

Exit criteria:

Input rebinding can be audited as owned data instead of scattered raw key listeners and button checks.

## Phase 19 - Release audit

Before release claims for this milestone, verify:

- `Look` and `Examine` are distinct but connected.
- `Examine` hides raw IDs and uses intelligence/relevant checks for information quality.
- Intent/state estimates refresh after several turns or repeated examination.
- Player movement supports manual movement ghost target placement, not only keypad double-auto prediction.
- Movement ghost placement uses shared viewport/targeting transforms.
- Movement preview validates destination/path and explains invalid or risky targets where feasible.
- Movement ghost overlay is distinct from quest, selection, combat, ownership, and debug markers.
- Quick directional movement and planned movement do not disagree about validation rules.
- Keyboard and mouse rebinding exists where controls are exposed.
- Controller/gamepad rebinding exists where controller support is exposed.
- Required actions cannot be left unrecoverably unbound.
- Rebinding UI detects same-context and dangerous cross-context conflicts.
- Reset-to-default and restore-last-good profile recovery exist.
- Controller deadzone, sensitivity, and axis inversion are available where analog axes are supported.
- Prompts reflect current bindings and fall back cleanly when glyph assets are missing.
- Input settings persist through save/load or settings reload as appropriate.
- Quest objective arrows and pulsing highlights are useful, not clutter.
- Pet interaction feedback exists for pettible non-hostile pets where implemented.
- Access, construction, ownership, interaction, and movement denial messages explain the actual reason.
- Conversation menus have readable speaker, faction, choice, service, quest, and exit structure.
- Trade menus explain stock, price, legality, restriction, provenance, service effects, and unavailable goods.
- Inventory and equipment menus show useful item details, quality, condition, legality, quest status, and transfer actions.
- Base inventory and container menus preserve shared transfer semantics and permission checks.
- Faction member management menus separate player command slots from NPC command rosters where required.
- Faction member inventory/equipment menus respect authority, ownership, rank, privacy, and task restrictions.
- Production/machine management menus show recipe, input, output, worker, utility, quality forecast, blocker, queue, and routing state where implemented.
- Construction/blueprint menus are organized by readable categories, expose placement constraints, and explain locked/unavailable entries.
- All high-traffic game menus share consistent title, close/back, list, detail, action, confirmation, disabled-state, scroll, prompt, and hot-link behavior where practical.
- Infopedia entries exist for major exposed mechanics, interactions, movement systems, input/rebinding systems, and options added by recent roadmap work.
- Developer/audit surfaces may show semantic IDs, but ordinary player UI does not.
- Player-facing text is compact, clear, and free of placeholder labels.

Exit criteria:

The game may claim in-depth examination, deep Infopedia reference, movement usability, input rebinding readiness, quest guidance, pet interaction feedback, management UI readiness, and player-readable mechanics only after these systems are inspectable, useful, compact, uniform, recoverable, configurable, and free of ordinary debug leakage.

## Non-goals for this milestone

This milestone does not require every hidden system to be revealed to the player. It requires visible information, uncertainty, denial reasons, and reference material to be useful and honest.

This milestone does not complete all UI implementation. It defines the readability, movement, input rebinding, examination, Infopedia, and menu-uniformity targets that later UI implementation must satisfy.

This milestone does not require every item/entity/mechanic/menu/input action to have a perfect final entry immediately. It requires exposed systems to move toward Infopedia coverage, shared menu grammar, movement-input consistency, configurable controls, and hot-linked player reference instead of remaining undocumented or isolated.

This milestone does not authorize external detached utility windows for normal play. Ordinary movement, conversation, trade, inventory, production, construction, faction, controls, and management UI should remain game-owned unless the user explicitly orders a developer-only tool.

## Deferred checkpoint summary

The player should have a dedicated `Examine` option beyond ordinary `Look`. `Look` gives immediate impressions and a surface intent read. `Examine` opens a deeper character-screen-like view that uses intelligence, relevant skill, visibility, distance, prior knowledge, tools, and time spent observing to estimate biography, state, body plan, stats, injury, intent, equipment, faction, role, pet status, ownership, quest relevance, and danger. Examination should refresh every few turns or through continued attention so stale intent reads can change and careful observation can reveal more.

The player movement system needs a deliberate usability pass. The movement ghost should not only be driven by keypad double-auto ghost prediction. The player should be able to manually invoke movement planning, choose the target location, place the movement ghost at the desired endpoint of the movement chain, preview or understand the path/route/direction where feasible, and receive clear invalid-target or hazard feedback before committing. Quick movement may remain, but precision movement must be available for careful positioning, hazards, doors, line of sight, interaction range, and tight spaces.

Input rebinding needs full depth. Keyboard, mouse, keypad, controller, and gamepad controls should flow through named actions, context maps, profiles, conflict detection, safe recovery, prompt updates, controller glyph/text fallback, deadzones, sensitivity, axis inversion, hold/tap behavior, and settings persistence. The player must be able to change controls without losing the ability to move, confirm, cancel, open menus, or recover defaults.

The Infopedia should become the long-term reference layer for the game's mechanics and interactions. Every significant exposed system should eventually have an Infopedia entry. Entries should be hot-linked so clicking a referenced item, entity, mechanic, room, machine, faction, status, blueprint, recipe, movement rule, or input rule jumps to its entry. Item entries should explain what the item is, what it does, its values and statistics, quality-level ranges, where it is made, what it is made from, and what it can be used to make.

All major user interfaces should receive a deliberate trimming, checking, and guideline pass. Conversation, trade, inventory management, base inventory management, faction member management, faction member inventory management, production management, construction management, construction menu organization, movement planning, controls/rebinding, and all other ordinary game menus should share one readable game-owned UI grammar. Menus should have clear purpose, compact title/header behavior, bounded text, reliable close/back controls, useful disabled-state explanations, Infopedia links where helpful, shared transfer semantics where items move, shared targeting semantics where the player places ghosts/targets, current-binding prompts where actions are displayed, and no raw implementation leakage in ordinary player-facing surfaces.

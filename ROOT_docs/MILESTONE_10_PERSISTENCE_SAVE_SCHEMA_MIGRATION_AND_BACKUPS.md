# Milestone 10 - Persistence, Save Schema, Migration, and Backups

This ordered milestone document consolidates planning for save ownership, world-file versus player-file separation, profile/settings persistence, save schema versioning, migrations, backups, autosaves, import/export, deterministic restore, multiplayer/server save rules, editor/content save safety, and validation of persistence for every gameplay system added by the milestone chain.

`MASTER_DEVELOPMENT_PLAN.md` remains the authoritative roadmap, active checkpoint, phase list, and dependency map. This file is an ordered phase-aligned implementation target for the persistence/save-schema/migration/backup slice of that roadmap.

This document is not a changelog. Completed implementation belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

## Why this milestone exists

The ordered milestone chain now includes large durable systems: assets, UI, movement, input, production, knowledge, population provenance, faction markets, deferred simulation, world events, construction, vehicles, schemes, quests, medicine, worldgen, editors, and modding. Every one of those systems creates persistent state.

Without a dedicated persistence milestone, the project risks losing state across saves, splitting state into the wrong file, producing stale references, duplicating ownership records, corrupting saves after schema changes, or making single-player and server saves diverge.

Persistence is not a final packaging detail. It is a cross-cutting architecture layer. Every milestone must know what it saves, where it saves, how it migrates, and how it is validated.

## Core doctrine

Save ownership must follow world truth.

The **world save** stores the current shared world state. Anything that exists as part of the world, belongs to the world, affects the world, or should remain true if the current player character dies and another character enters the same world belongs in the world save.

The **player save** stores the current player-character state and personal run state. Anything that is specific to the character's body, personal condition, personal carried inventory, personal knowledge, personal skills, personal quest log view, and character-specific UI/state belongs in the player save.

The **profile/settings save** stores machine/user preferences, controls, video/audio options, accessibility preferences, launcher preferences, account/profile-level preferences, and non-world UI settings.

The **launcher/cache/manifest state** stores update manifests, payload hashes, install/cache status, selected channel, rollback records, and local package-management records. It must not be confused with world state or character state.

The **editor/content state** stores authored definitions, not live save state. Editor-authored assets, items, rooms, quests, factions, vehicles, objects, features, recipes, world events, and mod content should become versioned content definitions that saves reference by stable IDs and content version where needed.

## Hard separation rule

World file examples:

- Faction relations between factions.
- Player-created faction records.
- Faction ownership of rooms, vehicles, facilities, machines, containers, and assets.
- Player-owned rooms, bases, shops, vehicles, factories, defenses, and constructed assets, because they exist in the world.
- World-generated rooms, zones, sectors, facilities, hazards, roads, import nodes, and worldgen history.
- World item locations and container contents, except personal carried/equipped inventory owned by the active player character.
- Faction member rosters and imported reinforcement records.
- Population provenance and aggregated population ledgers.
- Item provenance for world items.
- Raw material source provenance and shipment records.
- Deferred out-of-sector simulation ledgers.
- Top-down world events and their active modifiers.
- Faction schemes, Ages of Control, active world events, room-control changes, journals, intelligence documents, and scheme consequences.
- Quest world state such as spawned quest objects, target death state, proof-of-death evidence, stolen journals, room objective state, and event consequences.
- Vehicles, vehicle component damage, cargo, ownership, repair state, motor-pool assignment, and strategic asset ledgers.
- Medical clinics, stored medicine, treatment facilities, and world medical service state.
- Construction projects and queued world work.
- Production queues and machine state.
- Vendor stock, market state, prices, event restrictions, and local commerce state.
- Pets and animals that exist in the world, including ownership and care state.
- Save-slot world metadata, world seed, world schema version, content version locks, and migration history.

Player file examples:

- Player character identity.
- Player body state, injuries, pain, infection, shock, narcotic effects, prosthetics, cybernetics, hunger, thirst, sleep, and other character-specific condition state.
- Player carried inventory and equipped items.
- Player personal knowledge tree and skill tree progression.
- Player personal discovered information, notes, known rumors, known Infopedia unlocks where character-specific.
- Player personal quest log, accepted obligations, active objective view, and character-specific quest progress flags.
- Player map memory/fog-of-war memory where character-specific.
- Player personal reputation or standing only when it belongs to the character personally rather than the player's faction or world faction record.
- Player personal heat/suspicion only where it is character-specific. Faction-scale heat caused by owned rooms/assets belongs in the world save.
- Player personal UI state that should follow the character but not the machine profile.

Profile/settings examples:

- Keyboard/mouse/controller bindings.
- Control profiles.
- Deadzones, sensitivity, axis inversion, hold/tap preferences.
- Audio/video/display settings.
- Accessibility settings.
- UI scale and prompt display preferences.
- Last selected save slot where appropriate.
- Launcher channel selection and package preference if not tied to world content.

Launcher/cache/manifest examples:

- Installed client/server payload versions.
- Payload hashes.
- Download cache records.
- Rollback manifests.
- Launcher diagnostics.
- Package verification state.
- Selected update channel.
- Support library/runtime installation state.

## Special player/faction rule

Player-created faction information belongs in the world save because the faction exists in the world.

Owned assets belong in the world save because rooms, bases, vehicles, shops, storage, machines, pets, animals, production chains, and constructed facilities remain world objects. They should not disappear because the current character file is replaced.

The player file may store the current character's relationship to that faction, authority, rank, personal standing, personal permissions, and personal command access, but the faction itself, its assets, roster, territory, ledgers, and world footprint belong to the world.

## Single-player and server save doctrine

Single-player should be allowed to save a large snapshot of the entire world state at save time. This is acceptable because single-player has no external clients requiring live authoritative sync.

Server/multiplayer world saves should treat the server as the authority. The server saves current authoritative world state to itself and sends snapshots/deltas to clients for display and play. Player files may be separate from world files, but server-owned world truth must not be overwritten by stale client state.

The same schema rules should apply to both modes wherever possible, but authority differs:

- Single-player may run local world authority and save local snapshots.
- Server mode owns authoritative world state and player connection/session state.
- Clients receive snapshots, not ownership of world mutation.

## Save schema versioning

Every save file should include:

- Save type: world, player, profile/settings, launcher/cache, editor/content export, or backup bundle.
- Schema version.
- Game version.
- Content definition version/hash where relevant.
- Mod/content pack set and versions where relevant.
- Created timestamp.
- Last saved timestamp.
- World ID or player ID where relevant.
- Migration history.
- Checksum/integrity marker.

Exit criteria:

The project can identify what kind of save it is loading and whether migration or rejection is required.

## Migration doctrine

Save schemas will change. Migration is mandatory for a project this large.

Migrations should be:

- Versioned.
- Ordered.
- Idempotent where possible.
- Logged.
- Backup-first.
- Validated after application.
- Able to reject unsupported or unsafe saves with a readable message.
- Able to preserve unknown modded data where safely possible.
- Able to quarantine incompatible content references rather than crash.

No migration should silently discard world-owned state, player-owned state, owned assets, faction records, quest state, provenance records, vehicles, or editor-authored content without an explicit migration rule and log entry.

## Atomic save and backup doctrine

Saves should be written atomically where possible.

Required safeguards:

- Write to temporary file first.
- Flush/close safely.
- Validate written data.
- Rename/swap into place after validation.
- Preserve previous save until new save is confirmed.
- Create backups before migration.
- Avoid saving during unsafe shutdown, interrupted update, or package replacement.
- Server updates must not self-replace while saving or while clients are connected unless safe shutdown/restart is confirmed.

Backups should support:

- Manual backups.
- Autosave backups.
- Pre-migration backups.
- Pre-update backups.
- World backup bundles.
- Restore-from-backup path.
- Human-readable metadata.

## Autosave doctrine

Autosaves should exist but must not be destructive.

Recommended autosave triggers:

- Zone transition.
- In-game hourly interval where configured.
- Major quest state transition.
- Construction completion.
- Vehicle acquisition/loss.
- Faction scheme major transition.
- World event start/end.
- Manual save request.
- Safe server checkpoint.

Autosave retention should preserve multiple recent autosaves rather than only one overwrite.

## Data ownership by milestone

### Milestone 01 - Assets, Mod API, and content definitions

Persistent definitions:

- Semantic asset registry state.
- Content definition versions.
- Optional pack manifests.
- Mod API/example mod content definitions.
- Publish-safe asset statuses.

Save references:

- World/player saves reference semantic IDs and content versions, not raw paths.
- Missing assets fall back through audited semantic fallback rules.

### Milestone 02 - UI, input, movement, and readability

Profile/settings save:

- Input bindings.
- Control profiles.
- Controller settings.
- UI scale, prompt preferences, accessibility settings.

Player save where character-specific:

- Known information, discovered Infopedia entries, personal quest-log view, map memory where character-bound.

World save where world-owned:

- Objective markers tied to spawned world objects, quest evidence, room targets, and world-side quest state.

### Milestone 03 - Production, knowledge, skills, and quality

Player save:

- Player knowledge and skill progression.
- Character-specific fabrication knowledge.

World save:

- Machine queues.
- Production facilities.
- Facility knowledge where owned by factions/world organizations.
- Item quality/provenance for world items.
- Faction production mutation state.

### Milestone 04 - Population, economy, markets, deferred simulation, and world events

World save:

- Population provenance.
- Faction markets.
- Vendor stock.
- Item provenance.
- Raw material provenance.
- Shipments/imports/exports.
- Reinforcement timers and imported groups.
- Deferred out-of-sector probability ledgers.
- Top-down world events and modifiers.

Player save:

- Character-specific known rumors and discovered market information where appropriate.

### Milestone 05 - Construction, ownership, blueprints, and parity

World save:

- Constructed rooms.
- Owned bases.
- Ownership of rooms/assets/vehicles/containers/machines.
- Permits/licenses tied to world entities or player-created factions.
- Construction projects.
- Player-created faction assets.
- Faction-owned blueprints and construction rights.

Player save:

- Character-carried blueprint copies where personal.
- Personal construction skill/knowledge unlocks.

### Milestone 06 - Vehicles and structural scale

World save:

- Vehicles.
- Vehicle ownership.
- Vehicle component state.
- Cargo.
- Damage/repair state.
- Motor-pool assignments.
- Strategic vehicle ledgers.
- Wrecks and salvage.

Player save:

- Character-specific vehicle operation permissions only where personal rather than faction/world-owned.

### Milestone 07 - Ages, schemes, quests, and Quest Editor

World save:

- Ages of Control history.
- Faction schemes.
- Active scheme state.
- Leadership journals and intelligence documents.
- World-side quest objects.
- Quest target death/evidence state.
- Room-control changes.
- Intelligence sale consequences.

Player save:

- Accepted personal quests.
- Personal quest log view.
- Personal choices/obligations where character-specific.
- Character-known intelligence where not yet externalized into the world.

### Milestone 08 - Medical, cybernetics, narcotics, and body systems

Player save:

- Player body state.
- Injuries.
- Medical conditions.
- Narcotic effects.
- Addiction/dependency where implemented.
- Prosthetics/cybernetics installed on the player.

World save:

- Clinics.
- Medical stock.
- Treatment queues.
- NPC injuries where NPCs persist in world state.
- Corpses, evidence, and medical facilities.

### Milestone 09 - Worldgen, rooms, districts, facilities, objects, entities, and features

World save:

- Generated zones.
- Rooms and districts.
- Object placement.
- Feature placement.
- Entity placement where persistent.
- Hazards.
- Containers.
- Facilities.
- Room ownership and mutation history.
- Worldgen seed/settings/history.

### Milestone 10 - Persistence, migration, and backups

Persistent across save layers:

- Save schema versions.
- Migration rules.
- Save/load validation.
- Backups.
- Autosaves.
- Restore paths.
- Integrity checks.
- World/player/profile separation rules.

## Industry-standard gaps captured by this milestone

The following are treated as required planning lanes before publish-safe claims involving saves:

- Explicit data ownership boundaries.
- Schema versioning.
- Forward migration.
- Backward rejection with readable errors.
- Atomic write patterns.
- Backup-before-migration.
- Autosave retention.
- Integrity checks.
- Corruption detection.
- Mod/content version pinning.
- Missing content fallback/quarantine.
- Deterministic restore/smoke tests.
- Save/load performance budgets.
- Server-authoritative save behavior.
- Client snapshot separation.
- Player/profile/world/cache separation.
- Editor content definition versioning.
- Privacy/redaction for logs and diagnostics.

## Save/load validation pass

Every major feature should define save/load tests.

Validation should include:

- Save before feature change.
- Load after feature change.
- Save after feature change.
- Reload and compare expected state.
- Migrate old schema where applicable.
- Backup before migration.
- Detect missing content references.
- Confirm no world-owned state moved into player file incorrectly.
- Confirm no player body/inventory state moved into world file incorrectly.
- Confirm no settings/control profile state moved into world save incorrectly.

## Stale reference and ID safety pass

Saves must be resilient against stale references.

Potential stale references:

- Deleted room.
- Deleted entity.
- Dead actor.
- Moved item.
- Removed mod content.
- Missing semantic asset.
- Changed faction ID.
- Old quest target.
- Expired world event.
- Missing vehicle component definition.
- Replaced recipe/machine definition.

Handling should include:

- Stable IDs.
- Reference validation.
- Quarantine/repair where safe.
- Readable error where not safe.
- Audit log.
- Migration rule if recurring.

## Save-file privacy and diagnostics

Save diagnostics and logs should avoid exposing private or stream-sensitive values unnecessarily.

Redaction targets:

- IP/server address.
- Passwords.
- Tokens.
- Local filesystem user paths where possible.
- Account/profile identifiers where not necessary.
- Raw crash details that include sensitive environment variables.

Developer/audit mode may reveal technical identifiers, but ordinary UI and public logs should remain safe.

## Release audit

Before release claims involving persistence, verify:

- World save and player save boundaries are documented and tested.
- World-owned player-created factions and owned assets save to world files.
- Player body, carried inventory, skills, knowledge, and personal quest state save to player files.
- Input bindings and UI settings save to profile/settings files.
- Launcher/cache/update state does not pollute world/player saves.
- Save schema version exists.
- Migrations are versioned and logged.
- Backups are created before migration.
- Atomic save behavior exists where practical.
- Autosaves do not overwrite the only recovery path.
- Save/load smoke tests cover touched systems.
- Missing content references are handled without crashing where possible.
- Server-authoritative saves are not overwritten by stale client state.
- Public diagnostics redact sensitive fields.

Exit criteria:

The game may claim persistence readiness only when world, player, profile, launcher/cache, and editor/content state have clear ownership boundaries, versioning, migration, validation, backup, and recovery behavior.

## Non-goals

This milestone does not require every save format to be final before gameplay work continues.

This milestone does not require every distant actor or item to be saved individually when deferred ledgers are the correct representation.

This milestone does not make the player file own world factions, player-created faction assets, bases, rooms, vehicles, or world construction.

This milestone does not allow client-side multiplayer snapshots to overwrite server authority.

## Deferred checkpoint summary

Everything meaningful must survive save/load in the correct file. World truth belongs in the world save. Player character truth belongs in the player save. Control/UI preferences belong in profile/settings. Launcher/update state belongs in launcher/cache manifests. Editor-authored definitions belong in versioned content definitions. Owned assets, player-created faction records, faction relations, world events, deferred simulation ledgers, quests, rooms, vehicles, markets, and construction belong to the world when they are part of the shared world. Player body, carried inventory, personal knowledge, personal skills, and character-specific accepted quest state belong to the player. Save schemas require versioning, migration, backups, atomic writes, corruption detection, stale-reference handling, and validation before publish-safe claims.
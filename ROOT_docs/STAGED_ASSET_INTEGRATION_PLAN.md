# The Mechanist — Staged Semantic Asset Integration Plan

This file is the durable migration checklist for the asset-loading overhaul. It exists because the asset path problem is broad enough to need its own stage gate: if we try to replace every image path at once, we will break rendering, previews, icons, tile descriptors, and Infopedia auditing in a single blast radius.

This plan must be read before any asset-loader, item-icon, tile-art, portrait, machine-art, or Infopedia-asset pass. It is subordinate to `STANDARDS_AND_PRACTICES.md`, aligned with `MASTER_GOVERNANCE_REVISION_II.md`, and scheduled through `MASTER_DEVELOPMENT_PLAN.md` Phase 2.

## Permanent context reminders

- The asset migration is an in-game systems migration, not a detached utility-window project. Player-facing asset browsing belongs inside the main game Infopedia/menu surface.
- `ROOT_SRC_assets/` is preserved source material. Do not modify those files in place.
- Runtime-ready outputs belong in the consuming package tree, especially `PACKAGE_client/assets/` for client game assets and `PACKAGE_launcher/java/src/main/resources/assets/` for launcher assets.
- Do not use docs or manifest files as a substitute for physical placement. A package manifest may verify or acquire a payload, but the asset itself must live where the runtime architecture consumes it.
- New graphical references should resolve through a semantic ID, not fragile direct path calls.
- The target public handle is an exact 8-character ID such as `TILE-A01`, `OBJ-WB01`, `WEAP-K01`, or `MACH-A01`.
- The semantic registry must describe what the asset means, not only where the file lives. The Infopedia needs name, type, preview, ID, and in-universe description.
- Runtime code may keep low-level loader paths inside approved authority classes. Gameplay/UI code should not scatter raw asset file paths.
- Java 17 remains the release target. Any code stage must compile with `--release 17`, rebuild jars, run classfile major-version scans, and package only after the release gate passes.
- Existing art and tile descriptors are not discarded. The registry should first wrap and explain them, then gradually become the routing authority.
- Every stage must leave the game runnable and inspectable. If an asset is not migrated yet, it may keep its legacy path only through an explicit bridge/fallback, not through new scattered references.

## Current migration state

Status after Stage 9 implementation: the backend registry, InfoPedia asset browser, high-error indexing, item/UI preview migration, tile descriptor migration, object/fixture/construction/editor-palette migration, direct graphical path audit, and portrait/entity partitioning are in place. Stage 9 hardens the migrated categories by adding typed missing-art fallbacks, narrowing silent legacy fallback behavior for item/object/tile preview surfaces, and recording the remaining intentionally deferred legacy graphical boundaries.

Immediate next implementation target: **Stage 10 — mod and art-pack semantic registry extension**.

Probable next target after Stage 10: reassess Phase 2 exit criteria, then polish tools such as editor-assisted asset description authoring, atlas packing, localization keys, and high-resolution art-pack handling.

## Stage 0 — Durable plan creation and containment gate

Purpose: create this durable migration path so future asset work does not drift into ad-hoc filename fixes.

Operable actions:

- Create this staged asset integration file.
- Update the master plan so Phase 2 points to this file as the active asset-migration checklist.
- Update standards so the semantic ID registry becomes the required direction for new graphical references.
- Do not modify rendering behavior in this stage.
- Do not begin path replacement until the registry foundation exists.

Completion definition — what should have just been completed:

- This file exists in the project.
- The active stage is clearly marked as Stage 1.
- The file explains why staged migration is required and what must happen next.
- Documentation records that this is a user-ordered exception to the normal four-file docs containment rule.

Immediate next action after Stage 0:

- Implement Stage 1 registry foundation with model, loader, cache, fallback icon, starter registry file, and smokes.

Likely next stage after that:

- Stage 2, the in-game Infopedia asset-browser surface.

## Stage 1 — Semantic Asset Registry Foundation — COMPLETE IN 0.9.10jt

Purpose: establish the backend authority for asset IDs without disturbing the existing rendering pipeline.

Operable actions:

- Add an immutable Java 17 `AssetMetadata` record containing ID, path/URI, name, asset type, and semantic description.
- Add an `AssetType` enum with at least: `PORTRAIT`, `WALL_TILE`, `OBJECT`, and `MACHINE`; likely also include floor, road, sidewalk, corridor, item icon, weapon icon, armor icon, UI icon, fixture, corpse/decay, and unknown/internal categories.
- Add `AssetRegistry`, able to load a machine-readable registry file from `PACKAGE_client/assets/indexes/` or an equivalent approved package data path.
- Add `AssetManager.getAsset(String assetId)` as the central cached image-access method.
- Add a generated or hand-authored missing-asset fallback image/icon.
- Add a starter semantic registry file with the high-error assets first: water barrels, supply shelves, cots, scrap knife, bolter/ranged weapons, roads, sidewalks, walls, maintenance corridors, corpse tiles, and key machine/object previews.
- Add smokes for ID format, duplicate IDs, blank descriptions, valid types, fallback behavior, cache behavior, and path existence for the starter registry.
- Do not migrate world rendering, item rendering, or tile descriptors yet except for optional low-risk smoke/demo calls.

Completion definition — what should have just been completed when Stage 1 is done:

- `AssetMetadata`, `AssetType`, `AssetRegistry`, and `AssetManager` compile under Java 17.
- The starter registry loads without duplicate IDs.
- Every starter entry has a non-empty name, type, path, and semantic description.
- `AssetManager.getAsset(validId)` returns the expected cached icon.
- `AssetManager.getAsset(invalidId)` returns the missing-asset fallback without throwing.
- The shipped jar/classfile gate still reports Java 17 major version 61 or lower.
- Existing gameplay rendering still uses the old routes where not yet migrated.

Immediate next action after Stage 1:

- Build the in-game Infopedia asset index screen/tab on top of the registry. This should consume `AssetManager.registry()` / `AssetRegistry` and display the starter rows before any large migration occurs.

Likely next stage after that:

- Stage 2, then Stage 3 high-error category indexing.

Stage 1 completion note:

- Completed as 0.9.10jt with 26 starter entries across 13 categories. No old world rendering paths were migrated in this stage; the registry wraps available art and proves the ID/cache/fallback path.

## Stage 2 — Internal Infopedia Semantic Asset Browser — COMPLETE IN 0.9.10ju

Purpose: expose the registry through the in-game Infopedia so asset meaning, ID, preview, and description can be audited without opening external tools.

Operable actions:

- Add an in-game Infopedia asset-index view owned by the main game UI surface.
- Organize assets by type/purpose: Portraits, Wall Tiles, Floor Tiles, Road/Sidewalk/Corridor Tiles, Objects, Machines, Items/Weapons/Armor, UI Icons, and Hidden/Internal if needed.
- Add search/filter by ID, name, type, and semantic description.
- When a row is selected, show the sprite preview, exact 8-character ID, name, type, path/alias, and semantic description.
- Keep text and preview areas bounded inside their frames.
- Do not use detached `JFrame` or `JDialog` surfaces.

Completion definition — what should have just been completed when Stage 2 is done:

- The Infopedia has a registry-backed asset index page/tab.
- Selecting an asset updates preview and description through the registry.
- Search works across ID, name, and semantic description.
- Missing/bad assets are visible as missing assets instead of crashing.
- The UI is in-game owned and follows the existing frame/input/render model.

Immediate next action after Stage 2:

- Expand the registry around the high-error categories that have already produced bad icon/preview assignments.

Likely next stage after that:

- Stage 3 high-error asset category indexing.

Stage 2 completion note:

- Completed as 0.9.10ju. The game-owned Infopedia has an `ASSETS` tab. It displays semantic asset entries from `AssetManager.registry()`, supports type filtering and text filtering, renders previews through `AssetManager.getAsset(id)`, shows exact IDs/path/type/descriptions, and has smoke coverage through `InfoPediaSemanticAssetBrowserSmoke`. No item/tile/world render migration was performed in this stage.

## Stage 3 — High-error category indexing and semantic reconciliation — COMPLETE IN 0.9.10jv

Purpose: index the assets most likely to be confused by the current loose path/alias system before broad migration.

Operable actions:

- Create registry coverage for water barrels versus supply shelves.
- Create registry coverage for bed/cot graphics and retire the child play-mat fallback for generic cots.
- Create registry coverage for scrap knives, bolters, firearms, melee weapons, armor, and clothing icons.
- Create registry coverage for roads, sidewalks, sewer corridors, exterior maintenance corridors, noble corridors, room walls, corpse tiles, and transition doors.
- Create descriptions that state intended use and forbidden fallback behavior, such as “corpse tiles are containers/decay remnants, not floor coverings.”
- Add a coverage report showing which high-error asset families have IDs and which still need IDs.

Completion definition — what should have just been completed when Stage 3 is done:

- The known bad assignments have named registry IDs.
- Each high-error category has at least one correct fallback entry.
- The registry can distinguish visually similar but semantically different assets.
- The Infopedia can be used to inspect why a water barrel is not a shelf, a knife is not a bolter, and a cot is not unrelated domestic art.

Immediate next action after Stage 3:

- Migrate item previews and UI previews to resolve by asset ID first.

Likely next stage after that:

- Stage 4 UI/item-preview migration.


Stage 3 completion note:

- Completed as 0.9.10jv. The registry expanded from 26 starter rows to 277 semantic asset rows with no duplicate IDs and no duplicate asset paths. Added `PACKAGE_client/assets/indexes/high_error_asset_reconciliation.tsv` as a focused crosswalk for known bad assignments, including water barrel versus shelf, cot/bed/clothing, scrap knife versus bolter/firearm, road/sidewalk/corridor/wall families, corpse/decay markers, and emergency machines. Added `SemanticAssetHighErrorReconciliationSmoke` to verify category coverage, searchability, path uniqueness, and crosswalk references. No item/tile/world render migration was performed in this stage.

## Stage 4 — UI preview and item/icon migration — COMPLETE IN 0.9.10jw

Purpose: route player-visible previews through semantic IDs before touching the high-frequency map renderer.

Operable actions:

- Add `assetId` fields or resolver bridges to item definitions, equipment definitions, containers, object previews, and look/inspect menu preview payloads.
- Convert inventory icons, character equipment icons, look-frame previews, Infopedia item entries, and item tooltip thumbnails to request assets by ID.
- Keep legacy icon paths only as fallback bridges during migration.
- Add audits for items whose icon path exists but semantic ID is missing.

Completion definition — what should have just been completed when Stage 4 is done:

- Scrap Knife preview resolves to a knife ID, not a bolter/ranged weapon fallback.
- Water Barrel preview resolves to a barrel ID, not a supply shelf.
- Cot preview resolves to a cot/bed ID, not unrelated domestic art.
- UI preview errors surface as missing-asset fallback plus audit text, not silent wrong-art substitutions.

Stage 4 completion note:

- Completed as 0.9.10jw. Added `ItemSemanticAssetAuthority` as the Stage 4 resolver bridge from player-facing carried item names to exact 8-character semantic asset IDs. Inventory and carried-stack preview icons now call the Semantic Asset Registry through `AssetManager` first, then use the older alias classifier only as a migration fallback. Item Infopedia detail lines now expose the resolved semantic asset ID/type/name. Added a generic registry fallback row `ITEM-G01` and `SemanticAssetItemPreviewMigrationSmoke` to verify that scrap knives do not resolve as bolters, water barrels do not resolve as supply shelves, cots resolve as cots, and known clothing/armor/paper icons resolve through the registry.

Immediate next action after Stage 4:

- Move tile descriptor preview/art aliases into the semantic asset registry.

Likely next stage after that:

- Stage 5 tile and world-render migration.

## Stage 5 — Tile, road, wall, corridor, and transition migration

Purpose: migrate map tile art and world-generation descriptors only after registry/UI preview behavior is stable.

Operable actions:

- Add semantic asset IDs to tile descriptors for floors, walls, roads, sidewalks, corridors, sewer corridors, maintenance corridors, transitions, void, and overlays.
- Preserve existing glyph/descriptor authority while adding registry IDs as the graphical resolution layer.
- Route Zone Audit tile previews through registry metadata where available.
- Keep tile-generation logic separated from art selection; generation decides what a tile is, registry decides what art represents it.
- Add audits for tile descriptors with missing, unknown, or semantically inconsistent asset IDs.

Completion definition — what should have just been completed when Stage 5 is done:

- Active tile families can be inspected in Infopedia by semantic ID.
- Zone Audit look/audit output can report tile identity and asset ID together.
- Sewer/maintenance/noble/road/sidewalk families no longer rely on generic path fallback for their primary art identity.
- World rendering still functions under Java2D and does not require external asset browsers.

Stage 5 completion note:

- Completed as 0.9.10jx. Added `TileSemanticAssetAuthority` as the semantic bridge from compiled tile-art aliases to eight-character registry IDs. Tile descriptors now expose primary, underlay, and overlay semantic asset IDs in their inspect lines, the Zone Audit tile report receives those IDs through the existing descriptor inspection path, and Tile Infopedia detail lines list the registry ID/type for each allowed alias. Added floor, noble-floor, and active door/transition registry rows so roads, sidewalks, corridors, walls, floors, doors, overlays, and common fixture underlays can be audited by ID. The Java2D renderer now asks the registry first for compiled tile descriptor art and falls back to legacy alias/glyph art only when an ID is unmapped or missing. Added `SemanticAssetTileDescriptorMigrationSmoke`.

Immediate next action after Stage 5:

- Migrate machines, fixtures, interactables, and construction/build menu entries.

Likely next stage after that:

- Stage 6 object, machine, fixture, and construction-menu migration.

## Stage 6 — Object, machine, fixture, and construction-menu migration

Purpose: connect placeable objects, machines, fixtures, and construction-mode entries to the same semantic asset authority.

Operable actions:

- Add semantic asset IDs to machines, fixtures, containers, interactables, traps, lights, doors, construction menu entries, and editor palettes.
- Ensure locked/unavailable construction entries still display the correct asset preview while remaining inert.
- Let editor palettes show registry descriptions and IDs so placement errors can be diagnosed by semantic identity.
- Audit construction entries for missing registry IDs before allowing broad construction-mode expansion.

Completion definition — what should have just been completed when Stage 6 is done:

- Placeable objects have semantic IDs.
- Construction/editor palettes can show asset ID, preview, and description.
- Locked entries are visible with correct icons and descriptions but cannot execute.
- Machine/object preview mismatches become registry audit failures instead of user-discovered surprises.

Stage 6 completion note:

- Completed as 0.9.10jy. Added `ObjectSemanticAssetAuthority` as the semantic bridge for construction recipes, built base objects, map fixtures/interactables, traps, lights, and in-game editor palette items. Expanded the semantic registry with Stage 6 build/fixture rows (`BLD-*` and `FTR-*`) so construction menus and editor palettes have registry-backed IDs and image previews. Build tooltips and the build-detail panel now expose semantic object asset summaries; map object and look-stack previews now try registry images before legacy tile aliases; Zone Audit tile inspection reports semantic IDs for objects, lights, and traps; the Room Editor palette/grid preview shows asset IDs and previews. Added `SemanticAssetObjectFixtureMigrationSmoke` to audit all current build recipes, palette entries, known fixture mappings, trap mappings, and light mappings.

Immediate next action after Stage 6:

- Add stronger direct-path detection and migration enforcement.

Likely next stage after that:

- Stage 7 path-reference audit and enforcement.

## Stage 7 — Direct path reference audit and enforcement — COMPLETE IN 0.9.10jz

Purpose: stop new fragile graphical path calls from entering gameplay/UI code while allowing approved low-level loader bridges.

Operable actions:

- Add an audit that scans Java source for direct image-path loading outside approved classes.
- Maintain an allow-list for `AssetRegistry`, `AssetManager`, low-level pack importers, migration tools, and controlled test fixtures.
- Add audits for registry entries whose paths no longer exist.
- Add audits for active gameplay definitions that still use raw paths without an asset ID.
- Make the audit warn first, then fail only after the migrated surfaces are stable enough.

Completion definition — what should have just been completed when Stage 7 is done:

- New gameplay/UI direct path calls are visible in smoke output.
- Approved loader and importer exceptions are explicit.
- The project can quantify remaining legacy path usage.
- The migration stops regressing whenever future UI/item/tile work is added.

Stage 7 completion note:

- Completed as 0.9.10jz. Added `SemanticAssetPathAudit` as the source scanner and `SemanticAssetDirectPathAuditSmoke` as the enforcement smoke. Added `PACKAGE_client/assets/indexes/semantic_asset_direct_path_allowlist.tsv` to make low-level exceptions explicit and `PACKAGE_client/assets/indexes/semantic_asset_direct_path_baseline.tsv` to quantify existing legacy graphical path debt. The audit currently reports 263 direct graphical path findings: 222 approved low-level/media-bridge findings, 41 baselined legacy findings, and 0 unbaselined runtime references. Future direct image-path additions outside approved loaders or accepted baseline debt fail this stage smoke. Registry path existence is rechecked as part of the same smoke.

Immediate next action after Stage 7:

- Migrate portraits, factions, creatures, and name-locked special art into explicit registry partitions.

Likely next stage after that:

- Stage 8 portrait/faction/entity registry partitioning.

## Stage 8 — Portrait, faction, creature, and entity-art partitioning — COMPLETE IN 0.9.10ka

Purpose: make the existing portrait and entity-art authority inspectable by registry ID while preserving strict folder/category partition rules.

Operable actions:

- Register player portraits, faction NPC portraits, special/name-locked portraits, creatures, pets, mutants, servitors, cultists, nobles, Arbites, PDF, Mechanicus, medicae, servants, and children under explicit asset IDs.
- Preserve existing partition rules: no universal random portrait pool, no name-locked leakage, no creature/pet/mutant/servitor portraits in player creation unless explicitly allowed.
- Add Infopedia audit views for portrait/entity categories where appropriate.

Completion definition — what should have just been completed when Stage 8 is done:

- Portrait/category partitioning is visible through registry metadata.
- Random selection pools can be audited by semantic category.
- Name-locked/special portraits remain separated and do not leak into ordinary pools.

Immediate next action after Stage 8:

- Begin removing legacy fallback aliases that are now fully covered by the registry.

Likely next stage after that:

- Stage 9 legacy fallback retirement.

## Stage 9 — Legacy fallback retirement and registry authority hardening — COMPLETE IN 0.9.10kb

Purpose: remove old graphical ambiguity after enough categories have a proven registry path.

Operable actions:

- Retire legacy raw-path fallback for migrated families.
- Replace broad scrap/unknown icon fallbacks with typed fallbacks: missing item, missing wall, missing floor, missing object, missing machine, missing portrait.
- Mark any intentionally unmigrated asset as hidden/internal/deferred with an owner and reason.
- Convert warnings into build-failing audits where migration coverage is complete.

Completion definition — what should have just been completed when Stage 9 is done:

- Migrated categories cannot silently fall back to semantically wrong art.
- Missing assets are typed and obvious.
- Registry coverage is measurable and enforced.
- The Infopedia can function as the semantic asset audit surface for active art.

Immediate next action after Stage 9:

- Review asset-pack/modding implications and decide how external packs declare registry entries.

Likely next stage after that:

- Stage 10 mod/art-pack registry extension.

## Stage 10 — Mod and art-pack semantic registry extension

Purpose: allow future art packs and mods to extend the registry without undermining semantic authority.

Operable actions:

- Define how art packs declare additional registry entries.
- Define conflict rules for duplicate IDs, namespace ranges, overrides, and replacement packs.
- Ensure external registry entries pass the same ID/name/type/description/path audits.
- Keep modded art registry loading inside the asset acquisition/integrity phase before world/session initialization.

Completion definition — what should have just been completed when Stage 10 is done:

- Mods/art packs can contribute semantic asset entries through controlled data files.
- Duplicate or invalid IDs are rejected clearly.
- Asset acquisition, integrity verification, and registry loading remain separate from live world simulation.

Current implementation note:

- Stage 10 begins with controlled package-local TSV extension loading. Art-pack/profile-package roots may provide `semantic_asset_registry.tsv` with the same five columns as the core registry. Duplicate IDs, invalid rows, URI/classpath paths, and paths that escape the package root are rejected before the registry is installed.

Immediate next action after Stage 10:

- Reassess Phase 2 exit criteria and decide whether the asset migration can be considered broadly complete.

Likely next stage after that:

- Later polish: editor-assisted asset description authoring, atlas packing, localization keys for descriptions, and optional high-resolution art-pack handling.

## Required per-stage verification summary format

Every implementation pass that advances this file should report:

- Current stage advanced.
- Files/classes added or changed.
- Whether any old direct path calls were migrated or only wrapped.
- Registry entry count and category count.
- Missing path count.
- Duplicate ID count.
- Unknown/blank description count.
- Whether Infopedia remains in-game owned.
- Java 17 classfile gate result for code passes.
- Zip integrity result.
- What was not manually tested.

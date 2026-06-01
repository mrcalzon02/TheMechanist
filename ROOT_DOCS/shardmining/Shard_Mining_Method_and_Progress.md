# Shard Mining Method and Progress Ledger

Status: live operating procedure for GamePanel/shard-shell refactor.

Always read this together with `ROOT_DOCS/shardmining/Architecture_Map.md` before continuing extraction work.

## Current Goal

The project is refactoring an oversized Java 17 Swing monolith/shard shell into a set of small, named support classes and authorities. `GamePanel.java` and the shard shells are treated as the source of truth for code that has not yet been mined. The objective is to move behavior outward into coherent modules, wire minimal bridges, preserve compileability, then hollow out the shard by deleting what has been successfully extracted.

## Current Baseline

Latest known clean compile smoke:

- `diagnostics/shard8_smoke_20260601_084116/compile_errors.tsv`
- result: header only; no compiler error rows

Recent compiler cleanup path:

1. Fixed smoke harness path/response-file issues.
2. Fixed Concord/IP syntax cluster: `civic Wardens` -> `civicWardens`.
3. Removed duplicate helper classes `TextSurfaceApi.java` and `TextLayoutAuthority.java` because they already exist inside `UiRuntimeSupportFramework.java`.
4. Added faction compatibility aliases in `Faction.java`.
5. Cleared the compact 13-error support cluster through direct bridges/imports/API corrections.
6. Added `IntroCrawlSurfacePainter` as a conservative bridge without touching `GamePanel.java`.
7. Confirmed clean smoke compile.

## Shard Mining Method

### 1. Use the shard as source of truth

The shard file is the authoritative inventory of unmined functionality. Do not assume a feature is migrated just because a helper file exists. Verify the shard body and the call sites.

### 2. Extract by coherent zone, not by random line count

Prefer extracting complete conceptual slices:

- Rendering surface
- Options behavior
- Registry access
- Input prompts
- Runtime pathing
- World generation
- Fixture interaction
- Save/profile bridge
- Diagnostic/smoke bridge

Avoid half-extracting unrelated features just because they are near each other in the shard.

### 3. Create a named authority/subsystem

Use explicit names such as:

- `OptionsScreenPainter`
- `GeneratedArtPayloadOptionsSubsystem`
- `RuntimePathResolver`
- `TileArtSystem`
- `RoomFixtureInteractionAuthority`
- `IntroCrawlSurfacePainter`

Names should describe ownership, not just location.

### 4. Wire the smallest bridge first

Prefer a tiny bridge call from the shard to the extracted system. Keep `GamePanel.java` edits minimal. When possible, add support code outside `GamePanel.java` and let existing references compile.

### 5. Smoke test after every cluster

Run:

```powershell
powershell -ExecutionPolicy Bypass `
-File scripts/SMOKE_SHARD8_DIAGNOSTIC_WINDOWS.ps1
```

Then stage and push the new diagnostic folder when the result matters.

### 6. Use compile errors as the work queue

When smoke fails, inspect:

- `diagnostics/shard8_smoke_*/compile_errors.tsv`
- `diagnostics/shard8_smoke_*/SUMMARY.txt`
- `diagnostics/shard8_smoke_*/compile.log` if present

Patch the smallest compiler-stopping cluster first. Do not reopen broad refactors while a compact compile error cluster is active.

### 7. Delete mined material from the shard after ownership moves

The shrinkage of the shard is the progress meter. The shard should gradually hollow out. After a subsystem owns behavior and smoke confirms compile stability, remove the extracted implementation body from the shard and leave only the narrow call bridge or no call if ownership is fully transferred.

### 8. Keep a live journal

Every mining pass should update this ledger with:

- What was moved
- From where
- To what new authority/subsystem
- Whether the old shard material was deleted or still remains as a bridge
- Smoke result
- Remaining risks

### 9. Always update the architecture map

`ROOT_DOCS/shardmining/Architecture_Map.md` is the system-state map. Any new authority, zone ownership change, or important bridge must be added there.

### 10. Registry interactions must use public APIs

Do not reach into private registry fields. Use:

- `TileArtSystem.getRegistry()`
- `TileImageRegistry.getAlias(...)`
- `TileImageRegistry.aliasView()`
- `TileImageRegistry.findAlias(...)`
- `AssetManager.metadata(...)`
- `AssetManager.generatedAssetRuntime()`

Temporary compatibility aliases must be recorded in `Compatibility_Ledger.md` and later retired.

## Current Progress Journal

### Clean Smoke Baseline Established

Date marker: `20260601_084116`

Result:

- `compile_errors.tsv` contains only header.
- Javac smoke reached source compile and found zero errors.
- This is the current handoff baseline.

### Support Cluster Cleared

Moved/bridged:

- `DebugLog.error(system, message)` two-argument overload added as compile bridge.
- `TileArtSystem.semanticKeyForBuildName(...)` added.
- `TileArtSystem.semanticKeyForMapObject(...)` added.
- `GeneratedArtPayloadOptionsSubsystem` now imports `mechanist.assets.AssetManager`.
- `LayerG` now imports `mechanist.assets.AssetManager`.
- `OptionsScreenPainter` now imports `mechanist.assets.AssetManager`.
- `OptionsScreenPainter.controlsLines(...)` now uses `ControlReferenceTextSubsystem.controlsReferenceLines(panel)`.
- `RuntimePathResolver.resolveDirectoryPath(...)` uses `preferredClientRoot().toString()` instead of invalid `Path.getPath()`.
- `TileArtSystem.byAlias` compatibility view added for stale `TileInfopediaAuthority` access.
- `IntroCrawlSurfacePainter` added to satisfy `GamePanel` surface reference without touching `GamePanel.java`.

Status:

- Compile-clean.
- Some bridges are intentionally temporary and should be retired during cleanup.

### Public Registry Cleanup Pass

Moved/removed:

- Replaced `TileInfopediaAuthority.loadedAliasSummary(...)` direct `art.byAlias` access with `art.getRegistry().getAlias(a)`.
- Removed the temporary `TileArtSystem.byAlias` field from `TileArtSystem`.
- Removed the stale `java.io.File` and `java.util.Map` imports from `TileArtSystem` as part of the alias-field cleanup.
- Updated `Compatibility_Ledger.md` to move `TileArtSystem.byAlias` from active debt to retired bridge.
- Updated `Architecture_Map.md` so the asset/registry zone reflects the public-registry cleanup.

Status:

- Repository search for `byAlias` returned no remaining code or documentation references after the cleanup.
- Windows smoke artifact `diagnostics/shard8_smoke_20260601_084116/SUMMARY.txt` reports javac `ExitCode: 0`.
- `diagnostics/shard8_smoke_20260601_084116/compile_errors.tsv` remains header-only.

### Typed MapObjectState Semantic Cleanup Pass

Moved/removed:

- Replaced `TileArtSystem.semanticKeyForMapObject(...)` reflection probing with typed access to `MapObjectState.label`, `MapObjectState.type`, and `MapObjectState.stockState`.
- Removed the `java.lang.reflect.Field` import and the private reflective `firstStringField(...)` helper from `TileArtSystem`.
- Added a simple typed `firstNonBlank(...)` helper for the known semantic object fields.
- Updated `Compatibility_Ledger.md` to mark the reflection bridge as retired.
- Updated `Architecture_Map.md` to record the typed `MapObjectState` cleanup in the asset/registry zone.

Status:

- The typed fields are already used by `ObjectSemanticAssetAuthority.assetIdForMapObject(...)`, so this pass follows an existing compile-validated shape instead of guessing.
- Smoke still needs to be re-run after this code change and the generated diagnostics pushed if meaningful.
- Remaining compatibility debt: `IntroCrawlSurfacePainter` remains a conservative bridge; controlled Concord rename work remains parked.

### Concord/IP Sweep Status

The Concord/IP neutralization sweep was started and documented, but it is parked as backlog so shard mining can continue. Do not resume broad renaming until current shard mining has a stable plan and compile baseline.

Relevant files:

- `docs/CONCORD_IP_NEUTRALIZATION_LEDGER.md`
- `scripts/AUDIT_CONCORD_IP_TERMS_WINDOWS.ps1`
- `diagnostics/concord_ip_audit_*.tsv`

Important note:

- Legacy enum names and class names may remain for save/compile compatibility.
- Player-facing labels should prefer Concord terms.

## Current Next Steps

1. Pull latest `main`.
2. Run the Windows smoke harness because the last pass changed code in the asset/registry zone.
3. If smoke is clean, continue to the next compact retirement target: re-mine `IntroCrawlSurfacePainter` from the original richer shard behavior if it is still recoverable, or choose the next compile-safe UI/runtime slice.
4. If smoke fails, use the generated `compile_errors.tsv` as the work queue and patch the smallest compiler-stopping cluster first.
5. Continue updating this progress journal and `Architecture_Map.md` after each pass.

## Do Not Do

- Do not blindly mass-rename classes or persisted IDs.
- Do not edit `GamePanel.java` unless the compile/build boundary requires it.
- Do not add private field reach-through to registries.
- Do not leave undocumented temporary compatibility aliases.
- Do not perform broad Concord/IP cleanup while a shard compile cluster is active.

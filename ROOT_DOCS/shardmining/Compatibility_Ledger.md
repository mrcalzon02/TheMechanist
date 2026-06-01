# Shard Mining Compatibility Ledger

Status: bridge and compatibility-debt ledger for the GamePanel/shard-mining refactor.

Read this before changing compatibility helpers, aliases, registry bridge fields, or class-name aliases.

## Bridge Categories

### Direct Fix

A direct fix corrects a bad call, missing import, wrong method name, or obvious API typo. It is considered stable unless later architecture changes supersede it.

### Compatibility Alias Pattern

A compatibility alias keeps old/stale call sites compiling while a fuller refactor is deferred. It must be recorded here and retired later if possible.

### Conservative Bridge

A conservative bridge supplies minimal behavior to preserve compile/run continuity without re-mining the full original implementation yet.

## Current Bridges and Status

| Bridge / File | Category | Status | Notes |
|---|---|---|---|
| `DebugLog.error(String,String)` | Direct fix | Active, acceptable | Two-argument overload delegates to existing three-argument `error(system,message,Throwable)` with `null`. Used by `TileArtSystem`. |
| `TileArtSystem.semanticKeyForBuildName(String)` | Direct fix / bridge | Active | Provides normalized semantic key extraction for existing `GamePanel` calls. May later move into a dedicated semantic asset authority. |
| `TileArtSystem.semanticKeyForMapObject(MapObjectState)` | Direct fix / bridge | Active, typed | Uses the typed `MapObjectState` fields already consumed by `ObjectSemanticAssetAuthority`: `label`, `type`, and `stockState`. Reflection bridge removed. |
| `GeneratedArtPayloadOptionsSubsystem` `AssetManager` import | Direct fix | Active, acceptable | Missing import repaired. |
| `LayerG` `AssetManager` import | Direct fix | Active, acceptable | Missing import repaired. |
| `OptionsScreenPainter` `AssetManager` import | Direct fix | Active, acceptable | Missing import repaired. |
| `OptionsScreenPainter.controlsLines(...)` | Direct fix | Active, acceptable | Corrected nonexistent `controlReferenceLines(int)` to real `controlsReferenceLines(GamePanel)`. |
| `RuntimePathResolver.resolveDirectoryPath(...)` fallback | Direct fix | Active, acceptable | Replaced invalid `Path.getPath()` with `Path.toString()`. |
| `IntroCrawlSurfacePainter` | Conservative bridge | Active, not final | Added to satisfy `GamePanel` `ScreenPainter` reference without touching `GamePanel.java`. Rich original intro crawl behavior can be re-mined later. |
| `Faction.CIVIC_LEDGER_OFFICE` | Compatibility alias pattern | Active | Neutral alias added while legacy enum constants remain for save/profile compatibility. |
| `Faction.CIVIC_WARDENS` | Compatibility alias pattern | Active | Neutral alias added while `ARBITES` remains for compatibility. |
| `Faction.MECHANIST_COLLEGIA` | Compatibility alias pattern | Active | Neutral alias added while `MECHANICUS` remains for compatibility. |
| Legacy `Faction` labels | Direct visible-label neutralization | Active | Legacy constants retained, visible labels changed to Concord-safe language where appropriate. |
| `ArbitesPrecinctFixtureAuthority` class name | Deferred compatibility debt | Legacy retained | Do not rename class until a controlled file/class rename pass with migration/compile audit is planned. Player-facing language should use Civic Wardens. |
| `GuardPdfDefenseFixtureAuthority` class name | Deferred compatibility debt | Legacy retained | Potential Concord rename later, but not during compile-cluster cleanup. |

## Retired Bridges

| Bridge / File | Former Category | Retired In | Notes |
|---|---|---|---|
| `TileArtSystem.byAlias` | Compatibility alias pattern | Shard mining public-registry cleanup | `TileInfopediaAuthority.loadedAliasSummary(...)` now uses `art.getRegistry().getAlias(a)`. The `TileArtSystem.byAlias` field was removed; repository search found no remaining `byAlias` references after the cleanup. |
| `TileArtSystem.semanticKeyForMapObject(...)` reflection helper | Compatibility bridge | MapObjectState typed semantic cleanup | `TileArtSystem` no longer imports `java.lang.reflect.Field` or probes fields reflectively. It now uses typed `MapObjectState.label`, `type`, and `stockState`, matching the existing typed usage in `ObjectSemanticAssetAuthority`. |

## Rules for Adding a Bridge

1. Prefer direct API correction over aliasing.
2. If aliasing is necessary, document it here immediately.
3. Compatibility aliases must be small and removable.
4. Do not create a second source of truth.
5. Do not expose mutable internal maps unless the bridge is explicitly temporary.
6. Do not use a bridge to hide real architecture problems indefinitely.

## Registry Access Rule

Registry interactions should go through public APIs:

- `TileArtSystem.getRegistry()`
- `TileImageRegistry.getAlias(...)`
- `TileImageRegistry.findAlias(...)`
- `TileImageRegistry.aliasView()`
- `AssetManager.metadata(...)`
- `AssetManager.generatedAssetRuntime()`

Former temporary exception:

- `TileArtSystem.byAlias` has been retired and must not be restored as a pattern.

## Current Retirement Targets

1. Re-mine `IntroCrawlSurfacePainter` from the original richer shard behavior if present.
2. Plan controlled Concord rename passes only after active shard mining remains compile-clean.

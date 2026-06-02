# Worldgen Manager Extraction Ledger

Status: active post-pipeline extraction ledger.

## Current State

`World.generate()` has been prepped for phase extraction through `scripts/SPLIT_WORLD_GENERATE_PIPELINE.py`, which replaces the monolithic generation body with named phase methods and a `WorldGenerationPipelineRunState`.

Search review found no dedicated high-level manager classes named:

- `TileManager`
- `EntityManager`
- `RoomManager`
- `ZoneManager`

The codebase does contain many narrower authorities around tile compilation, room manifests, road grids, zone audits, fixtures, NPCs, population, and simulation. Those are useful domain authorities, but they are not the same as clean manager ownership shells.

## Required Manager Classes After Pipeline Extraction

After `World.generate()` is split into named phases and smoke passes, create or promote the following manager classes in this order:

| Manager | Mermaid zone | Initial responsibility | Notes |
|---|---|---|---|
| `ZoneGenerationManager` | `WORLD_GEN` | Own zone-level generation phase order, zone type inputs, validation/repair gates, and generation summaries. | This should eventually absorb the phase orchestration currently inside `World`. |
| `RoomGenerationManager` | `WORLD_GEN` | Own room target selection, room manifests, road-first room placement, room quota repair, shell normalization, and room faction assignment handoff. | Should not own object placement beyond room-level claims. |
| `TileGenerationManager` | `ASSET_REGISTRY` / `WORLD_GEN` | Own tile substrate reset, tile family assignment, tile descriptor compile handoff, and tile-grid sanitation rules. | Tile descriptor compilation may remain in `TileDataCompilationAuthority`; manager owns generation flow, not art registry semantics. |
| `EntitySpawnManager` | `COMBAT_SIM` | Own NPC/entity spawn orchestration, population-derived entity placement, and entity generation summaries. | Population ledgers remain separate; manager coordinates entity materialization. |
| `ZoneEconomyInitializationManager` | `INVENTORY_PERSIST` / `WORLD_GEN` | Own economy initialization handoff after population/rooms exist. | May wrap `WorldEconomyInitializationAuthority` once signals are stable. |
| `ZoneFixturePlacementManager` | `FIXTURE_MACHINE` / `WORLD_GEN` | Own road frontage fixtures, room fixtures, vending-machine stock seeding hooks, and special machine room placement coordination. | Should keep fixture interaction behavior outside raw worldgen. |

## Smoke Rule

Every manager extraction must run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\SMOKE_FUNCTION_MAP_OPERATIONS_WINDOWS.ps1 -CommandTimeoutSeconds 900
```

If the smoke harness hangs, lower the timeout for diagnosis:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\SMOKE_FUNCTION_MAP_OPERATIONS_WINDOWS.ps1 -CommandTimeoutSeconds 120 -SkipCompile
```

The timeout-safe smoke harness kills child processes that fail to release the command line and records exit code `124`.

## Next Extraction Rule

Do not move the low-level algorithms first. Move the orchestration first:

1. Apply `SPLIT_WORLD_GENERATE_PIPELINE.py --apply`.
2. Smoke.
3. Create manager shells.
4. Move one phase at a time from `World` into a manager.
5. Smoke after each move.

The first actual manager extraction target should be `ZoneGenerationManager`, because it can call the existing phase methods and become the stable owner of the generation sequence before deeper algorithm moves begin.

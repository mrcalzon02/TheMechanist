# Oversized Module Split Ledger

Status: active split-plan ledger for Mermaid WARN rows.

Source: refreshed `CODE_MERMAID_EVALUATION.tsv` after Mermaid ownership override pass, generated around `2026-06-01 14:51:11`.

## Current Gate State

The first Mermaid operations pass found one hard unpositioned module: `MediaLayerAlpha.java`. The override pass assigned it to `UI_RENDER`, clearing the hard Mermaid position error.

The remaining rows are WARN rows for oversized but positioned modules. These are not compile failures. They are structural split-plan targets.

## WARN Rows Reclassified As Split Targets

| Module | Mermaid zone | Size signal | Status | Split strategy |
|---|---|---:|---|---|
| `src/mechanist/WorldRuntimeGenerationFramework.java` | `WORLD_GEN` | `351 funcs / 8140 lines / 389004 bytes` | `split_plan_required` | Split only by generated world subsystem: plaza/road/room/corridor/fixture/transition. Do not start here until UI/runtime hook-up tests are stable. |
| `src/mechanist/ProductionAuthorityFramework.java` | `FIXTURE_MACHINE` | `314 funcs / 3105 lines / 341832 bytes` | `split_plan_required` | Split catalog/recipe data from powered-machine runtime behavior, then isolate production UI text. |
| `src/mechanist/WorldSimulationFramework.java` | `COMBAT_SIM` | `204 funcs / 1800 lines / 132955 bytes` | `split_plan_required` | Split turn scheduling, entity updates, heat/suspicion, and environmental simulation separately. High mutation risk. |
| `src/mechanist/ItemEconomyFramework.java` | `INVENTORY_PERSIST` | `71 funcs / 1327 lines / 224524 bytes` | `split_plan_required` | Split immutable catalog data from mutable economy/trade/loot rules. Save compatibility risk. |
| `src/mechanist/PopulationPersonnelFramework.java` | `COMBAT_SIM` | `122 funcs / 1262 lines / 82504 bytes` | `split_plan_required` | Split population definitions from personnel assignment/runtime simulation. |
| `src/mechanist/FirstPerson3DFramework.java` | `UI_RENDER` | `85 funcs / 1042 lines / 53871 bytes` | `parked_large_owner` | Experimental visual surface. Keep parked unless Doom-mode rendering becomes active work. |
| `src/mechanist/InfrastructurePromotionRegistry.java` | `ASSET_REGISTRY` | `11 funcs / 1001 lines / 61142 bytes` | `data_registry_review` | Likely data concentration, not necessarily bad logic concentration. Split only if registry/data ownership requires it. |
| `src/mechanist/FactionServicesFramework.java` | `COMBAT_SIM` | `105 funcs / 939 lines / 68684 bytes` | `split_plan_required` | Split services/catalog definitions from runtime faction service logic. Save/label compatibility risk. |
| `src/mechanist/SimulationEditorSuite.java` | `UI_RENDER` | `60 funcs / 824 lines / 45085 bytes` | `parked_large_owner` | In-game editor UI. Split later by editor panel after main runtime hook-up is stable. |
| `src/mechanist/ContainerTradeFramework.java` | `INVENTORY_PERSIST` | `92 funcs / 802 lines / 69293 bytes` | `split_plan_required` | Split container transaction rules from trade/merchant logic and UI text. |

## Priority Order

Do not split these in raw size order. Use risk order:

1. `ContainerTradeFramework` — contained inventory/trade boundary; good first non-UI split.
2. `InfrastructurePromotionRegistry` — likely data-registry cleanup, lower runtime mutation risk.
3. `FactionServicesFramework` — service definitions and label compatibility need careful split.
4. `PopulationPersonnelFramework` — simulation-bearing but domain-contained.
5. `ProductionAuthorityFramework` — large, data-heavy, production behavior risk.
6. `ItemEconomyFramework` — high save/economy compatibility risk.
7. `WorldSimulationFramework` — high mutation/runtime risk.
8. `WorldRuntimeGenerationFramework` — biggest and most generation-critical; split after smaller patterns are proven.
9. `SimulationEditorSuite` — UI/editor split after runtime systems are stable.
10. `FirstPerson3DFramework` — parked unless Doom-mode work resumes.

## Gate Rule

A WARN row becomes accepted only when this ledger names:

- the Mermaid zone,
- the reason the module is oversized,
- whether it is data concentration, active runtime behavior, or UI surface concentration,
- the split priority,
- and the smoke gate required after any split.

## Required Smoke After Any Split

After splitting any module in this ledger:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\SMOKE_FUNCTION_MAP_OPERATIONS_WINDOWS.ps1
```

The split is not complete unless:

- compile smoke passes,
- no Mermaid ERROR rows exist,
- new modules receive Mermaid positions,
- and the affected old owner either shrinks or is explicitly marked as an accepted large owner.

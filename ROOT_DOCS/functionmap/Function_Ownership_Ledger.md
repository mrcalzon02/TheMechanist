# Function Ownership Ledger

Status: active governance document for the post-shard function remap phase.

Purpose: convert raw generated inventories into deliberate subsystem ownership decisions. The generated reports under `ROOT_DOCS/functionmap/generated/` are evidence, not final architecture. This ledger is where final ownership is recorded.

## Operating Rule

Do not delete, rename, or move behavior simply because the mapper marks it duplicated, oversized, or retirement-candidate. The mapper is an index. Human-reviewed ownership decisions belong here before code is rewired.

## Canonical Reports

Generate reports with:

```powershell
py -3 scripts\BUILD_FUNCTION_MAP.py --apply
```

Expected outputs:

- `ROOT_DOCS/functionmap/generated/FUNCTION_MAP.tsv`
- `ROOT_DOCS/functionmap/generated/CLASS_MAP.tsv`
- `ROOT_DOCS/functionmap/generated/DUPLICATE_FUNCTION_NAMES.tsv`
- `ROOT_DOCS/functionmap/generated/FILE_INVENTORY.tsv`
- `ROOT_DOCS/functionmap/generated/DOCUMENT_INVENTORY.tsv`
- `ROOT_DOCS/functionmap/generated/DOC_RETIREMENT_CANDIDATES.tsv`
- `ROOT_DOCS/functionmap/generated/OVERSIZED_OWNERS.tsv`
- `ROOT_DOCS/functionmap/generated/SUMMARY.md`

## Ownership Status Values

Use these exact values in review notes:

- `active_owner` — class/function is the current canonical implementation.
- `bridge_owner` — class/function is a temporary compatibility bridge.
- `duplicate_candidate` — likely duplicate, requires source comparison before removal.
- `move_candidate` — function belongs in another subsystem but has not moved yet.
- `retire_candidate` — likely obsolete or replaced, requires smoke-safe removal plan.
- `generated_reference` — generated extraction/report material retained as evidence only.
- `do_not_touch` — high-risk persisted/save/runtime identity surface.

## Initial Subsystem Zones

| Zone | Intended responsibility | First review source |
|---|---|---|
| `ui.render.surface` | Screen painters, immediate-mode drawing, HUD, visual panels. | `FUNCTION_MAP.tsv`, generated subsystem `02_render_surfaces.txt` if present. |
| `ui.input.navigation` | Keyboard, mouse, controller, scrollbars, screen route changes. | `FUNCTION_MAP.tsv`, generated subsystem `03_input_scroll_navigation.txt` if present. |
| `runtime.options` | Display/audio/JVM/accessibility/options runtime controls. | `FUNCTION_MAP.tsv`, generated subsystem `10_options_runtime_controls.txt` if present. |
| `world.generation.transition` | World setup, atlas, zones, rooms, generation audit, transitions. | `FUNCTION_MAP.tsv`, generated subsystem `07_world_profile_generation.txt` if present. |
| `inventory.items.persistence` | Inventory, equipment, containers, item transfer, save/load/profile. | `FUNCTION_MAP.tsv`, generated subsystems `05_inventory_items_containers.txt` and `09_save_profile_persistence.txt` if present. |
| `interaction.fixtures.machines` | Fixtures, machines, vending, powered devices, construction defenses. | `FUNCTION_MAP.tsv`, generated subsystem `06_interaction_fixtures_machines.txt` if present. |
| `combat.entities.simulation` | NPC/entity state, turn advancement, combat, movement, heat/suspicion. | `FUNCTION_MAP.tsv`, generated subsystem `08_combat_entities_simulation.txt` if present. |
| `assets.registry.art` | Asset registry, tile art, glyphs, semantic art, Infopedia asset references. | `FUNCTION_MAP.tsv`, current asset/registry Java files. |
| `server.authority.launcher` | Authoritative runtime, launcher, client/server boundary, multiplayer host/session. | `FUNCTION_MAP.tsv`, server/launcher Java files. |
| `diagnostics.smoke.audit` | Smoke scripts, debug reports, validation, ledgers, generated diagnostics. | `DOCUMENT_INVENTORY.tsv`, `DOC_RETIREMENT_CANDIDATES.tsv`, smoke folders. |

## Review Order

1. Generate function map reports.
2. Review `SUMMARY.md` for counts and parse errors.
3. Review `OVERSIZED_OWNERS.tsv` for files that still concentrate too much behavior.
4. Review `DUPLICATE_FUNCTION_NAMES.tsv` for duplicate method names that may be harmless overloads or real duplicate ownership.
5. Review generated subsystem documents against current Java classes.
6. Promote confirmed ownership decisions into the table below.
7. Only then perform code movement/deletion in small smoke-tested passes.

## Confirmed Ownership Decisions

| Date | Function/Class/File | Current owner | Target owner | Status | Notes |
|---|---|---|---|---|---|
| 2026-06-01 | `scripts/EXTRACT_GAMEPANEL_SHARDS_TO_SUBSYSTEMS.py` | script | generated reference tool | generated_reference | Extraction already run; no more patching needed unless rerun support is explicitly required. |
| 2026-06-01 | `ROOT_DOCS/shardmining/generated_subsystems/*` | generated subsystem docs | review input | generated_reference | Use as source material for reattachment, not compile-ready Java. |

## Document Cleanup Rule

Documents may be marked `retire_candidate` only after one of these is true:

1. Their content is fully duplicated by an active governance document.
2. Their content is a generated diagnostic whose source run is superseded and not needed for the current baseline.
3. Their content describes a plan that was completed and summarized in the Development History.
4. Their content refers to a parked or abandoned path and is intentionally archived.

Do not delete active root governance documents, current shard/function maps, current compatibility ledgers, or current smoke baselines.

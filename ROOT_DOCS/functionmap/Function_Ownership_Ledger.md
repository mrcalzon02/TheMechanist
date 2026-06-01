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

Current reviewed report set: generated `2026-06-01 13:33:06`, supplied directly for the first ownership pass.

## Ownership Status Values

Use these exact values in review notes:

- `active_owner` — class/function is the current canonical implementation.
- `bridge_owner` — class/function is a temporary compatibility bridge.
- `duplicate_candidate` — likely duplicate, requires source comparison before removal.
- `move_candidate` — function belongs in another subsystem but has not moved yet.
- `retire_candidate` — likely obsolete or replaced, requires smoke-safe removal plan.
- `generated_reference` — generated extraction/report material retained as evidence only.
- `do_not_touch` — high-risk persisted/save/runtime identity surface.

Additional first-pass status labels:

- `expected_multi_entrypoints` — duplicate `main`/launcher/smoke entrypoint pattern; not a duplicate-removal target by name alone.
- `expected_java_object_pattern` — ordinary object methods such as `toString`, `equals`, or `hashCode`.
- `codec_pattern_review` — save/persistence-adjacent codec helpers; high compatibility risk.
- `display_text_pattern_review` — repeated display/line/label helpers; possible interface candidates only after manual review.
- `retire_candidate_after_hash_verification` — generated backup copy can be removed only after source/retired copy hashes are confirmed.

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
| 2026-06-01 | `src/mechanist/GamePanel.java` | active shell / monolith | narrow bridge shell while functions move outward | bridge_owner | Largest remaining owner: 1,397 parsed methods, 21,165 lines. Do not delete; drain through smoke-tested function moves. |
| 2026-06-01 | `src/mechanist/GamePanel.java` extracted behavior | active shell / monolith | named subsystem owners | move_candidate | Treat GamePanel as source for the first remap wave, especially UI routing/rendering methods. |
| 2026-06-01 | `src/mechanist/GamePanel.java::rebuildButtons` | GamePanel | `PanelButtonCommandRouter` or existing panel command authorities | move_candidate | First explicit remap target. Longest parsed GamePanel method: 719 lines. UI command wiring should move before gameplay mutation systems. |
| 2026-06-01 | `src/mechanist/GamePanel.java::handleKeyPressed` | GamePanel | `GamePanelKeyController` / input navigation subsystem | move_candidate | Second UI-shell remap target after button routing. Avoid simulation mutation changes in the same pass. |
| 2026-06-01 | `src/mechanist/GamePanel.java::drawOptions` | GamePanel | `OptionsScreenPainter` | move_candidate | Existing painter should absorb or bridge this surface if behavior is not already equivalent. |
| 2026-06-01 | `src/mechanist/GamePanel.java::drawGame` | GamePanel | dedicated game surface painter / renderer bridge | move_candidate | Rendering-only extraction candidate after button/input routing. |
| 2026-06-01 | `src/mechanist/GamePanel.java::drawCharacter` | GamePanel | character/dossier surface painter | move_candidate | Rendering-only extraction candidate; keep inventory/equipment mutation out of this pass. |
| 2026-06-01 | GamePanel Infopedia line builders | GamePanel | specific Infopedia authority classes | move_candidate | Includes `infopediaDetailLines`, `productionInfopediaLines`, `auditInfopediaLines`; split by domain authority. |
| 2026-06-01 | `src/mechanist/WorldRuntimeGenerationFramework.java` | active owner | future generation authorities | do_not_touch | Oversized but high-risk world-generation owner. Do not split until UI shell mapping is cleaner. |
| 2026-06-01 | `src/mechanist/ProductionAuthorityFramework.java` | active owner | future production/recipe authorities | do_not_touch | Oversized production behavior. Long catalog methods may be data-definition concentration, not duplicate logic. |
| 2026-06-01 | `src/mechanist/ItemEconomyFramework.java` | active owner | future catalog/economy authorities | do_not_touch | Save/catalog/economy-adjacent; high compatibility risk. |
| 2026-06-01 | `src/mechanist/WorldSimulationFramework.java` | active owner | future simulation authorities | do_not_touch | Runtime/simulation mutation behavior. Keep out of first UI remap batch. |
| 2026-06-01 | duplicate function group `main` | multiple launch/smoke/tool entrypoints | no consolidation by name | expected_multi_entrypoints | Expected Java tool/launcher pattern. |
| 2026-06-01 | duplicate function groups `toString` / `equals` / `hashCode` | Java object/record implementations | no consolidation by name | expected_java_object_pattern | Expected Java pattern. |
| 2026-06-01 | duplicate function groups `encode` / `decode` / `parse` / `esc` / `unesc` | local codec helpers | future codec review only | codec_pattern_review | Save/persistence-adjacent; do not consolidate without compatibility tests. |
| 2026-06-01 | duplicate display helper groups `label` / `statusLines` / `lines` / `compact` / `auditLines` | local display helpers | possible interface review | display_text_pattern_review | Not deletion targets until source-equivalence is proven. |
| 2026-06-01 | `ROOT_DOCS/shardmining/generated_subsystems/_retired_shards/*` | retired shard source copies | generated reference source of truth | generated_reference | Keep during active remap. Useful recovery material even though Git history also exists. |
| 2026-06-01 | `ROOT_DOCS/shardmining/generated_subsystems/_backups/*` | duplicate extractor backups | future cleanup | retire_candidate_after_hash_verification | Only remove after confirming manifest and retired-shard copy hashes. |
| 2026-06-01 | `docs/CONCORD_IP_NEUTRALIZATION_LEDGER.md` | parked IP governance | parked active governance | active_owner | Unknown-review by mapper, but semantically active parked-governance; not a retirement target. |

## First Remap Batch

Start with UI shell functions because they can usually be bridged without changing simulation state.

1. `GamePanel.rebuildButtons` -> target owner: `PanelButtonCommandRouter` or existing panel command authorities.
2. `GamePanel.handleKeyPressed` -> target owner: `GamePanelKeyController` / input navigation subsystem.
3. `GamePanel.drawOptions` -> target owner: `OptionsScreenPainter`.
4. `GamePanel.drawGame` and `GamePanel.drawCharacter` -> target owner: dedicated surface painters.
5. Infopedia line builders -> target owner: specific Infopedia authority classes.

Do not start with inventory, world generation, production, or simulation mutation until the UI routing shell is cleaner.

## Document Cleanup Rule

Documents may be marked `retire_candidate` only after one of these is true:

1. Their content is fully duplicated by an active governance document.
2. Their content is a generated diagnostic whose source run is superseded and not needed for the current baseline.
3. Their content describes a plan that was completed and summarized in the Development History.
4. Their content refers to a parked or abandoned path and is intentionally archived.

Do not delete active root governance documents, current shard/function maps, current compatibility ledgers, or current smoke baselines.

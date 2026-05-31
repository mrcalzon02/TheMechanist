# GamePanel Strip-Mining Progress Markers

## Current Gold Chain

The project goal is to strip-mine `GamePanel.java` behavior into named subsystems, painters, and controllers without editing the monolith through the connector. Shard files remain the extraction source of truth until their replacement code compiles, caller wiring is stable, and source-range ownership is verified.

## Overall Completion Estimate

- Shard 8 structural extraction: **100%**
- Shard 8 compile/caller cleanup: **about 35-45%**
- Whole GamePanel strip-mining project: **about 10-12%**

The whole-project percentage remains low because Shards 1-7 still need direct subsystem integration, but Shard 8 is no longer the extraction bottleneck.

## Marker S8-A — Structural Extraction

Status: **Complete**

Shard 8 functional material has been moved into:

- Runtime/options bridge layers: `LayerB` through `LayerJ`
- Named runtime subsystems already present in the project
- `OptionsScreenPainter`
- `UiTextSurfacePainter`
- `UiHoverHelpPainter`
- `UiModalButtonController`
- `PanelTargetingController`
- `OptionsDropdownMouseController`
- `MouseEarlyScreenController`
- `MouseGamePanelController`
- `MouseLateUiController`
- `KeyEarlyScreenController`
- `CharacterNameKeyController`
- `InventoryPanelKeyController`
- `GamePanelKeyController`

## Marker S8-B — Static Preflight

Status: **Active**

Current work is checking extracted Shard 8 files for obvious problems before the full compile sweep:

- Route-order drift from the shard
- Missing screen branches
- Suspicious API names
- Missing imports
- Access assumptions
- Duplicate or unreachable dispatch paths
- Extracted methods that should delegate rather than duplicate logic

Recent completed cleanup:

- `GamePanelKeyController` route order was aligned closer to the original shard flow.
- Top-level Knowledge screen key dispatch was restored through `handleKnowledgeScreenKey(...)`.
- Progress-marker tracking was added as a durable document.
- Direct-file preflight is now preferred over repository search alone because code search can lag newly committed files.
- `MouseEarlyScreenController` restored `INTRO_CRAWL` mouse preflight so click-to-continue is not lost before map-point conversion.
- `MouseGamePanelController` and `MouseLateUiController` were checked against the visible shard click flow and had no obvious line-level behavior drift in the fetched sections.
- Painter preflight found missing shared text helper classes and added `TextSurfaceApi` plus `TextLayoutAuthority` so `OptionsScreenPainter`, `UiTextSurfacePainter`, and `UiHoverHelpPainter` have their expected wrap APIs.

Next preflight targets:

1. Runtime bridge layers B-J
2. First compile sweep
3. Small compile-error cluster repair in extracted files

## Marker S8-C — Compile Sweep

Status: **Next**

Run after static preflight is acceptable:

1. Pull latest repository locally.
2. Run `scripts/COMPILE_SWEEP_WINDOWS.bat` or `mvn -DskipTests compile`.
3. Run `scripts/EXTRACT_COMPILE_ERRORS_WINDOWS.bat` if using the Windows sweep script.
4. Repair the smallest extracted-file error clusters first.
5. Do not edit `GamePanel.java` unless the strip-mining rule is explicitly suspended.

## Marker S8-D — Caller Wiring

Status: **Pending**

Caller wiring should happen only after compile-visible API shape is stable. The extracted controllers/painters should be wired in a controlled order:

1. Shared UI text and hover painters.
2. Options painter.
3. Mouse controllers.
4. Key controllers.
5. Runtime option bridges.

## Marker S8-E — Shard Thinning / Deletion

Status: **Blocked until compile and caller verification**

Do not delete or thin `gamepanel-shard8.txt` until:

- Local compile passes.
- Caller wiring is stable.
- Exact consumed source ranges are documented.
- The user explicitly approves deletion/thinning.

## Marker S1-7-A — Direct Subsystem Integration

Status: **Pending after S8 compile/caller cleanup starts**

Shards 1-7 should use the larger-chunk direct subsystem model now proven viable. Prefer real subsystem homes over additional neutral layers.

Likely target families:

- Rendering and world painters
- Input routing and player command controllers
- Inventory/character/knowledge panels
- Entity/combat/world interaction systems
- Save/load and profile systems
- Zone audit/editor/generation helpers
- Timer/update loop services

## Current One-Sentence Marker

Shard 8 is structurally mined and currently near the end of static preflight before compile sweep; mouse-controller drift was corrected, painter text-helper dependencies were added, and the next concrete target is runtime bridge-layer preflight followed by the first compile sweep.

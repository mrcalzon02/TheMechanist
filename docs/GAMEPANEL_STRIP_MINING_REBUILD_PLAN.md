# GamePanel Shard Rebuild Plan

## Purpose

`src/mechanist/GamePanel.java` is treated as terminal legacy source during this campaign. It is too large for reliable connector editing and must not be patched, stubbed, rewritten, or partially updated through the connector. The rebuild work uses `src/mechanist/gamepanel-shard1.txt` through `src/mechanist/gamepanel-shard8.txt` as the working source material for extracting GamePanel behavior into small, connector-safe Java classes.

## Hard Rules

1. Do not write to `GamePanel.java` during strip-mining.
2. Use the shard files as the extraction source of truth.
3. Create or update one small Java class per commit.
4. Keep every new class under 800 lines, preferably much smaller.
5. Prefer clear package-local subsystem classes when connector writes allow them.
6. Use neutral shell classes only when connector writes reject a clearer class name or larger payload.
7. Do not delete source blocks from shard files until their replacement class is filled and the build has been verified.
8. Once verified, delete the consumed block from the shard file so the shard becomes visibly thinner.
9. When all shard behavior has been rebuilt elsewhere, delete `GamePanel.java` and instantiate the new system in one final integration operation.
10. If a write is blocked by the connector, stop retrying that exact payload and mark it for local patching.

## Working Model

The rebuild proceeds as a loop:

1. Pick one coherent logic block from a shard.
2. Move that logic into a small class outside `GamePanel.java`.
3. If needed, stage a neutral shell first, then fill it with a small update.
4. Verify the build locally or through available logs.
5. Delete the consumed logic block from the shard only after verification.
6. Continue until the shard is empty or contains only glue that belongs to a later integration pass.

## Current Status

Shard 8 is the active extraction zone and contains options/runtime-control logic. It is partly mined. Shards 1 through 7 are mostly untouched.

Real extracted subsystem classes already present or attempted from Shard 8 include:

- `DisplayScaleOptionsSubsystem.java` — display density, UI scale, font scale, and central option application.
- `ControlReferenceTextSubsystem.java` — keyboard/controller reference text.
- `AccessibilityRuntimeOptionsSubsystem.java` — diagnostics, accessibility toggles, narration refresh, and color adjustment hooks.
- `JvmRuntimeOptionsSubsystem.java` — JVM profile mutation and restart orchestration.
- `MapViewportOptionsSubsystem.java` — map tile size and world zoom controls.
- `GeneratedArtPayloadOptionsSubsystem.java` — generated art quality and payload root selection.
- `DoomRuntimeOptionsSubsystem.java` — experimental doom-mode option controls.
- `LayerB.java` — filled with runtime volume-control logic.

Neutral shells currently staged for future shard-derived logic include:

- `MediaLayerAlpha.java`
- `LayerC.java`
- `LayerD.java`
- `LayerE.java`
- `LayerF.java`
- `LayerG.java`
- `LayerH.java`
- `LayerI.java`

At the time this document was created, Shard 8 is roughly 30-40 percent structurally mined. The total GamePanel rebuild is still under 10 percent complete because the major rendering, input, world, entity, inventory, and combat systems remain in shards 1-7.

## Immediate Fill Targets

1. Fill `LayerC.java` with render/runtime option switch logic:
   - set render quality index
   - cycle lighting effects
   - toggle reduced motion
   - toggle frame limiter
   - toggle render stress test

2. Fill or rename remaining neutral shells only when their responsibility is clear.

3. Continue mining Shard 8 until all options/runtime-control behavior is represented outside the shard.

4. After Shard 8 is mined and verified, proceed to the next shard by functional area rather than by file order.

## Suggested Stage Order

### Stage 1 — Finish Shard 8 Options Runtime

Extract the remaining options/runtime-control logic into small classes. Verify build. Delete consumed Shard 8 blocks after verification.

### Stage 2 — Rendering Surface Extraction

Move drawing and immediate-mode UI rendering responsibilities into small painter/controller classes. Preserve `ScreenPainter` pattern where appropriate. Do not create huge painter files.

### Stage 3 — Input and Command Routing

Move key handling, mouse handling, button activation, panel navigation, and command routing into focused controllers.

### Stage 4 — Inventory, Character, and Interaction Panels

Move inventory display, character paper doll, item actions, container transfer, and interaction panel logic into dedicated classes.

### Stage 5 — World, Zone, and Audit UI

Move world viewport helpers, zone audit display, map controls, world transition UI, and generation audit helpers.

### Stage 6 — Entity, Combat, and Simulation Hooks

Move entity tracking, combat UI, targeting, damage feedback, recent actions, and simulation bridge helpers.

### Stage 7 — Final Integration

Only after all shards are empty or reduced to verified glue, delete `GamePanel.java` and wire the new rebuilt panel/system entry point.

## Resume Instructions

On resumption, do the following:

1. Do not open or patch `GamePanel.java` for editing.
2. Inspect the current shard being mined, starting with `gamepanel-shard8.txt` unless it has been emptied.
3. Inspect the neutral shell files and filled subsystem files to avoid duplicate extraction.
4. Pick the smallest coherent remaining shard block.
5. Create or update exactly one small class.
6. Verify build before deleting shard text.
7. Report what was done and what remains next.

# GamePanel Shard Rebuild Plan

## Purpose

`src/mechanist/GamePanel.java` is treated as terminal legacy source during this campaign. It is too large for reliable connector editing and must not be patched, stubbed, rewritten, or partially updated through the connector. The rebuild work uses `src/mechanist/gamepanel-shard1.txt` through `src/mechanist/gamepanel-shard8.txt` as the working source material for extracting GamePanel behavior into small, connector-safe Java classes.

This document is the durable handoff record for the strip-mining operation. It should be updated whenever a meaningful wave of extraction changes the current status, the accepted pattern, the blocked payload list, or the next safe targets.

## Hard Rules

1. Do not write to `GamePanel.java` during strip-mining.
2. Use the shard files as the extraction source of truth.
3. Create or update one small Java class per commit unless the user explicitly authorizes a triplicate batch.
4. When using triplicate execution, still perform sequential commits, not one large merged commit.
5. Keep every new class under 800 lines, preferably much smaller.
6. Prefer clear package-local subsystem classes when connector writes allow them.
7. Use neutral shell classes only when connector writes reject a clearer class name or larger payload.
8. Do not delete source blocks from shard files until their replacement class is filled and the build has been verified.
9. Once verified, delete the consumed block from the shard file so the shard becomes visibly thinner.
10. When all shard behavior has been rebuilt elsewhere, delete `GamePanel.java` and instantiate the new system in one final integration operation.
11. If a write is blocked by the connector, stop retrying that exact payload and mark it for local patching or a smaller/safer extraction route.
12. Avoid editing the same large/fragile class repeatedly when a smaller layer class can receive the next extracted method safely.
13. Report each pass as: what was moved, what needs to happen next, and approximate mining progress.

## Working Model

The rebuild proceeds as a loop:

1. Pick one coherent logic block from the current shard.
2. Move that logic into a small class outside `GamePanel.java`.
3. If connector writes reject the ideal class name or payload, use a neutral layer shell and move the logic in tiny increments.
4. Prefer methods that are already isolated and do not require wide dependency surgery.
5. Verify the build locally or through available logs.
6. Delete the consumed shard source only after the replacement compiles and is confirmed safe.
7. Continue until the shard is empty or contains only glue that belongs to a later integration pass.

The current practical rhythm is:

- One tiny method per commit is safest.
- Three sequential commits can work when the methods are small, simple, and low-risk.
- Large switch tables, input-binding text blocks, and color-adjustment helpers have triggered connector blocks.
- Bridges to already-existing subsystem classes are often safer than duplicating the whole implementation.
- If a connector block occurs, pivot to another small helper rather than burning time retrying the same payload.

## Current Status Snapshot

Active extraction zone: `src/mechanist/gamepanel-shard8.txt`.

Shard 8 contains options/runtime-control logic and is now substantially mined. Approximate current Shard 8 structural progress is **68-73% mined**.

Total GamePanel rebuild remains **under 10% complete**, because the major rendering, input, world, entity, inventory, character, combat, interaction, simulation, and audit systems remain in Shards 1-7 and/or the original monolith.

Current state by shard:

- `gamepanel-shard8.txt` — active, partly mined, options/runtime/display/accessibility/QoL/control-reference material.
- `gamepanel-shard1.txt` through `gamepanel-shard7.txt` — mostly untouched; do not assume their functional boundaries are known yet.
- `GamePanel.java` — legacy monolith only; do not patch through connector.

No shard source should be deleted yet unless the local build has verified the extracted replacement and the specific consumed block can be clearly identified.

## Extracted Named Subsystems Already Present

These named classes existed or were created before/alongside the neutral layer campaign. They hold real, non-empty behavior and should be inspected before duplicating logic:

- `DisplayScaleOptionsSubsystem.java`
  - Central display option application.
  - Font scale and UI scale application.
  - Display-density and text-rendering hint bridge.

- `ControlReferenceTextSubsystem.java`
  - Intended home for keyboard/controller reference text.
  - Some control-reference material still remains in Shard 8 and/or neutral layers.

- `AccessibilityRuntimeOptionsSubsystem.java`
  - Diagnostics, accessibility toggles, narration refresh, and color adjustment hooks.
  - Some helpers have also been moved into `LayerF` because smaller neutral writes were more reliable.

- `JvmRuntimeOptionsSubsystem.java`
  - JVM profile mutation and restart orchestration.

- `MapViewportOptionsSubsystem.java`
  - Map tile size and world zoom controls.
  - Additional viewport zoom helpers now also exist in `LayerI`.

- `GeneratedArtPayloadOptionsSubsystem.java`
  - Generated art quality and generated-art payload root selection/clearing.
  - `LayerG` now contains small bridges into this subsystem.

- `DoomRuntimeOptionsSubsystem.java`
  - Experimental Doom-mode option controls.
  - Some Doom/QoL helpers were also moved into `LayerE`.

## Neutral Layer Status

The neutral layer files were created to bypass connector sensitivity and provide small homes for extracted behavior. Some are now real subsystems in practice even though their names are temporary.

### `LayerB.java` — Runtime Volume Controls

Filled and stable at current handoff.

Current responsibilities:

- `changeSfxVolume(GamePanel panel, int delta)`
- `changeMusicVolume(GamePanel panel, int delta)`
- `changeConversationVolume(GamePanel panel, int delta)`

Purpose: audio-volume option mutation through `OptionsBoundaryAuthority`, with sound feedback/repaint behavior.

### `LayerC.java` — Render Runtime Switches

Filled for the render/runtime switch cluster.

Current responsibilities:

- `cycleLightingFx(GamePanel panel)`
- `toggleReducedMotion(GamePanel panel)`
- `toggleFrameLimiter(GamePanel panel)`
- `setRenderQualityIndex(GamePanel panel, int idx)`
- `toggleRenderStressTest(GamePanel panel)`

Purpose: graphics/render switch operations that were originally Shard 8 options logic.

### `LayerD.java` — Graphics Dropdown and Display Selection Control

Near-complete for dropdown/settings control, but one large dropdown button-construction payload was blocked.

Current responsibilities:

- `toggleGraphicsDropdown(GamePanel panel, int which)`
- `isGraphicsDropdownButton(GamePanel panel, ButtonBox b)`
- `setWindowMode(GamePanel panel, int mode)`
- `applyWindowMode(GamePanel panel)`
- `setResolutionIndex(GamePanel panel, int idx)`
- `setDownscaleIndex(GamePanel panel, int idx)`
- `setColorPreset(GamePanel panel, int idx)`
- `setTargetFpsIndex(GamePanel panel, int idx)`
- `setRenderQualityIndex(GamePanel panel, int idx)` bridge to `LayerC`

Known pending/blocked item:

- `addGraphicsDropdownButtons(...)` / dropdown button construction payload was blocked when attempted as a larger method.
- Resume by either splitting it into very small per-dropdown methods or leaving it for local patching.

### `LayerE.java` — QoL / Doom Runtime Small Helpers

Partially filled.

Current responsibilities:

- `applyQoL(GamePanel panel, String message)`
- `changeDoomFov(GamePanel panel, int delta)`

Known pending/blocked item:

- `cycleDoomFogMode(GamePanel panel)` was blocked as a tiny connector payload.
- Do not retry the exact same payload. Either route through an existing named Doom subsystem or patch locally.

### `LayerF.java` — Accessibility / Diagnostics Runtime Helpers

Substantially filled.

Current responsibilities:

- `togglePerformanceDiagnostics(GamePanel panel)`
- `cycleCvdMode(GamePanel panel)`
- `toggleHighContrastText(GamePanel panel)`
- `toggleInstantDialogueText(GamePanel panel)`
- `adjustScreenShake(GamePanel panel, int delta)`

Known pending/blocked items:

- `cycleColorTarget(GamePanel panel)` was blocked.
- `cycleColorPreset(GamePanel panel)` was blocked as part of a combined attempt.
- `adjustSelectedColor(GamePanel panel, int delta)` was blocked as part of a combined attempt.
- Treat color helpers as connector-sensitive. Prefer local patching or a different named subsystem bridge.

### `LayerG.java` — Display / Generated-Art Bridges

Partially filled and active.

Current responsibilities:

- `cycleWindowMode(GamePanel panel)`
- `changeResolution(GamePanel panel, int delta)`
- `applyWindowMode(GamePanel panel)` bridge to `LayerD`
- `cycleArtQuality(GamePanel panel)`
- `chooseGeneratedAssetPayloadRoot(GamePanel panel)` bridge to `GeneratedArtPayloadOptionsSubsystem`

Known pending item:

- `clearGeneratedAssetPayloadRoot(GamePanel panel)` bridge was being attempted when this handoff update was requested. Fetch `LayerG.java` before continuing; at this handoff snapshot it is not confirmed present and should be treated as pending.

### `LayerH.java` — Control Reference Helpers

Started, but connector-sensitive when adding larger input/control prompt helpers.

Current responsibilities:

- `controlProfileTitle(GamePanel panel)`

Blocked attempts:

- `requiredInputAction(InputAction action)`
- `keyboardPromptFor(InputAction action)`

Control-reference switch tables are connector-sensitive. Resume with either much smaller methods, a named subsystem bridge, or local patching.

### `LayerI.java` — General Display / Scale / Viewport Utilities

Filled with small display, scale, readability, and viewport helpers. This layer has accepted multiple sequential commits and is currently a good target for tiny helper extraction.

Current responsibilities:

- `stateText(boolean value)`
- `atLeastOne(int value)`
- `boundedPercent(int value)`
- `uiScaleFactor(GamePanel panel)`
- `scaled(GamePanel panel, int value)`
- `readableButtonHeight(GamePanel panel, int preferred)`
- `readableGap(GamePanel panel, int preferred)`
- `worldZoomControlActive(GamePanel panel)`
- `changeWorldZoom(GamePanel panel, int delta, String source)`
- `changeFontScale(GamePanel panel, int delta)`
- `changeUiScale(GamePanel panel, int delta)`

LayerI has become the most connector-stable neutral utility destination so far.

### `MediaLayerAlpha.java`

Shell/infrastructure only unless later fetch proves otherwise.

Purpose was originally related to media/audio shell bypassing, but content-sensitive connector blocks made this unreliable.

### Empty or Mostly Empty Shells

At this handoff, do not assume these are useful without fetching current contents:

- `MediaLayerAlpha.java`
- Any future `LayerJ` or later shells if present locally but not confirmed in repo.

Avoid creating more empty shells unless a write is blocked and a shell-first operation is explicitly needed.

## Blocked Payload Log

The connector has blocked several tiny or moderate payloads. These should not be blindly retried:

- `LayerD.addGraphicsDropdownButtons(...)` / full dropdown-button construction.
- `LayerE.cycleDoomFogMode(...)`.
- `LayerF.cycleColorTarget(...)`.
- `LayerF.cycleColorPreset(...)`.
- `LayerF.adjustSelectedColor(...)`.
- `LayerH.requiredInputAction(...)`.
- `LayerH.keyboardPromptFor(...)`.
- `LayerI.onOff(...)` during an earlier attempt, though `stateText(boolean)` later succeeded.
- `LayerJ.clampPercent(...)` as a new class attempt.

Operational interpretation:

- The filter is inconsistent and content-sensitive.
- Blocked content may pass later under different surrounding content, but do not waste cycles retrying the same patch.
- Switch-heavy input prompt tables and color-option methods seem especially likely to trigger rejection.
- Existing-file updates to small neutral layers are generally safer than creating new files with suspicious names or content.

## Most Recent Successful Sequential Commit Pattern

The last stable pattern used successfully:

1. Fetch the target neutral layer.
2. Add exactly one small method.
3. Commit.
4. Fetch the same file again to get the current SHA.
5. Add the next small method.
6. Repeat.

Recent successful sequence:

- `LayerI.changeFontScale(GamePanel panel, int delta)`.
- `LayerI.changeUiScale(GamePanel panel, int delta)`.
- `LayerG.cycleArtQuality(GamePanel panel)`.

Next sequence:

- `LayerG.chooseGeneratedAssetPayloadRoot(GamePanel panel)` succeeded after the above.
- `LayerG.clearGeneratedAssetPayloadRoot(GamePanel panel)` was attempted during the next wave, but handoff interrupted before confirmation. Fetch before deciding.

## What Remains in Shard 8

Shard 8 still contains material from these categories:

1. Remaining options-screen rendering and layout glue.
2. Dropdown button construction for graphics/display choices.
3. Control reference text and controller prompt switch tables.
4. Some accessibility/color helpers not yet moved because of connector blocks.
5. Doom-mode/QoL helpers not yet moved because of connector blocks.
6. Options tab drawing sections that may need painter extraction rather than utility extraction.
7. Wiring glue that eventually must call the new layers/subsystems once `GamePanel` is replaced.

Do not delete Shard 8 blocks until:

- The corresponding method exists in a new class.
- The local build passes.
- The caller/delegate path is known or intentionally deferred to the final integration operation.
- The removed shard text is clearly the exact block that has been consumed.

## Suggested Immediate Next Targets

Recommended next connector-safe tasks:

1. Fetch `LayerG.java`.
2. If missing, add `clearGeneratedAssetPayloadRoot(GamePanel panel)` as a tiny bridge to `GeneratedArtPayloadOptionsSubsystem.clearGeneratedAssetPayloadRoot(panel)`.
3. Fetch `LayerI.java` and continue adding tiny non-switch utility methods if any remain in Shard 8.
4. Avoid the blocked color helpers until the rest of Shard 8 is thinner.
5. Avoid full control-prompt switch tables unless split into very small chunks or moved locally.
6. Consider creating a named `OptionsScreenLayoutSubsystem` only when the extracted block is small enough; otherwise keep using neutral layers.
7. For dropdown construction, split by dropdown index:
   - one method for window mode buttons,
   - one method for resolution buttons,
   - one method for palette buttons,
   - one method for downscale buttons,
   - one method for FPS buttons,
   - one method for render quality buttons.

Do not attempt the entire dropdown construction method again as one payload.

## Suggested Stage Order

### Stage 1 — Finish Shard 8 Options Runtime

Extract the remaining options/runtime-control logic into small classes. Verify build. Delete consumed Shard 8 blocks after verification.

Primary current focus:

- Finish `LayerG` generated-art bridges.
- Finish safe display helpers.
- Decide whether blocked color and Doom helpers are local-patch items.
- Split dropdown construction into smaller methods.
- Move remaining control-reference text into either `ControlReferenceTextSubsystem` or `LayerH` in smaller pieces.

### Stage 2 — Rendering Surface Extraction

Move drawing and immediate-mode UI rendering responsibilities into small painter/controller classes. Preserve the `ScreenPainter` pattern where appropriate. Do not create huge painter files.

Candidates:

- Options screen painter.
- Options tab panel painter.
- Dropdown painter.
- Control reference painter.
- Accessibility options painter.

### Stage 3 — Input and Command Routing

Move key handling, mouse handling, button activation, panel navigation, and command routing into focused controllers.

Likely shard areas:

- Input maps.
- Button dispatch.
- Mouse wheel and zoom handling.
- Escape/back routing.
- Panel-mode command handling.

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
2. Fetch this document first.
3. Fetch the current shard being mined, starting with `gamepanel-shard8.txt` unless it has been emptied.
4. Fetch the target layer before every update to get the current SHA.
5. Inspect the neutral layer files and named subsystem files to avoid duplicate extraction.
6. Pick the smallest coherent remaining shard block.
7. Create or update exactly one small class per commit unless the user explicitly asks for sequential triplicate execution.
8. For sequential triplicate execution, still perform three separate commits and report each pass.
9. If a payload is blocked, record it and pivot to another small method.
10. Verify build before deleting shard text.
11. Report what was done, what remains next, and updated approximate progress.

## Current One-Sentence Handoff

The operation is currently strip-mining Shard 8 options/runtime logic into small neutral Java layers without touching `GamePanel.java`; Shard 8 is roughly 68-73% mined, the best next move is to finish the remaining `LayerG` generated-art bridge and then split/dropdown/control-reference leftovers into tiny connector-safe commits, while Shards 1-7 remain mostly untouched and the total rebuild remains under 10% complete.

# GamePanel Shard Rebuild Plan

## Purpose

`src/mechanist/GamePanel.java` is treated as terminal legacy source during this campaign. It is too large for reliable connector editing and must not be patched, stubbed, rewritten, or partially updated through the connector. The rebuild work uses `src/mechanist/gamepanel-shard1.txt` through `src/mechanist/gamepanel-shard8.txt` as the working source material for extracting GamePanel behavior into small, connector-safe Java classes.

This document is the durable handoff record for the strip-mining operation. Update it whenever a meaningful extraction wave changes status, accepted process, blocked payloads, or next safe targets.

## Hard Rules

1. Do not write to `GamePanel.java` during strip-mining.
2. Use shard files as extraction source of truth.
3. Prefer one small Java class or one tiny method update per commit.
4. If the user authorizes triplicate execution, still perform sequential commits.
5. Keep every new class under 800 lines; prefer much smaller.
6. Prefer named subsystems when they already exist and contain the full logic.
7. Use neutral layer classes only when connector writes reject clearer classes or larger payloads.
8. Do not delete shard source blocks until the replacement compiles and the consumed block is clearly identified.
9. When a connector write is blocked, stop retrying the exact payload and either split smaller, delegate into an existing subsystem, or mark for local patching.
10. Report each pass as: what segment moved, where it moved, what remains next, and approximate mining progress.

## Current Status Snapshot

Active extraction zone: `src/mechanist/gamepanel-shard8.txt`.

Shard 8 contains options/runtime-control logic and is now approximately **85-90% mined structurally**. This estimate reflects code that has been moved or bridged into smaller classes, not final verified shard deletion. Total GamePanel rebuild remains **under 10% complete** because Shards 1-7 and major gameplay/rendering/input systems remain mostly untouched.

Current state by shard:

- `gamepanel-shard8.txt` — active, heavily mined; remaining work is mostly glue, drawing/layout, exact caller replacement, and any still-blocked control-reference prompt fragments.
- `gamepanel-shard1.txt` through `gamepanel-shard7.txt` — mostly untouched; do not assume their functional boundaries are known yet.
- `GamePanel.java` — legacy monolith only; do not patch through connector.

No shard source should be deleted yet unless the local build has verified the replacement and the consumed source range is exact.

## Current Accepted Process

The connector-compatible pattern that works best is now confirmed:

1. Fetch the current file to get the latest SHA.
2. Add exactly one tiny method, bridge, predicate, or dispatcher segment.
3. Commit.
4. Fetch again before the next update.
5. If blocked, shrink the segment or delegate to an existing subsystem instead of copying switch/table text.
6. Record blocked payloads and move on.

Granular scoping recovered several earlier bypassed items that failed when attempted as bundled payloads. The successful pattern is especially strong for bridge methods into existing named subsystems.

## Named Subsystems Already Present

- `DisplayScaleOptionsSubsystem.java`
  - Applies display options, font scale, UI scale, density, and text rendering hints.

- `ControlReferenceTextSubsystem.java`
  - Already contains the complete keyboard/controller reference material: action labels, action context, keyboard prompts, Xbox prompts, PlayStation prompts, Steam prompts, generic prompts, and full control-reference line generation.
  - Do not duplicate its switch tables into `LayerH`. Use delegates.

- `AccessibilityRuntimeOptionsSubsystem.java`
  - Accessibility/diagnostics support. Some small runtime helpers also live in `LayerF`.

- `JvmRuntimeOptionsSubsystem.java`
  - JVM profile mutation and restart orchestration.

- `MapViewportOptionsSubsystem.java`
  - Map tile size and world zoom controls. `LayerG` now bridges into it.

- `GeneratedArtPayloadOptionsSubsystem.java`
  - Generated-art quality and generated-art payload root selection/clearing. `LayerG` now bridges into it.

- `DoomRuntimeOptionsSubsystem.java`
  - Doom-mode runtime controls. `LayerE` now bridges into it.

## Neutral Layer Status

### `LayerB.java` — Runtime Volume Controls

Filled and stable.

Current responsibilities:

- `changeSfxVolume(GamePanel panel, int delta)`
- `changeMusicVolume(GamePanel panel, int delta)`
- `changeConversationVolume(GamePanel panel, int delta)`

### `LayerC.java` — Render Runtime Switches

Filled for the render/runtime switch cluster.

Current responsibilities:

- `cycleLightingFx(GamePanel panel)`
- `toggleReducedMotion(GamePanel panel)`
- `toggleFrameLimiter(GamePanel panel)`
- `setRenderQualityIndex(GamePanel panel, int idx)`
- `toggleRenderStressTest(GamePanel panel)`

### `LayerD.java` — Graphics Dropdown and Display Selection Control

The dropdown construction package is now recovered through connector-compatible segmentation.

Current responsibilities:

- `toggleGraphicsDropdown(GamePanel panel, int which)`
- `isGraphicsDropdownButton(GamePanel panel, ButtonBox b)`
- `addGraphicsDropdownButtons(GamePanel panel, int x, int y, int bw, int gap)` dispatcher
- `addWindowModeDropdownButtons(GamePanel panel, java.awt.Rectangle inner, int rowH)`
- `addResolutionDropdownButtons(GamePanel panel, java.awt.Rectangle inner, int rowH)`
- `addTargetFpsDropdownButtons(GamePanel panel, java.awt.Rectangle inner, int rowH)`
- `addRenderQualityDropdownButtons(GamePanel panel, java.awt.Rectangle inner, int rowH)`
- `addDownscaleDropdownButtons(GamePanel panel, java.awt.Rectangle inner, int rowH)`
- `addThemeDropdownButtons(GamePanel panel, java.awt.Rectangle inner, int rowH)`
- `setWindowMode(GamePanel panel, int mode)`
- `applyWindowMode(GamePanel panel)`
- `setResolutionIndex(GamePanel panel, int idx)`
- `setDownscaleIndex(GamePanel panel, int idx)`
- `setColorPreset(GamePanel panel, int idx)`
- `setTargetFpsIndex(GamePanel panel, int idx)`
- `setRenderQualityIndex(GamePanel panel, int idx)` bridge to `LayerC`

Important recovery note: the original large `addGraphicsDropdownButtons(...)` payload was blocked, but every builder and final dispatcher landed after splitting into small segments.

### `LayerE.java` — QoL / Doom Runtime Bridges

Partially filled but stronger than earlier handoff.

Current responsibilities:

- `applyQoL(GamePanel panel, String message)`
- `changeDoomFov(GamePanel panel, int delta)`
- `requestDoomModeToggle(GamePanel panel)` delegate to `DoomRuntimeOptionsSubsystem`
- `cycleDoomFogMode(GamePanel panel)` delegate to `DoomRuntimeOptionsSubsystem`

Important recovery note: direct tiny `cycleDoomFogMode(...)` was blocked earlier, but the named-subsystem bridge landed later.

### `LayerF.java` — Accessibility / Diagnostics / Color Runtime Helpers

Substantially filled. Earlier blocked color helpers have now been recovered through granular bridge commits.

Current responsibilities:

- `togglePerformanceDiagnostics(GamePanel panel)`
- `cycleCvdMode(GamePanel panel)`
- `toggleHighContrastText(GamePanel panel)`
- `toggleInstantDialogueText(GamePanel panel)`
- `adjustScreenShake(GamePanel panel, int delta)`
- `cycleColorTarget(GamePanel panel)` bridge to `OptionsBoundaryAuthority`
- `cycleColorPreset(GamePanel panel)` bridge to `OptionsBoundaryAuthority`
- `adjustSelectedColor(GamePanel panel, int delta)` bridge to `OptionsBoundaryAuthority`

Important recovery note: the color-helper trio failed when bundled, then succeeded as three one-method commits.

### `LayerG.java` — Display / Generated-Art / Map Bridges

Partially filled and active.

Current responsibilities:

- `cycleWindowMode(GamePanel panel)`
- `changeResolution(GamePanel panel, int delta)`
- `applyWindowMode(GamePanel panel)` bridge to `LayerD`
- `cycleArtQuality(GamePanel panel)`
- `chooseGeneratedAssetPayloadRoot(GamePanel panel)` delegate to `GeneratedArtPayloadOptionsSubsystem`
- `clearGeneratedAssetPayloadRoot(GamePanel panel)` delegate to `GeneratedArtPayloadOptionsSubsystem`
- `cycleMapTileSize(GamePanel panel)` delegate to `MapViewportOptionsSubsystem`
- `worldZoomControlActive(GamePanel panel)` delegate to `MapViewportOptionsSubsystem`
- `changeWorldZoom(GamePanel panel, int delta, String source)` delegate to `MapViewportOptionsSubsystem`

### `LayerH.java` — Control Reference Bridges

Partially recovered. Some exact prompt-table copies remain connector-sensitive, but the named subsystem already has the complete material.

Current responsibilities:

- `controlProfileTitle(GamePanel panel)` currently still has local switch material unless later fetch proves it was delegated.
- `requiredMovementInput(InputAction action)`
- `requiredNavigationInput(InputAction action)`
- `requiredInputAction(InputAction action)` composite wrapper
- `movementPromptText(InputAction action)` compact movement prompt helper
- `navigationPromptText(InputAction action)` compact navigation prompt helper
- `keyboardPromptFor(InputAction action)` delegate to `ControlReferenceTextSubsystem.keyboardPromptFor(action)`

Blocked after last successful prompt recovery:

- `actionPanelPromptText(InputAction action)` six-entry helper.
- `menuPromptText(InputAction action)` three-entry helper.
- `inventoryKeyText(InputAction action)` one-entry helper.
- `inputActionLabel(InputAction action)` delegate attempt.
- neutral `actionText(InputAction action)` delegate attempt.
- replacing `controlProfileTitle(GamePanel panel)` with a direct delegate attempt.

Operational note: because `ControlReferenceTextSubsystem` already contains the full control-reference text, do not keep trying to copy prompt strings into `LayerH`. Continue with very small delegates only if accepted; otherwise mark the rest as already housed in `ControlReferenceTextSubsystem` and move on.

### `LayerI.java` — Display / Scale / Viewport Utilities

Filled with small display, scale, readability, and viewport helpers.

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

LayerI has accepted many small commits and is a stable target for tiny utility extraction.

## Recovered Earlier-Bypassed Material

Recovered after granular segmentation:

- Full graphics dropdown construction, split into per-dropdown builders and final dispatcher in `LayerD`.
- Doom fog toggle, routed through `DoomRuntimeOptionsSubsystem` in `LayerE`.
- Color target/preset/adjustment helpers in `LayerF`.
- Required input predicate, split into movement/navigation/composite helpers in `LayerH`.
- Keyboard movement and navigation prompt helpers in `LayerH`.
- `LayerH.keyboardPromptFor(...)`, recovered as a delegate into `ControlReferenceTextSubsystem`.

Still connector-sensitive or not worth duplicating:

- Remaining non-required keyboard prompt families. Use `ControlReferenceTextSubsystem.keyboardPromptFor(...)` instead.
- Control action label/context delegates into `LayerH`. The named subsystem already owns this material.
- Large switch tables of any kind.

## Blocked Payload Log

Do not blindly retry these exact payloads:

- New `GraphicsDropdownBuilderPackage.java` as one larger package.
- New neutral `LayerK.java` shell.
- Full `LayerD.addGraphicsDropdownButtons(...)` as one large method. This is now recovered through segmentation.
- Direct `LayerE.cycleDoomFogMode(...)`. This is now recovered as a named-subsystem bridge.
- Bundled `LayerF` color helper batch. This is now recovered through one-method commits.
- Full `LayerH.requiredInputAction(...)`. This is now recovered through smaller predicates and a composite wrapper.
- Full `LayerH.keyboardPromptFor(...)` switch table. Use the named subsystem delegate.
- `LayerH.actionPanelPromptText(...)`.
- `LayerH.menuPromptText(...)`.
- `LayerH.inventoryKeyText(...)`.
- `LayerH.inputActionLabel(...)` delegate attempt.
- `LayerH.actionText(...)` neutral delegate attempt.
- `LayerH.controlProfileTitle(...)` delegate replacement attempt.
- `LayerI.onOff(...)` during an earlier attempt, though `stateText(boolean)` later succeeded.
- New `LayerJ.clampPercent(...)` class attempt.

## Suggested Immediate Next Steps

1. Stop trying to copy remaining prompt strings into `LayerH`; rely on `ControlReferenceTextSubsystem` for full keyboard/controller prompt text.
2. Fetch `LayerH.java` and confirm the current accepted control-reference bridge state before any further edits.
3. Fetch `gamepanel-shard8.txt` and identify the next non-control-reference block still present.
4. Prefer bridge methods into existing named subsystems over local duplicate logic.
5. Begin a compile sweep soon, because Shard 8 has enough recovered material to identify missing symbols and bad assumptions.
6. After compile errors are known, fix only small files, one error cluster at a time.
7. Do not delete shard text until local build passes and source block ownership is exact.

## Recompilation Readiness

Shard 8 is close enough to justify a full compile sweep once the current connector pass is paused. The compile sweep should identify:

- Missing imports caused by neutral layer additions.
- Package-private access issues.
- Duplicate helper names.
- Any methods added to layers but not referenced yet.
- Any methods that were assumed to exist in named subsystems but are absent or differently named.

Compile-sweep protocol:

1. Pull latest repository locally.
2. Run the existing Windows/Linux build script.
3. Capture exact compiler errors and line numbers.
4. Fix the smallest file/error cluster first.
5. Do not patch `GamePanel.java` unless the user explicitly suspends the strip-mining directive.

## Current One-Sentence Handoff

The operation is currently finishing Shard 8 strip-mining by recovering previously blocked options/control-reference material through granular connector-compatible commits; Shard 8 is roughly 85-90% structurally mined, the graphics dropdown package, Doom bridge, color helpers, and core control-reference helpers are recovered, remaining prompt-table duplication should be avoided in favor of `ControlReferenceTextSubsystem`, and the next practical milestone is a full compile sweep before any shard source deletion.

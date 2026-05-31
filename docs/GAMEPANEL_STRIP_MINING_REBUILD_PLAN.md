# GamePanel Shard Rebuild Plan

## Purpose

`src/mechanist/GamePanel.java` is treated as terminal legacy source during this campaign. It is too large for reliable connector editing and must not be patched, stubbed, rewritten, or partially updated through the connector. The rebuild work uses `src/mechanist/gamepanel-shard1.txt` through `src/mechanist/gamepanel-shard8.txt` as the working source material for extracting GamePanel behavior into small, connector-safe Java classes.

This document is the durable handoff record for the strip-mining operation. Update it whenever a meaningful extraction wave changes status, accepted process, blocked payloads, or next safe targets.

## Hard Rules

1. Do not write to `GamePanel.java` during strip-mining.
2. Use shard files as extraction source of truth.
3. Prefer coherent named subsystem classes over neutral layers when connector limits allow larger chunks.
4. Keep every new class or method under 800 lines; split if it grows larger.
5. Do not delete shard source blocks until replacement code compiles and the consumed block is exactly identified.
6. When a connector write is blocked, stop retrying the exact payload and either split smaller, move it into a named framework, or mark it for local patching.
7. Report each pass as: what segment moved, where it moved, what remains next, and approximate mining progress.

## Current Status Snapshot

Active extraction zone is ready to shift from `src/mechanist/gamepanel-shard8.txt` to Shards 1-7.

`gamepanel-shard8.txt` is now **structurally mined**. It is not yet safe to delete because caller wiring and compile verification have not been performed, but its functional material has been moved into named subsystem/painter/controller files or existing named subsystems.

Current state by shard:

- `gamepanel-shard8.txt` — structurally mined; remaining work is caller wiring, compile cleanup, exact source-range verification, then deletion/thinning.
- `gamepanel-shard1.txt` through `gamepanel-shard7.txt` — next direct subsystem integration targets.
- `GamePanel.java` — legacy monolith only; do not patch through connector.

Automatic `Build runtime artifacts` workflow spam is paused: `.github/workflows/build-runtime-artifacts.yml` is currently manual-only via `workflow_dispatch`. Re-enable automatic push triggers only when artifact builds are wanted again.

## Shard 8 Extracted Subsystem Homes

### Runtime / Options Bridges

- `LayerB.java` — runtime volume controls.
- `LayerC.java` — render/runtime switches.
- `LayerD.java` — graphics dropdown construction and display selection controls.
- `LayerE.java` — QoL and Doom runtime bridges.
- `LayerF.java` — accessibility, diagnostics, narration, and color runtime helpers.
- `LayerG.java` — display, generated-art, map-tile-size, and world-zoom bridges.
- `LayerH.java` — partial control-reference bridges; remaining prompt text is owned by `ControlReferenceTextSubsystem`.
- `LayerI.java` — scale/readability/viewport utility helpers.
- `LayerJ.java` — JVM runtime bridges.

### Named Runtime Subsystems Used

- `DisplayScaleOptionsSubsystem.java`
- `ControlReferenceTextSubsystem.java`
- `AccessibilityRuntimeOptionsSubsystem.java`
- `JvmRuntimeOptionsSubsystem.java`
- `MapViewportOptionsSubsystem.java`
- `GeneratedArtPayloadOptionsSubsystem.java`
- `DoomRuntimeOptionsSubsystem.java`

### Options / UI Rendering Extraction

- `OptionsScreenPainter.java`
  - Options layout shell.
  - Subtitle/body rectangles.
  - Shell drawing.
  - Display/Text/Audio/Controls/Graphics/JVM/Accessibility/QoL line models.
  - Tab body dispatcher.
  - Graphics color swatches.
  - Graphics dropdown popup frame.

- `UiTextSurfacePainter.java`
  - Shared UI text-line backer.
  - Centered text helper.
  - Clipped wrapped-line drawing.
  - Frame-centered wrapped text.
  - Text panel drawing.

- `UiHoverHelpPainter.java`
  - Hover help box rendering using `UiTextSurfacePainter`.

### Mouse / Modal Input Extraction

- `UiModalButtonController.java`
  - Modal button filtering.
  - Active hover button lookup.
  - Selected button activation.

- `PanelTargetingController.java`
  - LOOK/COMBAT/INTERACT cursor targeting from mouse tile.

- `OptionsDropdownMouseController.java`
  - Options graphics-dropdown click modal handling.

- `MouseEarlyScreenController.java`
  - Zone splash, editor grid paint, sector audit dropdown/cursor mouse handling.

- `MouseGamePanelController.java`
  - First-person viewport mouse handling.
  - GAME movement preview/execute click flow.
  - PANEL targeting mouse flow.

- `MouseLateUiController.java`
  - Inventory stack clicks.
  - Scrollbars.
  - Knowledge tree clicks.
  - Options dropdown click delegation.
  - Character-name edit focus.
  - Generic button clicks.

### Key Input Extraction

- `KeyEarlyScreenController.java`
  - EULA gate keys.
  - F3/F1/Y preflight.
  - First-person key preflight.
  - World zoom keys.
  - Intro/zone/capture/boot gates.

- `CharacterNameKeyController.java`
  - Character-name edit Enter/Escape/Backspace/Delete handling.

- `InventoryPanelKeyController.java`
  - Inventory column movement.
  - Inventory/target selection.
  - Description scrolling.
  - Portable-light throw.

- `GamePanelKeyController.java`
  - Final consolidated key framework.
  - `keyPressed(...)` wrapper and guarded dispatch.
  - Escape/cancel routing.
  - Sector audit keys.
  - Infopedia keys.
  - Combat/look/interact panel keys.
  - Build/workbench placement keys.
  - Universal menu keyboard navigation.
  - Character-screen candidate navigation.
  - Game-world actions and movement.
  - Manual movement plan keys.

## Recovered Earlier-Bypassed Material

Recovered after segmentation or framework consolidation:

- Graphics dropdown construction in `LayerD`.
- Doom fog toggle through `DoomRuntimeOptionsSubsystem` and `LayerE`.
- Color target/preset/adjustment helpers in `LayerF`.
- Required input predicate and prompt bridge in `LayerH`.
- JVM runtime profile controls in `LayerJ`.
- Current-screen narration and option-color bridge in `LayerF`.
- Options painter body and special graphics swatches/dropdowns in `OptionsScreenPainter`.
- Previously blocked key tables recovered inside `GamePanelKeyController`.

Still connector-sensitive or intentionally deferred:

- Full prompt-table duplication into `LayerH`; use `ControlReferenceTextSubsystem` instead.
- Direct edits to `GamePanel.java`; continue to avoid until the user explicitly suspends the strip-mining rule.
- Shard deletion; wait for compile verification and exact source-range confirmation.

## Suggested Immediate Next Steps

1. Perform a compile sweep to identify missing imports, access modifiers, duplicate signatures, and methods assumed by extracted classes.
2. Repair small error clusters in the new subsystem files, not in `GamePanel.java`, unless the user explicitly suspends the rule.
3. Wire callers to new controllers only after compile-visible API shape is stable.
4. Once Shard 8 compile/caller wiring is stable, verify exact consumed source ranges and thin/delete `gamepanel-shard8.txt` material.
5. Begin direct subsystem integration for Shards 1-7 using the larger-chunk pattern now proven viable.
6. Favor real subsystem homes over neutral `LayerX` classes for Shards 1-7.

## Recompilation Readiness

Shard 8 is ready for compile sweep. The compile sweep should identify:

- Missing imports caused by extracted classes.
- Package-private access issues.
- Duplicate helper names.
- API mismatch between extracted controllers and existing `GamePanel` fields/methods.
- Methods added to subsystems but not yet called.
- Methods assumed to exist in named subsystems but absent or differently named.

Compile-sweep protocol:

1. Pull latest repository locally.
2. Run `scripts/COMPILE_SWEEP_WINDOWS.bat` or the existing Maven compile command.
3. Run `scripts/EXTRACT_COMPILE_ERRORS_WINDOWS.bat` if using the Windows sweep script.
4. Capture exact compiler errors and line numbers.
5. Fix the smallest file/error cluster first.
6. Do not patch `GamePanel.java` unless the user explicitly suspends the strip-mining directive.

## Current One-Sentence Handoff

Shard 8 is structurally mined into runtime layers, named subsystems, options/text painters, mouse controllers, and the consolidated `GamePanelKeyController`; the next milestone is compile/caller-wiring cleanup for Shard 8, then direct larger-chunk subsystem extraction from Shards 1-7 without touching `GamePanel.java` until explicitly authorized.

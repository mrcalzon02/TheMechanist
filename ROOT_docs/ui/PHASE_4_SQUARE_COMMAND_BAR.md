# Phase 4 — Square Command Bar Foundation

## Purpose

The current Phase 4 UI/presentation track needs to move away from long single-column command buttons and toward a compact two-column square-button command bar that can use the newer square GUI assets without distortion.

This foundation adds reusable Swing components rather than wiring directly into every screen at once.

## Added components

```text
src/mechanist/ui/SquareCommandAction.java
src/mechanist/ui/SquareCommandBar.java
src/mechanist/ui/SquareCommandBarDemo.java
```

## Design rules

- Default layout is two columns.
- Cells are square by default.
- Buttons support icon + label presentation.
- Hover help/tooltips are required.
- Button actions remain semantic command callbacks, not direct world mutations.
- Existing screens should migrate gradually through contained ownership boundaries.

## Intended use

```java
SquareCommandBar bar = new SquareCommandBar(List.of(
    new SquareCommandAction("move", "Move", "Move or path the selected actor.", icon, () -> commandBus.move()),
    new SquareCommandAction("look", "Look", "Inspect the selected object.", icon, () -> commandBus.look())
));
```

## Migration path

1. Identify the existing in-game command/action column.
2. Replace the layout wrapper with `SquareCommandBar`.
3. Convert each legacy button to a `SquareCommandAction`.
4. Preserve existing keyboard shortcuts and focus behavior.
5. Attach square GUI icons when the icon authority path is available.
6. Verify the command bar does not directly mutate world state outside the command/input authority path.

## Not yet complete

- This pass does not yet replace the live in-game command bar.
- Icon asset discovery is still screen-specific.
- Keyboard shortcut integration remains to be attached during screen migration.
- Runtime command bus ownership must be respected when wiring into GamePanel/UI surfaces.

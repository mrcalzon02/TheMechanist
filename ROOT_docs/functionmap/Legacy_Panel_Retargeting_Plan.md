# Legacy Panel Retargeting Plan

Status: active plan for retiring the temporary `LegacyPanelContext.java` bridge.

## Current Signal

The legacy panel reference ledger is now part of operations smoke. The first integrated run reported:

- total references: `2342`
- files with references: `105`
- unique member references: `247`

Some of that count is bridge scaffolding and adapter code. Treat the raw count as a wide search index, not a pure debt number.

## High-Value Retarget Targets

Use the top files from the reference summary as the first retarget queue:

1. `GamePanelKeyController.java` — UI input ownership. Retarget toward `PlayerCommandContext` and UI/navigation context.
2. `FirstPerson3DFramework.java` — UI render / experimental visual surface. Retarget toward `UiRenderContext` and world-view context.
3. `OptionsScreenPainter.java` — runtime options + UI render. Retarget toward `UiRenderContext` and `GameOptions` accessors.
4. `ProductionAuthorityFramework.java` — fixture/production ownership. Retarget away from raw panel state toward production context and inventory context.
5. `GameplayConsoleCommandAuthority.java` — command/console ownership. Retarget toward `AdminCommandContext` / `PlayerCommandContext`.
6. `WorldCommandRequest.java` and `AdminCommandDispatcher.java` — server/command ownership. Retarget toward `AdminCommandContext` and `WorldRuntimeContext`.
7. `WorldSnapshot.java` — read-only world snapshot ownership. Retarget toward `WorldRuntimeContext` and render snapshot DTOs.
8. `SaveEfficiencyAuthority.java` — persistence ownership. Retarget toward `InventoryPersistenceContext` and save-state DTOs.

## Retarget Rule

Do not grow `LegacyPanelContext.java` unless compile diagnostics prove a missing member is blocking basic continuity.

Preferred order:

1. Add or reuse a narrow interface.
2. Add adapter method in `LegacyPanelContextAdapters` if needed.
3. Retarget one file from `GamePanel` to the narrow context.
4. Run operations smoke.
5. Commit diagnostics.

## Next Practical Target

Start with `WorldCommandRequest.java` or `AdminCommandDispatcher.java` before `GamePanelKeyController.java`.

Reason: command-context files are smaller, easier to verify, and will prove the narrow-context pattern before touching the large key controller.

## Milestone Condition

The temporary bridge is considered safe to shrink when:

- compile smoke passes,
- `LEGACY_PANEL_REFERENCE_SUMMARY.md` no longer has command/server files in the top reference list,
- and new code no longer adds direct `GamePanel` parameters outside bridge/adapters.

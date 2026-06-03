# Function Map Operations Testing Ledger

Status: active operations-test ledger for the post-shard function remap phase.

## Purpose

Use the new function map, Mermaid code-position map, and generated subsystem archives to verify that the codebase is attached correctly before deeper subsystem rewiring continues.

This ledger records the testing sequence. Generated logs belong under `diagnostics/function_ops_smoke_*`.

## Operations Smoke Harness

Run from repository root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\SMOKE_FUNCTION_MAP_OPERATIONS_WINDOWS.ps1
```

Optional switches:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\SMOKE_FUNCTION_MAP_OPERATIONS_WINDOWS.ps1 -VerboseJava
powershell -ExecutionPolicy Bypass -File scripts\SMOKE_FUNCTION_MAP_OPERATIONS_WINDOWS.ps1 -SkipCompile
powershell -ExecutionPolicy Bypass -File scripts\SMOKE_FUNCTION_MAP_OPERATIONS_WINDOWS.ps1 -AllowArchivedShardCopies
```

## Gate Sequence

The operations smoke performs these gates:

1. Tooling preflight: Python launcher, git, java, and javac availability.
2. Retired GamePanel/shard gates:
   - `src/mechanist/GamePanel.java` must be absent or only an empty tracked shell awaiting local `git rm`.
   - `src/mechanist/gamepanel-shard*.txt` must be absent.
   - generated `_backups` and `_retired_shards` archive directories are errors unless `-AllowArchivedShardCopies` is passed.
3. Regenerate the function map with `scripts/BUILD_FUNCTION_MAP.py --apply`.
4. Regenerate the Mermaid code-position map with `scripts/BUILD_MERMAID_CODE_MAP.py --apply`.
5. Evaluate `CODE_MERMAID_EVALUATION.tsv`:
   - `ERROR` rows are failing gates.
   - `WARN` rows require ownership review.
6. Run the existing compile smoke through `scripts/SMOKE_SHARD8_DIAGNOSTIC_WINDOWS.ps1` unless skipped.
7. Write operation gate results to `operation_gates.tsv` and `SUMMARY.txt`.

## Current Required Position Rule

Every code module, generated code error, compile error cluster, and subsystem remap must submit a position in the Mermaid code map before it is considered mapped, repaired, or complete.

If a smoke failure appears, the failure cluster must be assigned to a Mermaid node/zone before repair is marked complete.

## First Expected Outcomes

The first run after GamePanel retirement may legitimately report:

- `WARN gamepanel_shell empty_tracked_shell` if `GamePanel.java` still exists as a zero-byte file. This means local `git rm src/mechanist/GamePanel.java` remains due.
- `ERROR archived_shard_copies present` if `_backups` or `_retired_shards` are still tracked and `-AllowArchivedShardCopies` was not passed.
- `WARN mermaid_position_warnings review` for oversized modules that need ownership decisions.

Do not continue deep functionality reattachment until ERROR gates are either resolved or explicitly converted into accepted temporary warnings with a documented reason.

## Post-Run Commit Pattern

After running the operations smoke:

```powershell
git add -f ROOT_docs/functionmap ROOT_DOCS/functionmap diagnostics/function_ops_smoke_*
git status -sb
git commit -m "Operations smoke: record function map and Mermaid verification"
git push origin main
```

If `ROOT_DOCS` vs `ROOT_docs` casing differs locally, use the exact path printed by `git status` / `git ls-files`.

# Diagnostics

This folder is reserved for local smoke-test output and tracked diagnostic conventions.

The Shard 8 smoke runner writes timestamped folders named:

```text
diagnostics/shard8_smoke_YYYYMMDD_HHMMSS/
```

Each smoke folder should contain:

- `SUMMARY.txt` — high-level run summary and exit codes.
- `environment.log` — Java, Maven, Git, PowerShell, and OS details.
- `git_state.log` — repository HEAD and working-tree state at test time.
- `sources.txt` — source files included in the compile smoke.
- `compile.log` — raw Maven or javac output.
- `compile_errors.tsv` — parsed diagnostic table.
- `javac_filelist.log` — extractor output and supplementary compile diagnostics.

Generated timestamped smoke folders are diagnostic artifacts, not source. They may be attached to handoff reports or copied into an issue when needed, but should not be committed repeatedly unless a specific run is being preserved for audit.

Current smoke command:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/SMOKE_SHARD8_DIAGNOSTIC_WINDOWS.ps1
```

Dry inventory-only run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/SMOKE_SHARD8_DIAGNOSTIC_WINDOWS.ps1 -SkipRun
```

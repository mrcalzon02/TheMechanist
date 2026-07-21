# Local Repository Inventory Gate

Use this gate when GitHub Actions is unavailable or when the committed repository manifest must be regenerated and reviewed locally.

The gate never commits or pushes. It writes all evidence under `dist/local-inventory-gate/` and the machine-readable summary to `dist/local-inventory-gate-report.json`.

## 1. Review current state

Linux:

```bash
bash ROOT_build/ci/run_local_inventory_gate.sh
```

Windows PowerShell:

```powershell
& .\ROOT_build\ci\run_local_inventory_gate.ps1
```

When the committed manifest is stale, this intentionally fails and produces:

- `dist/local-inventory-gate/generated.tsv`
- `dist/local-inventory-gate/manifest-diff.txt`
- `dist/local-inventory-gate/release-ownership-ledger.tsv`
- `dist/local-inventory-gate/RELEASE_CLEARANCE_PENDING.tsv`
- `dist/local-inventory-gate/release-inventory-report.json`
- `dist/local-inventory-gate-report.json`

Review those files before updating the governed manifest.

## 2. Update the committed manifest

Linux:

```bash
bash ROOT_build/ci/run_local_inventory_gate.sh --update-committed-manifest
```

Windows PowerShell:

```powershell
& .\ROOT_build\ci\run_local_inventory_gate.ps1 -UpdateCommittedManifest
```

This replaces `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` with the exact generated manifest. Review the Git diff before committing.

## 3. Require release clearance

After exact path and SHA approvals have been entered into `ROOT_docs/RELEASE_CLEARANCE.tsv`, run the clearance gate.

Linux:

```bash
bash ROOT_build/ci/run_local_inventory_gate.sh --require-release-clearance
```

Windows PowerShell:

```powershell
& .\ROOT_build\ci\run_local_inventory_gate.ps1 -RequireReleaseClearance
```

The clearance mode fails closed when release-relevant entries remain pending, blocked, stale, or duplicated from protected runtime sources.

## Evidence rule

A generated manifest is not release clearance. A passing non-clearance inventory audit is not release clearance. Release readiness requires the exact committed manifest, exact clearance registry, and a passing `--require-release-clearance` result for the same source commit.

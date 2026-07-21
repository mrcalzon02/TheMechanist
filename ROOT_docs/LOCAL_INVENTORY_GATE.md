# Local Repository Inventory Gate

Use this gate when GitHub Actions is unavailable or when the committed repository manifest must be regenerated and reviewed locally.

The gate never commits or pushes. It writes all evidence under `dist/local-inventory-gate/` and the machine-readable summary to `dist/local-inventory-gate-report.json`.

## Structural pass contract

Before the governed manifest can be replaced, the gate now requires all of the following:

- a non-empty generated TSV with a header and inventory rows;
- at least 100 audited repository rows, matching the auditor's full-checkout credibility floor;
- a successful generation command and successful audit command recorded in the report;
- an audit status of `verified` or `review-required`;
- zero structural blockers;
- zero clearance-registry parse errors;
- non-empty ownership-ledger, pending-clearance, and audit-report evidence;
- an exact SHA-256 match after the generated manifest is copied into the governed path.

The local summary uses the auditor's authoritative field names, including `rowCount`, `releaseCandidateRows`, `clearanceRequiredCount`, `registryErrorCount`, and `releaseReady`.

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

This replaces `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` only after generation and the structural audit have passed. Review the Git diff before committing.

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

Clearance mode additionally requires `status=verified` and `releaseReady=true`. It fails closed when release-relevant entries remain pending, blocked, stale, rejected, or duplicated from protected runtime sources.

## Evidence rule

A generated manifest is not release clearance. A passing non-clearance inventory audit is not release clearance. Release readiness requires the exact committed manifest, exact clearance registry, and a passing `--require-release-clearance` result for the same source commit.

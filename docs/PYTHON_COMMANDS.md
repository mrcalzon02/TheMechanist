# The Mechanist Python Command Reference

This document is the repo-level command map for Python tools. Unless a section says otherwise, run commands from the repository root:

```powershell
cd "C:\GITS\The Mechanist\TheMechanist"
```

Use `python` on Windows PowerShell. If your machine uses the Python launcher instead, replace `python` with `py`.

## Standard asset update loop

Use this loop after slicing, reslicing, replacing, or compliance-editing generated art assets.

```powershell
python .\ROOT_tools\atlas_asset_pipeline\clear_atlas_previews.py
python .\ROOT_tools\atlas_asset_pipeline\manual_slice_exporter.py --config ".\ROOT_tools\atlas_asset_pipeline\configs\<asset>.slice.json" --raw-crops
python .\ROOT_tools\atlas_asset_pipeline\semantic_asset_indexer.py --size 256
python .\ROOT_tools\Compiled_asset_packager.py --dry-run
python .\ROOT_tools\Compiled_asset_packager.py
python .\ROOT_tools\repository_scan_indexer.py
python .\ROOT_tools\repository_manifest_auditor.py
```

Then inspect generated reports, run the game/package smoke test, and commit the changed generated assets/manifests.

## Root-level tools

### `ROOT_tools/Compiled_asset_packager.py`

Stable command for promoting compiled atlas output into package-facing runtime assets. This is the preferred command name for routine asset promotion.

Delegates to:

```text
ROOT_tools/atlas_asset_pipeline/promote_compiled_assets_to_packages.py
```

Primary use:

```powershell
python .\ROOT_tools\Compiled_asset_packager.py --dry-run
python .\ROOT_tools\Compiled_asset_packager.py
```

Automotive-only or focused test promotion:

```powershell
python .\ROOT_tools\Compiled_asset_packager.py --include "*automotive*" --include "*Automotive*"
```

Launcher/installer curated subset promotion:

```powershell
python .\ROOT_tools\Compiled_asset_packager.py `
  --launcher-root ".\PACKAGE_client\assets\launcher\generated" `
  --launcher-include "*icon*" `
  --launcher-include "*system*" `
  --installer-root ".\PACKAGE_client\assets\installer\generated" `
  --installer-include "*icon*" `
  --installer-include "*branding*" `
  --subset-tier standard_64
```

Inputs:

```text
ROOT_tools/atlas_asset_pipeline/compiled_assets
```

Outputs/updates:

```text
PACKAGE_client/assets/graphics/generated
PACKAGE_client/assets/indexes/runtime_asset_manifest.json
ROOT_tools/atlas_asset_pipeline/diagnostics/package_asset_promotion_report.json
ROOT_tools/atlas_asset_pipeline/diagnostics/package_asset_promotion_report.tsv
```

Notes:

- This is the bridge from tool-generated atlas slices to assets the actual client package can see.
- Always run `--dry-run` first after a large slicing pass.
- Run `repository_scan_indexer.py` after promotion so `docs/repository_file_manifest.tsv` reflects the new package state.

### `ROOT_tools/repository_scan_indexer.py`

Scans every repository file outside `.git` and rewrites the repository file manifest TSV.

Run from repo root:

```powershell
python .\ROOT_tools\repository_scan_indexer.py
```

Optional examples:

```powershell
python .\ROOT_tools\repository_scan_indexer.py --dry-run
python .\ROOT_tools\repository_scan_indexer.py --output docs/repository_file_manifest.tsv
python .\ROOT_tools\repository_scan_indexer.py --exclude-dir build --exclude-dir target
```

Primary output:

```text
docs/repository_file_manifest.tsv
```

Purpose:

- Maintains a full-file repository inventory.
- Records paths, file sizes, SHA-256 hashes, rough file family/category, image dimensions where detectable, and useful tags.
- Use this after generated asset changes, package changes, or bulk file moves.

### `ROOT_tools/repository_manifest_auditor.py`

Reads `docs/repository_file_manifest.tsv` and emits a smaller audit report plus an issue ledger.

Run from repo root:

```powershell
python .\ROOT_tools\repository_manifest_auditor.py
```

Outputs:

```text
docs/repository_manifest_audit_report.md
docs/repository_manifest_audit_issues.tsv
```

Purpose:

- Summarizes file-family counts, asset categories, root areas, and largest files.
- Flags suspicious rows such as duplicate paths, scan errors, unknown file families, images missing dimensions, duplicate hashes, and package-hostile PowerShell files.

## Atlas asset pipeline tools

### `ROOT_tools/atlas_asset_pipeline/clear_atlas_previews.py`

Clears stale generated preview/audit artifacts so a new slicing pass cannot be confused with old projected geometry.

Dry run:

```powershell
python .\ROOT_tools\atlas_asset_pipeline\clear_atlas_previews.py --dry-run
```

Real cleanup:

```powershell
python .\ROOT_tools\atlas_asset_pipeline\clear_atlas_previews.py
```

Default cleared folders:

```text
ROOT_tools/atlas_asset_pipeline/atlas_slice_preview
ROOT_tools/atlas_asset_pipeline/diagnostics/manual_slice_audits
```

Purpose:

- Expunges old browser preview pages, old JSON/TSV audit files, old contact sheets, and manual slice audit outputs.
- Use before regenerating preview/slice review artifacts for problematic sheets like automotive parts.

### `ROOT_tools/atlas_asset_pipeline/manual_slice_exporter.py`

Manual/dynamic atlas slicer for source sheets that cannot be safely sliced as uniform grids.

Config-driven run:

```powershell
python .\ROOT_tools\atlas_asset_pipeline\manual_slice_exporter.py `
  --config ".\ROOT_tools\atlas_asset_pipeline\configs\automotive_assorted_components.slice.json" `
  --raw-crops
```

Direct manual line run:

```powershell
python .\ROOT_tools\atlas_asset_pipeline\manual_slice_exporter.py `
  --image ".\PATH\TO\AUTOMOTIVE_PARTS.png" `
  --atlas-name "automotive_assorted_components" `
  --group "items" `
  --x-lines "0,64,132,210,288,356" `
  --y-lines "0,58,141,205,320" `
  --sizes "64,128,256" `
  --resize-mode contain `
  --raw-crops
```

Guide detection attempt:

```powershell
python .\ROOT_tools\atlas_asset_pipeline\manual_slice_exporter.py `
  --image ".\PATH\TO\AUTOMOTIVE_PARTS_WITH_GUIDES.png" `
  --atlas-name "automotive_assorted_components" `
  --group "items" `
  --detect-guides `
  --trim-guides-px 1 `
  --raw-crops
```

Outputs:

```text
ROOT_tools/atlas_asset_pipeline/compiled_assets/<size>px/<group>/...
ROOT_tools/atlas_asset_pipeline/diagnostics/manual_slice_audits/<atlas_name>/
```

Purpose:

- Uses explicit rectangles, explicit x/y slice boundaries, or guide detection.
- Supports variable row heights and variable column widths.
- Writes overlay/contact sheet/raw-crop audit files.
- Refuses to silently fall back to uniform-grid slicing when manual authority is missing.

### `ROOT_tools/atlas_asset_pipeline/semantic_asset_indexer.py`

Builds the semantic content index from already-compiled atlas tile PNGs.

Run after slicing/exporting compiled assets:

```powershell
python .\ROOT_tools\atlas_asset_pipeline\semantic_asset_indexer.py --size 256
```

Common explicit form:

```powershell
python .\ROOT_tools\atlas_asset_pipeline\semantic_asset_indexer.py `
  --compiled-root ".\ROOT_tools\atlas_asset_pipeline\compiled_assets" `
  --size 256 `
  --descriptor-config ".\ROOT_tools\atlas_asset_pipeline\configs\deep_asset_descriptors.json"
```

Outputs:

```text
ROOT_tools/atlas_asset_pipeline/compiled_assets/asset_content_index_256px.json
ROOT_tools/atlas_asset_pipeline/compiled_assets/asset_content_index_256px.tsv
ROOT_tools/atlas_asset_pipeline/compiled_assets/asset_compile_manifest.json
```

Purpose:

- Reads compiled tile images from `compiled_assets/<size>px`.
- Applies descriptor rules from `deep_asset_descriptors.json`.
- Emits content tags, descriptions, content type, visual traits, and index records.

Important limitation:

- This tool does not slice source atlases. It indexes already-sliced compiled PNGs. Fix bad slicing before running this.

### `ROOT_tools/atlas_asset_pipeline/promote_compiled_assets_to_packages.py`

Low-level promotion bridge used by `ROOT_tools/Compiled_asset_packager.py`.

Preferred command:

```powershell
python .\ROOT_tools\Compiled_asset_packager.py
```

Direct command, when debugging the bridge itself:

```powershell
python .\ROOT_tools\atlas_asset_pipeline\promote_compiled_assets_to_packages.py --dry-run
python .\ROOT_tools\atlas_asset_pipeline\promote_compiled_assets_to_packages.py
```

Purpose:

- Copies compiled asset slices into `PACKAGE_client/assets/graphics/generated`.
- Updates `PACKAGE_client/assets/indexes/runtime_asset_manifest.json`.
- Optionally copies curated subsets into launcher/installer asset roots.
- Writes promotion diagnostics.

## Packaging/build related Python tools

### `tools/packaging/generate_release_manifest.py`

Generates release/package manifest metadata for native build outputs.

This tool is normally called from the Windows packaging PowerShell scripts rather than by hand.

Representative direct use:

```powershell
python .\tools\packaging\generate_release_manifest.py `
  --repo-root . `
  --output .\dist\native\windows\release-manifest.json `
  --channel dev `
  --artifact .\dist\native\windows\app-image `
  --artifact .\dist\native\windows\game-app-image `
  --artifact .\dist\native\windows\server-app-image
```

Purpose:

- Describes packaged artifacts for release/download workflows.
- Used by native packaging orchestration.

## Recommended commit sequence after graphical asset work

After a slicing/promotion pass:

```powershell
python .\ROOT_tools\repository_scan_indexer.py
python .\ROOT_tools\repository_manifest_auditor.py

git status --short
```

Typical files to review and commit:

```text
PACKAGE_client/assets/graphics/generated/**
PACKAGE_client/assets/indexes/runtime_asset_manifest.json
ROOT_tools/atlas_asset_pipeline/compiled_assets/**
ROOT_tools/atlas_asset_pipeline/diagnostics/**
docs/repository_file_manifest.tsv
docs/repository_manifest_audit_report.md
docs/repository_manifest_audit_issues.tsv
```

Then:

```powershell
git add PACKAGE_client/assets/graphics/generated `
        PACKAGE_client/assets/indexes/runtime_asset_manifest.json `
        ROOT_tools/atlas_asset_pipeline/compiled_assets `
        ROOT_tools/atlas_asset_pipeline/diagnostics `
        docs/repository_file_manifest.tsv `
        docs/repository_manifest_audit_report.md `
        docs/repository_manifest_audit_issues.tsv

git commit -m "Assets: promote compiled graphical updates into runtime package"
git push
```

## Notes on generated files

- `atlas_slice_preview` and `diagnostics/manual_slice_audits` are review outputs. They are useful during a pass, but stale copies are dangerous because they can make old geometry look current.
- `compiled_assets` is tool-side generated output.
- `PACKAGE_client/assets/graphics/generated` is game/package-facing generated output.
- `runtime_asset_manifest.json` is the client mapping contract. If a sliced asset is not promoted into this layer, the game does not meaningfully know about it.

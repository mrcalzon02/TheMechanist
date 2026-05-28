# Atlas Asset Pipeline

This folder contains the source-atlas review, slicing, and mechanical image-edit tools for the project. It is intentionally separate from package/client assets: the tools read from source art, generate review output, and only modify source images when an image-edit action is explicitly run in source-edit mode.

## Folder Layout

- `atlas_pipeline.py` - single command entry point for named pipeline actions.
- `dynamic_atlas_slicer.py` - atlas geometry detector, preview generator, audit writer, and optional square-slice exporter.
- `configs/` - slicer overrides, scrub templates, and example/manual box configs.
- `image_tools/` - mechanical edit modules for bounded censorship, context stamping, procedural infill, texture infill, and template matching.
- `template_crops/` - source template images used by template matching.
- `atlas_slice_preview/` - generated audit reports, grid previews, contact sheets, review HTML, and optional preview slices.
- `requirements.txt` - Python package requirements for a standalone environment.
- `LICENSE` - MIT license for this tool family.
- `TOOL_CAPABILITY_MANIFEST.json` - machine-readable action list for future automation.

## Standard Commands

Run a full atlas geometry audit and preview pass:

```powershell
python ROOT_tools\atlas_asset_pipeline\atlas_pipeline.py slice --emit-overlay
```

Run only a specific source atlas while tuning detection:

```powershell
python ROOT_tools\atlas_asset_pipeline\atlas_pipeline.py slice --emit-overlay --only "Ping1x5"
```

Emit normalized square preview slices as well as overlays:

```powershell
python ROOT_tools\atlas_asset_pipeline\atlas_pipeline.py slice --emit-overlay --emit-slices
```

Apply exported manual review edge corrections:

```powershell
python ROOT_tools\atlas_asset_pipeline\atlas_pipeline.py slice --emit-overlay --review-tags C:\Users\Admin\Downloads\atlas_slice_review_tags.json
```

Run the broad template scrubber in dry-run mode:

```powershell
python ROOT_tools\atlas_asset_pipeline\atlas_pipeline.py template-scrub --dry-run
```

Run a bounded context-stamp correction against one image:

```powershell
python ROOT_tools\atlas_asset_pipeline\atlas_pipeline.py context-stamp --image "path\to\atlas.png" --config "path\to\stamp_boxes.json"
```

Convert clicked review cells into a bounded image-tool config:

```powershell
python ROOT_tools\atlas_asset_pipeline\atlas_pipeline.py cell-targets --review-tags C:\Users\Admin\Downloads\atlas_slice_review_tags.json
```

Compile selected resolution packages from the current audit:

```powershell
python ROOT_tools\atlas_asset_pipeline\atlas_pipeline.py compile --size 64 --size 256 --source-size 256
```

Build a content tag index from the 256px compiled package:

```powershell
python ROOT_tools\atlas_asset_pipeline\atlas_pipeline.py index-content --size 256
```

## Slicer Behavior

The slicer combines automatic detection with explicit overrides:

- Filename hints such as `8x8`, `6x6`, or `1x5` are honored.
- Large square project atlases default toward `5x5` unless a stronger signal or override says otherwise.
- Balanced boundary refinement searches near expected grid lines while avoiding huge width/height swings.
- Content-strip mode detects centered strip atlases, such as `Ping1x5`, and slices only the occupied strip.
- Row-specific mode supports atlases where different rows have different column counts, such as a `5,5,5,6,6` layout.
- Per-cell content trimming records the actual occupied content bounds inside each source cell.

The audit files record both the outer source slice box and the inner content box for every cell. This is important for dynamic tiles: the atlas cut can be stable while the individual tile content is scaled into the final square output.

## Manual Correction Paths

Manual correction is a supported fallback, not the primary detection method.

The generated `atlas_grid_preview_index.html` lets a reviewer mark a sheet as OK, incorrect, or needing review. It also supports exact manual edge capture:

- The default tab is `Active Edit`, which focuses one atlas and its controls at a time.
- Use `Left`, `Right`, or the active asset selector to cycle through atlases without losing marks.
- Use `Image zoom` to enlarge only the active image/work area; text and controls keep their normal size.
- Capture starts off for every atlas. `Capture X` and `Capture Y` are toggles, and `Capture Off` returns to ordinary cell selection.
- `Refresh Cut Preview` redraws visible cut overlays from the current edge and segment fields.
- Use `Capture X` to click vertical cut positions on the preview.
- Use `Capture Y` to click horizontal cut positions on the preview.
- Hovering while capture is active shows the pending cut line before you click.
- The cut origin is inferred from the closest image edge: vertical cuts start at top or bottom, horizontal cuts start at left or right.
- `Segment depth px`, `Row scope`, and `Column scope` describe partial cuts for irregular sheets instead of forcing a full-image edge.
- Clicking a detected cell while capture is off adds that cell to `Selected cells`.
- `Tool action` marks the intended image repair action for those selected cells.
- Export the tagged JSON.
- Re-run the slicer with `--review-tags`.

For uniform grids, exported `x_edges` and `y_edges` become exact manual slice boundaries. For row-specific sheets, add or adjust the matching entry in `configs/dynamic_atlas_slicer.overrides.json` using `row_cols`.

Example row-specific override:

```json
{
  "match": "Noble Houses Defenses 2.png",
  "row_cols": [5, 5, 5, 6, 6],
  "refine_boundaries": true,
  "reason": "first three rows are 5-wide, bottom two rows are 6-wide"
}
```

Example centered content-region override:

```json
{
  "match": "Ping1x5.png",
  "cols": 5,
  "rows": 1,
  "content_region": [3, 488, 1246, 283],
  "reason": "one-row strip centered in a larger canvas"
}
```

## Image-Edit Modules

These tools are tied into the same pipeline so identified regions can be corrected without writing ad hoc scripts.

- `censor-boxes` applies coordinate-bounded stamp, infill, blur, or pixelation operations from JSON.
- `context-stamp` copies a selected source patch into a target box with color matching and a tight seam.
- `texture-infill` searches nearby source texture patches and fills a target box from local context.
- `procedural-infill` generates local statistical texture inside a target box.
- `template-scrub` scans configured asset roots for known template crops and edits matched regions.
- `cell-targets` converts review-selected cells into a `censor-boxes` compatible config.

Most edit tools intentionally operate on exact boxes supplied by config. That keeps tightly packed atlas neighbors untouched.

## Key Config Fields

`configs/dynamic_atlas_slicer.overrides.json`:

- `match` - substring match against the atlas relative path or filename.
- `cols` / `rows` - uniform grid shape.
- `row_cols` - row-specific column counts. This overrides `cols`.
- `refine_boundaries` - enables balanced edge refinement around expected cuts.
- `x_edges` / `y_edges` - exact manual boundaries for uniform grids.
- `content_region` - `[x, y, width, height]` crop region to slice instead of the full canvas.
- `reason` - human-readable explanation written into the audit.

## Outputs

The slicer writes:

- `atlas_slicing_audit.tsv` - quick spreadsheet-friendly summary.
- `atlas_slicing_audit.json` - full machine-readable cell data.
- `*_grid_preview.png` - visual grid/content overlays per atlas.
- `atlas_grid_preview_contact_sheet.png` - compact overview image.
- `atlas_grid_preview_index.html` - clickable review interface.
- optional per-cell normalized preview slices when `--emit-slices` is used.

## Adaptive Compilation

The `compile` action writes selected size packages from the current audit. It can output any subset of `16`, `32`, `64`, `128`, `256`, `512`, and `1024` without regenerating every size.

The compile tab in `atlas_grid_preview_index.html` exposes:

- source atlas root
- target package root
- output root
- declared source tile size
- selected output sizes
- crispness, brightness, and contrast multipliers
- selected audit source paths

When an output size is larger than the declared source tile size, the tool prints a warning because upscaling can introduce blur and visible artifacts.

The compiler writes an `asset_compile_manifest.json` into the output root so future publish tooling can read the source root, logical target root, output root, selected sizes, and image adjustment values.

The output folder is generated and can be deleted/rebuilt at any time.

## Content Indexing

The `index-content` action scans one compiled resolution package, normally `256px`, and writes:

- `compiled_assets/asset_content_index_256px.json` - full per-image content metadata.
- `compiled_assets/asset_content_index_256px.tsv` - spreadsheet-friendly review table.

Each indexed tile records its compiled path, source folder, source atlas, row/column cell, category, content tags, and simple visual traits such as brightness, visible coverage, dominant color family, and edge density. The action also updates `compiled_assets/asset_compile_manifest.json` with a `content_index` block pointing to the JSON/TSV outputs and summarizing the available tags.

Richer content descriptions are loaded from `configs/deep_asset_descriptors.json`. That file supports atlas-level, row-level, and exact cell-level descriptor rules. Use it for terms that require visual understanding, such as `pistol`, `sword`, `chainsword`, `hammer`, `newspaper`, `armor`, or `train`. Re-run `index-content` after editing descriptor rules.

#!/usr/bin/env python3
"""
Promote atlas pipeline compiled slices into game/package-facing asset roots.

The atlas slicer and semantic indexer operate in ROOT_tools/atlas_asset_pipeline,
but the game reads packaged assets from PACKAGE_client/assets/graphics/generated
and the runtime remap manifest. This command is the bridge between those worlds.

Primary behavior:
  - Copies compiled slice PNGs from compiled_assets/<size>px/<group>/...
  - Rewrites/overwrites PACKAGE_client/assets/graphics/generated tier files
  - Adds/updates entries in PACKAGE_client/assets/indexes/runtime_asset_manifest.json
  - Optionally copies curated subsets into launcher/installer package asset roots
  - Writes promotion audit JSON/TSV files for review

The command is intentionally conservative. Launcher/installer promotion only
happens when include patterns are supplied in config or CLI arguments.
"""

from __future__ import annotations

import argparse
import csv
import fnmatch
import json
import re
import shutil
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any, Iterable

try:
    from PIL import Image
except Exception:  # Pillow is optional unless resize fallback is needed.
    Image = None  # type: ignore


PIPELINE_ROOT = Path(__file__).resolve().parent
REPO_ROOT = PIPELINE_ROOT.parent.parent
DEFAULT_COMPILED_ROOT = PIPELINE_ROOT / "compiled_assets"
DEFAULT_RUNTIME_MANIFEST = REPO_ROOT / "PACKAGE_client/assets/indexes/runtime_asset_manifest.json"
DEFAULT_CLIENT_GENERATED_ROOT = REPO_ROOT / "PACKAGE_client/assets/graphics/generated"
DEFAULT_PROMOTION_REPORT = PIPELINE_ROOT / "diagnostics/package_asset_promotion_report.json"
DEFAULT_PROMOTION_TSV = PIPELINE_ROOT / "diagnostics/package_asset_promotion_report.tsv"
DEFAULT_CONFIG = PIPELINE_ROOT / "configs/package_asset_promotion.json"

TIER_BY_SIZE = {
    256: "high_native",
    128: "intermediate_128",
    64: "standard_64",
    32: "low_32",
}
SIZE_BY_TIER = {value: key for key, value in TIER_BY_SIZE.items()}
TIER_ORDER = ["high_native", "intermediate_128", "standard_64", "low_32"]

COMPILED_CELL_RE = re.compile(r"^(?P<atlas>.+)_r(?P<row>\d+)c(?P<col>\d+)_(?P<size>\d+)px\.png$", re.IGNORECASE)
LEGACY_CELL_RE = re.compile(r"^(?P<atlas>.+)_r(?P<row>\d+)c(?P<col>\d+)\.png$", re.IGNORECASE)


@dataclass
class CompiledAsset:
    source_path: Path
    source_rel: str
    group: str
    atlas: str
    row: int
    col: int
    size: int

    @property
    def cell_key(self) -> str:
        return f"r{self.row:02d}c{self.col:02d}"

    @property
    def asset_id(self) -> str:
        return f"{slug(self.atlas)}__{self.cell_key}"

    @property
    def runtime_filename(self) -> str:
        return f"{self.atlas}_r{self.row}c{self.col}.png"


@dataclass
class PromotionRecord:
    asset_id: str
    atlas: str
    group: str
    row: int
    col: int
    tier: str
    size: int
    source: str
    destination: str
    package_target: str
    action: str
    note: str = ""


def slug(value: str) -> str:
    clean = re.sub(r"[^A-Za-z0-9]+", "_", value.strip().lower()).strip("_")
    return clean or "unknown"


def posix(path: Path) -> str:
    return path.as_posix()


def repo_rel(path: Path) -> str:
    try:
        return posix(path.resolve().relative_to(REPO_ROOT.resolve()))
    except ValueError:
        return str(path)


def load_json(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {}
    return json.loads(path.read_text(encoding="utf-8-sig"))


def write_json(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2), encoding="utf-8")


def parse_patterns(values: Iterable[str] | None) -> list[str]:
    if not values:
        return []
    result: list[str] = []
    for value in values:
        for part in re.split(r"[,;]\s*|\n+", value):
            clean = part.strip()
            if clean:
                result.append(clean)
    return result


def matches_any(text_values: Iterable[str], patterns: list[str]) -> bool:
    if not patterns:
        return False
    values = [value.replace("\\", "/") for value in text_values]
    for pattern in patterns:
        clean_pattern = pattern.replace("\\", "/")
        for value in values:
            if fnmatch.fnmatchcase(value, clean_pattern):
                return True
            if fnmatch.fnmatchcase(value.lower(), clean_pattern.lower()):
                return True
    return False


def parse_compiled_asset(path: Path, compiled_root: Path) -> CompiledAsset | None:
    match = COMPILED_CELL_RE.match(path.name)
    if not match:
        match = LEGACY_CELL_RE.match(path.name)
        if not match:
            return None

    try:
        rel = path.relative_to(compiled_root)
    except ValueError:
        rel = path

    parts = rel.parts
    group = "items"
    size = int(match.groupdict().get("size") or 0)

    # Expected: 256px/items/foo_r01c01_256px.png
    if len(parts) >= 3 and parts[0].lower().endswith("px"):
        try:
            size = int(parts[0][:-2])
        except ValueError:
            pass
        group = parts[1]
    elif len(parts) >= 2:
        group = parts[-2]

    if size <= 0:
        return None

    return CompiledAsset(
        source_path=path,
        source_rel=posix(rel),
        group=group,
        atlas=match.group("atlas"),
        row=int(match.group("row")),
        col=int(match.group("col")),
        size=size,
    )


def discover_compiled_assets(compiled_root: Path) -> list[CompiledAsset]:
    assets: list[CompiledAsset] = []
    for path in sorted(compiled_root.rglob("*.png")):
        asset = parse_compiled_asset(path, compiled_root)
        if asset is not None:
            assets.append(asset)
    return assets


def filter_assets(assets: list[CompiledAsset], include_patterns: list[str], exclude_patterns: list[str]) -> list[CompiledAsset]:
    result: list[CompiledAsset] = []
    for asset in assets:
        values = [
            asset.source_rel,
            asset.group,
            asset.atlas,
            asset.asset_id,
            asset.cell_key,
            f"{asset.atlas}/{asset.cell_key}",
        ]
        if include_patterns and not matches_any(values, include_patterns):
            continue
        if exclude_patterns and matches_any(values, exclude_patterns):
            continue
        result.append(asset)
    return result


def copy_file(source: Path, destination: Path, dry_run: bool) -> str:
    destination.parent.mkdir(parents=True, exist_ok=True)
    if dry_run:
        return "would_copy"
    shutil.copy2(source, destination)
    return "copied"


def resize_or_copy(source: Path, destination: Path, size: int, dry_run: bool) -> str:
    destination.parent.mkdir(parents=True, exist_ok=True)
    if dry_run:
        return "would_resize"
    if Image is None:
        raise RuntimeError("Pillow is required to synthesize missing package tiers by resizing")
    with Image.open(source) as img:
        rgba = img.convert("RGBA")
        resized = rgba.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(destination)
    return "resized"


def tier_roots_for_manifest(manifest: dict[str, Any], client_generated_root: Path) -> dict[str, str]:
    roots = dict(manifest.get("tier_roots", {}))
    if not roots:
        roots = {
            "high_native": "assets/graphics/generated/high_native",
            "intermediate_128": "assets/graphics/generated/intermediate_128",
            "standard_64": "assets/graphics/generated/standard_64",
            "low_32": "assets/graphics/generated/low_32",
        }
    return roots


def package_rel_to_package_client(path: Path) -> str:
    package_root = REPO_ROOT / "PACKAGE_client"
    try:
        return posix(path.resolve().relative_to(package_root.resolve()))
    except ValueError:
        return repo_rel(path)


def destination_for_client(asset: CompiledAsset, client_generated_root: Path, tier: str) -> Path:
    return client_generated_root / tier / asset.atlas / asset.runtime_filename


def asset_entry_for(asset: CompiledAsset, tier_paths: dict[str, str], existing_entry: dict[str, Any] | None = None) -> dict[str, Any]:
    entry = dict(existing_entry or {})
    entry["asset_id"] = asset.asset_id
    entry["sheet_id"] = asset.atlas
    entry["row"] = asset.row
    entry["col"] = asset.col
    entry.setdefault("family", infer_family(asset))
    entry.setdefault("source_phases", [])
    if "atlas_pipeline_promoted" not in entry["source_phases"]:
        entry["source_phases"] = [*entry["source_phases"], "atlas_pipeline_promoted"]
    entry["source_group"] = asset.group
    entry["tiers"] = tier_paths
    entry["promotion_source"] = "ROOT_tools/atlas_asset_pipeline/compiled_assets"
    return entry


def infer_family(asset: CompiledAsset) -> str:
    text = f"{asset.group} {asset.atlas}".lower()
    if any(token in text for token in ["portrait", "profile", "human", "arbites", "noble", "servitor"]):
        return "portrait_or_actor"
    if any(token in text for token in ["tile", "floor", "wall", "road", "bulkhead", "door"]):
        return "tile_or_architecture"
    if any(token in text for token in ["icon", "system", "skill", "knowledge"]):
        return "ui_or_icon"
    return "item_or_prop"


def promote_client_assets(
    assets: list[CompiledAsset],
    client_generated_root: Path,
    runtime_manifest_path: Path,
    dry_run: bool,
    synthesize_missing_tiers: bool,
) -> list[PromotionRecord]:
    records: list[PromotionRecord] = []
    manifest = load_json(runtime_manifest_path)
    if not manifest:
        manifest = {
            "version": "atlas-pipeline-promoted",
            "description": "Runtime remap manifest for generated graphical assets.",
            "tier_roots": {},
            "asset_count": 0,
            "tiers": TIER_ORDER,
            "entries": {},
        }
    entries = manifest.setdefault("entries", {})
    manifest["tiers"] = list(dict.fromkeys([*manifest.get("tiers", []), *TIER_ORDER]))
    tier_roots = tier_roots_for_manifest(manifest, client_generated_root)
    manifest["tier_roots"] = tier_roots

    by_cell: dict[tuple[str, int, int], dict[int, CompiledAsset]] = {}
    for asset in assets:
        by_cell.setdefault((asset.atlas, asset.row, asset.col), {})[asset.size] = asset

    for (_atlas, _row, _col), sized_assets in sorted(by_cell.items()):
        anchor = sized_assets.get(256) or sized_assets.get(128) or sized_assets.get(64) or next(iter(sized_assets.values()))
        tier_paths: dict[str, str] = {}
        for tier in TIER_ORDER:
            target_size = SIZE_BY_TIER[tier]
            source_asset = sized_assets.get(target_size)
            action = ""
            note = ""
            destination = destination_for_client(anchor, client_generated_root, tier)

            if source_asset is not None:
                action = copy_file(source_asset.source_path, destination, dry_run)
                source_path = source_asset.source_path
            elif synthesize_missing_tiers:
                best = sized_assets.get(256) or sized_assets.get(128) or sized_assets.get(64) or anchor
                action = resize_or_copy(best.source_path, destination, target_size, dry_run)
                source_path = best.source_path
                note = f"synthesized_{target_size}px_from_{best.size}px"
            else:
                note = f"missing_source_size_{target_size}px"
                records.append(PromotionRecord(anchor.asset_id, anchor.atlas, anchor.group, anchor.row, anchor.col, tier, target_size, "", repo_rel(destination), "client", "skipped", note))
                continue

            tier_paths[tier] = package_rel_to_package_client(destination)
            records.append(PromotionRecord(anchor.asset_id, anchor.atlas, anchor.group, anchor.row, anchor.col, tier, target_size, repo_rel(source_path), repo_rel(destination), "client", action, note))

        if tier_paths:
            existing = entries.get(anchor.asset_id)
            entries[anchor.asset_id] = asset_entry_for(anchor, tier_paths, existing)

    manifest["asset_count"] = len(entries)
    if not dry_run:
        write_json(runtime_manifest_path, manifest)
    return records


def promote_curated_target(
    target_name: str,
    assets: list[CompiledAsset],
    root: Path | None,
    include_patterns: list[str],
    tier: str,
    dry_run: bool,
) -> list[PromotionRecord]:
    records: list[PromotionRecord] = []
    if root is None or not include_patterns:
        return records
    target_size = SIZE_BY_TIER.get(tier, 256)
    selected = [asset for asset in assets if asset.size == target_size]
    selected = filter_assets(selected, include_patterns, [])
    for asset in selected:
        destination = root / asset.atlas / asset.runtime_filename
        action = copy_file(asset.source_path, destination, dry_run)
        records.append(PromotionRecord(asset.asset_id, asset.atlas, asset.group, asset.row, asset.col, tier, asset.size, repo_rel(asset.source_path), repo_rel(destination), target_name, action, "curated_subset"))
    return records


def write_tsv(records: list[PromotionRecord], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = list(asdict(records[0]).keys()) if records else [
        "asset_id", "atlas", "group", "row", "col", "tier", "size", "source", "destination", "package_target", "action", "note"
    ]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for record in records:
            writer.writerow(asdict(record))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Promote compiled atlas assets into package-facing runtime roots.")
    parser.add_argument("--config", default=str(DEFAULT_CONFIG), help="Optional JSON promotion config.")
    parser.add_argument("--compiled-root", default=str(DEFAULT_COMPILED_ROOT), help="Compiled atlas asset root.")
    parser.add_argument("--client-generated-root", default=str(DEFAULT_CLIENT_GENERATED_ROOT), help="PACKAGE_client generated graphics root.")
    parser.add_argument("--runtime-manifest", default=str(DEFAULT_RUNTIME_MANIFEST), help="Runtime asset manifest to update.")
    parser.add_argument("--include", action="append", default=[], help="Client include pattern. Defaults to all compiled assets if no config include list is present.")
    parser.add_argument("--exclude", action="append", default=[], help="Client exclude pattern.")
    parser.add_argument("--launcher-root", default=None, help="Optional launcher asset subset destination root.")
    parser.add_argument("--launcher-include", action="append", default=[], help="Pattern for launcher subset assets.")
    parser.add_argument("--installer-root", default=None, help="Optional installer asset subset destination root.")
    parser.add_argument("--installer-include", action="append", default=[], help="Pattern for installer subset assets.")
    parser.add_argument("--subset-tier", default="standard_64", choices=TIER_ORDER, help="Tier copied into launcher/installer curated roots.")
    parser.add_argument("--no-synthesize-missing-tiers", action="store_true", help="Do not synthesize missing package tiers by resizing available tiers.")
    parser.add_argument("--dry-run", action="store_true", help="Report what would be copied without writing files or manifest updates.")
    parser.add_argument("--report-json", default=str(DEFAULT_PROMOTION_REPORT), help="Promotion report JSON output.")
    parser.add_argument("--report-tsv", default=str(DEFAULT_PROMOTION_TSV), help="Promotion report TSV output.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    config_path = Path(args.config).resolve()
    config = load_json(config_path)

    compiled_root = Path(config.get("compiled_root", args.compiled_root)).resolve()
    client_generated_root = Path(config.get("client_generated_root", args.client_generated_root)).resolve()
    runtime_manifest = Path(config.get("runtime_manifest", args.runtime_manifest)).resolve()
    report_json = Path(args.report_json).resolve()
    report_tsv = Path(args.report_tsv).resolve()

    if not compiled_root.exists():
        raise FileNotFoundError(f"Compiled root not found: {compiled_root}")

    include_patterns = parse_patterns(args.include) or parse_patterns(config.get("client_include", []))
    exclude_patterns = parse_patterns(args.exclude) or parse_patterns(config.get("client_exclude", []))
    launcher_include = parse_patterns(args.launcher_include) or parse_patterns(config.get("launcher_include", []))
    installer_include = parse_patterns(args.installer_include) or parse_patterns(config.get("installer_include", []))

    launcher_root_text = args.launcher_root or config.get("launcher_root")
    installer_root_text = args.installer_root or config.get("installer_root")
    launcher_root = Path(launcher_root_text).resolve() if launcher_root_text else None
    installer_root = Path(installer_root_text).resolve() if installer_root_text else None

    assets = discover_compiled_assets(compiled_root)
    client_assets = filter_assets(assets, include_patterns, exclude_patterns)

    records: list[PromotionRecord] = []
    records.extend(
        promote_client_assets(
            client_assets,
            client_generated_root,
            runtime_manifest,
            dry_run=args.dry_run,
            synthesize_missing_tiers=not args.no_synthesize_missing_tiers,
        )
    )
    records.extend(promote_curated_target("launcher", assets, launcher_root, launcher_include, args.subset_tier, args.dry_run))
    records.extend(promote_curated_target("installer", assets, installer_root, installer_include, args.subset_tier, args.dry_run))

    report = {
        "schema": "mechanist.package_asset_promotion.v1",
        "dry_run": args.dry_run,
        "compiled_root": str(compiled_root),
        "client_generated_root": str(client_generated_root),
        "runtime_manifest": str(runtime_manifest),
        "discovered_compiled_assets": len(assets),
        "selected_client_assets": len(client_assets),
        "promotion_records": len(records),
        "client_include": include_patterns or ["*"],
        "client_exclude": exclude_patterns,
        "launcher_root": str(launcher_root) if launcher_root else "",
        "launcher_include": launcher_include,
        "installer_root": str(installer_root) if installer_root else "",
        "installer_include": installer_include,
        "records": [asdict(record) for record in records],
    }
    write_json(report_json, report)
    write_tsv(records, report_tsv)

    copied = sum(1 for record in records if record.action in {"copied", "resized"})
    would_copy = sum(1 for record in records if record.action.startswith("would_"))
    skipped = sum(1 for record in records if record.action == "skipped")
    print(f"Discovered compiled assets: {len(assets)}")
    print(f"Selected client assets:      {len(client_assets)}")
    print(f"Promotion records:          {len(records)}")
    print(f"Copied/resized:             {copied}")
    print(f"Would copy/resize:          {would_copy}")
    print(f"Skipped:                    {skipped}")
    print(f"Runtime manifest:           {runtime_manifest}")
    print(f"Report JSON:                {report_json}")
    print(f"Report TSV:                 {report_tsv}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

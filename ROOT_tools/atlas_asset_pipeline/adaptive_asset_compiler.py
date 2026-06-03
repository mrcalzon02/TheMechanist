#!/usr/bin/env python3
"""
Compile sliced atlas cells into selected resolution packages.

The compiler consumes the slicer's machine-readable audit JSON, reopens the
source atlases, crops each detected cell content box, and writes square tile
assets at only the requested sizes.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageEnhance


PIPELINE_ROOT = Path(__file__).resolve().parent
DEFAULT_AUDIT = PIPELINE_ROOT / "atlas_slice_preview/atlas_slicing_audit.json"
DEFAULT_SOURCE_ROOT = Path("ROOT_SRC_assets/Mechanist_art_SRC_do_not_MODIFY/Mechanist art/TILES")
DEFAULT_OUTPUT_ROOT = PIPELINE_ROOT / "compiled_assets"
STANDARD_SIZES = (16, 32, 64, 128, 256, 512, 1024)


@dataclass(frozen=True)
class CompileSettings:
    sizes: list[int]
    source_root: Path
    target_root: Path
    output_root: Path
    source_size: int
    crispness: float
    brightness: float
    contrast: float
    selected_sources: set[str]
    selected_cells: set[str]
    use_content_box: bool


def parse_sizes(values: list[str | int] | None) -> list[int]:
    if not values:
        return [256]
    sizes: set[int] = set()
    for value in values:
        if isinstance(value, int):
            if value <= 0:
                raise ValueError(f"Size must be positive: {value}")
            sizes.add(value)
            continue
        for item in value.split(","):
            item = item.strip()
            if item:
                size = int(item)
                if size <= 0:
                    raise ValueError(f"Size must be positive: {size}")
                sizes.add(size)
    return sorted(sizes)


def load_config(path: Path | None) -> dict:
    if path is None:
        return {}
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def selected_from_review(path: Path | None) -> tuple[set[str], set[str]]:
    if path is None or not path.exists():
        return set(), set()
    with path.open("r", encoding="utf-8") as handle:
        records = json.load(handle)
    sources: set[str] = set()
    cells: set[str] = set()
    for record in records:
        source = str(record.get("source", ""))
        if not source:
            continue
        sources.add(source)
        for cell in record.get("selected_cells", []):
            cells.add(f"{source}#{cell}")
    return sources, cells


def resolve_settings(args: argparse.Namespace) -> CompileSettings:
    config = load_config(Path(args.config).resolve() if args.config else None)
    review_sources, review_cells = selected_from_review(Path(args.review_tags).resolve() if args.review_tags else None)
    selected_sources = set(str(item) for item in config.get("selected_sources", []))
    selected_sources.update(review_sources)
    selected_sources.update(str(item) for item in (args.only or []))
    selected_cells = set(str(item) for item in config.get("selected_cells", []))
    selected_cells.update(review_cells)
    return CompileSettings(
        sizes=parse_sizes(args.size or config.get("sizes")),
        source_root=Path(config.get("source_root", args.source_root)).resolve(),
        target_root=Path(config.get("target_root", args.target_root)).resolve(),
        output_root=Path(config.get("output_root", args.output_root)).resolve(),
        source_size=int(config.get("source_size", args.source_size)),
        crispness=float(config.get("crispness", args.crispness)),
        brightness=float(config.get("brightness", args.brightness)),
        contrast=float(config.get("contrast", args.contrast)),
        selected_sources=selected_sources,
        selected_cells=selected_cells,
        use_content_box=bool(config.get("use_content_box", args.use_content_box)),
    )


def fit_square(image: Image.Image, box: list[int], size: int) -> Image.Image:
    x, y, w, h = [int(value) for value in box]
    tile = image.crop((x, y, x + w, y + h)).convert("RGBA")
    if tile.width <= 0 or tile.height <= 0:
        return Image.new("RGBA", (size, size), (0, 0, 0, 0))
    scale = min(size / tile.width, size / tile.height)
    resized = tile.resize((max(1, round(tile.width * scale)), max(1, round(tile.height * scale))), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((size - resized.width) // 2, (size - resized.height) // 2))
    return canvas


def enhance(tile: Image.Image, settings: CompileSettings) -> Image.Image:
    result = tile
    if settings.brightness != 1.0:
        result = ImageEnhance.Brightness(result).enhance(settings.brightness)
    if settings.contrast != 1.0:
        result = ImageEnhance.Contrast(result).enhance(settings.contrast)
    if settings.crispness != 1.0:
        result = ImageEnhance.Sharpness(result).enhance(settings.crispness)
    return result


def safe_stem(source: str) -> str:
    return Path(source).stem.replace(" ", "_").replace(",", "_")


def compile_assets(audit_path: Path, settings: CompileSettings, dry_run: bool) -> int:
    with audit_path.open("r", encoding="utf-8") as handle:
        audits = json.load(handle)
    written = 0
    for audit in audits:
        source = str(audit["source"])
        if settings.selected_sources and source not in settings.selected_sources:
            continue
        image_path = settings.source_root / source
        if not image_path.exists():
            print(f"WARNING source missing: {image_path}")
            continue
        with Image.open(image_path) as opened:
            image = opened.convert("RGBA")
        rel_parent = Path(source).parent
        for cell in audit.get("cells", []):
            cell_id = f"r{int(cell['row']):02d}c{int(cell['col']):02d}"
            if settings.selected_cells and f"{source}#{cell_id}" not in settings.selected_cells:
                continue
            box = cell["content_box"] if settings.use_content_box else cell["source_box"]
            for size in settings.sizes:
                if size > settings.source_size:
                    print(f"WARNING {source} {cell_id}: output {size}px exceeds declared source size {settings.source_size}px and may blur.")
                output_dir = settings.output_root / f"{size}px" / rel_parent
                output_name = f"{safe_stem(source)}_{cell_id}_{size}px.png"
                output_path = output_dir / output_name
                if dry_run:
                    print(f"DRY {source} {cell_id} -> {output_path}")
                    continue
                output_dir.mkdir(parents=True, exist_ok=True)
                tile = enhance(fit_square(image, box, size), settings)
                tile.save(output_path)
                written += 1
    if not dry_run:
        settings.output_root.mkdir(parents=True, exist_ok=True)
        manifest = {
            "source_root": str(settings.source_root),
            "target_root": str(settings.target_root),
            "output_root": str(settings.output_root),
            "source_size": settings.source_size,
            "sizes": settings.sizes,
            "crispness": settings.crispness,
            "brightness": settings.brightness,
            "contrast": settings.contrast,
            "use_content_box": settings.use_content_box,
            "written_files": written,
        }
        (settings.output_root / "asset_compile_manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    return written


def main() -> int:
    parser = argparse.ArgumentParser(description="Compile selected atlas slices into resolution packages.")
    parser.add_argument("--audit", default=str(DEFAULT_AUDIT), help="atlas_slicing_audit.json path.")
    parser.add_argument("--config", default=None, help="Optional JSON compile config.")
    parser.add_argument("--source-root", default=str(DEFAULT_SOURCE_ROOT), help="Source atlas root.")
    parser.add_argument("--target-root", default=str(DEFAULT_OUTPUT_ROOT), help="Logical package target root.")
    parser.add_argument("--output-root", default=str(DEFAULT_OUTPUT_ROOT), help="Output package root.")
    parser.add_argument("--size", action="append", help="Output size. Can be repeated or comma-separated.")
    parser.add_argument("--source-size", type=int, default=256, help="Nominal source tile size used for upscaling warnings.")
    parser.add_argument("--crispness", type=float, default=1.0, help="Pillow sharpness multiplier.")
    parser.add_argument("--brightness", type=float, default=1.0, help="Brightness multiplier.")
    parser.add_argument("--contrast", type=float, default=1.0, help="Contrast multiplier.")
    parser.add_argument("--only", action="append", help="Compile only this audit source path. Can be repeated.")
    parser.add_argument("--review-tags", default=None, help="Optional review export; selected cells limit compilation.")
    parser.add_argument("--use-content-box", action="store_true", default=True, help="Use trimmed content boxes.")
    parser.add_argument("--use-source-box", dest="use_content_box", action="store_false", help="Use full detected cell boxes.")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    settings = resolve_settings(args)
    written = compile_assets(Path(args.audit).resolve(), settings, args.dry_run)
    print(f"Compiled {written} tile file(s) into {settings.output_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

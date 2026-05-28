#!/usr/bin/env python3
"""
Manual/dynamic atlas slice exporter for The Mechanist.

This tool exists for atlases that must NOT be sliced as a uniform grid. It is
especially intended for intensive review of one problematic source atlas, such
as automotive parts sheets with variable row heights and hand-placed guide
lines.

It supports three authority modes:

1. manual rects: explicit x/y/w/h rectangles per cell.
2. manual lines: x/y slice-boundary lines, allowing variable row heights.
3. guide detection: detect cyan/ice-colored guide lines and derive boundaries.

The exporter writes normalized square PNGs into the existing compiled_assets
layout so semantic_asset_indexer.py can index them using the existing filename
pattern:

    compiled_assets/256px/<group>/<atlas>_r01c01_256px.png

It also writes an audit overlay, contact sheet, JSON, and TSV so the exact crop
geometry can be reviewed before the slice set is accepted.
"""

from __future__ import annotations

import argparse
import csv
import json
import math
import re
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Any, Iterable

from PIL import Image, ImageDraw, ImageStat


PIPELINE_ROOT = Path(__file__).resolve().parent
DEFAULT_OUTPUT_ROOT = PIPELINE_ROOT / "compiled_assets"
DEFAULT_AUDIT_ROOT = PIPELINE_ROOT / "diagnostics/manual_slice_audits"


@dataclass(frozen=True)
class SliceRect:
    cell: str
    row: int
    col: int
    x: int
    y: int
    width: int
    height: int
    source: str
    label: str = ""


@dataclass
class SliceAuditRecord:
    cell: str
    row: int
    col: int
    x: int
    y: int
    width: int
    height: int
    source: str
    label: str
    visible_ratio: float
    aspect_ratio: float
    warnings: list[str]
    output_files: list[str]


def slug(value: str) -> str:
    clean = re.sub(r"[^A-Za-z0-9]+", "_", value.strip().lower()).strip("_")
    return clean or "unknown"


def cell_key(row: int, col: int) -> str:
    return f"r{row:02d}c{col:02d}"


def parse_int_list(value: str | None) -> list[int]:
    if not value:
        return []
    result: list[int] = []
    for part in re.split(r"[,;\s]+", value.strip()):
        if not part:
            continue
        result.append(int(part))
    return result


def dedupe_sorted_ints(values: Iterable[int], lower: int, upper: int) -> list[int]:
    cleaned = sorted({max(lower, min(upper, int(v))) for v in values})
    return cleaned


def load_json(path: Path | None) -> dict[str, Any]:
    if path is None:
        return {}
    if not path.exists():
        raise FileNotFoundError(f"Slice config not found: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def resolve_path(base: Path, maybe_path: str | None) -> Path | None:
    if not maybe_path:
        return None
    path = Path(maybe_path)
    if path.is_absolute():
        return path
    return (base / path).resolve()


def is_guide_pixel(rgb: tuple[int, int, int], colors: list[list[int]], tolerance: int) -> bool:
    r, g, b = rgb
    if colors:
        for color in colors:
            if len(color) < 3:
                continue
            cr, cg, cb = int(color[0]), int(color[1]), int(color[2])
            distance = math.sqrt((r - cr) ** 2 + (g - cg) ** 2 + (b - cb) ** 2)
            if distance <= tolerance:
                return True
        return False

    # Default "ice line" detector: cyan/blue-green guide strokes.
    return b >= 135 and g >= 115 and r <= 130 and (max(r, g, b) - min(r, g, b)) >= 45


def guide_scores(image: Image.Image, axis: str, colors: list[list[int]], tolerance: int) -> list[float]:
    rgb = image.convert("RGB")
    width, height = rgb.size
    pixels = rgb.load()
    scores: list[float] = []

    if axis == "x":
        for x in range(width):
            hits = 0
            for y in range(height):
                if is_guide_pixel(pixels[x, y], colors, tolerance):
                    hits += 1
            scores.append(hits / max(1, height))
        return scores

    if axis == "y":
        for y in range(height):
            hits = 0
            for x in range(width):
                if is_guide_pixel(pixels[x, y], colors, tolerance):
                    hits += 1
            scores.append(hits / max(1, width))
        return scores

    raise ValueError(f"Unknown axis: {axis}")


def collapse_line_positions(scores: list[float], min_ratio: float, min_gap: int) -> list[int]:
    groups: list[list[int]] = []
    current: list[int] = []

    for index, score in enumerate(scores):
        if score >= min_ratio:
            current.append(index)
        elif current:
            groups.append(current)
            current = []
    if current:
        groups.append(current)

    centers = [int(round(sum(group) / len(group))) for group in groups]
    if not centers:
        return []

    merged: list[list[int]] = [[centers[0]]]
    for center in centers[1:]:
        if center - merged[-1][-1] <= min_gap:
            merged[-1].append(center)
        else:
            merged.append([center])

    return [int(round(sum(group) / len(group))) for group in merged]


def detect_guide_lines(image: Image.Image, config: dict[str, Any]) -> tuple[list[int], list[int], dict[str, Any]]:
    width, height = image.size
    colors = config.get("colors", []) or config.get("guide_colors", []) or []
    tolerance = int(config.get("tolerance", 36))
    x_min_ratio = float(config.get("x_min_ratio", config.get("min_ratio", 0.45)))
    y_min_ratio = float(config.get("y_min_ratio", config.get("min_ratio", 0.45)))
    min_gap = int(config.get("min_gap", 2))

    x_scores = guide_scores(image, "x", colors, tolerance)
    y_scores = guide_scores(image, "y", colors, tolerance)
    x_lines = collapse_line_positions(x_scores, x_min_ratio, min_gap)
    y_lines = collapse_line_positions(y_scores, y_min_ratio, min_gap)

    x_boundaries = dedupe_sorted_ints([0, *x_lines, width], 0, width)
    y_boundaries = dedupe_sorted_ints([0, *y_lines, height], 0, height)

    diagnostics = {
        "mode": "detected_guides",
        "colors": colors,
        "tolerance": tolerance,
        "x_min_ratio": x_min_ratio,
        "y_min_ratio": y_min_ratio,
        "min_gap": min_gap,
        "detected_x_lines": x_lines,
        "detected_y_lines": y_lines,
        "x_boundaries": x_boundaries,
        "y_boundaries": y_boundaries,
    }
    return x_boundaries, y_boundaries, diagnostics


def validate_boundaries(lines: list[int], maximum: int, name: str) -> list[int]:
    boundaries = dedupe_sorted_ints(lines, 0, maximum)
    if not boundaries or boundaries[0] != 0:
        boundaries.insert(0, 0)
    if boundaries[-1] != maximum:
        boundaries.append(maximum)
    if len(boundaries) < 2:
        raise ValueError(f"{name} boundaries must contain at least two values")
    for left, right in zip(boundaries, boundaries[1:]):
        if right <= left:
            raise ValueError(f"{name} boundaries must be strictly increasing: {boundaries}")
    return boundaries


def rects_from_lines(
    x_lines: list[int],
    y_lines: list[int],
    image_size: tuple[int, int],
    trim_guides_px: int,
    labels: dict[str, Any],
    source: str,
) -> list[SliceRect]:
    width, height = image_size
    xs = validate_boundaries(x_lines, width, "x")
    ys = validate_boundaries(y_lines, height, "y")
    rects: list[SliceRect] = []

    for row_index in range(len(ys) - 1):
        for col_index in range(len(xs) - 1):
            row = row_index + 1
            col = col_index + 1
            key = cell_key(row, col)
            x1 = xs[col_index]
            x2 = xs[col_index + 1]
            y1 = ys[row_index]
            y2 = ys[row_index + 1]

            if trim_guides_px > 0:
                if col_index > 0:
                    x1 += trim_guides_px
                if col_index < len(xs) - 2:
                    x2 -= trim_guides_px
                if row_index > 0:
                    y1 += trim_guides_px
                if row_index < len(ys) - 2:
                    y2 -= trim_guides_px

            x1 = max(0, min(width, x1))
            x2 = max(0, min(width, x2))
            y1 = max(0, min(height, y1))
            y2 = max(0, min(height, y2))

            label = label_for_cell(labels, key)
            rects.append(SliceRect(key, row, col, x1, y1, max(0, x2 - x1), max(0, y2 - y1), source, label))

    return rects


def label_for_cell(labels: dict[str, Any], key: str) -> str:
    label = labels.get(key, "")
    if isinstance(label, str):
        return label
    if isinstance(label, dict):
        return str(label.get("canonical_name") or label.get("display_name") or label.get("name") or "")
    return ""


def rects_from_config(config: dict[str, Any], labels: dict[str, Any]) -> list[SliceRect]:
    rects: list[SliceRect] = []
    for index, item in enumerate(config.get("rects", []), start=1):
        row = int(item.get("row", 1))
        col = int(item.get("col", index))
        key = str(item.get("cell") or cell_key(row, col))
        label = str(item.get("label") or item.get("canonical_name") or label_for_cell(labels, key))
        rects.append(
            SliceRect(
                cell=key,
                row=row,
                col=col,
                x=int(item["x"]),
                y=int(item["y"]),
                width=int(item.get("width", item.get("w"))),
                height=int(item.get("height", item.get("h"))),
                source="manual_rect",
                label=label,
            )
        )
    return rects


def visible_ratio(image: Image.Image) -> float:
    rgba = image.convert("RGBA")
    alpha = rgba.getchannel("A")
    stat = ImageStat.Stat(alpha)
    return round(stat.mean[0] / 255.0, 5)


def normalize_crop(crop: Image.Image, size: int, mode: str) -> Image.Image:
    rgba = crop.convert("RGBA")
    if mode == "stretch":
        return rgba.resize((size, size), Image.Resampling.LANCZOS)

    if mode != "contain":
        raise ValueError(f"Unknown resize mode: {mode}")

    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    if rgba.width == 0 or rgba.height == 0:
        return canvas
    scale = min(size / rgba.width, size / rgba.height)
    new_width = max(1, int(round(rgba.width * scale)))
    new_height = max(1, int(round(rgba.height * scale)))
    resized = rgba.resize((new_width, new_height), Image.Resampling.LANCZOS)
    x = (size - new_width) // 2
    y = (size - new_height) // 2
    canvas.alpha_composite(resized, (x, y))
    return canvas


def clip_rect(rect: SliceRect, width: int, height: int) -> tuple[int, int, int, int, list[str]]:
    warnings: list[str] = []
    x1 = max(0, min(width, rect.x))
    y1 = max(0, min(height, rect.y))
    x2 = max(0, min(width, rect.x + rect.width))
    y2 = max(0, min(height, rect.y + rect.height))
    if x1 != rect.x or y1 != rect.y or x2 != rect.x + rect.width or y2 != rect.y + rect.height:
        warnings.append("clipped_to_image_bounds")
    if x2 <= x1 or y2 <= y1:
        warnings.append("empty_or_invalid_rect")
    return x1, y1, x2, y2, warnings


def draw_overlay(source: Image.Image, rects: list[SliceRect], output_path: Path) -> None:
    overlay = source.convert("RGBA")
    draw = ImageDraw.Draw(overlay)
    for rect in rects:
        x1, y1, x2, y2, _ = clip_rect(rect, *source.size)
        color = (0, 255, 255, 255)
        draw.rectangle((x1, y1, x2, y2), outline=color, width=2)
        label = rect.cell if not rect.label else f"{rect.cell} {rect.label}"
        draw.rectangle((x1, max(0, y1 - 14), x1 + min(220, 7 * len(label) + 8), y1), fill=(0, 0, 0, 190))
        draw.text((x1 + 3, max(0, y1 - 13)), label, fill=(255, 255, 255, 255))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    overlay.save(output_path)


def draw_contact_sheet(exports: list[tuple[SliceRect, Image.Image]], output_path: Path, tile_size: int = 128) -> None:
    if not exports:
        return
    cols = min(8, max(1, math.ceil(math.sqrt(len(exports)))))
    rows = math.ceil(len(exports) / cols)
    label_height = 24
    sheet = Image.new("RGBA", (cols * tile_size, rows * (tile_size + label_height)), (24, 24, 24, 255))
    draw = ImageDraw.Draw(sheet)
    for index, (rect, image) in enumerate(exports):
        col = index % cols
        row = index // cols
        x = col * tile_size
        y = row * (tile_size + label_height)
        preview = normalize_crop(image, tile_size, "contain")
        sheet.alpha_composite(preview, (x, y))
        label = rect.cell if not rect.label else f"{rect.cell} {rect.label}"
        draw.text((x + 3, y + tile_size + 4), label[:28], fill=(255, 255, 255, 255))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output_path)


def write_audit_tsv(records: list[SliceAuditRecord], path: Path) -> None:
    fieldnames = [
        "cell",
        "row",
        "col",
        "x",
        "y",
        "width",
        "height",
        "source",
        "label",
        "visible_ratio",
        "aspect_ratio",
        "warnings",
        "output_files",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for record in records:
            row = asdict(record)
            row["warnings"] = ",".join(record.warnings)
            row["output_files"] = ",".join(record.output_files)
            writer.writerow(row)


def export_slices(
    image_path: Path,
    image: Image.Image,
    rects: list[SliceRect],
    atlas_name: str,
    group: str,
    output_root: Path,
    audit_dir: Path,
    sizes: list[int],
    resize_mode: str,
    focus_cell: str | None,
    raw_crops: bool,
    omit_empty_threshold: float,
) -> list[SliceAuditRecord]:
    width, height = image.size
    atlas_slug = slug(atlas_name)
    group_slug = slug(group)
    records: list[SliceAuditRecord] = []
    contact_exports: list[tuple[SliceRect, Image.Image]] = []

    for rect in rects:
        if focus_cell and rect.cell.lower() != focus_cell.lower():
            continue
        x1, y1, x2, y2, warnings = clip_rect(rect, width, height)
        crop = image.crop((x1, y1, x2, y2)).convert("RGBA")
        ratio = visible_ratio(crop) if crop.width > 0 and crop.height > 0 else 0.0
        if ratio <= omit_empty_threshold:
            warnings.append("omitted_empty_or_near_empty")
        aspect = round((crop.width / crop.height), 5) if crop.height else 0.0
        output_files: list[str] = []

        if ratio > omit_empty_threshold and crop.width > 0 and crop.height > 0:
            for size in sizes:
                out_dir = output_root / f"{size}px" / group_slug
                out_dir.mkdir(parents=True, exist_ok=True)
                out_path = out_dir / f"{atlas_slug}_{rect.cell}_{size}px.png"
                normalized = normalize_crop(crop, size, resize_mode)
                normalized.save(out_path)
                output_files.append(out_path.relative_to(output_root).as_posix())

            if raw_crops:
                raw_dir = audit_dir / "raw_crops"
                raw_dir.mkdir(parents=True, exist_ok=True)
                raw_path = raw_dir / f"{atlas_slug}_{rect.cell}_raw.png"
                crop.save(raw_path)

            contact_exports.append((rect, crop))

        records.append(
            SliceAuditRecord(
                cell=rect.cell,
                row=rect.row,
                col=rect.col,
                x=x1,
                y=y1,
                width=max(0, x2 - x1),
                height=max(0, y2 - y1),
                source=rect.source,
                label=rect.label,
                visible_ratio=ratio,
                aspect_ratio=aspect,
                warnings=warnings,
                output_files=output_files,
            )
        )

    draw_contact_sheet(contact_exports, audit_dir / f"{atlas_slug}_contact_sheet.png")
    return records


def build_rects_from_inputs(image: Image.Image, config: dict[str, Any], args: argparse.Namespace) -> tuple[list[SliceRect], dict[str, Any]]:
    labels = config.get("labels", {}) if isinstance(config.get("labels", {}), dict) else {}
    diagnostics: dict[str, Any] = {}

    if config.get("rects"):
        return rects_from_config(config, labels), {"mode": "manual_rects", "rect_count": len(config.get("rects", []))}

    cli_x = parse_int_list(args.x_lines)
    cli_y = parse_int_list(args.y_lines)
    config_x = [int(v) for v in config.get("x_lines", [])]
    config_y = [int(v) for v in config.get("y_lines", [])]
    x_lines = cli_x or config_x
    y_lines = cli_y or config_y

    if x_lines and y_lines:
        return (
            rects_from_lines(x_lines, y_lines, image.size, args.trim_guides_px, labels, "manual_lines"),
            {"mode": "manual_lines", "x_boundaries": validate_boundaries(x_lines, image.width, "x"), "y_boundaries": validate_boundaries(y_lines, image.height, "y")},
        )

    guide_config = config.get("guide_detection", {}) if isinstance(config.get("guide_detection", {}), dict) else {}
    if args.detect_guides or guide_config.get("enabled"):
        detected_x, detected_y, diagnostics = detect_guide_lines(image, guide_config)
        return (
            rects_from_lines(detected_x, detected_y, image.size, args.trim_guides_px, labels, "detected_guide_lines"),
            diagnostics,
        )

    raise ValueError(
        "No slicing authority found. Provide rects, x_lines/y_lines, or enable guide_detection. "
        "This exporter intentionally refuses to fall back to uniform slicing for manual-audit atlases."
    )


def parse_sizes(value: str) -> list[int]:
    sizes = [int(v) for v in re.split(r"[,;\s]+", value.strip()) if v]
    if not sizes:
        raise ValueError("At least one output size is required")
    return sorted(set(sizes))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Export one atlas using manual/dynamic slicing with audit artifacts.")
    parser.add_argument("--image", required=False, help="Source atlas image. Can also be supplied in config as source_image.")
    parser.add_argument("--config", help="JSON slice override/audit config.")
    parser.add_argument("--atlas-name", help="Atlas output name. Defaults to config atlas_name or source image stem.")
    parser.add_argument("--group", default=None, help="Compiled output group/category. Defaults to config group or items.")
    parser.add_argument("--output-root", default=str(DEFAULT_OUTPUT_ROOT), help="Compiled assets root.")
    parser.add_argument("--audit-root", default=str(DEFAULT_AUDIT_ROOT), help="Audit output root.")
    parser.add_argument("--sizes", default="64,128,256", help="Comma/space separated output sizes.")
    parser.add_argument("--x-lines", help="Manual x boundary lines, e.g. '0,64,132,220'.")
    parser.add_argument("--y-lines", help="Manual y boundary lines, supports non-uniform row heights.")
    parser.add_argument("--detect-guides", action="store_true", help="Detect cyan/ice guide lines and derive dynamic boundaries.")
    parser.add_argument("--trim-guides-px", type=int, default=1, help="Pixels trimmed inward from internal guide boundaries.")
    parser.add_argument("--resize-mode", choices=["contain", "stretch"], default="contain", help="How variable-sized crops become square output cells.")
    parser.add_argument("--focus-cell", help="Only export/audit one cell such as r03c02.")
    parser.add_argument("--raw-crops", action="store_true", help="Also write exact raw crops into the audit folder.")
    parser.add_argument("--omit-empty-threshold", type=float, default=0.01, help="Visible alpha ratio at/below which a cell is omitted from exports.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    config_path = Path(args.config).resolve() if args.config else None
    config = load_json(config_path)
    config_base = config_path.parent if config_path else Path.cwd()

    image_path = resolve_path(config_base, args.image or config.get("source_image"))
    if image_path is None:
        raise ValueError("Source image is required via --image or config source_image")
    if not image_path.exists():
        raise FileNotFoundError(f"Source image not found: {image_path}")

    atlas_name = args.atlas_name or config.get("atlas_name") or image_path.stem
    group = args.group or config.get("group") or "items"
    atlas_slug = slug(str(atlas_name))
    sizes = parse_sizes(args.sizes)
    output_root = Path(args.output_root).resolve()
    audit_dir = Path(args.audit_root).resolve() / atlas_slug

    with Image.open(image_path) as opened:
        source = opened.convert("RGBA")

    rects, diagnostics = build_rects_from_inputs(source, config, args)
    if not rects:
        raise ValueError("Slicing produced no rectangles")

    audit_dir.mkdir(parents=True, exist_ok=True)
    draw_overlay(source, rects, audit_dir / f"{atlas_slug}_slice_overlay.png")
    records = export_slices(
        image_path=image_path,
        image=source,
        rects=rects,
        atlas_name=str(atlas_name),
        group=str(group),
        output_root=output_root,
        audit_dir=audit_dir,
        sizes=sizes,
        resize_mode=args.resize_mode,
        focus_cell=args.focus_cell,
        raw_crops=args.raw_crops,
        omit_empty_threshold=args.omit_empty_threshold,
    )

    audit_json = {
        "schema": "mechanist.manual_slice_audit.v1",
        "source_image": str(image_path),
        "source_size": {"width": source.width, "height": source.height},
        "atlas_name": str(atlas_name),
        "group": str(group),
        "output_root": str(output_root),
        "sizes": sizes,
        "resize_mode": args.resize_mode,
        "focus_cell": args.focus_cell or "",
        "diagnostics": diagnostics,
        "rect_count": len(rects),
        "exported_count": sum(1 for record in records if record.output_files),
        "records": [asdict(record) for record in records],
    }
    (audit_dir / f"{atlas_slug}_slice_audit.json").write_text(json.dumps(audit_json, indent=2), encoding="utf-8")
    write_audit_tsv(records, audit_dir / f"{atlas_slug}_slice_audit.tsv")

    print(f"Manual slice export complete for {atlas_name}")
    print(f"Source:       {image_path}")
    print(f"Source size:  {source.width}x{source.height}")
    print(f"Group:        {group}")
    print(f"Rects:        {len(rects)}")
    print(f"Exported:     {sum(1 for record in records if record.output_files)}")
    print(f"Output root:  {output_root}")
    print(f"Audit dir:    {audit_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

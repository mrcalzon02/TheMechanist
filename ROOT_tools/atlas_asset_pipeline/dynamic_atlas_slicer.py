#!/usr/bin/env python3
"""
Dynamic source-atlas slicer and auditor.

This tool is intentionally non-destructive by default. It reads source sprite
atlases, infers grid geometry from filename hints and image structure, writes a
TSV/JSON audit, and can emit preview slices into a separate output folder.

It is built for mixed atlas families:
- named grids such as Humans8x8.png, Faction rank journals,6x6.png, Ping1x5.png
- regular 5x5 sheets with gutters/margins
- rectangular grids such as automotive 6x5 sheets
- dynamic object sheets where grid lines are irregular or cells are sparse

Dependencies: Pillow and numpy.
"""

from __future__ import annotations

import argparse
import base64
import json
import math
import re
import urllib.parse
from dataclasses import asdict, dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, UnidentifiedImageError


IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".webp", ".bmp"}
DEFAULT_ROOT = Path("ROOT_SRC_assets/Mechanist_art_SRC_do_not_MODIFY/Mechanist art/TILES")
PIPELINE_ROOT = Path("ROOT_tools/atlas_asset_pipeline")
DEFAULT_OUTPUT = PIPELINE_ROOT / "atlas_slice_preview"


@dataclass
class Cell:
    row: int
    col: int
    source_box: list[int]
    content_box: list[int]
    output_name: str


@dataclass
class AtlasAudit:
    source: str
    width: int
    height: int
    mode: str
    rows: int
    cols: int
    confidence: float
    reason: str
    boundary_mode: str
    cell_widths: list[int]
    cell_heights: list[int]
    content_width_range: list[int]
    content_height_range: list[int]
    variable_content: bool
    cells: list[Cell]


def parse_grid_hint(path: Path) -> tuple[int, int] | None:
    text = path.as_posix().lower()
    matches = re.findall(r"(?<!\d)(\d{1,2})\s*x\s*(\d{1,2})(?!\d)", text)
    if not matches:
        return None
    a, b = (int(v) for v in matches[-1])
    if a <= 0 or b <= 0 or a > 20 or b > 20:
        return None
    return a, b


def content_bounds(image: Image.Image, threshold: int = 10) -> tuple[int, int, int, int] | None:
    arr = np.asarray(image.convert("RGBA"))
    alpha = arr[:, :, 3]
    rgb = arr[:, :, :3].astype(np.int16)
    border = np.concatenate([rgb[0, :, :], rgb[-1, :, :], rgb[:, 0, :], rgb[:, -1, :]], axis=0)
    bg = np.median(border, axis=0)
    diff = np.abs(rgb - bg).sum(axis=2)
    mask = (alpha > 8) & (diff > threshold)
    ys, xs = np.where(mask)
    if len(xs) == 0 or len(ys) == 0:
        return None
    pad = 2
    return (
        max(0, int(xs.min()) - pad),
        max(0, int(ys.min()) - pad),
        min(image.width, int(xs.max()) + 1 + pad),
        min(image.height, int(ys.max()) + 1 + pad),
    )


def normalized_path(path: Path, root: Path) -> str:
    try:
        return path.relative_to(root).as_posix()
    except ValueError:
        return path.as_posix()


def split_even(total: int, count: int) -> list[tuple[int, int]]:
    edges = [round(i * total / count) for i in range(count + 1)]
    return [(edges[i], edges[i + 1]) for i in range(count)]


def ranges_from_edges(edges: list[int]) -> list[tuple[int, int]]:
    return [(edges[i], edges[i + 1]) for i in range(len(edges) - 1)]


def edge_strength(gray: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    vertical = np.abs(np.diff(gray.astype(np.float32), axis=1)).mean(axis=0)
    horizontal = np.abs(np.diff(gray.astype(np.float32), axis=0)).mean(axis=1)
    return vertical, horizontal


def score_grid(gray: np.ndarray, cols: int, rows: int) -> float:
    vertical, horizontal = edge_strength(gray)
    h, w = gray.shape
    expected_x = [round(i * w / cols) for i in range(1, cols)]
    expected_y = [round(i * h / rows) for i in range(1, rows)]

    def local_score(line: np.ndarray, positions: list[int]) -> float:
        if not positions:
            return 0.0
        global_mean = float(line.mean()) + 1.0e-6
        values = []
        for pos in positions:
            left = max(0, pos - 4)
            right = min(len(line), pos + 5)
            if left < right:
                values.append(float(line[left:right].max()) / global_mean)
        return sum(values) / max(len(values), 1)

    sx = local_score(vertical, expected_x)
    sy = local_score(horizontal, expected_y)
    divisibility_bonus = 0.0
    if w % cols == 0:
        divisibility_bonus += 0.15
    if h % rows == 0:
        divisibility_bonus += 0.15
    return min(1.0, ((sx + sy) / 8.0) + divisibility_bonus)


def load_config(path: Path | None) -> dict:
    if path is None or not path.exists():
        return {"overrides": []}
    return json.loads(path.read_text(encoding="utf-8"))


def parse_edge_list(raw: str) -> list[int] | None:
    if not raw or not str(raw).strip():
        return None
    values = [int(float(item.strip())) for item in str(raw).split(",") if item.strip()]
    return sorted(set(values))


def apply_review_tags(config: dict, review_path: Path | None) -> dict:
    if review_path is None or not review_path.exists():
        return config
    records = json.loads(review_path.read_text(encoding="utf-8"))
    overrides = list(config.get("overrides", []))
    for record in records:
        source = str(record.get("source", ""))
        if not source:
            continue
        x_edges = parse_edge_list(record.get("x_edges", ""))
        y_edges = parse_edge_list(record.get("y_edges", ""))
        if not x_edges and not y_edges:
            continue
        grid = str(record.get("detected_grid", "")).lower().split("x")
        if len(grid) != 2:
            continue
        entry = {
            "match": source,
            "cols": int(grid[0]),
            "rows": int(grid[1]),
            "refine_boundaries": False,
            "reason": "manual review export edge override",
        }
        if x_edges:
            entry["x_edges"] = x_edges
        if y_edges:
            entry["y_edges"] = y_edges
        overrides.insert(0, entry)
    merged = dict(config)
    merged["overrides"] = overrides
    return merged


def matching_override(path: Path, root: Path, config: dict) -> dict | None:
    rel = normalized_path(path, root).lower()
    name = path.name.lower()
    for entry in config.get("overrides", []):
        pattern = str(entry.get("match", "")).lower()
        if not pattern:
            continue
        if pattern in rel or pattern in name:
            return entry
    return None


def infer_grid(path: Path, root: Path, image: Image.Image, config: dict) -> tuple[str, int, int, float, str]:
    override = matching_override(path, root, config)
    if override:
        if "row_cols" in override:
            row_cols = [int(value) for value in override["row_cols"]]
            cols = max(row_cols)
            rows = len(row_cols)
        else:
            cols = int(override["cols"])
            rows = int(override["rows"])
        reason = str(override.get("reason", f"manual override {cols}x{rows}"))
        return "manual_override", cols, rows, 1.0, reason

    hint = parse_grid_hint(path)
    gray = np.asarray(image.convert("L"), dtype=np.uint8)
    w, h = image.size
    if hint:
        cols, rows = hint
        # Some project names use "1x5" to mean a one-row strip containing five
        # tiles. If that strip sits inside a large square canvas, use the actual
        # content aspect ratio to orient it instead of slicing empty space.
        if min(cols, rows) == 1 and max(cols, rows) > 1 and min(w, h) >= 900 and 0.9 <= (w / h) <= 1.1:
            bounds = content_bounds(image)
            if bounds:
                bx0, by0, bx1, by1 = bounds
                bw = bx1 - bx0
                bh = by1 - by0
                if bw > bh * 1.6:
                    cols, rows = max(cols, rows), 1
                    return "filename_strip_hint", cols, rows, 0.98, f"filename declares centered one-row strip {cols}x{rows}"
                if bh > bw * 1.6:
                    cols, rows = 1, max(cols, rows)
                    return "filename_strip_hint", cols, rows, 0.98, f"filename declares centered one-column strip {cols}x{rows}"
        return "filename_hint", cols, rows, 0.98, f"filename declares {cols}x{rows}"

    candidates: list[tuple[float, int, int, str]] = []
    for cols in range(1, 9):
        for rows in range(1, 9):
            if cols == 1 and rows == 1:
                continue
            # The atlases are mostly near-square cells; allow rectangular grids
            # but penalize wild cell aspect ratios.
            cell_w = w / cols
            cell_h = h / rows
            aspect = max(cell_w, cell_h) / max(min(cell_w, cell_h), 1.0)
            if aspect > 1.7:
                continue
            score = score_grid(gray, cols, rows)
            if cols == 5 and rows == 5:
                score += 0.18
            score -= abs(cols - rows) * 0.015
            candidates.append((score, cols, rows, f"structural grid score {score:.3f}"))

    candidates.sort(reverse=True)
    square_project_atlas = min(w, h) >= 900 and 0.9 <= (w / h) <= 1.1
    best_5x5 = next((item for item in candidates if item[1] == 5 and item[2] == 5), None)

    if square_project_atlas and best_5x5:
        best_score, best_cols, best_rows, best_reason = candidates[0]
        five_score = best_5x5[0]
        if (best_cols, best_rows) == (5, 5):
            return "detected_grid", 5, 5, round(min(five_score, 0.95), 3), "project square atlas resolved as 5x5"

        # Internal object silhouettes often produce stronger false grid edges
        # than real atlas gutters. Only trust a non-5x5 square-sheet result if
        # it beats the 5x5 score by a large margin and is itself high confidence.
        if best_score >= 0.88 and (best_score - five_score) >= 0.22:
            return "detected_grid_review", best_cols, best_rows, round(min(best_score, 0.95), 3), (
                f"{best_reason}; non-5x5 accepted over 5x5={five_score:.3f}"
            )
        return "project_default_5x5", 5, 5, round(min(five_score, 0.95), 3), (
            f"defaulted to 5x5; alternate {best_cols}x{best_rows} score={best_score:.3f} "
            f"was not strong enough over 5x5={five_score:.3f}"
        )

    if candidates and candidates[0][0] >= 0.48:
        score, cols, rows, reason = candidates[0]
        return "detected_grid", cols, rows, round(min(score, 0.95), 3), reason

    # Last conservative fallback: 5x5 for large square-ish atlases, otherwise
    # single image. The audit marks this so it can be manually reviewed.
    if square_project_atlas:
        return "fallback_5x5_review", 5, 5, 0.25, "large square atlas with no reliable grid signal"
    return "single_image", 1, 1, 0.9, "no grid; treated as standalone"


def smooth_profile(profile: np.ndarray, radius: int = 3) -> np.ndarray:
    if radius <= 0:
        return profile.astype(np.float32)
    kernel = np.ones((radius * 2) + 1, dtype=np.float32) / ((radius * 2) + 1)
    return np.convolve(profile.astype(np.float32), kernel, mode="same")


def line_strength(gray: np.ndarray, axis: int) -> np.ndarray:
    if axis == 0:
        mean_line = gray.mean(axis=0).astype(np.float32)
        variance_line = gray.var(axis=0).astype(np.float32)
        gradient = np.abs(np.diff(gray.astype(np.float32), axis=1)).mean(axis=0)
        gradient = np.pad(gradient, (0, 1), mode="edge")
    else:
        mean_line = gray.mean(axis=1).astype(np.float32)
        variance_line = gray.var(axis=1).astype(np.float32)
        gradient = np.abs(np.diff(gray.astype(np.float32), axis=0)).mean(axis=1)
        gradient = np.pad(gradient, (0, 1), mode="edge")

    dark_line = 1.0 - (mean_line / 255.0)
    low_variance = 1.0 - np.clip(variance_line / (float(variance_line.max()) + 1.0e-6), 0.0, 1.0)
    gradient_score = gradient / (float(gradient.max()) + 1.0e-6)
    strength = (gradient_score * 0.55) + (dark_line * 0.30) + (low_variance * 0.15)
    return smooth_profile(strength, 3)


def refine_edges(total: int, count: int, strength: np.ndarray, search_px: int) -> list[int]:
    edges = [0]
    min_gap = max(24, int(total / max(count, 1) * 0.55))
    last = 0
    for index in range(1, count):
        nominal = round(index * total / count)
        left = max(last + min_gap, nominal - search_px)
        right = min(total - ((count - index) * min_gap), nominal + search_px)
        if left >= right:
            chosen = nominal
        else:
            window = strength[left:right + 1]
            chosen = left + int(np.argmax(window))
        edges.append(max(last + min_gap, min(total, chosen)))
        last = edges[-1]
    edges.append(total)
    return edges


def refine_edges_balanced(total: int, count: int, strength: np.ndarray, search_px: int) -> list[int]:
    if count <= 1:
        return [0, total]

    nominal_edges = [round(i * total / count) for i in range(count + 1)]
    nominal_cell = total / count
    min_gap = max(12, round(nominal_cell * 0.86))
    max_gap = round(nominal_cell * 1.14)
    normalized = strength.astype(np.float32)
    spread = float(normalized.max() - normalized.min())
    if spread > 1.0e-6:
        normalized = (normalized - float(normalized.min())) / spread

    candidate_sets: list[list[int]] = []
    for index in range(1, count):
        nominal = nominal_edges[index]
        candidates = set()
        left = max(1, nominal - search_px)
        right = min(total - 1, nominal + search_px)
        for pos in range(left, right + 1):
            candidates.add(pos)
        candidates.add(nominal)
        candidate_sets.append(sorted(candidates))

    states: dict[int, tuple[float, list[int]]] = {0: (0.0, [0])}
    for edge_index, candidates in enumerate(candidate_sets, start=1):
        next_states: dict[int, tuple[float, list[int]]] = {}
        for previous_edge, (previous_score, previous_path) in states.items():
            previous_width = previous_path[-1] - previous_path[-2] if len(previous_path) >= 2 else nominal_cell
            for candidate in candidates:
                gap = candidate - previous_edge
                if gap < min_gap or gap > max_gap:
                    continue
                nominal = nominal_edges[edge_index]
                deviation = abs(candidate - nominal) / max(search_px, 1)
                width_deviation = abs(gap - nominal_cell) / nominal_cell
                width_jump = abs(gap - previous_width) / nominal_cell
                score = (
                    previous_score
                    + float(normalized[candidate])
                    - (0.42 * deviation)
                    - (0.95 * width_deviation)
                    - (0.30 * width_jump)
                )
                existing = next_states.get(candidate)
                if existing is None or score > existing[0]:
                    next_states[candidate] = (score, previous_path + [candidate])
        if not next_states:
            return nominal_edges
        # Keep the best local candidates; the grid is tiny but this prevents a
        # very noisy sheet from exploding state count.
        states = dict(sorted(next_states.items(), key=lambda item: item[1][0], reverse=True)[:450])

    best_path = None
    best_score = -1.0e18
    for last_edge, (score, path) in states.items():
        final_gap = total - last_edge
        if final_gap < min_gap or final_gap > max_gap:
            continue
        previous_width = path[-1] - path[-2] if len(path) >= 2 else nominal_cell
        width_deviation = abs(final_gap - nominal_cell) / nominal_cell
        width_jump = abs(final_gap - previous_width) / nominal_cell
        final_score = score - (0.95 * width_deviation) - (0.30 * width_jump)
        if final_score > best_score:
            best_score = final_score
            best_path = path + [total]
    return best_path or nominal_edges


def should_refine_boundaries(path: Path, root: Path, config: dict, mode: str, cols: int, rows: int) -> bool:
    if cols <= 1 and rows <= 1:
        return False
    override = matching_override(path, root, config)
    if override and "refine_boundaries" in override:
        return bool(override["refine_boundaries"])
    rel = normalized_path(path, root)
    review_sources = set(config.get("review_refine_sources", []))
    if rel in review_sources:
        return True
    if "dynamic" in (override or {}).get("reason", "").lower():
        return True
    if cols == 5 and rows == 5 and mode in {"detected_grid", "project_default_5x5", "fallback_5x5_review", "manual_override"}:
        return True
    return False


def grid_ranges(
    image: Image.Image,
    path: Path,
    root: Path,
    config: dict,
    mode: str,
    cols: int,
    rows: int,
) -> tuple[list[tuple[int, int]], list[tuple[int, int]], str]:
    override = matching_override(path, root, config)
    if override:
        content_region = override.get("content_region")
        if content_region:
            rx, ry, rw, rh = [int(value) for value in content_region]
            return (
                [(rx + x0, rx + x1) for x0, x1 in split_even(rw, cols)],
                [(ry + y0, ry + y1) for y0, y1 in split_even(rh, rows)],
                "manual_content_region",
            )
        x_edges = override.get("x_edges")
        y_edges = override.get("y_edges")
        if x_edges or y_edges:
            if x_edges:
                x_edges = [int(value) for value in x_edges]
                if len(x_edges) != cols + 1 or x_edges[0] < 0 or x_edges[-1] > image.width or any(a >= b for a, b in zip(x_edges, x_edges[1:])):
                    raise ValueError(f"Invalid x_edges for {path}: expected {cols + 1} increasing edges within 0..{image.width}")
                x_ranges = ranges_from_edges(x_edges)
            else:
                x_ranges = split_even(image.width, cols)
            if y_edges:
                y_edges = [int(value) for value in y_edges]
                if len(y_edges) != rows + 1 or y_edges[0] < 0 or y_edges[-1] > image.height or any(a >= b for a, b in zip(y_edges, y_edges[1:])):
                    raise ValueError(f"Invalid y_edges for {path}: expected {rows + 1} increasing edges within 0..{image.height}")
                y_ranges = ranges_from_edges(y_edges)
            else:
                y_ranges = split_even(image.height, rows)
            return x_ranges, y_ranges, "manual_edges"

    if mode == "filename_strip_hint":
        bounds = content_bounds(image)
        if bounds:
            rx0, ry0, rx1, ry1 = bounds
            return (
                [(rx0 + x0, rx0 + x1) for x0, x1 in split_even(rx1 - rx0, cols)],
                [(ry0 + y0, ry0 + y1) for y0, y1 in split_even(ry1 - ry0, rows)],
                "auto_content_strip",
            )

    if not should_refine_boundaries(path, root, config, mode, cols, rows):
        return split_even(image.width, cols), split_even(image.height, rows), "even"

    gray = np.asarray(image.convert("L"), dtype=np.uint8)
    x_search = max(6, min(30, round(image.width / max(cols, 1) * 0.12)))
    y_search = max(6, min(30, round(image.height / max(rows, 1) * 0.12)))
    x_edges = refine_edges_balanced(image.width, cols, line_strength(gray, 0), x_search)
    y_edges = refine_edges_balanced(image.height, rows, line_strength(gray, 1), y_search)
    return ranges_from_edges(x_edges), ranges_from_edges(y_edges), f"balanced_refine_x{x_search}_y{y_search}"


def row_grid_ranges(
    image: Image.Image,
    path: Path,
    root: Path,
    config: dict,
    mode: str,
    cols: int,
    rows: int,
) -> tuple[list[tuple[int, tuple[int, int], list[tuple[int, int]]]], str]:
    override = matching_override(path, root, config)
    if not override or "row_cols" not in override:
        x_ranges, y_ranges, boundary_mode = grid_ranges(image, path, root, config, mode, cols, rows)
        return [(index + 1, y_range, x_ranges) for index, y_range in enumerate(y_ranges)], boundary_mode

    row_cols = [int(value) for value in override["row_cols"]]
    gray = np.asarray(image.convert("L"), dtype=np.uint8)
    y_search = max(6, min(30, round(image.height / max(len(row_cols), 1) * 0.12)))
    y_edges = refine_edges_balanced(image.height, len(row_cols), line_strength(gray, 1), y_search)
    y_ranges = ranges_from_edges(y_edges)
    refine = bool(override.get("refine_boundaries", True))
    rows_out: list[tuple[int, tuple[int, int], list[tuple[int, int]]]] = []
    modes = []
    for row_index, (y_range, row_col_count) in enumerate(zip(y_ranges, row_cols), start=1):
        if refine and row_col_count > 1:
            y0, y1 = y_range
            row_gray = gray[y0:y1, :]
            x_search = max(6, min(30, round(image.width / max(row_col_count, 1) * 0.12)))
            x_edges = refine_edges_balanced(image.width, row_col_count, line_strength(row_gray, 0), x_search)
            x_ranges = ranges_from_edges(x_edges)
            modes.append(f"r{row_index}x{x_search}")
        else:
            x_ranges = split_even(image.width, row_col_count)
        rows_out.append((row_index, y_range, x_ranges))
    return rows_out, f"row_specific_{','.join(str(value) for value in row_cols)}_{';'.join(modes) or 'even'}"


def trim_content(image: Image.Image, box: tuple[int, int, int, int], threshold: int = 8) -> tuple[int, int, int, int]:
    region = image.crop(box).convert("RGBA")
    arr = np.asarray(region)
    alpha = arr[:, :, 3]
    rgb = arr[:, :, :3].astype(np.int16)
    border = np.concatenate([rgb[0, :, :], rgb[-1, :, :], rgb[:, 0, :], rgb[:, -1, :]], axis=0)
    bg = np.median(border, axis=0)
    diff = np.abs(rgb - bg).sum(axis=2)
    mask = (alpha > 8) & (diff > threshold)
    ys, xs = np.where(mask)
    if len(xs) == 0 or len(ys) == 0:
        return list(box)
    x0, y0, x1, y1 = box
    pad = 1
    return (
        max(x0, x0 + int(xs.min()) - pad),
        max(y0, y0 + int(ys.min()) - pad),
        min(x1, x0 + int(xs.max()) + 1 + pad),
        min(y1, y0 + int(ys.max()) + 1 + pad),
    )


def make_square_tile(image: Image.Image, content_box: tuple[int, int, int, int], size: int) -> Image.Image:
    tile = image.crop(content_box).convert("RGBA")
    w, h = tile.size
    if w == 0 or h == 0:
        return Image.new("RGBA", (size, size), (0, 0, 0, 0))
    scale = min(size / w, size / h)
    new_size = (max(1, round(w * scale)), max(1, round(h * scale)))
    tile = tile.resize(new_size, Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(tile, ((size - new_size[0]) // 2, (size - new_size[1]) // 2))
    return canvas


def analyze_atlas(path: Path, root: Path, output_dir: Path, tile_size: int, emit_slices: bool, emit_overlay: bool, config: dict) -> AtlasAudit:
    with Image.open(path) as opened:
        image = opened.convert("RGBA")
    cols_mode, cols, rows, confidence, reason = infer_grid(path, root, image, config)
    row_layouts, boundary_mode = row_grid_ranges(image, path, root, config, cols_mode, cols, rows)
    rel = normalized_path(path, root)
    safe_stem = re.sub(r"[^A-Za-z0-9_.-]+", "_", path.stem).strip("_")
    atlas_out = output_dir / safe_stem
    cells: list[Cell] = []
    cell_widths: list[int] = []
    cell_heights: list[int] = []

    for row, (y0, y1), x_ranges in row_layouts:
        cell_heights.append(y1 - y0)
        for col, (x0, x1) in enumerate(x_ranges, start=1):
            cell_widths.append(x1 - x0)
            source_box = (x0, y0, x1, y1)
            content_box = trim_content(image, source_box)
            output_name = f"{safe_stem}_r{row:02d}c{col:02d}.png"
            cells.append(
                Cell(
                    row=row,
                    col=col,
                    source_box=[x0, y0, x1 - x0, y1 - y0],
                    content_box=[content_box[0], content_box[1], content_box[2] - content_box[0], content_box[3] - content_box[1]],
                    output_name=output_name,
                )
            )
            if emit_slices:
                atlas_out.mkdir(parents=True, exist_ok=True)
                make_square_tile(image, content_box, tile_size).save(atlas_out / output_name)

    if emit_overlay:
        output_dir.mkdir(parents=True, exist_ok=True)
        preview = image.convert("RGB").copy()
        draw = ImageDraw.Draw(preview)
        for _, (y0, y1), x_ranges in row_layouts:
            draw.rectangle((0, y0, image.width - 1, y1 - 1), outline=(0, 220, 255), width=2)
            for x0, x1 in x_ranges:
                draw.rectangle((x0, y0, x1 - 1, y1 - 1), outline=(255, 225, 0), width=2)
        for cell in cells:
            x, y, w, h = cell.content_box
            draw.rectangle((x, y, x + w - 1, y + h - 1), outline=(255, 80, 80), width=1)
        preview.thumbnail((1000, 1000), Image.Resampling.LANCZOS)
        preview.save(output_dir / f"{safe_stem}_grid_preview.png")

    content_widths = [cell.content_box[2] for cell in cells]
    content_heights = [cell.content_box[3] for cell in cells]
    width_range = [min(content_widths), max(content_widths)] if content_widths else [0, 0]
    height_range = [min(content_heights), max(content_heights)] if content_heights else [0, 0]
    variable_content = (
        width_range[1] - width_range[0] > max(8, round(image.width / max(cols, 1) * 0.12))
        or height_range[1] - height_range[0] > max(8, round(image.height / max(rows, 1) * 0.12))
        or "dynamic" in reason.lower()
        or "per-cell" in reason.lower()
    )

    return AtlasAudit(
        source=rel,
        width=image.width,
        height=image.height,
        mode=cols_mode,
        rows=rows,
        cols=cols,
        confidence=confidence,
        reason=reason,
        boundary_mode=boundary_mode,
        cell_widths=cell_widths,
        cell_heights=cell_heights,
        content_width_range=width_range,
        content_height_range=height_range,
        variable_content=variable_content,
        cells=cells,
    )


def iter_images(root: Path):
    for path in sorted(root.rglob("*")):
        if path.is_file() and path.suffix.lower() in IMAGE_EXTENSIONS:
            yield path


def write_reports(audits: list[AtlasAudit], output_dir: Path) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    data = [asdict(audit) for audit in audits]
    (output_dir / "atlas_slicing_audit.json").write_text(json.dumps(data, indent=2), encoding="utf-8")
    lines = ["source\twidth\theight\tmode\tcols\trows\tconfidence\treason\tboundary_mode\tcell_widths\tcell_heights\tcontent_width_range\tcontent_height_range\tvariable_content\tcell_count"]
    for audit in audits:
        lines.append(
            "\t".join(
                [
                    audit.source,
                    str(audit.width),
                    str(audit.height),
                    audit.mode,
                    str(audit.cols),
                    str(audit.rows),
                    f"{audit.confidence:.3f}",
                    audit.reason,
                    audit.boundary_mode,
                    ",".join(str(v) for v in audit.cell_widths),
                    ",".join(str(v) for v in audit.cell_heights),
                    ",".join(str(v) for v in audit.content_width_range),
                    ",".join(str(v) for v in audit.content_height_range),
                    str(audit.variable_content).lower(),
                    str(len(audit.cells)),
                ]
            )
        )
    (output_dir / "atlas_slicing_audit.tsv").write_text("\n".join(lines) + "\n", encoding="utf-8")


def html_attr(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace('"', "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )


def html_json_attr(value) -> str:
    raw = json.dumps(value, separators=(",", ":")).encode("utf-8")
    return base64.b64encode(raw).decode("ascii")


def preview_stem_for_source(source: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", Path(source).stem).strip("_")


def source_image_href(source: str) -> str:
    parts = ["..", "..", "..", *Path(DEFAULT_ROOT).parts, *Path(source).parts]
    return "/".join(urllib.parse.quote(part) for part in parts)


def write_preview_index(output_dir: Path, audits: list[AtlasAudit]) -> None:
    previews = sorted(output_dir.glob("*_grid_preview.png"))
    if not previews:
        return
    audit_by_stem = {preview_stem_for_source(audit.source): audit for audit in audits}

    thumb_size = 220
    padding = 18
    label_h = 34
    cols = 4
    rows = math.ceil(len(previews) / cols)
    sheet = Image.new("RGB", (cols * (thumb_size + padding) + padding, rows * (thumb_size + label_h + padding) + padding), (24, 24, 24))
    draw = ImageDraw.Draw(sheet)
    html_items = []

    for index, path in enumerate(previews):
        col = index % cols
        row = index // cols
        x = padding + col * (thumb_size + padding)
        y = padding + row * (thumb_size + label_h + padding)
        with Image.open(path) as opened:
            thumb = opened.convert("RGB")
            thumb.thumbnail((thumb_size, thumb_size), Image.Resampling.LANCZOS)
        sheet.paste(thumb, (x + (thumb_size - thumb.width) // 2, y))
        label = path.name.replace("_grid_preview.png", "")
        audit = audit_by_stem.get(label)
        source = audit.source if audit else label
        source_href = source_image_href(source) if audit else path.name
        mode = audit.mode if audit else ""
        boundary_mode = audit.boundary_mode if audit else ""
        grid = f"{audit.cols}x{audit.rows}" if audit else ""
        confidence = f"{audit.confidence:.3f}" if audit else ""
        reason = audit.reason if audit else ""
        width = str(audit.width) if audit else ""
        height = str(audit.height) if audit else ""
        cells_json = html_json_attr([asdict(cell) for cell in audit.cells] if audit else [])
        uniform_grid = bool(audit and audit.cells and len(audit.cells) == audit.rows * audit.cols)
        current_x_edges = ""
        if uniform_grid:
            current_x_edges = ",".join(
                str(edge) for edge in [audit.cells[0].source_box[0], *[cell.source_box[0] + cell.source_box[2] for cell in audit.cells[: audit.cols]]]
            )
        current_y_edges = ""
        if uniform_grid:
            row_first_cells = audit.cells[0::audit.cols]
            current_y_edges = ",".join(str(edge) for edge in [row_first_cells[0].source_box[1], *[cell.source_box[1] + cell.source_box[3] for cell in row_first_cells]])
        draw.text((x, y + thumb_size + 4), label[:34], fill=(230, 230, 230))
        html_items.append(
            '<figure class="atlas-card" '
            f'data-source="{html_attr(source)}" '
            f'data-preview="{html_attr(path.name)}" '
            f'data-source-image="{html_attr(source_href)}" '
            f'data-mode="{html_attr(mode)}" '
            f'data-boundary-mode="{html_attr(boundary_mode)}" '
            f'data-grid="{html_attr(grid)}" '
            f'data-width="{html_attr(width)}" '
            f'data-height="{html_attr(height)}" '
            f'data-cells="{html_attr(cells_json)}" '
            f'data-default-x-edges="{html_attr(current_x_edges)}" '
            f'data-default-y-edges="{html_attr(current_y_edges)}" '
            f'data-confidence="{html_attr(confidence)}" '
            f'data-reason="{html_attr(reason)}">'
            '<div class="preview-wrap">'
            f'<a href="{html_attr(source_href)}"><img src="{html_attr(source_href)}" alt="{html_attr(label)}"></a>'
            '<div class="cut-overlay"></div><div class="hover-line"></div><div class="segment-line"></div><div class="cell-box"></div>'
            '</div>'
            f'<figcaption><strong>{html_attr(source)}</strong><span>{html_attr(mode)} {html_attr(grid)} conf={html_attr(confidence)} {html_attr(boundary_mode)} | <a href="{html_attr(path.name)}">baked preview</a></span></figcaption>'
            '<div class="review-buttons">'
            '<button type="button" data-mark="ok">OK</button>'
            '<button type="button" data-mark="incorrect">Incorrect Slice</button>'
            '<button type="button" data-mark="review">Needs Review</button>'
            '</div>'
            '<div class="edge-tools">'
            '<button type="button" data-capture="off" data-active="true">Capture Off</button>'
            '<button type="button" data-capture="x">Capture X</button>'
            '<button type="button" data-capture="y">Capture Y</button>'
            '<button type="button" data-refresh-preview>Refresh Cut Preview</button>'
            '<span class="coord-readout"></span>'
            '</div>'
            '<div class="scope-tools">'
            '<label>Segment depth px<input data-scope="depth" placeholder="blank = full image"></label>'
            '<label>Row scope<input data-scope="row" placeholder="optional row number"></label>'
            '<label>Column scope<input data-scope="col" placeholder="optional column number"></label>'
            '</div>'
            f'<label>X edges<input data-edge="x" value="{html_attr(current_x_edges)}"></label>'
            f'<label>Y edges<input data-edge="y" value="{html_attr(current_y_edges)}"></label>'
            '<label>Segment cuts<input data-segment-cuts placeholder="captured partial cuts JSON"></label>'
            '<label>Selected cells<input data-selected-cells placeholder="click cells to add rNcM targets"></label>'
            '<label>Tool action<select data-tool-action><option value=""></option><option value="context-stamp">context-stamp</option><option value="texture-infill">texture-infill</option><option value="procedural-infill">procedural-infill</option><option value="censor-boxes">censor-boxes</option></select></label>'
            '<textarea placeholder="Optional notes: expected grid, bad row/column, manual sizing issue"></textarea>'
            '</figure>'
        )

    sheet.save(output_dir / "atlas_grid_preview_contact_sheet.png")
    html = """<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Atlas Grid Preview Audit</title>
<style>
body{background:#181818;color:#eee;font-family:Arial,sans-serif;margin:24px}
.toolbar{position:sticky;top:0;z-index:2;background:#181818;border-bottom:1px solid #555;padding:12px 0 14px;margin-bottom:18px}
.toolbar button{margin-right:8px}
.tabs{display:flex;gap:8px;margin:10px 0 14px}
.tab-panel{display:none}
.tab-panel[data-active="true"]{display:block}
.active-editor{display:grid;grid-template-columns:minmax(0,1fr);gap:12px}
.active-nav{display:flex;align-items:center;gap:8px;flex-wrap:wrap;background:#242424;border:1px solid #555;padding:10px}
.active-card-slot{overflow:auto;background:#111;border:1px solid #555;padding:10px;max-height:calc(100vh - 210px)}
.active-card-slot figure{width:var(--active-width,900px);max-width:none}
.active-card-slot .preview-wrap{width:var(--active-width,900px)}
.active-card-slot figcaption,.active-card-slot label,.active-card-slot textarea,.active-card-slot button,.active-card-slot select,.active-card-slot input{font-size:12px}
.compile-panel{background:#242424;border:1px solid #555;padding:12px;margin-bottom:18px}
.compile-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:10px}
.size-row{display:flex;gap:8px;flex-wrap:wrap;margin:10px 0}
.size-row label{display:flex;align-items:center;gap:4px;margin:0}
.compile-warning{color:#ffcf6e;font-size:12px}
.command-box{font-family:Consolas,monospace;background:#101010;border:1px solid #555;color:#eee;padding:8px;white-space:pre-wrap;word-break:break-word}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(260px,1fr));gap:18px}
figure{margin:0;background:#242424;padding:10px;border:2px solid transparent}
figure[data-status="incorrect"]{border-color:#ff6b4a;background:#33221e}
figure[data-status="review"]{border-color:#e6c94a;background:#302b1a}
figure[data-status="ok"]{border-color:#50b878}
img{width:100%;height:auto;display:block}
.preview-wrap{position:relative}
.preview-wrap a{display:block}
.cut-overlay{position:absolute;inset:0;pointer-events:none}
.hover-line,.segment-line,.cell-box{display:none;position:absolute;pointer-events:none;box-sizing:border-box}
.hover-line{z-index:3;background:#f6ff58;box-shadow:0 0 0 1px #111,0 0 8px rgba(246,255,88,.85)}
.segment-line{z-index:4;background:#ff7a38;box-shadow:0 0 0 1px #111,0 0 10px rgba(255,122,56,.9)}
.cell-box{z-index:2;border:2px solid #7cff9e;background:rgba(124,255,158,.12)}
figcaption{font-size:12px;margin-top:8px;word-break:break-word;min-height:48px}
figcaption span{display:block;color:#bbb;margin-top:4px}
.review-buttons{display:flex;gap:6px;margin-top:8px;flex-wrap:wrap}
.edge-tools{display:flex;gap:6px;margin-top:8px;align-items:center;flex-wrap:wrap}
.edge-tools button[data-active="true"]{border-color:#7cff9e;background:#245335;color:#fff}
.scope-tools{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:6px}
.coord-readout{font-size:12px;color:#bbb}
button{background:#333;color:#eee;border:1px solid #777;padding:7px 9px;cursor:pointer}
button:hover{background:#444}
label{display:block;font-size:12px;color:#ccc;margin-top:8px}
input{box-sizing:border-box;width:100%;background:#171717;color:#eee;border:1px solid #666;padding:7px;margin-top:3px}
select{box-sizing:border-box;width:100%;background:#171717;color:#eee;border:1px solid #666;padding:7px;margin-top:3px}
textarea{box-sizing:border-box;width:100%;min-height:58px;margin-top:8px;background:#171717;color:#eee;border:1px solid #666;padding:8px}
.counts{color:#bbb;margin-left:8px}
a{color:#9ed0ff}
</style>
</head>
<body>
<h1>Atlas Asset Pipeline</h1>
<p>Yellow/cyan lines are projected cell cuts; red boxes are detected per-cell content bounds.</p>
<div class="toolbar">
<div class="tabs">
<button type="button" data-tab="active">Active Edit</button>
<button type="button" data-tab="review">Review Slices</button>
<button type="button" data-tab="compile">Compile Packages</button>
</div>
<button type="button" id="export-json">Export Tagged JSON</button>
<button type="button" id="export-tsv">Export Tagged TSV</button>
<button type="button" id="clear-review">Clear Local Marks</button>
<span class="counts" id="counts"></span>
<p><a href="atlas_slicing_audit.tsv">TSV audit</a> | <a href="atlas_slicing_audit.json">JSON audit</a> | <a href="atlas_grid_preview_contact_sheet.png">contact sheet</a></p>
</div>
<section class="tab-panel" data-panel="compile">
<div class="compile-panel">
<div class="compile-grid">
<label>Source atlas root<input id="compile-source-root" value="ROOT_SRC_assets/Mechanist_art_SRC_do_not_MODIFY/Mechanist art/TILES"></label>
<label>Target package root<input id="compile-target-root" value="ROOT_tools/atlas_asset_pipeline/compiled_assets"></label>
<label>Output root<input id="compile-output-root" value="ROOT_tools/atlas_asset_pipeline/compiled_assets"></label>
<label>Declared source tile size<input id="compile-source-size" type="number" value="256" min="1"></label>
<label>Crispness<input id="compile-crispness" type="number" value="1.0" min="0" step="0.1"></label>
<label>Brightness<input id="compile-brightness" type="number" value="1.0" min="0" step="0.05"></label>
<label>Contrast<input id="compile-contrast" type="number" value="1.0" min="0" step="0.05"></label>
<label>Selected sources<input id="compile-sources" placeholder="optional audit source paths, comma-separated"></label>
</div>
<div class="size-row" id="compile-sizes">
<label><input type="checkbox" value="16">16</label>
<label><input type="checkbox" value="32">32</label>
<label><input type="checkbox" value="64">64</label>
<label><input type="checkbox" value="128">128</label>
<label><input type="checkbox" value="256" checked>256</label>
<label><input type="checkbox" value="512">512</label>
<label><input type="checkbox" value="1024">1024</label>
</div>
<p class="compile-warning" id="compile-warning"></p>
<button type="button" id="export-compile-config">Export Compile Config JSON</button>
<button type="button" id="copy-compile-command">Refresh Compile Command</button>
<p class="command-box" id="compile-command"></p>
</div>
</section>
<section class="tab-panel" data-panel="active" data-active="true">
<div class="active-editor">
<div class="active-nav">
<button type="button" id="active-prev">Left</button>
<button type="button" id="active-next">Right</button>
<label>Active asset<select id="active-select"></select></label>
<label>Image zoom<input id="active-zoom" type="range" min="360" max="1800" step="40" value="900"></label>
<span id="active-label"></span>
</div>
<div class="active-card-slot" id="active-card-slot"></div>
</div>
</section>
<section class="tab-panel" data-panel="review">
<div class="grid">
""" + "\n".join(html_items) + """
</div>
</section>
<script>
const STORAGE_KEY = "mechanist_atlas_slice_review_v1";
const cards = Array.from(document.querySelectorAll(".atlas-card"));
let activeIndex = 0;
let activeCard = null;
let activePlaceholder = null;

function setTab(name) {
  if (name === "active") {
    showActive(activeIndex);
  } else {
    restoreActiveCard();
  }
  for (const panel of document.querySelectorAll(".tab-panel")) {
    panel.dataset.active = panel.dataset.panel === name ? "true" : "false";
  }
}

document.addEventListener("click", event => {
  const tab = event.target.closest("button[data-tab]");
  if (!tab) return;
  setTab(tab.dataset.tab);
});

function restoreActiveCard() {
  if (activeCard && activePlaceholder && activePlaceholder.parentNode) {
    activePlaceholder.parentNode.replaceChild(activeCard, activePlaceholder);
  }
  activeCard = null;
  activePlaceholder = null;
}

function populateActiveSelect() {
  const select = document.getElementById("active-select");
  select.innerHTML = "";
  cards.forEach((card, index) => {
    const option = document.createElement("option");
    option.value = String(index);
    option.textContent = card.dataset.source;
    select.appendChild(option);
  });
}

function showActive(index) {
  if (!cards.length) return;
  restoreActiveCard();
  activeIndex = (index + cards.length) % cards.length;
  const card = cards[activeIndex];
  const parent = card.parentNode;
  activePlaceholder = document.createComment(`active-placeholder-${activeIndex}`);
  parent.insertBefore(activePlaceholder, card);
  document.getElementById("active-card-slot").appendChild(card);
  activeCard = card;
  document.getElementById("active-select").value = String(activeIndex);
  document.getElementById("active-label").textContent = `${activeIndex + 1} / ${cards.length}`;
  setCardCapture(card, "");
  refreshCutPreview(card);
}

function updateActiveZoom() {
  const value = Number(document.getElementById("active-zoom").value || 900);
  document.getElementById("active-card-slot").style.setProperty("--active-width", `${value}px`);
  if (activeCard) refreshCutPreview(activeCard);
}

function loadState() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
  } catch (error) {
    return {};
  }
}

function saveState(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function parseCells(card) {
  try {
    const text = atob(card.dataset.cells || "");
    return JSON.parse(text || "[]");
  } catch (error) {
    return [];
  }
}

function parseList(value) {
  return value.split(",").map(item => item.trim()).filter(Boolean);
}

function parseSegmentCuts(card) {
  const value = card.querySelector("input[data-segment-cuts]").value.trim();
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    return [];
  }
}

function sourcePoint(card, image, event) {
  const rect = image.getBoundingClientRect();
  const sourceWidth = Number(card.dataset.width || image.naturalWidth);
  const sourceHeight = Number(card.dataset.height || image.naturalHeight);
  return {
    rect,
    sourceWidth,
    sourceHeight,
    x: Math.round((event.clientX - rect.left) / rect.width * sourceWidth),
    y: Math.round((event.clientY - rect.top) / rect.height * sourceHeight)
  };
}

function closestOrigin(axis, point) {
  if (axis === "x") {
    return point.y <= point.sourceHeight / 2 ? "top" : "bottom";
  }
  return point.x <= point.sourceWidth / 2 ? "left" : "right";
}

function findCell(card, sourceX, sourceY) {
  return parseCells(card).find(cell => {
    const [x, y, w, h] = cell.source_box;
    return sourceX >= x && sourceX < x + w && sourceY >= y && sourceY < y + h;
  });
}

function updateLineElement(element, image, card, axis, point, segmented) {
  const rect = image.getBoundingClientRect();
  const sourceWidth = Number(card.dataset.width || image.naturalWidth);
  const sourceHeight = Number(card.dataset.height || image.naturalHeight);
  const xPx = point.x / sourceWidth * rect.width;
  const yPx = point.y / sourceHeight * rect.height;
  const depthInput = Number(card.querySelector('input[data-scope="depth"]').value || 0);
  const fullDepth = axis === "x" ? rect.height : rect.width;
  const sourceFullDepth = axis === "x" ? sourceHeight : sourceWidth;
  const depthPx = depthInput > 0 ? Math.max(1, Math.min(fullDepth, depthInput / sourceFullDepth * fullDepth)) : fullDepth;
  const origin = closestOrigin(axis, point);
  element.style.display = "block";
  if (axis === "x") {
    element.style.left = `${xPx}px`;
    element.style.width = "2px";
    element.style.height = `${segmented ? depthPx : fullDepth}px`;
    element.style.top = segmented && origin === "bottom" ? `${rect.height - depthPx}px` : "0";
  } else {
    element.style.top = `${yPx}px`;
    element.style.height = "2px";
    element.style.width = `${segmented ? depthPx : fullDepth}px`;
    element.style.left = segmented && origin === "right" ? `${rect.width - depthPx}px` : "0";
  }
}

function updateCellBox(card, image, cell) {
  const box = card.querySelector(".cell-box");
  if (!cell) {
    box.style.display = "none";
    return;
  }
  const rect = image.getBoundingClientRect();
  const sourceWidth = Number(card.dataset.width || image.naturalWidth);
  const sourceHeight = Number(card.dataset.height || image.naturalHeight);
  const [x, y, w, h] = cell.source_box;
  box.style.display = "block";
  box.style.left = `${x / sourceWidth * rect.width}px`;
  box.style.top = `${y / sourceHeight * rect.height}px`;
  box.style.width = `${w / sourceWidth * rect.width}px`;
  box.style.height = `${h / sourceHeight * rect.height}px`;
}

function setCardCapture(card, mode) {
  const capture = mode === "x" || mode === "y" ? mode : "";
  card.dataset.capture = capture;
  for (const button of card.querySelectorAll("button[data-capture]")) {
    const buttonMode = button.dataset.capture === "off" ? "" : button.dataset.capture;
    button.dataset.active = buttonMode === capture ? "true" : "false";
  }
  card.querySelector(".coord-readout").textContent = capture ? `capture ${capture.toUpperCase()} on` : "capture off";
  card.querySelector(".hover-line").style.display = "none";
}

function sourceToDisplay(card, x, y) {
  const image = card.querySelector("img");
  const rect = image.getBoundingClientRect();
  return {
    x: x / Number(card.dataset.width || image.naturalWidth) * rect.width,
    y: y / Number(card.dataset.height || image.naturalHeight) * rect.height,
    rect,
    image
  };
}

function addPreviewLine(card, axis, value, origin, depth) {
  const overlay = card.querySelector(".cut-overlay");
  const image = card.querySelector("img");
  const rect = image.getBoundingClientRect();
  const sourceWidth = Number(card.dataset.width || image.naturalWidth);
  const sourceHeight = Number(card.dataset.height || image.naturalHeight);
  const line = document.createElement("div");
  line.className = "segment-line preview-cut-line";
  line.style.display = "block";
  const fullDepth = axis === "x" ? rect.height : rect.width;
  const sourceFullDepth = axis === "x" ? sourceHeight : sourceWidth;
  const depthPx = depth ? Math.max(1, Math.min(fullDepth, Number(depth) / sourceFullDepth * fullDepth)) : fullDepth;
  if (axis === "x") {
    line.style.left = `${value / sourceWidth * rect.width}px`;
    line.style.width = "2px";
    line.style.height = `${depthPx}px`;
    line.style.top = origin === "bottom" ? `${rect.height - depthPx}px` : "0";
  } else {
    line.style.top = `${value / sourceHeight * rect.height}px`;
    line.style.height = "2px";
    line.style.width = `${depthPx}px`;
    line.style.left = origin === "right" ? `${rect.width - depthPx}px` : "0";
  }
  overlay.appendChild(line);
}

function refreshCutPreview(card) {
  card.querySelector(".cut-overlay").replaceChildren();
  card.querySelector(".segment-line").style.display = "none";
  card.querySelector(".hover-line").style.display = "none";
  for (const value of parseList(card.querySelector('input[data-edge="x"]').value).map(Number).filter(Number.isFinite)) {
    addPreviewLine(card, "x", value, "top", null);
  }
  for (const value of parseList(card.querySelector('input[data-edge="y"]').value).map(Number).filter(Number.isFinite)) {
    addPreviewLine(card, "y", value, "left", null);
  }
  for (const cut of parseSegmentCuts(card)) {
    if ((cut.axis === "x" || cut.axis === "y") && Number.isFinite(Number(cut.value))) {
      addPreviewLine(card, cut.axis, Number(cut.value), cut.origin, cut.depth);
    }
  }
}

function cardRecord(card) {
  const xValue = card.querySelector('input[data-edge="x"]').value.trim();
  const yValue = card.querySelector('input[data-edge="y"]').value.trim();
  const selectedCells = parseList(card.querySelector("input[data-selected-cells]").value.trim());
  const cells = parseCells(card);
  const selectedCellBoxes = selectedCells.map(id => {
    const match = id.match(/^r(\\d+)c(\\d+)$/i);
    if (!match) return null;
    const row = Number(match[1]);
    const col = Number(match[2]);
    const cell = cells.find(item => item.row === row && item.col === col);
    return cell ? { id, row, col, source_box: cell.source_box, content_box: cell.content_box, output_name: cell.output_name } : null;
  }).filter(Boolean);
  return {
    source: card.dataset.source,
    preview: card.dataset.preview,
    detected_grid: card.dataset.grid,
    mode: card.dataset.mode,
    boundary_mode: card.dataset.boundaryMode,
    confidence: card.dataset.confidence,
    reason: card.dataset.reason,
    width: card.dataset.width,
    height: card.dataset.height,
    x_edges: xValue === (card.dataset.defaultXEdges || "") ? "" : xValue,
    y_edges: yValue === (card.dataset.defaultYEdges || "") ? "" : yValue,
    segment_cuts: parseSegmentCuts(card),
    selected_cells: selectedCells,
    selected_cell_boxes: selectedCellBoxes,
    tool_action: card.querySelector("select[data-tool-action]").value,
    status: card.dataset.status || "",
    notes: card.querySelector("textarea").value.trim()
  };
}

function applyState() {
  const state = loadState();
  for (const card of cards) {
    const record = state[card.dataset.source] || {};
    card.dataset.status = record.status || "";
    card.querySelector("textarea").value = record.notes || "";
    if (record.x_edges) card.querySelector('input[data-edge="x"]').value = record.x_edges;
    if (record.y_edges) card.querySelector('input[data-edge="y"]').value = record.y_edges;
    if (record.segment_cuts) card.querySelector("input[data-segment-cuts]").value = JSON.stringify(record.segment_cuts);
    if (record.selected_cells) card.querySelector("input[data-selected-cells]").value = Array.isArray(record.selected_cells) ? record.selected_cells.join(",") : record.selected_cells;
    if (record.tool_action) card.querySelector("select[data-tool-action]").value = record.tool_action;
  }
  updateCounts();
}

function updateCard(card, status) {
  const state = loadState();
  const record = state[card.dataset.source] || {};
  record.status = status;
  record.notes = card.querySelector("textarea").value.trim();
  record.x_edges = card.querySelector('input[data-edge="x"]').value.trim();
  record.y_edges = card.querySelector('input[data-edge="y"]').value.trim();
  record.segment_cuts = parseSegmentCuts(card);
  record.selected_cells = parseList(card.querySelector("input[data-selected-cells]").value.trim());
  record.tool_action = card.querySelector("select[data-tool-action]").value;
  state[card.dataset.source] = record;
  saveState(state);
  card.dataset.status = status;
  updateCounts();
}

function taggedRecords() {
  return cards.map(cardRecord).filter(record => record.status === "incorrect" || record.status === "review" || record.notes || record.x_edges || record.y_edges || record.segment_cuts.length || record.selected_cells.length || record.tool_action);
}

function updateCounts() {
  const records = cards.map(cardRecord);
  const incorrect = records.filter(record => record.status === "incorrect").length;
  const review = records.filter(record => record.status === "review").length;
  const ok = records.filter(record => record.status === "ok").length;
  document.getElementById("counts").textContent = `${incorrect} incorrect, ${review} review, ${ok} ok`;
}

function download(name, mime, text) {
  const blob = new Blob([text], { type: mime });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function asTsv(records) {
  const headers = ["source", "status", "notes", "detected_grid", "mode", "boundary_mode", "confidence", "width", "height", "x_edges", "y_edges", "segment_cuts", "selected_cells", "tool_action", "preview", "reason"];
  const rows = records.map(record => headers.map(key => {
    const value = Array.isArray(record[key]) ? JSON.stringify(record[key]) : String(record[key] || "");
    return value.replaceAll("\\t", " ").replaceAll("\\n", " ");
  }).join("\\t"));
  return headers.join("\\t") + "\\n" + rows.join("\\n") + "\\n";
}

function compileSizes() {
  return Array.from(document.querySelectorAll("#compile-sizes input:checked")).map(input => Number(input.value)).filter(Number.isFinite).sort((a, b) => a - b);
}

function compileConfig() {
  const sourceSize = Number(document.getElementById("compile-source-size").value || 256);
  const sizes = compileSizes();
  const selectedSources = document.getElementById("compile-sources").value.split(",").map(item => item.trim()).filter(Boolean);
  return {
    source_root: document.getElementById("compile-source-root").value.trim(),
    target_root: document.getElementById("compile-target-root").value.trim(),
    output_root: document.getElementById("compile-output-root").value.trim(),
    source_size: sourceSize,
    sizes,
    crispness: Number(document.getElementById("compile-crispness").value || 1),
    brightness: Number(document.getElementById("compile-brightness").value || 1),
    contrast: Number(document.getElementById("compile-contrast").value || 1),
    selected_sources: selectedSources,
    use_content_box: true
  };
}

function updateCompileCommand() {
  const config = compileConfig();
  const oversize = config.sizes.filter(size => size > config.source_size);
  document.getElementById("compile-warning").textContent = oversize.length ? `Upscaling requested for ${oversize.join(", ")}px. Increasing beyond source size can introduce blur or artifacts.` : "";
  const sizeArgs = config.sizes.map(size => `--size ${size}`).join(" ");
  const command = `python ROOT_tools\\atlas_asset_pipeline\\atlas_pipeline.py compile ${sizeArgs} --source-root "${config.source_root}" --target-root "${config.target_root}" --output-root "${config.output_root}" --source-size ${config.source_size} --crispness ${config.crispness} --brightness ${config.brightness} --contrast ${config.contrast}`;
  document.getElementById("compile-command").textContent = command;
  return command;
}

document.addEventListener("mousemove", event => {
  const image = event.target.closest(".atlas-card img");
  if (!image) return;
  const card = image.closest(".atlas-card");
  const capture = card.dataset.capture;
  const point = sourcePoint(card, image, event);
  const cell = findCell(card, point.x, point.y);
  updateCellBox(card, image, cell);
  card.querySelector(".coord-readout").textContent = `x=${point.x}, y=${point.y}${cell ? ` r${cell.row}c${cell.col}` : ""}`;
  const hoverLine = card.querySelector(".hover-line");
  if (capture) {
    updateLineElement(hoverLine, image, card, capture, point, false);
  } else {
    hoverLine.style.display = "none";
  }
});

document.addEventListener("mouseleave", event => {
  const card = event.target.closest ? event.target.closest(".atlas-card") : null;
  if (!card) return;
  card.querySelector(".hover-line").style.display = "none";
}, true);

document.addEventListener("click", event => {
  const button = event.target.closest("button[data-mark]");
  if (button) {
    const card = button.closest(".atlas-card");
    updateCard(card, button.dataset.mark);
    return;
  }
  const captureButton = event.target.closest("button[data-capture]");
  if (captureButton) {
    const card = captureButton.closest(".atlas-card");
    const requested = captureButton.dataset.capture === "off" ? "" : captureButton.dataset.capture;
    const nextMode = card.dataset.capture === requested ? "" : requested;
    setCardCapture(card, nextMode);
    return;
  }
  const refreshButton = event.target.closest("button[data-refresh-preview]");
  if (refreshButton) {
    refreshCutPreview(refreshButton.closest(".atlas-card"));
    return;
  }
  const image = event.target.closest(".atlas-card img");
  if (image) {
    const card = image.closest(".atlas-card");
    const capture = card.dataset.capture;
    event.preventDefault();
    const point = sourcePoint(card, image, event);
    const sourceX = point.x;
    const sourceY = point.y;
    const cell = findCell(card, sourceX, sourceY);
    if (!capture) {
      if (cell) {
        const input = card.querySelector("input[data-selected-cells]");
        const id = `r${String(cell.row).padStart(2, "0")}c${String(cell.col).padStart(2, "0")}`;
        const values = parseList(input.value);
        if (!values.includes(id)) values.push(id);
        input.value = values.join(",");
        card.dataset.status = card.dataset.status || "review";
      }
      const state = loadState();
      const record = state[card.dataset.source] || {};
      record.status = card.dataset.status || "review";
      record.notes = card.querySelector("textarea").value.trim();
      record.x_edges = card.querySelector('input[data-edge="x"]').value.trim();
      record.y_edges = card.querySelector('input[data-edge="y"]').value.trim();
      record.segment_cuts = parseSegmentCuts(card);
      record.selected_cells = parseList(card.querySelector("input[data-selected-cells]").value.trim());
      record.tool_action = card.querySelector("select[data-tool-action]").value;
      state[card.dataset.source] = record;
      saveState(state);
      updateCounts();
      return;
    }
    const input = card.querySelector(`input[data-edge="${capture}"]`);
    const value = capture === "x" ? sourceX : sourceY;
    const parts = input.value.split(",").map(item => item.trim()).filter(Boolean);
    parts.push(String(value));
    const numeric = Array.from(new Set(parts.map(Number).filter(Number.isFinite))).sort((a, b) => a - b);
    input.value = numeric.join(",");
    const depthValue = Number(card.querySelector('input[data-scope="depth"]').value || 0);
    const rowScope = card.querySelector('input[data-scope="row"]').value.trim();
    const colScope = card.querySelector('input[data-scope="col"]').value.trim();
    const segmentCuts = parseSegmentCuts(card);
    segmentCuts.push({
      axis: capture,
      value,
      origin: closestOrigin(capture, point),
      depth: depthValue > 0 ? depthValue : null,
      row: rowScope || (cell ? cell.row : null),
      col: colScope || (cell ? cell.col : null)
    });
    card.querySelector("input[data-segment-cuts]").value = JSON.stringify(segmentCuts);
    refreshCutPreview(card);
    const state = loadState();
    const record = state[card.dataset.source] || {};
    record.status = card.dataset.status || "review";
    record.notes = card.querySelector("textarea").value.trim();
    record.x_edges = card.querySelector('input[data-edge="x"]').value.trim();
    record.y_edges = card.querySelector('input[data-edge="y"]').value.trim();
    record.segment_cuts = segmentCuts;
    record.selected_cells = parseList(card.querySelector("input[data-selected-cells]").value.trim());
    record.tool_action = card.querySelector("select[data-tool-action]").value;
    state[card.dataset.source] = record;
    saveState(state);
    card.dataset.status = record.status;
    card.querySelector(".coord-readout").textContent = `x=${sourceX}, y=${sourceY}`;
    updateCounts();
  }
});

document.addEventListener("input", event => {
  const note = event.target.closest("textarea");
  const edgeInput = event.target.closest("input[data-edge]");
  const segmentInput = event.target.closest("input[data-segment-cuts]");
  const selectedInput = event.target.closest("input[data-selected-cells]");
  const scopeInput = event.target.closest("input[data-scope]");
  const toolSelect = event.target.closest("select[data-tool-action]");
  if (!note && !edgeInput && !segmentInput && !selectedInput && !scopeInput && !toolSelect) return;
  const card = (note || edgeInput || segmentInput || selectedInput || scopeInput || toolSelect).closest(".atlas-card");
  const state = loadState();
  const record = state[card.dataset.source] || {};
  record.status = card.dataset.status || "";
  record.notes = card.querySelector("textarea").value.trim();
  record.x_edges = card.querySelector('input[data-edge="x"]').value.trim();
  record.y_edges = card.querySelector('input[data-edge="y"]').value.trim();
  record.segment_cuts = parseSegmentCuts(card);
  record.selected_cells = parseList(card.querySelector("input[data-selected-cells]").value.trim());
  record.tool_action = card.querySelector("select[data-tool-action]").value;
  state[card.dataset.source] = record;
  saveState(state);
  updateCounts();
});

document.getElementById("export-json").addEventListener("click", () => {
  download("atlas_slice_review_tags.json", "application/json", JSON.stringify(taggedRecords(), null, 2));
});

document.getElementById("export-tsv").addEventListener("click", () => {
  download("atlas_slice_review_tags.tsv", "text/tab-separated-values", asTsv(taggedRecords()));
});

document.getElementById("export-compile-config").addEventListener("click", () => {
  download("atlas_compile_config.json", "application/json", JSON.stringify(compileConfig(), null, 2));
  updateCompileCommand();
});

document.getElementById("copy-compile-command").addEventListener("click", () => {
  updateCompileCommand();
});

document.querySelector(".compile-panel").addEventListener("input", () => {
  updateCompileCommand();
});

document.getElementById("active-prev").addEventListener("click", () => {
  showActive(activeIndex - 1);
});

document.getElementById("active-next").addEventListener("click", () => {
  showActive(activeIndex + 1);
});

document.getElementById("active-select").addEventListener("change", event => {
  showActive(Number(event.target.value || 0));
});

document.getElementById("active-zoom").addEventListener("input", () => {
  updateActiveZoom();
});

document.getElementById("clear-review").addEventListener("click", () => {
  if (!confirm("Clear all local review marks and notes for this sheet?")) return;
  localStorage.removeItem(STORAGE_KEY);
  applyState();
});

populateActiveSelect();
updateActiveZoom();
applyState();
showActive(0);
updateCompileCommand();
</script>
</body>
</html>
"""
    (output_dir / "atlas_grid_preview_index.html").write_text(html, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit and preview dynamic slicing for source sprite atlases.")
    parser.add_argument("--root", default=str(DEFAULT_ROOT), help="Source atlas root to scan.")
    parser.add_argument("--output-dir", default=str(DEFAULT_OUTPUT), help="Non-source output folder for reports/previews.")
    parser.add_argument("--tile-size", type=int, default=256, help="Square output preview tile size.")
    parser.add_argument("--config", default=str(PIPELINE_ROOT / "configs/dynamic_atlas_slicer.overrides.json"), help="Optional atlas geometry override JSON.")
    parser.add_argument("--review-tags", default=None, help="Optional exported review JSON with x_edges/y_edges overrides.")
    parser.add_argument("--only", action="append", default=None, help="Substring filter for testing selected atlases.")
    parser.add_argument("--emit-slices", action="store_true", help="Write normalized square preview slices.")
    parser.add_argument("--emit-overlay", action="store_true", help="Write grid/content overlay preview images.")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    output_dir = Path(args.output_dir).resolve()
    config = load_config(Path(args.config).resolve() if args.config else None)
    config = apply_review_tags(config, Path(args.review_tags).resolve() if args.review_tags else None)
    filters = [item.lower() for item in (args.only or [])]
    paths = [path for path in iter_images(root) if not filters or any(f in path.name.lower() for f in filters)]
    audits = []
    for path in paths:
        try:
            audits.append(analyze_atlas(path, root, output_dir, args.tile_size, args.emit_slices, args.emit_overlay, config))
        except (FileNotFoundError, UnidentifiedImageError, OSError) as exc:
            print(f"WARNING skipped unreadable atlas {path}: {exc}")
    write_reports(audits, output_dir)
    if args.emit_overlay:
        write_preview_index(output_dir, audits)
    for audit in audits:
        print(
            f"{audit.mode:20s} {audit.cols}x{audit.rows} conf={audit.confidence:.3f} "
            f"{audit.width}x{audit.height} {audit.source}"
        )
    print(f"Wrote audit reports to {output_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

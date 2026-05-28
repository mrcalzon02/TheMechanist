#!/usr/bin/env python3
"""
Coordinate-based texture atlas censoring pipeline.

This tool performs strictly bounded mechanical edits on image atlases using a
JSON box configuration. By default, configured input files under input_assets/
are edited directly. Provide --output-dir only when edited copies should be
written somewhere else.

Dependencies:
    pip install pillow opencv-python numpy
"""

from __future__ import annotations

import argparse
import json
import logging
import math
import os
import shutil
import fnmatch
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

import cv2
import numpy as np
from PIL import Image


SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".tif", ".tiff", ".webp"}
DEFAULT_MODE = "pixelate"


@dataclass(frozen=True)
class BoxOperation:
    x: int
    y: int
    width: int
    height: int
    mode: str
    color: tuple[int, int, int, int] | None = None
    alpha: int | None = None
    blur_kernel: int = 51
    pixel_size: int = 16
    clamp_strategy: str = "mirror"


def configure_logging(verbose: bool) -> None:
    level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(level=level, format="%(levelname)s: %(message)s")


def normalize_mode(mode: str) -> str:
    normalized = mode.strip().lower().replace("_", "-").replace("/", "-")
    aliases = {
        "stamp": "stamp-solid",
        "solid": "stamp-solid",
        "stamp-solid": "stamp-solid",
        "transparent": "stamp-solid",
        "infill": "infill-clamp",
        "clamp": "infill-clamp",
        "infill-clamp": "infill-clamp",
        "blur": "blur",
        "gaussian-blur": "blur",
        "pixelate": "pixelate",
        "blur-pixelate": "pixelate",
    }
    if normalized not in aliases:
        raise ValueError(f"Unsupported edit mode '{mode}'")
    return aliases[normalized]


def parse_color(value: Any) -> tuple[int, int, int, int]:
    if isinstance(value, str):
        raw = value.strip()
        if raw.startswith("#"):
            raw = raw[1:]
        if len(raw) not in (6, 8):
            raise ValueError(f"Color string must be #RRGGBB or #RRGGBBAA, got '{value}'")
        channels = [int(raw[i : i + 2], 16) for i in range(0, len(raw), 2)]
        if len(channels) == 3:
            channels.append(255)
        return tuple(channels)  # type: ignore[return-value]

    if isinstance(value, list) and len(value) in (3, 4):
        channels = [int(v) for v in value]
        if len(channels) == 3:
            channels.append(255)
        if any(v < 0 or v > 255 for v in channels):
            raise ValueError(f"Color channel out of 0..255 range: {value}")
        return tuple(channels)  # type: ignore[return-value]

    raise ValueError(f"Color must be #RRGGBB/#RRGGBBAA or [r,g,b,a], got {value!r}")


def parse_box(raw: Any, defaults: dict[str, Any]) -> BoxOperation:
    if isinstance(raw, list):
        if len(raw) != 4:
            raise ValueError(f"List box must be [x, y, width, height], got {raw!r}")
        data = {"box": raw}
    elif isinstance(raw, dict):
        data = dict(raw)
    else:
        raise ValueError(f"Box entry must be a list or object, got {type(raw).__name__}")

    if "box" in data:
        x, y, width, height = [int(v) for v in data["box"]]
    else:
        x = int(data["x"])
        y = int(data["y"])
        width = int(data["width"])
        height = int(data["height"])

    if width <= 0 or height <= 0:
        raise ValueError(f"Box dimensions must be positive, got {width}x{height}")

    mode = normalize_mode(str(data.get("mode", defaults.get("mode", DEFAULT_MODE))))
    color_value = data.get("color", defaults.get("color", [255, 0, 255, 255]))
    alpha = data.get("alpha", defaults.get("alpha"))
    alpha_value = None if alpha is None else int(alpha)
    if alpha_value is not None and (alpha_value < 0 or alpha_value > 255):
        raise ValueError(f"Alpha must be in 0..255, got {alpha_value}")

    blur_kernel = int(data.get("blur_kernel", defaults.get("blur_kernel", 51)))
    pixel_size = int(data.get("pixel_size", defaults.get("pixel_size", 16)))
    clamp_strategy = str(data.get("clamp_strategy", defaults.get("clamp_strategy", "mirror"))).lower()

    return BoxOperation(
        x=x,
        y=y,
        width=width,
        height=height,
        mode=mode,
        color=parse_color(color_value),
        alpha=alpha_value,
        blur_kernel=max(3, blur_kernel),
        pixel_size=max(2, pixel_size),
        clamp_strategy=clamp_strategy,
    )


def load_config(config_path: Path) -> dict[str, list[BoxOperation]]:
    with config_path.open("r", encoding="utf-8") as handle:
        config = json.load(handle)

    defaults = dict(config.get("defaults", {}))
    atlas_map = config.get("atlases", config)
    if not isinstance(atlas_map, dict):
        raise ValueError("Config must contain an 'atlases' object or be an atlas-to-boxes object")

    parsed: dict[str, list[BoxOperation]] = {}
    for filename, raw_entries in atlas_map.items():
        if filename == "defaults":
            continue
        if not isinstance(raw_entries, list):
            raise ValueError(f"Atlas '{filename}' must map to a list of box entries")
        parsed[filename] = [parse_box(entry, defaults) for entry in raw_entries]
    return parsed


def matching_config_keys(relative: Path, source_path: Path, config: dict[str, list[BoxOperation]]) -> list[str]:
    relative_posix = relative.as_posix()
    relative_native = str(relative)
    candidates = []
    for key in config:
        normalized_key = key.replace("\\", "/")
        if key in {source_path.name, relative_posix, relative_native}:
            candidates.append(key)
            continue
        if fnmatch.fnmatch(relative_posix, normalized_key) or fnmatch.fnmatch(source_path.name, normalized_key):
            candidates.append(key)
    return candidates


def validate_box(image: Image.Image, op: BoxOperation, filename: str) -> tuple[int, int, int, int]:
    left = op.x
    top = op.y
    right = op.x + op.width
    bottom = op.y + op.height

    if left < 0 or top < 0 or right > image.width or bottom > image.height:
        raise ValueError(
            f"{filename}: box [{op.x}, {op.y}, {op.width}, {op.height}] exceeds "
            f"image bounds {image.width}x{image.height}"
        )
    return left, top, right, bottom


def ensure_odd_kernel(kernel: int, max_size: int) -> int:
    kernel = max(3, kernel)
    kernel = min(kernel, max_size if max_size % 2 == 1 else max_size - 1)
    if kernel < 3:
        kernel = 3
    if kernel % 2 == 0:
        kernel += 1
    return kernel


def apply_stamp_solid(arr: np.ndarray, bounds: tuple[int, int, int, int], op: BoxOperation) -> None:
    left, top, right, bottom = bounds
    replacement = np.array(op.color, dtype=np.uint8)
    if op.alpha is not None:
        replacement[3] = op.alpha
    arr[top:bottom, left:right] = replacement


def apply_blur(arr: np.ndarray, bounds: tuple[int, int, int, int], op: BoxOperation) -> None:
    left, top, right, bottom = bounds
    roi = arr[top:bottom, left:right].copy()
    max_kernel = max(3, min(roi.shape[0], roi.shape[1]))
    kernel = ensure_odd_kernel(op.blur_kernel, max_kernel)
    blurred = cv2.GaussianBlur(roi, (kernel, kernel), 0, borderType=cv2.BORDER_REFLECT)
    arr[top:bottom, left:right] = blurred


def apply_pixelate(arr: np.ndarray, bounds: tuple[int, int, int, int], op: BoxOperation) -> None:
    left, top, right, bottom = bounds
    roi = arr[top:bottom, left:right].copy()
    height, width = roi.shape[:2]
    small_w = max(1, math.ceil(width / op.pixel_size))
    small_h = max(1, math.ceil(height / op.pixel_size))
    small = cv2.resize(roi, (small_w, small_h), interpolation=cv2.INTER_LINEAR)
    pixelated = cv2.resize(small, (width, height), interpolation=cv2.INTER_NEAREST)
    arr[top:bottom, left:right] = pixelated


def choose_edge_patch(arr: np.ndarray, bounds: tuple[int, int, int, int], op: BoxOperation) -> np.ndarray:
    left, top, right, bottom = bounds
    height, width = bottom - top, right - left
    image_h, image_w = arr.shape[:2]
    candidates: list[tuple[int, str, np.ndarray]] = []

    if top > 0:
        candidates.append((width, "top", arr[top - 1 : top, left:right]))
    if bottom < image_h:
        candidates.append((width, "bottom", arr[bottom : bottom + 1, left:right]))
    if left > 0:
        candidates.append((height, "left", arr[top:bottom, left - 1 : left]))
    if right < image_w:
        candidates.append((height, "right", arr[top:bottom, right : right + 1]))

    if not candidates:
        return np.zeros((height, width, arr.shape[2]), dtype=np.uint8)

    _, edge_name, edge = max(candidates, key=lambda item: item[0])
    if edge_name in ("top", "bottom"):
        patch = np.repeat(edge, height, axis=0)
        if op.clamp_strategy == "mirror" and edge_name == "top" and top >= height:
            patch = arr[top - height : top, left:right][::-1, :, :]
        elif op.clamp_strategy == "mirror" and edge_name == "bottom" and bottom + height <= image_h:
            patch = arr[bottom : bottom + height, left:right][::-1, :, :]
    else:
        patch = np.repeat(edge, width, axis=1)
        if op.clamp_strategy == "mirror" and edge_name == "left" and left >= width:
            patch = arr[top:bottom, left - width : left][:, ::-1, :]
        elif op.clamp_strategy == "mirror" and edge_name == "right" and right + width <= image_w:
            patch = arr[top:bottom, right : right + width][:, ::-1, :]

    return patch[:height, :width].copy()


def apply_infill_clamp(arr: np.ndarray, bounds: tuple[int, int, int, int], op: BoxOperation) -> None:
    left, top, right, bottom = bounds
    patch = choose_edge_patch(arr, bounds, op)
    arr[top:bottom, left:right] = patch


def apply_operation(image: Image.Image, op: BoxOperation, filename: str) -> Image.Image:
    bounds = validate_box(image, op, filename)
    rgba = image.convert("RGBA")
    arr = np.array(rgba)

    if op.mode == "stamp-solid":
        apply_stamp_solid(arr, bounds, op)
    elif op.mode == "infill-clamp":
        apply_infill_clamp(arr, bounds, op)
    elif op.mode == "blur":
        apply_blur(arr, bounds, op)
    elif op.mode == "pixelate":
        apply_pixelate(arr, bounds, op)
    else:
        raise ValueError(f"{filename}: unsupported mode {op.mode}")

    edited = Image.fromarray(arr, mode="RGBA")
    if image.mode != "RGBA":
        edited = edited.convert(image.mode)
    return edited


def iter_input_files(input_dir: Path) -> Iterable[Path]:
    for path in sorted(input_dir.rglob("*")):
        if path.is_file() and path.suffix.lower() in SUPPORTED_EXTENSIONS:
            yield path


def save_image_safely(image: Image.Image, output_path: Path) -> None:
    temp_path = output_path.with_name(f"{output_path.name}.tmp{output_path.suffix}")
    try:
        image.save(temp_path)
        os.replace(temp_path, output_path)
    finally:
        if temp_path.exists():
            temp_path.unlink()


def process_assets(
    config: dict[str, list[BoxOperation]],
    input_dir: Path,
    output_dir: Path | None,
    copy_unconfigured: bool,
) -> int:
    if not input_dir.exists():
        raise FileNotFoundError(f"Input directory does not exist: {input_dir}")
    write_copies = output_dir is not None
    if write_copies:
        output_dir.mkdir(parents=True, exist_ok=True)

    processed = 0
    configured_names = set(config)
    seen_configured: set[str] = set()

    for source_path in iter_input_files(input_dir):
        relative = source_path.relative_to(input_dir)
        matching_keys = matching_config_keys(relative, source_path, config)
        output_path = output_dir / relative if write_copies else source_path
        if write_copies:
            output_path.parent.mkdir(parents=True, exist_ok=True)

        if not matching_keys:
            if copy_unconfigured and write_copies:
                shutil.copy2(source_path, output_path)
                logging.info("Copied unconfigured asset unchanged: %s", relative.as_posix())
            elif copy_unconfigured:
                logging.debug("Source-edit mode leaves unconfigured asset unchanged: %s", relative.as_posix())
            else:
                logging.debug("Skipped unconfigured asset: %s", relative.as_posix())
            continue

        for matching_key in matching_keys:
            seen_configured.add(matching_key)
        operations = [op for matching_key in matching_keys for op in config[matching_key]]
        with Image.open(source_path) as image:
            edited = image.copy()
            logging.info(
                "Processing %s with %d operation(s) from %d config rule(s)",
                relative.as_posix(),
                len(operations),
                len(matching_keys),
            )
            for index, op in enumerate(operations, start=1):
                logging.info(
                    "  op %d: mode=%s box=[%d,%d,%d,%d]",
                    index,
                    op.mode,
                    op.x,
                    op.y,
                    op.width,
                    op.height,
                )
                edited = apply_operation(edited, op, relative.as_posix())
            save_kwargs: dict[str, Any] = {}
            if edited.format:
                save_kwargs["format"] = edited.format
            save_image_safely(edited, output_path)
            if write_copies:
                logging.info("  wrote output: %s", output_path)
            else:
                logging.info("  modified source: %s", relative.as_posix())
        processed += 1

    missing = sorted(configured_names - seen_configured)
    for name in missing:
        logging.warning("Configured atlas was not found under input directory: %s", name)

    return processed


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Apply coordinate-bounded mechanical censorship edits to texture atlases."
    )
    parser.add_argument("--config", default="censor_config.json", help="JSON coordinate configuration file.")
    parser.add_argument("--input-dir", default="input_assets", help="Directory containing source atlas images.")
    parser.add_argument("--output-dir", default=None, help="Optional directory for edited copies. Omit to edit sources.")
    parser.add_argument(
        "--copy-unconfigured",
        action="store_true",
        help="Copy image files that have no configured edits into the output directory unchanged.",
    )
    parser.add_argument("--verbose", action="store_true", help="Enable debug logging.")
    return parser


def main() -> int:
    parser = build_arg_parser()
    args = parser.parse_args()
    configure_logging(args.verbose)

    config_path = Path(args.config).resolve()
    input_dir = Path(args.input_dir).resolve()
    output_dir = Path(args.output_dir).resolve() if args.output_dir else None

    logging.info("Loading config: %s", config_path)
    config = load_config(config_path)
    logging.info("Configured atlases: %d", len(config))
    logging.info("Input directory: %s", input_dir)
    if output_dir is None:
        logging.warning("SOURCE EDIT MODE: configured input atlas files will be overwritten.")
    else:
        logging.info("Output directory: %s", output_dir)

    processed = process_assets(config, input_dir, output_dir, args.copy_unconfigured)
    logging.info("Done. Edited %d configured atlas file(s).", processed)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

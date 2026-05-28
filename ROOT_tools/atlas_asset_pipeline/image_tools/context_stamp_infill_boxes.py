#!/usr/bin/env python3
"""
Edge-aware context stamping for scrubbed atlas boxes.

Copies crisp source-local patches into target boxes, matches the patch color to
the destination border, and blends only a very narrow seam. This keeps atlas
texture detail sharp and avoids the smeared look of blur/procedural fills.

For tiny infringing text or markings, configure the target box tightly around
only the offending pixels and use seam 0 or 1. Do not expand the box to a whole
object, tile, or panel unless the whole region is actually non-compliant.

Dependencies: Pillow and numpy.
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path

import numpy as np
from PIL import Image


def load_ops(path: Path) -> list[dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return data["stamps"]


def box_tuple(raw: list[int]) -> tuple[int, int, int, int]:
    x, y, w, h = (int(v) for v in raw)
    return x, y, x + w, y + h


def ring_sample(arr: np.ndarray, box: tuple[int, int, int, int], ring: int) -> np.ndarray:
    x0, y0, x1, y1 = box
    h, w = arr.shape[:2]
    samples = []
    if y0 >= ring:
        samples.append(arr[y0 - ring : y0, x0:x1])
    if y1 + ring <= h:
        samples.append(arr[y1 : y1 + ring, x0:x1])
    if x0 >= ring:
        samples.append(arr[y0:y1, x0 - ring : x0])
    if x1 + ring <= w:
        samples.append(arr[y0:y1, x1 : x1 + ring])
    if not samples:
        return arr.reshape(-1, arr.shape[2]).astype(np.float32)
    return np.concatenate([sample.reshape(-1, arr.shape[2]) for sample in samples], axis=0).astype(np.float32)


def color_match(source: np.ndarray, source_box: tuple[int, int, int, int], dest: np.ndarray, dest_box: tuple[int, int, int, int], ring: int) -> np.ndarray:
    sx0, sy0, sx1, sy1 = source_box
    patch = source[sy0:sy1, sx0:sx1].astype(np.float32)
    source_ring = ring_sample(source, source_box, ring)
    dest_ring = ring_sample(dest, dest_box, ring)

    source_mean = source_ring.mean(axis=0)
    dest_mean = dest_ring.mean(axis=0)
    source_std = np.maximum(source_ring.std(axis=0), 1.0)
    dest_std = np.maximum(dest_ring.std(axis=0), 1.0)
    matched = ((patch - source_mean) * (dest_std / source_std)) + dest_mean
    return np.clip(matched, 0, 255)


def seam_mask(height: int, width: int, seam: int) -> np.ndarray:
    if seam <= 0:
        return np.ones((height, width, 1), dtype=np.float32)
    yy = np.arange(height, dtype=np.float32)[:, None]
    xx = np.arange(width, dtype=np.float32)[None, :]
    dist = np.minimum(np.minimum(yy, height - 1 - yy), np.minimum(xx, width - 1 - xx))
    return np.clip(dist / seam, 0.0, 1.0)[:, :, None]


def apply_stamp(arr: np.ndarray, target_box: tuple[int, int, int, int], source_box: tuple[int, int, int, int], ring: int, seam: int) -> None:
    x0, y0, x1, y1 = target_box
    patch = color_match(arr, source_box, arr, target_box, ring)
    current = arr[y0:y1, x0:x1].astype(np.float32)
    mask = seam_mask(y1 - y0, x1 - x0, seam)
    result = (patch * mask) + (current * (1.0 - mask))
    arr[y0:y1, x0:x1] = np.clip(result, 0, 255).astype(np.uint8)


def run(image_path: Path, config_path: Path, warn_area: int) -> None:
    ops = load_ops(config_path)
    with Image.open(image_path) as opened:
        image = opened.convert("RGB")
    arr = np.asarray(image).copy()
    for op in ops:
        target = box_tuple(op["target"])
        source = box_tuple(op["source"])
        ring = int(op.get("ring", 4))
        seam = int(op.get("seam", 1))
        width = target[2] - target[0]
        height = target[3] - target[1]
        area = width * height
        if area > warn_area:
            print(
                f"WARNING large target area {area}px for {op['target']}; "
                "verify the box is tightly scoped to infringing content."
            )
        apply_stamp(arr, target, source, ring, seam)
        print(f"stamped target={op['target']} source={op['source']} seam={seam}")

    temp_path = image_path.with_name(image_path.name + ".tmp" + image_path.suffix)
    try:
        Image.fromarray(arr, mode="RGB").save(temp_path)
        os.replace(temp_path, image_path)
    finally:
        if temp_path.exists():
            temp_path.unlink()


def main() -> int:
    parser = argparse.ArgumentParser(description="Crisp context-stamp infill for atlas scrub boxes.")
    parser.add_argument("--image", required=True)
    parser.add_argument("--config", required=True)
    parser.add_argument("--warn-area", type=int, default=12000, help="Warn when a target box exceeds this pixel area.")
    args = parser.parse_args()
    run(Path(args.image).resolve(), Path(args.config).resolve(), args.warn_area)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

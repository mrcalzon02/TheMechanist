#!/usr/bin/env python3
"""
Procedural local texture infill for scrubbed atlas boxes.

Samples clean pixels around each target box, generates low-frequency mottled
texture from those local colors, and feathers it into the box. This avoids
pasting recognizable neighboring objects into the scrubbed region.

Dependencies: Pillow and numpy.
"""

from __future__ import annotations

import argparse
import json
import os
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter


@dataclass(frozen=True)
class Box:
    x: int
    y: int
    width: int
    height: int

    @property
    def right(self) -> int:
        return self.x + self.width

    @property
    def bottom(self) -> int:
        return self.y + self.height


def load_boxes(path: Path) -> list[Box]:
    data = json.loads(path.read_text(encoding="utf-8"))
    return [Box(*(int(v) for v in (entry["box"] if isinstance(entry, dict) else entry))) for entry in data["boxes"]]


def feather_mask(height: int, width: int, feather: int) -> np.ndarray:
    yy = np.arange(height, dtype=np.float32)[:, None]
    xx = np.arange(width, dtype=np.float32)[None, :]
    vertical = np.minimum(yy, height - 1 - yy)
    horizontal = np.minimum(xx, width - 1 - xx)
    distance = np.minimum(vertical, horizontal)
    return np.clip(distance / max(1, feather), 0.0, 1.0)[:, :, None]


def local_sample(arr: np.ndarray, box: Box, radius: int) -> np.ndarray:
    h, w = arr.shape[:2]
    left = max(0, box.x - radius)
    top = max(0, box.y - radius)
    right = min(w, box.right + radius)
    bottom = min(h, box.bottom + radius)
    region = arr[top:bottom, left:right]

    mask = np.ones(region.shape[:2], dtype=bool)
    bx0 = box.x - left
    by0 = box.y - top
    mask[max(0, by0 - 10) : min(mask.shape[0], by0 + box.height + 10), max(0, bx0 - 10) : min(mask.shape[1], bx0 + box.width + 10)] = False
    sample = region[mask]
    if sample.size == 0:
        sample = arr.reshape(-1, arr.shape[2])
    return sample.astype(np.float32)


def synthesize_texture(arr: np.ndarray, box: Box, radius: int, seed: int) -> np.ndarray:
    sample = local_sample(arr, box, radius)
    rng = np.random.default_rng(seed)
    mean = sample.mean(axis=0)
    std = np.maximum(sample.std(axis=0), 4.0)

    base = rng.normal(loc=mean, scale=std * 0.42, size=(box.height, box.width, arr.shape[2]))
    base = np.clip(base, 0, 255).astype(np.uint8)
    image = Image.fromarray(base, mode="RGB")

    low = image.resize((max(1, box.width // 9), max(1, box.height // 9)), Image.Resampling.BILINEAR)
    low = low.resize((box.width, box.height), Image.Resampling.BICUBIC).filter(ImageFilter.GaussianBlur(1.2))
    high = image.filter(ImageFilter.GaussianBlur(0.6))
    blended = Image.blend(low, high, 0.28)
    return np.asarray(blended).astype(np.float32)


def infill(image_path: Path, boxes: list[Box], radius: int, feather: int) -> None:
    image = Image.open(image_path).convert("RGB")
    arr = np.asarray(image).copy()
    for index, box in enumerate(boxes):
        current = arr[box.y : box.bottom, box.x : box.right].astype(np.float32)
        texture = synthesize_texture(arr, box, radius, seed=(box.x * 31 + box.y * 17 + index))
        mask = feather_mask(box.height, box.width, feather)

        # Preserve the immediate edge pixels more strongly for atlas continuity.
        result = (texture * mask) + (current * (1.0 - mask))
        arr[box.y : box.bottom, box.x : box.right] = np.clip(result, 0, 255).astype(np.uint8)
        print(f"infilled box [{box.x},{box.y},{box.width},{box.height}]")

    temp_path = image_path.with_name(image_path.name + ".tmp" + image_path.suffix)
    try:
        Image.fromarray(arr, mode="RGB").save(temp_path)
        os.replace(temp_path, image_path)
    finally:
        if temp_path.exists():
            temp_path.unlink()


def main() -> int:
    parser = argparse.ArgumentParser(description="Procedurally infill scrubbed boxes from local texture statistics.")
    parser.add_argument("--image", required=True)
    parser.add_argument("--config", required=True)
    parser.add_argument("--radius", type=int, default=80)
    parser.add_argument("--feather", type=int, default=10)
    args = parser.parse_args()
    infill(Path(args.image).resolve(), load_boxes(Path(args.config).resolve()), args.radius, args.feather)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

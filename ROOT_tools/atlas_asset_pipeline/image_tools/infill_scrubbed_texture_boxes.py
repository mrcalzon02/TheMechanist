#!/usr/bin/env python3
"""
Patch-based texture infill for previously scrubbed atlas boxes.

Uses only source-local image material: for each target box, it searches nearby
same-size patches that do not overlap any target box, scores them against the
target border, and pastes the best patch with a feathered edge.

Dependencies: Pillow and numpy.
"""

from __future__ import annotations

import argparse
import json
import os
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image


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


def overlaps(a: Box, b: Box, margin: int = 0) -> bool:
    return not (
        a.right + margin <= b.x
        or b.right + margin <= a.x
        or a.bottom + margin <= b.y
        or b.bottom + margin <= a.y
    )


def load_boxes(config_path: Path) -> list[Box]:
    data = json.loads(config_path.read_text(encoding="utf-8"))
    boxes = []
    for entry in data["boxes"]:
        raw = entry["box"] if isinstance(entry, dict) else entry
        boxes.append(Box(*(int(v) for v in raw)))
    return boxes


def border_score(arr: np.ndarray, target: Box, candidate: Box, border: int) -> float:
    h, w = target.height, target.width
    score = 0.0
    samples = 0

    target_patch = arr[target.y : target.bottom, target.x : target.right].astype(np.float32)
    candidate_patch = arr[candidate.y : candidate.bottom, candidate.x : candidate.right].astype(np.float32)

    # Compare candidate edge colors to the clean pixels immediately outside the target.
    if target.y >= border:
        outside = arr[target.y - border : target.y, target.x : target.right].astype(np.float32)
        cand = candidate_patch[:border, :, :]
        score += float(((outside - cand) ** 2).mean())
        samples += 1
    if target.bottom + border <= arr.shape[0]:
        outside = arr[target.bottom : target.bottom + border, target.x : target.right].astype(np.float32)
        cand = candidate_patch[h - border : h, :, :]
        score += float(((outside - cand) ** 2).mean())
        samples += 1
    if target.x >= border:
        outside = arr[target.y : target.bottom, target.x - border : target.x].astype(np.float32)
        cand = candidate_patch[:, :border, :]
        score += float(((outside - cand) ** 2).mean())
        samples += 1
    if target.right + border <= arr.shape[1]:
        outside = arr[target.y : target.bottom, target.right : target.right + border].astype(np.float32)
        cand = candidate_patch[:, w - border : w, :]
        score += float(((outside - cand) ** 2).mean())
        samples += 1

    # Penalize candidates that are very flat, since pixelated scrub blocks are flat too.
    variance = float(candidate_patch.var())
    flat_penalty = max(0.0, 80.0 - variance) * 10.0

    # Strongly avoid accidentally selecting another scrubbed/pixelated patch.
    target_similarity_penalty = max(0.0, 400.0 - float(((target_patch - candidate_patch) ** 2).mean()))
    return (score / max(1, samples)) + flat_penalty + target_similarity_penalty


def find_best_patch(arr: np.ndarray, target: Box, all_targets: list[Box], stride: int, border: int, local_radius: int) -> Box:
    h, w = arr.shape[:2]
    best_box: Box | None = None
    best_score = float("inf")

    search_left = max(0, target.x - local_radius)
    search_top = max(0, target.y - local_radius)
    search_right = min(w - target.width, target.x + local_radius)
    search_bottom = min(h - target.height, target.y + local_radius)

    for y in range(search_top, search_bottom + 1, stride):
        for x in range(search_left, search_right + 1, stride):
            candidate = Box(x, y, target.width, target.height)
            if any(overlaps(candidate, existing, margin=border) for existing in all_targets):
                continue
            score = border_score(arr, target, candidate, border)
            if score < best_score:
                best_score = score
                best_box = candidate

    if best_box is None:
        raise RuntimeError(f"No valid patch found for box {target}")
    print(f"box [{target.x},{target.y},{target.width},{target.height}] <- patch [{best_box.x},{best_box.y}] score={best_score:.2f}")
    return best_box


def feather_mask(height: int, width: int, feather: int) -> np.ndarray:
    y = np.arange(height, dtype=np.float32)
    x = np.arange(width, dtype=np.float32)
    dist_top = y[:, None]
    dist_bottom = (height - 1 - y)[:, None]
    dist_left = x[None, :]
    dist_right = (width - 1 - x)[None, :]
    dist = np.minimum(np.minimum(dist_top, dist_bottom), np.minimum(dist_left, dist_right))
    mask = np.clip(dist / max(1, feather), 0.0, 1.0)
    return mask[:, :, None]


def infill(image_path: Path, boxes: list[Box], stride: int, border: int, feather: int, local_radius: int) -> None:
    image = Image.open(image_path).convert("RGB")
    arr = np.asarray(image).copy()
    original = arr.copy()

    for box in boxes:
        patch_box = find_best_patch(original, box, boxes, stride, border, local_radius)
        patch = original[patch_box.y : patch_box.bottom, patch_box.x : patch_box.right].astype(np.float32)
        current = arr[box.y : box.bottom, box.x : box.right].astype(np.float32)
        mask = feather_mask(box.height, box.width, feather)
        blended = (patch * mask) + (current * (1.0 - mask))
        arr[box.y : box.bottom, box.x : box.right] = np.clip(blended, 0, 255).astype(np.uint8)

    temp_path = image_path.with_name(image_path.name + ".tmp" + image_path.suffix)
    try:
        Image.fromarray(arr, mode="RGB").save(temp_path)
        os.replace(temp_path, image_path)
    finally:
        if temp_path.exists():
            temp_path.unlink()


def main() -> int:
    parser = argparse.ArgumentParser(description="Infill scrubbed atlas boxes with source-local texture patches.")
    parser.add_argument("--image", required=True)
    parser.add_argument("--config", required=True)
    parser.add_argument("--stride", type=int, default=4)
    parser.add_argument("--border", type=int, default=8)
    parser.add_argument("--feather", type=int, default=12)
    parser.add_argument("--local-radius", type=int, default=360)
    args = parser.parse_args()

    infill(
        Path(args.image).resolve(),
        load_boxes(Path(args.config).resolve()),
        max(1, args.stride),
        max(1, args.border),
        max(1, args.feather),
        max(64, args.local_radius),
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

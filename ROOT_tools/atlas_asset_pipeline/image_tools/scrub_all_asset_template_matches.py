#!/usr/bin/env python3
"""
Template-driven all-asset visual scrubber.

Scans every image under the configured source asset roots, finds source-template
matches, and mechanically edits the matched regions in place. This is intended
for broad legal-art cleanup where the same prohibited motif appears across many
source atlases. Packaged client/launcher assets are deliberately not part of the
default scan; regenerate those from cleaned source later.

Dependencies: Pillow and numpy.
"""

from __future__ import annotations

import argparse
import json
import logging
import os
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter


IMAGE_EXTENSIONS = {".png", ".jpg", ".jpeg", ".bmp", ".tif", ".tiff", ".webp"}
DEFAULT_ROOTS = [
    "ROOT_SRC_assets",
]


@dataclass(frozen=True)
class TemplateRule:
    path: Path
    threshold: float
    padding: int
    mode: str
    pixel_size: int
    blur_radius: float
    scales: tuple[float, ...]
    flip_horizontal: bool


@dataclass(frozen=True)
class Match:
    score: float
    left: int
    top: int
    right: int
    bottom: int
    template_name: str
    variant: str


def configure_logging(verbose: bool) -> None:
    logging.basicConfig(level=logging.DEBUG if verbose else logging.INFO, format="%(levelname)s: %(message)s")


def load_rules(config_path: Path) -> list[TemplateRule]:
    with config_path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    rules = []
    base = config_path.parent
    for entry in data.get("templates", []):
        rules.append(
            TemplateRule(
                path=(base / entry["path"]).resolve(),
                threshold=float(entry.get("threshold", data.get("threshold", 0.72))),
                padding=int(entry.get("padding", data.get("padding", 4))),
                mode=str(entry.get("mode", data.get("mode", "pixelate"))).lower(),
                pixel_size=int(entry.get("pixel_size", data.get("pixel_size", 12))),
                blur_radius=float(entry.get("blur_radius", data.get("blur_radius", 8))),
                scales=tuple(float(item) for item in entry.get("scales", data.get("scales", [1.0]))),
                flip_horizontal=bool(entry.get("flip_horizontal", data.get("flip_horizontal", True))),
            )
        )
    if not rules:
        raise ValueError("Config must contain at least one template rule")
    return rules


def image_to_gray_array(image: Image.Image) -> np.ndarray:
    return np.asarray(image.convert("L"), dtype=np.float32) / 255.0


def fft_correlate_valid(image: np.ndarray, template_zero_mean: np.ndarray) -> np.ndarray:
    ih, iw = image.shape
    th, tw = template_zero_mean.shape
    pad_shape = (ih + th - 1, iw + tw - 1)
    rotated = template_zero_mean[::-1, ::-1]
    corr = np.fft.irfft2(
        np.fft.rfft2(image, pad_shape) * np.fft.rfft2(rotated, pad_shape),
        pad_shape,
    )
    return corr[th - 1 : ih, tw - 1 : iw]


def window_sums(image: np.ndarray, height: int, width: int) -> tuple[np.ndarray, np.ndarray]:
    integral = np.pad(image, ((1, 0), (1, 0)), mode="constant").cumsum(axis=0).cumsum(axis=1)
    integral_sq = np.pad(image * image, ((1, 0), (1, 0)), mode="constant").cumsum(axis=0).cumsum(axis=1)

    sums = integral[height:, width:] - integral[:-height, width:] - integral[height:, :-width] + integral[:-height, :-width]
    sums_sq = (
        integral_sq[height:, width:]
        - integral_sq[:-height, width:]
        - integral_sq[height:, :-width]
        + integral_sq[:-height, :-width]
    )
    return sums, sums_sq


def similarity_match(image: np.ndarray, template: np.ndarray) -> np.ndarray:
    th, tw = template.shape
    sums, sums_sq = window_sums(image, th, tw)
    area = float(th * tw)
    template_zero_mean = template - float(template.mean())
    template_energy = float((template_zero_mean * template_zero_mean).sum())
    if template_energy <= 1.0e-9:
        return np.zeros((image.shape[0] - th + 1, image.shape[1] - tw + 1), dtype=np.float32)

    cross = fft_correlate_valid(image, template_zero_mean)
    window_energy = sums_sq - ((sums * sums) / area)
    usable = window_energy > (area * 0.0004)
    scores = np.zeros_like(cross, dtype=np.float32)
    scores[usable] = cross[usable] / np.sqrt(window_energy[usable] * template_energy)
    return np.clip(scores, -1.0, 1.0)


def overlaps(a: Match, b: Match) -> bool:
    return not (a.right <= b.left or b.right <= a.left or a.bottom <= b.top or b.bottom <= a.top)


def template_variants(template_image: Image.Image, rule: TemplateRule):
    seen: set[tuple[int, int, str]] = set()
    for scale in rule.scales:
        width = max(4, int(round(template_image.width * scale)))
        height = max(4, int(round(template_image.height * scale)))
        if width < 4 or height < 4:
            continue
        scaled = template_image.resize((width, height), Image.Resampling.BICUBIC)
        variants = [("scale=%.3g" % scale, scaled)]
        if rule.flip_horizontal:
            variants.append(("scale=%.3g,flip=h" % scale, scaled.transpose(Image.Transpose.FLIP_LEFT_RIGHT)))
        for name, variant in variants:
            key = (variant.width, variant.height, name)
            if key in seen:
                continue
            seen.add(key)
            yield name, variant


def find_matches(image: Image.Image, rule: TemplateRule) -> list[Match]:
    template_image = Image.open(rule.path).convert("RGBA")
    source_gray = image_to_gray_array(image)

    candidates = []
    for variant_name, variant_image in template_variants(template_image, rule):
        if variant_image.width > image.width or variant_image.height > image.height:
            continue
        template_gray = image_to_gray_array(variant_image)
        scores = similarity_match(source_gray, template_gray)

        ys, xs = np.where(scores >= rule.threshold)
        candidates.extend(
            Match(
                score=float(scores[y, x]),
                left=max(0, int(x) - rule.padding),
                top=max(0, int(y) - rule.padding),
                right=min(image.width, int(x) + variant_image.width + rule.padding),
                bottom=min(image.height, int(y) + variant_image.height + rule.padding),
                template_name=rule.path.name,
                variant=variant_name,
            )
            for y, x in zip(ys, xs)
        )
    candidates.sort(key=lambda item: item.score, reverse=True)

    kept: list[Match] = []
    for candidate in candidates:
        if not any(overlaps(candidate, existing) for existing in kept):
            kept.append(candidate)
    return kept


def pixelate(region: Image.Image, pixel_size: int) -> Image.Image:
    width, height = region.size
    small = region.resize((max(1, width // pixel_size), max(1, height // pixel_size)), Image.Resampling.BILINEAR)
    return small.resize(region.size, Image.Resampling.NEAREST)


def edge_infill(image: Image.Image, match: Match) -> Image.Image:
    width = match.right - match.left
    height = match.bottom - match.top
    replacement = Image.new(image.mode, (width, height))
    pixels = replacement.load()
    source = image.load()

    for y in range(height):
        global_y = match.top + y
        for x in range(width):
            global_x = match.left + x
            samples = []
            if match.left > 0:
                samples.append(source[match.left - 1, global_y])
            if match.right < image.width:
                samples.append(source[match.right, global_y])
            if match.top > 0:
                samples.append(source[global_x, match.top - 1])
            if match.bottom < image.height:
                samples.append(source[global_x, match.bottom])
            if not samples:
                samples.append(source[global_x, global_y])
            weight_left = (width - x) / max(width, 1)
            weight_top = (height - y) / max(height, 1)
            weighted = []
            if match.left > 0:
                weighted.append((weight_left, source[match.left - 1, global_y]))
            if match.right < image.width:
                weighted.append((1.0 - weight_left, source[match.right, global_y]))
            if match.top > 0:
                weighted.append((weight_top, source[global_x, match.top - 1]))
            if match.bottom < image.height:
                weighted.append((1.0 - weight_top, source[global_x, match.bottom]))
            total_weight = sum(weight for weight, _ in weighted) or 1.0
            channels = len(samples[0]) if isinstance(samples[0], tuple) else 1
            if channels == 1:
                pixels[x, y] = int(sum(weight * value for weight, value in weighted) / total_weight)
            else:
                pixels[x, y] = tuple(
                    int(sum(weight * value[channel] for weight, value in weighted) / total_weight)
                    for channel in range(channels)
                )
    return replacement.filter(ImageFilter.GaussianBlur(radius=0.4))


def edit_match(image: Image.Image, match: Match, mode: str, pixel_size: int, blur_radius: float) -> None:
    box = (match.left, match.top, match.right, match.bottom)
    region = image.crop(box)
    if mode == "blur":
        replacement = region.filter(ImageFilter.GaussianBlur(radius=blur_radius))
    elif mode == "solid":
        replacement = Image.new(image.mode, region.size, (0, 0, 0, 0) if image.mode == "RGBA" else (20, 20, 20))
    elif mode == "edge_infill":
        replacement = edge_infill(image, match)
    else:
        replacement = pixelate(region, pixel_size)
    image.paste(replacement, box)


def save_safely(image: Image.Image, path: Path) -> None:
    temp_path = path.with_name(path.name + ".tmp" + path.suffix)
    try:
        image.save(temp_path)
        os.replace(temp_path, path)
    finally:
        if temp_path.exists():
            temp_path.unlink()


def iter_images(root: Path):
    for path in sorted(root.rglob("*")):
        if path.is_file() and path.suffix.lower() in IMAGE_EXTENSIONS:
            yield path


def process(config_path: Path, roots: list[Path], dry_run: bool) -> int:
    rules = load_rules(config_path)
    total = 0
    image_paths: list[Path] = []
    for root in roots:
        if not root.exists():
            logging.warning("Asset root missing: %s", root)
            continue
        image_paths.extend(iter_images(root))

    logging.info("Scanning %d source image(s) with %d template rule(s).", len(image_paths), len(rules))
    for index, path in enumerate(image_paths, start=1):
            logging.info("Scanning [%d/%d] %s", index, len(image_paths), path)
            with Image.open(path) as opened:
                image = opened.convert("RGBA") if opened.mode in {"P", "LA"} else opened.convert(opened.mode)
                matches: list[tuple[TemplateRule, Match]] = []
                for rule in rules:
                    matches.extend((rule, match) for match in find_matches(image, rule))
                if not matches:
                    continue
                matches.sort(key=lambda item: item[1].score, reverse=True)
                logging.info("%s: %d match(es)", path, len(matches))
                for rule, match in matches:
                    logging.info(
                        "  %s score=%.3f box=[%d,%d,%d,%d] mode=%s",
                        f"{match.template_name} {match.variant}",
                        match.score,
                        match.left,
                        match.top,
                        match.right - match.left,
                        match.bottom - match.top,
                        rule.mode,
                    )
                    if not dry_run:
                        edit_match(image, match, rule.mode, rule.pixel_size, rule.blur_radius)
                        total += 1
                if not dry_run:
                    save_safely(image, path)
    return total


def main() -> int:
    parser = argparse.ArgumentParser(description="Template-match and scrub prohibited motifs across source asset roots.")
    parser.add_argument("--config", default="ROOT_tools/atlas_asset_pipeline/configs/all_asset_template_scrub_config.json")
    parser.add_argument("--root", action="append", default=None, help="Asset root to scan. Can be repeated.")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()
    configure_logging(args.verbose)

    roots = [Path(root).resolve() for root in (args.root or DEFAULT_ROOTS)]
    edited = process(Path(args.config).resolve(), roots, args.dry_run)
    logging.info("Done. Edited %d matched region(s).", edited)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

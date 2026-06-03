#!/usr/bin/env python3
"""
Build a content/tag index for compiled atlas tiles.

The indexer intentionally works from one high-fidelity resolution package
instead of re-indexing every scaled duplicate. The resulting asset IDs are
resolution-neutral enough for downstream tools to map the same tile back to
other output sizes.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from PIL import Image, ImageFilter, ImageStat


PIPELINE_ROOT = Path(__file__).resolve().parent
DEFAULT_COMPILED_ROOT = PIPELINE_ROOT / "compiled_assets"
DEFAULT_DESCRIPTOR_CONFIG = PIPELINE_ROOT / "configs/deep_asset_descriptors.json"
CELL_RE = re.compile(r"^(?P<atlas>.+)_r(?P<row>\d+)c(?P<col>\d+)_(?P<size>\d+)px$")
TOKEN_RE = re.compile(r"[A-Za-z0-9]+")
STOP_TOKENS = {
    "a",
    "and",
    "asset",
    "assets",
    "c",
    "new",
    "o",
    "one",
    "r",
    "rgba",
    "the",
}
TOKEN_ALIASES = {
    "protraits": "portraits",
}
CATEGORY_ALIASES = {
    "emergency_machines_1x1": "emergency_machines",
    "protraits": "portraits",
}


CATEGORY_TAGS: dict[str, list[str]] = {
    "bulkhead": ["architecture", "bulkhead", "wall", "door", "structure"],
    "corridors": ["architecture", "corridor", "floor", "walkway", "interior"],
    "defenses": ["defense", "fortification", "military", "equipment"],
    "doors": ["architecture", "door", "entry", "portal"],
    "emergency_machines": ["machine", "emergency", "equipment", "industrial"],
    "faction": ["faction", "symbol", "banner", "identity"],
    "floors": ["architecture", "floor", "ground", "surface"],
    "implants": ["implant", "medical", "body", "component"],
    "items": ["item", "object", "pickup", "inventory"],
    "knowledges": ["knowledge", "icon", "skill", "interface"],
    "objects": ["object", "prop", "set_dressing"],
    "protraits": ["portrait", "character", "face"],
    "portraits": ["portrait", "character", "face"],
    "roads": ["road", "street", "vehicle_path", "surface"],
    "system": ["system", "interface", "icon", "ui"],
    "vehicles": ["vehicle", "transport", "machine"],
    "void_tiles": ["space", "void", "environment", "background"],
    "walls": ["architecture", "wall", "structure", "barrier"],
}


KEYWORD_TAGS: dict[str, list[str]] = {
    "alcohol": ["bottle", "consumable", "valuable"],
    "alcohols": ["bottle", "consumable", "valuable"],
    "algae": ["tank", "biotech", "industrial"],
    "animal": ["animal", "pen", "livestock"],
    "armors": ["armor", "equipment", "wearable"],
    "automotive": ["vehicle_part", "machine_part", "mechanical"],
    "automata": ["automata", "servitor", "machine", "character"],
    "banner": ["banner", "faction", "symbol"],
    "barstuff": ["bar", "furniture", "prop"],
    "beasts": ["animal", "creature"],
    "bulkhead": ["bulkhead", "wall", "industrial"],
    "chemical": ["chemical", "reagent", "industrial"],
    "chem": ["chemical", "lab"],
    "children": ["child", "creche", "civilian"],
    "clerics": ["clergy", "character", "portrait"],
    "clerks": ["clerk", "character", "portrait"],
    "cloning": ["cloning", "vat", "biotech"],
    "component": ["component", "part"],
    "components": ["component", "part"],
    "corridor": ["corridor", "interior"],
    "counters": ["counter", "shop", "furniture"],
    "creatures": ["creature", "character"],
    "creche": ["creche", "child", "civilian"],
    "cult": ["cult", "faction"],
    "cultists": ["cultist", "character"],
    "darkcity": ["city", "urban", "dark"],
    "decor": ["decor", "prop", "set_dressing"],
    "defense": ["defense", "fortification"],
    "defenses": ["defense", "fortification"],
    "designator": ["signage", "label", "sector"],
    "devices": ["device", "equipment"],
    "domestic": ["domestic", "furniture", "civilian"],
    "door": ["door", "entry"],
    "doors": ["door", "entry"],
    "drugs": ["drug", "medical", "consumable"],
    "elevator": ["elevator", "platform", "architecture"],
    "enforcer": ["security", "law", "military"],
    "enforcement": ["security", "law"],
    "farm": ["farm", "agriculture"],
    "faction": ["faction", "symbol"],
    "factory": ["factory", "industrial"],
    "fake": ["fake", "prop"],
    "flatgrid": ["grid", "floor", "surface"],
    "flesh": ["biotech", "organic", "cult"],
    "floor": ["floor", "surface"],
    "floors": ["floor", "surface"],
    "frameless": ["frameless", "ui", "icon"],
    "framed": ["framed", "ui", "icon"],
    "gangers": ["ganger", "character"],
    "gang": ["gang", "faction"],
    "generic": ["generic", "item"],
    "goods": ["goods", "cargo", "item"],
    "hatches": ["hatch", "entry"],
    "heretics": ["heretic", "character"],
    "hiver": ["hiver", "clothing", "character"],
    "hospital": ["hospital", "medical"],
    "humans8x8": ["human", "character", "portrait"],
    "industrial": ["industrial", "machine"],
    "infrastructure": ["infrastructure", "road"],
    "interwall": ["wall", "interior"],
    "journals": ["journal", "book", "document"],
    "junk": ["junk", "scrap", "item"],
    "knowledge": ["knowledge", "skill"],
    "labschem": ["lab", "chemical"],
    "ladders": ["ladder", "access"],
    "law": ["law", "security"],
    "lightcity": ["city", "urban", "bright"],
    "loot": ["loot", "item"],
    "lost": ["relic", "artifact"],
    "machine": ["machine", "equipment"],
    "machinery": ["machine", "industrial", "equipment"],
    "medicae": ["medical", "hospital"],
    "medicaie": ["medical", "hospital"],
    "medical": ["medical", "hospital"],
    "military": ["military", "weapon", "equipment"],
    "modifications": ["modification", "implant"],
    "multi": ["multi_tile"],
    "mutants": ["mutant", "character"],
    "narcotics": ["narcotic", "drug", "consumable"],
    "noble": ["noble", "faction", "defense"],
    "nobles": ["noble", "character", "portrait"],
    "newspaper": ["newspaper", "paper", "document"],
    "parks": ["park", "outdoor", "prop"],
    "paperwork": ["paper", "document", "office"],
    "pens": ["pen", "animal"],
    "pets": ["pet", "animal"],
    "platforms": ["platform", "architecture"],
    "posh": ["posh", "noble", "floor"],
    "precinct": ["precinct", "security", "law"],
    "profiles": ["profile", "portrait", "character"],
    "quality": ["quality_tier", "equipment"],
    "reagent": ["chemical", "reagent"],
    "reagents": ["chemical", "reagent"],
    "relic": ["relic", "artifact"],
    "relics": ["relic", "artifact"],
    "road": ["road", "street"],
    "rondels": ["rondel", "icon", "skill"],
    "schola": ["school", "child", "civilian"],
    "scrappy": ["scrap", "rough", "item"],
    "sector": ["sector", "signage"],
    "sealed": ["sealed", "container"],
    "servants": ["servant", "character"],
    "servitors": ["servitor", "character", "machine"],
    "sewer": ["sewer", "industrial", "floor"],
    "sewerwalls": ["sewer", "wall"],
    "shops": ["shop", "counter", "furniture"],
    "skill": ["skill", "icon"],
    "skills": ["skill", "icon"],
    "stairs": ["stairs", "access", "architecture"],
    "system": ["system", "ui"],
    "tables": ["table", "furniture"],
    "tank": ["tank", "container"],
    "temple": ["temple", "religious", "architecture"],
    "templewalls": ["temple", "wall", "religious"],
    "tier": ["quality_tier", "equipment"],
    "train": ["train", "rail", "vehicle"],
    "trainstuff": ["train", "rail", "vehicle"],
    "undyinglords": ["undying_lords", "faction", "character"],
    "valuables": ["valuable", "loot"],
    "vats": ["vat", "biotech"],
    "vehicle": ["vehicle", "transport"],
    "vehicles": ["vehicle", "transport"],
    "vending": ["vending", "machine", "shop"],
    "void": ["void", "space"],
    "wall": ["wall", "architecture"],
    "walls": ["wall", "architecture"],
    "weapons": ["weapon", "equipment"],
}


@dataclass(frozen=True)
class ParsedName:
    source_atlas: str
    row: int
    col: int
    size: int


def slug(value: str) -> str:
    clean = re.sub(r"[^A-Za-z0-9]+", "_", value.strip().lower()).strip("_")
    return clean or "unknown"


def normalize_token(value: str) -> str:
    clean = slug(value)
    return TOKEN_ALIASES.get(clean, clean)


def normalize_category(value: str) -> str:
    clean = slug(value)
    return CATEGORY_ALIASES.get(clean, clean)


def dedupe(values: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for value in values:
        item = slug(value)
        if item and item not in seen:
            seen.add(item)
            result.append(item)
    return result


def parse_name(path: Path) -> ParsedName:
    match = CELL_RE.match(path.stem)
    if not match:
        return ParsedName(path.stem, 0, 0, 0)
    return ParsedName(
        source_atlas=match.group("atlas"),
        row=int(match.group("row")),
        col=int(match.group("col")),
        size=int(match.group("size")),
    )


def name_tags(group: str, atlas_name: str) -> tuple[list[str], list[str]]:
    text = f"{group} {atlas_name}"
    tokens = []
    for token in TOKEN_RE.findall(text):
        clean = normalize_token(token)
        if clean in STOP_TOKENS or clean.isdigit() or len(clean) < 2:
            continue
        tokens.append(clean)
    tags: list[str] = []
    for token in tokens:
        tags.append(token)
        if token in KEYWORD_TAGS:
            tags.extend(KEYWORD_TAGS[token])

    group_key = normalize_category(group)
    if group_key in CATEGORY_TAGS:
        tags.extend(CATEGORY_TAGS[group_key])
    return dedupe(tokens), dedupe(tags)


def dominant_color_tag(rgb: tuple[float, float, float]) -> str:
    r, g, b = rgb
    if max(rgb) < 35:
        return "color_black"
    if max(rgb) - min(rgb) < 18:
        if max(rgb) > 190:
            return "color_light_gray"
        if max(rgb) < 85:
            return "color_dark_gray"
        return "color_gray"
    if r > g * 1.25 and r > b * 1.25:
        return "color_red"
    if g > r * 1.18 and g > b * 1.15:
        return "color_green"
    if b > r * 1.18 and b > g * 1.15:
        return "color_blue"
    if r > 120 and g > 95 and b < 95:
        return "color_brown_or_gold"
    return "color_mixed"


def visual_tags(path: Path) -> tuple[list[str], dict[str, Any]]:
    with Image.open(path) as image:
        rgba = image.convert("RGBA")
        width, height = rgba.size
        alpha = rgba.getchannel("A")
        alpha_stat = ImageStat.Stat(alpha)
        alpha_mean = alpha_stat.mean[0]
        visible_ratio = alpha_mean / 255.0

        opaque = Image.new("RGBA", rgba.size, (0, 0, 0, 255))
        opaque.alpha_composite(rgba)
        rgb = opaque.convert("RGB")
        stat = ImageStat.Stat(rgb)
        mean_rgb = tuple(round(v, 2) for v in stat.mean)
        gray = rgb.convert("L")
        brightness = ImageStat.Stat(gray).mean[0]
        edges = gray.filter(ImageFilter.FIND_EDGES)
        edge_density = ImageStat.Stat(edges).mean[0] / 255.0

    tags = [dominant_color_tag(mean_rgb)]
    if visible_ratio < 0.03:
        tags.append("mostly_empty")
    elif visible_ratio < 0.35:
        tags.append("sparse")
    else:
        tags.append("filled")

    if brightness < 55:
        tags.append("dark")
    elif brightness > 180:
        tags.append("bright")
    else:
        tags.append("mid_value")

    if edge_density > 0.18:
        tags.append("high_detail")
    elif edge_density < 0.055:
        tags.append("low_detail")
    else:
        tags.append("medium_detail")

    traits = {
        "width": width,
        "height": height,
        "visible_ratio": round(visible_ratio, 4),
        "brightness": round(brightness, 2),
        "edge_density": round(edge_density, 4),
        "mean_rgb": mean_rgb,
    }
    return dedupe(tags), traits


def load_descriptor_config(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {"atlas_rules": []}
    return json.loads(path.read_text(encoding="utf-8"))


def descriptor_for(config: dict[str, Any], group: str, atlas_name: str, row: int, col: int) -> dict[str, Any]:
    result: dict[str, Any] = {
        "description": "",
        "descriptor_tags": [],
        "content_type": "",
        "descriptor_source": "derived",
        "replace_derived_tags": False,
    }
    cell_key = f"r{row:02d}c{col:02d}"
    atlas_key = slug(atlas_name)
    group_key = normalize_category(group)

    for rule in config.get("atlas_rules", []):
        match = rule.get("match", "")
        rule_group = rule.get("group")
        if rule_group and normalize_category(str(rule_group)) != group_key:
            continue
        if match and slug(match) not in atlas_key:
            continue

        result["descriptor_tags"].extend(rule.get("tags", []))
        if rule.get("content_type"):
            result["content_type"] = rule["content_type"]
        if rule.get("description") and not result["description"]:
            result["description"] = rule["description"]
            result["descriptor_source"] = "atlas_rule"
        if rule.get("replace_derived_tags"):
            result["replace_derived_tags"] = True

        row_rules = rule.get("row_rules", {})
        row_key = f"r{row:02d}"
        if row_key in row_rules:
            row_rule = row_rules[row_key]
            result["descriptor_tags"].extend(row_rule.get("tags", []))
            if row_rule.get("content_type"):
                result["content_type"] = row_rule["content_type"]
            if row_rule.get("description"):
                result["description"] = row_rule["description"]
                result["descriptor_source"] = "row_rule"

        cell_rules = rule.get("cells", {})
        if cell_key in cell_rules:
            cell_rule = cell_rules[cell_key]
            result["descriptor_tags"].extend(cell_rule.get("tags", []))
            if cell_rule.get("content_type"):
                result["content_type"] = cell_rule["content_type"]
            if cell_rule.get("description"):
                result["description"] = cell_rule["description"]
            result["descriptor_source"] = "cell_rule"

    result["descriptor_tags"] = dedupe(result["descriptor_tags"])
    return result


def build_index(compiled_root: Path, size: int, descriptor_config: dict[str, Any]) -> dict[str, Any]:
    size_root = compiled_root / f"{size}px"
    if not size_root.exists():
        raise FileNotFoundError(f"Missing compiled size folder: {size_root}")

    assets: list[dict[str, Any]] = []
    tag_counts: Counter[str] = Counter()
    category_counts: Counter[str] = Counter()
    atlas_counts: Counter[str] = Counter()
    group_counts: Counter[str] = Counter()

    for path in sorted(size_root.rglob("*.png")):
        rel = path.relative_to(compiled_root).as_posix()
        rel_to_size = path.relative_to(size_root)
        group = rel_to_size.parts[0] if len(rel_to_size.parts) > 1 else "ROOT"
        parsed = parse_name(path)
        tokens, derived_tags = name_tags(group, parsed.source_atlas)
        image_tags, traits = visual_tags(path)
        category = normalize_category(group)
        descriptor = descriptor_for(descriptor_config, group, parsed.source_atlas, parsed.row, parsed.col)
        base_tags = [] if descriptor["replace_derived_tags"] else derived_tags
        all_tags = dedupe([category, *base_tags, *descriptor["descriptor_tags"], *image_tags])
        asset_id = slug(f"{group}_{parsed.source_atlas}_r{parsed.row:02d}c{parsed.col:02d}")

        record = {
            "asset_id": asset_id,
            "path": rel,
            "resolution": size,
            "source_group": group,
            "source_atlas": parsed.source_atlas,
            "cell": {"row": parsed.row, "col": parsed.col},
            "category": category,
            "name_tokens": tokens,
            "description": descriptor["description"],
            "content_type": descriptor["content_type"],
            "content_tags": all_tags,
            "descriptor_source": descriptor["descriptor_source"],
            "visual_traits": traits,
        }
        assets.append(record)
        tag_counts.update(all_tags)
        category_counts.update([category])
        atlas_counts.update([parsed.source_atlas])
        group_counts.update([group])

    return {
        "schema": "mechanist.asset_content_index.v1",
        "resolution": size,
        "asset_count": len(assets),
        "source_root": str(size_root),
        "descriptor_config": str(DEFAULT_DESCRIPTOR_CONFIG),
        "summary": {
            "groups": dict(sorted(group_counts.items())),
            "categories": dict(sorted(category_counts.items())),
            "top_tags": dict(tag_counts.most_common(80)),
            "atlas_count": len(atlas_counts),
        },
        "assets": assets,
    }


def write_tsv(index: dict[str, Any], path: Path) -> None:
    fieldnames = [
        "asset_id",
        "path",
        "resolution",
        "source_group",
        "source_atlas",
        "row",
        "col",
        "category",
        "content_type",
        "description",
        "descriptor_source",
        "content_tags",
        "brightness",
        "edge_density",
        "visible_ratio",
    ]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, delimiter="\t")
        writer.writeheader()
        for asset in index["assets"]:
            traits = asset["visual_traits"]
            writer.writerow(
                {
                    "asset_id": asset["asset_id"],
                    "path": asset["path"],
                    "resolution": asset["resolution"],
                    "source_group": asset["source_group"],
                    "source_atlas": asset["source_atlas"],
                    "row": asset["cell"]["row"],
                    "col": asset["cell"]["col"],
                    "category": asset["category"],
                    "content_type": asset["content_type"],
                    "description": asset["description"],
                    "descriptor_source": asset["descriptor_source"],
                    "content_tags": ",".join(asset["content_tags"]),
                    "brightness": traits["brightness"],
                    "edge_density": traits["edge_density"],
                    "visible_ratio": traits["visible_ratio"],
                }
            )


def update_compile_manifest(compiled_root: Path, index_path: Path, tsv_path: Path, index: dict[str, Any]) -> None:
    manifest_path = compiled_root / "asset_compile_manifest.json"
    manifest: dict[str, Any] = {}
    if manifest_path.exists():
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

    manifest["content_index"] = {
        "schema": index["schema"],
        "resolution": index["resolution"],
        "asset_count": index["asset_count"],
        "json": index_path.relative_to(compiled_root).as_posix(),
        "tsv": tsv_path.relative_to(compiled_root).as_posix(),
        "tag_summary": index["summary"]["top_tags"],
        "group_summary": index["summary"]["groups"],
    }
    manifest_path.write_text(json.dumps(manifest, indent=2), encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Build a semantic content index for compiled atlas tiles.")
    parser.add_argument("--compiled-root", default=str(DEFAULT_COMPILED_ROOT), help="Root containing compiled resolution folders.")
    parser.add_argument("--size", type=int, default=256, help="Compiled resolution to index.")
    parser.add_argument("--descriptor-config", default=str(DEFAULT_DESCRIPTOR_CONFIG), help="JSON descriptor rules and per-cell tags.")
    parser.add_argument("--output-json", default=None, help="Output JSON path. Defaults inside compiled root.")
    parser.add_argument("--output-tsv", default=None, help="Output TSV path. Defaults inside compiled root.")
    parser.add_argument("--no-manifest-update", action="store_true", help="Do not register the index in asset_compile_manifest.json.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    compiled_root = Path(args.compiled_root).resolve()
    descriptor_config = load_descriptor_config(Path(args.descriptor_config).resolve())
    index = build_index(compiled_root, args.size, descriptor_config)
    index["descriptor_config"] = str(Path(args.descriptor_config).resolve())

    index_path = Path(args.output_json).resolve() if args.output_json else compiled_root / f"asset_content_index_{args.size}px.json"
    tsv_path = Path(args.output_tsv).resolve() if args.output_tsv else compiled_root / f"asset_content_index_{args.size}px.tsv"
    index_path.write_text(json.dumps(index, indent=2), encoding="utf-8")
    write_tsv(index, tsv_path)

    if not args.no_manifest_update:
        update_compile_manifest(compiled_root, index_path, tsv_path, index)

    print(f"Indexed {index['asset_count']} asset(s) from {compiled_root / f'{args.size}px'}")
    print(f"Wrote {index_path}")
    print(f"Wrote {tsv_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

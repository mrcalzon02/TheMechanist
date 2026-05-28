#!/usr/bin/env python3
"""
Build image-tool box configs from atlas review cell selections.

The review HTML exports selected cell ids and their source/content boxes. This
converter turns those selections into bounded operation configs so the image
tools can target dynamic cells without hand-copying coordinates.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


PIPELINE_ROOT = Path(__file__).resolve().parent
DEFAULT_REVIEW = PIPELINE_ROOT / "atlas_slice_preview/atlas_slice_review_tags.json"
DEFAULT_OUTPUT = PIPELINE_ROOT / "configs/cell_tool_targets.generated.json"


def load_records(path: Path) -> list[dict]:
    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    if not isinstance(data, list):
        raise ValueError("Review export must be a JSON list")
    return data


def main() -> int:
    parser = argparse.ArgumentParser(description="Convert selected review cells into image-tool box config.")
    parser.add_argument("--review-tags", default=str(DEFAULT_REVIEW), help="Review JSON exported from atlas_grid_preview_index.html.")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT), help="Output JSON config path.")
    parser.add_argument("--box", choices=["source", "content"], default="content", help="Use full source cell boxes or trimmed content boxes.")
    parser.add_argument("--mode", default="pixelate", help="Default censor-boxes operation mode.")
    parser.add_argument("--pixel-size", type=int, default=16)
    parser.add_argument("--blur-kernel", type=int, default=51)
    args = parser.parse_args()

    records = load_records(Path(args.review_tags).resolve())
    config = {
        "defaults": {
            "mode": args.mode,
            "pixel_size": args.pixel_size,
            "blur_kernel": args.blur_kernel,
            "color": [255, 0, 255, 255],
            "clamp_strategy": "mirror",
        },
        "atlases": {},
    }
    for record in records:
        source = record.get("source")
        if not source:
            continue
        boxes = []
        for cell in record.get("selected_cell_boxes", []):
            box = cell.get(f"{args.box}_box")
            if not box:
                continue
            boxes.append(
                {
                    "box": box,
                    "mode": record.get("tool_action") or args.mode,
                    "cell": cell.get("id"),
                    "source_cell_box": cell.get("source_box"),
                    "content_cell_box": cell.get("content_box"),
                }
            )
        if boxes:
            config["atlases"].setdefault(source, []).extend(boxes)

    output = Path(args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(config, indent=2), encoding="utf-8")
    print(f"Wrote {sum(len(v) for v in config['atlases'].values())} cell target box(es) to {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

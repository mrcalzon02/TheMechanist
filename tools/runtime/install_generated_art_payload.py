#!/usr/bin/env python3
"""
Install or validate generated art payloads for The Mechanist 0.9.10kc rebase.

This accepts either of the two supported payload layouts:
  1. Canonical: <source>/assets/graphics/generated/<tier>/<sheet>/<file>.png
  2. Onboarding export: <source>/exports/<tier>/<sheet>/<file>.png

Examples:
  python tools/runtime/install_generated_art_payload.py --project-root . --source /path/to/mech_phase_b3_remaining_uniform_approval_0.9.10kc --mode validate
  python tools/runtime/install_generated_art_payload.py --project-root . --source /path/to/mech_phase_b3_remaining_uniform_approval_0.9.10kc --mode copy --tiers standard_64,intermediate_128
"""
from __future__ import annotations
import argparse
import csv
import shutil
from dataclasses import dataclass
from pathlib import Path

TIERS = ("high_native", "intermediate_128", "standard_64", "low_32")

@dataclass(frozen=True)
class PayloadFile:
    tier: str
    sheet: str
    source: Path
    canonical_relative: Path


def discover_payload_files(source: Path, requested_tiers: set[str]) -> list[PayloadFile]:
    roots = []
    canonical = source / "assets" / "graphics" / "generated"
    exports = source / "exports"
    if canonical.is_dir():
        roots.append((canonical, "canonical"))
    if exports.is_dir():
        roots.append((exports, "exports"))
    if not roots:
        raise SystemExit(f"No supported generated-art payload layout found under {source}")

    out: list[PayloadFile] = []
    seen: set[Path] = set()
    for root, _kind in roots:
        for tier in TIERS:
            if tier not in requested_tiers:
                continue
            tier_root = root / tier
            if not tier_root.is_dir():
                continue
            for png in sorted(tier_root.rglob("*.png")):
                if not png.is_file():
                    continue
                rel_after_tier = png.relative_to(tier_root)
                if len(rel_after_tier.parts) < 2:
                    continue
                sheet = rel_after_tier.parts[0]
                canonical_relative = Path("assets") / "graphics" / "generated" / tier / rel_after_tier
                if canonical_relative in seen:
                    continue
                seen.add(canonical_relative)
                out.append(PayloadFile(tier=tier, sheet=sheet, source=png, canonical_relative=canonical_relative))
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--project-root", required=True, type=Path)
    ap.add_argument("--source", required=True, type=Path)
    ap.add_argument("--mode", choices=("copy", "validate"), default="validate")
    ap.add_argument("--tiers", default="standard_64,intermediate_128,high_native",
                    help="Comma-separated generated tiers. Defaults to higher tiers only; low_32 is already bundled in the lean project.")
    ap.add_argument("--report", type=Path, default=None)
    args = ap.parse_args()

    project_root = args.project_root.resolve()
    source = args.source.resolve()
    requested = {t.strip() for t in args.tiers.split(",") if t.strip()}
    bad = requested.difference(TIERS)
    if bad:
        raise SystemExit(f"Unknown tier(s): {sorted(bad)}")

    payload = discover_payload_files(source, requested)
    rows = []
    for item in payload:
        destination = project_root / item.canonical_relative
        exists = destination.is_file()
        copied = False
        if args.mode == "copy":
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(item.source, destination)
            exists = True
            copied = True
        rows.append({
            "tier": item.tier,
            "sheet": item.sheet,
            "source": str(item.source),
            "destination": str(destination),
            "canonical_relative": str(item.canonical_relative).replace("\\", "/"),
            "exists_after": str(exists).lower(),
            "copied": str(copied).lower(),
        })

    report = args.report or (project_root / "logs" / "generated_art_payload_install_report.csv")
    report.parent.mkdir(parents=True, exist_ok=True)
    with report.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["tier", "sheet", "source", "destination", "canonical_relative", "exists_after", "copied"])
        writer.writeheader()
        writer.writerows(rows)
    print(f"mode={args.mode} source={source} files={len(rows)} report={report}")


if __name__ == "__main__":
    main()

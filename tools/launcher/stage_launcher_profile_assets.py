#!/usr/bin/env python3
"""
Locate and stage the minimal launcher profile assets from the existing client asset tree.

This helper intentionally does not generate replacement portrait art and does not copy
any full portrait catalog. It only looks for approved source hints used by the thin
launcher profile packages:

- 8X8 Source / 8x8 source portrait sheet
- approved cleared special/name-locked portrait asset

Packaging scripts may call this helper before jpackage. If an exact source cannot be
found, it writes a clear audit report and returns non-zero unless --allow-missing is set.
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

IMAGE_SUFFIXES = {".png", ".jpg", ".jpeg", ".webp", ".bmp", ".gif"}


@dataclass(frozen=True)
class AssetRule:
    package_id: str
    package_dir_name: str
    output_subdir: str
    required_hint: str
    filename_tokens_any_order: tuple[str, ...]
    fallback_tokens: tuple[str, ...]


RULES = (
    AssetRule(
        package_id="launcher-human-8x8-v1",
        package_dir_name="human-8x8",
        output_subdir="human-8x8/assets",
        required_hint="8X8 Source",
        filename_tokens_any_order=("8", "source"),
        fallback_tokens=("8x8", "8X8", "human8x8", "human-8x8"),
    ),
    AssetRule(
        package_id="launcher-special-portraits-v1",
        package_dir_name="special-portraits",
        output_subdir="special-portraits/assets",
        required_hint="approved cleared special/name-locked portrait asset in main client",
        filename_tokens_any_order=("special",),
        fallback_tokens=("name locked", "namelocked", "profile special", "special portrait"),
    ),
)


def iter_files(root: Path) -> Iterable[Path]:
    if not root.exists():
        return
    for path in root.rglob("*"):
        if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES:
            yield path


def score_candidate(path: Path, rule: AssetRule) -> int:
    name = path.name.lower()
    full = str(path).lower()
    score = 0
    if all(token.lower() in name for token in rule.filename_tokens_any_order):
        score += 100
    if all(token.lower() in full for token in rule.filename_tokens_any_order):
        score += 50
    for token in rule.fallback_tokens:
        token_l = token.lower()
        if token_l in name:
            score += 25
        elif token_l in full:
            score += 10
    # Prefer files closer to portrait/client asset folders if there is ambiguity.
    if "portrait" in full:
        score += 15
    if "assets" in full:
        score += 5
    return score


def choose_candidate(asset_root: Path, rule: AssetRule) -> tuple[Path | None, list[tuple[int, str]]]:
    scored: list[tuple[int, str]] = []
    for file in iter_files(asset_root):
        score = score_candidate(file, rule)
        if score > 0:
            scored.append((score, str(file)))
    scored.sort(key=lambda item: (-item[0], item[1].lower()))
    if not scored:
        return None, []
    best_score, best_path = scored[0]
    if best_score < 50:
        return None, scored[:20]
    return Path(best_path), scored[:20]


def update_manifest(manifest_path: Path, source_path: Path, staged_path: Path, project_root: Path) -> None:
    data = json.loads(manifest_path.read_text(encoding="utf-8"))
    source_policy = data.setdefault("source_policy", {})
    source_policy["exact_asset_path_status"] = "mapped"
    source_policy["exact_source_path"] = source_path.relative_to(project_root).as_posix()
    source_policy["staged_asset_path"] = staged_path.relative_to(project_root).as_posix()
    data["staged_assets"] = [staged_path.relative_to(project_root).as_posix()]
    manifest_path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", default=".")
    parser.add_argument("--asset-root", default="assets")
    parser.add_argument("--launcher-package-root", default="launcher/profile-packages")
    parser.add_argument("--allow-missing", action="store_true")
    args = parser.parse_args(argv)

    project_root = Path(args.project_root).resolve()
    asset_root = (project_root / args.asset_root).resolve()
    launcher_root = (project_root / args.launcher_package_root).resolve()
    report: dict[str, object] = {"asset_root": str(asset_root), "packages": []}
    missing: list[str] = []

    for rule in RULES:
        source, candidates = choose_candidate(asset_root, rule)
        manifest = launcher_root / rule.package_dir_name / "package.json"
        entry: dict[str, object] = {
            "package_id": rule.package_id,
            "required_hint": rule.required_hint,
            "manifest": str(manifest),
            "candidates": candidates,
        }
        if source is None:
            missing.append(rule.package_id)
            entry["status"] = "missing"
            report["packages"].append(entry)
            continue
        output_dir = launcher_root / rule.output_subdir
        output_dir.mkdir(parents=True, exist_ok=True)
        staged = output_dir / source.name
        shutil.copy2(source, staged)
        if manifest.exists():
            update_manifest(manifest, source, staged, project_root)
        entry.update({
            "status": "staged",
            "source": source.relative_to(project_root).as_posix(),
            "staged": staged.relative_to(project_root).as_posix(),
        })
        report["packages"].append(entry)

    report_path = launcher_root / "asset-staging-report.json"
    report_path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"Launcher profile asset staging report: {report_path}")
    if missing and not args.allow_missing:
        print("Missing launcher profile assets: " + ", ".join(missing), file=sys.stderr)
        print("Use --allow-missing only for documentation/manifest-only transition builds.", file=sys.stderr)
        return 23
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

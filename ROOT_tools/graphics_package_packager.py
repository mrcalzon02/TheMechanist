#!/usr/bin/env python3
"""Package compiled graphical assets into client-selectable graphics packages.

This command copies actual files into PACKAGE_client/assets/graphics/packages.
It does not leave the client pointing at ROOT_tools or loose source locations.

Default behavior creates/refreshes the uncompressed ready-to-use default_32 package.
Higher-resolution packages can be staged as unzipped folders or compressed zip bundles.
"""

from __future__ import annotations

import argparse
import json
import shutil
import zipfile
from pathlib import Path
from datetime import datetime, timezone

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
COMPILED_ROOT = REPO_ROOT / "ROOT_tools/atlas_asset_pipeline/compiled_assets"
PACKAGE_ROOT = REPO_ROOT / "PACKAGE_client/assets/graphics/packages"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Copy compiled assets into client graphics package folders/bundles.")
    parser.add_argument("--source-size", default="32", help="Compiled source size folder without px suffix. Default: 32")
    parser.add_argument("--package-id", default="default_32", help="Client graphics package id/folder name.")
    parser.add_argument("--display-name", default="Default 32px", help="Display name written into package_info.json.")
    parser.add_argument("--compressed", action="store_true", help="Write a .zip bundle instead of an unzipped package folder.")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def copy_tree(source: Path, destination: Path, dry_run: bool) -> int:
    files = [path for path in source.rglob("*") if path.is_file()]
    if dry_run:
        return len(files)
    if destination.exists():
        shutil.rmtree(destination)
    destination.mkdir(parents=True, exist_ok=True)
    for file in files:
        rel = file.relative_to(source)
        target = destination / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, target)
    return len(files)


def write_manifest(package_dir: Path, package_id: str, display_name: str, source_size: str, file_count: int, dry_run: bool) -> None:
    payload = {
        "schema": "mechanist.graphics_package.v1",
        "id": package_id,
        "display_name": display_name,
        "nominal_size_px": int(source_size),
        "compressed": False,
        "ready_to_use": True,
        "file_count": file_count,
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
    }
    if not dry_run:
        (package_dir / "package_info.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")


def zip_package(package_dir: Path, zip_path: Path, dry_run: bool) -> int:
    files = [path for path in package_dir.rglob("*") if path.is_file()]
    if dry_run:
        return len(files)
    if zip_path.exists():
        zip_path.unlink()
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as zf:
        for file in files:
            zf.write(file, file.relative_to(package_dir.parent).as_posix())
    return len(files)


def main() -> int:
    args = parse_args()
    source_size = str(args.source_size).removesuffix("px")
    source = COMPILED_ROOT / f"{source_size}px"
    if not source.is_dir():
        raise FileNotFoundError(f"Compiled source size folder not found: {source}")

    package_dir = PACKAGE_ROOT / args.package_id
    file_count = copy_tree(source, package_dir, args.dry_run)
    write_manifest(package_dir, args.package_id, args.display_name, source_size, file_count, args.dry_run)

    zip_path = PACKAGE_ROOT / f"{args.package_id}.zip"
    if args.compressed:
        zip_count = zip_package(package_dir, zip_path, args.dry_run)
        if not args.dry_run and package_dir.exists():
            shutil.rmtree(package_dir)
        print(f"Packaged compressed graphics bundle: {zip_path} ({zip_count} files)")
    else:
        print(f"Packaged uncompressed graphics folder: {package_dir} ({file_count} files)")

    if args.dry_run:
        print("Dry run only; no files were written.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

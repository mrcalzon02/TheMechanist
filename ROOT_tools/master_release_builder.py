#!/usr/bin/env python3
"""Stage a future master release build under ROOT_RELEASE.

This is intentionally a scaffold, not the final production packager. It creates
ROOT_RELEASE, records the intended release command chain, and writes a manifest
that later release tooling can fill out with installer/launcher/client/server
artifacts.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from datetime import datetime, timezone

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
DEFAULT_RELEASE_ROOT = REPO_ROOT / "ROOT_RELEASE"

COMMAND_PLAN = [
    "python ROOT_tools/repository_scan_indexer.py",
    "python ROOT_tools/repository_manifest_auditor.py",
    "python ROOT_tools/Compiled_asset_packager.py --dry-run",
    "python ROOT_tools/Compiled_asset_packager.py",
    "PowerShell native Windows packaging pipeline: pending final integration",
    "Installer packaging pipeline: pending final integration",
    "Launcher/client/server executable staging: pending final integration",
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Create/update ROOT_RELEASE release staging scaffold.")
    parser.add_argument("--release-root", default=str(DEFAULT_RELEASE_ROOT))
    parser.add_argument("--version", default="dev-unversioned")
    parser.add_argument("--channel", default="dev")
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    release_root = Path(args.release_root).resolve()
    payload = {
        "schema": "mechanist.master_release_scaffold.v1",
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "version": args.version,
        "channel": args.channel,
        "release_root": str(release_root),
        "status": "scaffold_only",
        "note": "This is not the final production release builder yet. It stages ROOT_RELEASE metadata and records the intended command plan.",
        "command_plan": COMMAND_PLAN,
        "expected_future_outputs": [
            "ROOT_RELEASE/installer/",
            "ROOT_RELEASE/launcher/",
            "ROOT_RELEASE/client/",
            "ROOT_RELEASE/server/",
            "ROOT_RELEASE/checksums/",
            "ROOT_RELEASE/release_manifest.json"
        ]
    }
    print(json.dumps(payload, indent=2))
    if args.dry_run:
        print("Dry run only; ROOT_RELEASE was not written.")
        return 0

    release_root.mkdir(parents=True, exist_ok=True)
    for child in ["installer", "launcher", "client", "server", "checksums", "logs"]:
        (release_root / child).mkdir(parents=True, exist_ok=True)
    (release_root / "release_scaffold_manifest.json").write_text(json.dumps(payload, indent=2), encoding="utf-8")
    (release_root / "README.md").write_text(
        "# The Mechanist ROOT_RELEASE\n\n"
        "This directory is the staged output root for future master release builds.\n\n"
        "Current status: scaffold only. Final installer/launcher/client/server packaging is pending integration.\n",
        encoding="utf-8",
    )
    print(f"Wrote release scaffold under {release_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

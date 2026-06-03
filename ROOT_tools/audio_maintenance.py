#!/usr/bin/env python3
"""One-command local audio maintenance for The Mechanist.

Runs the currently required local audio cleanup sequence:
  1. Move .mp3 source/export files out of PACKAGE_client/assets/sound and into ROOT_SRC_assets/ROOT_Sounds.
  2. Re-run the audio package audit.
  3. Optionally run repository scan/audit so docs reflect the moved files.

This exists so routine package hygiene does not require remembering several
separate Python commands.

Default mode is dry-run. Pass --apply to move files.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
PYTHON = sys.executable


def run_step(label: str, args: list[str]) -> int:
    print("\n=== " + label + " ===")
    print(" ".join(args))
    completed = subprocess.run(args, cwd=REPO_ROOT, text=True)
    if completed.returncode != 0:
        print(f"Step failed: {label} exit={completed.returncode}", file=sys.stderr)
    return completed.returncode


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run local audio package maintenance steps.")
    parser.add_argument("--apply", action="store_true", help="Actually move MP3 source files. Omit for dry-run.")
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing destination MP3 files when moving.")
    parser.add_argument("--skip-repo-scan", action="store_true", help="Do not run repository_scan_indexer.py or repository_manifest_auditor.py after audio audit.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    move_cmd = [PYTHON, str(ROOT_TOOLS / "move_client_mp3_sources.py")]
    if args.apply:
        move_cmd.append("--apply")
    if args.overwrite:
        move_cmd.append("--overwrite")
    code = run_step("Move client MP3 source/export files", move_cmd)
    if code != 0:
        return code

    code = run_step("Audit client audio package", [PYTHON, str(ROOT_TOOLS / "audio_package_auditor.py")])
    if code != 0:
        return code

    if not args.skip_repo_scan:
        scanner = ROOT_TOOLS / "repository_scan_indexer.py"
        auditor = ROOT_TOOLS / "repository_manifest_auditor.py"
        if scanner.exists():
            code = run_step("Refresh repository file manifest", [PYTHON, str(scanner)])
            if code != 0:
                return code
        if auditor.exists():
            code = run_step("Refresh repository manifest audit", [PYTHON, str(auditor)])
            if code != 0:
                return code

    print("\nAudio maintenance complete.")
    if not args.apply:
        print("Dry-run mode was used. Re-run with --apply to move files.")
    else:
        print("Review git status, test the client, then commit the moved files and audit outputs.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

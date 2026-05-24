#!/usr/bin/env python3
"""Generate a packaging release manifest for The Mechanist artifacts."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import pathlib
import platform
import subprocess
import sys
from typing import Any


def run(cmd: list[str], cwd: pathlib.Path | None = None) -> str | None:
    try:
        out = subprocess.check_output(cmd, cwd=str(cwd) if cwd else None, stderr=subprocess.DEVNULL, text=True)
        return out.strip()
    except Exception:
        return None


def sha256(path: pathlib.Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def describe_path(path: pathlib.Path) -> dict[str, Any]:
    entry: dict[str, Any] = {
        "path": str(path),
        "exists": path.exists(),
    }
    if path.is_file():
        entry["type"] = "file"
        entry["bytes"] = path.stat().st_size
        entry["sha256"] = sha256(path)
    elif path.is_dir():
        entry["type"] = "directory"
        files = [p for p in path.rglob("*") if p.is_file()]
        entry["file_count"] = len(files)
        entry["bytes"] = sum(p.stat().st_size for p in files)
        digest = hashlib.sha256()
        for p in sorted(files):
            rel = p.relative_to(path).as_posix()
            digest.update(rel.encode("utf-8"))
            digest.update(b"\0")
            digest.update(sha256(p).encode("ascii"))
            digest.update(b"\n")
        entry["tree_sha256"] = digest.hexdigest()
    else:
        entry["type"] = "missing"
    return entry


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate The Mechanist release manifest.")
    parser.add_argument("--repo-root", default=".")
    parser.add_argument("--output", required=True)
    parser.add_argument("--artifact", action="append", default=[])
    parser.add_argument("--channel", default="dev")
    parser.add_argument("--notes", default="")
    args = parser.parse_args()

    repo_root = pathlib.Path(args.repo_root).resolve()
    output = pathlib.Path(args.output).resolve()
    output.parent.mkdir(parents=True, exist_ok=True)

    java_version = run(["java", "-version"])
    if java_version is None:
        java_version = "<java unavailable or version printed to stderr>"

    manifest: dict[str, Any] = {
        "schema": "stellarcore.mechanist.release-manifest.v1",
        "generated_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "channel": args.channel,
        "notes": args.notes,
        "repo": {
            "root": str(repo_root),
            "commit": run(["git", "rev-parse", "HEAD"], repo_root),
            "branch": run(["git", "rev-parse", "--abbrev-ref", "HEAD"], repo_root),
            "dirty": run(["git", "status", "--porcelain"], repo_root) not in (None, ""),
        },
        "builder": {
            "os": platform.platform(),
            "machine": platform.machine(),
            "python": sys.version.split()[0],
            "java_runtime": java_version,
        },
        "artifacts": [describe_path(pathlib.Path(a).resolve()) for a in args.artifact],
    }

    output.write_text(json.dumps(manifest, indent=2, sort_keys=True), encoding="utf-8")
    print(f"Wrote release manifest: {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

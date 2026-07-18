#!/usr/bin/env python3
"""Append one idempotent verified remote-release entry to development history."""

from __future__ import annotations

import argparse
import datetime as dt
import glob
import json
import pathlib


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--history", type=pathlib.Path, required=True)
    parser.add_argument("--reports", required=True,
                        help="Glob matching platform verification reports")
    parser.add_argument("--commit", required=True)
    parser.add_argument("--run-url", required=True)
    args = parser.parse_args()

    report_paths = sorted(glob.glob(args.reports, recursive=True))
    if len(report_paths) < 2:
        raise SystemExit(f"Expected Linux and Windows reports; found {len(report_paths)}")
    reports = [json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
               for path in report_paths]
    versions = {str(report.get("version", "")) for report in reports}
    commits = {str(report.get("commit", "")) for report in reports}
    platforms = {str(report.get("platform", "")) for report in reports}
    statuses = {str(report.get("status", "")) for report in reports}
    if len(versions) != 1 or "" in versions:
        raise SystemExit(f"Verification reports disagree on version: {sorted(versions)}")
    if commits != {args.commit}:
        raise SystemExit(f"Verification reports do not identify {args.commit}: {sorted(commits)}")
    if not {"linux-x64", "windows-x64"}.issubset(platforms):
        raise SystemExit(f"Verification reports lack both platforms: {sorted(platforms)}")
    if statuses != {"verified"}:
        raise SystemExit(f"Verification reports are not all verified: {sorted(statuses)}")

    version = next(iter(versions))
    marker = f"verified-remote-release:{version}:{args.commit}"
    history = args.history.read_text(encoding="utf-8")
    if marker in history:
        print(f"Development history already contains {marker}")
        return 0

    date = dt.datetime.now(dt.timezone.utc).date().isoformat()
    entry = f"""

## Remote Release {version} - Java 17, Gate 3, and Runnable Distribution Certification

Recorded the first-class remote release pipeline for the launcher -> client -> server distribution path. GitHub-hosted Linux x64 and Windows x64 jobs compiled the exact source tree with Java 17, rebuilt the client, server, and launcher packages, staged platform-specific support libraries and bundled Java runtimes, generated schema-2 SHA-256 manifests, and produced portable runnable ZIP distributions. Gate 3 ran only after both platform package jobs completed successfully, and final distribution certification revalidated archive integrity, manifest completeness, entry points, platform-native support libraries, and Java 17 classfile compatibility before release publication.

Verification: exact source commit `{args.commit}`; version `{version}`; platforms `linux-x64` and `windows-x64`; Java 17 compile passed; packaged client and headless server operation smokes passed; downstream Gate 3 passed; distribution verification reports returned `verified`; final release certification passed. Workflow evidence: {args.run_url}

<!-- {marker} -->
"""
    args.history.write_text(history.rstrip() + entry + "\n", encoding="utf-8")
    print(f"Appended verified release history entry for {version} at {args.commit}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

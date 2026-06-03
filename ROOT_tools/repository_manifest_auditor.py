#!/usr/bin/env python3
"""
Repository manifest auditor for The Mechanist.

Consumes docs/repository_file_manifest.tsv and emits compact, reviewable audit
outputs instead of forcing humans to inspect a 15k+ row manifest by hand.

Default outputs:
  docs/repository_manifest_audit_report.md
  docs/repository_manifest_audit_issues.tsv

The generated report is intentionally deterministic so it can be committed when
useful, diffed across asset passes, or discarded after local review.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
from pathlib import Path
from collections import Counter, defaultdict
from typing import Iterable

DEFAULT_MANIFEST = "docs/repository_file_manifest.tsv"
DEFAULT_REPORT = "docs/repository_manifest_audit_report.md"
DEFAULT_ISSUES = "docs/repository_manifest_audit_issues.tsv"

ISSUE_COLUMNS = [
    "severity",
    "issue_type",
    "path",
    "detail",
    "recommendation",
]

IMAGE_DIMENSION_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".tif", ".tiff"}
EXPECTED_LAUNCHER_EXTENSIONS = {".exe", ".cmd", ".bat", ".ps1"}
BINARY_PACKAGE_EXTENSIONS = {".exe", ".dll", ".jar", ".ico", ".png", ".jpg", ".jpeg", ".wav", ".mp3", ".ogg"}


def posix(path: Path) -> str:
    return path.as_posix()


def default_root() -> Path:
    script_path = Path(__file__).resolve()
    if script_path.parent.name.lower() in {"root_tools", "roots_tools", "tools"}:
        return script_path.parent.parent
    return Path.cwd()


def read_manifest(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        raise FileNotFoundError(f"Manifest not found: {path}")
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


def as_int(value: str, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def sorted_counter(counter: Counter[str], limit: int | None = None) -> list[tuple[str, int]]:
    items = sorted(counter.items(), key=lambda item: (-item[1], item[0]))
    return items if limit is None else items[:limit]


def add_issue(issues: list[dict[str, str]], severity: str, issue_type: str, path: str, detail: str, recommendation: str) -> None:
    issues.append({
        "severity": severity,
        "issue_type": issue_type,
        "path": path,
        "detail": detail,
        "recommendation": recommendation,
    })


def audit_manifest(rows: list[dict[str, str]], root: Path, manifest_rel: str) -> tuple[list[dict[str, str]], dict[str, Counter[str]]]:
    issues: list[dict[str, str]] = []

    by_sha: dict[str, list[dict[str, str]]] = defaultdict(list)
    counters: dict[str, Counter[str]] = {
        "root_area": Counter(),
        "file_family": Counter(),
        "asset_category": Counter(),
        "likely_content_role": Counter(),
        "extension": Counter(),
    }

    paths_seen: set[str] = set()

    for row in rows:
        path = row.get("path", "")
        extension = row.get("extension", "")
        family = row.get("file_family", "")
        category = row.get("asset_category", "")
        role = row.get("likely_content_role", "")
        root_area = row.get("root_area", "") or "(repo-root)"
        sha = row.get("sha256", "")
        scan_note = row.get("scan_note", "")

        counters["root_area"][root_area] += 1
        counters["file_family"][family or "(blank)"] += 1
        counters["asset_category"][category or "(blank)"] += 1
        counters["likely_content_role"][role or "(blank)"] += 1
        counters["extension"][extension or "(none)"] += 1

        if path in paths_seen:
            add_issue(
                issues,
                "error",
                "duplicate_manifest_path",
                path,
                "The manifest contains the same path more than once.",
                "Regenerate the manifest and check for case-only path collisions or duplicate scan roots.",
            )
        paths_seen.add(path)

        if family == "scan_error" or category == "scan_error" or role == "scan_error":
            add_issue(
                issues,
                "error",
                "scan_error_row",
                path,
                scan_note or "Row was emitted by the scanner exception fallback.",
                "Inspect the file and update the scanner so the file can be classified without falling through to scan_error.",
            )

        if family == "unknown":
            add_issue(
                issues,
                "warning",
                "unknown_file_family",
                path,
                f"Extension '{extension or '(none)'}' is not classified by the scanner.",
                "Add the extension to the scanner family tables if this file type is expected and meaningful.",
            )

        if path != manifest_rel and not sha:
            add_issue(
                issues,
                "warning",
                "missing_sha256",
                path,
                "The file row has no SHA-256 hash.",
                "Regenerate the manifest. If the hash still fails, inspect filesystem permissions or long-path handling.",
            )

        if sha:
            by_sha[sha].append(row)

        if family == "image" and extension in IMAGE_DIMENSION_EXTENSIONS:
            if not row.get("width_px") or not row.get("height_px"):
                add_issue(
                    issues,
                    "warning",
                    "image_missing_dimensions",
                    path,
                    "Raster image row lacks width/height metadata.",
                    "Install Pillow for richer probing or inspect whether the image file is corrupt/unsupported.",
                )

        if "launcher" in path.lower() and extension not in EXPECTED_LAUNCHER_EXTENSIONS:
            add_issue(
                issues,
                "info",
                "launcher_related_non_entry_file",
                path,
                "Launcher-related file is not an obvious Windows entry-point extension.",
                "This may be fine for source/config, but ensure packaged release folders expose .exe entry points.",
            )

        if root_area.lower().startswith("package") and extension == ".ps1":
            add_issue(
                issues,
                "warning",
                "packaged_powershell_script",
                path,
                "A PowerShell script exists inside a package tree.",
                "For production Windows releases, expose compiled/native .exe entry points and keep .ps1 files internal or diagnostic only.",
            )

    for sha, duplicate_rows in by_sha.items():
        if len(duplicate_rows) <= 1:
            continue
        paths = [row.get("path", "") for row in duplicate_rows]
        # Duplicate hashes are often intentional for copied icons or mirrored assets, so this is informational.
        add_issue(
            issues,
            "info",
            "duplicate_content_hash",
            paths[0],
            f"Same SHA-256 appears in {len(paths)} files: " + "; ".join(paths[:12]) + ("; ..." if len(paths) > 12 else ""),
            "Review if this is expected mirroring. If not, remove redundant files or consolidate references.",
        )

    manifest_path = root / manifest_rel
    if manifest_path.exists():
        actual_size = manifest_path.stat().st_size
        actual_lines = sum(1 for _ in manifest_path.open("r", encoding="utf-8-sig", errors="replace"))
        for row in rows:
            if row.get("path") == manifest_rel:
                recorded_size = as_int(row.get("size_bytes", ""), -1)
                recorded_lines = as_int(row.get("line_count", ""), -1)
                if recorded_size != actual_size or recorded_lines != actual_lines:
                    add_issue(
                        issues,
                        "info",
                        "manifest_self_row_stale",
                        manifest_rel,
                        f"Manifest self-row records size={recorded_size}, lines={recorded_lines}; actual size={actual_size}, lines={actual_lines}.",
                        "This is expected if the scanner indexes the old manifest before rewriting it. Consider omitting the self-row or patching it after write.",
                    )
                break

    issues.sort(key=lambda item: (severity_rank(item["severity"]), item["issue_type"], item["path"]))
    return issues, counters


def severity_rank(severity: str) -> int:
    return {"error": 0, "warning": 1, "info": 2}.get(severity, 9)


def write_issues(issues: list[dict[str, str]], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=ISSUE_COLUMNS, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(issues)


def markdown_table(counter: Counter[str], limit: int = 25) -> str:
    lines = ["| Value | Count |", "|---|---:|"]
    for value, count in sorted_counter(counter, limit):
        safe_value = value.replace("|", "\\|")
        lines.append(f"| `{safe_value}` | {count} |")
    return "\n".join(lines)


def top_large_files(rows: list[dict[str, str]], limit: int = 30) -> list[dict[str, str]]:
    return sorted(rows, key=lambda row: as_int(row.get("size_bytes", "")), reverse=True)[:limit]


def write_report(rows: list[dict[str, str]], issues: list[dict[str, str]], counters: dict[str, Counter[str]], report_path: Path, issues_rel: str) -> None:
    report_path.parent.mkdir(parents=True, exist_ok=True)

    issue_counts = Counter(issue["severity"] for issue in issues)
    total_size = sum(as_int(row.get("size_bytes", "")) for row in rows)
    image_count = counters["file_family"].get("image", 0)
    audio_count = counters["file_family"].get("audio", 0)
    source_count = counters["file_family"].get("source_code", 0)

    lines: list[str] = []
    lines.append("# Repository Manifest Audit Report")
    lines.append("")
    lines.append(f"Generated UTC: `{dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat()}`")
    lines.append("")
    lines.append("## Summary")
    lines.append("")
    lines.append(f"- Manifest rows: `{len(rows)}`")
    lines.append(f"- Approximate indexed bytes: `{total_size}`")
    lines.append(f"- Images: `{image_count}`")
    lines.append(f"- Audio files: `{audio_count}`")
    lines.append(f"- Source files: `{source_count}`")
    lines.append(f"- Audit issues: `{len(issues)}`")
    lines.append(f"  - Errors: `{issue_counts.get('error', 0)}`")
    lines.append(f"  - Warnings: `{issue_counts.get('warning', 0)}`")
    lines.append(f"  - Info: `{issue_counts.get('info', 0)}`")
    lines.append(f"- Full issue ledger: `{issues_rel}`")
    lines.append("")

    lines.append("## File Families")
    lines.append("")
    lines.append(markdown_table(counters["file_family"]))
    lines.append("")

    lines.append("## Asset Categories")
    lines.append("")
    lines.append(markdown_table(counters["asset_category"], 40))
    lines.append("")

    lines.append("## Root Areas")
    lines.append("")
    lines.append(markdown_table(counters["root_area"], 40))
    lines.append("")

    lines.append("## Largest Files")
    lines.append("")
    lines.append("| Size Bytes | Family | Path |")
    lines.append("|---:|---|---|")
    for row in top_large_files(rows):
        path = row.get("path", "").replace("|", "\\|")
        lines.append(f"| {as_int(row.get('size_bytes', ''))} | `{row.get('file_family', '')}` | `{path}` |")
    lines.append("")

    lines.append("## Highest Priority Issues")
    lines.append("")
    lines.append("| Severity | Type | Path | Detail |")
    lines.append("|---|---|---|---|")
    for issue in issues[:50]:
        severity = issue["severity"]
        issue_type = issue["issue_type"]
        path = issue["path"].replace("|", "\\|")
        detail = issue["detail"].replace("|", "\\|")
        lines.append(f"| `{severity}` | `{issue_type}` | `{path}` | {detail} |")
    if len(issues) > 50:
        lines.append(f"| ... | ... | ... | {len(issues) - 50} more issues in `{issues_rel}` |")
    lines.append("")

    report_path.write_text("\n".join(lines), encoding="utf-8")


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit docs/repository_file_manifest.tsv and produce reviewable reports.")
    parser.add_argument("--root", default=None, help="Repository root. Defaults to parent of ROOT_tools when run from ROOT_tools.")
    parser.add_argument("--manifest", default=DEFAULT_MANIFEST, help=f"Manifest TSV path relative to repo root. Default: {DEFAULT_MANIFEST}")
    parser.add_argument("--report", default=DEFAULT_REPORT, help=f"Markdown report path relative to repo root. Default: {DEFAULT_REPORT}")
    parser.add_argument("--issues", default=DEFAULT_ISSUES, help=f"Issue TSV path relative to repo root. Default: {DEFAULT_ISSUES}")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    root = Path(args.root).resolve() if args.root else default_root().resolve()
    manifest_rel = posix(Path(args.manifest))
    report_rel = posix(Path(args.report))
    issues_rel = posix(Path(args.issues))

    manifest_path = root / manifest_rel
    report_path = root / report_rel
    issues_path = root / issues_rel

    rows = read_manifest(manifest_path)
    issues, counters = audit_manifest(rows, root, manifest_rel)
    write_issues(issues, issues_path)
    write_report(rows, issues, counters, report_path, issues_rel)

    print(f"Read {len(rows)} manifest rows from {manifest_path}")
    print(f"Wrote audit report: {report_path}")
    print(f"Wrote audit issues: {issues_path}")
    print(f"Issues: {len(issues)} total; "
          f"errors={sum(1 for issue in issues if issue['severity'] == 'error')}; "
          f"warnings={sum(1 for issue in issues if issue['severity'] == 'warning')}; "
          f"info={sum(1 for issue in issues if issue['severity'] == 'info')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

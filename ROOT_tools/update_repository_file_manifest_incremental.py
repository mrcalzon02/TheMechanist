#!/usr/bin/env python3
"""Stable incremental generator for ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv.

The manifest covers every non-.git file in the checkout. Existing SHA-256 values
are reused immediately when path, size, and mtime match. On a clean checkout,
where mtimes commonly change even though bytes do not, the file is rehashed and
the prior recorded modified time is preserved when the digest is unchanged.
This keeps the committed manifest deterministic instead of rewriting every row
merely because the repository was checked out again.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import sys
import tempfile

COLUMNS = [
    "relative_path",
    "file_kind",
    "text_or_binary",
    "extension",
    "bytes",
    "modified_utc",
    "sha256",
]
BINARY_EXTENSIONS = {
    ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".webp",
    ".mp3", ".wav", ".ogg", ".flac", ".m4a",
    ".jar", ".zip", ".exe", ".msi", ".dll", ".so", ".dylib", ".class",
}


def repo_root() -> Path:
    return Path(__file__).resolve().parent.parent


def posix_relative(path: Path, root: Path) -> str:
    return path.resolve().relative_to(root).as_posix()


def clean(value: object) -> str:
    if value is None:
        return ""
    return str(value).replace("\t", " ").replace("\r", " ").replace("\n", " ")


def modified_utc(path: Path) -> str:
    return (
        dt.datetime.fromtimestamp(path.stat().st_mtime, tz=dt.timezone.utc)
        .replace(microsecond=0)
        .strftime("%Y-%m-%dT%H:%M:%SZ")
    )


def file_kind(relative_path: str, extension: str) -> str:
    path = relative_path.lower()
    ext = extension.lower()
    if path.startswith("root_src_assets/"):
        return "protected_source_asset"
    if path.startswith("package_client/assets/"):
        return "client_runtime_asset"
    if path.startswith("package_launcher/") and "/resources/assets/" in path:
        return "launcher_runtime_asset"
    if path.startswith("package_client/"):
        return "client_package_file"
    if path.startswith("package_launcher/"):
        return "launcher_package_file"
    if path.startswith("package_installer/"):
        return "installer_package_file"
    if path.startswith("root_docs/"):
        return "documentation"
    if path.startswith("root_tools/") or path.startswith("scripts/"):
        return "tooling"
    if path.startswith("root_build/"):
        return "build_orchestration"
    if path.startswith(".github/"):
        return "continuous_integration"
    if path.startswith("config/"):
        return "build_configuration"
    if path.startswith("src/"):
        return "source_code"
    if ext in {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".svg", ".webp"}:
        return "image"
    if ext in {".mp3", ".wav", ".ogg", ".flac", ".m4a"}:
        return "audio"
    if ext in {".jar", ".zip", ".exe", ".msi", ".dll", ".so", ".dylib", ".class"}:
        return "binary"
    if ext in {".md", ".txt", ".csv", ".tsv", ".json", ".xml", ".properties", ".mf", ".conf", ".yml", ".yaml"}:
        return "text_data"
    return "other"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def valid_sha256(value: str | None) -> bool:
    text = (value or "").strip().lower()
    return len(text) == 64 and all(char in "0123456789abcdef" for char in text)


def load_existing(path: Path) -> dict[str, dict[str, str]]:
    if not path.exists() or path.stat().st_size == 0:
        return {}
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if reader.fieldnames != COLUMNS:
            return {}
        return {
            row.get("relative_path", ""): row
            for row in reader
            if row.get("relative_path")
        }


def iter_files(root: Path, target: Path) -> list[Path]:
    files: list[Path] = []
    for current_root, dir_names, file_names in os.walk(root):
        dir_names[:] = sorted(name for name in dir_names if name != ".git")
        for file_name in sorted(file_names):
            path = Path(current_root) / file_name
            if path.resolve() == target:
                continue
            if path.is_file():
                files.append(path)
    files.sort(key=lambda item: item.resolve().as_posix().lower())
    return files


def write_rows(path: Path, rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(
        prefix=path.name + ".",
        suffix=".tmp",
        dir=str(path.parent),
    )
    os.close(fd)
    temp_path = Path(temp_name)
    try:
        with temp_path.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(
                handle,
                fieldnames=COLUMNS,
                delimiter="\t",
                lineterminator="\n",
            )
            writer.writeheader()
            writer.writerows(rows)
        temp_path.replace(path)
    finally:
        if temp_path.exists():
            temp_path.unlink()


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Incrementally refresh ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv."
    )
    parser.add_argument(
        "--target",
        default="ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv",
        help="Manifest path relative to repo root.",
    )
    parser.add_argument(
        "--force-hash",
        action="store_true",
        help="Rehash every file while preserving stable recorded times for unchanged bytes.",
    )
    parser.add_argument(
        "--report",
        type=Path,
        help="Optional JSON generation report path relative to repo root.",
    )
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    root = repo_root()
    target = (root / args.target).resolve()
    try:
        target.relative_to(root)
    except ValueError:
        print(f"Manifest target must stay inside the repository: {target}", file=sys.stderr)
        return 2

    report_path = None
    if args.report:
        report_path = (root / args.report).resolve() if not args.report.is_absolute() else args.report.resolve()
        try:
            report_path.relative_to(root)
        except ValueError:
            print(f"Manifest report must stay inside the repository: {report_path}", file=sys.stderr)
            return 2

    existing = load_existing(target)
    rows: list[dict[str, str]] = []
    reused_metadata = 0
    rehashed_same = 0
    hashed_changed = 0
    errors = 0
    total_bytes = 0

    for path in iter_files(root, target):
        relative = posix_relative(path, root)
        stat = path.stat()
        ext_with_dot = path.suffix.lower()
        ext = ext_with_dot[1:]
        current_modified = modified_utc(path)
        bytes_text = str(stat.st_size)
        total_bytes += stat.st_size
        previous = existing.get(relative)
        previous_sha = previous.get("sha256", "") if previous else ""
        previous_modified = previous.get("modified_utc", "") if previous else ""
        sha = ""
        recorded_modified = current_modified

        if (
            not args.force_hash
            and previous
            and previous.get("bytes") == bytes_text
            and previous_modified == current_modified
            and valid_sha256(previous_sha)
        ):
            sha = previous_sha.lower()
            recorded_modified = previous_modified
            reused_metadata += 1
        else:
            try:
                sha = sha256_file(path)
                if (
                    previous
                    and previous.get("bytes") == bytes_text
                    and valid_sha256(previous_sha)
                    and sha.lower() == previous_sha.lower()
                    and previous_modified
                ):
                    recorded_modified = previous_modified
                    rehashed_same += 1
                else:
                    hashed_changed += 1
            except OSError as exc:
                sha = "HASH_ERROR:" + clean(exc)
                errors += 1

        rows.append({
            "relative_path": clean(relative),
            "file_kind": clean(file_kind(relative, ext_with_dot)),
            "text_or_binary": "binary" if ext_with_dot in BINARY_EXTENSIONS else "text_or_unknown",
            "extension": clean(ext),
            "bytes": bytes_text,
            "modified_utc": clean(recorded_modified),
            "sha256": clean(sha),
        })

    rows.append({
        "relative_path": posix_relative(target, root),
        "file_kind": "generated_repository_manifest",
        "text_or_binary": "text_or_unknown",
        "extension": "tsv",
        "bytes": "GENERATED",
        "modified_utc": "GENERATED",
        "sha256": "SELF_GENERATED",
    })
    write_rows(target, rows)

    report = {
        "status": "generated" if errors == 0 else "hash-errors",
        "target": posix_relative(target, root),
        "indexedFiles": len(rows) - 1,
        "totalBytes": total_bytes,
        "existingRows": len(existing),
        "metadataReused": reused_metadata,
        "rehashedUnchanged": rehashed_same,
        "hashedChangedOrNew": hashed_changed,
        "hashErrors": errors,
        "stableSelfRow": True,
    }
    rendered = json.dumps(report, indent=2, sort_keys=True)
    print(rendered)
    if report_path:
        report_path.parent.mkdir(parents=True, exist_ok=True)
        report_path.write_text(rendered + "\n", encoding="utf-8")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

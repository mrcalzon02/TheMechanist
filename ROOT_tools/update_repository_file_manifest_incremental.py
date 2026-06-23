#!/usr/bin/env python3
"""Incremental generator for ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv.

The legacy PowerShell generator hashes every file on every run. This script
keeps the same seven-column schema while reusing existing SHA-256 values when
path, byte length, and second-granularity UTC modified time are unchanged.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import os
from pathlib import Path
import sys
import tempfile

COLUMNS = ["relative_path", "file_kind", "text_or_binary", "extension", "bytes", "modified_utc", "sha256"]
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
    return dt.datetime.fromtimestamp(path.stat().st_mtime, tz=dt.timezone.utc).replace(microsecond=0).strftime("%Y-%m-%dT%H:%M:%SZ")


def file_kind(relative_path: str, extension: str) -> str:
    path = relative_path.lower()
    ext = extension.lower()
    if path.startswith("root_src_assets/"):
        return "protected_source_asset"
    if path.startswith("package_client/assets/"):
        return "client_runtime_asset"
    if path.startswith("package_launcher/") and "/resources/assets/" in path:
        return "launcher_runtime_asset"
    if path.startswith("root_docs/"):
        return "documentation"
    if path.startswith("root_tools/") or path.startswith("scripts/"):
        return "tooling"
    if path.startswith("src/"):
        return "source_code"
    if path.startswith("package_client/"):
        return "client_package_file"
    if path.startswith("package_launcher/"):
        return "launcher_package_file"
    if path.startswith("package_installer/"):
        return "installer_package_file"
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


def load_existing(path: Path) -> dict[str, dict[str, str]]:
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        return {row.get("relative_path", ""): row for row in reader if row.get("relative_path")}


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
    files.sort(key=lambda item: str(item.resolve()).lower())
    return files


def write_rows(path: Path, rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temp_name = tempfile.mkstemp(prefix=path.name + ".", suffix=".tmp", dir=str(path.parent))
    os.close(fd)
    temp_path = Path(temp_name)
    try:
        with temp_path.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=COLUMNS, delimiter="\t", lineterminator="\n")
            writer.writeheader()
            writer.writerows(rows)
        temp_path.replace(path)
    finally:
        if temp_path.exists():
            temp_path.unlink()


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Incrementally refresh ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv.")
    parser.add_argument("--target", default="ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv", help="Manifest path relative to repo root.")
    parser.add_argument("--force-hash", action="store_true", help="Ignore cached hashes and hash every file.")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    root = repo_root()
    target = (root / args.target).resolve()
    if not str(target).lower().startswith(str(root).lower()):
        print(f"Manifest target must stay inside the repository: {target}", file=sys.stderr)
        return 2

    existing = load_existing(target)
    rows: list[dict[str, str]] = []
    reused = 0
    hashed = 0
    errors = 0
    for path in iter_files(root, target):
        relative = posix_relative(path, root)
        stat = path.stat()
        ext_with_dot = path.suffix.lower()
        ext = ext_with_dot[1:]
        modified = modified_utc(path)
        bytes_text = str(stat.st_size)
        previous = existing.get(relative)
        sha = ""
        if not args.force_hash and previous and previous.get("bytes") == bytes_text and previous.get("modified_utc") == modified:
            sha = previous.get("sha256", "")
            reused += 1
        if not sha:
            try:
                sha = sha256_file(path)
                hashed += 1
            except OSError as exc:
                sha = "HASH_ERROR:" + clean(exc)
                errors += 1
        rows.append({
            "relative_path": clean(relative),
            "file_kind": clean(file_kind(relative, ext_with_dot)),
            "text_or_binary": "binary" if ext_with_dot in BINARY_EXTENSIONS else "text_or_unknown",
            "extension": clean(ext),
            "bytes": bytes_text,
            "modified_utc": clean(modified),
            "sha256": clean(sha),
        })

    rows.append({
        "relative_path": posix_relative(target, root),
        "file_kind": "generated_repository_manifest",
        "text_or_binary": "text_or_unknown",
        "extension": "tsv",
        "bytes": "GENERATED",
        "modified_utc": dt.datetime.now(dt.timezone.utc).replace(microsecond=0).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "sha256": "SELF_GENERATED",
    })
    write_rows(target, rows)
    print(f"Wrote {len(rows) - 1} indexed file rows to {target}")
    print(f"Manifest hash reuse: reused={reused} hashed={hashed} errors={errors}")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

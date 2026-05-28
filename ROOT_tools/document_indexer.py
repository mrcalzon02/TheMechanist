#!/usr/bin/env python3
"""Index repository documentation for the local tool dashboard.

Produces:
  docs/document_index.json
  docs/document_index.tsv

The index is intentionally lightweight: path, title, first heading, modified
size, SHA-256, and a short preview. It helps the dashboard expose docs,
milestones, and development history without hardcoded document lists.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import mimetypes
from pathlib import Path
from typing import Iterable
import json

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
DEFAULT_OUTPUT_JSON = REPO_ROOT / "docs/document_index.json"
DEFAULT_OUTPUT_TSV = REPO_ROOT / "docs/document_index.tsv"
DOC_EXTENSIONS = {".md", ".txt", ".rst", ".json", ".tsv", ".csv"}
SKIP_NAMES = {"repository_file_manifest.tsv"}


def rel(path: Path) -> str:
    return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def safe_read(path: Path, max_chars: int = 5000) -> str:
    try:
        return path.read_text(encoding="utf-8-sig", errors="replace")[:max_chars]
    except Exception:
        return ""


def extract_heading(text: str, fallback: str) -> str:
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("#"):
            return stripped.lstrip("#").strip() or fallback
    for line in text.splitlines():
        stripped = line.strip()
        if stripped:
            return stripped[:120]
    return fallback


def preview(text: str) -> str:
    clean_lines = []
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        clean_lines.append(stripped)
        if sum(len(item) for item in clean_lines) > 360:
            break
    return " ".join(clean_lines)[:420]


def classify(path: Path) -> str:
    text = rel(path).lower()
    name = path.name.lower()
    if "milestone" in text or "roadmap" in text:
        return "milestone_or_roadmap"
    if "history" in text or "development" in text or "briefing" in text:
        return "development_history"
    if "command" in name or "python" in name or "standard" in name:
        return "process_reference"
    if "audit" in name or "manifest" in name or "index" in name:
        return "index_or_audit"
    return "documentation"


def iter_docs() -> Iterable[Path]:
    roots = [REPO_ROOT / "docs", REPO_ROOT]
    seen: set[Path] = set()
    for root in roots:
        if not root.exists():
            continue
        for path in sorted(root.rglob("*")):
            if not path.is_file():
                continue
            if path in seen:
                continue
            if ".git" in path.parts:
                continue
            if path.name in SKIP_NAMES:
                continue
            if path.suffix.lower() not in DOC_EXTENSIONS:
                continue
            if path.parts and any(part in {"PACKAGE_client", "ROOT_SRC_assets"} for part in path.parts):
                continue
            seen.add(path)
            yield path


def build_index() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    for path in iter_docs():
        text = safe_read(path)
        stat = path.stat()
        rows.append({
            "path": rel(path),
            "title": extract_heading(text, path.stem),
            "category": classify(path),
            "extension": path.suffix.lower(),
            "size_bytes": stat.st_size,
            "sha256": sha256(path),
            "mime_type": mimetypes.guess_type(path.name)[0] or "",
            "preview": preview(text),
        })
    rows.sort(key=lambda item: (str(item["category"]), str(item["path"]).lower()))
    return rows


def write_tsv(rows: list[dict[str, object]], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = ["path", "title", "category", "extension", "size_bytes", "sha256", "mime_type", "preview"]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Index repository documentation for dashboard use.")
    parser.add_argument("--output-json", default=str(DEFAULT_OUTPUT_JSON))
    parser.add_argument("--output-tsv", default=str(DEFAULT_OUTPUT_TSV))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    rows = build_index()
    payload = {
        "schema": "mechanist.document_index.v1",
        "document_count": len(rows),
        "documents": rows,
    }
    output_json = Path(args.output_json).resolve()
    output_tsv = Path(args.output_tsv).resolve()
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    write_tsv(rows, output_tsv)
    print(f"Indexed {len(rows)} document(s)")
    print(f"Wrote {output_json}")
    print(f"Wrote {output_tsv}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

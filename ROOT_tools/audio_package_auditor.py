#!/usr/bin/env python3
"""Audit and repair client audio packaging for The Mechanist.

This tool is built around the self-contained-package rule: runtime audio required
by the client must physically exist under PACKAGE_client. The client must not
rely on ROOT_SRC_assets, ROOT_tools, or parent repository folders at runtime.

Default behavior is audit-only.

Useful commands:
  python ROOT_tools/audio_package_auditor.py
  python ROOT_tools/audio_package_auditor.py --copy-missing-references
  python ROOT_tools/audio_package_auditor.py --copy-all-discovered-audio

Outputs:
  docs/audio_package_audit.json
  docs/audio_package_audit.tsv
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import shutil
from dataclasses import asdict, dataclass
from pathlib import Path
from datetime import datetime, timezone
from typing import Iterable

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
PACKAGE_CLIENT = REPO_ROOT / "PACKAGE_client"
DEFAULT_JSON = REPO_ROOT / "docs/audio_package_audit.json"
DEFAULT_TSV = REPO_ROOT / "docs/audio_package_audit.tsv"
DEFAULT_AUDIO_DEST = PACKAGE_CLIENT / "assets/audio"
AUDIO_EXTENSIONS = {".wav", ".ogg", ".mp3", ".aiff", ".aif", ".au", ".mid", ".midi"}
SKIP_DIRS = {".git", "target", "build", "dist", "ROOT_RELEASE"}
JAVA_AUDIO_REFERENCE_RE = re.compile(r"[\"'](?P<path>[^\"']+\.(?:wav|ogg|mp3|aiff|aif|au|mid|midi))[\"']", re.IGNORECASE)
JAVA_AUDIO_KEYWORDS = ("AudioSystem", "Clip", "SourceDataLine", "sound", "music", "volume")


@dataclass
class AudioFileRecord:
    path: str
    package_client: bool
    size_bytes: int
    extension: str
    copy_candidate: bool


@dataclass
class AudioReferenceRecord:
    source_file: str
    line: int
    reference: str
    exists_in_package: bool
    exists_in_repo: bool
    candidate_count: int
    candidates: str
    action: str
    copied_to: str


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def should_skip(path: Path) -> bool:
    return any(part in SKIP_DIRS for part in path.parts)


def iter_files(root: Path, suffixes: set[str]) -> Iterable[Path]:
    if not root.exists():
        return
    for path in sorted(root.rglob("*")):
        if should_skip(path):
            continue
        if path.is_file() and path.suffix.lower() in suffixes:
            yield path


def iter_java_files() -> Iterable[Path]:
    src = REPO_ROOT / "src"
    if not src.exists():
        return
    for path in sorted(src.rglob("*.java")):
        if path.is_file():
            yield path


def collect_audio_files() -> list[AudioFileRecord]:
    records: list[AudioFileRecord] = []
    for path in iter_files(REPO_ROOT, AUDIO_EXTENSIONS):
        in_package = path.resolve().is_relative_to(PACKAGE_CLIENT.resolve())
        records.append(AudioFileRecord(
            path=rel(path),
            package_client=in_package,
            size_bytes=path.stat().st_size,
            extension=path.suffix.lower(),
            copy_candidate=not in_package,
        ))
    return records


def package_path_for_reference(reference: str) -> Path:
    clean = reference.replace("\\", "/").lstrip("/")
    return PACKAGE_CLIENT / clean


def repo_path_for_reference(reference: str) -> Path:
    clean = reference.replace("\\", "/").lstrip("/")
    return REPO_ROOT / clean


def find_candidates(reference: str, audio_records: list[AudioFileRecord]) -> list[Path]:
    basename = Path(reference.replace("\\", "/")).name.lower()
    candidates: list[Path] = []
    for record in audio_records:
        path = REPO_ROOT / record.path
        if path.name.lower() == basename:
            candidates.append(path)
    return candidates


def choose_candidate(candidates: list[Path]) -> Path | None:
    non_package = [path for path in candidates if not path.resolve().is_relative_to(PACKAGE_CLIENT.resolve())]
    if non_package:
        # Prefer ROOT_SRC_assets, then assets, then anything else outside package.
        def score(path: Path) -> tuple[int, int, str]:
            parts = set(path.parts)
            if "ROOT_SRC_assets" in parts:
                priority = 0
            elif "assets" in parts:
                priority = 1
            else:
                priority = 2
            return (priority, len(path.parts), str(path))
        return sorted(non_package, key=score)[0]
    return candidates[0] if candidates else None


def copy_reference_candidate(reference: str, candidate: Path, dry_run: bool) -> Path:
    destination = package_path_for_reference(reference)
    if dry_run:
        return destination
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(candidate, destination)
    return destination


def copy_all_discovered_audio(audio_records: list[AudioFileRecord], dry_run: bool) -> list[tuple[str, str]]:
    copied: list[tuple[str, str]] = []
    for record in audio_records:
        if not record.copy_candidate:
            continue
        source = REPO_ROOT / record.path
        # Preserve enough path structure to avoid collisions while keeping the package self-contained.
        if "ROOT_SRC_assets" in source.parts:
            idx = source.parts.index("ROOT_SRC_assets")
            rel_tail = Path(*source.parts[idx + 1:])
        elif "assets" in source.parts:
            idx = source.parts.index("assets")
            rel_tail = Path(*source.parts[idx + 1:])
        else:
            rel_tail = Path("imported") / Path(record.path)
        destination = DEFAULT_AUDIO_DEST / rel_tail
        copied.append((rel(source), rel(destination)))
        if not dry_run:
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
    return copied


def collect_references(audio_records: list[AudioFileRecord], copy_missing: bool, dry_run: bool) -> list[AudioReferenceRecord]:
    references: list[AudioReferenceRecord] = []
    for java_file in iter_java_files():
        try:
            lines = java_file.read_text(encoding="utf-8", errors="replace").splitlines()
        except Exception:
            continue
        likely_audio_file = any(keyword in "\n".join(lines[:2000]) for keyword in JAVA_AUDIO_KEYWORDS) or True
        if not likely_audio_file:
            continue
        for line_no, line in enumerate(lines, start=1):
            for match in JAVA_AUDIO_REFERENCE_RE.finditer(line):
                reference = match.group("path")
                package_path = package_path_for_reference(reference)
                repo_path = repo_path_for_reference(reference)
                candidates = find_candidates(reference, audio_records)
                candidate = choose_candidate(candidates)
                copied_to = ""
                action = "ok" if package_path.exists() else "missing_from_package"
                if not package_path.exists() and copy_missing and candidate is not None:
                    destination = copy_reference_candidate(reference, candidate, dry_run)
                    copied_to = rel(destination)
                    action = "would_copy_missing_reference" if dry_run else "copied_missing_reference"
                references.append(AudioReferenceRecord(
                    source_file=rel(java_file),
                    line=line_no,
                    reference=reference,
                    exists_in_package=package_path.exists(),
                    exists_in_repo=repo_path.exists(),
                    candidate_count=len(candidates),
                    candidates=";".join(rel(path) for path in candidates[:20]),
                    action=action,
                    copied_to=copied_to,
                ))
    return references


def write_tsv(audio_files: list[AudioFileRecord], references: list[AudioReferenceRecord], copied_all: list[tuple[str, str]], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(["record_type", "field_1", "field_2", "field_3", "field_4", "field_5", "field_6", "field_7", "field_8"])
        for item in audio_files:
            writer.writerow(["audio_file", item.path, item.package_client, item.size_bytes, item.extension, item.copy_candidate, "", "", ""])
        for item in references:
            writer.writerow(["java_reference", item.source_file, item.line, item.reference, item.exists_in_package, item.exists_in_repo, item.candidate_count, item.action, item.copied_to])
        for source, destination in copied_all:
            writer.writerow(["copied_all_audio", source, destination, "", "", "", "", "", ""])


def write_json(audio_files: list[AudioFileRecord], references: list[AudioReferenceRecord], copied_all: list[tuple[str, str]], output: Path, dry_run: bool) -> None:
    payload = {
        "schema": "mechanist.audio_package_audit.v1",
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "dry_run": dry_run,
        "package_client": str(PACKAGE_CLIENT),
        "audio_file_count": len(audio_files),
        "package_audio_file_count": sum(1 for item in audio_files if item.package_client),
        "external_audio_file_count": sum(1 for item in audio_files if not item.package_client),
        "java_audio_reference_count": len(references),
        "missing_package_reference_count": sum(1 for item in references if not item.exists_in_package),
        "copied_all_audio_count": len(copied_all),
        "audio_files": [asdict(item) for item in audio_files],
        "java_references": [asdict(item) for item in references],
        "copied_all_audio": [{"source": source, "destination": destination} for source, destination in copied_all],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, indent=2), encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit and optionally repair self-contained client audio packaging.")
    parser.add_argument("--copy-missing-references", action="store_true", help="Copy best matching source file into PACKAGE_client for Java literal audio references that are missing from package.")
    parser.add_argument("--copy-all-discovered-audio", action="store_true", help="Copy all discovered non-package audio into PACKAGE_client/assets/audio while preserving safe source subpaths.")
    parser.add_argument("--dry-run", action="store_true", help="Show copy actions without writing files.")
    parser.add_argument("--output-json", default=str(DEFAULT_JSON))
    parser.add_argument("--output-tsv", default=str(DEFAULT_TSV))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    audio_files = collect_audio_files()
    references = collect_references(audio_files, args.copy_missing_references, args.dry_run)
    copied_all: list[tuple[str, str]] = []
    if args.copy_all_discovered_audio:
        copied_all = copy_all_discovered_audio(audio_files, args.dry_run)
        # Re-collect after copy for accurate counts on real runs.
        if not args.dry_run:
            audio_files = collect_audio_files()
            references = collect_references(audio_files, False, False)

    output_json = Path(args.output_json).resolve()
    output_tsv = Path(args.output_tsv).resolve()
    write_json(audio_files, references, copied_all, output_json, args.dry_run)
    write_tsv(audio_files, references, copied_all, output_tsv)

    print(f"Audio files discovered:       {len(audio_files)}")
    print(f"Audio files in PACKAGE_client:{sum(1 for item in audio_files if item.package_client)}")
    print(f"Audio refs in Java literals:  {len(references)}")
    print(f"Missing package references:   {sum(1 for item in references if not item.exists_in_package)}")
    print(f"Copy-all actions:             {len(copied_all)}")
    print(f"Wrote JSON:                   {output_json}")
    print(f"Wrote TSV:                    {output_tsv}")
    if args.dry_run:
        print("Dry run only; no files were copied.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""
Repository scan indexer for The Mechanist.

Run from the repository root or from ROOT_tools:

    python ROOT_tools/repository_scan_indexer.py
    python repository_scan_indexer.py

It scans every repository file outside .git and rewrites:

    docs/repository_file_manifest.tsv

The manifest is deterministic and intended to be committed after local asset
correction passes.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import hashlib
import mimetypes
import os
from pathlib import Path
import struct
import sys
from typing import Iterable

DEFAULT_OUTPUT = "docs/repository_file_manifest.tsv"
DEFAULT_EXCLUDED_DIRS = {".git"}
HASH_CHUNK_BYTES = 1024 * 1024
TEXT_SAMPLE_BYTES = 1024 * 1024

COLUMNS = [
    "path",
    "directory",
    "filename",
    "stem",
    "extension",
    "root_area",
    "size_bytes",
    "modified_time_utc",
    "sha256",
    "mime_type",
    "file_family",
    "asset_category",
    "likely_content_role",
    "is_binary",
    "text_encoding",
    "line_count",
    "width_px",
    "height_px",
    "image_mode",
    "has_alpha",
    "tags",
    "scan_note",
]

IMAGE_EXTENSIONS = {
    ".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".tif", ".tiff",
    ".ase", ".aseprite", ".psd", ".kra",
}
AUDIO_EXTENSIONS = {".wav", ".ogg", ".mp3", ".flac", ".m4a", ".aac", ".mid", ".midi"}
VIDEO_EXTENSIONS = {".mp4", ".mov", ".avi", ".webm", ".mkv"}
FONT_EXTENSIONS = {".ttf", ".otf", ".woff", ".woff2", ".fnt"}
ARCHIVE_EXTENSIONS = {".zip", ".7z", ".rar", ".tar", ".gz", ".bz2", ".xz"}
DOCUMENT_EXTENSIONS = {".md", ".txt", ".rst", ".pdf", ".docx", ".odt", ".rtf"}
SPREADSHEET_EXTENSIONS = {".csv", ".tsv", ".xlsx", ".ods"}
CONFIG_EXTENSIONS = {".json", ".jsonl", ".yaml", ".yml", ".toml", ".ini", ".cfg", ".properties"}
CODE_EXTENSIONS = {
    ".java", ".py", ".lua", ".js", ".jsx", ".ts", ".tsx", ".html", ".css",
    ".scss", ".xml", ".gradle", ".bat", ".cmd", ".ps1", ".sh", ".c", ".cpp",
    ".h", ".hpp", ".cs", ".go", ".rs", ".kt", ".kts", ".sql",
}
BINARY_EXTENSIONS = {".class", ".jar", ".dll", ".exe", ".so", ".dylib", ".pdb", ".bin", ".dat"}


def posix(path: Path) -> str:
    return path.as_posix()


def rel(path: Path, root: Path) -> str:
    return posix(path.relative_to(root))


def extension(path: Path) -> str:
    return path.suffix.lower()


def utc_timestamp(timestamp: float) -> str:
    return dt.datetime.fromtimestamp(timestamp, tz=dt.timezone.utc).replace(microsecond=0).isoformat()


def tokens(text: str) -> set[str]:
    cleaned = "".join(ch if ch.isalnum() else " " for ch in text.lower())
    return {part for part in cleaned.split() if part}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(HASH_CHUNK_BYTES), b""):
            digest.update(chunk)
    return digest.hexdigest()


def file_family(path: Path, mime_type: str) -> str:
    ext = extension(path)
    if ext in IMAGE_EXTENSIONS:
        return "image"
    if ext in AUDIO_EXTENSIONS:
        return "audio"
    if ext in VIDEO_EXTENSIONS:
        return "video"
    if ext in FONT_EXTENSIONS:
        return "font"
    if ext in ARCHIVE_EXTENSIONS:
        return "archive"
    if ext in SPREADSHEET_EXTENSIONS:
        return "spreadsheet_or_table"
    if ext in DOCUMENT_EXTENSIONS:
        return "document"
    if ext in CONFIG_EXTENSIONS:
        return "config_or_data"
    if ext in CODE_EXTENSIONS:
        return "source_code"
    if ext in BINARY_EXTENSIONS:
        return "binary"
    if mime_type.startswith("text/"):
        return "text"
    if mime_type.startswith("image/"):
        return "image"
    if mime_type.startswith("audio/"):
        return "audio"
    if mime_type.startswith("video/"):
        return "video"
    return "unknown"


def asset_category(rel_path: str, family: str) -> str:
    path_tokens = tokens(rel_path)
    root_area = rel_path.split("/", 1)[0].lower() if rel_path else ""

    if root_area == "docs":
        return "documentation"
    if root_area in {"root_tools", "roots_tools", "tools", "scripts"}:
        return "developer_tooling"
    if root_area in {"src", "client", "server", "launcher", "installer"} and family == "source_code":
        return "runtime_source"

    if family == "image":
        if {"portrait", "portraits"} & path_tokens:
            return "portrait_asset"
        if {"tile", "tiles", "terrain"} & path_tokens:
            return "tile_or_terrain_asset"
        if {"sprite", "sprites", "atlas"} & path_tokens:
            return "sprite_or_atlas_asset"
        if {"icon", "icons"} & path_tokens:
            return "icon_asset"
        if root_area in {"assets", "client"}:
            return "image_asset"
        return "image_file"

    if family == "audio":
        if "music" in path_tokens:
            return "music_asset"
        if {"sound", "sfx", "audio"} & path_tokens:
            return "sound_effect_asset"
        return "audio_asset"

    if family in {"config_or_data", "spreadsheet_or_table"}:
        if {"manifest", "index"} & path_tokens:
            return "manifest_or_index"
        if {"recipe", "recipes"} & path_tokens:
            return "recipe_data"
        if {"faction", "factions"} & path_tokens:
            return "faction_data"
        return "data_file"

    if family == "document":
        return "documentation_or_reference"
    return "repository_file"


def likely_content_role(rel_path: str, family: str, category: str) -> str:
    path_tokens = tokens(rel_path)
    lower_path = rel_path.lower()
    if "repository_file_manifest" in lower_path:
        return "repository_manifest"
    role_checks = [
        (("manifest",), "manifest"),
        (("index",), "index"),
        (("atlas",), "atlas"),
        (("sprite", "sprites"), "sprite"),
        (("portrait", "portraits"), "portrait"),
        (("icon", "icons"), "icon"),
        (("tile", "tiles"), "tile"),
        (("vehicle", "vehicles", "automotive", "car"), "vehicle_or_automotive_asset"),
        (("weapon", "weapons"), "weapon_asset"),
        (("armor", "armour"), "armor_asset"),
        (("paperwork", "permit", "legal"), "document_item_asset"),
        (("relic", "relics", "valuable", "valuables"), "valuable_or_relic_asset"),
        (("recipe", "recipes"), "recipe_or_crafting_data"),
        (("sound", "sfx"), "sound_effect"),
        (("music",), "music"),
        (("test", "tests"), "test"),
    ]
    for names, role in role_checks:
        if set(names) & path_tokens:
            return role
    if family == "source_code":
        return "source"
    return category


def tag_list(rel_path: str, family: str, category: str, role: str) -> str:
    interesting = {
        "asset", "assets", "atlas", "sprite", "sprites", "icon", "icons", "portrait",
        "portraits", "tile", "tiles", "terrain", "vehicle", "vehicles", "automotive",
        "weapon", "weapons", "armor", "armour", "paperwork", "legal", "permit",
        "relic", "relics", "valuable", "valuables", "creche", "train", "military",
        "sound", "sfx", "music", "recipe", "recipes", "faction", "factions",
        "manifest", "index", "launcher", "client", "server", "installer", "root", "root_tools",
        "tool", "tools", "script", "scripts", "docs", "src",
    }
    found = {family, category, role}
    found.update(token for token in tokens(rel_path) if token in interesting)
    return ",".join(sorted(tag for tag in found if tag))


def text_probe(path: Path, size_bytes: int) -> tuple[str, str, str]:
    try:
        sample = path.read_bytes()[: min(size_bytes, TEXT_SAMPLE_BYTES)]
    except OSError:
        return "true", "", ""
    if b"\x00" in sample:
        return "true", "", ""
    for encoding in ("utf-8-sig", "utf-8", "cp1252"):
        try:
            sample.decode(encoding)
            line_count = ""
            if size_bytes <= 64 * 1024 * 1024:
                with path.open("r", encoding=encoding, errors="replace", newline="") as handle:
                    line_count = str(sum(1 for _ in handle))
            return "false", encoding, line_count
        except UnicodeDecodeError:
            continue
        except OSError:
            return "false", encoding, ""
    return "true", "", ""


def png_dimensions(header: bytes) -> tuple[int, int] | None:
    if len(header) >= 24 and header.startswith(b"\x89PNG\r\n\x1a\n"):
        return struct.unpack(">II", header[16:24])
    return None


def gif_dimensions(header: bytes) -> tuple[int, int] | None:
    if len(header) >= 10 and (header.startswith(b"GIF87a") or header.startswith(b"GIF89a")):
        return struct.unpack("<HH", header[6:10])
    return None


def bmp_dimensions(header: bytes) -> tuple[int, int] | None:
    if len(header) >= 26 and header.startswith(b"BM"):
        width, height = struct.unpack("<ii", header[18:26])
        return abs(width), abs(height)
    return None


def jpeg_dimensions(path: Path) -> tuple[int, int] | None:
    try:
        with path.open("rb") as handle:
            if handle.read(2) != b"\xff\xd8":
                return None
            while True:
                prefix = handle.read(1)
                if not prefix:
                    return None
                if prefix != b"\xff":
                    continue
                marker = handle.read(1)
                while marker == b"\xff":
                    marker = handle.read(1)
                if marker in {b"\xd8", b"\xd9"}:
                    continue
                length_data = handle.read(2)
                if len(length_data) != 2:
                    return None
                segment_length = struct.unpack(">H", length_data)[0]
                if segment_length < 2:
                    return None
                if marker and 0xC0 <= marker[0] <= 0xCF and marker[0] not in {0xC4, 0xC8, 0xCC}:
                    segment = handle.read(segment_length - 2)
                    if len(segment) >= 5:
                        height, width = struct.unpack(">HH", segment[1:5])
                        return width, height
                    return None
                handle.seek(segment_length - 2, os.SEEK_CUR)
    except OSError:
        return None


def image_probe(path: Path, family: str) -> tuple[str, str, str, str]:
    if family != "image":
        return "", "", "", ""
    try:
        from PIL import Image  # type: ignore
        with Image.open(path) as image:
            mode = image.mode
            has_alpha = "true" if mode in {"RGBA", "LA"} or (mode == "P" and "transparency" in image.info) else "false"
            return str(image.width), str(image.height), mode, has_alpha
    except Exception:
        pass
    try:
        header = path.read_bytes()[:64]
    except OSError:
        return "", "", "", ""
    dimensions = png_dimensions(header) or gif_dimensions(header) or bmp_dimensions(header)
    if dimensions is None and extension(path) in {".jpg", ".jpeg"}:
        dimensions = jpeg_dimensions(path)
    if dimensions is None:
        return "", "", "", ""
    return str(dimensions[0]), str(dimensions[1]), "", ""


def iter_files(root: Path, excluded_dirs: set[str]) -> Iterable[Path]:
    for current_root, dir_names, file_names in os.walk(root):
        dir_names[:] = sorted(name for name in dir_names if name not in excluded_dirs)
        for file_name in sorted(file_names):
            path = Path(current_root) / file_name
            if path.is_file():
                yield path


def row_for(path: Path, root: Path, output_rel: str) -> dict[str, str]:
    rel_path = rel(path, root)
    stat = path.stat()
    mime_type = mimetypes.guess_type(path.name)[0] or ""
    family = file_family(path, mime_type)
    category = asset_category(rel_path, family)
    role = likely_content_role(rel_path, family, category)
    width, height, mode, has_alpha = image_probe(path, family)
    is_binary, encoding, line_count = text_probe(path, stat.st_size)

    sha256 = ""
    scan_note = ""
    if rel_path == output_rel:
        scan_note = "generated manifest self-reference; sha256 omitted because file changes during write"
        is_binary = "false"
        encoding = "utf-8"
    else:
        try:
            sha256 = sha256_file(path)
        except OSError as exc:
            scan_note = f"hash_error:{exc}"

    path_obj = Path(rel_path)
    return {
        "path": rel_path,
        "directory": posix(path_obj.parent) if path_obj.parent != Path(".") else "",
        "filename": path.name,
        "stem": path.stem,
        "extension": extension(path),
        "root_area": rel_path.split("/", 1)[0] if "/" in rel_path else "",
        "size_bytes": str(stat.st_size),
        "modified_time_utc": utc_timestamp(stat.st_mtime),
        "sha256": sha256,
        "mime_type": mime_type,
        "file_family": family,
        "asset_category": category,
        "likely_content_role": role,
        "is_binary": is_binary,
        "text_encoding": encoding,
        "line_count": line_count,
        "width_px": width,
        "height_px": height,
        "image_mode": mode,
        "has_alpha": has_alpha,
        "tags": tag_list(rel_path, family, category, role),
        "scan_note": scan_note,
    }


def write_tsv(rows: list[dict[str, str]], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = output_path.with_suffix(output_path.suffix + ".tmp")
    with temp_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=COLUMNS, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    temp_path.replace(output_path)


def default_root() -> Path:
    script_path = Path(__file__).resolve()
    if script_path.parent.name.lower() in {"root_tools", "roots_tools", "tools"}:
        return script_path.parent.parent
    return Path.cwd()


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Index every repository file and regenerate docs/repository_file_manifest.tsv.")
    parser.add_argument("--root", default=None, help="Repository root. Defaults to parent of ROOT_tools when run from ROOT_tools.")
    parser.add_argument("--output", default=DEFAULT_OUTPUT, help=f"Manifest TSV path relative to repository root. Default: {DEFAULT_OUTPUT}")
    parser.add_argument("--exclude-dir", action="append", default=[], help="Directory basename to exclude. Can be supplied repeatedly.")
    parser.add_argument("--include-git", action="store_true", help="Include .git internals. Not recommended.")
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    root = Path(args.root).resolve() if args.root else default_root().resolve()
    if not root.exists() or not root.is_dir():
        print(f"Repository root does not exist or is not a directory: {root}", file=sys.stderr)
        return 2

    excluded_dirs = set(args.exclude_dir)
    if not args.include_git:
        excluded_dirs.update(DEFAULT_EXCLUDED_DIRS)

    output_rel = posix(Path(args.output))
    output_path = root / output_rel

    rows: list[dict[str, str]] = []
    for file_path in iter_files(root, excluded_dirs):
        try:
            rows.append(row_for(file_path, root, output_rel))
        except Exception as exc:
            rel_path = rel(file_path, root)
            path_obj = Path(rel_path)
            rows.append({
                "path": rel_path,
                "directory": posix(path_obj.parent) if path_obj.parent != Path(".") else "",
                "filename": file_path.name,
                "stem": file_path.stem,
                "extension": extension(file_path),
                "root_area": rel_path.split("/", 1)[0] if "/" in rel_path else "",
                "size_bytes": "",
                "modified_time_utc": "",
                "sha256": "",
                "mime_type": mimetypes.guess_type(file_path.name)[0] or "",
                "file_family": "scan_error",
                "asset_category": "scan_error",
                "likely_content_role": "scan_error",
                "is_binary": "",
                "text_encoding": "",
                "line_count": "",
                "width_px": "",
                "height_px": "",
                "image_mode": "",
                "has_alpha": "",
                "tags": "scan_error",
                "scan_note": f"scan_error:{exc}",
            })

    rows.sort(key=lambda item: item["path"].lower())
    write_tsv(rows, output_path)

    family_counts: dict[str, int] = {}
    category_counts: dict[str, int] = {}
    for row in rows:
        family_counts[row["file_family"]] = family_counts.get(row["file_family"], 0) + 1
        category_counts[row["asset_category"]] = category_counts.get(row["asset_category"], 0) + 1

    print(f"Wrote {len(rows)} repository file rows to {output_path}")
    print("File families:")
    for name, count in sorted(family_counts.items()):
        print(f"  {name}: {count}")
    print("Asset categories:")
    for name, count in sorted(category_counts.items()):
        print(f"  {name}: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

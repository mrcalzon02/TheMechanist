#!/usr/bin/env python3
"""
Repository scan indexer for The Mechanist.

Walks the repository working tree, indexes every file outside .git, and rewrites
a deterministic TSV manifest at docs/repository_file_manifest.tsv by default.

The scanner is intentionally self-contained. Pillow is used for richer image
metadata when available, but the script still reports basic PNG/JPEG/GIF/BMP
dimensions without third-party dependencies.
"""

from __future__ import annotations

import argparse
import csv
import datetime as _dt
import hashlib
import mimetypes
import os
from pathlib import Path
import struct
import sys
from typing import Iterable


DEFAULT_OUTPUT = "docs/repository_file_manifest.tsv"
DEFAULT_EXCLUDED_DIRS = {".git"}
TEXT_SAMPLE_BYTES = 1024 * 1024
HASH_CHUNK_BYTES = 1024 * 1024


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
BINARY_EXTENSIONS = {
    ".class", ".jar", ".dll", ".exe", ".so", ".dylib", ".pdb", ".bin", ".dat",
}


def utc_iso_from_timestamp(timestamp: float) -> str:
    return _dt.datetime.fromtimestamp(timestamp, tz=_dt.timezone.utc).replace(microsecond=0).isoformat()


def stable_posix(path: Path) -> str:
    return path.as_posix()


def rel_to_root(path: Path, root: Path) -> str:
    return stable_posix(path.relative_to(root))


def normalize_extension(path: Path) -> str:
    return path.suffix.lower()


def split_tokens(path_text: str) -> set[str]:
    cleaned = []
    for ch in path_text.lower():
        cleaned.append(ch if ch.isalnum() else " ")
    return {part for part in "".join(cleaned).split() if part}


def guess_file_family(path: Path, mime_type: str) -> str:
    ext = normalize_extension(path)
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


def guess_asset_category(rel_path: str, file_family: str) -> str:
    tokens = split_tokens(rel_path)
    root_area = rel_path.split("/", 1)[0].lower() if rel_path else ""

    if root_area == "docs":
        return "documentation"
    if root_area == "tools" or root_area == "scripts":
        return "developer_tooling"
    if root_area in {"src", "client", "server", "launcher", "installer"} and file_family == "source_code":
        return "runtime_source"

    if file_family == "image":
        if "portrait" in tokens or "portraits" in tokens:
            return "portrait_asset"
        if "tile" in tokens or "tiles" in tokens or "terrain" in tokens:
            return "tile_or_terrain_asset"
        if "sprite" in tokens or "sprites" in tokens or "atlas" in tokens:
            return "sprite_or_atlas_asset"
        if "icon" in tokens or "icons" in tokens:
            return "icon_asset"
        if root_area in {"assets", "client"}:
            return "image_asset"
        return "image_file"

    if file_family == "audio":
        if "music" in tokens:
            return "music_asset"
        if "sound" in tokens or "sfx" in tokens or "audio" in tokens:
            return "sound_effect_asset"
        return "audio_asset"

    if file_family in {"config_or_data", "spreadsheet_or_table"}:
        if "manifest" in tokens or "index" in tokens:
            return "manifest_or_index"
        if "recipe" in tokens or "recipes" in tokens:
            return "recipe_data"
        if "faction" in tokens or "factions" in tokens:
            return "faction_data"
        return "data_file"

    if file_family == "document":
        return "documentation_or_reference"

    return "repository_file"


def guess_likely_content_role(rel_path: str, file_family: str, asset_category: str) -> str:
    tokens = split_tokens(rel_path)
    if "repository_file_manifest" in rel_path.lower():
        return "repository_manifest"
    if "manifest" in tokens:
        return "manifest"
    if "index" in tokens:
        return "index"
    if "atlas" in tokens:
        return "atlas"
    if "sprite" in tokens or "sprites" in tokens:
        return "sprite"
    if "portrait" in tokens or "portraits" in tokens:
        return "portrait"
    if "icon" in tokens or "icons" in tokens:
        return "icon"
    if "tile" in tokens or "tiles" in tokens:
        return "tile"
    if "vehicle" in tokens or "automotive" in tokens or "car" in tokens:
        return "vehicle_or_automotive_asset"
    if "weapon" in tokens or "weapons" in tokens:
        return "weapon_asset"
    if "armor" in tokens or "armour" in tokens:
        return "armor_asset"
    if "paperwork" in tokens or "permit" in tokens or "legal" in tokens:
        return "document_item_asset"
    if "relic" in tokens or "valuables" in tokens or "valuable" in tokens:
        return "valuable_or_relic_asset"
    if "recipe" in tokens or "recipes" in tokens:
        return "recipe_or_crafting_data"
    if "sound" in tokens or "sfx" in tokens:
        return "sound_effect"
    if "music" in tokens:
        return "music"
    if "test" in tokens or "tests" in tokens:
        return "test"
    if file_family == "source_code":
        return "source"
    return asset_category


def build_tags(rel_path: str, file_family: str, asset_category: str, likely_content_role: str) -> list[str]:
    tokens = split_tokens(rel_path)
    tags: set[str] = {file_family, asset_category, likely_content_role}

    interesting_tokens = {
        "asset", "assets", "atlas", "sprite", "sprites", "icon", "icons", "portrait",
        "portraits", "tile", "tiles", "terrain", "vehicle", "vehicles", "automotive",
        "weapon", "weapons", "armor", "armour", "paperwork", "legal", "permit",
        "relic", "relics", "valuable", "valuables", "creche", "train", "military",
        "sound", "sfx", "music", "recipe", "recipes", "faction", "factions",
        "manifest", "index", "launcher", "client", "server", "installer", "tool",
        "tools", "script", "scripts", "docs", "src",
    }
    tags.update(token for token in tokens if token in interesting_tokens)
    return sorted(tag for tag in tags if tag)


def hash_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(HASH_CHUNK_BYTES), b""):
            digest.update(chunk)
    return digest.hexdigest()


def probe_text(path: Path, size_bytes: int) -> tuple[bool, str, str]:
    """
    Returns (is_binary, encoding, line_count).

    line_count is an empty string for binary files or very large undecodable files.
    """
    try:
        sample = path.read_bytes()[: min(size_bytes, TEXT_SAMPLE_BYTES)]
    except OSError:
        return True, "", ""

    if b"\x00" in sample:
        return True, "", ""

    for encoding in ("utf-8-sig", "utf-8", "cp1252"):
        try:
            sample.decode(encoding)
            if size_bytes <= 64 * 1024 * 1024:
                with path.open("r", encoding=encoding, errors="replace", newline="") as handle:
                    return False, encoding, str(sum(1 for _ in handle))
            return False, encoding, ""
        except UnicodeDecodeError:
            continue
        except OSError:
            return False, encoding, ""

    return True, "", ""


def png_dimensions(data: bytes) -> tuple[int, int] | None:
    if len(data) >= 24 and data.startswith(b"\x89PNG\r\n\x1a\n"):
        width, height = struct.unpack(">II", data[16:24])
        return int(width), int(height)
    return None


def gif_dimensions(data: bytes) -> tuple[int, int] | None:
    if len(data) >= 10 and (data.startswith(b"GIF87a") or data.startswith(b"GIF89a")):
        width, height = struct.unpack("<HH", data[6:10])
        return int(width), int(height)
    return None


def bmp_dimensions(data: bytes) -> tuple[int, int] | None:
    if len(data) >= 26 and data.startswith(b"BM"):
        width, height = struct.unpack("<ii", data[18:26])
        return int(abs(width)), int(abs(height))
    return None


def jpeg_dimensions(path: Path) -> tuple[int, int] | None:
    try:
        with path.open("rb") as handle:
            if handle.read(2) != b"\xff\xd8":
                return None
            while True:
                marker_prefix = handle.read(1)
                if not marker_prefix:
                    return None
                if marker_prefix != b"\xff":
                    continue
                marker = handle.read(1)
                while marker == b"\xff":
                    marker = handle.read(1)
                if marker in {b"\xd8", b"\xd9"}:
                    continue
                length_bytes = handle.read(2)
                if len(length_bytes) != 2:
                    return None
                segment_length = struct.unpack(">H", length_bytes)[0]
                if segment_length < 2:
                    return None
                if marker and 0xC0 <= marker[0] <= 0xCF and marker[0] not in {0xC4, 0xC8, 0xCC}:
                    segment = handle.read(segment_length - 2)
                    if len(segment) >= 5:
                        height, width = struct.unpack(">HH", segment[1:5])
                        return int(width), int(height)
                    return None
                handle.seek(segment_length - 2, os.SEEK_CUR)
    except OSError:
        return None


def probe_image(path: Path, file_family: str) -> tuple[str, str, str, str]:
    """
    Returns width, height, image_mode, has_alpha as strings.

    Uses Pillow if present, otherwise falls back to lightweight parsing for
    common formats.
    """
    if file_family != "image":
        return "", "", "", ""

    try:
        from PIL import Image  # type: ignore

        with Image.open(path) as image:
            mode = image.mode
            has_alpha = "true" if (
                mode in {"RGBA", "LA"}
                or (mode == "P" and "transparency" in image.info)
            ) else "false"
            return str(image.width), str(image.height), mode, has_alpha
    except Exception:
        pass

    try:
        header = path.read_bytes()[:64]
    except OSError:
        return "", "", "", ""

    dimensions = png_dimensions(header) or gif_dimensions(header) or bmp_dimensions(header)
    mode = ""
    has_alpha = ""
    if dimensions is None and normalize_extension(path) in {".jpg", ".jpeg"}:
        dimensions = jpeg_dimensions(path)
    if dimensions is None:
        return "", "", mode, has_alpha

    return str(dimensions[0]), str(dimensions[1]), mode, has_alpha


def should_skip_dir(dir_name: str, excluded_dirs: set[str]) -> bool:
    return dir_name in excluded_dirs


def iter_repository_files(root: Path, excluded_dirs: set[str]) -> Iterable[Path]:
    for current_root, dir_names, file_names in os.walk(root):
        dir_names[:] = sorted(
            dirname for dirname in dir_names
            if not should_skip_dir(dirname, excluded_dirs)
        )
        for file_name in sorted(file_names):
            path = Path(current_root) / file_name
            if path.is_file():
                yield path


def row_for_file(path: Path, root: Path, output_rel: str) -> dict[str, str]:
    rel_path = rel_to_root(path, root)
    stat = path.stat()
    mime_type = mimetypes.guess_type(path.name)[0] or ""
    file_family = guess_file_family(path, mime_type)
    asset_category = guess_asset_category(rel_path, file_family)
    likely_content_role = guess_likely_content_role(rel_path, file_family, asset_category)
    tags = build_tags(rel_path, file_family, asset_category, likely_content_role)
    width, height, image_mode, has_alpha = probe_image(path, file_family)

    scan_note = ""
    sha256 = ""
    is_binary = ""
    text_encoding = ""
    line_count = ""

    if rel_path == output_rel:
        scan_note = "generated manifest self-reference; sha256 omitted because file changes during write"
        is_binary = "false"
        text_encoding = "utf-8"
    else:
        try:
            sha256 = hash_file(path)
        except OSError as exc:
            scan_note = f"hash_error:{exc}"
        try:
            binary, encoding, lines = probe_text(path, stat.st_size)
            is_binary = "true" if binary else "false"
            text_encoding = encoding
            line_count = lines
        except OSError as exc:
            scan_note = f"{scan_note};text_probe_error:{exc}".strip(";")

    root_area = rel_path.split("/", 1)[0] if "/" in rel_path else ""

    return {
        "path": rel_path,
        "directory": stable_posix(Path(rel_path).parent) if Path(rel_path).parent != Path(".") else "",
        "filename": path.name,
        "stem": path.stem,
        "extension": normalize_extension(path),
        "root_area": root_area,
        "size_bytes": str(stat.st_size),
        "modified_time_utc": utc_iso_from_timestamp(stat.st_mtime),
        "sha256": sha256,
        "mime_type": mime_type,
        "file_family": file_family,
        "asset_category": asset_category,
        "likely_content_role": likely_content_role,
        "is_binary": is_binary,
        "text_encoding": text_encoding,
        "line_count": line_count,
        "width_px": width,
        "height_px": height,
        "image_mode": image_mode,
        "has_alpha": has_alpha,
        "tags": ",".join(tags),
        "scan_note": scan_note,
    }


def write_manifest(rows: list[dict[str, str]], output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = output_path.with_suffix(output_path.suffix + ".tmp")
    with temp_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=COLUMNS,
            delimiter="\t",
            lineterminator="\n",
            extrasaction="ignore",
        )
        writer.writeheader()
        for row in rows:
            writer.writerow(row)
    temp_path.replace(output_path)


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Index every repository file and regenerate docs/repository_file_manifest.tsv."
    )
    parser.add_argument(
        "--root",
        default=None,
        help="Repository root. Defaults to the parent of this script's tools directory when possible, otherwise the current working directory.",
    )
    parser.add_argument(
        "--output",
        default=DEFAULT_OUTPUT,
        help=f"Manifest TSV path relative to the repository root. Default: {DEFAULT_OUTPUT}",
    )
    parser.add_argument(
        "--exclude-dir",
        action="append",
        default=[],
        help="Directory basename to exclude from the scan. Can be supplied multiple times. .git is always excluded unless --include-git is used.",
    )
    parser.add_argument(
        "--include-git",
        action="store_true",
        help="Include .git internals. Not recommended for the repository manifest.",
    )
    return parser.parse_args(argv)


def default_root() -> Path:
    script_path = Path(__file__).resolve()
    if script_path.parent.name == "tools":
        return script_path.parent.parent
    return Path.cwd()


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    root = Path(args.root).resolve() if args.root else default_root().resolve()
    if not root.exists() or not root.is_dir():
        print(f"Repository root does not exist or is not a directory: {root}", file=sys.stderr)
        return 2

    excluded_dirs = set(args.exclude_dir)
    if not args.include_git:
        excluded_dirs.update(DEFAULT_EXCLUDED_DIRS)

    output_rel = stable_posix(Path(args.output))
    output_path = root / output_rel

    rows: list[dict[str, str]] = []
    for path in iter_repository_files(root, excluded_dirs):
        try:
            rows.append(row_for_file(path, root, output_rel))
        except Exception as exc:  # keep one bad file from killing the whole index
            rel_path = rel_to_root(path, root)
            rows.append({
                "path": rel_path,
                "directory": stable_posix(Path(rel_path).parent) if Path(rel_path).parent != Path(".") else "",
                "filename": path.name,
                "stem": path.stem,
                "extension": normalize_extension(path),
                "root_area": rel_path.split("/", 1)[0] if "/" in rel_path else "",
                "size_bytes": "",
                "modified_time_utc": "",
                "sha256": "",
                "mime_type": mimetypes.guess_type(path.name)[0] or "",
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

    rows.sort(key=lambda row: row["path"].lower())
    write_manifest(rows, output_path)

    family_counts: dict[str, int] = {}
    category_counts: dict[str, int] = {}
    for row in rows:
        family_counts[row["file_family"]] = family_counts.get(row["file_family"], 0) + 1
        category_counts[row["asset_category"]] = category_counts.get(row["asset_category"], 0) + 1

    print(f"Wrote {len(rows)} repository file rows to {output_path}")
    print("File families:")
    for family, count in sorted(family_counts.items()):
        print(f"  {family}: {count}")
    print("Asset categories:")
    for category, count in sorted(category_counts.items()):
        print(f"  {category}: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

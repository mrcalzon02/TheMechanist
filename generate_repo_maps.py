from pathlib import Path
import subprocess
import json
import hashlib

CHUNK_MAX_BYTES = 200_000

CODE_SUFFIXES = {
    ".java", ".xml", ".md", ".txt", ".json", ".yml", ".yaml",
    ".properties", ".conf", ".args", ".sh", ".bat", ".ps1",
    ".csv", ".tsv", ".html", ".css", ".js", ".lua"
}

BINARY_ASSET_SUFFIXES = {
    ".png", ".jpg", ".jpeg", ".gif", ".webp", ".ico", ".bmp",
    ".wav", ".mp3", ".ogg", ".flac",
    ".jar", ".zip", ".7z", ".exe", ".dll", ".so", ".dylib",
    ".class"
}

EXCLUDE_SOURCE_PREFIXES = (
    "diagnostics/",
    "target/",
    "build/",
    "out/",
    "dist/",
)

GENERATED_PREFIX = "ROOT_tools/repo_maps/"

def repo_root() -> Path:
    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        check=True,
        capture_output=True,
        text=True,
    )
    return Path(result.stdout.strip())

def write_text(path: Path, lines):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + ("\n" if lines else ""), encoding="utf-8")

root = repo_root()
out = root / "ROOT_tools" / "repo_maps"
out.mkdir(parents=True, exist_ok=True)

raw = subprocess.run(
    ["git", "ls-files", "-z"],
    cwd=root,
    check=True,
    capture_output=True,
).stdout

paths = sorted(
    [
        p.decode("utf-8", errors="replace").replace("\\", "/")
        for p in raw.split(b"\0")
        if p
    ],
    key=str.lower,
)

# Avoid indexing generated chunk files as they multiply forever.
paths = [p for p in paths if not p.startswith(GENERATED_PREFIX)]

java_paths = [p for p in paths if p.endswith(".java")]

source_paths = []
for p in paths:
    lower = p.lower()
    suffix = Path(lower).suffix
    if lower.startswith(EXCLUDE_SOURCE_PREFIXES):
        continue
    if suffix in BINARY_ASSET_SUFFIXES:
        continue
    if suffix in CODE_SUFFIXES:
        source_paths.append(p)

write_text(out / "java_file_index.txt", java_paths)
write_text(out / "source_file_index.txt", source_paths)
write_text(out / "repo_file_index_full.txt", paths)

# Chunk the full repo index into connector-readable pieces.
chunks_dir = out / "chunks"
chunks_dir.mkdir(parents=True, exist_ok=True)

for old in chunks_dir.glob("repo_file_index_chunk_*.txt"):
    old.unlink()

chunk = []
chunk_bytes = 0
chunk_no = 1

for p in paths:
    line_bytes = len((p + "\n").encode("utf-8"))
    if chunk and chunk_bytes + line_bytes > CHUNK_MAX_BYTES:
        write_text(chunks_dir / f"repo_file_index_chunk_{chunk_no:04d}.txt", chunk)
        chunk_no += 1
        chunk = []
        chunk_bytes = 0
    chunk.append(p)
    chunk_bytes += line_bytes

if chunk:
    write_text(chunks_dir / f"repo_file_index_chunk_{chunk_no:04d}.txt", chunk)

summary = {
    "repo_root": root.name,
    "total_tracked_files": len(paths),
    "java_files": len(java_paths),
    "source_like_files": len(source_paths),
    "full_index_chunks": chunk_no,
    "chunk_max_bytes": CHUNK_MAX_BYTES,
    "primary_maps": [
        "ROOT_tools/repo_maps/java_file_index.txt",
        "ROOT_tools/repo_maps/source_file_index.txt",
        "ROOT_tools/repo_maps/repo_file_index_full.txt",
        "ROOT_tools/repo_maps/chunks/repo_file_index_chunk_0001.txt",
    ],
}

(out / "README.md").write_text(
    "# Repository Maps\n\n"
    "These files exist so connector-based assistants can navigate this repository without GitHub code search.\n\n"
    "- `java_file_index.txt` - Java source paths only.\n"
    "- `source_file_index.txt` - source/config/docs/text-like paths, excluding diagnostics and binary assets.\n"
    "- `repo_file_index_full.txt` - all tracked paths; may be too large for some connector fetches.\n"
    "- `chunks/` - full tracked path index split into connector-readable chunks.\n\n"
    "Use `java_file_index.txt` first for code work.\n",
    encoding="utf-8",
)

(out / "manifest_summary.json").write_text(
    json.dumps(summary, indent=2) + "\n",
    encoding="utf-8",
)

print(json.dumps(summary, indent=2))

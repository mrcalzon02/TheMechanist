from pathlib import Path
import json
import hashlib
import subprocess
import sys

def repo_root() -> Path:
    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        check=True,
        capture_output=True,
        text=True,
    )
    return Path(result.stdout.strip())

root = repo_root()
out = root / "ROOT_tools"
out.mkdir(parents=True, exist_ok=True)

generated = {
    "ROOT_tools/repo_file_index.txt",
    "ROOT_tools/repo_tree.txt",
    "ROOT_tools/repo_file_manifest.json",
}

result = subprocess.run(
    ["git", "ls-files", "-z"],
    cwd=root,
    check=True,
    capture_output=True,
)

paths = [
    p.decode("utf-8", errors="replace")
    for p in result.stdout.split(b"\0")
    if p
]

paths = [p.replace("\\", "/") for p in paths if p.replace("\\", "/") not in generated]
paths = sorted(paths, key=str.lower)

dirs = set()
files = []

for rel in paths:
    p = root / rel

    parent = Path(rel).parent
    while str(parent) not in ("", "."):
        dirs.add(parent.as_posix() + "/")
        parent = parent.parent

    entry = {
        "path": rel,
        "size_bytes": None,
        "sha256": None,
    }

    if p.exists() and p.is_file():
        data = p.read_bytes()
        entry["size_bytes"] = len(data)
        entry["sha256"] = hashlib.sha256(data).hexdigest()

    files.append(entry)

dirs = sorted(dirs, key=str.lower)

(out / "repo_file_index.txt").write_text(
    "\n".join(paths) + "\n",
    encoding="utf-8",
)

(out / "repo_tree.txt").write_text(
    "\n".join(sorted(list(dirs) + paths, key=str.lower)) + "\n",
    encoding="utf-8",
)

(out / "repo_file_manifest.json").write_text(
    json.dumps(
        {
            "repo_root": root.name,
            "generated_by": "ROOT_tools repo index generator",
            "directory_count": len(dirs),
            "file_count": len(files),
            "directories": dirs,
            "files": files,
        },
        indent=2,
    ) + "\n",
    encoding="utf-8",
)

print(f"Repo root: {root}")
print(f"Wrote {len(files)} tracked files and {len(dirs)} directories into ROOT_tools/")

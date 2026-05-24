#!/usr/bin/env python3
"""Bootstrap LWJGL runtime jars for The Mechanist."""
from __future__ import annotations

import argparse
import os
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

VERSION = "3.4.1"
REPOSITORY = "https://repo1.maven.org/maven2"
MODULES = ("lwjgl", "lwjgl-glfw", "lwjgl-opengl", "lwjgl-stb")
PLATFORMS = {"windows": "natives-windows", "linux": "natives-linux"}


def app_root() -> Path:
    return Path(__file__).resolve().parents[2]


def artifact_filename(module: str, classifier: str | None = None) -> str:
    suffix = f"-{classifier}" if classifier else ""
    return f"{module}-{VERSION}{suffix}.jar"


def artifact_url(module: str, classifier: str | None = None) -> str:
    filename = artifact_filename(module, classifier)
    return f"{REPOSITORY}/org/lwjgl/{module}/{VERSION}/{filename}"


def required_artifacts(platform: str) -> list[tuple[str, str]]:
    platforms = ["windows", "linux"] if platform == "all" else [platform]
    result: list[tuple[str, str]] = []
    for module in MODULES:
        result.append((artifact_filename(module), artifact_url(module)))
    for name in platforms:
        classifier = PLATFORMS[name]
        for module in MODULES:
            result.append((artifact_filename(module, classifier), artifact_url(module, classifier)))
    return result


def valid_jar(path: Path) -> bool:
    try:
        if not path.is_file() or path.stat().st_size < 128:
            return False
        with path.open("rb") as fh:
            return fh.read(4) == b"PK\x03\x04"
    except OSError:
        return False


def download(url: str, destination: Path, retries: int = 3, timeout: int = 45) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    last_error: BaseException | None = None
    for attempt in range(1, retries + 1):
        tmp_fd, tmp_name = tempfile.mkstemp(prefix=destination.name + ".", suffix=".tmp", dir=str(destination.parent))
        os.close(tmp_fd)
        tmp = Path(tmp_name)
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "TheMechanist-LWJGL-Bootstrap/0.9.10iy"})
            with urllib.request.urlopen(req, timeout=timeout) as response, tmp.open("wb") as out:
                status = getattr(response, "status", 200)
                if status and int(status) >= 400:
                    raise RuntimeError(f"HTTP {status}")
                while True:
                    chunk = response.read(1024 * 256)
                    if not chunk:
                        break
                    out.write(chunk)
            if not valid_jar(tmp):
                raise RuntimeError(f"downloaded file is not a valid jar: {url}")
            tmp.replace(destination)
            return
        except (urllib.error.URLError, OSError, RuntimeError) as exc:
            last_error = exc
            try:
                tmp.unlink(missing_ok=True)
            except OSError:
                pass
            if attempt < retries:
                time.sleep(0.75 * attempt)
    raise RuntimeError(f"failed to download {url}: {last_error}")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Download pinned LWJGL runtime jars used by The Mechanist.")
    default_platform = "windows" if os.name == "nt" else "linux"
    parser.add_argument("--platform", choices=("windows", "linux", "all"), default=default_platform)
    parser.add_argument("--root", default=str(app_root()))
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args(argv)

    root = Path(args.root).resolve()
    lib = root / "lib" / "lwjgl"
    lib.mkdir(parents=True, exist_ok=True)
    failures: list[str] = []
    fetched = 0
    skipped = 0
    print(f"LWJGL bootstrap: version={VERSION} platform={args.platform} root={root}")
    for filename, url in required_artifacts(args.platform):
        destination = lib / filename
        if not args.force and valid_jar(destination):
            print(f"  present: {destination.relative_to(root)}")
            skipped += 1
            continue
        print(f"  fetch:   {filename}")
        try:
            download(url, destination)
            fetched += 1
        except RuntimeError as exc:
            failures.append(str(exc))
            print(f"  ERROR:   {exc}", file=sys.stderr)
    if failures:
        print("LWJGL bootstrap failed; missing runtime jars must be installed before the client can start.", file=sys.stderr)
        return 23
    print(f"LWJGL bootstrap complete: fetched={fetched} present={skipped} dir={lib}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

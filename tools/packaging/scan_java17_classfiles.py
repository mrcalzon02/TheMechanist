#!/usr/bin/env python3
"""Scan .class and .jar files for Java 17 classfile compatibility.

Java 17 uses classfile major version 61. The packaging pipeline should fail if any
shipped class exceeds that value.
"""

from __future__ import annotations

import argparse
import pathlib
import struct
import sys
import zipfile

JAVA17_MAJOR = 61


def class_major(data: bytes) -> int | None:
    if len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe":
        return None
    _minor, major = struct.unpack(">HH", data[4:8])
    return major


def scan_class(path: pathlib.Path):
    major = class_major(path.read_bytes())
    if major is not None:
        yield str(path), major


def scan_jar(path: pathlib.Path):
    with zipfile.ZipFile(path) as zf:
        for name in zf.namelist():
            if not name.endswith(".class"):
                continue
            major = class_major(zf.read(name))
            if major is not None:
                yield f"{path}!{name}", major


def scan_path(path: pathlib.Path):
    if path.is_dir():
        for child in path.rglob("*"):
            if child.is_file() and child.suffix.lower() == ".class":
                yield from scan_class(child)
            elif child.is_file() and child.suffix.lower() == ".jar":
                yield from scan_jar(child)
    elif path.suffix.lower() == ".class":
        yield from scan_class(path)
    elif path.suffix.lower() == ".jar":
        yield from scan_jar(path)


def main() -> int:
    parser = argparse.ArgumentParser(description="Scan Java classfile major versions.")
    parser.add_argument("paths", nargs="+", help="Jar, class, or directory paths to scan.")
    parser.add_argument("--max-major", type=int, default=JAVA17_MAJOR)
    args = parser.parse_args()

    total = 0
    failures: list[tuple[str, int]] = []
    for raw in args.paths:
        path = pathlib.Path(raw)
        if not path.exists():
            print(f"ERROR: path does not exist: {path}", file=sys.stderr)
            return 2
        for source, major in scan_path(path):
            total += 1
            if major > args.max_major:
                failures.append((source, major))

    print("============================================================")
    print("The Mechanist Java 17 Classfile Scan")
    print("============================================================")
    print(f"Scanned classfiles: {total}")
    print(f"Maximum allowed major: {args.max_major}")
    if failures:
        print(f"FAILED: {len(failures)} classfiles exceed Java 17 compatibility", file=sys.stderr)
        for source, major in failures[:50]:
            print(f"  major={major} {source}", file=sys.stderr)
        return 1
    print("PASS: all scanned classfiles are Java 17-compatible.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

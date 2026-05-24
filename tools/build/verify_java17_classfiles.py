#!/usr/bin/env python3
"""Verify that Java .class files in jars/directories target Java 17 or lower.

This release gate prevents a package compiled on a newer local JDK from being
shipped as "Java 17 compatible" when its classfile major version is too new for
Java 17 runtimes. Java 17 classfiles have major version 61.
"""
from __future__ import annotations

import argparse
import pathlib
import struct
import sys
import zipfile

JAVA17_MAX_MAJOR = 61
CLASS_MAGIC = b"\xca\xfe\xba\xbe"


def class_major(data: bytes, label: str) -> int | None:
    if len(data) < 8:
        print(f"WARN: {label}: too short to be a classfile", file=sys.stderr)
        return None
    if data[:4] != CLASS_MAGIC:
        print(f"WARN: {label}: missing classfile magic", file=sys.stderr)
        return None
    _minor, major = struct.unpack(">HH", data[4:8])
    return major


def iter_class_entries(path: pathlib.Path):
    if path.is_dir():
        for child in sorted(path.rglob("*.class")):
            rel = child.relative_to(path)
            yield f"{path}:{rel}", child.read_bytes()
    elif path.suffix.lower() == ".jar" or zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as jar:
            for name in sorted(jar.namelist()):
                if name.endswith(".class"):
                    yield f"{path}!/{name}", jar.read(name)
    elif path.suffix.lower() == ".class":
        yield str(path), path.read_bytes()
    else:
        print(f"WARN: skipping unsupported path {path}", file=sys.stderr)


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify Java classfile major versions are Java 17 compatible.")
    parser.add_argument("paths", nargs="+", help="Jars, class files, or directories containing .class files to scan")
    parser.add_argument("--max-major", type=int, default=JAVA17_MAX_MAJOR, help="Maximum allowed classfile major version; Java 17 is 61")
    args = parser.parse_args()

    scanned = 0
    highest = 0
    offenders: list[tuple[str, int]] = []

    for raw in args.paths:
        path = pathlib.Path(raw)
        if not path.exists():
            print(f"ERROR: missing path {path}", file=sys.stderr)
            return 2
        for label, data in iter_class_entries(path):
            major = class_major(data, label)
            if major is None:
                continue
            scanned += 1
            highest = max(highest, major)
            if major > args.max_major:
                offenders.append((label, major))

    print(f"Scanned {scanned} classfile(s). Highest major version: {highest}. Allowed maximum: {args.max_major}.")
    if offenders:
        print("ERROR: Java 17 classfile gate failed; these classes exceed Java 17 major version 61:", file=sys.stderr)
        for label, major in offenders[:200]:
            print(f"  major {major}: {label}", file=sys.stderr)
        if len(offenders) > 200:
            print(f"  ... {len(offenders) - 200} more offender(s)", file=sys.stderr)
        return 1
    print("Java 17 classfile gate passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

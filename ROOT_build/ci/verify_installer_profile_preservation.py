#!/usr/bin/env python3
"""Verify byte-for-byte mutable profile preservation across native installer operations.

The test owns only two dedicated sentinel files outside the installed application.
A baseline stored in CI evidence is compared after maintenance reinstall and purge.
It does not claim to exercise a real user's historical saves or a version upgrade.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import secrets
import sys
import traceback

SCHEMA = 1
SENTINELS = (
    "TheMechanist/installed-game-storage/installer-preservation-test.sav",
    "TheMechanist/profiles/installer-preservation-test.profile",
)


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def assert_location(profile: pathlib.Path, image: pathlib.Path) -> None:
    if profile == image or image in profile.parents:
        raise RuntimeError("profile root is inside installed application")
    if profile in image.parents:
        raise RuntimeError("installed application is inside mutable profile root")


def sentinel_path(profile: pathlib.Path, image: pathlib.Path, relative: str) -> pathlib.Path:
    path = profile / relative
    resolved = path.resolve()
    if profile not in resolved.parents or image == resolved or image in resolved.parents:
        raise RuntimeError("sentinel escapes mutable profile or enters installed application")
    if path.is_symlink():
        raise RuntimeError("sentinel may not be a symlink")
    return path


def capture(profile: pathlib.Path, image: pathlib.Path, commit: str, baseline: pathlib.Path) -> dict:
    profile = profile.resolve()
    image = image.resolve()
    assert_location(profile, image)
    if not image.is_dir():
        raise RuntimeError("installed application is missing at capture time")
    if baseline.exists():
        raise RuntimeError("baseline already exists; refusing to overwrite evidence")
    files = {}
    for relative in SENTINELS:
        path = sentinel_path(profile, image, relative)
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.exists() or path.is_symlink():
            raise RuntimeError("refusing to overwrite existing profile data: " + str(path))
        data = ("The Mechanist installer preservation test: " + relative + "\n").encode() + secrets.token_bytes(64)
        with path.open("xb") as stream:
            stream.write(data)
        files[relative] = {"size": len(data), "sha256": digest(data)}
    record = {
        "schema": SCHEMA,
        "expectedCommit": commit,
        "profileRoot": str(profile),
        "installedImage": str(image),
        "files": files,
    }
    baseline.parent.mkdir(parents=True, exist_ok=True)
    baseline.write_text(json.dumps(record, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return record


def check(profile: pathlib.Path, image: pathlib.Path, commit: str, baseline: pathlib.Path) -> dict:
    profile = profile.resolve()
    image = image.resolve()
    assert_location(profile, image)
    record = json.loads(baseline.read_text(encoding="utf-8"))
    if (record.get("schema") != SCHEMA or record.get("expectedCommit") != commit
            or record.get("profileRoot") != str(profile)
            or record.get("installedImage") != str(image)):
        raise RuntimeError("baseline identity, source commit, or paths differ")
    files = record.get("files")
    if not isinstance(files, dict) or set(files) != set(SENTINELS):
        raise RuntimeError("baseline does not contain exactly the required sentinels")
    for relative in SENTINELS:
        path = sentinel_path(profile, image, relative)
        if not path.is_file():
            raise RuntimeError("mutable sentinel was removed: " + str(path))
        data = path.read_bytes()
        expected = files[relative]
        if len(data) != expected["size"] or digest(data) != expected["sha256"]:
            raise RuntimeError("mutable sentinel changed: " + str(path))
    return record


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("phase", choices=("capture", "check"))
    parser.add_argument("--profile-root", required=True, type=pathlib.Path)
    parser.add_argument("--installed-image", required=True, type=pathlib.Path)
    parser.add_argument("--expected-commit", required=True)
    parser.add_argument("--baseline", required=True, type=pathlib.Path)
    parser.add_argument("--report", required=True, type=pathlib.Path)
    args = parser.parse_args()
    result = {"schema": SCHEMA, "phase": args.phase, "status": "failed"}
    try:
        if args.phase == "capture":
            record = capture(args.profile_root, args.installed_image, args.expected_commit, args.baseline)
        else:
            record = check(args.profile_root, args.installed_image, args.expected_commit, args.baseline)
        result.update({
            "status": "passed",
            "commit": record["expectedCommit"],
            "profileRoot": record["profileRoot"],
            "installedImage": record["installedImage"],
            "verifiedFiles": record["files"],
        })
        print("INSTALLER MUTABLE PROFILE " + args.phase.upper() + " PASSED")
        returncode = 0
    except Exception as exc:
        result.update({"errorType": type(exc).__name__, "error": str(exc),
                       "traceback": traceback.format_exc()})
        print("INSTALLER MUTABLE PROFILE FAILED: " + str(exc), file=sys.stderr)
        returncode = 1
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return returncode


if __name__ == "__main__":
    raise SystemExit(main())

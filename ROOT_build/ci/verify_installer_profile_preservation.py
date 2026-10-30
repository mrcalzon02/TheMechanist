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



# Upgrade evidence requires two real DEBs and distinct commits, not a repair reinstall.
def _sha256_file(path: pathlib.Path) -> str:
    hasher = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            hasher.update(block)
    return hasher.hexdigest()


def _command_output(*argv: str) -> str:
    import subprocess
    completed = subprocess.run(argv, capture_output=True, text=True, check=False)
    if completed.returncode != 0:
        raise RuntimeError(f"package command failed ({argv[0]}): {completed.stderr.strip()}")
    return completed.stdout.strip()


def deb_identity(deb: pathlib.Path) -> dict:
    if not deb.is_file():
        raise RuntimeError(f"actual DEB artifact is missing: {deb}")
    fields = [_command_output("dpkg-deb", "-f", str(deb), field)
              for field in ("Package", "Version")]
    if len(fields) != 2 or not all(fields) or any(
            char.isspace() for field in fields for char in field):
        raise RuntimeError("DEB package/version metadata is missing or malformed")
    return {"package": fields[0], "version": fields[1], "sha256": _sha256_file(deb)}


def installed_deb_version(package: str) -> str:
    lines = _command_output("dpkg-query", "-W",
                            "-f=${Status}\n${Version}\n", package).splitlines()
    if len(lines) != 2 or lines[0] != "install ok installed" or not lines[1]:
        raise RuntimeError(f"DEB is not fully installed: {package}")
    return lines[1]


def verified_deployment(report_path: pathlib.Path, image: pathlib.Path,
                        commit: str) -> dict:
    report = json.loads(report_path.read_text(encoding="utf-8"))
    required = ("nativeLauncherRunning", "gameProcessRunning", "singlePlayerLifecycle",
                "installTreeImmutable", "mutableStorageOutsideInstall",
                "installedImageVerified")
    if (report.get("status") != "passed" or report.get("commit") != commit
            or report.get("expectedCommit") != commit
            or report.get("platform") != "linux-x64"
            or pathlib.Path(str(report.get("image", ""))).resolve() != image.resolve()
            or any(report.get(field) is not True for field in required)):
        raise RuntimeError("installed-game deployment report lacks exact successful identity/launch evidence")
    return report


def is_deb_upgrade(old_version: str, new_version: str) -> bool:
    import subprocess
    return subprocess.run(["dpkg", "--compare-versions", old_version, "lt", new_version],
                          check=False).returncode == 0


def capture_deb_upgrade(profile: pathlib.Path, image: pathlib.Path, old_commit: str,
                        baseline: pathlib.Path, old_deb: pathlib.Path,
                        deployment_report: pathlib.Path) -> dict:
    old = deb_identity(old_deb)
    if installed_deb_version(old["package"]) != old["version"]:
        raise RuntimeError("installed old DEB version differs from the old artifact")
    deployed = verified_deployment(deployment_report, image, old_commit)
    if str(deployed.get("version")) != old["version"].split("-")[0]:
        raise RuntimeError("old installed game version differs from old DEB app version")
    record = capture(profile, image, old_commit, baseline)
    record["upgrade"] = {"oldDeb": old, "oldCommit": old_commit,
                         "oldDeploymentReportSha256": _sha256_file(deployment_report)}
    baseline.write_text(json.dumps(record, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return record


def check_deb_upgrade(profile: pathlib.Path, image: pathlib.Path, old_commit: str,
                      new_commit: str, baseline: pathlib.Path, old_deb: pathlib.Path,
                      new_deb: pathlib.Path, deployment_report: pathlib.Path) -> dict:
    if not old_commit or not new_commit or old_commit == new_commit:
        raise RuntimeError("upgrade requires two distinct nonempty source commits")
    record = check(profile, image, old_commit, baseline)
    prior = record.get("upgrade")
    if not isinstance(prior, dict) or prior.get("oldCommit") != old_commit:
        raise RuntimeError("old installed-version capture evidence is absent or mismatched")
    old = deb_identity(old_deb)
    if prior.get("oldDeb") != old:
        raise RuntimeError("old DEB identity/hash differs from captured artifact")
    new = deb_identity(new_deb)
    if new["package"] != old["package"] or new["version"] == old["version"]:
        raise RuntimeError("upgrade requires the same package identity at a different version")
    if not is_deb_upgrade(old["version"], new["version"]):
        raise RuntimeError("new DEB version is not greater than installed old version")
    if installed_deb_version(new["package"]) != new["version"]:
        raise RuntimeError("installed DEB version does not match the new artifact")
    deployed = verified_deployment(deployment_report, image, new_commit)
    if str(deployed.get("version")) != new["version"].split("-")[0]:
        raise RuntimeError("installed game version differs from new DEB app version")
    return {"old": old, "new": new, "oldCommit": old_commit, "newCommit": new_commit,
            "preservedFiles": record["files"], "installedGameVerified": True}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("phase", choices=("capture", "check", "upgrade-capture", "upgrade-check"))
    parser.add_argument("--profile-root", required=True, type=pathlib.Path)
    parser.add_argument("--installed-image", required=True, type=pathlib.Path)
    parser.add_argument("--expected-commit", required=True)
    parser.add_argument("--baseline", required=True, type=pathlib.Path)
    parser.add_argument("--report", required=True, type=pathlib.Path)
    parser.add_argument("--old-deb", type=pathlib.Path)
    parser.add_argument("--new-deb", type=pathlib.Path)
    parser.add_argument("--new-commit")
    parser.add_argument("--deployment-report", type=pathlib.Path)
    args = parser.parse_args()
    result = {"schema": SCHEMA, "phase": args.phase, "status": "failed"}
    try:
        if args.phase == "capture":
            record = capture(args.profile_root, args.installed_image, args.expected_commit, args.baseline)
        elif args.phase == "check":
            record = check(args.profile_root, args.installed_image, args.expected_commit, args.baseline)
        elif args.phase == "upgrade-capture":
            if not args.old_deb or not args.deployment_report:
                raise RuntimeError("upgrade-capture requires --old-deb and --deployment-report")
            record = capture_deb_upgrade(args.profile_root, args.installed_image,
                                         args.expected_commit, args.baseline, args.old_deb,
                                         args.deployment_report)
        else:
            if not all((args.old_deb, args.new_deb, args.new_commit, args.deployment_report)):
                raise RuntimeError("upgrade-check requires --old-deb, --new-deb, --new-commit and --deployment-report")
            record = check_deb_upgrade(args.profile_root, args.installed_image,
                                       args.expected_commit, args.new_commit, args.baseline,
                                       args.old_deb, args.new_deb, args.deployment_report)
            result["upgrade"] = record
        result.update({
            "status": "passed",
            "commit": record.get("expectedCommit", args.new_commit),
            "profileRoot": str(args.profile_root.resolve()),
            "installedImage": str(args.installed_image.resolve()),
            "verifiedFiles": record.get("files", record.get("preservedFiles")),
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

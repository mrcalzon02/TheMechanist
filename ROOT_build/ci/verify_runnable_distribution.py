#!/usr/bin/env python3
"""Verify The Mechanist's staged runnable distribution.

The verifier is intentionally dependency-free so GitHub-hosted and local Java 17
builds use the same release checks. It validates package layout, manifest hashes,
JAR entry points, Java 17 classfile versions, archive integrity, and required
runtime/support payload presence.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import sys
import zipfile

JAVA_17_MAJOR = 61
REQUIRED_MANIFEST_KEYS = {
    "schema",
    "distributionModel",
    "version",
    "platform",
    "commit",
    "artifacts",
}
REQUIRED_ARTIFACT_ROLES = {"launcher", "client", "server"}
EXPECTED_MAIN_CLASSES = {
    "launcher": "mechanist.launcher.MechanistLauncherApp",
    "client": "mechanist.TheMechanist",
    "server": "mechanist.MechanistServerMain",
}


def fail(message: str) -> None:
    raise RuntimeError(message)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def manifest_main_class(jar: pathlib.Path) -> str:
    with zipfile.ZipFile(jar) as archive:
        try:
            raw = archive.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
        except KeyError as exc:
            fail(f"{jar}: missing META-INF/MANIFEST.MF")
            raise exc
    unfolded: list[str] = []
    for line in raw.replace("\r\n", "\n").split("\n"):
        if line.startswith(" ") and unfolded:
            unfolded[-1] += line[1:]
        else:
            unfolded.append(line)
    for line in unfolded:
        if line.lower().startswith("main-class:"):
            return line.split(":", 1)[1].strip()
    fail(f"{jar}: manifest has no Main-Class")
    return ""


def scan_jar(jar: pathlib.Path, role: str) -> tuple[int, int]:
    expected_main = EXPECTED_MAIN_CLASSES[role]
    actual_main = manifest_main_class(jar)
    if actual_main != expected_main:
        fail(f"{jar}: Main-Class {actual_main!r}, expected {expected_main!r}")

    class_count = 0
    project_class_count = 0
    with zipfile.ZipFile(jar) as archive:
        bad_entries = archive.testzip()
        if bad_entries:
            fail(f"{jar}: corrupt ZIP/JAR entry {bad_entries}")
        for info in archive.infolist():
            name = info.filename
            if name.startswith("/") or ".." in pathlib.PurePosixPath(name).parts:
                fail(f"{jar}: unsafe archive path {name}")
            if not name.endswith(".class"):
                continue
            data = archive.read(info)
            if len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe":
                fail(f"{jar}: malformed classfile {name}")
            major = int.from_bytes(data[6:8], "big")
            if major > JAVA_17_MAJOR:
                fail(f"{jar}: {name} uses classfile major {major}; Java 17 maximum is 61")
            if name.startswith("mechanist/") and major != JAVA_17_MAJOR:
                fail(f"{jar}: project class {name} uses major {major}, expected 61")
            class_count += 1
            if name.startswith("mechanist/"):
                project_class_count += 1

    if class_count == 0:
        fail(f"{jar}: contains no classfiles")
    if project_class_count == 0:
        fail(f"{jar}: contains no mechanist project classes")
    return class_count, project_class_count


def verify_distribution(root: pathlib.Path, archive: pathlib.Path | None) -> dict[str, object]:
    manifest_path = root / "manifests" / "runtime-manifest.json"
    if not manifest_path.is_file():
        fail(f"missing runtime manifest: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    missing_keys = REQUIRED_MANIFEST_KEYS - manifest.keys()
    if missing_keys:
        fail(f"runtime manifest missing keys: {sorted(missing_keys)}")
    if manifest["schema"] != 2:
        fail(f"runtime manifest schema must be 2, got {manifest['schema']!r}")
    if manifest["distributionModel"] != "installer-thin-launcher-client-server":
        fail("runtime manifest distributionModel is not the governed launcher/client/server model")

    artifacts = manifest["artifacts"]
    if not isinstance(artifacts, list) or not artifacts:
        fail("runtime manifest artifacts must be a non-empty list")
    roles = {entry.get("role") for entry in artifacts if isinstance(entry, dict)}
    missing_roles = REQUIRED_ARTIFACT_ROLES - roles
    if missing_roles:
        fail(f"runtime manifest missing required roles: {sorted(missing_roles)}")

    jar_summary: dict[str, dict[str, object]] = {}
    for entry in artifacts:
        if not isinstance(entry, dict):
            fail("runtime manifest artifact entry is not an object")
        for key in ("role", "path", "sha256", "size"):
            if key not in entry:
                fail(f"artifact entry missing {key}: {entry}")
        relative = pathlib.PurePosixPath(str(entry["path"]))
        if relative.is_absolute() or ".." in relative.parts:
            fail(f"unsafe artifact path in manifest: {relative}")
        path = root.joinpath(*relative.parts)
        if not path.is_file():
            fail(f"manifest artifact does not exist: {path}")
        actual_size = path.stat().st_size
        if actual_size != int(entry["size"]):
            fail(f"{path}: size {actual_size}, manifest says {entry['size']}")
        actual_hash = sha256(path)
        if actual_hash.lower() != str(entry["sha256"]).lower():
            fail(f"{path}: SHA-256 mismatch")
        role = str(entry["role"])
        if role in EXPECTED_MAIN_CLASSES:
            classes, project_classes = scan_jar(path, role)
            jar_summary[role] = {
                "path": str(relative),
                "classes": classes,
                "projectClasses": project_classes,
                "sha256": actual_hash,
            }

    support_dir = root / "packages" / "support" / "lib"
    support_jars = sorted(support_dir.glob("*.jar")) if support_dir.is_dir() else []
    if not support_jars:
        fail("distribution contains no launcher-managed support libraries")
    support_names = [path.name.lower() for path in support_jars]
    for required_fragment in ("netty", "lwjgl", "natives-linux"):
        if not any(required_fragment in name for name in support_names):
            fail(f"support library set is missing required fragment {required_fragment!r}")
    for support in support_jars:
        with zipfile.ZipFile(support) as archive_file:
            bad = archive_file.testzip()
            if bad:
                fail(f"{support}: corrupt support JAR entry {bad}")

    if archive is not None:
        if not archive.is_file():
            fail(f"distribution archive does not exist: {archive}")
        with zipfile.ZipFile(archive) as distribution_zip:
            bad = distribution_zip.testzip()
            if bad:
                fail(f"distribution ZIP contains corrupt entry {bad}")
            names = set(distribution_zip.namelist())
            manifest_suffix = f"{root.name}/manifests/runtime-manifest.json"
            if manifest_suffix not in names:
                fail(f"distribution ZIP does not contain {manifest_suffix}")

    return {
        "distribution": root.name,
        "version": manifest["version"],
        "platform": manifest["platform"],
        "commit": manifest["commit"],
        "artifactCount": len(artifacts),
        "supportJarCount": len(support_jars),
        "jars": jar_summary,
        "archive": str(archive) if archive else None,
        "status": "verified",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("distribution", type=pathlib.Path)
    parser.add_argument("--archive", type=pathlib.Path)
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()
    try:
        summary = verify_distribution(args.distribution.resolve(),
                                      args.archive.resolve() if args.archive else None)
    except Exception as exc:  # noqa: BLE001 - release verifier must print one clear failure
        print(f"DISTRIBUTION VERIFICATION FAILED: {exc}", file=sys.stderr)
        return 1
    rendered = json.dumps(summary, indent=2, sort_keys=True)
    print(rendered)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Verify The Mechanist's staged runnable distribution.

The verifier is dependency-free so GitHub-hosted and local Java 17 builds use
the same release checks. It validates package layout, canonical and thin-
launcher manifests, hashes, JAR entry points, Java 17 classfile versions,
archive integrity, required runtime/support payloads, and release-hardening
identity for explicit prerelease publication.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
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
    "javaRelease",
    "releaseHardened",
    "artifacts",
}
REQUIRED_ARTIFACT_ROLES = {"launcher", "client", "server"}
EXPECTED_MAIN_CLASSES = {
    "launcher": "mechanist.launcher.MechanistLauncherApp",
    "client": "mechanist.TheMechanist",
    "server": "mechanist.MechanistServerMain",
}
PRIMARY_PATHS = {
    "launcher": "launcher/MechanistLauncher.jar",
    "client": "packages/client/TheMechanist.jar",
    "server": "packages/server/TheMechanistServer.jar",
}
LAUNCHER_COMPATIBILITY_MANIFEST = "manifests/launcher-runtime-manifest.json"


def fail(message: str) -> None:
    raise RuntimeError(message)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def github_publish_prerelease_requested() -> bool:
    if os.environ.get("GITHUB_EVENT_NAME") != "workflow_dispatch":
        return False
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path:
        return False
    try:
        event = json.loads(pathlib.Path(event_path).read_text(encoding="utf-8"))
    except (OSError, ValueError, TypeError):
        return False
    inputs = event.get("inputs") or {}
    return str(inputs.get("publish_prerelease")).strip().lower() == "true"


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
        bad_entry = archive.testzip()
        if bad_entry:
            fail(f"{jar}: corrupt ZIP/JAR entry {bad_entry}")
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


def native_fragment(platform_name: str) -> str:
    if platform_name.startswith("windows-"):
        return "natives-windows"
    if platform_name.startswith("linux-"):
        return "natives-linux"
    if platform_name.startswith("macos-"):
        return "natives-macos"
    fail(f"unsupported manifest platform {platform_name!r}")
    return ""


def safe_relative_path(value: object, label: str) -> pathlib.PurePosixPath:
    relative = pathlib.PurePosixPath(str(value))
    if relative.is_absolute() or ".." in relative.parts:
        fail(f"unsafe {label} path: {relative}")
    return relative


def validate_artifact_entry(
    root: pathlib.Path,
    entry: dict[str, object],
    declared_paths: set[str],
) -> tuple[pathlib.Path, str, str]:
    for key in ("role", "path", "sha256", "size"):
        if key not in entry:
            fail(f"artifact entry missing {key}: {entry}")
    relative = safe_relative_path(entry["path"], "artifact")
    relative_text = relative.as_posix()
    if relative_text in declared_paths:
        fail(f"runtime manifest declares duplicate artifact path {relative_text}")
    declared_paths.add(relative_text)
    path = root.joinpath(*relative.parts)
    if not path.is_file():
        fail(f"manifest artifact does not exist: {path}")
    actual_size = path.stat().st_size
    if actual_size != int(entry["size"]):
        fail(f"{path}: size {actual_size}, manifest says {entry['size']}")
    actual_hash = sha256(path)
    if actual_hash.lower() != str(entry["sha256"]).lower():
        fail(f"{path}: SHA-256 mismatch")
    return path, str(entry["role"]), actual_hash


def verify_launcher_compatibility_manifest(
    root: pathlib.Path,
    canonical: dict[str, object],
    canonical_entries: dict[str, dict[str, object]],
) -> dict[str, object]:
    path = root / LAUNCHER_COMPATIBILITY_MANIFEST
    if not path.is_file():
        fail(
            "missing thin-launcher compatibility manifest: "
            f"{LAUNCHER_COMPATIBILITY_MANIFEST}"
        )
    if LAUNCHER_COMPATIBILITY_MANIFEST not in canonical_entries:
        fail("thin-launcher compatibility manifest is not covered by the canonical artifact ledger")

    data = json.loads(path.read_text(encoding="utf-8"))
    required = {
        "schema",
        "distribution_model",
        "version",
        "platform",
        "client",
        "server",
        "support_libraries",
    }
    missing = required - data.keys()
    if missing:
        fail(f"thin-launcher compatibility manifest missing keys: {sorted(missing)}")
    if data["schema"] != 2:
        fail(f"thin-launcher compatibility schema must be 2, got {data['schema']!r}")
    if data["distribution_model"] != "installer-thin-launcher-client-server":
        fail("thin-launcher compatibility distribution_model is invalid")
    if data["version"] != canonical["version"]:
        fail("thin-launcher compatibility version does not match canonical manifest")
    if data["platform"] != canonical["platform"]:
        fail("thin-launcher compatibility platform does not match canonical manifest")
    if "release_hardened" in data and bool(data["release_hardened"]) != bool(canonical["releaseHardened"]):
        fail("thin-launcher compatibility hardening identity does not match canonical manifest")

    checked_paths: set[str] = set()
    for role in ("client", "server"):
        entry = data[role]
        if not isinstance(entry, dict):
            fail(f"thin-launcher {role} block is not an object")
        for key in ("path", "sha256", "size"):
            if key not in entry:
                fail(f"thin-launcher {role} block missing {key}")
        expected_path = PRIMARY_PATHS[role]
        if str(entry["path"]) != expected_path:
            fail(f"thin-launcher {role} path {entry['path']!r}, expected {expected_path!r}")
        canonical_entry = canonical_entries.get(expected_path)
        if canonical_entry is None:
            fail(f"canonical manifest does not declare thin-launcher {role} artifact")
        if str(entry["sha256"]).lower() != str(canonical_entry["sha256"]).lower():
            fail(f"thin-launcher {role} hash does not match canonical manifest")
        if int(entry["size"]) != int(canonical_entry["size"]):
            fail(f"thin-launcher {role} size does not match canonical manifest")
        checked_paths.add(expected_path)

    support = data["support_libraries"]
    if not isinstance(support, list) or not support:
        fail("thin-launcher compatibility manifest contains no support_libraries")
    for index, entry in enumerate(support):
        if not isinstance(entry, dict):
            fail(f"thin-launcher support_libraries[{index}] is not an object")
        for key in ("path", "sha256", "size"):
            if key not in entry:
                fail(f"thin-launcher support_libraries[{index}] missing {key}")
        relative = safe_relative_path(entry["path"], "thin-launcher support library").as_posix()
        if not relative.startswith("packages/support/lib/") or not relative.endswith(".jar"):
            fail(f"thin-launcher support library path is outside the support library root: {relative}")
        canonical_entry = canonical_entries.get(relative)
        if canonical_entry is None or canonical_entry.get("role") != "support":
            fail(f"thin-launcher support library is absent from canonical support ledger: {relative}")
        if str(entry["sha256"]).lower() != str(canonical_entry["sha256"]).lower():
            fail(f"thin-launcher support library hash mismatch: {relative}")
        if int(entry["size"]) != int(canonical_entry["size"]):
            fail(f"thin-launcher support library size mismatch: {relative}")
        if relative in checked_paths:
            fail(f"thin-launcher compatibility manifest duplicates path: {relative}")
        checked_paths.add(relative)

    canonical_support = {
        item_path
        for item_path, item in canonical_entries.items()
        if item.get("role") == "support"
    }
    launcher_support = {
        item_path for item_path in checked_paths if item_path.startswith("packages/support/lib/")
    }
    if launcher_support != canonical_support:
        missing_from_launcher = sorted(canonical_support - launcher_support)
        extra_in_launcher = sorted(launcher_support - canonical_support)
        fail(
            "thin-launcher support ledger differs from canonical support ledger: "
            f"missing={missing_from_launcher[:8]} extra={extra_in_launcher[:8]}"
        )

    return {
        "path": LAUNCHER_COMPATIBILITY_MANIFEST,
        "supportLibraryCount": len(launcher_support),
        "status": "verified",
    }


def verify_distribution(
    root: pathlib.Path,
    archive: pathlib.Path | None,
    require_release_hardened: bool = False,
) -> dict[str, object]:
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
    if manifest["javaRelease"] != 17:
        fail(f"runtime manifest javaRelease must be 17, got {manifest['javaRelease']!r}")
    if not isinstance(manifest["releaseHardened"], bool):
        fail("runtime manifest releaseHardened must be a boolean")
    if require_release_hardened and not manifest["releaseHardened"]:
        fail("explicit prerelease publication requires releaseHardened=true")

    artifacts = manifest["artifacts"]
    if not isinstance(artifacts, list) or not artifacts:
        fail("runtime manifest artifacts must be a non-empty list")
    roles = {entry.get("role") for entry in artifacts if isinstance(entry, dict)}
    missing_roles = REQUIRED_ARTIFACT_ROLES - roles
    if missing_roles:
        fail(f"runtime manifest missing required roles: {sorted(missing_roles)}")

    declared_paths: set[str] = set()
    canonical_entries: dict[str, dict[str, object]] = {}
    jar_summary: dict[str, dict[str, object]] = {}
    for entry in artifacts:
        if not isinstance(entry, dict):
            fail("runtime manifest artifact entry is not an object")
        path, role, actual_hash = validate_artifact_entry(root, entry, declared_paths)
        relative_text = pathlib.PurePosixPath(str(entry["path"])).as_posix()
        canonical_entries[relative_text] = entry
        if role in EXPECTED_MAIN_CLASSES:
            expected_path = PRIMARY_PATHS[role]
            if relative_text != expected_path:
                fail(f"canonical {role} artifact path {relative_text!r}, expected {expected_path!r}")
            classes, project_classes = scan_jar(path, role)
            jar_summary[role] = {
                "path": relative_text,
                "classes": classes,
                "projectClasses": project_classes,
                "sha256": actual_hash,
            }

    actual_paths = {
        pathlib.PurePosixPath(path.relative_to(root).as_posix()).as_posix()
        for path in root.rglob("*")
        if path.is_file() and path != manifest_path
    }
    undeclared = actual_paths - declared_paths
    missing = declared_paths - actual_paths
    if undeclared:
        fail(f"distribution contains undeclared files: {sorted(undeclared)[:12]}")
    if missing:
        fail(f"manifest declares missing files: {sorted(missing)[:12]}")

    launcher_compatibility = verify_launcher_compatibility_manifest(
        root,
        manifest,
        canonical_entries,
    )

    runtime_java = root / "runtime" / "bin" / (
        "java.exe" if str(manifest["platform"]).startswith("windows-") else "java"
    )
    if not runtime_java.is_file():
        fail(f"bundled Java runtime is missing executable {runtime_java}")

    support_dir = root / "packages" / "support" / "lib"
    support_jars = sorted(support_dir.glob("*.jar")) if support_dir.is_dir() else []
    if not support_jars:
        fail("distribution contains no launcher-managed support libraries")
    support_names = [path.name.lower() for path in support_jars]
    required_fragments = ("netty", "lwjgl", native_fragment(str(manifest["platform"])))
    for required_fragment in required_fragments:
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
            launcher_suffix = f"{root.name}/{LAUNCHER_COMPATIBILITY_MANIFEST}"
            if manifest_suffix not in names:
                fail(f"distribution ZIP does not contain {manifest_suffix}")
            if launcher_suffix not in names:
                fail(f"distribution ZIP does not contain {launcher_suffix}")

    return {
        "distribution": root.name,
        "version": manifest["version"],
        "platform": manifest["platform"],
        "commit": manifest["commit"],
        "javaRelease": manifest["javaRelease"],
        "releaseHardened": manifest["releaseHardened"],
        "artifactCount": len(artifacts),
        "supportJarCount": len(support_jars),
        "requiredNativeFragment": native_fragment(str(manifest["platform"])),
        "launcherCompatibility": launcher_compatibility,
        "jars": jar_summary,
        "archive": str(archive) if archive else None,
        "status": "verified",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("distribution", type=pathlib.Path)
    parser.add_argument("--archive", type=pathlib.Path)
    parser.add_argument("--report", type=pathlib.Path)
    parser.add_argument("--require-release-hardened", action="store_true")
    args = parser.parse_args()
    require_release_hardened = (
        args.require_release_hardened or github_publish_prerelease_requested()
    )
    try:
        summary = verify_distribution(
            args.distribution.resolve(),
            args.archive.resolve() if args.archive else None,
            require_release_hardened=require_release_hardened,
        )
    except Exception as exc:  # noqa: BLE001 - release verifier prints one clear failure
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

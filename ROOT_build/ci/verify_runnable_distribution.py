#!/usr/bin/env python3
"""Verify The Mechanist's staged runnable distribution."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import pathlib
import sys
import zipfile

JAVA_17_MAJOR = 61
REMOTE_CLIENT_MAIN = "mechanist.RemoteClientMain"
REMOTE_CLIENT_CLASS = "mechanist/RemoteClientMain.class"
DISTRIBUTION_MODEL = "installer-thin-launcher-client-server"
LAUNCHER_COMPATIBILITY_MANIFEST = "manifests/launcher-runtime-manifest.json"
REQUIRED_MANIFEST_KEYS = {
    "schema",
    "distributionModel",
    "version",
    "platform",
    "commit",
    "javaRelease",
    "releaseHardened",
    "remoteClientEntryPoint",
    "artifacts",
}
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


def safe_relative_path(value: object, label: str) -> pathlib.PurePosixPath:
    relative = pathlib.PurePosixPath(str(value))
    if relative.is_absolute() or ".." in relative.parts:
        fail(f"unsafe {label} path: {relative}")
    return relative


def manifest_attributes(jar: pathlib.Path) -> dict[str, str]:
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
    attributes: dict[str, str] = {}
    for line in unfolded:
        if ":" not in line:
            continue
        key, value = line.split(":", 1)
        attributes[key.strip()] = value.strip()
    return attributes


def scan_jar(
    jar: pathlib.Path,
    role: str,
    expected_version: str,
) -> dict[str, object]:
    attributes = manifest_attributes(jar)
    actual_main = attributes.get("Main-Class", "")
    expected_main = EXPECTED_MAIN_CLASSES[role]
    if actual_main != expected_main:
        fail(f"{jar}: Main-Class {actual_main!r}, expected {expected_main!r}")
    actual_version = attributes.get("Implementation-Version", "")
    if not actual_version:
        fail(f"{jar}: manifest has no Implementation-Version")
    if actual_version != expected_version:
        fail(
            f"{jar}: Implementation-Version {actual_version!r}, "
            f"expected canonical version {expected_version!r}"
        )

    class_count = 0
    project_class_count = 0
    remote_client_class = False
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
            if role == "client" and name == REMOTE_CLIENT_CLASS:
                remote_client_class = True

    if class_count == 0 or project_class_count == 0:
        fail(f"{jar}: missing required classfiles")
    if role == "client" and not remote_client_class:
        fail(f"{jar}: missing packaged remote-client entry class {REMOTE_CLIENT_CLASS}")
    return {
        "classes": class_count,
        "projectClasses": project_class_count,
        "mainClass": actual_main,
        "implementationVersion": actual_version,
        "remoteClientEntryClass": remote_client_class if role == "client" else None,
    }


def native_fragment(platform_name: str) -> str:
    if platform_name.startswith("windows-"):
        return "natives-windows"
    if platform_name.startswith("linux-"):
        return "natives-linux"
    if platform_name.startswith("macos-"):
        return "natives-macos"
    fail(f"unsupported manifest platform {platform_name!r}")
    return ""


def remote_launcher_path(platform_name: str) -> str:
    if platform_name.startswith("windows-"):
        return "Run-Remote-Client.cmd"
    if platform_name.startswith("linux-") or platform_name.startswith("macos-"):
        return "run-remote-client.sh"
    fail(f"unsupported remote-client launcher platform {platform_name!r}")
    return ""


def validate_artifact_entry(
    root: pathlib.Path,
    entry: dict[str, object],
    declared_paths: set[str],
) -> tuple[pathlib.Path, str, str, str]:
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
    return path, str(entry["role"]), actual_hash, relative_text


def verify_launcher_compatibility_manifest(
    root: pathlib.Path,
    canonical: dict[str, object],
    canonical_entries: dict[str, dict[str, object]],
) -> dict[str, object]:
    relative = LAUNCHER_COMPATIBILITY_MANIFEST
    path = root / relative
    if not path.is_file():
        fail(f"missing thin-launcher compatibility manifest: {relative}")
    if relative not in canonical_entries:
        fail("thin-launcher compatibility manifest is not in the canonical ledger")

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
    if data["schema"] != 2 or data["distribution_model"] != DISTRIBUTION_MODEL:
        fail("thin-launcher compatibility identity is invalid")
    if data["version"] != canonical["version"] or data["platform"] != canonical["platform"]:
        fail("thin-launcher compatibility version/platform differs from canonical manifest")
    if "release_hardened" in data and bool(data["release_hardened"]) != bool(canonical["releaseHardened"]):
        fail("thin-launcher hardening identity differs from canonical manifest")

    checked_paths: set[str] = set()
    for role in ("client", "server"):
        block = data[role]
        if not isinstance(block, dict):
            fail(f"thin-launcher {role} block is not an object")
        for key in ("path", "sha256", "size"):
            if key not in block:
                fail(f"thin-launcher {role} block missing {key}")
        expected_path = PRIMARY_PATHS[role]
        if str(block["path"]) != expected_path:
            fail(f"thin-launcher {role} path is inconsistent")
        canonical_entry = canonical_entries.get(expected_path)
        if canonical_entry is None:
            fail(f"canonical manifest omits thin-launcher {role} artifact")
        if str(block["sha256"]).lower() != str(canonical_entry["sha256"]).lower():
            fail(f"thin-launcher {role} hash differs from canonical manifest")
        if int(block["size"]) != int(canonical_entry["size"]):
            fail(f"thin-launcher {role} size differs from canonical manifest")
        checked_paths.add(expected_path)

    client = data["client"]
    if client.get("main_class") != EXPECTED_MAIN_CLASSES["client"]:
        fail("thin-launcher client main_class is invalid")
    if client.get("remote_main_class") != REMOTE_CLIENT_MAIN:
        fail("thin-launcher remote_main_class is invalid")
    if canonical.get("remoteClientEntryPoint") != REMOTE_CLIENT_MAIN:
        fail("canonical remoteClientEntryPoint is invalid")

    support = data["support_libraries"]
    if not isinstance(support, list) or not support:
        fail("thin-launcher compatibility manifest contains no support libraries")
    for index, block in enumerate(support):
        if not isinstance(block, dict):
            fail(f"thin-launcher support_libraries[{index}] is not an object")
        for key in ("path", "sha256", "size"):
            if key not in block:
                fail(f"thin-launcher support_libraries[{index}] missing {key}")
        support_path = safe_relative_path(
            block["path"],
            "thin-launcher support library",
        ).as_posix()
        if not support_path.startswith("packages/support/lib/") or not support_path.endswith(".jar"):
            fail(f"thin-launcher support path is outside governed root: {support_path}")
        canonical_entry = canonical_entries.get(support_path)
        if canonical_entry is None or canonical_entry.get("role") != "support":
            fail(f"thin-launcher support library is absent from canonical ledger: {support_path}")
        if str(block["sha256"]).lower() != str(canonical_entry["sha256"]).lower():
            fail(f"thin-launcher support hash mismatch: {support_path}")
        if int(block["size"]) != int(canonical_entry["size"]):
            fail(f"thin-launcher support size mismatch: {support_path}")
        if support_path in checked_paths:
            fail(f"thin-launcher compatibility duplicates path: {support_path}")
        checked_paths.add(support_path)

    canonical_support = {
        path
        for path, entry in canonical_entries.items()
        if entry.get("role") == "support"
    }
    launcher_support = {
        path for path in checked_paths if path.startswith("packages/support/lib/")
    }
    if launcher_support != canonical_support:
        fail("thin-launcher support ledger differs from canonical support ledger")

    return {
        "path": relative,
        "supportLibraryCount": len(launcher_support),
        "remoteClientEntryPoint": REMOTE_CLIENT_MAIN,
        "status": "verified",
    }


def verify_remote_launcher(
    root: pathlib.Path,
    platform_name: str,
    canonical_entries: dict[str, dict[str, object]],
) -> dict[str, object]:
    relative = remote_launcher_path(platform_name)
    entry = canonical_entries.get(relative)
    if entry is None or entry.get("role") != "launch-script":
        fail(f"canonical manifest omits governed remote-client launcher {relative}")
    path = root / relative
    text = path.read_text(encoding="utf-8", errors="replace")
    if REMOTE_CLIENT_MAIN not in text:
        fail(f"remote-client launcher does not invoke {REMOTE_CLIENT_MAIN}: {relative}")
    if "runtime" not in text or "TheMechanist.jar" not in text:
        fail(f"remote-client launcher does not use bundled runtime/client: {relative}")
    if relative.endswith(".sh") and not text.startswith("#!/usr/bin/env sh\n"):
        fail(f"remote-client shell launcher has wrong shebang: {relative}")
    return {
        "path": relative,
        "mainClass": REMOTE_CLIENT_MAIN,
        "portableAfterZipExtraction": True,
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
    if manifest["distributionModel"] != DISTRIBUTION_MODEL:
        fail("runtime manifest distributionModel is invalid")
    if manifest["javaRelease"] != 17:
        fail(f"runtime manifest javaRelease must be 17, got {manifest['javaRelease']!r}")
    if not isinstance(manifest["releaseHardened"], bool):
        fail("runtime manifest releaseHardened must be a boolean")
    if manifest["remoteClientEntryPoint"] != REMOTE_CLIENT_MAIN:
        fail("runtime manifest remoteClientEntryPoint is invalid")
    if require_release_hardened and not manifest["releaseHardened"]:
        fail("explicit prerelease publication requires releaseHardened=true")

    artifacts = manifest["artifacts"]
    if not isinstance(artifacts, list) or not artifacts:
        fail("runtime manifest artifacts must be a non-empty list")
    roles = {entry.get("role") for entry in artifacts if isinstance(entry, dict)}
    missing_roles = set(EXPECTED_MAIN_CLASSES) - roles
    if missing_roles:
        fail(f"runtime manifest missing required roles: {sorted(missing_roles)}")

    declared_paths: set[str] = set()
    canonical_entries: dict[str, dict[str, object]] = {}
    jar_summary: dict[str, dict[str, object]] = {}
    expected_version = str(manifest["version"])
    for entry in artifacts:
        if not isinstance(entry, dict):
            fail("runtime manifest artifact entry is not an object")
        path, role, actual_hash, relative = validate_artifact_entry(
            root,
            entry,
            declared_paths,
        )
        canonical_entries[relative] = entry
        if role in EXPECTED_MAIN_CLASSES:
            expected_path = PRIMARY_PATHS[role]
            if relative != expected_path:
                fail(f"canonical {role} path {relative!r}, expected {expected_path!r}")
            jar_summary[role] = {
                "path": relative,
                "sha256": actual_hash,
                **scan_jar(path, role, expected_version),
            }

    actual_paths = {
        pathlib.PurePosixPath(path.relative_to(root).as_posix()).as_posix()
        for path in root.rglob("*")
        if path.is_file() and path != manifest_path
    }
    undeclared = actual_paths - declared_paths
    missing_files = declared_paths - actual_paths
    if undeclared:
        fail(f"distribution contains undeclared files: {sorted(undeclared)[:12]}")
    if missing_files:
        fail(f"manifest declares missing files: {sorted(missing_files)[:12]}")

    launcher_compatibility = verify_launcher_compatibility_manifest(
        root,
        manifest,
        canonical_entries,
    )
    remote_launcher = verify_remote_launcher(
        root,
        str(manifest["platform"]),
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
    for fragment in ("netty", "lwjgl", native_fragment(str(manifest["platform"]))):
        if not any(fragment in name for name in support_names):
            fail(f"support library set is missing required fragment {fragment!r}")
    for support in support_jars:
        with zipfile.ZipFile(support) as support_zip:
            bad = support_zip.testzip()
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
            required_archive_paths = {
                f"{root.name}/manifests/runtime-manifest.json",
                f"{root.name}/{LAUNCHER_COMPATIBILITY_MANIFEST}",
                f"{root.name}/{remote_launcher['path']}",
            }
            missing_archive_paths = required_archive_paths - names
            if missing_archive_paths:
                fail(
                    "distribution ZIP omits required files: "
                    f"{sorted(missing_archive_paths)}"
                )

    return {
        "distribution": root.name,
        "version": manifest["version"],
        "platform": manifest["platform"],
        "commit": manifest["commit"],
        "javaRelease": manifest["javaRelease"],
        "releaseHardened": manifest["releaseHardened"],
        "versionAuthorityVerified": True,
        "remoteClientEntryPoint": manifest["remoteClientEntryPoint"],
        "remoteClientLauncher": remote_launcher,
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
    except Exception as exc:  # noqa: BLE001 - one release-gate failure surface
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

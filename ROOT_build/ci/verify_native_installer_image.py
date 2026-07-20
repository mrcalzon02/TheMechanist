#!/usr/bin/env python3
"""Verify a jpackage app-image built from a canonical The Mechanist distribution."""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import sys
import zipfile

from verify_runnable_distribution import EXPECTED_MAIN_CLASSES, manifest_main_class, scan_jar

LAUNCHER_MANIFEST = pathlib.PurePosixPath("manifests/launcher-runtime-manifest.json")


def fail(message: str) -> None:
    raise RuntimeError(message)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def platform_java(image_root: pathlib.Path, platform_name: str) -> pathlib.Path:
    executable = "java.exe" if platform_name.startswith("windows-") else "java"
    return image_root / "runtime" / "bin" / executable


def payload_root(image_root: pathlib.Path) -> pathlib.Path:
    candidate = image_root / "app"
    if (candidate / "manifests").is_dir() and (candidate / "packages").is_dir():
        return candidate
    if (image_root / "manifests").is_dir() and (image_root / "packages").is_dir():
        return image_root
    fail(f"could not locate jpackage application payload under {image_root}")
    return image_root


def verify_declared_artifact(
    payload: pathlib.Path,
    entry: dict[str, object],
    label: str,
) -> pathlib.Path:
    for key in ("path", "sha256", "size"):
        if key not in entry:
            fail(f"{label} entry missing {key}")
    relative = pathlib.PurePosixPath(str(entry["path"]))
    if relative.is_absolute() or ".." in relative.parts:
        fail(f"{label} path is unsafe: {relative}")
    path = payload.joinpath(*relative.parts)
    if not path.is_file():
        fail(f"{label} is missing: {path}")
    if path.stat().st_size != int(entry["size"]):
        fail(f"{label} size mismatch: {relative}")
    if sha256(path).lower() != str(entry["sha256"]).lower():
        fail(f"{label} SHA-256 mismatch: {relative}")
    return path


def verify_image(
    image_root: pathlib.Path,
    expected_platform: str | None,
) -> dict[str, object]:
    image_root = image_root.resolve()
    payload = payload_root(image_root)
    manifest_path = payload.joinpath(*LAUNCHER_MANIFEST.parts)
    if not manifest_path.is_file():
        fail(f"missing launcher runtime manifest: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

    if manifest.get("schema") != 2:
        fail(f"launcher manifest schema must be 2, got {manifest.get('schema')!r}")
    if manifest.get("distribution_model") != "installer-thin-launcher-client-server":
        fail("launcher manifest distribution_model is invalid")
    platform_name = str(manifest.get("platform", ""))
    if expected_platform and platform_name != expected_platform:
        fail(f"native image platform {platform_name!r}, expected {expected_platform!r}")
    if manifest.get("java_release") != 17:
        fail(f"launcher manifest java_release must be 17, got {manifest.get('java_release')!r}")
    if manifest.get("release_hardened") is not True:
        fail("native installer image requires release_hardened=true")

    launcher_jar = payload / "launcher" / "MechanistLauncher.jar"
    if not launcher_jar.is_file():
        fail(f"missing launcher jar: {launcher_jar}")
    if manifest_main_class(launcher_jar) != EXPECTED_MAIN_CLASSES["launcher"]:
        fail("launcher jar has the wrong Main-Class")
    launcher_classes, launcher_project_classes = scan_jar(launcher_jar, "launcher")

    client = manifest.get("client")
    server = manifest.get("server")
    support = manifest.get("support_libraries")
    if not isinstance(client, dict) or not isinstance(server, dict):
        fail("launcher manifest must contain client and server objects")
    if not isinstance(support, list) or not support:
        fail("launcher manifest must contain support_libraries")

    client_jar = verify_declared_artifact(payload, client, "client")
    server_jar = verify_declared_artifact(payload, server, "server")
    client_classes, client_project_classes = scan_jar(client_jar, "client")
    server_classes, server_project_classes = scan_jar(server_jar, "server")

    support_paths: set[str] = set()
    for index, entry in enumerate(support):
        if not isinstance(entry, dict):
            fail(f"support_libraries[{index}] is not an object")
        path = verify_declared_artifact(payload, entry, f"support_libraries[{index}]")
        relative = pathlib.PurePosixPath(path.relative_to(payload).as_posix()).as_posix()
        if not relative.startswith("packages/support/lib/") or not relative.endswith(".jar"):
            fail(f"support library is outside the governed directory: {relative}")
        if relative in support_paths:
            fail(f"support library is declared twice: {relative}")
        support_paths.add(relative)
        with zipfile.ZipFile(path) as archive:
            bad = archive.testzip()
            if bad:
                fail(f"support library contains corrupt entry {bad}: {relative}")

    java = platform_java(image_root, platform_name)
    if not java.is_file():
        fail(f"jpackage runtime is missing Java executable: {java}")

    forbidden = ("saves", "settings", "profiles", "logs", "cache", "mods", "modsarchived", "export")
    leaked = [name for name in forbidden if (payload / name).exists()]
    if leaked:
        fail(f"mutable user-storage directories leaked into installer payload: {leaked}")

    certification = payload / "certification" / "installer-source-verification.json"
    if not certification.is_file():
        fail(f"missing installer source verification record: {certification}")
    source = json.loads(certification.read_text(encoding="utf-8"))
    for key in ("version", "platform", "commit", "canonicalManifestSha256"):
        if not source.get(key):
            fail(f"installer source verification record is missing {key}")
    if source.get("releaseHardened") is not True:
        fail("installer source verification record is not release hardened")
    if source.get("platform") != platform_name:
        fail("installer source verification platform does not match launcher manifest")
    if source.get("version") != manifest.get("version"):
        fail("installer source verification version does not match launcher manifest")

    return {
        "status": "verified",
        "image": image_root.name,
        "payload": str(payload),
        "version": manifest.get("version"),
        "platform": platform_name,
        "releaseHardened": True,
        "supportLibraryCount": len(support_paths),
        "runtimeJava": str(java),
        "jars": {
            "launcher": {
                "classes": launcher_classes,
                "projectClasses": launcher_project_classes,
            },
            "client": {
                "classes": client_classes,
                "projectClasses": client_project_classes,
            },
            "server": {
                "classes": server_classes,
                "projectClasses": server_project_classes,
            },
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("image", type=pathlib.Path)
    parser.add_argument("--expected-platform")
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()
    try:
        summary = verify_image(args.image, args.expected_platform)
    except Exception as exc:  # noqa: BLE001 - verifier must print one clear error
        print(f"NATIVE INSTALLER IMAGE VERIFICATION FAILED: {exc}", file=sys.stderr)
        return 1
    rendered = json.dumps(summary, indent=2, sort_keys=True)
    print(rendered)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

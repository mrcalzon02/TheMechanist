#!/usr/bin/env python3
"""Verify a jpackage app image built from one canonical Mechanist distribution."""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import sys
import zipfile

from verify_runnable_distribution import (
    DISTRIBUTION_MODEL,
    EXPECTED_MAIN_CLASSES,
    REMOTE_CLIENT_MAIN,
    scan_jar,
)

LAUNCHER_MANIFEST = pathlib.PurePosixPath(
    "manifests/launcher-runtime-manifest.json"
)
MAIN_LAUNCHER_NAME = "The Mechanist"
REMOTE_LAUNCHER_NAME = "The Mechanist Remote Lobby"
PLAYTEST_DOCUMENT_COUNT = 8


def fail(message: str) -> None:
    raise RuntimeError(message)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def payload_root(image_root: pathlib.Path) -> pathlib.Path:
    for candidate in (
        image_root / "app",
        image_root / "lib" / "app",
        image_root,
    ):
        if (candidate / "manifests").is_dir() and (
            candidate / "packages"
        ).is_dir():
            return candidate
    fail(f"could not locate jpackage application payload under {image_root}")
    return image_root


def runtime_root(image_root: pathlib.Path) -> pathlib.Path:
    for candidate in (
        image_root / "runtime",
        image_root / "lib" / "runtime",
    ):
        if candidate.is_dir():
            return candidate
    fail(f"could not locate jpackage runtime under {image_root}")
    return image_root


def native_launcher(
    image_root: pathlib.Path,
    platform_name: str,
    launcher_name: str,
) -> pathlib.Path:
    if platform_name.startswith("windows-"):
        return image_root / f"{launcher_name}.exe"
    return image_root / "bin" / launcher_name


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


def verify_remote_launcher_configuration(
    image_root: pathlib.Path,
    payload: pathlib.Path,
) -> pathlib.Path:
    candidates = sorted({*image_root.rglob("*.cfg"), *payload.rglob("*.cfg")})
    for path in candidates:
        text = path.read_text(encoding="utf-8", errors="replace")
        normalized = text.replace("\\", "/")
        if (
            REMOTE_CLIENT_MAIN in text
            and "packages/client/TheMechanist.jar" in normalized
        ):
            return path
    fail(
        "jpackage image contains no launcher configuration connecting "
        f"{REMOTE_LAUNCHER_NAME!r} to {REMOTE_CLIENT_MAIN} and the verified client JAR"
    )
    return image_root


def require_false(record: dict[str, object], key: str, label: str) -> None:
    if record.get(key) is not False:
        fail(f"{label} must declare {key}=false")


def canonical_artifacts(
    canonical: dict[str, object],
) -> dict[str, dict[str, object]]:
    entries = canonical.get("artifacts")
    if not isinstance(entries, list):
        fail("canonical source manifest has no artifact list")
    artifacts: dict[str, dict[str, object]] = {}
    for index, entry in enumerate(entries):
        if not isinstance(entry, dict):
            fail(f"canonical source artifact {index} is not an object")
        relative = entry.get("path")
        if not isinstance(relative, str) or not relative:
            fail(f"canonical source artifact {index} has no path")
        if relative in artifacts:
            fail(f"canonical source manifest repeats artifact path: {relative}")
        artifacts[relative] = entry
    return artifacts


def verify_playtest_documents(
    payload: pathlib.Path,
    source: dict[str, object],
    canonical: dict[str, object],
) -> list[dict[str, object]]:
    if source.get("playtestOperationsVerified") is not True:
        fail("installer source verification did not verify playtest operations")
    if source.get("playtestDocumentCount") != PLAYTEST_DOCUMENT_COUNT:
        fail(
            "installer source verification must declare exactly "
            f"{PLAYTEST_DOCUMENT_COUNT} playtest documents"
        )
    declared = source.get("playtestDocuments")
    if not isinstance(declared, list) or len(declared) != PLAYTEST_DOCUMENT_COUNT:
        fail(
            "installer source verification playtest document list is incomplete"
        )
    artifacts = canonical_artifacts(canonical)
    verified: list[dict[str, object]] = []
    seen: set[str] = set()
    for index, entry in enumerate(declared):
        if not isinstance(entry, dict):
            fail(f"playtestDocuments[{index}] is not an object")
        relative = entry.get("path")
        digest = entry.get("sha256")
        size = entry.get("size")
        if not isinstance(relative, str) or not relative.startswith("docs/"):
            fail(f"playtestDocuments[{index}] has invalid path: {relative!r}")
        pure = pathlib.PurePosixPath(relative)
        if pure.is_absolute() or ".." in pure.parts:
            fail(f"playtestDocuments[{index}] has unsafe path: {relative}")
        if relative in seen:
            fail(f"installer source verification repeats playtest document: {relative}")
        if not isinstance(digest, str) or len(digest) != 64:
            fail(f"playtestDocuments[{index}] has invalid SHA-256: {relative}")
        if not isinstance(size, int) or size <= 0:
            fail(f"playtestDocuments[{index}] has invalid size: {relative}")
        path = payload.joinpath(*pure.parts)
        if not path.is_file():
            fail(f"native image is missing playtest document: {relative}")
        if path.stat().st_size != size:
            fail(f"native image playtest document size mismatch: {relative}")
        actual = sha256(path)
        if actual != digest:
            fail(f"native image playtest document hash mismatch: {relative}")
        canonical_entry = artifacts.get(relative)
        if canonical_entry is None:
            fail(f"canonical source manifest omits playtest document: {relative}")
        if canonical_entry.get("role") != "documentation":
            fail(f"canonical source manifest has wrong role for {relative}")
        if canonical_entry.get("size") != size:
            fail(f"canonical source manifest size differs for {relative}")
        if canonical_entry.get("sha256") != digest:
            fail(f"canonical source manifest hash differs for {relative}")
        seen.add(relative)
        verified.append({
            "path": relative,
            "size": size,
            "sha256": digest,
            "role": "documentation",
        })
    return verified


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
    expected_version = str(manifest.get("version", "")).strip()
    if not expected_version:
        fail("launcher manifest has no canonical version")

    if manifest.get("schema") != 2:
        fail(f"launcher manifest schema must be 2, got {manifest.get('schema')!r}")
    if manifest.get("distribution_model") != DISTRIBUTION_MODEL:
        fail("launcher manifest distribution_model is invalid")
    platform_name = str(manifest.get("platform", ""))
    if expected_platform and platform_name != expected_platform:
        fail(
            f"native image platform {platform_name!r}, "
            f"expected {expected_platform!r}"
        )
    if manifest.get("java_release") != 17:
        fail(
            "launcher manifest java_release must be 17, "
            f"got {manifest.get('java_release')!r}"
        )
    if manifest.get("release_hardened") is not True:
        fail("native installer image requires release_hardened=true")

    launcher_jar = payload / "launcher" / "MechanistLauncher.jar"
    if not launcher_jar.is_file():
        fail(f"missing launcher jar: {launcher_jar}")
    launcher_scan = scan_jar(
        launcher_jar,
        "launcher",
        expected_version,
    )

    client = manifest.get("client")
    server = manifest.get("server")
    support = manifest.get("support_libraries")
    if not isinstance(client, dict) or not isinstance(server, dict):
        fail("launcher manifest must contain client and server objects")
    if not isinstance(support, list) or not support:
        fail("launcher manifest must contain support_libraries")
    if client.get("main_class") != EXPECTED_MAIN_CLASSES["client"]:
        fail("launcher manifest client main_class is invalid")
    if client.get("remote_main_class") != REMOTE_CLIENT_MAIN:
        fail("launcher manifest remote_main_class is invalid")

    client_jar = verify_declared_artifact(payload, client, "client")
    server_jar = verify_declared_artifact(payload, server, "server")
    client_scan = scan_jar(client_jar, "client", expected_version)
    server_scan = scan_jar(server_jar, "server", expected_version)

    support_paths: set[str] = set()
    for index, entry in enumerate(support):
        if not isinstance(entry, dict):
            fail(f"support_libraries[{index}] is not an object")
        path = verify_declared_artifact(
            payload,
            entry,
            f"support_libraries[{index}]",
        )
        relative = pathlib.PurePosixPath(
            path.relative_to(payload).as_posix()
        ).as_posix()
        if not relative.startswith("packages/support/lib/") or not relative.endswith(".jar"):
            fail(f"support library is outside governed directory: {relative}")
        if relative in support_paths:
            fail(f"support library is declared twice: {relative}")
        support_paths.add(relative)
        with zipfile.ZipFile(path) as archive:
            bad = archive.testzip()
            if bad:
                fail(f"support library contains corrupt entry {bad}: {relative}")

    runtime = runtime_root(image_root)
    java_name = "java.exe" if platform_name.startswith("windows-") else "java"
    java = runtime / "bin" / java_name
    if not java.is_file():
        fail(f"jpackage runtime is missing Java executable: {java}")

    main_launcher = native_launcher(
        image_root,
        platform_name,
        MAIN_LAUNCHER_NAME,
    )
    remote_launcher = native_launcher(
        image_root,
        platform_name,
        REMOTE_LAUNCHER_NAME,
    )
    if not main_launcher.is_file():
        fail(f"native main launcher is missing: {main_launcher}")
    if not remote_launcher.is_file():
        fail(f"native remote-lobby launcher is missing: {remote_launcher}")
    remote_config = verify_remote_launcher_configuration(image_root, payload)

    forbidden = (
        "saves",
        "settings",
        "profiles",
        "logs",
        "cache",
        "mods",
        "modsarchived",
        "export",
        "remote-client",
    )
    leaked = [name for name in forbidden if (payload / name).exists()]
    if leaked:
        fail(
            "mutable user-storage directories leaked into installer payload: "
            f"{leaked}"
        )

    certification = payload / "certification"
    source_path = certification / "installer-source-verification.json"
    canonical_copy = certification / "canonical-runtime-manifest.json"
    if not source_path.is_file():
        fail(f"missing installer source verification record: {source_path}")
    if not canonical_copy.is_file():
        fail(f"missing canonical runtime-manifest copy: {canonical_copy}")
    source = json.loads(source_path.read_text(encoding="utf-8"))

    if source.get("schema") != 4:
        fail("installer source verification schema must be 4")
    for key in (
        "version",
        "platform",
        "commit",
        "canonicalManifestSha256",
        "canonicalArtifactCount",
        "remoteClientEntryPoint",
        "portableRemoteClientLauncher",
        "nativeRemoteClientLauncher",
    ):
        if not source.get(key):
            fail(f"installer source verification record is missing {key}")
    if source.get("releaseHardened") is not True:
        fail("installer source verification record is not release hardened")
    if source.get("platform") != platform_name:
        fail("installer source verification platform does not match launcher manifest")
    if source.get("version") != manifest.get("version"):
        fail("installer source verification version does not match launcher manifest")
    if source.get("remoteClientEntryPoint") != REMOTE_CLIENT_MAIN:
        fail("installer source verification remote entry point is invalid")
    if source.get("nativeRemoteClientLauncher") != REMOTE_LAUNCHER_NAME:
        fail("installer source verification native remote launcher identity is invalid")
    if source.get("authenticatedWaitAuthority") is not True:
        fail("installer source verification did not certify authenticated wait authority")
    if source.get("waitCommand") != "WAIT":
        fail("installer source verification wait command is invalid")
    for key in (
        "movementAuthority",
        "mapAuthority",
        "fullRemoteWorldAuthority",
        "remoteGameplayCertified",
        "mutableStorageIncluded",
    ):
        require_false(source, key, "installer source verification")

    canonical = json.loads(canonical_copy.read_text(encoding="utf-8"))
    if sha256(canonical_copy) != source.get("canonicalManifestSha256"):
        fail("installer source canonical-manifest hash is invalid")
    if canonical.get("version") != source.get("version"):
        fail("canonical source manifest version differs from source certificate")
    if canonical.get("platform") != source.get("platform"):
        fail("canonical source manifest platform differs from source certificate")
    if canonical.get("commit") != source.get("commit"):
        fail("canonical source manifest commit differs from source certificate")
    if canonical.get("releaseHardened") is not True:
        fail("canonical source manifest is not release hardened")
    if canonical.get("remoteClientEntryPoint") != REMOTE_CLIENT_MAIN:
        fail("canonical source manifest remote entry point is invalid")
    if len(canonical.get("artifacts", [])) != int(source["canonicalArtifactCount"]):
        fail("canonical source artifact count differs from source certificate")

    playtest_documents = verify_playtest_documents(payload, source, canonical)

    return {
        "status": "verified",
        "image": image_root.name,
        "payload": str(payload),
        "runtimeRoot": str(runtime),
        "version": manifest.get("version"),
        "platform": platform_name,
        "commit": source.get("commit"),
        "releaseHardened": True,
        "supportLibraryCount": len(support_paths),
        "runtimeJava": str(java),
        "mainNativeLauncher": str(main_launcher),
        "remoteLobbyNativeLauncher": str(remote_launcher),
        "remoteLobbyLauncherConfig": str(remote_config),
        "remoteLobbyEntryPoint": REMOTE_CLIENT_MAIN,
        "remoteLobbyNativeExecutable": True,
        "playtestOperationsVerified": True,
        "playtestDocumentCount": len(playtest_documents),
        "playtestDocuments": playtest_documents,
        "authenticatedWaitAuthority": True,
        "waitCommand": "WAIT",
        "movementAuthority": False,
        "mapAuthority": False,
        "fullRemoteWorldAuthority": False,
        "remoteGameplayCertified": False,
        "canonicalSourceManifest": str(canonical_copy),
        "canonicalSourceManifestSha256": sha256(canonical_copy),
        "canonicalSourceArtifactCount": len(canonical.get("artifacts", [])),
        "installerSourceVerificationSchema": 4,
        "mutableStorageIncluded": False,
        "jars": {
            "launcher": launcher_scan,
            "client": client_scan,
            "server": server_scan,
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
    except Exception as exc:  # noqa: BLE001 - one native-image failure surface
        print(
            f"NATIVE INSTALLER IMAGE VERIFICATION FAILED: {exc}",
            file=sys.stderr,
        )
        return 1
    rendered = json.dumps(summary, indent=2, sort_keys=True)
    print(rendered)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

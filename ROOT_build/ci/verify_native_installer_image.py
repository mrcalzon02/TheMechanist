#!/usr/bin/env python3
"""Verify a jpackage app-image built from a canonical The Mechanist distribution."""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import sys
import zipfile

from verify_runnable_distribution import (
    EXPECTED_MAIN_CLASSES,
    REMOTE_CLIENT_MAIN,
    manifest_main_class,
    scan_jar,
)

LAUNCHER_MANIFEST = pathlib.PurePosixPath(
    "manifests/launcher-runtime-manifest.json"
)
MAIN_LAUNCHER_NAME = "The Mechanist"
REMOTE_LAUNCHER_NAME = "The Mechanist Remote Lobby"


def fail(message: str) -> None:
    raise RuntimeError(message)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def payload_root(image_root: pathlib.Path) -> pathlib.Path:
    candidates = (
        image_root / "app",
        image_root / "lib" / "app",
        image_root,
    )
    for candidate in candidates:
        if (candidate / "manifests").is_dir() and (
            candidate / "packages"
        ).is_dir():
            return candidate
    fail(f"could not locate jpackage application payload under {image_root}")
    return image_root


def runtime_root(image_root: pathlib.Path) -> pathlib.Path:
    candidates = (
        image_root / "runtime",
        image_root / "lib" / "runtime",
    )
    for candidate in candidates:
        if candidate.is_dir():
            return candidate
    fail(f"could not locate jpackage runtime under {image_root}")
    return image_root


def platform_java(image_root: pathlib.Path, platform_name: str) -> pathlib.Path:
    executable = "java.exe" if platform_name.startswith("windows-") else "java"
    return runtime_root(image_root) / "bin" / executable


def native_launcher(
    image_root: pathlib.Path,
    platform_name: str,
    launcher_name: str,
) -> pathlib.Path:
    if platform_name.startswith("windows-"):
        return image_root / f"{launcher_name}.exe"
    return image_root / "bin" / launcher_name


def portable_remote_launcher(platform_name: str) -> str:
    return (
        "Run-Remote-Client.cmd"
        if platform_name.startswith("windows-")
        else "run-remote-client.sh"
    )


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
    candidates = sorted(
        {
            *image_root.rglob("*.cfg"),
            *payload.rglob("*.cfg"),
        }
    )
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
        f"{REMOTE_LAUNCHER_NAME!r} to {REMOTE_CLIENT_MAIN} "
        "and the verified client JAR"
    )
    return image_root


def verify_source_certification(
    payload: pathlib.Path,
    manifest: dict[str, object],
    platform_name: str,
) -> dict[str, object]:
    certification_root = payload / "certification"
    source_path = certification_root / "installer-source-verification.json"
    canonical_path = certification_root / "canonical-runtime-manifest.json"
    if not source_path.is_file():
        fail(f"missing installer source verification record: {source_path}")
    if not canonical_path.is_file():
        fail(f"missing certified canonical runtime manifest: {canonical_path}")

    source = json.loads(source_path.read_text(encoding="utf-8"))
    if source.get("schema") != 2:
        fail("installer source verification schema must be 2")
    for key in (
        "sourceDistribution",
        "version",
        "platform",
        "commit",
        "javaRelease",
        "canonicalManifestSha256",
        "canonicalArtifactCount",
        "launcherCompatibilityManifest",
        "remoteClientEntryPoint",
        "portableRemoteClientLauncher",
        "nativeRemoteClientLauncher",
        "runtimeImageSource",
    ):
        if source.get(key) in (None, ""):
            fail(f"installer source verification record is missing {key}")
    if source.get("releaseHardened") is not True:
        fail("installer source verification record is not release hardened")
    if source.get("javaRelease") != 17:
        fail("installer source verification Java release must be 17")
    if source.get("platform") != platform_name:
        fail(
            "installer source verification platform does not match "
            "launcher manifest"
        )
    if source.get("version") != manifest.get("version"):
        fail(
            "installer source verification version does not match "
            "launcher manifest"
        )
    if source.get("remoteClientEntryPoint") != REMOTE_CLIENT_MAIN:
        fail("installer source verification remote entry is invalid")
    if source.get("portableRemoteClientLauncher") != portable_remote_launcher(
        platform_name
    ):
        fail("installer source verification portable remote launcher is invalid")
    if source.get("nativeRemoteClientLauncher") != REMOTE_LAUNCHER_NAME:
        fail("installer source verification native remote launcher is invalid")
    if source.get("mutableStorageIncluded") is not False:
        fail("installer source verification must declare mutable storage absent")
    if source.get("remoteWorldAuthority") is not False:
        fail("installer source verification overclaims remote world authority")
    if source.get("launcherCompatibilityManifest") != LAUNCHER_MANIFEST.as_posix():
        fail("installer source verification launcher manifest path is invalid")

    actual_canonical_hash = sha256(canonical_path)
    if actual_canonical_hash.lower() != str(
        source.get("canonicalManifestSha256", "")
    ).lower():
        fail("certified canonical runtime manifest hash does not match source record")

    canonical = json.loads(canonical_path.read_text(encoding="utf-8"))
    if canonical.get("schema") != 2:
        fail("certified canonical runtime manifest schema must be 2")
    if canonical.get("version") != source.get("version"):
        fail("certified canonical manifest version differs from source record")
    if canonical.get("platform") != platform_name:
        fail("certified canonical manifest platform differs from source record")
    if canonical.get("commit") != source.get("commit"):
        fail("certified canonical manifest commit differs from source record")
    if canonical.get("javaRelease") != 17:
        fail("certified canonical manifest Java release must be 17")
    if canonical.get("releaseHardened") is not True:
        fail("certified canonical manifest is not release hardened")
    if canonical.get("remoteClientEntryPoint") != REMOTE_CLIENT_MAIN:
        fail("certified canonical manifest remote entry is invalid")
    artifacts = canonical.get("artifacts")
    if not isinstance(artifacts, list):
        fail("certified canonical manifest artifacts must be a list")
    if len(artifacts) != int(source.get("canonicalArtifactCount")):
        fail("certified canonical artifact count differs from source record")

    return {
        "path": str(source_path),
        "schema": 2,
        "sourceDistribution": source.get("sourceDistribution"),
        "commit": source.get("commit"),
        "canonicalManifestSha256": actual_canonical_hash,
        "canonicalArtifactCount": len(artifacts),
        "remoteClientEntryPoint": REMOTE_CLIENT_MAIN,
        "portableRemoteClientLauncher": source.get(
            "portableRemoteClientLauncher"
        ),
        "nativeRemoteClientLauncher": REMOTE_LAUNCHER_NAME,
        "mutableStorageIncluded": False,
        "remoteWorldAuthority": False,
        "status": "verified",
    }


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
        fail(
            f"launcher manifest schema must be 2, "
            f"got {manifest.get('schema')!r}"
        )
    if manifest.get("distribution_model") != (
        "installer-thin-launcher-client-server"
    ):
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
    if manifest_main_class(launcher_jar) != EXPECTED_MAIN_CLASSES["launcher"]:
        fail("launcher jar has the wrong Main-Class")
    launcher_scan = scan_jar(launcher_jar, "launcher")

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
    client_scan = scan_jar(client_jar, "client")
    server_scan = scan_jar(server_jar, "server")

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
        if (
            not relative.startswith("packages/support/lib/")
            or not relative.endswith(".jar")
        ):
            fail(f"support library is outside governed directory: {relative}")
        if relative in support_paths:
            fail(f"support library is declared twice: {relative}")
        support_paths.add(relative)
        with zipfile.ZipFile(path) as archive:
            bad = archive.testzip()
            if bad:
                fail(
                    f"support library contains corrupt entry {bad}: {relative}"
                )

    java = platform_java(image_root, platform_name)
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

    source_certification = verify_source_certification(
        payload,
        manifest,
        platform_name,
    )

    return {
        "status": "verified",
        "image": image_root.name,
        "payload": str(payload),
        "runtimeRoot": str(runtime_root(image_root)),
        "version": manifest.get("version"),
        "platform": platform_name,
        "releaseHardened": True,
        "supportLibraryCount": len(support_paths),
        "runtimeJava": str(java),
        "mainNativeLauncher": str(main_launcher),
        "remoteLobbyNativeLauncher": str(remote_launcher),
        "remoteLobbyLauncherConfig": str(remote_config),
        "remoteLobbyEntryPoint": REMOTE_CLIENT_MAIN,
        "remoteLobbyNativeExecutable": True,
        "mutableStorageIncluded": False,
        "remoteWorldAuthority": False,
        "sourceCertification": source_certification,
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

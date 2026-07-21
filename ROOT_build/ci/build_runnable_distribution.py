#!/usr/bin/env python3
"""Build a self-contained The Mechanist portable distribution.

This builder is shared by GitHub Actions and local release verification. It
compiles the client/server and launcher with Java 17, stages launcher-managed
packages and runtime dependencies, creates a platform Java runtime with jlink,
writes canonical and launcher-compatibility integrity manifests, includes the
governed limited-alpha operating documentation, and produces a verified ZIP
candidate.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import pathlib
import platform as host_platform
import shutil
import subprocess
import sys
import zipfile

RUNTIME_MODULES = (
    "java.base,java.desktop,java.logging,java.management,java.naming,"
    "java.net.http,java.prefs,java.sql,java.xml,jdk.crypto.ec,jdk.unsupported"
)
PLAYTEST_DOCS = (
    ("PACKAGE_client/EULA.md", "EULA.md"),
    ("PACKAGE_client/README.md", "CLIENT_README.md"),
    ("PACKAGE_client/RUN_INSTRUCTIONS.md", "RUN_INSTRUCTIONS.md"),
    ("PACKAGE_client/WINDOWS_QUICK_START.md", "WINDOWS_QUICK_START.md"),
    (
        "PACKAGE_client/LIMITED_ALPHA_PLAYTEST_GUIDE.md",
        "LIMITED_ALPHA_PLAYTEST_GUIDE.md",
    ),
    (
        "PACKAGE_client/KNOWN_ALPHA_LIMITATIONS.md",
        "KNOWN_ALPHA_LIMITATIONS.md",
    ),
    ("PACKAGE_client/DIAGNOSTIC_COLLECTION.md", "DIAGNOSTIC_COLLECTION.md"),
    ("PACKAGE_client/server/README.md", "SERVER_README.md"),
)
PLAYTEST_DOCUMENT_COUNT = 8


def run(command: list[str], cwd: pathlib.Path, env: dict[str, str] | None = None) -> None:
    print("+", " ".join(command), flush=True)
    subprocess.run(command, cwd=cwd, env=env, check=True)


def executable(name: str) -> str:
    return name + (".exe" if os.name == "nt" else "")


def maven_command() -> str:
    return "mvn.cmd" if os.name == "nt" else "mvn"


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def project_version(repo: pathlib.Path) -> str:
    result = subprocess.run(
        [
            maven_command(),
            "-q",
            "-DforceStdout",
            "help:evaluate",
            "-Dexpression=project.version",
        ],
        cwd=repo,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
    )
    lines = [line.strip() for line in result.stdout.splitlines() if line.strip()]
    if not lines:
        raise RuntimeError("Maven did not report the project version")
    return lines[-1]


def platform_id() -> str:
    machine = host_platform.machine().lower()
    arch = "x64" if machine in {"x86_64", "amd64"} else machine.replace(
        "aarch64", "arm64"
    )
    if os.name == "nt":
        return f"windows-{arch}"
    if sys.platform.startswith("linux"):
        return f"linux-{arch}"
    if sys.platform == "darwin":
        return f"macos-{arch}"
    raise RuntimeError(f"unsupported release platform: {sys.platform}")


def native_classifier(platform_name: str) -> str:
    if platform_name.startswith("windows-"):
        return "natives-windows"
    if platform_name.startswith("linux-"):
        return "natives-linux"
    if platform_name.startswith("macos-"):
        return "natives-macos"
    raise RuntimeError(f"unsupported native-library platform: {platform_name}")


def dependency_matches_platform(path: pathlib.Path, platform_name: str) -> bool:
    name = path.name.lower()
    return "natives-" not in name or native_classifier(platform_name) in name


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


def copy_file(source: pathlib.Path, destination: pathlib.Path) -> None:
    if not source.is_file() or source.stat().st_size <= 0:
        raise RuntimeError(f"required build artifact missing or empty: {source}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)
    if destination.stat().st_size != source.stat().st_size:
        raise RuntimeError(f"copied build artifact size mismatch: {destination}")


def jar_implementation_version(path: pathlib.Path) -> str:
    if not path.is_file() or path.stat().st_size <= 0:
        raise RuntimeError(f"versioned JAR is missing or empty: {path}")
    try:
        with zipfile.ZipFile(path) as archive:
            manifest = archive.read("META-INF/MANIFEST.MF").decode(
                "utf-8", errors="strict"
            )
    except (KeyError, OSError, UnicodeDecodeError, zipfile.BadZipFile) as exc:
        raise RuntimeError(f"cannot read JAR manifest version: {path}") from exc

    entries: dict[str, str] = {}
    current_key: str | None = None
    for raw_line in manifest.splitlines():
        if raw_line.startswith(" ") and current_key is not None:
            entries[current_key] += raw_line[1:]
            continue
        if ": " not in raw_line:
            current_key = None
            continue
        key, value = raw_line.split(": ", 1)
        entries[key] = value
        current_key = key

    version = entries.get("Implementation-Version", "").strip()
    if not version:
        raise RuntimeError(f"JAR has no Implementation-Version: {path}")
    return version


def require_jar_version(path: pathlib.Path, expected: str, role: str) -> str:
    actual = jar_implementation_version(path)
    if actual != expected:
        raise RuntimeError(
            f"{role} version mismatch: expected {expected}, found {actual} in {path}"
        )
    return actual


def stage_playtest_documents(repo: pathlib.Path, distribution: pathlib.Path) -> list[str]:
    if len(PLAYTEST_DOCS) != PLAYTEST_DOCUMENT_COUNT:
        raise RuntimeError(
            f"builder must govern exactly {PLAYTEST_DOCUMENT_COUNT} tester documents"
        )
    docs = distribution / "docs"
    destinations: list[str] = []
    seen: set[str] = set()
    for source_relative, destination_name in PLAYTEST_DOCS:
        if destination_name in seen:
            raise RuntimeError(
                f"builder repeats tester-document destination: {destination_name}"
            )
        source = repo.joinpath(*pathlib.PurePosixPath(source_relative).parts)
        destination = docs / destination_name
        copy_file(source, destination)
        if sha256(source) != sha256(destination):
            raise RuntimeError(
                f"tester document changed while staging: {source_relative}"
            )
        seen.add(destination_name)
        destinations.append(destination_name)
    return destinations


def write_launchers(root: pathlib.Path, platform_name: str) -> None:
    if platform_name.startswith("windows-"):
        scripts = {
            "Run-The-Mechanist.cmd": (
                "@echo off\r\nsetlocal\r\nset ROOT=%~dp0\r\n"
                '"%ROOT%runtime\\bin\\java.exe" -jar '
                '"%ROOT%launcher\\MechanistLauncher.jar" %*\r\n'
            ),
            "Run-Client-Direct.cmd": (
                "@echo off\r\nsetlocal\r\nset ROOT=%~dp0\r\n"
                '"%ROOT%runtime\\bin\\java.exe" -cp '
                '"%ROOT%packages\\client\\TheMechanist.jar;%ROOT%packages\\support\\lib\\*" '
                "mechanist.TheMechanist %*\r\n"
            ),
            "Run-Remote-Client.cmd": (
                "@echo off\r\nsetlocal\r\nset ROOT=%~dp0\r\n"
                '"%ROOT%runtime\\bin\\java.exe" -cp '
                '"%ROOT%packages\\client\\TheMechanist.jar;%ROOT%packages\\support\\lib\\*" '
                "mechanist.RemoteClientMain %*\r\n"
            ),
            "Run-Server.cmd": (
                "@echo off\r\nsetlocal\r\nset ROOT=%~dp0\r\n"
                '"%ROOT%runtime\\bin\\java.exe" -cp '
                '"%ROOT%packages\\server\\TheMechanistServer.jar;%ROOT%packages\\support\\lib\\*" '
                "mechanist.MechanistServerMain %*\r\n"
            ),
        }
    else:
        scripts = {
            "run-the-mechanist.sh": (
                '#!/usr/bin/env sh\nset -eu\nROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)\n'
                'exec "$ROOT/runtime/bin/java" -jar "$ROOT/launcher/MechanistLauncher.jar" "$@"\n'
            ),
            "run-client-direct.sh": (
                '#!/usr/bin/env sh\nset -eu\nROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)\n'
                'exec "$ROOT/runtime/bin/java" -cp '
                '"$ROOT/packages/client/TheMechanist.jar:$ROOT/packages/support/lib/*" '
                'mechanist.TheMechanist "$@"\n'
            ),
            "run-remote-client.sh": (
                '#!/usr/bin/env sh\nset -eu\nROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)\n'
                'exec "$ROOT/runtime/bin/java" -cp '
                '"$ROOT/packages/client/TheMechanist.jar:$ROOT/packages/support/lib/*" '
                'mechanist.RemoteClientMain "$@"\n'
            ),
            "run-server.sh": (
                '#!/usr/bin/env sh\nset -eu\nROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)\n'
                'exec "$ROOT/runtime/bin/java" -cp '
                '"$ROOT/packages/server/TheMechanistServer.jar:$ROOT/packages/support/lib/*" '
                'mechanist.MechanistServerMain "$@"\n'
            ),
        }
    for name, content in scripts.items():
        path = root / name
        path.write_text(content, encoding="utf-8", newline="")
        if name.endswith(".sh"):
            path.chmod(0o755)


def artifact_role(relative: pathlib.PurePosixPath) -> str:
    text = relative.as_posix()
    if text == "launcher/MechanistLauncher.jar":
        return "launcher"
    if text == "packages/client/TheMechanist.jar":
        return "client"
    if text == "packages/server/TheMechanistServer.jar":
        return "server"
    if text.startswith("packages/support/lib/"):
        return "support"
    if text.startswith("runtime/"):
        return "runtime"
    if text.startswith("docs/"):
        return "documentation"
    if text.startswith("manifests/"):
        return "manifest"
    if text.endswith(".sh") or text.endswith(".cmd"):
        return "launch-script"
    return "distribution-file"


def artifact_record(root: pathlib.Path, relative: str, role: str) -> dict[str, object]:
    path = root.joinpath(*pathlib.PurePosixPath(relative).parts)
    if not path.is_file():
        raise RuntimeError(f"launcher manifest artifact is missing: {path}")
    return {
        "role": role,
        "path": relative,
        "size": path.stat().st_size,
        "sha256": sha256(path),
    }


def write_launcher_compatibility_manifest(
    root: pathlib.Path,
    version: str,
    platform_name: str,
    release_hardened: bool,
) -> pathlib.Path:
    client = artifact_record(root, "packages/client/TheMechanist.jar", "client")
    server = artifact_record(root, "packages/server/TheMechanistServer.jar", "server")
    support: list[dict[str, object]] = []
    support_root = root / "packages" / "support" / "lib"
    for path in sorted(support_root.glob("*.jar")):
        relative = pathlib.PurePosixPath(path.relative_to(root).as_posix()).as_posix()
        support.append(artifact_record(root, relative, "support"))
    if not support:
        raise RuntimeError("launcher compatibility manifest requires support libraries")

    payload = {
        "schema": 2,
        "distribution_model": "installer-thin-launcher-client-server",
        "version": version,
        "platform": platform_name,
        "java_release": 17,
        "release_hardened": release_hardened,
        "client": {
            "path": client["path"],
            "sha256": client["sha256"],
            "size": client["size"],
            "main_class": "mechanist.TheMechanist",
            "remote_main_class": "mechanist.RemoteClientMain",
            "launcher_main_class": "mechanist.launcher.ThinLauncherMain",
        },
        "server": {
            "path": server["path"],
            "sha256": server["sha256"],
            "size": server["size"],
            "main_class": "mechanist.MechanistServerMain",
        },
        "support_libraries": [
            {"path": item["path"], "sha256": item["sha256"], "size": item["size"]}
            for item in support
        ],
    }
    path = root / "manifests" / "launcher-runtime-manifest.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return path


def write_manifest(
    root: pathlib.Path,
    version: str,
    platform_name: str,
    commit: str,
    release_hardened: bool,
) -> pathlib.Path:
    artifacts: list[dict[str, object]] = []
    manifest_path = root / "manifests" / "runtime-manifest.json"
    for path in sorted(root.rglob("*")):
        if not path.is_file() or path == manifest_path:
            continue
        relative = pathlib.PurePosixPath(path.relative_to(root).as_posix())
        artifacts.append({
            "role": artifact_role(relative),
            "path": relative.as_posix(),
            "size": path.stat().st_size,
            "sha256": sha256(path),
        })
    payload = {
        "schema": 2,
        "distributionModel": "installer-thin-launcher-client-server",
        "version": version,
        "platform": platform_name,
        "commit": commit,
        "javaRelease": 17,
        "releaseHardened": release_hardened,
        "remoteClientEntryPoint": "mechanist.RemoteClientMain",
        "artifacts": artifacts,
    }
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return manifest_path


def zip_distribution(root: pathlib.Path, archive: pathlib.Path) -> None:
    archive.parent.mkdir(parents=True, exist_ok=True)
    if archive.exists():
        archive.unlink()
    with zipfile.ZipFile(
        archive,
        "w",
        compression=zipfile.ZIP_DEFLATED,
        compresslevel=9,
    ) as output:
        for path in sorted(root.rglob("*")):
            if not path.is_file():
                continue
            arcname = pathlib.PurePosixPath(root.name) / pathlib.PurePosixPath(
                path.relative_to(root).as_posix()
            )
            output.write(path, arcname.as_posix())


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=pathlib.Path, default=pathlib.Path.cwd())
    parser.add_argument("--version")
    parser.add_argument("--commit", default=os.environ.get("GITHUB_SHA", "local"))
    parser.add_argument("--release-hardened", action="store_true")
    parser.add_argument("--output", type=pathlib.Path, default=pathlib.Path("dist/releases"))
    args = parser.parse_args()

    repo = args.repo.resolve()
    version = args.version or project_version(repo)
    current_platform = platform_id()
    release_hardened = args.release_hardened or github_publish_prerelease_requested()
    output_root = (repo / args.output).resolve() if not args.output.is_absolute() else args.output
    distribution = output_root / f"TheMechanist-{version}-{current_platform}"
    archive = output_root / f"TheMechanist-{version}-{current_platform}.zip"
    shutil.rmtree(distribution, ignore_errors=True)
    distribution.mkdir(parents=True, exist_ok=True)

    maven = maven_command()
    root_package = [maven, "-B", "-DskipTests", "clean", "package"]
    if release_hardened:
        root_package.insert(2, "-Prelease-obfuscation")
    run(root_package, repo)
    run(
        [
            maven,
            "-B",
            "-DskipTests",
            f"-Drevision={version}",
            "clean",
            "package",
        ],
        repo / "PACKAGE_launcher" / "java",
    )

    dependencies = repo / "target" / f"release-support-{current_platform}"
    shutil.rmtree(dependencies, ignore_errors=True)
    run(
        [
            maven,
            "-B",
            "dependency:copy-dependencies",
            "-DincludeScope=runtime",
            f"-DoutputDirectory={dependencies}",
        ],
        repo,
    )

    client_source = repo / "target" / (
        "TheMechanist-obfuscated.jar" if release_hardened else "TheMechanist-all.jar"
    )
    server_source = repo / "target" / (
        "TheMechanistServer-obfuscated.jar"
        if release_hardened
        else "TheMechanistServer-all.jar"
    )
    launcher_candidates = sorted(
        (repo / "PACKAGE_launcher" / "java" / "target").glob(
            "mechanist-launcher-*.jar"
        )
    )
    launcher_candidates = [
        path for path in launcher_candidates if not path.name.startswith("original-")
    ]
    if not launcher_candidates:
        raise RuntimeError("launcher Maven build did not produce a runnable JAR")
    launcher_source = launcher_candidates[-1]

    runtime_artifact_versions = {
        "client": require_jar_version(client_source, version, "client"),
        "server": require_jar_version(server_source, version, "server"),
        "launcher": require_jar_version(launcher_source, version, "launcher"),
    }

    copy_file(
        client_source,
        distribution / "packages" / "client" / "TheMechanist.jar",
    )
    copy_file(
        server_source,
        distribution / "packages" / "server" / "TheMechanistServer.jar",
    )
    copy_file(
        launcher_source,
        distribution / "launcher" / "MechanistLauncher.jar",
    )

    support_target = distribution / "packages" / "support" / "lib"
    support_target.mkdir(parents=True, exist_ok=True)
    for dependency in sorted(dependencies.glob("*.jar")):
        if dependency_matches_platform(dependency, current_platform):
            copy_file(dependency, support_target / dependency.name)

    java_home = pathlib.Path(os.environ.get("JAVA_HOME", ""))
    jlink = java_home / "bin" / executable("jlink")
    if not jlink.is_file():
        raise RuntimeError(f"JAVA_HOME does not provide jlink: {jlink}")
    run(
        [
            str(jlink),
            "--add-modules",
            RUNTIME_MODULES,
            "--strip-debug",
            "--no-header-files",
            "--no-man-pages",
            "--compress=2",
            "--output",
            str(distribution / "runtime"),
        ],
        repo,
    )

    playtest_documents = stage_playtest_documents(repo, distribution)
    write_launchers(distribution, current_platform)
    write_launcher_compatibility_manifest(
        distribution,
        version,
        current_platform,
        release_hardened,
    )
    write_manifest(
        distribution,
        version,
        current_platform,
        args.commit,
        release_hardened,
    )
    zip_distribution(distribution, archive)

    result = {
        "distribution": str(distribution),
        "archive": str(archive),
        "version": version,
        "platform": current_platform,
        "releaseHardened": release_hardened,
        "versionAuthorityVerifiedAtBuild": True,
        "runtimeArtifactVersions": runtime_artifact_versions,
        "remoteClientEntryPoint": "mechanist.RemoteClientMain",
        "playtestOperationsVerifiedAtBuild": True,
        "playtestDocumentCount": len(playtest_documents),
        "playtestDocuments": playtest_documents,
    }
    print(json.dumps(result, indent=2))
    if os.environ.get("GITHUB_OUTPUT"):
        with pathlib.Path(os.environ["GITHUB_OUTPUT"]).open(
            "a", encoding="utf-8"
        ) as output:
            output.write(f"distribution={distribution}\n")
            output.write(f"archive={archive}\n")
            output.write(f"version={version}\n")
            output.write(f"platform={current_platform}\n")
            output.write(f"release_hardened={str(release_hardened).lower()}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

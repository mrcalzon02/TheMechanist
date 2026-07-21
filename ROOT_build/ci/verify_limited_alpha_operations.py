#!/usr/bin/env python3
"""Verify limited-alpha tester operations and packaged support documentation.

This verifier owns the release contract for tester-facing instructions. It can
validate repository source documents before compilation and reopen a staged
portable distribution to prove that the required documents are present,
manifest-covered, and consistent with the candidate identity boundary.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import pathlib
import platform as host_platform
import sys
import traceback
from typing import Iterable

SCHEMA = 2

SOURCE_DOCS = {
    "PACKAGE_client/EULA.md": "EULA.md",
    "PACKAGE_client/README.md": "CLIENT_README.md",
    "PACKAGE_client/RUN_INSTRUCTIONS.md": "RUN_INSTRUCTIONS.md",
    "PACKAGE_client/WINDOWS_QUICK_START.md": "WINDOWS_QUICK_START.md",
    "PACKAGE_client/LIMITED_ALPHA_PLAYTEST_GUIDE.md": "LIMITED_ALPHA_PLAYTEST_GUIDE.md",
    "PACKAGE_client/KNOWN_ALPHA_LIMITATIONS.md": "KNOWN_ALPHA_LIMITATIONS.md",
    "PACKAGE_client/DIAGNOSTIC_COLLECTION.md": "DIAGNOSTIC_COLLECTION.md",
    "PACKAGE_client/server/README.md": "SERVER_README.md",
}

SOURCE_REQUIREMENTS = {
    "PACKAGE_client/LIMITED_ALPHA_PLAYTEST_GUIDE.md": (
        "authorized playtest personnel",
        "SHA256SUMS.txt",
        "Windows x64",
        "Linux x64",
        "Save protection",
        "Modified content",
        "Reporting a problem",
        "Severity guide",
        "Candidate and evaluation boundary",
        "not yet an authoritative remote gameplay server",
    ),
    "PACKAGE_client/KNOWN_ALPHA_LIMITATIONS.md": (
        "Single-player authority",
        "Independent host",
        "does not initialize or own authoritative remote world state",
        "Assets and redistribution",
        "Support boundary",
        "resume-token custody file",
    ),
    "PACKAGE_client/DIAGNOSTIC_COLLECTION.md": (
        "Exact source commit",
        "Release-hardening state",
        "Modified packages",
        "Before sharing",
        "Manual collection when the launcher cannot start",
        "Crash reproduction",
        "Save and persistence defects",
        "Independent host defects",
        "resume-token custody",
    ),
    "PACKAGE_client/RUN_INSTRUCTIONS.md": (
        "Java 17",
        "launcher",
    ),
    "PACKAGE_client/server/README.md": (
        "bind",
        "server",
    ),
}

ISSUE_FORM = ".github/ISSUE_TEMPLATE/limited-alpha-defect.yml"
ISSUE_FORM_REQUIREMENTS = (
    "name: Limited Alpha Defect",
    "id: severity",
    "id: candidate_version",
    "id: source_commit",
    "id: platform",
    "id: reproduction",
    "id: expected",
    "id: actual",
    "id: diagnostics",
    "resume-token custody",
    "modified content",
)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def require_file(path: pathlib.Path, label: str) -> str:
    if not path.is_file():
        raise RuntimeError(f"required {label} is missing: {path}")
    text = path.read_text(encoding="utf-8")
    if not text.strip():
        raise RuntimeError(f"required {label} is empty: {path}")
    return text


def require_phrases(text: str, phrases: Iterable[str], label: str) -> None:
    folded = text.casefold()
    missing = [phrase for phrase in phrases if phrase.casefold() not in folded]
    if missing:
        raise RuntimeError(
            f"{label} is missing required alpha-operating content: {', '.join(missing)}"
        )


def detect_platform() -> str:
    machine = host_platform.machine().lower()
    if machine not in {"x86_64", "amd64"}:
        raise RuntimeError(f"unsupported limited-alpha architecture: {machine}")
    if os.name == "nt":
        return "windows-x64"
    if sys.platform.startswith("linux"):
        return "linux-x64"
    raise RuntimeError(f"unsupported limited-alpha operating system: {sys.platform}")


def find_distribution(search_root: pathlib.Path) -> pathlib.Path:
    platform_name = detect_platform()
    matches = sorted(
        path.resolve()
        for path in search_root.glob(f"TheMechanist-*-{platform_name}")
        if path.is_dir()
    )
    if len(matches) != 1:
        raise RuntimeError(
            f"expected exactly one {platform_name} canonical distribution under "
            f"{search_root}; found {matches}"
        )
    return matches[0]


def source_record(repo: pathlib.Path, relative: str, destination: str) -> dict[str, object]:
    path = repo / relative
    text = require_file(path, "limited-alpha source document")
    requirements = SOURCE_REQUIREMENTS.get(relative, ())
    require_phrases(text, requirements, relative)
    return {
        "source": relative,
        "destination": f"docs/{destination}",
        "size": path.stat().st_size,
        "sha256": sha256(path),
        "requiredPhraseCount": len(requirements),
    }


def verify_source(repo: pathlib.Path) -> dict[str, object]:
    documents = [
        source_record(repo, source, destination)
        for source, destination in SOURCE_DOCS.items()
    ]
    issue_path = repo / ISSUE_FORM
    issue_text = require_file(issue_path, "limited-alpha issue form")
    require_phrases(issue_text, ISSUE_FORM_REQUIREMENTS, ISSUE_FORM)
    return {
        "status": "verified",
        "documents": documents,
        "issueForm": {
            "path": ISSUE_FORM,
            "size": issue_path.stat().st_size,
            "sha256": sha256(issue_path),
        },
    }


def artifact_map(manifest: dict[str, object]) -> dict[str, dict[str, object]]:
    artifacts = manifest.get("artifacts")
    if not isinstance(artifacts, list):
        raise RuntimeError("canonical runtime manifest has no artifact list")
    result: dict[str, dict[str, object]] = {}
    for entry in artifacts:
        if not isinstance(entry, dict):
            raise RuntimeError("canonical runtime manifest contains a malformed artifact")
        path = entry.get("path")
        if not isinstance(path, str) or not path:
            raise RuntimeError("canonical runtime manifest contains an artifact without a path")
        if path in result:
            raise RuntimeError(f"canonical runtime manifest repeats artifact path: {path}")
        result[path] = entry
    return result


def verify_distribution(
    distribution: pathlib.Path,
    source: dict[str, object],
    require_release_hardened: bool,
) -> dict[str, object]:
    manifest_path = distribution / "manifests" / "runtime-manifest.json"
    manifest_text = require_file(manifest_path, "canonical runtime manifest")
    manifest = json.loads(manifest_text)
    if not isinstance(manifest, dict):
        raise RuntimeError("canonical runtime manifest root must be an object")
    if require_release_hardened and manifest.get("releaseHardened") is not True:
        raise RuntimeError("limited-alpha operations require releaseHardened=true")
    version = manifest.get("version")
    commit = manifest.get("commit")
    platform = manifest.get("platform")
    java_release = manifest.get("javaRelease")
    if not isinstance(version, str) or not version.strip():
        raise RuntimeError("canonical runtime manifest has no version identity")
    if not isinstance(commit, str) or len(commit.strip()) != 40:
        raise RuntimeError("canonical runtime manifest has no exact 40-character commit identity")
    if not isinstance(platform, str) or not platform.strip():
        raise RuntimeError("canonical runtime manifest has no platform identity")
    if java_release != 17:
        raise RuntimeError(f"limited-alpha package must target Java 17, found {java_release!r}")

    artifacts = artifact_map(manifest)
    packaged: list[dict[str, object]] = []
    source_documents = source.get("documents")
    if not isinstance(source_documents, list):
        raise RuntimeError("source verification did not return its document records")
    for record in source_documents:
        if not isinstance(record, dict):
            raise RuntimeError("source verification returned a malformed document record")
        relative = record.get("destination")
        if not isinstance(relative, str):
            raise RuntimeError("source document record has no packaged destination")
        path = distribution.joinpath(*pathlib.PurePosixPath(relative).parts)
        require_file(path, "packaged limited-alpha document")
        manifest_entry = artifacts.get(relative)
        if manifest_entry is None:
            raise RuntimeError(f"packaged alpha document is absent from the manifest: {relative}")
        if manifest_entry.get("role") != "documentation":
            raise RuntimeError(f"packaged alpha document has wrong manifest role: {relative}")
        actual_size = path.stat().st_size
        actual_hash = sha256(path)
        if manifest_entry.get("size") != actual_size:
            raise RuntimeError(f"packaged alpha document size disagrees with manifest: {relative}")
        if manifest_entry.get("sha256") != actual_hash:
            raise RuntimeError(f"packaged alpha document hash disagrees with manifest: {relative}")
        if actual_hash != record.get("sha256"):
            raise RuntimeError(f"packaged alpha document differs from its governed source: {relative}")
        packaged.append({
            "path": relative,
            "size": actual_size,
            "sha256": actual_hash,
            "role": "documentation",
        })

    return {
        "status": "verified",
        "root": str(distribution),
        "version": version,
        "commit": commit,
        "platform": platform,
        "javaRelease": java_release,
        "releaseHardened": manifest.get("releaseHardened") is True,
        "documents": packaged,
        "manifest": str(manifest_path),
        "manifestSha256": sha256(manifest_path),
    }


def write_report(path: pathlib.Path, report: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=pathlib.Path, default=pathlib.Path.cwd())
    distribution_group = parser.add_mutually_exclusive_group()
    distribution_group.add_argument("--distribution", type=pathlib.Path)
    distribution_group.add_argument("--distribution-search-root", type=pathlib.Path)
    parser.add_argument("--require-release-hardened", action="store_true")
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()

    repo = args.repo.resolve()
    distribution_requested = (
        args.distribution is not None or args.distribution_search_root is not None
    )
    report: dict[str, object] = {
        "schema": SCHEMA,
        "status": "running",
        "repo": str(repo),
        "distributionRequired": distribution_requested,
        "requireReleaseHardened": args.require_release_hardened,
    }
    try:
        source = verify_source(repo)
        report["source"] = source
        if distribution_requested:
            distribution = (
                args.distribution.resolve()
                if args.distribution is not None
                else find_distribution(args.distribution_search_root.resolve())
            )
            report["distribution"] = verify_distribution(
                distribution,
                source,
                args.require_release_hardened,
            )
        report["status"] = "verified"
        if args.report is not None:
            write_report(args.report.resolve(), report)
        print(json.dumps(report, indent=2, sort_keys=True))
        return 0
    except Exception as exc:  # noqa: BLE001
        report["status"] = "failed"
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        report["traceback"] = traceback.format_exc()
        if args.report is not None:
            write_report(args.report.resolve(), report)
        print(f"LIMITED ALPHA OPERATIONS VERIFICATION FAILED: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

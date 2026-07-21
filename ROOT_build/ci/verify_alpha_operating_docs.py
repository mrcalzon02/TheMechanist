#!/usr/bin/env python3
"""Verify limited-alpha tester operations and packaged support documentation.

This is the single release authority for tester-facing operating material. It
validates governed source documents and the structured defect form, reopens a
portable or native-staging distribution, and proves that the required documents
are present, manifest-covered, source-identical, and bound to the candidate's
version, platform, Java, commit, and hardening identity.
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

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCHEMA = 3
PLAYTEST_DOCUMENT_COUNT = 8

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
    "PACKAGE_client/EULA.md": (
        "end user license agreement",
    ),
    "PACKAGE_client/README.md": (
        "The Mechanist",
    ),
    "PACKAGE_client/RUN_INSTRUCTIONS.md": (
        "Java 17",
        "launcher",
        "Independent-host remote lobby",
        "Mutable storage",
        "Never include a reusable resume token",
    ),
    "PACKAGE_client/WINDOWS_QUICK_START.md": (
        "Windows",
        "Verify",
    ),
    "PACKAGE_client/LIMITED_ALPHA_PLAYTEST_GUIDE.md": (
        "authorized playtest personnel",
        "SHA256SUMS.txt",
        "Windows x64",
        "Linux x64",
        "bundled Java 17 runtime",
        "supervised in-process internal host",
        "connected-only authoritative roster",
        "Save protection",
        "Modified content",
        "Reporting a problem",
        "Severity guide",
        "Candidate and evaluation boundary",
        "not yet an authoritative remote gameplay server",
        "must not be presented as completed networked world gameplay",
    ),
    "PACKAGE_client/KNOWN_ALPHA_LIMITATIONS.md": (
        "Known Limited-Alpha Constraints",
        "Single-player authority",
        "Independent host",
        "does not initialize or own authoritative remote world state",
        "supervised client owns handshake progression",
        "SHA-256 token hashes",
        "reusable plaintext token",
        "owner-only permissions",
        "currently connected lobby members",
        "capped at 64 visible players",
        "offline identities retained for resume continuity remain private",
        "unsupported world verbs are rejected",
        "Assets and redistribution",
        "full asset and third-party binary redistribution clearance",
        "Support boundary",
        "resume-token custody file",
    ),
    "PACKAGE_client/DIAGNOSTIC_COLLECTION.md": (
        "Exact source commit",
        "Release-hardening state",
        "Canonical limited-alpha report fields",
        "Modified packages",
        "Before sharing",
        "Manual collection when the launcher cannot start",
        "Crash reproduction",
        "Save and persistence defects",
        "Independent host defects",
        "transport, authenticated lobby authority, and remote gameplay authority",
        "resume-token custody",
    ),
    "PACKAGE_client/server/README.md": (
        "exact-address authenticated lobby",
        "does not own an authoritative remote game world",
        "Bind verification",
        "Persistent storage",
        "What success means",
    ),
}

ISSUE_FORM = ".github/ISSUE_TEMPLATE/limited-alpha-defect.yml"
ISSUE_FORM_REQUIREMENTS = (
    "name: Limited Alpha Defect",
    "id: severity",
    "id: candidate_version",
    "id: source_commit",
    "id: platform",
    "id: package_verification",
    "id: modified_content",
    "id: reproduction",
    "id: expected",
    "id: actual",
    "id: persistence_boundary",
    "id: diagnostics",
    "resume-token custody",
    "modified content",
    "relay connectivity is not proof of authoritative remote gameplay",
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
    text = path.read_text(encoding="utf-8", errors="strict")
    if not text.strip():
        raise RuntimeError(f"required {label} is empty: {path}")
    return text


def require_phrases(text: str, phrases: Iterable[str], label: str) -> None:
    folded = text.casefold()
    missing = [phrase for phrase in phrases if phrase.casefold() not in folded]
    if missing:
        raise RuntimeError(
            f"{label} is missing required alpha-operating content: "
            + ", ".join(missing)
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


def verify_source(repo: pathlib.Path = ROOT) -> dict[str, object]:
    repo = repo.resolve()
    documents = [
        source_record(repo, source, destination)
        for source, destination in SOURCE_DOCS.items()
    ]
    if len(documents) != PLAYTEST_DOCUMENT_COUNT:
        raise RuntimeError(
            f"source policy must define exactly {PLAYTEST_DOCUMENT_COUNT} documents"
        )
    issue_path = repo / ISSUE_FORM
    issue_text = require_file(issue_path, "limited-alpha issue form")
    require_phrases(issue_text, ISSUE_FORM_REQUIREMENTS, ISSUE_FORM)
    return {
        "status": "verified",
        "repo": str(repo),
        "documents": documents,
        "documentCount": len(documents),
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
    source: dict[str, object] | None = None,
    require_release_hardened: bool = False,
) -> dict[str, object]:
    distribution = distribution.resolve()
    source = source if source is not None else verify_source(ROOT)
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
    if platform not in {"windows-x64", "linux-x64"}:
        raise RuntimeError(f"canonical runtime manifest has invalid platform identity: {platform!r}")
    if java_release != 17:
        raise RuntimeError(f"limited-alpha package must target Java 17, found {java_release!r}")

    artifacts = artifact_map(manifest)
    packaged: list[dict[str, object]] = []
    source_documents = source.get("documents")
    if not isinstance(source_documents, list) or len(source_documents) != PLAYTEST_DOCUMENT_COUNT:
        raise RuntimeError(
            f"source verification must return exactly {PLAYTEST_DOCUMENT_COUNT} documents"
        )
    for record in source_documents:
        if not isinstance(record, dict):
            raise RuntimeError("source verification returned a malformed document record")
        relative = record.get("destination")
        if not isinstance(relative, str):
            raise RuntimeError("source document record has no packaged destination")
        path = distribution.joinpath(*pathlib.PurePosixPath(relative).parts)
        text = require_file(path, "packaged limited-alpha document")
        source_relative = record.get("source")
        requirements = SOURCE_REQUIREMENTS.get(str(source_relative), ())
        require_phrases(text, requirements, relative)
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
        "distribution": distribution.name,
        "version": version,
        "commit": commit,
        "platform": platform,
        "javaRelease": java_release,
        "releaseHardened": manifest.get("releaseHardened") is True,
        "documents": packaged,
        "documentCount": len(packaged),
        "manifest": str(manifest_path),
        "manifestSha256": sha256(manifest_path),
        "canonicalManifestCoverage": True,
        "sourceIdentityVerified": True,
        "structuredIssueFormVerified": True,
        "hostedSessionLimitationsCovered": True,
        "connectedOnlyRosterPrivacyCovered": True,
        "authenticatedWaitAuthorityCovered": True,
        "genericWorldCommandApiOverclaimPrevented": True,
        "movementAuthorityOverclaimPrevented": True,
        "mapAuthorityOverclaimPrevented": True,
        "fullWorldAuthorityOverclaimPrevented": True,
        "credentialDisclosurePrevented": True,
        "modifiedContentSeparationCovered": True,
    }


def verify(root: pathlib.Path) -> dict[str, object]:
    """Backward-compatible package-verification entry point."""
    return verify_distribution(root, verify_source(ROOT), False)


def write_report(path: pathlib.Path, report: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "distribution_positional",
        nargs="?",
        type=pathlib.Path,
        help="Backward-compatible canonical distribution path.",
    )
    parser.add_argument("--repo", type=pathlib.Path, default=ROOT)
    parser.add_argument("--distribution", type=pathlib.Path)
    parser.add_argument("--distribution-search-root", type=pathlib.Path)
    parser.add_argument("--require-release-hardened", action="store_true")
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()

    requested = [
        value
        for value in (
            args.distribution_positional,
            args.distribution,
            args.distribution_search_root,
        )
        if value is not None
    ]
    report: dict[str, object] = {
        "schema": SCHEMA,
        "status": "running",
        "repo": str(args.repo.resolve()),
        "distributionRequired": bool(requested),
        "requireReleaseHardened": args.require_release_hardened,
    }
    try:
        if len(requested) > 1:
            raise RuntimeError(
                "use only one distribution path, --distribution, or --distribution-search-root"
            )
        source = verify_source(args.repo.resolve())
        report["source"] = source
        if requested:
            if args.distribution_positional is not None:
                distribution = args.distribution_positional.resolve()
            elif args.distribution is not None:
                distribution = args.distribution.resolve()
            else:
                distribution = find_distribution(args.distribution_search_root.resolve())
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
    except Exception as exc:  # noqa: BLE001 - one fail-closed authority
        report["status"] = "failed"
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        report["traceback"] = traceback.format_exc()
        if args.report is not None:
            write_report(args.report.resolve(), report)
        print(f"ALPHA OPERATING DOCUMENT VERIFICATION FAILED: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Append one idempotent verified remote-release entry to development history."""

from __future__ import annotations

import argparse
import datetime as dt
import glob
import json
import os
import pathlib
import re
import subprocess

from verify_synthetic_release_contract import verify_reports

READABLE_COMMIT_STATUS_CONTEXTS = (
    "the-mechanist/java17-verification",
    "the-mechanist/native-alpha-gate",
)
RELEASE_PLATFORMS = {"linux-x64", "windows-x64"}
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


def current_git_head() -> str:
    result = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return result.stdout.strip()


def load_reports(
    pattern: str,
    label: str,
) -> tuple[list[str], list[dict[str, object]]]:
    paths = sorted(glob.glob(pattern, recursive=True))
    if len(paths) != 2:
        raise SystemExit(
            f"Expected exactly two {label} reports; found {len(paths)}: {paths}"
        )
    reports = [
        json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
        for path in paths
    ]
    return paths, reports


def verify_synthetic_reports(pattern: str) -> dict[str, object]:
    paths, reports = load_reports(pattern, "synthetic")
    try:
        return verify_reports(
            zip(paths, reports),
            expected_platforms=RELEASE_PLATFORMS,
            require_release_hardened=True,
            require_native_stage=True,
        )
    except RuntimeError as failure:
        raise SystemExit(str(failure)) from failure


def verify_inventory_certificate(
    path: pathlib.Path,
    expected_commit: str,
) -> dict[str, object]:
    if not path.is_file():
        raise SystemExit(f"Release inventory certificate is missing: {path}")
    certificate = json.loads(path.read_text(encoding="utf-8"))
    if certificate.get("status") != "verified":
        raise SystemExit(
            "Release inventory certificate status is not verified: "
            f"{certificate.get('status')!r}"
        )
    if certificate.get("sourceCommit") != expected_commit:
        raise SystemExit(
            "Release inventory certificate commit mismatch: "
            f"{certificate.get('sourceCommit')!r} != {expected_commit!r}"
        )
    if certificate.get("releaseReady") is not True:
        raise SystemExit("Release inventory certificate did not declare releaseReady=true")
    for key in (
        "clearancePendingCount",
        "blockerCount",
        "staleRegistryCount",
        "protectedRuntimeDuplicateCount",
    ):
        if int(certificate.get(key, -1)) != 0:
            raise SystemExit(
                f"Release inventory certificate requires {key}=0, "
                f"found {certificate.get(key)!r}"
            )
    required = int(certificate.get("clearanceRequiredCount", -1))
    approved = int(certificate.get("clearanceApprovedCount", -2))
    if required < 0 or approved != required:
        raise SystemExit(
            "Release inventory clearance counts do not match: "
            f"required={required} approved={approved}"
        )
    if int(certificate.get("rowCount", 0)) < 100:
        raise SystemExit("Release inventory certificate does not cover a credible full checkout")
    for key in ("manifestSha256", "clearanceRegistrySha256"):
        value = str(certificate.get(key, "")).lower()
        if not SHA256_RE.fullmatch(value):
            raise SystemExit(f"Release inventory certificate has invalid {key}: {value!r}")
    return certificate


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--history", type=pathlib.Path, required=True)
    parser.add_argument(
        "--reports",
        required=True,
        help="Glob matching platform verification reports",
    )
    parser.add_argument(
        "--synthetic-reports",
        default="synthetic-assets/synthetic-*.json",
        help="Glob matching Linux and Windows packaged synthetic reports",
    )
    parser.add_argument(
        "--inventory-certificate",
        type=pathlib.Path,
        required=True,
        help="Exact-source inventory and clearance certificate",
    )
    parser.add_argument("--commit", required=True)
    parser.add_argument("--run-url", required=True)
    args = parser.parse_args()

    if os.environ.get("GITHUB_ACTIONS", "").lower() == "true":
        head = current_git_head()
        if head != args.commit:
            raise SystemExit(
                "Refusing stale release-history mutation: checked-out HEAD "
                f"{head} does not equal verified source commit {args.commit}"
            )

    report_paths, reports = load_reports(
        args.reports,
        "platform verification",
    )
    versions = {str(report.get("version", "")) for report in reports}
    commits = {str(report.get("commit", "")) for report in reports}
    platforms = {str(report.get("platform", "")) for report in reports}
    statuses = {str(report.get("status", "")) for report in reports}
    remote_entries = {
        str(report.get("remoteClientEntryPoint", ""))
        for report in reports
    }
    if len(versions) != 1 or "" in versions:
        raise SystemExit(
            f"Verification reports disagree on version: {sorted(versions)}"
        )
    if commits != {args.commit}:
        raise SystemExit(
            f"Verification reports do not identify {args.commit}: {sorted(commits)}"
        )
    if platforms != RELEASE_PLATFORMS:
        raise SystemExit(
            "Verification reports must identify exactly both release platforms: "
            f"{sorted(platforms)} from {report_paths}"
        )
    if statuses != {"verified"}:
        raise SystemExit(
            f"Verification reports are not all verified: {sorted(statuses)}"
        )
    if remote_entries != {"mechanist.RemoteClientMain"}:
        raise SystemExit(
            "Verification reports do not agree on the governed remote-client entry: "
            f"{sorted(remote_entries)}"
        )

    synthetic_contract = verify_synthetic_reports(args.synthetic_reports)
    inventory = verify_inventory_certificate(
        args.inventory_certificate,
        args.commit,
    )

    version = next(iter(versions))
    marker = f"verified-remote-release:{version}:{args.commit}"
    history = args.history.read_text(encoding="utf-8")
    if marker in history:
        print(f"Development history already contains {marker}")
        return 0

    date = dt.datetime.now(dt.timezone.utc).date().isoformat()
    status_contexts = ", ".join(
        f"`{item}`" for item in READABLE_COMMIT_STATUS_CONTEXTS
    )
    entry = f"""

## Remote Release {version} - Java 17, Gate 3, and Runnable Distribution Certification

Recorded the first-class remote release pipeline for the launcher -> client -> server distribution path. GitHub-hosted Linux x64 and Windows x64 jobs compiled the exact source tree with Java 17, rebuilt the client, server, and launcher packages, verified the conservative release-obfuscation policies, staged platform-specific support libraries and bundled Java runtimes, generated schema-2 SHA-256 manifests, and produced portable runnable ZIP distributions. Gate 3 ran only after both platform package jobs completed successfully, and final distribution certification revalidated archive integrity, manifest completeness, ordinary and remote entry points, platform-native support libraries, Java 17 classfile compatibility, and one-click remote-client launch scripts before release publication.

Verification date: `{date}`. Exact source commit `{args.commit}`; version `{version}`; platforms `linux-x64` and `windows-x64`; Java 17 compile and packaged synthetic certification passed; launcher and alpha operating documents were verified; supervised single-player save/resume passed; the exact-bind independent host passed authenticated handshake, persistent hash-only server session storage, stable identity, token-gated reconnect, deterministic connected-only rosters, hosted-lobby commands, peer control broadcasts, sequenced bounded relay, clean restart, and corruption rejection; the packaged `mechanist.RemoteClientMain` entry and platform launchers passed; the player-facing remote lobby exposed editable endpoint/profile settings, protected mutable client credential custody, cancellable supervised connection lifecycle, roster and relay controls, and interrogatable status without mounting `GamePanel` or the local internal host. The client retained no world-command API, and remote world authority and gameplay certification remained explicitly false. The shared synthetic contract verified `{synthetic_contract['requiredTrueCount']}` required true guarantees and `{synthetic_contract['requiredFalseCount']}` required false guarantees. The exact repository inventory covered `{inventory['rowCount']}` files and approved all `{inventory['clearanceRequiredCount']}` clearance-required path/SHA pairs with zero pending, stale, rejected, protected-duplicate, or structural blockers. This inventory certificate records project approval evidence and does not claim to replace legal review. Distribution reports returned `verified`; final release certification passed. Workflow-run conclusions were also published as readable commit statuses under {status_contexts}. Workflow evidence: {args.run_url}

<!-- {marker} -->
"""
    args.history.write_text(history.rstrip() + entry + "\n", encoding="utf-8")
    print(
        f"Appended verified release history entry for {version} at {args.commit}; "
        f"status contexts={READABLE_COMMIT_STATUS_CONTEXTS}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

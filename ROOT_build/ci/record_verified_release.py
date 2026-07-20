#!/usr/bin/env python3
"""Append one idempotent verified remote-release entry to development history."""

from __future__ import annotations

import argparse
import datetime as dt
import glob
import json
import os
import pathlib
import subprocess


READABLE_COMMIT_STATUS_CONTEXTS = (
    "the-mechanist/java17-verification",
    "the-mechanist/native-alpha-gate",
)

REQUIRED_SYNTHETIC_TRUE = (
    "releaseHardened",
    "pathWithSpaces",
    "isolatedProfile",
    "readOnlyInstall",
    "alphaOperatingDocuments",
    "launcherBundledPackageVerification",
    "packagedGate3",
    "singlePlayerInternalHostLifecycle",
    "singlePlayerSaveResume",
    "independentHostTransportSession",
    "independentHostExactBind",
    "independentHostClientDrivenHandshake",
    "independentHostIntegrityChallenge",
    "independentHostServerOwnedSessionLedger",
    "independentHostStablePlayerIdentity",
    "independentHostResumeTokenContinuity",
    "independentHostDuplicateAttachmentDenied",
    "independentHostInvalidResumeTokenDenied",
    "independentHostImmutableSessionSnapshots",
    "independentHostLifetimeRelayAccounting",
    "independentHostAtomicSessionLedgerPersistence",
    "independentHostResumeTokenHashOnlyPersistence",
    "independentHostCorruptSessionLedgerRejected",
    "independentHostSessionPersistenceAcrossProcessRestart",
    "independentHostRelayOnlyAccess",
    "independentHostPreAuthenticationDataDenied",
    "independentHostBadChallengeDenied",
    "serverOperation",
    "serverHostBind",
    "nativeInstallerPayloadStage",
    "tamperedManifestRejected",
    "missingSupportRejected",
)

REQUIRED_SYNTHETIC_FALSE = (
    "launcherRemoteAcquisitionAdvertised",
    "independentHostWorldAuthority",
    "independentHostGameplaySessionCertified",
)


def current_git_head() -> str:
    result = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return result.stdout.strip()


def load_reports(pattern: str, label: str) -> tuple[list[str], list[dict[str, object]]]:
    paths = sorted(glob.glob(pattern, recursive=True))
    if len(paths) != 2:
        raise SystemExit(f"Expected exactly two {label} reports; found {len(paths)}: {paths}")
    reports = [
        json.loads(pathlib.Path(path).read_text(encoding="utf-8"))
        for path in paths
    ]
    return paths, reports


def synthetic_platform(report: dict[str, object]) -> str:
    distribution = str(report.get("distribution", "")).lower()
    if "windows-x64" in distribution:
        return "windows-x64"
    if "linux-x64" in distribution:
        return "linux-x64"
    return "unknown"


def verify_synthetic_reports(pattern: str) -> None:
    paths, reports = load_reports(pattern, "synthetic")
    statuses = {str(report.get("status", "")) for report in reports}
    if statuses != {"passed"}:
        raise SystemExit(f"Synthetic reports are not all passed: {statuses}")
    platforms = {synthetic_platform(report) for report in reports}
    if platforms != {"linux-x64", "windows-x64"}:
        raise SystemExit(
            "Synthetic reports do not identify exactly Linux and Windows x64: "
            f"{sorted(platforms)} from {paths}"
        )

    failures: list[str] = []
    for path, report in zip(paths, reports):
        for key in REQUIRED_SYNTHETIC_TRUE:
            if report.get(key) is not True:
                failures.append(f"{path}: expected {key}=true, found {report.get(key)!r}")
        for key in REQUIRED_SYNTHETIC_FALSE:
            if report.get(key) is not False:
                failures.append(f"{path}: expected {key}=false, found {report.get(key)!r}")
        if report.get("nativeInstallerPayloadStageRequired") is not True:
            failures.append(
                f"{path}: release candidate did not require native installer payload staging"
            )
    if failures:
        raise SystemExit(
            "Synthetic release contract failed:\n- " + "\n- ".join(failures)
        )


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

    report_paths, reports = load_reports(args.reports, "platform verification")
    versions = {str(report.get("version", "")) for report in reports}
    commits = {str(report.get("commit", "")) for report in reports}
    platforms = {str(report.get("platform", "")) for report in reports}
    statuses = {str(report.get("status", "")) for report in reports}
    if len(versions) != 1 or "" in versions:
        raise SystemExit(f"Verification reports disagree on version: {sorted(versions)}")
    if commits != {args.commit}:
        raise SystemExit(f"Verification reports do not identify {args.commit}: {sorted(commits)}")
    if platforms != {"linux-x64", "windows-x64"}:
        raise SystemExit(
            "Verification reports must identify exactly both release platforms: "
            f"{sorted(platforms)} from {report_paths}"
        )
    if statuses != {"verified"}:
        raise SystemExit(f"Verification reports are not all verified: {sorted(statuses)}")

    verify_synthetic_reports(args.synthetic_reports)

    version = next(iter(versions))
    marker = f"verified-remote-release:{version}:{args.commit}"
    history = args.history.read_text(encoding="utf-8")
    if marker in history:
        print(f"Development history already contains {marker}")
        return 0

    date = dt.datetime.now(dt.timezone.utc).date().isoformat()
    status_contexts = ", ".join(f"`{item}`" for item in READABLE_COMMIT_STATUS_CONTEXTS)
    entry = f"""

## Remote Release {version} - Java 17, Gate 3, and Runnable Distribution Certification

Recorded the first-class remote release pipeline for the launcher -> client -> server distribution path. GitHub-hosted Linux x64 and Windows x64 jobs compiled the exact source tree with Java 17, rebuilt the client, server, and launcher packages, staged platform-specific support libraries and bundled Java runtimes, generated schema-2 SHA-256 manifests, and produced portable runnable ZIP distributions. Gate 3 ran only after both platform package jobs completed successfully, and final distribution certification revalidated archive integrity, manifest completeness, entry points, platform-native support libraries, and Java 17 classfile compatibility before release publication.

Verification date: `{date}`. Exact source commit `{args.commit}`; version `{version}`; platforms `linux-x64` and `windows-x64`; Java 17 compile passed; packaged client and headless server operation smokes passed; downstream Gate 3 passed; Linux and Windows synthetic environment certification passed; launcher and alpha operating documents were verified; supervised single-player host save/resume passed; independent-host identity, manifest, restart, integrity challenge, exact bind, server-owned persistent session ledger, stable remote player identity, token-gated reconnect continuity, duplicate/invalid-token denial, immutable session snapshots, lifetime relay accounting, atomic hash-only storage, clean host-restart continuity, and corrupt-ledger rejection passed; remote world authority and gameplay certification remained explicitly false; distribution verification reports returned `verified`; final release certification passed. Workflow-run conclusions were also published as readable commit statuses under {status_contexts}. Workflow evidence: {args.run_url}

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

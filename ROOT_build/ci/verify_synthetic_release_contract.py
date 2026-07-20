#!/usr/bin/env python3
"""Validate packaged synthetic reports against one limited-alpha contract."""

from __future__ import annotations

import argparse
import json
import pathlib
import sys
from collections.abc import Iterable

REQUIRED_TRUE = (
    "pathWithSpaces",
    "isolatedProfile",
    "readOnlyInstall",
    "proguardPolicyVerified",
    "proguardMappingArtifactsOutsideDistribution",
    "alphaOperatingDocuments",
    "launcherBundledPackageVerification",
    "packagedGate3",
    "remoteClientEntryPoint",
    "remoteClientPortableLauncher",
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
    "independentHostHostedSessionCommands",
    "independentHostHostedSessionCommandOrdering",
    "independentHostHostedSessionRoster",
    "independentHostHostedSessionCommandAccountingPersistence",
    "independentHostStaleHostedLivenessReset",
    "independentHostHostedRosterJoinBroadcast",
    "independentHostHostedCommandPeerBroadcast",
    "independentHostHostedDisconnectRosterBroadcast",
    "independentHostHostedResumeRosterBroadcast",
    "independentHostAsynchronousControlSeparatedFromRelay",
    "independentHostCanonicalRosterClientAuthority",
    "independentHostRosterConnectedOnlyVisibility",
    "independentHostOfflineResumeIdentityPrivate",
    "independentHostRosterMalformedFrameRejected",
    "independentHostRosterMonotonicVersioning",
    "independentHostRosterVisiblePlayerLimit",
    "independentHostRosterWorldAuthorityRejected",
    "independentHostSupervisedClient",
    "independentHostClientHandshakeOwnership",
    "independentHostClientAtomicResumeTokenCustody",
    "independentHostClientResumeTokenPlaintextCustody",
    "independentHostClientTokenDiagnosticsRedacted",
    "independentHostClientHostedCommandSequencing",
    "independentHostClientRelayDispatch",
    "independentHostClientLivingHostResume",
    "independentHostClientHostRestartResume",
    "independentHostClientCorruptTokenRejected",
    "independentHostRemoteClientEntryPoint",
    "independentHostRemoteLobbyWindow",
    "independentHostRemoteLobbyEditableConnection",
    "independentHostRemoteLobbyMutableStorageOutsideInstall",
    "independentHostRemoteLobbyPendingConnectionCancellable",
    "independentHostRemoteLobbyFailedSessionTeardown",
    "independentHostTurnAuthorityCore",
    "independentHostTurnAuthoritySharedCommandContract",
    "independentHostTurnAuthoritySingleWriter",
    "independentHostTurnAuthorityWaitCommand",
    "independentHostTurnAuthorityExactOrdering",
    "independentHostTurnAuthorityAtomicPersistence",
    "independentHostTurnAuthorityRestartContinuity",
    "independentHostUnsupportedWorldCommandsRejected",
    "independentHostRelayOnlyAccess",
    "independentHostPreAuthenticationDataDenied",
    "independentHostBadChallengeDenied",
    "serverOperation",
    "serverHostBind",
    "tamperedManifestRejected",
    "missingSupportRejected",
)

REQUIRED_FALSE = (
    "launcherRemoteAcquisitionAdvertised",
    "independentHostHostedSessionLivenessPersistence",
    "independentHostClientWorldCommandApi",
    "independentHostTurnAuthorityNetworkExposed",
    "independentHostMovementAuthority",
    "independentHostMapAuthority",
    "independentHostRemoteLobbyGamePanelMounted",
    "independentHostRemoteLobbyInternalHostMounted",
    "independentHostRemoteLobbyWorldCommandApi",
    "independentHostWorldAuthority",
    "independentHostGameplaySessionCertified",
)


def platform_from_distribution(report: dict[str, object]) -> str:
    distribution = str(report.get("distribution", "")).lower()
    for platform in ("linux-x64", "windows-x64", "macos-x64", "macos-arm64"):
        if platform in distribution:
            return platform
    return "unknown"


def validate_report(
    report: dict[str, object],
    *,
    source: str,
    require_release_hardened: bool,
    require_native_stage: bool,
) -> list[str]:
    failures: list[str] = []
    if report.get("status") != "passed":
        failures.append(f"{source}: expected status='passed', found {report.get('status')!r}")
    if require_release_hardened and report.get("releaseHardened") is not True:
        failures.append(f"{source}: releaseHardened must be true")
    for key in REQUIRED_TRUE:
        if report.get(key) is not True:
            failures.append(f"{source}: expected {key}=true, found {report.get(key)!r}")
    for key in REQUIRED_FALSE:
        if report.get(key) is not False:
            failures.append(f"{source}: expected {key}=false, found {report.get(key)!r}")
    stage_required = report.get("nativeInstallerPayloadStageRequired")
    stage_verified = report.get("nativeInstallerPayloadStage")
    if require_native_stage:
        if stage_required is not True:
            failures.append(f"{source}: nativeInstallerPayloadStageRequired must be true")
        if stage_verified is not True:
            failures.append(f"{source}: nativeInstallerPayloadStage must be true")
    elif stage_required is True and stage_verified is not True:
        failures.append(
            f"{source}: required native installer staging was not verified"
        )
    return failures


def verify_reports(
    reports: Iterable[tuple[str, dict[str, object]]],
    *,
    expected_platforms: set[str] | None = None,
    require_release_hardened: bool = False,
    require_native_stage: bool = False,
) -> dict[str, object]:
    materialized = list(reports)
    if not materialized:
        raise RuntimeError("no synthetic reports were supplied")
    failures: list[str] = []
    platforms: set[str] = set()
    for source, report in materialized:
        platform = platform_from_distribution(report)
        platforms.add(platform)
        failures.extend(
            validate_report(
                report,
                source=source,
                require_release_hardened=require_release_hardened,
                require_native_stage=require_native_stage,
            )
        )
    if expected_platforms is not None and platforms != expected_platforms:
        failures.append(
            "synthetic report platforms differ from required set: "
            f"found={sorted(platforms)} required={sorted(expected_platforms)}"
        )
    if failures:
        raise RuntimeError("synthetic release contract failed:\n- " + "\n- ".join(failures))
    return {
        "status": "verified",
        "reportCount": len(materialized),
        "platforms": sorted(platforms),
        "requiredTrueCount": len(REQUIRED_TRUE),
        "requiredFalseCount": len(REQUIRED_FALSE),
        "releaseHardenedRequired": require_release_hardened,
        "nativeInstallerStageRequired": require_native_stage,
        "remoteWorldAuthority": False,
        "remoteGameplayCertified": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("reports", nargs="+", type=pathlib.Path)
    parser.add_argument(
        "--expected-platforms",
        help="Comma-separated exact platform set, such as linux-x64,windows-x64",
    )
    parser.add_argument("--require-release-hardened", action="store_true")
    parser.add_argument("--require-native-stage", action="store_true")
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()

    expected = None
    if args.expected_platforms:
        expected = {
            item.strip()
            for item in args.expected_platforms.split(",")
            if item.strip()
        }
    try:
        loaded = [
            (
                str(path),
                json.loads(path.read_text(encoding="utf-8")),
            )
            for path in args.reports
        ]
        summary = verify_reports(
            loaded,
            expected_platforms=expected,
            require_release_hardened=args.require_release_hardened,
            require_native_stage=args.require_native_stage,
        )
    except Exception as exc:  # noqa: BLE001 - one certification failure surface
        print(f"SYNTHETIC RELEASE CONTRACT FAILED: {exc}", file=sys.stderr)
        return 1

    rendered = json.dumps(summary, indent=2, sort_keys=True)
    print(rendered)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

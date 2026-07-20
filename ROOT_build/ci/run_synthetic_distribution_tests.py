#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import pathlib
import shutil
import stat
import subprocess
import sys
import tempfile


def run(cmd, cwd=None, env=None, expect=0, timeout=180):
    print("+", " ".join(str(x) for x in cmd), flush=True)
    completed = subprocess.run(
        [str(x) for x in cmd],
        cwd=cwd,
        env=env,
        timeout=timeout,
    )
    if completed.returncode != expect:
        raise RuntimeError(
            f"command returned {completed.returncode}, expected {expect}: {cmd}"
        )


def java_bin(root: pathlib.Path) -> pathlib.Path:
    return root / "runtime" / "bin" / ("java.exe" if os.name == "nt" else "java")


def classpath(root: pathlib.Path, role: str) -> str:
    jar_name = "TheMechanist.jar" if role == "client" else "TheMechanistServer.jar"
    jar = root / "packages" / role / jar_name
    separator = ";" if os.name == "nt" else ":"
    return f'{jar}{separator}{root / "packages" / "support" / "lib" / "*"}'


def profile_env(profile: pathlib.Path) -> dict[str, str]:
    env = os.environ.copy()
    env["HOME"] = str(profile)
    env["USERPROFILE"] = str(profile)
    env["APPDATA"] = str(profile / "AppData" / "Roaming")
    env["LOCALAPPDATA"] = str(profile / "AppData" / "Local")
    env["XDG_DATA_HOME"] = str(profile / ".local" / "share")
    env["XDG_CONFIG_HOME"] = str(profile / ".config")
    return env


def java_profile_args(profile: pathlib.Path) -> list[str]:
    return [f"-Duser.home={profile}"]


def make_read_only(root: pathlib.Path) -> None:
    for path in root.rglob("*"):
        try:
            mode = path.stat().st_mode
            path.chmod(mode & ~stat.S_IWUSR & ~stat.S_IWGRP & ~stat.S_IWOTH)
        except OSError:
            pass


def remove_tree(root: pathlib.Path) -> None:
    def repair_and_retry(function, path, _exc_info):
        try:
            os.chmod(path, stat.S_IWUSR | stat.S_IRUSR | stat.S_IXUSR)
            function(path)
        except OSError:
            pass

    shutil.rmtree(root, ignore_errors=False, onerror=repair_and_retry)


def verify_proguard_policy(
    root: pathlib.Path,
    distribution_verifier: pathlib.Path,
) -> bool:
    policy_verifier = distribution_verifier.parent / "verify_proguard_configuration.py"
    if not policy_verifier.is_file():
        raise RuntimeError(f"ProGuard policy verifier is missing: {policy_verifier}")
    repo = distribution_verifier.parents[2]
    report = root / "proguard-policy-verification.json"
    run(
        [
            sys.executable,
            policy_verifier,
            "--repo",
            repo,
            "--report",
            report,
        ],
        cwd=repo,
        timeout=180,
    )
    summary = json.loads(report.read_text(encoding="utf-8"))
    if summary.get("status") != "verified":
        raise RuntimeError("ProGuard source policy did not return verified")
    if summary.get("mappingArtifactsOutsideDistribution") is not True:
        raise RuntimeError("ProGuard policy did not keep mapping artifacts outside distribution")
    return True


def verify_native_stage(
    source: pathlib.Path,
    root: pathlib.Path,
    verifier: pathlib.Path,
) -> bool:
    manifest = json.loads(
        (source / "manifests" / "runtime-manifest.json").read_text(encoding="utf-8")
    )
    if manifest.get("releaseHardened") is not True:
        print(
            "SKIP native installer staging: ordinary exact-head verification "
            "distribution is not release hardened.",
            flush=True,
        )
        return False

    platform_name = str(manifest["platform"])
    stager = verifier.parent / "stage_native_installer_payload.py"
    if not stager.is_file():
        raise RuntimeError(f"native installer stager is missing: {stager}")

    staged = root / "Native Installer Payload With Spaces"
    report = root / "native-installer-staging.json"
    run(
        [
            sys.executable,
            stager,
            source,
            "--output",
            staged,
            "--expected-platform",
            platform_name,
            "--report",
            report,
        ],
        cwd=root,
        timeout=600,
    )
    if (staged / "runtime").exists():
        raise RuntimeError("native installer staging duplicated the canonical runtime")
    required = (
        staged / "launcher" / "MechanistLauncher.jar",
        staged / "packages" / "client" / "TheMechanist.jar",
        staged / "packages" / "server" / "TheMechanistServer.jar",
        staged / "manifests" / "launcher-runtime-manifest.json",
        staged / "certification" / "canonical-runtime-manifest.json",
        staged / "certification" / "installer-source-verification.json",
    )
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise RuntimeError(f"native installer staging omitted required files: {missing}")
    stage_summary = json.loads(report.read_text(encoding="utf-8"))
    if stage_summary.get("releaseHardened") is not True:
        raise RuntimeError("native installer staging did not preserve hardening identity")
    if stage_summary.get("platform") != platform_name:
        raise RuntimeError("native installer staging changed platform identity")
    return True


def verify_operating_docs(
    install: pathlib.Path,
    root: pathlib.Path,
    verifier: pathlib.Path,
) -> None:
    docs_verifier = verifier.parent / "verify_alpha_operating_docs.py"
    if not docs_verifier.is_file():
        raise RuntimeError(f"alpha operating-document verifier is missing: {docs_verifier}")
    run(
        [
            sys.executable,
            docs_verifier,
            install,
            "--report",
            root / "alpha-operating-docs.json",
        ],
        cwd=root,
        timeout=180,
    )


def run_client_smoke(
    java: pathlib.Path,
    profile_args: list[str],
    install: pathlib.Path,
    root: pathlib.Path,
    env: dict[str, str],
    main_class: str,
    timeout: int,
    extra_args: list[str] | None = None,
) -> None:
    run(
        [
            java,
            *profile_args,
            *(extra_args or []),
            "-Djava.awt.headless=true",
            "-cp",
            classpath(install, "client"),
            main_class,
        ],
        cwd=root,
        env=env,
        timeout=timeout,
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("distribution", type=pathlib.Path)
    parser.add_argument("--verifier", type=pathlib.Path, required=True)
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()

    source = args.distribution.resolve()
    if not source.is_dir():
        raise SystemExit(f"missing distribution: {source}")

    canonical = json.loads(
        (source / "manifests" / "runtime-manifest.json").read_text(encoding="utf-8")
    )
    release_hardened = canonical.get("releaseHardened") is True
    remote_entry = str(canonical.get("remoteClientEntryPoint", ""))
    if remote_entry != "mechanist.RemoteClientMain":
        raise RuntimeError(f"unexpected remote client entry point: {remote_entry!r}")

    root = pathlib.Path(tempfile.mkdtemp(prefix="Mechanist Synthetic Path With Spaces "))
    try:
        install = root / "Read Only Program Files" / source.name
        shutil.copytree(source, install)
        profile = root / "Synthetic User Profile With Spaces"
        profile.mkdir(parents=True)
        env = profile_env(profile)
        java = java_bin(install)
        profile_args = java_profile_args(profile)

        verifier = args.verifier.resolve()
        proguard_policy_verified = verify_proguard_policy(root, verifier)
        run([sys.executable, verifier, install], env=env)
        verify_operating_docs(install, root, verifier)
        native_stage_verified = verify_native_stage(source, root, verifier)

        run(
            [
                java,
                *profile_args,
                f"-Dmechanist.launcher.installRoot={install}",
                "-Djava.awt.headless=true",
                "-cp",
                install / "launcher" / "MechanistLauncher.jar",
                "mechanist.launcher.LauncherBundledPackageVerificationSmoke",
            ],
            cwd=root,
            env=env,
            timeout=180,
        )

        run_client_smoke(
            java,
            profile_args,
            install,
            root,
            env,
            "mechanist.Gate3PlayerFacingTextSmokeSuite",
            900,
        )

        internal_host_storage = profile / "Internal Host Storage With Spaces"
        run_client_smoke(
            java,
            profile_args,
            install,
            root,
            env,
            "mechanist.SinglePlayerInternalHostLifecycleSmoke",
            900,
            [f"-Dmechanist.storage.root={internal_host_storage}"],
        )

        for main_class in (
            "mechanist.RemoteClientStartupSmoke",
            "mechanist.IndependentHostTransportSessionSmoke",
            "mechanist.IndependentHostHostedSessionWireSmoke",
            "mechanist.HostedRosterClientAuthoritySmoke",
            "mechanist.IndependentHostClientSupervisorSmoke",
            "mechanist.IndependentHostPersistentSessionRestartSmoke",
        ):
            run_client_smoke(
                java,
                profile_args,
                install,
                root,
                env,
                main_class,
                300,
            )

        run(
            [
                java,
                *profile_args,
                "-cp",
                classpath(install, "server"),
                "mechanist.MechanistServerMain",
                "--help",
            ],
            cwd=root,
            env=env,
        )
        run(
            [
                java,
                *profile_args,
                "-cp",
                classpath(install, "server"),
                "mechanist.MechanistServerMain",
                "--host-once",
                "--no-steam",
                "--bind=127.0.0.1",
            ],
            cwd=root,
            env=env,
            timeout=60,
        )

        before = sorted(str(path.relative_to(profile)) for path in profile.rglob("*"))
        run(
            [
                java,
                *profile_args,
                "-cp",
                classpath(install, "server"),
                "mechanist.MechanistServerMain",
                "--help",
            ],
            cwd=root,
            env=env,
        )
        after = sorted(str(path.relative_to(profile)) for path in profile.rglob("*"))
        if not after:
            raise RuntimeError("synthetic profile remained empty after server initialization")

        make_read_only(install)
        run(
            [
                java,
                *profile_args,
                "-cp",
                classpath(install, "server"),
                "mechanist.MechanistServerMain",
                "--help",
            ],
            cwd=root,
            env=env,
        )

        tampered = root / "Tampered Distribution"
        shutil.copytree(source, tampered)
        manifest = tampered / "manifests" / "runtime-manifest.json"
        data = json.loads(manifest.read_text(encoding="utf-8"))
        declared = next(
            (
                entry
                for entry in data.get("artifacts", [])
                if entry.get("role") in {"client", "server", "launcher"}
            ),
            None,
        )
        if declared is None:
            raise RuntimeError("manifest contains no primary artifact to tamper")
        declared["sha256"] = "0" * 64
        manifest.write_text(
            json.dumps(data, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        run([sys.executable, verifier, tampered], expect=1)

        missing = root / "Missing Support Library"
        shutil.copytree(source, missing)
        support = sorted((missing / "packages" / "support" / "lib").glob("*.jar"))
        if not support:
            raise RuntimeError("no support library available for missing-library rejection test")
        support[0].unlink()
        run([sys.executable, verifier, missing], expect=1)

        remote_launcher = (
            install / "Run-Remote-Client.cmd"
            if str(canonical.get("platform", "")).startswith("windows-")
            else install / "run-remote-client.sh"
        )
        if not remote_launcher.is_file():
            raise RuntimeError(f"portable remote-client launcher is missing: {remote_launcher}")

        summary = {
            "status": "passed",
            "distribution": source.name,
            "releaseHardened": release_hardened,
            "pathWithSpaces": True,
            "isolatedProfile": True,
            "returningProfile": len(after) >= len(before),
            "readOnlyInstall": True,
            "proguardPolicyVerified": proguard_policy_verified,
            "proguardMappingArtifactsOutsideDistribution": True,
            "alphaOperatingDocuments": True,
            "launcherBundledPackageVerification": True,
            "launcherRemoteAcquisitionAdvertised": False,
            "packagedGate3": True,
            "remoteClientEntryPoint": True,
            "remoteClientPortableLauncher": True,
            "singlePlayerInternalHostLifecycle": True,
            "singlePlayerSaveResume": True,
            "independentHostTransportSession": True,
            "independentHostExactBind": True,
            "independentHostClientDrivenHandshake": True,
            "independentHostIntegrityChallenge": True,
            "independentHostServerOwnedSessionLedger": True,
            "independentHostStablePlayerIdentity": True,
            "independentHostResumeTokenContinuity": True,
            "independentHostDuplicateAttachmentDenied": True,
            "independentHostInvalidResumeTokenDenied": True,
            "independentHostImmutableSessionSnapshots": True,
            "independentHostLifetimeRelayAccounting": True,
            "independentHostAtomicSessionLedgerPersistence": True,
            "independentHostResumeTokenHashOnlyPersistence": True,
            "independentHostCorruptSessionLedgerRejected": True,
            "independentHostSessionPersistenceAcrossProcessRestart": True,
            "independentHostHostedSessionCommands": True,
            "independentHostHostedSessionCommandOrdering": True,
            "independentHostHostedSessionRoster": True,
            "independentHostHostedSessionCommandAccountingPersistence": True,
            "independentHostHostedSessionLivenessPersistence": False,
            "independentHostStaleHostedLivenessReset": True,
            "independentHostHostedRosterJoinBroadcast": True,
            "independentHostHostedCommandPeerBroadcast": True,
            "independentHostHostedDisconnectRosterBroadcast": True,
            "independentHostHostedResumeRosterBroadcast": True,
            "independentHostAsynchronousControlSeparatedFromRelay": True,
            "independentHostCanonicalRosterClientAuthority": True,
            "independentHostRosterConnectedOnlyVisibility": True,
            "independentHostOfflineResumeIdentityPrivate": True,
            "independentHostRosterMalformedFrameRejected": True,
            "independentHostRosterMonotonicVersioning": True,
            "independentHostRosterVisiblePlayerLimit": True,
            "independentHostRosterWorldAuthorityRejected": True,
            "independentHostSupervisedClient": True,
            "independentHostClientHandshakeOwnership": True,
            "independentHostClientAtomicResumeTokenCustody": True,
            "independentHostClientResumeTokenPlaintextCustody": True,
            "independentHostClientTokenDiagnosticsRedacted": True,
            "independentHostClientHostedCommandSequencing": True,
            "independentHostClientRelayDispatch": True,
            "independentHostClientLivingHostResume": True,
            "independentHostClientHostRestartResume": True,
            "independentHostClientCorruptTokenRejected": True,
            "independentHostClientWorldCommandApi": False,
            "independentHostRemoteClientEntryPoint": True,
            "independentHostRemoteLobbyWindow": True,
            "independentHostRemoteLobbyEditableConnection": True,
            "independentHostRemoteLobbyMutableStorageOutsideInstall": True,
            "independentHostRemoteLobbyPendingConnectionCancellable": True,
            "independentHostRemoteLobbyFailedSessionTeardown": True,
            "independentHostRemoteLobbyGamePanelMounted": False,
            "independentHostRemoteLobbyInternalHostMounted": False,
            "independentHostRemoteLobbyWorldCommandApi": False,
            "independentHostUnsupportedWorldCommandsRejected": True,
            "independentHostRelayOnlyAccess": True,
            "independentHostPreAuthenticationDataDenied": True,
            "independentHostBadChallengeDenied": True,
            "independentHostWorldAuthority": False,
            "independentHostGameplaySessionCertified": False,
            "serverOperation": True,
            "serverHostBind": True,
            "nativeInstallerPayloadStage": native_stage_verified,
            "nativeInstallerPayloadStageRequired": release_hardened,
            "tamperedManifestRejected": True,
            "missingSupportRejected": True,
        }
        rendered = json.dumps(summary, indent=2, sort_keys=True)
        print(rendered)
        if args.report:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(rendered + "\n", encoding="utf-8")
        return 0
    finally:
        try:
            remove_tree(root)
        except OSError:
            shutil.rmtree(root, ignore_errors=True)


if __name__ == "__main__":
    raise SystemExit(main())

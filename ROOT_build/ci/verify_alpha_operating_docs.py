#!/usr/bin/env python3
"""Verify that a staged alpha distribution includes its operating documents."""

from __future__ import annotations

import argparse
import json
import pathlib
import sys

REQUIRED_DOCS = {
    "docs/EULA.md": (
        "eula",
        "authorized evaluator",
        "resume-token custody files",
        "formal legal review",
    ),
    "docs/CLIENT_README.md": (
        "mechanist",
        "mechanist.remoteclientmain",
        "connected-only authoritative roster",
        "remote world gameplay remains unavailable",
    ),
    "docs/RUN_INSTRUCTIONS.md": (
        "run-the-mechanist",
        "run-remote-client.cmd",
        "run-remote-client.sh",
        "mechanist.remoteclientmain",
        "no remote world gameplay",
    ),
    "docs/SERVER_README.md": (
        "headless server",
        "exact requested address",
        "sha-256 resume-token hashes",
        "does not prove remote world gameplay",
    ),
    "docs/LIMITED_ALPHA_PLAYTEST_GUIDE.md": (
        "limited alpha",
        "verify the candidate",
        "single-player",
        "independent-host lobby start",
        "mechanist.remoteclientmain",
        "run-remote-client.cmd",
        "run-remote-client.sh",
        "two-tester sequence",
        "credential protection",
        "investor or backer evaluation",
        "must not be presented as completed networked world gameplay",
        "save protection",
    ),
    "docs/KNOWN_ALPHA_LIMITATIONS.md": (
        "known limited-alpha constraints",
        "remote acquisition",
        "supervised in-process internal host",
        "bounded relay transport",
        "supervised client owns handshake progression",
        "sha-256 token hashes",
        "reusable plaintext token",
        "owner-only permissions",
        "status and diagnostic text",
        "never attach or paste",
        "clean server-process restart",
        "readiness, presence, and chat-state",
        "monotonic command sequence",
        "deterministically ordered hosted-session roster",
        "currently connected lobby members",
        "capped at 64 visible players",
        "offline identities retained for resume continuity remain private",
        "authoritative hosted-roster broadcasts",
        "asynchronous `mech` control frames",
        "separate from `seq` relay payloads",
        "client exposes no movement",
        "missing, corrupted, wrong-profile, or wrong-server",
        "unsupported world verbs are rejected",
        "remote world state",
        "redistribution",
    ),
    "docs/DIAGNOSTIC_COLLECTION.md": (
        "diagnostic collection",
        "candidate version",
        "source commit",
        "modified packages",
        "independent host defects",
    ),
}


def fail(message: str) -> None:
    raise RuntimeError(message)


def verify(root: pathlib.Path) -> dict[str, object]:
    root = root.resolve()
    manifest_path = root / "manifests" / "runtime-manifest.json"
    if not manifest_path.is_file():
        fail(f"canonical runtime manifest is missing: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("remoteClientEntryPoint") != "mechanist.RemoteClientMain":
        fail("canonical manifest does not declare mechanist.RemoteClientMain")
    entries = {
        str(entry.get("path")): entry
        for entry in manifest.get("artifacts", [])
        if isinstance(entry, dict)
    }

    checked: list[str] = []
    for relative, phrases in REQUIRED_DOCS.items():
        path = root.joinpath(*pathlib.PurePosixPath(relative).parts)
        if not path.is_file():
            fail(f"required alpha operating document is missing: {relative}")
        entry = entries.get(relative)
        if entry is None:
            fail(f"alpha operating document is absent from canonical manifest: {relative}")
        if entry.get("role") != "documentation":
            fail(f"alpha operating document has wrong manifest role: {relative}")
        text = path.read_text(encoding="utf-8", errors="replace").lower()
        for phrase in phrases:
            if phrase not in text:
                fail(
                    f"alpha operating document {relative} "
                    f"is missing required phrase {phrase!r}"
                )
        checked.append(relative)

    platform = str(manifest.get("platform", ""))
    remote_launcher = (
        "Run-Remote-Client.cmd"
        if platform.startswith("windows-")
        else "run-remote-client.sh"
    )
    launcher_entry = entries.get(remote_launcher)
    if launcher_entry is None or launcher_entry.get("role") != "launch-script":
        fail(f"canonical manifest omits documented remote launcher {remote_launcher}")

    windows_quick = root / "docs" / "WINDOWS_QUICK_START.md"
    if platform.startswith("windows-") and not windows_quick.is_file():
        fail("Windows distribution is missing docs/WINDOWS_QUICK_START.md")

    return {
        "status": "verified",
        "distribution": root.name,
        "platform": manifest.get("platform"),
        "version": manifest.get("version"),
        "documents": checked,
        "documentCount": len(checked),
        "canonicalManifestCoverage": True,
        "remoteClientEntryDocumented": True,
        "remoteClientLauncherDocumented": True,
        "twoTesterProcedureCovered": True,
        "investorEvaluationBoundaryCovered": True,
        "hostedSessionLimitationsCovered": True,
        "hostedRosterBroadcastLimitationsCovered": True,
        "asynchronousControlAndRelaySeparationCovered": True,
        "supervisedClientLimitationsCovered": True,
        "clientTokenCustodyDisclosureCovered": True,
        "connectedOnlyRosterPrivacyCovered": True,
        "clientWorldCommandApiOverclaimPrevented": True,
        "remoteWorldAuthorityOverclaimPrevented": True,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("distribution", type=pathlib.Path)
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()
    try:
        summary = verify(args.distribution)
    except Exception as exc:  # noqa: BLE001 - verifier needs one clear failure
        print(f"ALPHA OPERATING DOCUMENT VERIFICATION FAILED: {exc}", file=sys.stderr)
        return 1
    rendered = json.dumps(summary, indent=2, sort_keys=True)
    print(rendered)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

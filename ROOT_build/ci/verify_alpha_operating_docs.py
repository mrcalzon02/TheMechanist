#!/usr/bin/env python3
"""Verify that a staged alpha distribution includes its operating documents."""

from __future__ import annotations

import argparse
import json
import pathlib
import sys

REQUIRED_DOCS = {
    "docs/EULA.md": ("eula",),
    "docs/CLIENT_README.md": ("mechanist",),
    "docs/RUN_INSTRUCTIONS.md": ("run",),
    "docs/SERVER_README.md": ("server",),
    "docs/LIMITED_ALPHA_PLAYTEST_GUIDE.md": (
        "limited alpha",
        "verify the candidate",
        "single-player",
        "independent host",
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
                fail(f"alpha operating document {relative} is missing required phrase {phrase!r}")
        checked.append(relative)

    windows_quick = root / "docs" / "WINDOWS_QUICK_START.md"
    if str(manifest.get("platform", "")).startswith("windows-") and not windows_quick.is_file():
        fail("Windows distribution is missing docs/WINDOWS_QUICK_START.md")

    return {
        "status": "verified",
        "distribution": root.name,
        "platform": manifest.get("platform"),
        "version": manifest.get("version"),
        "documents": checked,
        "documentCount": len(checked),
        "canonicalManifestCoverage": True,
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

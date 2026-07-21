#!/usr/bin/env python3
"""Finalize installer lifecycle evidence after package-manager uninstall."""

from __future__ import annotations

import argparse
import json
import pathlib
import sys
import traceback

SCHEMA = 1


def load(path: pathlib.Path) -> dict[str, object]:
    if not path.is_file():
        raise RuntimeError(f"installed deployment report is missing: {path}")
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise RuntimeError("installed deployment report root is not an object")
    return value


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--deployment-report", type=pathlib.Path, required=True)
    parser.add_argument("--installed-image", type=pathlib.Path, required=True)
    parser.add_argument("--survival-marker", type=pathlib.Path, required=True)
    parser.add_argument("--report", type=pathlib.Path, required=True)
    args = parser.parse_args()

    output = args.report.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    result: dict[str, object] = {"schema": SCHEMA, "status": "failed"}
    try:
        deployment = load(args.deployment_report.resolve())
        for key, expected in (
            ("status", "passed"),
            ("releaseHardened", True),
            ("installedImageVerified", True),
            ("nativeLauncherRunning", True),
            ("gameProcessRunning", True),
            ("launcherBundledPackageVerification", True),
            ("singlePlayerLifecycle", True),
            ("installTreeImmutable", True),
            ("mutableStorageOutsideInstall", True),
        ):
            if deployment.get(key) != expected:
                raise RuntimeError(
                    f"installed deployment report requires {key}={expected!r}, "
                    f"found {deployment.get(key)!r}"
                )

        image = args.installed_image.resolve()
        marker = args.survival_marker.resolve()
        if image.exists():
            remaining_executables = [
                path for path in image.rglob("*")
                if path.is_file()
                and path.name.lower() in {
                    "the mechanist",
                    "the mechanist.exe",
                    "the mechanist remote lobby",
                    "the mechanist remote lobby.exe",
                }
            ]
            if remaining_executables:
                raise RuntimeError(
                    "installer uninstall left native executables behind: "
                    f"{remaining_executables}"
                )
        if not marker.is_file():
            raise RuntimeError(
                f"mutable user-data survival marker was removed by uninstall: {marker}"
            )
        try:
            marker.relative_to(image)
        except ValueError:
            pass
        else:
            raise RuntimeError("survival marker was placed inside the install tree")

        result.update({
            "status": "passed",
            "version": deployment.get("version"),
            "platform": deployment.get("platform"),
            "commit": deployment.get("commit"),
            "releaseHardened": True,
            "installerInstalled": True,
            "installedImageVerified": True,
            "nativeLauncherRunning": True,
            "gameProcessRunning": True,
            "launcherBundledPackageVerification": True,
            "singlePlayerLifecycle": True,
            "installTreeImmutable": True,
            "installerUninstalled": True,
            "installedExecutablesRemoved": True,
            "mutableDataSurvivedUninstall": True,
            "installedImage": str(image),
            "survivalMarker": str(marker),
            "deploymentReport": str(args.deployment_report.resolve()),
        })
        output.write_text(
            json.dumps(result, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"INSTALLER LIFECYCLE VERIFIED: {output}")
        return 0
    except Exception as exc:  # noqa: BLE001 - one lifecycle failure surface
        result["errorType"] = type(exc).__name__
        result["error"] = str(exc)
        result["traceback"] = traceback.format_exc()
        output.write_text(
            json.dumps(result, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"INSTALLER LIFECYCLE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {output}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

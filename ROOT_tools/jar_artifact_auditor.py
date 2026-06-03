#!/usr/bin/env python3
from __future__ import annotations

import csv
import hashlib
import json
import zipfile
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
CLIENT_JAR = REPO_ROOT / "PACKAGE_client" / "TheMechanist.jar"
SERVER_JAR = REPO_ROOT / "PACKAGE_server" / "TheMechanistServer.jar"
OUT_JSON = REPO_ROOT / "docs" / "jar_artifact_audit.json"
OUT_TSV = REPO_ROOT / "docs" / "jar_artifact_audit.tsv"

CLIENT_ONLY_MARKERS = (
    "GamePanel.class",
    "ImageCache.class",
    "SoundManager.class",
    "DynamicMusicManager.class",
    "TileArtSystem.class",
    "java/awt/",
    "javax/swing/",
)
SERVER_ENTRY_MARKERS = (
    "mechanist/TheMechanistServer.class",
)
CLIENT_ENTRY_MARKERS = (
    "mechanist/TheMechanist.class",
)


@dataclass
class JarAudit:
    name: str
    path: str
    exists: bool
    size_bytes: int
    sha256: str
    manifest_main_class: str
    class_count: int
    resource_count: int
    contains_client_entrypoint: bool
    contains_server_entrypoint: bool
    contains_gamepanel: bool
    contains_swing_or_awt_named_classes: bool
    contains_assets: bool
    first_classes: str
    warnings: str


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def read_manifest_main(zf: zipfile.ZipFile) -> str:
    try:
        raw = zf.read("META-INF/MANIFEST.MF").decode("utf-8", errors="replace")
    except KeyError:
        return ""
    current_key = None
    values: dict[str, str] = {}
    for line in raw.splitlines():
        if line.startswith(" ") and current_key:
            values[current_key] += line[1:]
            continue
        if ":" in line:
            key, value = line.split(":", 1)
            current_key = key.strip()
            values[current_key] = value.strip()
    return values.get("Main-Class", "")


def audit_jar(name: str, path: Path, expect: str) -> JarAudit:
    if not path.exists():
        return JarAudit(name, rel(path), False, 0, "", "", 0, 0, False, False, False, False, False, "", "missing_jar")
    warnings: list[str] = []
    with zipfile.ZipFile(path, "r") as zf:
        names = sorted(zf.namelist())
        classes = [n for n in names if n.endswith(".class")]
        resources = [n for n in names if not n.endswith(".class") and not n.endswith("/")]
        main_class = read_manifest_main(zf)
        contains_client_entry = any(marker in names for marker in CLIENT_ENTRY_MARKERS)
        contains_server_entry = any(marker in names for marker in SERVER_ENTRY_MARKERS)
        contains_gamepanel = any(n.endswith("GamePanel.class") or "/GamePanel$" in n for n in classes)
        contains_swing_named = any(("Swing" in n or "JFrame" in n or "JPanel" in n or "javax/swing" in n or "java/awt" in n) for n in names)
        contains_assets = any(n.startswith("assets/") or "/assets/" in n for n in names)
        if expect == "client" and not contains_client_entry:
            warnings.append("client_entrypoint_missing")
        if expect == "server" and not contains_server_entry:
            warnings.append("server_entrypoint_missing")
        if expect == "server" and contains_gamepanel:
            warnings.append("server_contains_gamepanel")
        if expect == "server" and contains_assets:
            warnings.append("server_contains_assets")
        if expect == "server" and contains_client_entry:
            warnings.append("server_contains_client_entrypoint")
        if expect == "client" and contains_server_entry:
            warnings.append("client_contains_server_entrypoint")
        return JarAudit(
            name=name,
            path=rel(path),
            exists=True,
            size_bytes=path.stat().st_size,
            sha256=sha256(path),
            manifest_main_class=main_class,
            class_count=len(classes),
            resource_count=len(resources),
            contains_client_entrypoint=contains_client_entry,
            contains_server_entrypoint=contains_server_entry,
            contains_gamepanel=contains_gamepanel,
            contains_swing_or_awt_named_classes=contains_swing_named,
            contains_assets=contains_assets,
            first_classes=";".join(classes[:40]),
            warnings=";".join(warnings),
        )


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def write_reports(audits: list[JarAudit], global_warnings: list[str]) -> None:
    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(json.dumps({
        "schema": "mechanist.jar_artifact_audit.v1",
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "global_warnings": global_warnings,
        "jars": [asdict(a) for a in audits],
    }, indent=2), encoding="utf-8")
    with OUT_TSV.open("w", encoding="utf-8", newline="") as handle:
        fieldnames = list(asdict(audits[0]).keys()) if audits else []
        writer = csv.DictWriter(handle, fieldnames=fieldnames, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for audit in audits:
            writer.writerow(asdict(audit))


def main() -> int:
    audits = [audit_jar("client", CLIENT_JAR, "client"), audit_jar("server", SERVER_JAR, "server")]
    warnings: list[str] = []
    client, server = audits
    if client.exists and server.exists:
        if client.sha256 == server.sha256:
            warnings.append("client_and_server_jars_are_hash_identical")
        if client.size_bytes == server.size_bytes:
            warnings.append("client_and_server_jars_have_identical_size")
    write_reports(audits, warnings)
    print(f"Wrote {rel(OUT_JSON)}")
    print(f"Wrote {rel(OUT_TSV)}")
    for audit in audits:
        print(f"{audit.name}: exists={audit.exists} size={audit.size_bytes} main={audit.manifest_main_class} classes={audit.class_count} warnings={audit.warnings}")
    if warnings:
        print("GLOBAL WARNINGS: " + ";".join(warnings))
    return 2 if warnings or any(a.warnings for a in audits if a.exists) else 0


if __name__ == "__main__":
    raise SystemExit(main())

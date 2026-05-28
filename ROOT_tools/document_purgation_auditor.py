#!/usr/bin/env python3
"""Audit documentation for purgation, relocation, and classification problems.

Produces:
  docs/document_purgation_audit.json
  docs/document_purgation_audit.tsv

This tool does not delete files. It classifies likely action items so root docs
can remain focused on engineering/project work while player-facing lore moves
into PACKAGE_client/assets/infopedia.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
DEFAULT_JSON = REPO_ROOT / "docs/document_purgation_audit.json"
DEFAULT_TSV = REPO_ROOT / "docs/document_purgation_audit.tsv"
DOC_EXTENSIONS = {".md", ".txt", ".rst", ".json", ".tsv", ".csv"}
GENERATED_DOCS = {
    "docs/repository_file_manifest.tsv",
    "docs/repository_manifest_audit_report.md",
    "docs/repository_manifest_audit_issues.tsv",
    "docs/document_index.json",
    "docs/document_index.tsv",
    "docs/document_purgation_audit.json",
    "docs/document_purgation_audit.tsv",
}

LORE_KEYWORDS = [
    "historicus",
    "infopedia",
    "in-universe",
    "world lore",
    "faction lore",
    "planetary",
    "imperial",
    "adeptus",
    "mechanicus",
    "arbites",
]

PROCESS_KEYWORDS = [
    "command", "build", "release", "packaging", "audit", "standard", "practice", "manifest", "milestone", "workflow", "tool"
]


@dataclass
class PurgationIssue:
    action: str
    severity: str
    path: str
    category: str
    reason: str
    recommendation: str


def rel(path: Path) -> str:
    return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()


def safe_read(path: Path, limit: int = 100_000) -> str:
    try:
        return path.read_text(encoding="utf-8-sig", errors="replace")[:limit]
    except Exception:
        return ""


def iter_docs() -> Iterable[Path]:
    for path in sorted(REPO_ROOT.rglob("*")):
        if not path.is_file():
            continue
        if ".git" in path.parts:
            continue
        if any(part in {"ROOT_SRC_assets", "build", "dist"} for part in path.parts):
            continue
        if path.suffix.lower() in DOC_EXTENSIONS:
            yield path


def classify_path(path_text: str, content: str) -> tuple[str, str, str, str]:
    lower_path = path_text.lower()
    lower_content = content.lower()
    in_docs = lower_path.startswith("docs/")
    in_infopedia = lower_path.startswith("package_client/assets/infopedia/")
    is_generated = path_text in GENERATED_DOCS
    lore_hits = [kw for kw in LORE_KEYWORDS if kw in lower_path or kw in lower_content]
    process_hits = [kw for kw in PROCESS_KEYWORDS if kw in lower_path or kw in lower_content[:5000]]

    if is_generated:
        return "regenerate", "info", "generated_doc", "Generated documentation; do not hand-edit. Regenerate from tooling when stale."

    if "historicus" in lower_path and not in_infopedia:
        return "move_to_infopedia", "warning", "historicus_wrong_location", "Historicus is world lore and belongs under PACKAGE_client/assets/infopedia/historicus/."

    if in_docs and "historicus" in lower_content and "engineering note" not in lower_content:
        return "move_to_infopedia", "warning", "historicus_content_in_root_docs", "Historicus content appears in root docs. Move actual lore to client infopedia."

    if in_docs and lore_hits and not process_hits:
        return "move_to_infopedia", "review", "probable_lore_in_root_docs", "Document appears lore-heavy and process-light. Move to client infopedia if player-facing."

    if in_infopedia:
        return "keep", "info", "client_infopedia", "Client-facing lore/infopedia content is in the correct tree."

    if "temporary" in lower_content or "coming soon" in lower_content or "placeholder" in lower_content:
        return "review", "warning", "placeholder_or_temporary_language", "Review for stale placeholder/temporary language. Remove from player-facing docs and purge if obsolete."

    if in_docs:
        return "keep", "info", "root_doc", "Root documentation appears to be in an acceptable project/process location."

    if path_text.lower().endswith((".md", ".txt", ".rst")):
        return "review", "info", "doc_outside_docs", "Documentation-like file exists outside docs or infopedia; confirm location is intentional."

    return "keep", "info", "not_actionable", "No purgation action suggested."


def build_issues() -> list[PurgationIssue]:
    issues: list[PurgationIssue] = []
    for path in iter_docs():
        path_text = rel(path)
        content = safe_read(path)
        action, severity, category, reason = classify_path(path_text, content)
        if action == "keep" and category == "not_actionable":
            continue
        recommendation = {
            "move_to_infopedia": "Move actual lore/player-facing content to PACKAGE_client/assets/infopedia/, then replace root doc with a short engineering note only if needed.",
            "regenerate": "Regenerate with the appropriate ROOT_tools command instead of hand editing.",
            "review": "Inspect manually and either keep, merge, move, archive, or purge.",
            "keep": "No action required unless later review changes classification.",
        }.get(action, "Review manually.")
        issues.append(PurgationIssue(action, severity, path_text, category, reason, recommendation))
    issues.sort(key=lambda x: (x.action, x.severity, x.path))
    return issues


def write_tsv(issues: list[PurgationIssue], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["action", "severity", "path", "category", "reason", "recommendation"], delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for issue in issues:
            writer.writerow(asdict(issue))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit docs for purgation/relocation candidates.")
    parser.add_argument("--output-json", default=str(DEFAULT_JSON))
    parser.add_argument("--output-tsv", default=str(DEFAULT_TSV))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    issues = build_issues()
    payload = {
        "schema": "mechanist.document_purgation_audit.v1",
        "issue_count": len(issues),
        "issues": [asdict(issue) for issue in issues],
    }
    output_json = Path(args.output_json).resolve()
    output_tsv = Path(args.output_tsv).resolve()
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    write_tsv(issues, output_tsv)
    print(f"Wrote {len(issues)} purgation audit row(s)")
    print(f"Wrote {output_json}")
    print(f"Wrote {output_tsv}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

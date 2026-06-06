# Documentation, Tooling, and Repository Storage Standards

This document defines where durable project information belongs, where durable tools belong, where build orchestration belongs, and how stale documentation, generated diagnostics, and scratch artifacts are purged or relocated.

## Canonical repository roots

The repository uses explicit root prefixes so project material is not scattered across ad-hoc top-level folders.

```text
ROOT_docs/          Durable development documentation, governance, milestones, ledgers, generated review maps, and archives.
ROOT_tools/         Durable development tools, scanners, indexers, auditors, packagers, map builders, and maintenance scripts.
ROOT_build/         Checked-in build orchestration, build definitions, build docs, and durable build helper entry points.
ROOT_SRC_assets/    Protected source art/material. Do not modify source assets in place.
PACKAGE_client/     Runtime client package payload and client-ready assets.
PACKAGE_launcher/   Launcher package source, launcher resources, and launcher runtime assets.
PACKAGE_installer/  Installer/package pipeline source and installer documentation.
src/                Java source modules for the client/runtime codebase.
```

Avoid creating new top-level storage roots. A new root is allowed only when the user explicitly approves it and the governance documents are updated in the same pass.

## Durable development documents

Durable project documentation belongs under:

```text
ROOT_docs/
```

`ROOT_docs/` is for project operations, engineering context, release process, build instructions, audit reports, standards, command references, milestone tracking, generated ledgers, development history, and archives that help contributors work on the repository.

Do not create or revive a root-level `docs/` directory for active project documentation. Historical references to `docs/` mean `ROOT_docs/` unless they explicitly refer to an old archived path.

`ROOT_docs/` is not a lore dump, gameplay encyclopedia, in-universe codex, player-facing manual, asset staging area, scratchpad folder, or build-output folder.

## Durable tools

Durable project tooling belongs under:

```text
ROOT_tools/
```

Examples include repository scanners, document indexers, purgation auditors, package seed builders, Java classfile scanners, manifest generators, Mermaid/function-map builders, and command-line maintenance helpers.

Do not add new durable tooling under root-level `scripts/`, `tools/`, `bin/`, or ad-hoc folders. Existing legacy tooling outside `ROOT_tools/` should be moved or wrapped into `ROOT_tools/` when touched. If a legacy script must remain temporarily for compatibility, add a `ROOT_tools/` wrapper and update current documentation to prefer the wrapper.

## Build orchestration

Checked-in build orchestration and durable build helper entry points belong under:

```text
ROOT_build/
```

Generated build products should not be checked in unless they are explicit package payloads or review artifacts approved by the current release workflow. Runtime package payloads belong under the appropriate `PACKAGE_*` tree, not under `ROOT_docs/` or `ROOT_tools/`.

## Client-facing lore and Infopedia content

In-universe world lore, Historicus material, faction lore, place lore, item lore, character lore, and player-facing codex/encyclopedia entries belong in the client package under:

```text
PACKAGE_client/assets/infopedia/
```

Historicus material specifically belongs under:

```text
PACKAGE_client/assets/infopedia/historicus/
```

Root docs may contain a short engineering note explaining how infopedia files are formatted, indexed, localized, and packaged, but the actual lore text belongs in the client-facing infopedia tree.

## Documentation classes

Every document should fit one of these classes:

- `project_standard`: standards, practices, rules, and policies.
- `command_reference`: command maps and tool usage references.
- `development_history`: chronological engineering history or resumption briefings.
- `release_process`: packaging, installer, launcher, server/client release workflow.
- `audit_report`: generated or manually curated audit output.
- `milestone_tracker`: roadmap, current status, review markers.
- `generated_ledger`: regenerated map, manifest, index, scan output, or evaluation table.
- `client_infopedia`: player-facing in-universe or explanatory content packaged with the client.
- `archive_candidate`: old, superseded, temporary, duplicated, stale, or no-longer-actionable content.
- `purge_candidate`: obsolete generated output, broken scratch files, misleading stale previews, or documents that actively create confusion.

## Purgation standard

Documentation purgation does not mean blindly deleting useful context. It means removing or relocating material that is no longer serving the correct audience.

A document, tool, diagnostic, or generated artifact should be purged, archived, moved, or regenerated when any of the following are true:

- It is a stale generated report that can be regenerated.
- It describes an old workflow that has been replaced.
- It duplicates a current canonical document without adding useful history.
- It is a temporary scratchpad, preview, conversation note, or export artifact.
- It is in-universe/player-facing lore stored in `ROOT_docs/` or repository root instead of client Infopedia.
- It contains placeholder, temporary, or coming-soon language that would be harmful if treated as player-facing.
- It contradicts the current build/package workflow.
- It is a one-off conversation resumption note that has already been superseded by a later resumption or development history entry.
- It is a durable tool located outside `ROOT_tools/` and has no compatibility reason to stay there.
- It is a build helper located outside `ROOT_build/`, `ROOT_tools/`, or the owning `PACKAGE_*` tree.

## Action categories

Use these actions in audits and reviews:

- `keep`: material is useful and in the correct location.
- `merge`: material contains useful content that should be merged into a canonical document.
- `move_to_root_docs`: active development documentation should move to `ROOT_docs/`.
- `move_to_root_tools`: durable tools should move to `ROOT_tools/`.
- `move_to_root_build`: durable build orchestration should move to `ROOT_build/`.
- `move_to_infopedia`: lore/player-facing content should move to `PACKAGE_client/assets/infopedia/`.
- `move_to_archive`: historically useful but no longer active; archive under the nearest appropriate `archive/` directory.
- `regenerate`: generated report should be rebuilt from tools instead of hand-edited.
- `purge`: remove from active repository once confirmed unnecessary.
- `review`: human inspection required before action.

## Historicus rule

Historicus is world lore. It is not root project documentation.

If a file in `ROOT_docs/` or repository root contains Historicus material, it should be treated as `move_to_infopedia` unless it is only an engineering note about the Historicus Infopedia system.

Correct location:

```text
PACKAGE_client/assets/infopedia/historicus/
```

Incorrect locations:

```text
docs/Historicus*.md
ROOT_docs/Historicus*.md
ROOT_docs/*historicus*.md
Historicus*.md
```

## Generated documentation rules

Generated documentation is allowed when it is useful for review, but it must be clearly named, regenerable from tools, and stored under `ROOT_docs/` unless it is a package-owned runtime artifact.

Examples:

```text
ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv
ROOT_docs/repository_manifest_audit_report.md
ROOT_docs/repository_manifest_audit_issues.tsv
ROOT_docs/document_index.json
ROOT_docs/document_index.tsv
ROOT_docs/document_purgation_audit.json
ROOT_docs/document_purgation_audit.tsv
ROOT_docs/functionmap/generated/CODE_MERMAID_POSITION_LEDGER.tsv
ROOT_docs/functionmap/generated/CODE_MERMAID_EVALUATION.tsv
ROOT_docs/functionmap/generated/MERMAID_CODE_MAP.md
```

Generated documents should not be manually edited unless the tool explicitly expects manual curation. If the connector cannot regenerate a checksum or filesystem ledger, leave it untouched and record the required local command instead of fabricating values.

## Dashboard and tooling integration

The dashboard should surface root docs, milestones, development history, and purge candidates separately from client Infopedia material.

The documentation indexer and purgation auditor should be run after major documentation moves:

```powershell
python .\ROOT_tools\document_indexer.py
python .\ROOT_tools\document_purgation_auditor.py
python .\ROOT_tools\repository_scan_indexer.py
```

Repository inventory and code-map regeneration should be run after file moves, tool moves, or Java module changes:

```powershell
ROOT_tools\update-repository-file-manifest.ps1
py -3 ROOT_tools\functionmap\BUILD_MERMAID_CODE_MAP.py --apply
```

If a compatibility wrapper is still needed outside the canonical root, current documentation must point to the canonical `ROOT_tools/` or `ROOT_build/` entry point and describe the old path as legacy.

## Review before deletion

Deletion should be deliberate. Before purging a document, tool, diagnostic, or generated artifact, confirm one of these is true:

- The content is generated and can be regenerated.
- The content has been merged into a canonical document.
- The content has been relocated to the correct package/client/tool/docs/build path.
- The content is actively misleading or obsolete.
- The content is a duplicate with no unique history or information.
- The file is empty or broken and a durable replacement exists.

When in doubt, mark it `review`, not `purge`.

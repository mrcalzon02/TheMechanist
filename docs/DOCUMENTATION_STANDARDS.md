# Documentation Standards and Purgation Rules

This document defines what belongs in root project documentation, what belongs in client-facing data, and how stale documentation is purged or relocated.

## Root `docs/` purpose

The root `docs/` directory is for project operations, engineering context, release process, build instructions, audit reports, standards, command references, milestone tracking, and development history that helps contributors work on the repository.

Root `docs/` is not a lore dump, gameplay encyclopedia, in-universe codex, player-facing manual, or asset staging area.

## Client-facing lore and infopedia content

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
- `client_infopedia`: player-facing in-universe or explanatory content packaged with the client.
- `archive_candidate`: old, superseded, temporary, duplicated, stale, or no-longer-actionable content.
- `purge_candidate`: obsolete generated output, broken scratch files, misleading stale previews, or documents that actively create confusion.

## Purgation standard

Documentation purgation does not mean blindly deleting useful context. It means removing or relocating material that is no longer serving the correct audience.

A document should be purged, archived, or moved when any of the following are true:

- It is a stale generated report that can be regenerated.
- It describes an old workflow that has been replaced.
- It duplicates a current canonical document without adding useful history.
- It is a temporary scratchpad, preview, or export artifact.
- It is in-universe/player-facing lore stored in root `docs/` instead of client infopedia.
- It contains placeholder, temporary, or coming-soon language that would be harmful if treated as player-facing.
- It contradicts the current build/package workflow.
- It is a one-off conversation resumption note that has already been superseded by a later resumption or development history entry.

## Action categories

Use these actions in audits and reviews:

- `keep`: document is useful and in the correct location.
- `merge`: document contains useful material that should be merged into a canonical document.
- `move_to_infopedia`: lore/player-facing content should move to `PACKAGE_client/assets/infopedia/`.
- `move_to_archive`: historically useful but no longer active; archive if an archive directory exists.
- `regenerate`: generated report should be rebuilt from tools instead of hand-edited.
- `purge`: remove from active repository once confirmed unnecessary.
- `review`: human inspection required before action.

## Historicus rule

Historicus is world lore. It is not root project documentation.

If a file in `docs/` or repository root contains Historicus material, it should be treated as `move_to_infopedia` unless it is only an engineering note about the Historicus infopedia system.

Correct location:

```text
PACKAGE_client/assets/infopedia/historicus/
```

Incorrect locations:

```text
docs/Historicus*.md
docs/*historicus*.md
Historicus*.md
```

## Generated documentation rules

Generated documentation is allowed when it is useful for review, but it must be clearly named and regenerable from tools.

Examples:

```text
docs/repository_file_manifest.tsv
docs/repository_manifest_audit_report.md
docs/repository_manifest_audit_issues.tsv
docs/document_index.json
docs/document_index.tsv
docs/document_purgation_audit.json
docs/document_purgation_audit.tsv
```

Generated documents should not be manually edited unless the tool explicitly expects manual curation.

## Dashboard and tooling integration

The dashboard should surface root docs, milestones, development history, and purge candidates separately from client infopedia material.

The documentation indexer and purgation auditor should be run after major documentation moves:

```powershell
python .\ROOT_tools\document_indexer.py
python .\ROOT_tools\document_purgation_auditor.py
python .\ROOT_tools\repository_scan_indexer.py
```

## Review before deletion

Deletion should be deliberate. Before purging a document, confirm one of these is true:

- The content is generated and can be regenerated.
- The content has been merged into a canonical document.
- The content has been relocated to the correct package/client path.
- The content is actively misleading or obsolete.
- The content is a duplicate with no unique history or information.

When in doubt, mark it `review`, not `purge`.

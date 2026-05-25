# The Mechanist — Public-Safe Rebase Audit

## Purpose

This document begins the IP-neutralization and documentation-containment purge. It exists as a controlled audit ledger for the public-safe rebase and must not become a general changelog, roadmap, lore bible, or replacement master plan.

The immediate goal is to make The Mechanist safe for public development and public artifact iteration by removing or replacing public-facing third-party/protected names, faction identities, lore terms, logos, visual silhouettes, asset names, UI strings, diagnostics, release text, and documentation references.

This is not legal advice. The project standard remains: a disclaimer is useful notice text, not permission to ship someone else's protected identity.

## Governing constraints read before this pass

- `docs/briefings/NEW_CONVERSATION_BRIEFING_IP_NEUTRALIZATION_REBASE.md` directs this pass toward audit first, replacement map second, bounded rewrites third.
- `docs/STANDARDS_AND_PRACTICES.md` requires strict documentation containment and says ordinary pass notes, audit notes, architecture notes, and temporary planning documents must not accumulate in `docs/` unless explicitly ordered.
- `docs/MASTER_DEVELOPMENT_PLAN.md` states that planning belongs in the master plan, completed work belongs in development history, durable rules belong in standards, and doctrine belongs in governance.
- `docs/MASTER_GOVERNANCE_REVISION_II.md` repeats that standing notes, audit files, architecture addenda, and pass summaries must be folded into the correct durable document or deleted.

## Documentation purge alignment rule for this rebase

The IP-neutralization rebase temporarily permits this controlled audit ledger only because the user explicitly ordered the purge/audit pass and the rebase briefing calls for a tracked audit surface.

This exception is narrow:

1. Do not create a sprawling family of new Markdown files for every subtopic.
2. Do not create separate lore bibles, replacement essays, faction encyclopedias, or pass diaries before the audit map exists.
3. Fold durable rules into `STANDARDS_AND_PRACTICES.md`.
4. Fold roadmap changes into `MASTER_DEVELOPMENT_PLAN.md`.
5. Fold completed actions into `DEVELOPMENT_HISTORY.md`.
6. Fold doctrine into `MASTER_GOVERNANCE_REVISION_II.md`.
7. When the rebase is complete, either delete this audit ledger or condense its durable remnants into the four durable docs plus the existing staged asset integration exception.

## Current docs-folder risk finding

Known from the briefing and durable docs review:

- The durable standard expects `docs/` to remain tightly contained.
- The IP-neutralization briefing currently lives under `docs/briefings/`, which is useful as a handoff artifact but conflicts with the long-term anti-sprawl standard if it remains permanent.
- The briefing requested several `docs/ip_neutralization/` files. For the purge pass, those should be consolidated into this single audit ledger until an actual inventory proves more files are necessary.

First purge decision: do not add the full four-file IP-neutralization document set yet. Use this one controlled audit ledger while the docs-folder inventory and asset-risk map are assembled.

## Public-safe rebase lanes

### Lane A — Audit

Find direct and indirect public-risk material in:

- `src/`
- `launcher/`
- `assets/`
- `config/`
- `docs/`
- `tools/`
- `.github/`
- README and root legal files
- installer/launcher text
- server admin GUI text
- runtime diagnostics and log strings
- release/package names
- asset filenames and folder names
- music/image manifests
- faction, unit, item, weapon, status, room, zone, and infopedia text

### Lane B — Replacement map

Build neutral original-IP replacements for:

- factions and subfactions
- ranks and institutional bodies
- military/security bodies
- religious/ritual/maintenance groups
- enemy archetypes
- weapons, armor, vehicles, statuses, and currencies
- zone/facility/room families
- public docs, package names, launcher text, and release text

### Lane C — Controlled rewrite

Apply replacements in small passes:

1. public docs / README / package text
2. launcher and installer text
3. faction/unit/item visible strings
4. infopedia/codex/menu text
5. filenames/manifests/assets references
6. EULA/disclaimer update
7. visual-asset quarantine or replacement

Build and grep after each rewrite pass.

## Risk categories

| Category | Meaning | Required action |
|---|---|---|
| GREEN | Generic/original enough | Leave or polish |
| YELLOW | Genre-adjacent or suspicious | Rename before public release if visible |
| RED | Obvious protected term, faction, place, lore phrase, character, logo, or derivative identity | Replace before public repo/release |
| LICENSED-ONLY | Belongs only in private notes, licensed pitch material, or removed prototype history | Remove from public-facing build and docs |

## Immediate documentation purge tasks

- Inventory every Markdown file under `docs/` and classify it as durable, explicit exception, controlled rebase ledger, fold into durable doc, archive, or delete.
- Fold the IP-neutralization briefing's durable instructions into the master plan, standards, governance, and development history once the first audit pass is complete.
- Remove or quarantine stale pass notes that duplicate the master plan or development history.
- Keep README user-facing and free of internal legal panic, roadmap sprawl, or private prototype lineage.
- Keep root legal/EULA text public-facing and sober: original project, no affiliation, third-party marks belong to owners, public builds include only owned/cleared content, and user mods/assets must not infringe.

## Immediate asset assessment tasks

- Inventory semantic asset registry rows for names/descriptions that contain protected or suspicious terminology.
- Inventory asset folder names and filenames for protected or suspicious terminology.
- Classify visual assets by silhouette risk, icon/logo risk, faction-color risk, armor/weapon resemblance risk, and UI motif risk.
- Mark risky assets as: rename metadata only, redraw/silhouette-diverge, quarantine, or delete.
- Treat high-resolution optional art packs as separate exposure surfaces; do not assume the core low tier is the only public-risk layer.

## First known high-risk vocabulary families inside current durable docs

These are examples observed during the standards/history/plan review and must be audited across code/assets/config before public exposure:

- `Imperial`, when used as a setting-specific government/faction identity rather than a generic adjective.
- `Adeptus`, `Ecclesiarchy`, `Ministorum`, and related direct faction/religious institution names.
- `Arbites`, `PDF`, `Mechanicus`, `medicae`, `genestealer`, `servitor`, `bolter`, `heavy bolter`, and similar faction/unit/item terms.
- Any old project text that explicitly frames content as franchise-derived or names a protected universe.

Do not blindly delete mechanics attached to those terms. Replace the public-facing identity while preserving underlying systems where possible.

## Replacement direction seed

The safe tone is industrial civic decay, maintenance superstition, institutional brutality, and infrastructure collapse without protected franchise identity.

Possible neutral anchors for later mapping:

- Civic Engine
- Ministry of Works
- Registry
- Mandate Guard
- Public Order Directorate
- Boiler Houses
- Charter Houses
- Buried Transit Authority
- Null Chapel
- Old Machine Cult
- Low Habitations
- Municipal Arsenal

These are not final lore law. They are placeholders for replacement planning.

## Verification notes for this audit start

No code was changed in this first audit-ledger creation. No compile was run. The next implementation pass should begin with a real docs-folder inventory and then update the four durable documents rather than expanding this file family.
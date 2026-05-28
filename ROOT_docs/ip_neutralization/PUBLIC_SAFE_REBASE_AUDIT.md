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

## Initial known docs classification

The connector currently cannot list the full `docs/` directory tree, and indexed repository search is returning false negatives even for terms already visible in known files. Until a local checkout or reliable tree listing is available, this table classifies the known files already read during the purge.

| Path | Classification | Current action | Later action |
|---|---|---|---|
| `docs/MASTER_DEVELOPMENT_PLAN.md` | Durable | Keep | Update roadmap when rebase phase order changes |
| `docs/STANDARDS_AND_PRACTICES.md` | Durable | Keep | Add public-safe naming/asset/legal-text gate |
| `docs/DEVELOPMENT_HISTORY.md` | Durable | Keep | Record completed purge actions after implementation passes |
| `docs/MASTER_GOVERNANCE_REVISION_II.md` | Durable | Keep | Add original-IP/public-safe doctrine |
| `docs/STAGED_ASSET_INTEGRATION_PLAN.md` | Explicit user-ordered exception | Keep for now | Merge/retire after semantic asset migration is complete |
| `docs/briefings/NEW_CONVERSATION_BRIEFING_IP_NEUTRALIZATION_REBASE.md` | Temporary handoff/briefing | Keep during active rebase only | Fold durable instructions into the four durable docs, then archive/delete |
| `docs/ip_neutralization/PUBLIC_SAFE_REBASE_AUDIT.md` | Temporary controlled audit ledger | Keep during active rebase only | Delete or fold into durable docs when rebase closes |

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

## Starter original-IP replacement map

These are first-pass working replacements. They are not final lore law, but they are specific enough to start removing protected identity from public-facing text.

| Risk term/family | Risk | Working replacement direction | Notes |
|---|---|---|---|
| Imperial, when used as specific empire identity | YELLOW/RED by context | Civic, Charter, Mandate, StellarCore, City-State, Commonwealth | Generic adjective use may remain only when genuinely generic. |
| Adeptus / Adeptus-style institution names | RED | Directorate, Ministry, Office, College, Order, Guild, Engine House | Avoid faux-Latin franchise cadence. |
| Ecclesiarchy / Ministorum | RED | Civic Rite, Public Chapel Network, Ministry of Rites, Null Chapel, Ash Choir | Preserve civic religion/ritual maintenance without direct identity. |
| Mechanicus / tech-priest framing | RED | Mechanist, Old Machine Cult, Engine Wardens, Rite Engineers, Foundry Rite | The project title can remain; avoid direct faction/lore borrowing. |
| Arbites | RED | Public Order Directorate, Mandate Wardens, Civic Marshals | Keep law/security mechanics. |
| PDF as planetary defense force | YELLOW/RED by context | Municipal Defense Force, Charter Militia, Civic Reserve | PDF file-format references are GREEN. |
| Guard as franchise military identity | YELLOW/RED by context | Mandate Guard, Civic Guard, Line Guard, Municipal Army | Generic guard can remain for ordinary role descriptions. |
| Servitor | RED | Bound Worker, Civic Automaton, Labor Husk, Penal Automaton, Debt-Bound Shell | Needs careful ethical/worldbuilding treatment. |
| Genestealer | RED | Brood infiltrator, deep-cell hybrid, tunnel-born infiltrator | Avoid recognizable faction ecology. |
| Heretic as franchise enemy class | YELLOW/RED by context | Renegade, oathbreaker, proscribed cell, taboo cultist | Generic religious use only if not derivative. |
| Bolter / Heavy Bolter | RED | Rivet gun, storm rifle, heavy impact rifle, gyrocarbine, heavy gyro-rifle | Avoid recognizable weapon silhouette/name pairing. |
| Lasgun / Melta / Flamer-style direct families | RED/YELLOW | charge rifle, heat lance, industrial burner, pressure torch | Rebuild item stats around original function. |
| Medicae | YELLOW/RED | clinic, medical, field surgeon, civic medic, triage ward | Use plain language unless setting needs a specific institution. |
| Inquisitorial / Inquisition | RED | Black Ledger Office, Internal Registry, Mandate Inspectors | Keep investigation/secret-police mechanics without naming cadence. |
| Noble house 40K-style framing | YELLOW | Charter House, Lineage House, Estate Bloc, Patron Family | Nobility itself is generic; presentation must diverge. |
| Hive city / underhive if used as franchise tone | YELLOW/RED by context | vaulted city, stacked city, low habitations, deep wards | Avoid direct franchise phrase clusters. |

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

## First rewrite priority

The first rewrite pass should not begin with deep code architecture. It should begin with public-facing language and semantic metadata:

1. README/root docs.
2. EULA/legal notice text.
3. Launcher/installer/package strings.
4. Player-facing menu/UI strings.
5. Infopedia/codex strings.
6. Semantic asset registry names/descriptions.
7. Asset folder/file names and manifests.
8. Only then class/package/internal code identifiers when public exposure or future modding API exposure requires it.

## Verification notes for this audit start

No gameplay code was changed in this audit-ledger update. No compile was run. The next implementation pass should continue by adding durable public-safe rules to `STANDARDS_AND_PRACTICES.md`, then begin direct replacement in public-facing docs and legal text.
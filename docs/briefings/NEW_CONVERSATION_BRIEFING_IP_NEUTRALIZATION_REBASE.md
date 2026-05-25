# New Conversation Briefing — IP-Neutralization Rebase for Public Development

## Purpose

Use this briefing to open a new conversation after the launcher/distribution realization that the current private-repo workflow blocks the intended public thin-launcher development loop.

The immediate project objective is now:

```text
Sanitize and rebase The Mechanist into an original-IP, public-development-safe form so the repository and launcher artifact channel can operate publicly without exposing third-party protected names, faction identities, lore terms, logos, or obvious derivative public-facing material.
```

This is a planning and execution bridge, not a legal opinion. A EULA/disclaimer can reduce user confusion and define usage terms, but it is not a substitute for owning or licensing the content.

## Emotional/context reset

The prior thread discovered a critical distribution problem too late: a private GitHub repository can build private artifacts, but a public thin launcher cannot freely fetch release payloads from a private repo without authentication. That means the desired rapid public launcher iteration loop requires a public artifact channel, and a public artifact channel requires the project to be safer to expose.

The decision for the next thread is:

```text
Do the IP-neutralization rebase now, before broad public development/testing, instead of delaying it until final pre-release packaging.
```

Do not argue the user back into private-only Actions artifacts. That was discussed. The new working assumption is that public development/public artifact iteration is strategically more important than preserving the current private-only loop.

## Non-negotiable constraints

1. Do not convert the whole game to a full localization/text-key system yet unless explicitly instructed.
2. Do not start a sweeping runtime architecture rewrite unless the user explicitly authorizes it.
3. Do not rely on EULA/disclaimer language as the only mitigation.
4. Preserve gameplay systems, code architecture, save-path architecture, launcher work, server-admin work, and Phase 4 UI progress wherever possible.
5. Prefer a targeted text/content neutralization pass first: names, faction labels, obvious lore references, public-facing UI text, docs, file names, visible config names, package names if necessary, and generated output names.
6. Keep internal TODOs and audit notes clear, but do not leave public-facing third-party terminology in screenshots, release assets, launcher text, store text, UI, faction/unit/item names, or user-facing logs.
7. Treat visual identity as a separate but parallel risk audit: logos, faction icons, silhouettes, armor, weapons, banners, UI motifs, splash/loading art, capsule art, and screenshots.

## What not to do yet

Do not implement a full locale/content-pack system immediately. That remains a later Phase 15/16 infrastructure goal.

The current pass should instead use pragmatic direct renaming and text neutralization so public development can proceed. If a future string is expected to change again, still neutralize the visible text now using stable original-IP language. The localization/key system can later formalize the final text inventory in one controlled sweep.

Do not rename every class/package mechanically unless it is public-facing, release-facing, or legally risky. Keep implementation churn contained.

Do not create new lore bibles, faction encyclopedias, or expansive setting rewrites before the audit map exists.

## Current project status to preserve

Recent completed/merged work includes:

- Java 17 Swing launcher direction.
- StellarCore install-path structure.
- launcher diagnostics and client hash reporting.
- modded-content diagnostic warning/blocking for base-game issue reports.
- native packaging scripts and app-image/installer scaffolding.
- downloadable GitHub Actions artifact workflow.
- server admin/update foundations.
- server maintenance backup/restore authorities.
- server runtime status and backup index adapters.
- safe server update/restart marker flow.
- stream-safe multiplayer join fields.
- return to durable Phase 4 UI/presentation work, including the new square command bar foundation.

Do not discard these. The IP-neutralization pass should rebase text/content around them.

## Corrected distribution architecture

The intended public development architecture after neutralization is:

```text
Public-safe source repo and/or public artifact repo
  -> GitHub Actions builds launcher/game/server artifacts
  -> Releases or workflow artifacts expose downloadable test builds
  -> thin launcher can pull public manifest/assets without GitHub auth
  -> local install/update can iterate rapidly without source checkout
```

Until the project is public-safe, the private repo can still use Actions artifacts. But the new task is to make public-safe development possible.

## Immediate working goal

Create a public-safe rebase path with three lanes:

```text
Lane A — Audit
  Find all third-party/protected/public-risk text and visible identifiers.

Lane B — Replacement Map
  Define original-IP replacement names and naming rules.

Lane C — Controlled Rewrite
  Apply the replacements in bounded passes, test build after each pass, and avoid unnecessary system rewrites.
```

## Risk categories

Use this classification for every found item:

```text
GREEN
  Generic/original enough. Leave or lightly polish.

YELLOW
  Genre-adjacent or suspicious. Rename before public release if visible.

RED
  Obvious third-party protected term, faction, place, character, trademark, lore phrase, or public-facing derivative identity. Replace before public repo/release.

LICENSED-ONLY
  Can exist only in private notes or explicitly licensed/pitch-only content, not public-facing builds.
```

## Audit targets

Search and inspect all of the following:

```text
src/
launcher/
assets/
config/
docs/
tools/
.github/
README files
EULA/disclaimer files
installer/launcher text
server admin GUI text
runtime diagnostics text
log strings that can surface in bug reports
release/package names
asset filenames and folder names
music manifests
image manifests
faction/unit/item/weapon/status names
infopedia/codex text
main menu and setup text
campaign/mission/dialogue strings
```

## Likely high-risk vocabulary families

Search for obvious third-party/franchise terminology families first. Include direct names, partial names, abbreviations, and case variants.

Examples of categories to search:

```text
specific faction names
specific organization names
specific ranks/titles unique to a franchise
specific place names
specific character names
specific god/emperor/legion/chapter/order names
specific alien/species names
specific weapon/vehicle/unit names
specific lore phrases
specific icon/logo filenames
specific copyrighted universe names
```

Do not paste a long external IP list into public docs. Keep the public-facing replacement map focused on what exists in this project and what it becomes.

## Replacement style target

The Mechanist should retain the intended design atmosphere without copying protected identity.

Keep:

```text
industrial decay
bureaucratic cruelty
post-collapse civic machinery
ritualized maintenance culture
ancient machinery treated with superstition
factional urban sectors
security forces
nobility
guild-like industry
mutants/cultists/bandits/scavengers as generic concepts where safe
```

Replace protected specific setting identities with original factions and neutral system names.

Suggested original-IP tonal anchors:

```text
StellarCore
The Mechanist
The Civic Engine
The Foundry Rite
The Vaulted City
The Ministry of Works
The Registry
The Mandate Guard
The Boiler Houses
The Ash Choir
The Charter Houses
The Low Habitations
The Old Machine Cult
The Public Order Directorate
The Municipal Arsenal
The Buried Transit Authority
The Black Ledger Office
The Null Chapel
```

These are placeholders for replacement planning, not final lore law.

## EULA/disclaimer direction

Create a stronger public-development EULA/disclaimer, but do not overclaim. It should say, in substance:

- This is an original independent project in development.
- Names, factions, art, UI, lore, and systems are subject to change.
- Users may not upload or distribute infringing mods/assets through official channels.
- Third-party marks belong to their owners.
- No affiliation or endorsement is implied.
- Internal/private prototype references, if any are found, are not part of the public release identity.
- Public builds are distributed only with project-owned or cleared content.

Avoid language that claims a disclaimer magically authorizes third-party IP use.

## First-pass execution plan for the next conversation

### Step 1 — Establish branch and audit files

Create a branch such as:

```text
ip-neutralization/public-safe-rebase
```

Create audit documents:

```text
docs/ip_neutralization/PUBLIC_SAFE_REBASE_AUDIT.md
docs/ip_neutralization/REPLACEMENT_MAP.md
docs/ip_neutralization/VISUAL_ASSET_RISK_AUDIT.md
docs/ip_neutralization/EULA_PUBLIC_DEV_NOTES.md
```

### Step 2 — Run repository searches

Search for high-risk terms across code, docs, assets, config, and packaging files. Record every hit with:

```text
file path
line/context
risk category
public-facing yes/no
proposed replacement
rewrite priority
```

### Step 3 — Do not rewrite first

First PR should be audit-only or audit-plus-obvious-doc-warning only. Do not mangle the project before seeing the blast radius.

### Step 4 — Create replacement taxonomy

Build neutral names for:

```text
project-facing factions
subfactions
sectors
government/security bodies
industrial groups
religious/ritual groups
enemy archetypes
weapons/items/vehicles/statuses
UI/menu labels
public-facing docs/package names
```

### Step 5 — Apply bounded rewrite passes

Use small PRs by category:

```text
PR 1: public docs / README / package names
PR 2: launcher and installer text
PR 3: faction/unit/item visible strings
PR 4: infopedia/codex/menu text
PR 5: filenames/manifests/assets references
PR 6: EULA/disclaimer public-development update
PR 7: visual asset risk triage and quarantine plan
```

### Step 6 — Build/test after every rewrite pass

After each pass:

```text
mvn test/package or available build command
launcher compile/package check
Actions artifact build if available
manual grep/audit for old terms
```

## Opening prompt for the new conversation

Use this exact prompt or adapt it slightly:

```text
We are rebasing The Mechanist for public-safe development. The repository is currently private, but the strategic goal is to make public artifact/download iteration possible through a public-safe source or artifact channel. The immediate task is IP-neutralization: audit and replace public-facing third-party/protected names, faction identities, lore terms, asset names, UI text, docs, launcher text, release text, and visible diagnostic/log strings with original-IP equivalents.

Do not convert the whole game to a full localization/text-key system yet. That is a later Phase 15/16 pass. Right now we need a pragmatic public-safety rebase: audit first, replacement map second, bounded rewrite passes third. Do not rely on a EULA as a magic shield, but do prepare stronger public-development EULA/disclaimer language later in the pass.

Preserve recent infrastructure: Java 17 launcher, packaging workflows, Actions artifact workflow, server admin/update tools, stream-safe networking fields, and Phase 4 square command bar foundation. Do not discard or restart those systems.

Begin by reading this briefing and then inspect the repo for existing standards/planning files. Create or update docs/ip_neutralization/PUBLIC_SAFE_REBASE_AUDIT.md, docs/ip_neutralization/REPLACEMENT_MAP.md, docs/ip_neutralization/VISUAL_ASSET_RISK_AUDIT.md, and docs/ip_neutralization/EULA_PUBLIC_DEV_NOTES.md. First action should be an audit and plan, not a sweeping rewrite.
```

## Success condition for this rebase

The project can move public when:

```text
1. obvious third-party text identifiers are gone from public-facing files;
2. obvious protected faction/unit/lore names are replaced;
3. package/release/launcher/server/admin UI text is original-IP safe;
4. visual assets are inventoried and risky assets are removed, quarantined, or replaced;
5. EULA/disclaimer is strengthened but not used as the sole legal shield;
6. build and packaging workflows still run;
7. launcher artifact workflow can produce public-testable outputs without exposing private source-only material;
8. future full text-key/localization conversion remains possible but is not forced prematurely.
```

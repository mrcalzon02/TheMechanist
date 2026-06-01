# Concord IP Neutralization Ledger

## Purpose

This ledger tracks the project-wide sweep to remove Warhammer 40K-adjacent intellectual-property language from source files, class names, menu text, internal IDs, generated strings, diagnostics, and documentation, replacing it with Concord-adjacent original terminology.

This is not a blind text replacement campaign. The same visible term can appear as:

- a Java class name
- a filename
- a method name
- a string shown to the player
- a save/profile/internal ID
- a diagnostics label
- a branch, file, folder, or historical documentation reference

Each class of usage has different breakage risk.

## Current Sweep Status

Status: **Audit-first, replacement map active**

The sweep begins with term discovery and classification before destructive renaming. Compiler smoke work continues in parallel.

## Replacement Doctrine

Use Concord-original names that preserve gameplay meaning while breaking direct 40K identity.

Preferred project flavor:

- industrial civic bureaucracy
- occult-administrative machinery
- decayed interstellar institutions
- Concord registry / charter / ward / machine-cult-adjacent but original language
- no direct Games Workshop / Warhammer / 40K proper nouns

## High-Risk Terms and Preferred Replacements

| Suspect Term | Risk | Preferred Concord Replacement | Notes |
|---|---:|---|---|
| Warhammer | Critical | Concord | Remove direct reference everywhere except migration docs if absolutely needed. |
| 40K / 40k | Critical | Concord | Remove direct reference. |
| Imperium / Imperial | High | Concordat / Concord / Charter | Choose based on context. |
| Adeptus | High | Collegium / Collegia | Use for institutions. |
| Mechanicus | High | Mechanist / Collegium Mechanica / Iron Collegium | Existing project name can remain `TheMechanist`; avoid `Mechanicus`. |
| Tech Priest / Tech-Priest | High | Mechanist / Engine Cleric / Machine Adjunct | Prefer `Mechanist` for class-facing terms. |
| Magos | High | Master Mechanist / Prime Mechanist / Arch-Mechanist | Use rank flavor. |
| Omnissiah | High | Prime Engine / Canticle Engine / Machine Covenant | Avoid direct theology match. |
| Arbites | High | Civic Wardens / Concord Wardens | Current compiler cluster already points at Civic Wardens. |
| Adeptus Arbites | High | Concord Civic Wardens | Institution-level replacement. |
| Administratum | High | Concord Registry / Charter Bureau | Use for bureaucracy and paperwork. |
| Ecclesiarchy | High | Covenant Synod / Concord Synod | Religious/civic authority. |
| Ministorum | High | Covenant Ministry / Civic Ministry | Avoid direct term. |
| Inquisition / Inquisitor | High | Auditorium / Concord Auditor / Black Registry Inspector | Preserve investigation role without 40K term. |
| Ordo | High | Registry Order / Audit Order | Avoid direct Latinized 40K order naming when attached to investigation. |
| Exterminatus | High | Planetary Nullification / Extinction Writ | Use only as fictional Concord legal terror. |
| Rosarius | High | Covenant Reliquary / Ward Sigil | Avoid direct artifact term. |
| Warrant of Trade | High | Free Charter / Void Charter | Preserve trade-privilege gameplay meaning. |
| Space Marine / Astartes | Critical | Void Marine / Concord Marine / Armored Trooper | Avoid direct super-soldier branding. |
| Servitor | Medium-High | Bound Laborer / Cortex Thrall / Contract Thrall | Depends on body-horror/labor context. |
| Cogitator | Medium-High | Logic Engine / Registry Engine / Calculation Shrine | Use `logic engine` for neutral systems. |
| Hive City / Hive | Medium | Stack City / Arcology / Spire-Warrens | `hive` can be generic; replace direct 40K civic usage. |
| Medicae | Medium-High | Clinic / Medical Ward / Chirurgeon | Prefer readable terms. |
| PDF / Planetary Defense Force | Medium-High | Local Defense Wardens / Charter Defense Force | Preserve militia/defense meaning. |
| Astra Militarum / Imperial Guard | Critical | Concord Guard / Charter Guard | Avoid faction identity. |
| Guilliman | Critical | Lord Regent / Concord Regent / High Regent | Remove named character. |
| Baneblade / Leman Russ / Chimera / Basilisk / Sentinel / Hydra | Critical when used as 40K vehicles | Original vehicle names | Replace if present as direct 40K vehicle references. |

## Identifier Replacement Conventions

Use lowerCamelCase for variables and methods:

- `arbites` -> `civicWardens`
- `civic Wardens` -> `civicWardens`
- `mechanicus` -> `mechanist` or `ironCollegium`
- `ecclesiarchy` -> `covenantSynod`
- `administratum` -> `charterBureau`
- `inquisitor` -> `concordAuditor`
- `cogitator` -> `logicEngine`
- `servitor` -> `cortexThrall`

Use UpperCamelCase for classes:

- `ArbitesPrecinctFixtureAuthority` -> `CivicWardenPrecinctFixtureAuthority`
- `EcclesiarchyTempleApi` -> `CovenantSynodTempleApi`
- `Mechanicus...` -> `Mechanist...` or `IronCollegium...`

Use kebab/snake-safe names for IDs only after compatibility review:

- `arbites_precinct` -> `civic_warden_precinct`
- `mechanicus_forge` -> `mechanist_forge` or `iron_collegium_forge`
- `ecclesiarchy_temple` -> `covenant_synod_temple`

## Risk Categories

### Safe Immediate Replacements

- Comments
- Player-facing strings
- Diagnostic labels
- Non-persisted display names
- Local variable names that do not affect serialization

### Requires Compile-Aware Rename

- Class names
- File names
- Method names
- Enum constants
- Public static constants

### Requires Migration / Alias Plan

- Save IDs
- Serialized enum names
- Profile keys
- Asset paths
- Network protocol IDs
- Script commands
- Mod/plugin IDs

## Current Known Compiler Cluster

The first real Java smoke compile reached source diagnostics and found a syntax cluster caused by the invalid identifier phrase:

```java
civic Wardens
```

Files:

- `src/mechanist/RoomFixtureInteractionAuthority.java`
- `src/mechanist/WorldRuntimeGenerationFramework.java`

Preferred fix:

```java
civicWardens
```

This is both a compile repair and a Concord-safe replacement direction for the previous `Arbites`/law-enforcement concept.

## Sweep Procedure

1. Generate an audit report of all suspect terms.
2. Classify each hit as class/file/method/string/ID/comment/doc.
3. Apply safe local string/comment/variable replacements first.
4. Apply compile-aware Java identifier renames in small clusters.
5. Defer serialized IDs until aliases/migrations are available.
6. Run smoke compile after each cluster.
7. Record replacement decisions in this ledger.

## Current One-Sentence Marker

The Concord IP-neutralization sweep is now active as an audit-first campaign running beside compile cleanup; the first confirmed actionable overlap is the `civic Wardens` syntax cluster, which should be normalized to `civicWardens` while broader Warhammer-adjacent terms are mapped into Concord-original institutions and IDs.

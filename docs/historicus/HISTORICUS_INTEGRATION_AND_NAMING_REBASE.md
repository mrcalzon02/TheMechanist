# The Mechanist — Historicus Integration and Naming Rebase

## Purpose

This document establishes the durable integration structure for the Concord Archive lore package and defines the canonical naming and ingestion direction for the ongoing public-safe thematic restructuring effort.

The archive material is not treated as disposable flavor text. It is part of the long-term world simulation, identity, historical, factional, institutional, and cultural foundation of The Mechanist.

This document exists to:

- Support Historicus integration.
- Support public-safe thematic divergence.
- Support semantic lore indexing.
- Support future localization.
- Support long-term codex and Infopedia tooling.
- Support NPC identity and lineage systems.
- Support future editor and modding integration.
- Replace inherited franchise-dependent thematic scaffolding with durable original setting architecture.

## Historicus root structure

Recommended top-level Historicus categories:

| Registry Prefix | Category |
|---|---|
| HIST-000 | Foundational Setting and Continuity |
| HIST-100 | Civilization and Civic Structure |
| HIST-200 | Military Orders and Security Arms |
| HIST-300 | Industrial Orders and Continuance Doctrine |
| HIST-400 | Spireholds and Population Conditions |
| HIST-500 | Logistics, Trade, and Provisioning |
| HIST-600 | Religious Movements and Cultural Doctrine |
| HIST-700 | Threat Entities and Hostile Forces |
| HIST-800 | Historical Catastrophes and Frontier Losses |
| HIST-900 | Naming Registries and Identity Doctrine |

## Archive normalization rules

- Preserve numerical sequencing internally for stable lore references and future localization.
- Convert lore files into canonical Historicus IDs.
- Avoid franchise-derived terminology in filenames, categories, and public UI labels.
- Use uppercase underscore-separated archival filenames for raw source storage.
- Expose player-facing names through localization keys and registry metadata rather than raw filenames.
- Maintain lore-source lineage metadata for future editor and codex tooling.
- Historicus entries should be treated as semantic registry records, not isolated text blobs.

## Canonical naming pattern

Recommended lore entry pattern:

`HIST-[CATEGORY]-[ENTRY]-[VARIANT]`

Examples:

- `HIST-200-003A-LEGIO_LINEA`
- `HIST-400-004C-LOW_HABITATIONS`
- `HIST-900-011A-CONCORD_NAME_REGISTRY`

## Runtime authority direction

Recommended future authorities:

- `HistoricusRegistryAuthority`
- `LoreEntryAuthority`
- `IdentityLedgerAuthority`
- `ConcordNameRegistryAuthority`
- `CivicLineageAuthority`
- `ArchiveLocalizationAuthority`
- `LoreCrossReferenceAuthority`
- `HistoricusCategoryIndex`

## Archive ingestion mapping

| Current Archive Family | Historicus Category | Suggested Runtime Purpose |
|---|---|---|
| `001_CORE_SETTING_AND_AGE_OF_CONTINUITY` | HIST-000 | Foundational world-state introduction |
| `002_*` catastrophe/event records | HIST-800 | Historical crisis and event archive |
| `003_*` military organizations | HIST-200 | Faction and military codex entries |
| `004_*` spirehold civilian material | HIST-400 | Population simulation and civic doctrine |
| `005_*` logistics/provisioning | HIST-500 | Trade, supply, and economic simulation |
| `011_*` naming and registries | HIST-900 | NPC generation and identity ledgers |
| `017_*` continuance doctrine | HIST-300 | Immortality/mechanical continuity doctrine |
| `020_*` threat entities | HIST-700 | Hostile ecosystem and bestiary references |

## Historicus integration doctrine

The Historicus system is not merely a codex browser.

Long-term intended functions:

- world-history persistence,
- faction and institutional records,
- dynamic NPC lineage references,
- civic registry integration,
- historical event references,
- localized lore rendering,
- Infopedia cross-linking,
- event-chain references,
- editor integration,
- and simulation-state archival context.

Historicus entries should eventually support:

- semantic IDs,
- category metadata,
- cross-reference links,
- source lineage,
- public/restricted classification,
- localization keys,
- and future procedural historical injection.

## Naming registry doctrine

The Concord naming archives are now considered authoritative replacement direction for:

- player generation,
- NPC generation,
- historical registries,
- civic ledgers,
- obituary systems,
- military rosters,
- trade manifests,
- archival records,
- and institutional identity tracking.

The naming structure should eventually support:

- weighted regional distributions,
- noble/common variants,
- lineage persistence,
- registry IDs,
- cultural drift,
- and sector-specific naming divergence.

## Public-safe terminology direction

The project direction is industrial civic decay, continuity obsession, infrastructural survival, institutional persistence, and mechanized bureaucracy without reliance on protected franchise identity.

First-pass terminology replacement direction:

| Legacy Direction | Public-Safe Direction |
|---|---|
| Imperial | Civic / Charter / Mandate |
| Hive / Underhive | Vaulted City / Low Habitations / Deep Wards |
| Mechanicus-style institutions | Engine Orders / Foundry Rites / Mechanists |
| Arbites-style security | Public Order Directorate / Mandate Wardens |
| Ecclesiarchy-style institutions | Civic Rite / Null Chapel / Ash Choir |

## Long-term integration target

The Concord Archive material is intended to become:

- the spine of the Historicus system,
- the backbone of long-term cultural simulation,
- the foundation of institutional and historical continuity,
- and a major component of the project's public-safe thematic divergence effort.

The long-term goal is not merely to rename inherited concepts.

The goal is to establish a setting capable of standing independently through:

- civic structure,
- logistics,
- population pressure,
- continuity doctrine,
- institutional memory,
- infrastructure decay,
- archivalism,
- identity registries,
- and cultural persistence.

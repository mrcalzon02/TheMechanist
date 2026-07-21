# The Mechanist - Master Development Plan

This document is the authoritative roadmap, checkpoint, and handoff control surface for The Mechanist. It is not a changelog, implementation transcript, asset index, or full design bible. Detailed phase-group expansions live in the ordered milestone sequence indexed by `ROOT_docs/MILESTONE_INDEX.md`.

## Authority documents

Before any development pass, read these documents in order:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md` - current checkpoint, phase order, and movement authority.
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md` - durable implementation, build, packaging, asset, UI, Java 17, security, and release-safety rules.
3. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md` - long-term architecture doctrine and boundary discipline.
4. `ROOT_docs/DEVELOPMENT_HISTORY.md` - active milestone-era completion log; older history is archived under `ROOT_docs/archive/`.
5. `ROOT_docs/MILESTONE_INDEX.md` - ordered milestone navigation for detailed phase-group targets.
6. `ROOT_docs/LEGACY_MILESTONE_SOURCE_MAP.md` - source/archive map for older topical source material.

The Standards file remains hard release law. Governance remains design ethos and architectural boundary law. This master plan controls movement and handoff order.

## Documentation containment

Planning belongs here or in the ordered milestone sequence. Completed implementation belongs in `DEVELOPMENT_HISTORY.md`. Durable implementation rules belong in `STANDARDS_AND_PRACTICES.md`. High-level doctrine belongs in `MASTER_GOVERNANCE_REVISION_II.md`.

Do not create additional milestone, supplement, audit, addendum, or pass-specific planning files unless the user explicitly orders a separate durable artifact and the material cannot fit an existing authority document. Legacy topical milestone files remain source/archive material until explicitly retired.

## Active checkpoint

Current checkpoint: **Milestone 06 - Vehicles, Structural Scale, and Strategic Power**, with the cross-cutting **Sub-milestone 06.R limited-alpha release-readiness gate** active wherever package, launcher, server, persistence, inventory, native-image, or evidence work blocks safe testing.

The prior Milestone 02 / Gate 3 readability lane is a completed foundation, not the current movement owner. Its UI, targeting, movement-safety, conversation, trade, inventory, Doom viewport, and player-facing validation authorities remain regression constraints and must be reused rather than replaced.

Current Milestone 06 implementation truth includes:

- persistent vehicle identity, class, manufacturer, model, variant, production batch, ownership, legality, condition, components, history, purchase, repair, salvage, and seizure state;
- vehicle access, cargo, driver, crew, passenger, and motor-pool assignment authority;
- constrained local road, lane, garage, depot, parking, and curb-adjacent transit with readable refusal and non-mutation behavior;
- strategic vehicle transit, fuel or power readiness, source-coordinate persistence, and atomic transfer commits;
- maintenance, damage, loss, recovery, seizure, repair, salvage, faction doctrine, motor-pool, deterrence, and balance-of-power integration;
- operation feedback, bounded pulse state, facing/headlight emitters, route preview, and manual planned-movement integration;
- registered Milestone 06 smoke chains through the Gate 3 player-facing suite where those systems are touched.

Sub-milestone 06.R remains **not ready for limited-alpha distribution**. Canonical portable composition, native app-image staging, shared build identity, supervised single-player hosting, independent-host transport/session foundations, inventory gates, and local release evidence tooling exist, but exact Java 17, inventory, native, clean-machine, installer-lifecycle, clearance, and publication evidence has not been accepted.

The next implementation slice must come from the newest `DEVELOPMENT_HISTORY.md` boundary and `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md`. Prefer a coherent player-visible or systemic vehicle slice that reuses existing ownership, persistence, economy, faction, movement, UI, and world authorities. If a concrete release-gate failure is observed first, repair that exact failure before expanding gameplay scope.

### Allowed now

- Milestone 06 vehicle definitions, components, provenance, ownership, access, transit, operation feedback, maintenance, loss, faction doctrine, strategic power, contracts, structural-scale, audit, and Infopedia work in dependency order.
- Phase 0 governance, build hygiene, Java 17 verification, package identity, inventory, release evidence, and handoff corrections required to keep the active work publish-safe.
- Sub-milestone 06.R portable distribution, launcher, internal-host, independent-host, native app-image, diagnostics, inventory, and limited-alpha operating support.
- Small UI/readability repairs needed to expose Milestone 06 state honestly through existing game-owned surfaces.
- Narrow persistence and server-boundary repairs required by touched vehicle or release systems.
- Documentation updates to existing authority documents when implementation truth or movement order changes.

### Not active now

- Broad Milestone 07 through 09 implementation unless a narrow dependency is explicitly required by the active vehicle slice.
- Broad world-generation expansion or facility stamping before the systems those stamps consume are ready.
- Public matchmaking, open server browsing, production account/authentication services, or claims of fully authoritative remote gameplay.
- Live external mod downloading, arbitrary classpath mutation, or hot loading.
- True per-sector parallel world mutation while shared legacy world state remains unsplit.
- Native installer lifecycle, release clearance, prerelease publication, or release-history claims without exact target-platform evidence.
- New standalone Markdown notes or duplicate planning/audit documents.

## Current publish-safe gates

Each gate must leave the repository buildable, reviewable, and honest. Do not begin a later gate by expanding scope around an unresolved earlier failure.

### Gate 1 - Documentation and repository hygiene

Required outcomes:

- this master plan identifies the actual active checkpoint;
- `MILESTONE_INDEX.md` lists the ordered `00` through `10` sequence;
- `LEGACY_MILESTONE_SOURCE_MAP.md` maps stale topical files to canonical homes;
- README remains user-facing rather than becoming a roadmap or changelog;
- generated indexes and manifests remain under their governed consuming locations;
- no duplicate history, audit, handoff, or planning authority is created.

### Gate 2 - Bootstrap and package identity hygiene

Required outcomes:

- execution remains installer -> thin launcher -> verified client/server payloads;
- one canonical distribution authority owns package composition;
- launcher, client, server, runtime, support libraries, native images, manifests, checksums, and reports agree on exact version, commit, platform, Java release, and hardening identity;
- mutable saves, profiles, mods, exports, diagnostics, and caches remain outside installer-controlled application directories;
- dirty source trees cannot be labeled as an exact clean commit candidate.

### Gate 3 - Player-facing containment and validation

Required outcomes:

- high-traffic UI and vehicle controls remain inside game-owned surfaces;
- movement, transit, access, repair, collision, structural-damage, and contract refusals provide honest readable reasons;
- presentation state does not mask or invent gameplay authority;
- vehicle operation feedback, route preview, inspection, faction state, and audit text avoid raw IDs and placeholder leakage;
- touched systems remain registered through focused Milestone 06 and Gate 3 smokes.

### Gate 4 - Build, Java 17, package, and evidence

Required outcomes:

- compile from source with Java 17 compatibility;
- rebuild client and server artifacts from the same corrected source tree;
- scan every shipped classfile and reject major versions above 61;
- run focused smokes tied to the touched subsystem;
- verify canonical distribution, archive integrity, packaged Gate 3, synthetic extracted-install behavior, native app-image composition where required, and cross-stage evidence coherence;
- require a clean source identity and a committed governed repository manifest before native or clearance certification;
- state honestly what was not executed or manually tested.

## Ordered milestone sequence

Detailed phase-group expansions are maintained in `ROOT_docs/MILESTONE_INDEX.md`:

- `MILESTONE_00_GOVERNANCE_PACKAGE_AND_INDEX.md`
- `MILESTONE_01_ASSET_SEMANTICS_AND_CONTENT_PROMOTION.md`
- `MILESTONE_02_UI_EXAMINATION_INFOPEDIA_AND_PLAYER_READABILITY.md`
- `MILESTONE_03_PRODUCTION_KNOWLEDGE_SKILLS_AND_ITEM_QUALITY.md`
- `MILESTONE_04_POPULATION_PROVENANCE_ECONOMY_AND_FACTION_MARKETS.md`
- `MILESTONE_05_CONSTRUCTION_BLUEPRINTS_OWNERSHIP_AND_PLAYER_FACTION_PARITY.md`
- `MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md`
- `MILESTONE_07_AGES_OF_CONTROL_SCHEMES_AND_QUEST_LIFECYCLES.md`
- `MILESTONE_08_MEDICAL_CYBERNETICS_NARCOTICS_AND_BODY_SYSTEMS.md`
- `MILESTONE_09_WORLDGEN_DISTRICTS_ROOMS_AND_FACILITY_STAMPS.md`
- `MILESTONE_10_PERSISTENCE_SAVE_SCHEMA_MIGRATION_AND_BACKUPS.md`

## Global dependency order

- Asset readiness precedes broad content placement.
- UI readability and Infopedia support precede or accompany systems the player must understand.
- Knowledge, skills, machines, materials, and quality precede deep production claims.
- Population provenance precedes item provenance; item provenance precedes robust markets.
- Vehicle/component provenance precedes vehicle factories, deployment, and strategic power.
- Vehicle ownership and access precede contracts, seizure, motor-pool, and faction deployment claims.
- Comparative structural scale precedes vehicle, machine, wall, and mounted-weapon combat claims.
- Vehicle operation state must drive route behavior, feedback, lighting, sound, fuel/power, persistence, and audit truth rather than spawning parallel decorative state.
- Ages of Control and leadership schemes consume established population, item, facility, vehicle, and room-control provenance.
- Worldgen expansion consumes semantic assets, provenance, faction identity, schemes, world events, and audit surfaces.
- Persistence ownership applies to every milestone before publish-safe claims.

## Quality-of-life and sensibility evaluator

Every implementation pass must answer:

- What becomes easier, clearer, safer, more interesting, or more meaningful for the player?
- Is the result player-visible, audit-visible, or internal, and is that appropriate?
- Does the command flow coherently into authority, feedback, persistence, and recovery?
- Are failure reasons honest and useful?
- Are raw IDs, class names, registry handles, manifest keys, duplicate state, and debug residue hidden from ordinary UI?
- Does the pass preserve old-machine performance and avoid hot-loop allocation, string churn, audio spam, asset bloat, and needless redraws?
- Does the result remain publish-safe and save/load-safe?

No pass may claim completion while player comprehension, authority integrity, persistence, package identity, or release evidence has an unresolved negative answer.

## Current development handoff

Current target: continue **Milestone 06** from the newest active development-history entry, while treating **issue #47 / Sub-milestone 06.R** as the release-blocker lane.

Required first reads:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md`
3. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`
4. `ROOT_docs/DEVELOPMENT_HISTORY.md`
5. `ROOT_docs/MILESTONE_INDEX.md`
6. `ROOT_docs/MILESTONE_06_VEHICLES_STRUCTURAL_SCALE_AND_STRATEGIC_POWER.md`
7. `ROOT_docs/LOCAL_RELEASE_SEQUENCE.md` when build, packaging, inventory, native, or evidence paths are touched.
8. The exact vehicle authorities and focused smoke chain named by the newest relevant commit or history entry.

Preferred next-work shape:

- choose one coherent Milestone 06 slice rather than extending an audit chain;
- reuse existing vehicle runtime, ownership, transit, maintenance, faction, economy, contract, UI, and persistence authorities;
- prove success, refusal, persistence, and non-mutation boundaries in a focused smoke;
- register the smoke through the existing Milestone 06 chain and Gate 3 where appropriate;
- compile with Java 17 and run the touched smoke before recording completion;
- regenerate the repository manifest and Mermaid/function-map ledgers locally when required; do not fabricate connector-only generated evidence;
- keep release history and readiness blocked until exact evidence is accepted.

## Anti-drift directive

When uncertain, do not create another document and do not widen scope. Read the authorities, identify one owner, make the smallest direct implementation that advances Milestone 06 or repairs the first concrete release failure, and state exactly what verification remains.
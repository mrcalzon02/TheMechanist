# The Mechanist — Project State Onboarding Assessment

Generated: 2026-06-03  
Repository: `mrcalzon02/TheMechanist`  
Assessment scope: repository governance, development history, file manifest, package layout, Mermaid function map, selected source entrypoints, package pipeline, and staged asset plan.

## 0. Status of this document

This is a user-ordered onboarding and state-assessment snapshot. It is not a replacement for the master plan, standards, governance, milestone index, or development history.

Use it as a fast orientation document for a new development conversation or Codex-style handoff. When it conflicts with an authority document, resolve the conflict in this order:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md` for current checkpoint, phase order, and movement authority.
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md` for durable implementation, build, packaging, asset, UI, and release-safety rules.
3. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md` for doctrine and architecture boundaries.
4. `ROOT_docs/DEVELOPMENT_HISTORY.md` for completed implementation record.
5. `ROOT_docs/MILESTONE_INDEX.md` and the ordered `MILESTONE_00` through `MILESTONE_10` files for detailed phase-aligned targets.
6. This assessment for orientation only.

## 1. Executive summary

The Mechanist is currently a Java 17 desktop/client and headless-server simulation project in a segmented workspace. The repository is not meant to be the final installed client layout. The intended delivery chain is:

```text
PACKAGE_installer -> PACKAGE_launcher -> PACKAGE_client -> packaged server payload
```

The active checkpoint is still **Phase 4 publish-safe client containment and package/handoff hygiene**. The current practical line is not broad new simulation expansion. It is making the Swing client readable, publish-safe, packageable, and handoff-safe while preserving the server-authoritative and persistence boundaries already introduced in the prior 0.9.10-era work.

The most recent development history shows four dominant workstreams:

1. **Gate 1 documentation and repository hygiene**: workspace roots were repaired to the implemented `ROOT_*` and `PACKAGE_*` architecture, and a full repository file manifest was added as a discovery-only index.
2. **Gate 2 package identity**: the launcher moved away from full-repository clone/update behavior and toward manifest-described client/server/support packages, local seed acquisition, hash verification, compatibility checks, and rollback repair. Remote artifact authentication/signing remains open.
3. **Gate 3 UI/readability/Milestone 02**: a very large sequence of player-facing readability, interaction, Examine, Infopedia, movement-planning, prompt, pet, trade, construction, base, faction-roster, medical, evidence, market, and transfer improvements has been implemented and smoke-tested according to the history.
4. **Gate 4 local package seed**: an offline local seed builder can compile/stage client, server, and launcher jars under a launcher-managed manifest package layout and scan Java 17 classfile compatibility. Full native package release readiness remains gated by platform packaging, authentication, trust metadata, and full verification.

The codebase is not small. The current Mermaid map reports 391 mapped Java modules, zero unpositioned modules, and 11 oversized mapped modules. The biggest architectural pressure points are the retired monolithic `GamePanel` compatibility bridge, the very large world-generation and production frameworks, and the need to keep all new work aligned to named authorities rather than re-growing central switch clusters.

## 2. Current workspace map

The implemented top-level workspace should be read as follows:

| Workspace | Current role | Assessment |
|---|---|---|
| `PACKAGE_client/` | Shipped client/runtime package boundary: runtime assets, client launchers, config, bundled libraries, modding examples, client-facing notes, and server payload material. | Active runtime package tree. Do not treat root repository paths as the final client layout. |
| `PACKAGE_launcher/` | Thin launcher/orchestrator workspace: launcher Java code, launcher resources, profiles, package acquisition, verification, diagnostics, update, rollback, and launch handoff. | Central to Gate 2. The launcher should own package acquisition and verification before client launch. |
| `PACKAGE_installer/` | Installer/native packaging workspace. | Should install the launcher/support set, not the whole repo or full client payload. |
| `ROOT_docs/` | Durable governance, standards, milestones, history, handoff, archive, package notes, and planning material. | This is the active document root, not legacy `docs/`. Avoid additional doc sprawl unless user-ordered. |
| `ROOT_SRC_assets/` | Protected source asset vault. | Source material only. Do not modify in place. Promote transformed/runtime-ready outputs into consuming package trees. |
| `ROOT_tools/` | Developer tooling and verification helpers outside the shipped runtime. | Contains the repository manifest updater. Use it whenever files are added, removed, renamed, moved, or replaced. |
| `scripts/` | Current build/package/security automation not yet fully relocated under tighter ownership. | Still active; some scripts remain transitional. |
| `src/` | Main Java source tree while the root build remains Maven-rooted. | Runtime code is highly modular but still carries compatibility bridges. |
| `.github/` | Workflow material. | Windows native packaging workflow exists, but full native verification must still be stated honestly when not run. |

The file manifest at `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` is a discovery index, not architecture authority. Its purpose is to make binary assets, audio, jars, package files, docs, tools, and source files findable when code search cannot inspect them. It must be regenerated after this assessment and any other file change.

## 3. Authority and governance interpretation

The project is in a **platform-architecture era**, not a loose prototype era. The governance rule is to reduce future cost by building shared authorities, stable data boundaries, inspectable tooling, and disciplined documentation before expanding every mechanic.

The practical doctrine is:

- Prefer named authorities and APIs over local switch clusters.
- Keep simulation depth proportional to relevance: immediate detail near the player, ledgers for local districts, and strategic summaries for distant systems.
- Keep player-facing UI readable and contained in the main game surface.
- Treat assets as semantic runtime identities, not just image files or atlas cells.
- Keep external mod loading, hot-loading, source acquisition, public multiplayer, and real remote server authority closed until their architecture, safety, package, and test gates exist.
- Separate installer, launcher, client, server, package manifests, runtime assets, user saves, profile/settings, and editor/content definitions.

One low-risk documentation issue remains: `MASTER_GOVERNANCE_REVISION_II.md` still contains older wording that refers to durable documents in `docs/`, while the active README, master plan, standards, and manifest now use `ROOT_docs/`. This should be corrected during the next documentation hygiene pass without changing doctrine.

## 4. Development history state

The active development history is no longer the pre-milestone mega-ledger. It is the milestone-era completion log. The pre-milestone record is archived under `ROOT_docs/archive/DEVELOPMENT_HISTORY_PRE_MILESTONE_DEVELOPMENT.md`.

### Gate 1 — documentation and repository hygiene

Gate 1 repaired the active map from stale roots like `docs/`, `client/`, `launcher/`, `installer/`, `assets/`, and `tools/` to the implemented `ROOT_docs/`, `ROOT_SRC_assets/`, `ROOT_tools/`, `PACKAGE_client/`, `PACKAGE_launcher/`, and `PACKAGE_installer/` roots. The repository file manifest was added as a user-ordered discovery index, with 980 indexed file rows and 407 binary files at the time it was generated.

Assessment: strong and necessary. The risk is that the manifest becomes stale immediately after new files are added unless regenerated.

### Gate 2 — bootstrap and package identity

Gate 2 has made real progress: the launcher no longer relies on an active full-repository updater path as the main bootstrap route. It verifies manifest-described client, server, and support package layout; supports local package seed acquisition; validates support-library hashes; handles rollback repair; and rejects unsupported schema/platform combinations.

Assessment: solid local architecture, but incomplete for release. The documented open dependency is publish-safe authenticated remote acquisition, signing or equivalent trust metadata, and full native Windows/Linux packaging verification.

### Gate 3 — UI, readability, and Milestone 02

Gate 3 is the most active recent development area. The history records player-facing text containment, presentation audit consolidation, movement planning, movement refusal feedback, Look-to-Examine, repeated Examine depth, controls references, pet interaction feedback, interaction preflight feedback, objective guidance, inventory/trade/build/conversation/production guidance, Infopedia audits and related links, transfer workflow consistency, base storage, faction roster/equipment, condition estimates, medical status, and quest evidence readability.

Assessment: this is a major improvement lane and should continue only through exact, testable migrations. Future work should prefer `PlayerFacingText` as the facade instead of importing individual helper classes in scattered callsites.

### Gate 4 — build and package reproducibility

The local package seed builder can compile client/server and launcher sources with `javac --release 17`, create executable jars, stage them under a launcher-managed manifest layout, and scan classfiles for Java 17 compatibility.

Assessment: this is the correct offline bridge while Maven/private artifact authentication is unresolved. It is not a substitute for full Maven/native package release verification.

## 5. Mermaid function map interpretation

The current Mermaid master map is an active code-position record generated/evaluated on `2026-06-02 09:23:43`. It reports:

- Java modules mapped: `391`
- Unpositioned modules: `0`
- Oversized mapped modules: `11`

This is a very good architecture-control signal: the project has a map, and the map claims total positioning coverage. The important warning is that the generated ledgers named by the Mermaid file could not be fetched through the repository contents API during this pass at the expected paths:

- `ROOT_docs/functionmap/generated/CODE_MERMAID_POSITION_LEDGER.tsv`
- `ROOT_docs/functionmap/generated/CODE_MERMAID_EVALUATION.tsv`

That does not prove they are absent from every branch or local workspace, but it does mean the committed path should be verified or regenerated. Since standards state that unpositioned modules and error clusters are architecture debt, future source changes should regenerate or validate the map and ledgers before claiming completion.

### Functional zone map

| Zone | Modules | Interpretation | Evaluation |
|---|---:|---|---|
| `UI_RENDER` | 96 | Largest mapped zone. Includes first-person/visual/render/editor/economic/topology/identity/support surfaces. | Highest UI complexity and likely highest regression risk. Keep under Gate 3/Milestone 02 discipline. |
| `LOCALIZATION_TEXT` | 60 | Text/media/save/audio/senses/art/persistence/loot/knowledge/chat/construction naming and display boundaries. | Strong evidence of player-facing text containment work; continue routing through stable facades. |
| `UI_INPUT` | 46 | Sector manager, commands, key binding, gamepad, targeting, command requests, security-adjacent input paths. | Important for movement planning, targeting consistency, and future controller support. Must consume shared transforms. |
| `SERVER_AUTH` | 39 | Headless server, admin console, TCP relay, launcher/client context, disaster recovery, network throttling, secure packet paths. | Architecture exists, but public multiplayer and real remote authority remain closed by governance. |
| `WORLD_GEN` | 31 | World runtime generation, rooms, roads, zone generation, transition paths. | One oversized core: `WorldRuntimeGenerationFramework`. Do not expand broadly until prerequisites are met. |
| `RUNTIME_OPTIONS` | 25 | Game options, display scale, JVM profile, accessibility, experimental/mod/runtime toggles, Netty binding. | Important for old-machine viability and settings persistence. |
| `ASSET_REGISTRY` | 22 | Semantic asset registry, tile/object/portrait authorities, generated asset runtime, path audit. | Strong staged migration. Stage 10 remains next for mod/art-pack registry extension. |
| `COMBAT_SIM` | 21 | World simulation, population, faction services, movement, combat runtime, command parity, interpolation, turn manager. | Rich core simulation; must remain tied to server/world authority. |
| `FIXTURE_MACHINE` | 18 | Production authority, machine queue, fixture authorities, interaction registry. | High value, high risk. `ProductionAuthorityFramework` is oversized. |
| `INVENTORY_PERSIST` | 16 | Item economy, container/trade, storage, transaction guard, stock trackers, save/profile touchpoints. | Important for markets, transfer, persistence, and provenance. Keep transaction guards central. |
| `DIAGNOSTIC_DOC` | 17 | Audits, overlays, debug logging, player-facing text smokes. | Good verification backbone. Keep smoke suites updated with every UI/presentation pass. |

### Oversized-module pressure points

The map calls out several large functional centers. The most important ones for future refactor planning are:

- `WorldRuntimeGenerationFramework.java` — 357 functions / 8163 lines.
- `ProductionAuthorityFramework.java` — 314 functions / 3105 lines.
- `FirstPerson3DFramework.java` — 85 functions / 1042 lines.
- `ItemEconomyFramework.java` — 71 functions / 1327 lines.
- `ContainerTradeFramework.java` — 92 functions / 802 lines.
- `GameOptionsFramework.java` — 73 functions / 712 lines.
- `SectorManager.java` — 59 functions / 609 lines.
- `SimulationEditorSuite.java` — 60 functions / 824 lines.
- `PopulationPersonnelFramework.java`, `FactionServicesFramework.java`, and `LegacyPanelContext.java` also deserve careful boundary review.

Oversized does not mean wrong. It means future work should be narrow, diagnostic-first, and should extract stable authority seams only when touching the subsystem anyway.

## 6. Current runtime architecture

### Client entry

`src/mechanist/TheMechanist.java` is a thin Swing entrypoint. It checks package-root relaunch authority, loads JVM/runtime/display settings, loads game options, starts Swing, initializes debug logging, audits runtime separation, creates the main frame, applies the app icon, constructs `GamePanel`, installs world-start flow, starts the boot sequence, configures window mode, and requests focus.

Assessment: the entrypoint is reasonably clean and delegates to named authorities. This aligns with governance.

### GamePanel bridge state

`src/mechanist/GamePanel.java` is intentionally absent. The active compatibility surface is `GamePanel` as a package-private class inside `src/mechanist/LegacyPanelContext.java`, extending `LegacyPanelBridgeBase`.

The file explicitly describes itself as a temporary compile bridge for post-shard GamePanel retirement. It exists to preserve the old context surface while extracted subsystems are retargeted toward narrower interfaces/managers.

Assessment: this is the largest architectural warning in the current project. It is not simply a missing file. It is an intentional transitional compatibility bridge. Future work should not re-inflate it. Any pass touching it should either:

1. Retarget a dependency to a narrower authority/context, or
2. Preserve compatibility while documenting why extraction is not yet safe.

### Server entry

`src/mechanist/MechanistServerMain.java` initializes the headless server runtime, save-path namespace, server state, security core, optional host binding, status/usage text, and audit summaries. It distinguishes server state/world/slot directories from desktop single-player save directories.

Assessment: the server lane exists and has real separation mechanics, but governance still forbids claiming public multiplayer/live external server authority until architecture, package, safety, and test gates are complete.

## 7. Package and release architecture

The Maven root build targets Java 17 and defines a desktop/client shaded jar plus a headless-server shaded jar. Dependencies include Netty, LWJGL modules and platform natives, and optional Jamepad. The release-obfuscation profile exists but is explicitly separate from ordinary development packaging.

The installer packaging pipeline defines the correct launcher-managed layout:

```text
manifests/
packages/client/TheMechanist.jar
packages/server/TheMechanistServer.jar
packages/support/lib/*.jar
```

The platform runtime manifest is the package identity source of truth. It records version, platform, schema, distribution model, paths, sizes, hashes, and main classes.

Assessment: the package model is coherent. The release blockers are not conceptual; they are verification and trust-chain blockers:

- Remote artifact authentication.
- Signing or equivalent trust metadata.
- Full Windows/Linux native packaging on target OSes.
- Support-library staging, especially LWJGL/native jars.
- Mandatory Java 17 classfile scan.
- Zip/archive integrity checks.
- Honest manual-test limitations.

## 8. Semantic asset state

The staged semantic asset migration has advanced through Stage 9. The current plan states that the backend registry, Infopedia asset browser, high-error indexing, item/UI preview migration, tile descriptor migration, object/fixture/construction/editor-palette migration, direct graphical path audit, portrait/entity partitioning, typed missing-art fallbacks, and deferred legacy boundary recording are in place.

Current next target: **Stage 10 — mod and art-pack semantic registry extension**.

Important current asset rules:

- `ROOT_SRC_assets/` is source material. Do not edit it in place.
- Runtime-ready assets belong under the consuming package tree, usually `PACKAGE_client/assets/` or launcher resources.
- New gameplay/UI graphical references should use exact 8-character semantic asset IDs.
- InfoPedia is the in-game audit surface for registry-backed assets.
- Documentation manifests are not a substitute for physical runtime placement.

Assessment: the asset system is one of the strongest parts of the current architecture. It should not regress into raw path usage. Stage 10 should be data-driven and package-integrity-driven, not live mod hot-loading.

## 9. Itemized evaluation by project area

### 9.1 Governance and documentation

Status: strong, but with minor stale references.

Strengths:

- Clear authority hierarchy.
- Active master plan, standards, governance, development history, milestone index, and ordered milestones.
- Anti-sprawl rules prevent uncontrolled note growth.
- Current checkpoint and gates are explicit.

Risks:

- Governance still includes older `docs/` wording that should be updated to `ROOT_docs/`.
- `NEW_DEVELOPMENT_CONVERSATION_BRIEFING.md` had accumulated stale recovery/regression material and should remain short after this pass.
- Mermaid generated ledger paths named in the master map should be verified/regenerated.
- This new assessment makes `REPOSITORY_FILE_MANIFEST.tsv` stale until regenerated.

### 9.2 Repository/file index

Status: useful and recently repaired.

Strengths:

- Manifest covers text, binary, source, assets, jars, package files, tools, and docs.
- File kinds distinguish client runtime assets, package files, protected source assets, tooling, source code, and documentation.

Risks:

- The manifest is easy to stale-date because it records hashes and sizes. Any repo file change requires regeneration.
- The manifest must remain discovery-only and must not become a runtime composition layer.

### 9.3 Core code architecture

Status: broad and authority-oriented, but still carrying transition debt.

Strengths:

- Many systems have named `Authority`, `Framework`, `Runtime`, `Bridge`, `Manager`, and `Smoke` classes.
- Mermaid map has zero unpositioned modules.
- Client and server entrypoints delegate instead of holding all behavior inline.

Risks:

- `LegacyPanelContext` and `LegacyPanelBridgeBase` are transitional compile bridges.
- Some central frameworks are oversized and should not be casually expanded.
- Future changes must avoid re-growing a new monolith under different names.

### 9.4 UI, input, and player readability

Status: most active and currently healthiest implementation lane.

Strengths:

- Gate 3 has substantial history-backed work.
- Player-facing smokes exist.
- Movement planning, Examine, prompt, transfer, pet, construction, trade, base, roster, medical, and evidence readability have visible support.

Risks:

- UI render is the largest Mermaid zone.
- Regression risk is high because many surfaces intersect the compatibility bridge.
- Future work should migrate exact callsites onto `PlayerFacingText` rather than adding more helper sprawl.

### 9.5 World generation and districts

Status: mapped and planned, not current active expansion target.

Strengths:

- Worldgen has a clear zone and a dedicated milestone.
- Room, road, frontage, transit, zone context, and room manifest authorities exist.

Risks:

- `WorldRuntimeGenerationFramework` is extremely large.
- Master plan explicitly says not to start broad Milestone 09 expansion first.
- Worldgen depends on assets, readability, provenance, construction, vehicles, schemes, body/medical, world events, and persistence.

### 9.6 Combat, NPCs, and simulation

Status: present and substantial.

Strengths:

- Combat simulation zone includes world simulation, population, faction services, movement, combat runtime, command parity, interpolation, NPC turn budget, and turn manager.
- Simulation tier doctrine exists to prevent full-tile background simulation everywhere.

Risks:

- Must not bypass server/world authority.
- Any expansion should include diagnostic/audit visibility and save ownership.

### 9.7 Inventory, economy, trade, and persistence

Status: rich but high coupling potential.

Strengths:

- Item economy, container trade, storage, transaction guard, stock trackers, semantic asset integration, and character save management are mapped.
- Gate 3 added transfer, market, base storage, trade, and inventory readability.

Risks:

- Item provenance and broader economy are milestone-dependent and should not be overclaimed.
- Persistence ownership must be explicit before durable save/load claims.

### 9.8 Production, fixtures, and machines

Status: large and important.

Strengths:

- `ProductionAuthorityFramework`, machine queue, fixture authorities, and fixture interaction registry are mapped.
- Recent UI guidance improves machine/production readability.

Risks:

- `ProductionAuthorityFramework` is oversized.
- Production quality/provenance systems belong under Milestone 03 and depend on readable inspection and persistence boundaries.

### 9.9 Server, networking, and authority

Status: foundational but not opened for public claims.

Strengths:

- Headless server entrypoint exists.
- Server save namespace, security core, host binding, network policy, admin/maintenance, and recovery systems are mapped.

Risks:

- Governance forbids public multiplayer/live external mod loading/real remote authority until explicit gates exist.
- Any network work must be narrow, testable, and security-aware.

### 9.10 Packaging, installer, and launcher

Status: coherent but incomplete for release.

Strengths:

- Thin launcher is the correct owner for package acquisition, verification, update, rollback, diagnostics, and launch.
- Local package seed builder provides offline verification.
- Manifest schema/platform compatibility checks exist.

Risks:

- Remote acquisition authentication and trust metadata are still open.
- Native packaging must be verified on target OSes.
- The client must never opportunistically download support libraries at game launch.

### 9.11 Assets and audio

Status: strong staged migration.

Strengths:

- Runtime assets and source assets are separated.
- Semantic registry and Infopedia audit surface exist.
- Music/sound assets are present in runtime package locations.
- Direct path debt is quantified and baselined.

Risks:

- Stage 10 must not become uncontrolled live mod loading.
- New art must not be left as source-only or pointer-only documentation.

### 9.12 Modding and external content

Status: examples and API material exist; live external loading remains closed.

Strengths:

- `PACKAGE_client/modding/` contains API reference and examples.
- Stage 10 is correctly pointed toward controlled registry extension.

Risks:

- Do not confuse example mods/API docs with a safe live mod-loading release state.
- External pack entries must pass semantic registry audits and package integrity checks before session/world initialization.

### 9.13 Diagnostics, smoke tests, and audits

Status: unusually strong for a prototype-era project.

Strengths:

- Gate 3 smokes and diagnostics are central to recent history.
- Mermaid positioning is an explicit release discipline.
- Performance, debug, retarget, sector/zone audit, and text smoke modules exist.

Risks:

- This connector assessment did not run compile, smoke, jar rebuild, classfile scan, or zip integrity checks.
- Generated Mermaid ledgers should be committed or regenerated where standards expect them.

## 10. Current risk register

| Risk | Severity | Description | Recommended containment |
|---|---|---|---|
| R1 | High | Remote package acquisition/authentication/signing remains open. | Finish Gate 2 trust/auth policy before release claims. |
| R2 | High | `GamePanel` exists as a compatibility bridge after monolith retirement. | Retarget touched dependencies to narrower authorities; do not re-inflate bridge. |
| R3 | High | Several modules are oversized, especially worldgen and production. | Use narrow pass scopes and extract only when touching exact seams. |
| R4 | Medium | Some docs contain stale path wording or stale handoff material. | Correct `docs/` to `ROOT_docs/`; keep briefing short. |
| R5 | Medium | Mermaid generated ledger paths named in master map were not fetchable through the contents API during this pass. | Verify committed paths or regenerate map/ledgers. |
| R6 | Medium | File manifest becomes stale after this document and briefing update. | Run `ROOT_tools/update-repository-file-manifest.ps1` after file changes. |
| R7 | Medium | Asset source/runtime boundary can regress if new art is only documented, not promoted. | Keep using semantic IDs and consuming package placement. |
| R8 | Medium | Public multiplayer/mod loading could be accidentally overclaimed. | Keep them closed until explicit architecture, safety, package, and test gates exist. |
| R9 | Medium | Connector assessment did not execute local build/test commands. | Next implementation pass must run Java 17 compile/smokes/classfile scan as appropriate. |

## 11. Recommended next work order

### First: finish this documentation handoff cleanup

1. Keep this assessment in `ROOT_docs/` as the single user-ordered onboarding snapshot.
2. Keep `NEW_DEVELOPMENT_CONVERSATION_BRIEFING.md` short and pointed at this assessment.
3. Append a concise entry to `ROOT_docs/DEVELOPMENT_HISTORY.md` for this documentation pass.
4. Regenerate `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` with `ROOT_tools/update-repository-file-manifest.ps1` after all file changes.
5. Verify or regenerate `ROOT_docs/functionmap/generated/*` ledgers if they are intended to be committed.

### Second: continue Gate 3 / Milestone 02 only through exact UI callsites

Best next implementation slice: select one player-facing surface still bypassing `PlayerFacingText`, migrate that exact surface, run `Gate3PlayerFacingTextSmokeSuite`, and record the result.

Do not use Gate 3 as a license for broad worldgen, economy, vehicle, body, or quest expansion.

### Third: close Gate 2 release blockers

Best package slice: define and test publish-safe remote artifact authentication and trust metadata without hard-coded secrets. Then verify Windows/Linux native packaging on target OSes.

### Fourth: advance Stage 10 semantic asset registry extension

Best asset slice: define controlled art-pack/mod registry extension data format, duplicate-ID conflict rules, path validation, and package-time integrity checks. Keep loading before world/session initialization.

### Fifth: use Milestone 10 as a persistence audit before broad expansion

Before major worldgen/provenance/economy/vehicle/body/scheme expansion, confirm what state belongs to world files, character saves, profiles/settings, launcher cache/manifests, and editor/content definitions.

## 12. New conversation briefing instructions

A new development conversation should begin with this sequence:

1. Read `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`.
2. Read `ROOT_docs/STANDARDS_AND_PRACTICES.md`.
3. Read `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`.
4. Read `ROOT_docs/DEVELOPMENT_HISTORY.md`.
5. Read `ROOT_docs/MILESTONE_INDEX.md`.
6. Read `ROOT_docs/LEGACY_MILESTONE_SOURCE_MAP.md`.
7. Read this file: `ROOT_docs/PROJECT_STATE_ONBOARDING_ASSESSMENT.md`.
8. If the work touches assets, read `ROOT_docs/STAGED_ASSET_INTEGRATION_PLAN.md`.
9. If the work touches package/installer/launcher, read `PACKAGE_installer/PACKAGING_PIPELINE.md` and `PACKAGE_launcher/README_LAUNCHER.md`.
10. If the work touches code architecture, read `ROOT_docs/functionmap/Mermaid_Code_Map_Master.md` and verify/regenerate generated ledgers as required by standards.

Modification rules for future sessions:

- After every completed code, package, asset, or documentation pass, update `ROOT_docs/DEVELOPMENT_HISTORY.md` with what changed, why it matters, what verification ran, and what was not tested.
- If a file is added, removed, renamed, moved, or replaced, regenerate `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` with `ROOT_tools/update-repository-file-manifest.ps1`.
- If a Java module is added, moved, renamed, repaired after a generated error, or remapped, update the Mermaid map and position ledger before claiming the pass complete.
- If the current checkpoint, gate, or milestone order changes, update `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`.
- If a durable implementation rule changes, update `ROOT_docs/STANDARDS_AND_PRACTICES.md`.
- If a long-term doctrine/boundary changes, update `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`.
- Do not create new audit notes, addenda, temporary Markdown files, or duplicate roadmap documents unless the user explicitly orders a separate artifact.
- State verification honestly. A connector-only documentation pass is not a compile, not a smoke test, not a jar rebuild, not a classfile scan, and not a package integrity check.

## 13. Assessment limitations

This assessment did not run code locally. It did not compile the repository, run smokes, rebuild jars, inspect binary files, launch the client, launch the server, run package scripts, regenerate the repository manifest, or regenerate the Mermaid ledgers.

The assessment is based on repository documents, the repository file manifest, the Mermaid master map, package pipeline documentation, staged asset migration documentation, development history, and selected source entrypoints. It should be treated as an onboarding map and risk register, not as proof of build or runtime correctness.

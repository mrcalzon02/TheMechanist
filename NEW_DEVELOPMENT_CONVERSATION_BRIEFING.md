# New Conversation Handoff - The Mechanist

## Current checkpoint

Active checkpoint: **Phase 4 publish-safe client containment and package/handoff hygiene**.

Primary orientation snapshot for the next conversation:

- `ROOT_docs/PROJECT_STATE_ONBOARDING_ASSESSMENT.md`

This briefing is intentionally short. It is not a second master plan, a changelog, or a substitute for the durable authority documents.

## Required reading order

Before any code, package, asset, or documentation pass, read:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md`
3. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`
4. `ROOT_docs/DEVELOPMENT_HISTORY.md`
5. `ROOT_docs/MILESTONE_INDEX.md`
6. `ROOT_docs/LEGACY_MILESTONE_SOURCE_MAP.md`
7. `ROOT_docs/PROJECT_STATE_ONBOARDING_ASSESSMENT.md`

Additional required reading by work type:

- Asset-loader, image, tile, portrait, item-icon, machine-art, fixture-art, or Infopedia asset work: read `ROOT_docs/STAGED_ASSET_INTEGRATION_PLAN.md`.
- Package, launcher, installer, runtime-manifest, or support-library work: read `PACKAGE_installer/PACKAGING_PIPELINE.md` and `PACKAGE_launcher/README_LAUNCHER.md`.
- Java module mapping, code architecture, generated error repair, or subsystem remap work: read `ROOT_docs/functionmap/Mermaid_Code_Map_Master.md` and verify or regenerate the generated Mermaid ledgers required by standards.

## Active workspace boundaries

- `ROOT_docs/` is the durable development-document root.
- `ROOT_SRC_assets/` is protected source material. Do not modify source assets in place.
- Runtime-ready assets belong in the consuming package tree, usually `PACKAGE_client/assets/` or `PACKAGE_launcher/java/src/main/resources/assets/`.
- Current delivery path remains `PACKAGE_installer -> PACKAGE_launcher -> PACKAGE_client -> packaged server payload`.
- The launcher owns acquisition, verification, update, rollback, diagnostics, and launch handoff.
- The client must not opportunistically download support libraries at game launch.
- `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` is a discovery index only, not a runtime composition layer or architecture authority.
- Do not create pointer-only docs, manifest-only maps, temporary audit notes, or duplicate roadmap files unless the user explicitly orders a separate artifact.

## Current state summary

- Gate 1 documentation and repository-layout hygiene repaired the active map to the implemented `ROOT_*` and `PACKAGE_*` structure.
- Gate 2 launcher/package identity has local manifest verification, local package seed acquisition, support-library hash checks, compatibility checks, and rollback repair. Remote artifact authentication, signing/trust metadata, and full native package verification remain open.
- Gate 3 / Milestone 02 is the strongest recent implementation lane: player-facing text containment, movement planning, Examine, Infopedia, prompt lines, transfer guidance, pet care, market legality, base storage, faction roster, medical status, and quest evidence readability are recorded in development history.
- Gate 4 has a local package seed builder path, but full release packaging still requires Java 17 compile, smokes, jar rebuild, classfile scan, platform-native package verification, and integrity checks.
- `GamePanel.java` is intentionally absent. The active transitional compatibility surface is `GamePanel` inside `src/mechanist/LegacyPanelContext.java`, extending `LegacyPanelBridgeBase`. Do not re-inflate it; retarget touched dependencies to narrower authorities when safe.
- The Mermaid master map reports 391 mapped Java modules, 0 unpositioned modules, and 11 oversized mapped modules. The generated ledger paths named by the map should be verified or regenerated before claiming future module-map completion.
- The semantic asset migration is complete through Stage 9. The next asset-system target is Stage 10: controlled mod/art-pack semantic registry extension.

## Required update discipline

After every completed code, package, asset, or documentation pass:

- Update `ROOT_docs/DEVELOPMENT_HISTORY.md` with what changed, why it matters, what verification ran, and what was not tested.
- If any file is added, removed, renamed, moved, or replaced, regenerate `ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv` with `ROOT_tools/update-repository-file-manifest.ps1`.
- If a Java module is added, moved, renamed, repaired after generated errors, or remapped, update the Mermaid map and position ledger before claiming the pass complete.
- If the active checkpoint, gate, or milestone order changes, update `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`.
- If a durable implementation rule changes, update `ROOT_docs/STANDARDS_AND_PRACTICES.md`.
- If a long-term doctrine or architecture boundary changes, update `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`.
- State verification honestly. A connector-only documentation pass is not a compile, not a smoke test, not a jar rebuild, not a classfile scan, and not a package integrity check.


* **Menu Encapsulation:** All game interfaces must be contained within the internal engine context. We need to audit and eliminate all external menu calls that break user flow or trigger compatibility layer errors.

### 2. Primary Navigation & World Management

* **Refinement of Game Initialization Flow:** The current "New Game" implementation is an unoptimized placeholder. The flow must be re-architected into a formal **World Management** suite:
1. **World Selection:** Choose from existing save data or trigger a new world generation pipeline.
2. **Character Initialization:** Transition from world management to the character creator.
3. **World Entry:** Instantiate the game world only after character confirmation.


* **Feature Parity:** All legacy "dead-end" editor links must be resolved to their intended interactive menus or removed until the editor modules are fully integrated.

### 3. Gameplay Mechanics & Viewport Overlays

* **Viewport & UI Overlay Cleanup:** * **HUD/UI Positioning:** Resolve the current splash screen/overlay obstruction issue; the current implementation consumes ~66% of the screen, impeding core visibility during gameplay.
* **Camera Controls:** Restore functional zoom-level logic for the main play area.
* **Interaction Feedback:** Repair the "Line Draw to Location" visual indicator for Look/Interact commands.


* **Movement Planning System:** We need to reinstate the previous iteration of the pathfinding/movement prediction system.
* **Workflow:** User selects a movement mode (Walk/Sneak) $\rightarrow$ Trigger movement planning mode $\rightarrow$ User confirms target tile/vector via click or hotkey.



---

### Action Items for Team Review

* **Audit:** Conduct a full sweep of the `UI_Manager` class to identify dangling references resulting from the recent decomposition.
* **Regression Testing:** Prioritize the movement prediction logic, as this is a known working system that requires integration with the updated viewport.

---

To ensure the development team (and your internal documentation) recognizes that these are **regressions** rather than entirely new feature requests, I have updated the report. This framing clarifies that the core logic exists within your codebase but has been decoupled during the `GamePanel.java` refactoring ("Shard Mining").

---

## Technical Debt & Regression Analysis: System Reconnection

Following the major architectural decomposition and refactoring of `GamePanel.java`, we are currently experiencing a widespread "feature-loss" event. While the core logic for these systems remains present within our codebase, they are currently siloed in fragmented shards and remain disconnected from the primary execution path.

**Our immediate goal is to reclaim and re-integrate these pre-existing, functional subsystems.**

### 1. Reclaiming Functional Subsystems

The following features were fully operational prior to the recent structural refactoring. We must trace these from their current decomposed state and re-bind them to the core loop:

* **Environmental Perception:** Re-establish the previously functional illumination, light-blocking (occlusion), and view-range calculations.
* **Sensory Simulation:** Re-integrate the established hearing-limitation and AI-awareness logic that was decoupled during the reorganization of the player-perception engine.
* **Movement & Pathfinding:** Re-bind the movement prediction, "Move/Sneak" planning, and click-to-move vectors that are currently dormant within the codebase.

### 2. Asset & Logic Re-Alignment

We are currently experiencing a mismatch between our intended design patterns and the current state of our assets due to the system fragmentation.

* **Diegetic Asset Mapping:** The current reliance on generic bulkheads is a regression. We need to point the generation engine back to our specific "Hive" asset library, which was previously mapped but lost connection during the shard migration.


---

### Integration Roadmap

We are not building these features from scratch; we are performing **System Recovery**.

1. **Audit:** Identify the specific "shard" files containing the dormant lighting, sensory, and movement logic.
2. **Re-binding:** Inject these fragments back into the primary `GamePanel` execution flow.
3. **Sanity Check:** Ensure that the assets (specifically Hive interior vs. maintenance exterior) are correctly resolving to their intended class definitions within the now-fragmented directory.

---

### Status Summary

| Feature | Current State | Required Action |
| --- | --- | --- |
| **Lighting/Visibility** | Disconnected | Re-link logic shards to the primary render cycle. |
| **Movement Prediction** | Dormant | Re-bind input handlers to the existing prediction module. |
| **Asset Mapping** | Mismatched | Update asset references to point to correct diegetic paths. |
| **Faction Gen** | Unlinked | Restore call-sequence from generation engine to faction database. |

---
## Procedural Generation & Population Logistics: Remediation Scope

To finalize the restoration of our architecture, we must address the logic governing zone generation and population dynamics. This is a transition from a generic, static environment to a dynamic, faction-driven ecosystem. Like the previous tasks, these systems are essentially "re-connections" of logic that were previously abstracted or are currently failing to call their assigned data packages.

---

### 1. Architectural Scaling: The "Age-Tier" Asset Library

We are moving away from monolithic room templates toward a modular, "Technology Age" (AA) scaling system. This requires a refactor of our room-generation stamps to support standardized sizing across all factions.

* **Standardized Stamp Library:** Develop a library of small, medium, and large variants for all core functional rooms (e.g., Barracks, Kitchens, Creches).
* **Faction-Specific Implementation:** Ensure the generation engine calls the appropriate faction-tier asset library rather than generic templates. Each faction will require its own "Age" logic for its specific room aesthetic.

### 2. Population Dynamics & Logistics Hook

We need to finalize the integration of the "Beds = Capacity" logic. This hooks population growth directly to physical infrastructure.

* **Capacity Constraint Logic:** The faction population cap must be dynamically calculated based on the total active, un-damaged bed count.
* **Growth/Import Mechanisms:**
* **Natural Growth:** Linked to the existence and functionality of **Children’s Creches**.
* **Reinforcement Pulls:** Linked to external import events; the system must query current bed capacity to determine if incoming reinforcements can be safely "housed."


* **System Integration:** These population hooks must be re-bound to the faction database to prevent desync between actual personnel and structural capacity.

### 3. Rendering Pipeline Correction

We are currently defaulting to fallback typeface/placeholders for our UI assets. This is a configuration error within the asset pipeline.

* **Rendering Audit:** The rendering engine must be re-configured to prioritize the **Client Package Art** directory over the system's global fallback.
* **Asset Resolution:** We need to ensure that the rendering manifest correctly maps the visual assets for the current faction/technology age to the draw call.

---

### Implementation Roadmap

| Focus Area | Task | Dependency |
| --- | --- | --- |
| **Zone Generation** | Implement AA-Tier Room Variants | Faction Asset Database |
| **Population Logic** | Link Bed Count to Population Cap | Faction Database & Room Registry |
| **Rendering** | Overhaul Asset Mapping/Prioritization | Client Package/Resource Loader |

---
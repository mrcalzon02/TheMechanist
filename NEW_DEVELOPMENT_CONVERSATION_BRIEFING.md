# New Conversation Handoff - The Mechanist

## Current Checkpoint

The active checkpoint is Phase 4 publish-safe client containment and package/handoff hygiene, with an immediate documentation repair focus: make the written project map match the implemented `ROOT_*` and `PACKAGE_*` structure.

Before any code, package, or asset pass, read:

1. `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`
2. `ROOT_docs/STANDARDS_AND_PRACTICES.md`
3. `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`
4. `ROOT_docs/DEVELOPMENT_HISTORY.md`
5. `ROOT_docs/MILESTONE_INDEX.md`
6. `ROOT_docs/LEGACY_MILESTONE_SOURCE_MAP.md`

## Active Workspace Boundaries

- Keep durable planning in `ROOT_docs/`, preferably in the master plan, standards, development history, governance, or ordered milestone sequence.
- Treat `ROOT_SRC_assets/` as protected source material. Do not modify those files in place.
- Put transformed, renamed, compressed, cleared, or runtime-ready assets into the consuming package tree, usually `PACKAGE_client/assets/` or `PACKAGE_launcher/java/src/main/resources/assets/`.
- Keep client launch material under `PACKAGE_client/`, launcher bootstrap work under `PACKAGE_launcher/`, installer work under `PACKAGE_installer/`, server launch/package material under `PACKAGE_client/server/`, and developer tooling under `ROOT_tools/` or `scripts/`.
- Do not create pointer-only docs, manifest-only maps, or placeholder README layers to stand in for moving files into the correct architecture.

## Recovery package targets 

To professionalize your documentation, I have refactored your notes into a structured **System Technical Debt & Refactoring Report**. This language is better suited for a Jira ticket, a GitHub issue, or a developer sync meeting.

---

## Technical Debt & UI/UX Remediation Roadmap

Following the recent architectural decomposition, we have identified significant regression and structural fragmentation across our UI/UX layer. The following areas require immediate refactoring to align with our intended design patterns.

### 1. Boot Sequence & Initialization

* **Splash Screen Restoration:** The current boot animation has been lost during system migration. We need to implement a native sequential boot sequence.
* **Requirements:** Top-aligned animated logo, center-aligned typewriter-style narrative text, and a persistent loading progress bar at the base of the viewport.


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
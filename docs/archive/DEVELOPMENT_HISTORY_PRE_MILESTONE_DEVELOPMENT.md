## Gate 1 - Documentation and Repository Hygiene

Completed a focused Gate 1 hygiene pass for the publish-safe checkpoint. Verified that `MASTER_DEVELOPMENT_PLAN.md` remains the concise roadmap and gate authority, `MILESTONE_INDEX.md` lists the ordered `00` through `10` milestone sequence, and `LEGACY_MILESTONE_SOURCE_MAP.md` preserves older topical milestone files as source/archive material rather than active implementation entry points.

The root handoff briefing was reduced from stale 0.9.10kc package notes to the current Phase 4 gate handoff, with Gate 2 identified as the next narrow pass. Removed the stale root `RUN_THE_MECHANIST_WINDOWS_VERBOSE.bat` alias because client launch commands now belong under `client/launchers/` and the alias pointed at a non-existent root launcher.

Verification: repository status was clean before the pass; no untracked files were present; README remained user-facing; ordered milestone files `00` through `10` were present; no compile was run because this was documentation and repository hygiene only.

## 0.9.10jv — Semantic Asset High-Error Indexing

0.9.10jv advances the staged semantic asset program through Stage 3. The Semantic Asset Registry now indexes the high-error art families most likely to produce wrong previews: domestic water barrels, water dispensers, supply shelves, cots, beds, domestic counters/tables, melee weapons, ranged firearms, heavy weapons, newspapers, armor/clothing icons, roads, sidewalks, industrial/sewer/maintenance corridors, walls, corpse/decay markers, machinery, and shop/supply fixtures. The registry expanded to 277 rows while preserving unique IDs and unique graphical paths.

A focused reconciliation crosswalk was added at `assets/indexes/high_error_asset_reconciliation.tsv` to map failure-prone legacy terms such as water barrel, supply shelf, sleeping cot, scrap knife, bolter, heavy bolter, scavenger rags, corpse tile, maintenance corridor, and emergency machines to their intended semantic IDs before live preview migration begins. `SemanticAssetHighErrorReconciliationSmoke` verifies coverage thresholds, exact canonical IDs, path uniqueness, search behavior, and crosswalk references. No renderer or inventory preview route was migrated in this pass; Stage 4 remains the controlled item/UI preview migration.

Verification: Java source and the new smoke compiled with `javac --release 17`. Semantic registry/image/Infopedia/high-error smokes passed. The carried-forward client/server jars remained Java 17 classfile clean under the release gate. Zip integrity passed.

## 0.9.10iq — Sector Audit Density / Road Separation Repair

0.9.10iq converts the second Windows/Linux visual test findings into a targeted local repair archive. The Sector Audit right rail F9522 is now button-only; the redundant explanatory copy was removed, the command buttons were tightened, and the BACK action moved into the bottom of the command rail so the audit surface can reclaim unused header space from F8887. The audit viewport now uses a smaller audit-specific font/tile cell path and a tighter report font so the map reads as pulled back rather than zoomed into oversized text.

Road and sidewalk separation was hardened across generation, descriptor compilation, and renderer art selection. Sidewalks no longer promote into road lanes when adjacent to opposite road tiles. Road intersections count only true road-lane continuity. Street painting, road-adjacent civic structures, and road-frontage fixtures now refuse the full room rectangle instead of relying only on room interior IDs, preventing roads, sidewalks, and frontage fixtures from landing inside room envelopes.

Void tiles now render as actual black void in the tile-icon renderer instead of borrowing a floor-like placeholder. Neutral Civilian Floor lighting was raised to a maintained civic profile with denser, brighter room and corridor fixtures so the sector audit is no longer dominated by blackness.

Verification: Java 17 source compile passed. `SectorAuditDensityRoadSeparationSmoke` verified that sidewalks remain sidewalks between road lanes, normalization performs zero sidewalk road promotions, void tiles compile as `void_space`, a generated 222x154 Neutral Civilian Floor has no roads/sidewalks inside room rectangles, and the test slice generated 82 light sources. `SimulationEditorSuiteSmoke` passed. `EulaAndGameplayConsoleSmoke` passed under Xvfb. Jar rebuild and zip integrity passed for the delivery archive.

## 0.9.10ip — Windows/Linux UI Containment Repair and Room Editor Visual Scaffold

0.9.10ip converts the first Windows/Linux visual test findings into a versioned local repair archive rather than waiting for a full retest cycle. The EULA overlay now wraps by actual rendered pixel width through `GuiLayoutApi.wrapText(...)`, aligns its buttons to the same wide panel geometry used for drawing, and avoids rough character-count wrapping that could let legal text clip off the right side on Windows font metrics.

The main menu splash stack was lifted and mildly tightened so image frame I2182 consumes less vertical room before the route frame. F5137 now reserves a discrete footer band for EXIT instead of allowing the centered exit button to collide with the Multiplayer row, and the route frame keeps a taller minimum height at common 16:9 test resolutions. The Editor / Mod Packaging surface F7094 now places B001 and B002 in a lower action bay and wraps descriptive copy inside a text area that does not run beneath the buttons.

The Room Editor gained a first-pass visual layout editor scaffold. The right side now combines the property table with a preview canvas that reads room width, height, floor material, oxygen seal, security terminal, and placement-node count from the selected room record. A tabbed palette was added for Items, Entity Spawners, Floor Tiles, Wall Tiles, and Objects. The palette is intentionally read-only in this repair pass; it establishes the visual workbench and category parsing before drag/drop mutation and stamp serialization are attached.

Verification: Java 17 targeted source compile passed for the repaired client/editor/EULA classes. `SimulationEditorSuiteSmoke` passed and now verifies the editor audit advertises the room visual layout scaffold and five palette tabs. The patched class files were injected back into both desktop and server jars to keep the local archive internally current.

## 0.9.10il — Windows Direct Launch Repair / Visible Diagnostics

0.9.10il pivots Windows testing away from the native installer path and back to a direct extracted-zip launch path. `RUN_THE_MECHANIST_WINDOWS.bat` now performs visible diagnostics instead of launching `java -jar` and immediately closing. It checks for `TheMechanist.jar`, searches for Java under a bundled `runtime\bin`, `JAVA_HOME`, and `PATH`, runs `mechanist.WindowsLaunchHealthCheck`, writes `%LOCALAPPDATA%\TheMechanist\logs\launch-client.log`, and pauses with the log on failure. `RUN_MECHANIST_SERVER_WINDOWS.bat` received the same visible Java/log handling. `WINDOWS_QUICK_START.md` was added so Windows testing can focus on getting the current jar to open before spending more time on native packaging.

## 0.9.10ig — Doom Depth Fog / Java2D Acceleration Surface Pass

## 0.9.10ik — Windows Installer Repair / Desktop Shortcut and Portable App-Image Path

0.9.10ik repairs the native packaging workflow for Windows-first testing. The Windows packaging path now produces a portable app-image ZIP with a directly runnable `The Mechanist.exe`, plus EXE and MSI installers when WiX Toolset 3.x is present. The EXE/MSI path requests install-directory selection, Start Menu integration, a desktop shortcut, a shortcut prompt, a stable upgrade UUID, and the bundled `.ico` icon. A double-clickable `build-windows-installers.cmd` wrapper was added so local Windows tests do not fail silently when PowerShell execution policy or missing tools are involved. The Linux packaging script also now emits a portable app-image tarball beside DEB/RPM outputs.


0.9.10ig integrates the next experimental first-person rendering pass without changing the default locked state of `doom` mode. The first-person raycaster now has a selectable fog mode in the QoL menu: linear perpendicular Z-depth fog for fish-eye-resistant wall columns, and radial Euclidean fog for a spherical visibility falloff. `DoomDepthFog` owns the color interpolation table and exposes both `applyFog(Color, distance, useRadial)` and low-allocation ARGB helpers. Wall columns now fog after light decay and side darkening; billboard sprites fog from their projected depth or radial camera distance. The status line reports the active fog mode.

The pass also hardens the Java2D rendering path. Client runtime defaults now prefer OS acceleration, Windows emits Direct3D plus VRAM surface flags, Linux emits OpenGL, and macOS emits Metal plus OpenGL fallback through the existing JVM runtime profile authority. The normal-mode lightmap renderer now uses a `VolatileImage` when a graphics device configuration is available, validates the surface each frame, handles lost contents, and falls back to `BufferedImage` in headless or incompatible environments. Shared visual hinting now includes a GPU-friendly hint profile used by presentation and lightmap passes.

Verification: Java 17 source compile passed. `DoomFogAccelerationSmoke` verified linear/radial fog selection, fog interpolation, Java2D platform flags, and lightmap volatile/fallback surface status. Continuous movement, doom viewport, visual HUD, packaging/storage, hot-restart handshake, sandbox/security, secure server, multiplayer, E2EE, network optimization, editor, icon, EULA, modding API, crash de-obfuscator, and obfuscation smoke tests were rerun. Server status and a `--host-once` fallback server run succeeded. Jar rebuild and zip integrity passed.

## 0.9.10gn — Save Efficiency Catalog / Slot-World Definition Separation

0.9.10gn begins the user-directed save-efficiency pass by cataloging what belongs in the save slot versus the generated world file. The new `SaveEfficiencyAuthority` reports key counts, approximate payload size, namespace counts, and embedded world-definition-key counts for slot saves and `.mechworld` definitions. The host/admin console exposes `/save_catalog` and `/persistence_catalog` for a compact runtime persistence audit.

New `.mechsave` writes no longer embed the full `worlddef.*` generated-world ledger. Save slots now declare `save.schema=slot-v2-world-reference`, `save.worldId`, `save.worldSeed`, `save.worldFile`, and `save.worldDefinitionEmbedded=false`, then carry run state, player/account/inventory/equipment state, current mutable slice state, faction/logistics/bank/news/base ledgers, and other resume-critical runtime state. `.mechworld` remains the owner of generated hive-world definition data: sector/zone names, zone history, faction epochs, facilities, production, stock movement, conflict loss, item materialization, labor assignment, setup, and world-generation progress. Older saves with embedded `worlddef.*` remain readable through existing fallback paths.

Verification: Java 17 source compile passed. `SaveEfficiencySmoke` created a generated world, wrote a single-player save, verified the `.mechsave` contains zero embedded `worlddef.*` keys, verified the slot references `saves/singleplayer/worlds/*.mechworld`, verified the world file retains `worlddef.*` ledgers plus catalog metadata, and verified `/save_catalog` reports `slotWorldDefKeys=0`. Jar rebuild, server jar rebuild, server status run, Xvfb desktop startup, and zip integrity were completed for the delivery archive.

## 0.9.10gk — Turn Mode and Long-Action Registry Hardening

0.9.10gk continues the user-directed launcher → client → internal-server authority line after the command-request conversion. The pass keeps the shared legacy `World` behind the single-writer `AuthoritativeWorldRuntime` and strengthens the `WorldTurnManager`/`PlayerActionRegistry` layer so long actions are now a usable server-side gate rather than only a future-shaped scaffold.

`WorldTurnManager` now names the continuous mode as `SLOW_CONTINUOUS` while preserving `/worldmode continuous` as the host/admin console phrasing. Strict turn-based play remains the default. Continuous ticking still routes through the authoritative world lane and remains admin-controlled by `/worldmode` plus `/setrate`.

`PlayerActionRegistry` now stores active long actions with tick progress, compact progress bars, source labels, and optional completion as a typed `WorldCommandRequest` instead of an arbitrary `Runnable`. The registry can cancel actions, report action status, and return completed command requests for the bridge to apply inside the authoritative lane. The local player is gated while an action is active: ordinary movement/combat/use commands are rejected with an action-busy report, while Wait advances the gated action by one server-authoritative step in strict turn mode. Slow continuous mode also decrements active long actions on timer ticks.

The host/admin console now includes `/action_status`, `/long_action <ticks> [name]`, `/long_move <ticks> <dx> <dy>`, and `/clear_action`. `/long_action` exercises pure progress gating; `/long_move` verifies that completion resolves through a named `MovePlayerCommand` on the authoritative world lane. The Tactical Slate now exposes the active server action line when the player is gated, and compact authoritative snapshots include active action text for audit visibility.

Public multiplayer hosting/joining, external mod loading, classpath mutation, hot restart, and true per-sector parallel mutation remain closed. This pass still deliberately keeps legacy world mutation serialized; it does not attempt to make the shared `World` object safe for independent sector writers.

Verification: Java 17 source compile passed. A targeted authoritative action-registry smoke test created a local generated world, queued `/long_action 2 valve prayer`, verified player gating, advanced the action with strict-mode Wait commands, confirmed the gate cleared after the required ticks, queued `/long_move 1 1 0`, confirmed the completion command moved the player through the authoritative lane, checked `/action_status` for the 0.9.10gk registry version, and confirmed a published authoritative snapshot. Jar rebuild, Xvfb startup, and zip integrity were completed for the delivery archive.

## 0.9.10gj — Command Request Conversion / Authoritative World Runtime Lane

0.9.10gj continues the launcher → client → internal-server path by adding explicit `WorldCommandRequest` records for the first player-action surfaces while keeping the safe single-writer `AuthoritativeWorldRuntime` beneath `SinglePlayerSectorRuntimeBridge`. Swing now submits named command requests for committed movement, wait, interaction confirmation, zone-transition confirmation, combat confirmation, selected inventory use/equip, and selected equipment unequip. The server bridge validates the local player/action gate, audits the request, then resolves the command on the authoritative world lane rather than allowing those UI entry points to directly own truth.

The authoritative lane remains the same safety boundary: normal turn advancement, sleep turn advancement, bounded deferred distant-zone maintenance, and committed command bodies publish compact `AuthoritativeWorldSnapshot` records after mutation. The runtime tracks world-version increments, records EDT-originated submissions, and rejects mutation if a world body attempts to execute on the Swing event thread. The implementation is still deliberately single-writer; the shared legacy `World` object is not yet split for safe per-sector parallel writers.

`WorldTurnManager` preserves strict turn-based mode as the default and slow continuous ticking as an admin-controlled mode. `InternalServerSessionAuthority` keeps single-player host identity aligned with the local world owner. `AdminCommandDispatcher` continues to route `/server_status`, `/worldmode strict|continuous`, `/setrate <milliseconds>`, `/add_money <amount>`, `/advance_turn <count>`, and fail-closed `/forcekick` through the same server authority boundary.

`PlayerActionRegistry` remains the long-action gate shape requested by the pasted development direction without executing arbitrary world Runnables from the registry. Active actions carry progress and completion-command tokens so later pathing, work, healing, or construction actions can resolve through known server commands. `PlayerLifecycleService` keeps unconscious/death spawn resolution behind a named lifecycle service while preserving the single-player game-over outcome.

Public multiplayer hosting/joining, external mod loading, classpath mutation, and hot restart remain closed. This is the first command-request conversion pass, not a complete command architecture: many older buttons still call legacy helper methods internally once their command enters the authoritative lane, and the client still waits synchronously for the local internal-server commit so the current desktop UX remains stable.

Verification: Java 17 source compile passed. A targeted authoritative-command smoke test created a local generated world, submitted a `WaitCommand` from the Swing event thread, confirmed the published snapshot came from the authoritative world thread, submitted a `MovePlayerCommand`, verified position/turn advancement, verified admin `/add_money`, and confirmed `/server_status` reports the 0.9.10gj authority versions. The jar was rebuilt, Xvfb startup reached boot/menu runtime without uncaught exceptions, and zip integrity passed.

## 0.9.10gg — Single-Player World Loop Migrated to Sector Manager by Default

0.9.10gg extends the temporary launcher/client/server override by routing the existing desktop single-player turn loop through the server-authoritative sector manager by default. `SinglePlayerSectorRuntimeBridge` binds the local desktop player to the active `SectorKey`, updates that binding on new-game insertion and zone transitions, and wraps `advanceTurn` plus `advanceTurnSleeping` in `SectorManager.runLocalAuthoritativeTurn(...)`.

The legacy `World` mutation body still runs synchronously on the Swing/client thread because the current world object, UI state, sensory caches, and player-facing event log are not yet safe for off-thread mutation. The sector manager now owns presence, tier state, non-overlap guarding, sector tick accounting, and sector-isolated local turn packet emission. Empty sectors still downgrade through the same manager path and release heavy pooled sector entities. Public multiplayer hosting/joining remains closed.

Verification: Java 17 source compile passed. A targeted single-player sector-runtime smoke test instantiated the client panel under Xvfb, bound a local world to the sector bridge, advanced a player turn through `advanceTurn`, confirmed the sector remained inhabited/full, confirmed the sector full-tick counter advanced, confirmed the local player received a sector-isolated turn packet, and closed the bridge cleanly. The jar was rebuilt, Xvfb startup reached the boot/menu runtime, and zip integrity passed.

## 0.9.10gf — Temporary-Priority Server-Authoritative Sector Simulation Foundation

0.9.10gf temporarily prioritizes launcher/client/server diversification over the Phase 4 UI efficiency track. The pass adds `SectorManager`, a Java 17 server-authoritative sector simulation boundary. Player presence is tracked by `SectorKey`; inhabited sectors immediately transition to full-detail authoritative ticking inside a shared sector tick pool, while empty sectors cancel full ticks, scrub heavy population entities, return them to `SectorEntityPool`, and continue only lightweight background ledger ticks.

The pass adds a sector-isolated network gateway stub. Outbound sector delta packets are broadcast only to players currently bound to the packet's sector; players in other sectors receive no internal state updates for that sector. The implementation uses Java 17 concurrency primitives: `ConcurrentHashMap`, atomic state, scheduled executors, a fixed worker tick pool, per-sector locks, and non-overlapping tick guards. The desktop client does not start live hosting or multiplayer by default; the authority is a prepared server-side seam for later launcher/server work.

Verification: Java 17 source compile passed. Targeted sector simulation smoke testing verified inhabited full-detail transition, packet isolation between sectors, empty-sector downgrade, pooled entity return/reuse, and clean shutdown. The jar was rebuilt, Xvfb startup reached the boot/menu runtime, and zip integrity passed.

## 0.9.10ge — Phase 4 Render Image Scale Cache Efficiency

0.9.10ge continues the Phase 4 efficiency track by adding `RenderImageScaleCacheAuthority`, a bounded shared cache for scaled map and entity images. High-frequency map rendering now reuses scaled tile, feature, object, NPC, and player portrait rasters at the active cell size instead of asking Java2D to rescale the same source images repeatedly every paint. The pass preserves the existing descriptor-backed tile identity and visible-frame drawing rules while reducing redundant per-frame image work.

Paint benchmark audit output now reports the scaled-image cache size alongside render profile and worst-frame metrics. No Mods/Tools, Sector Editor, broad world-generation, or later-phase gameplay systems were reopened.

## 0.9.10gc — Phase 4 Cached Text Layout and Bounded Text-Surface Efficiency

0.9.10gc continues Phase 4 with an efficiency-focused text-surface repair. A shared `TextLayoutAuthority` now performs pixel-width word wrapping with a bounded LRU cache, so high-frequency surfaces no longer recompute rough character-count wraps every frame and no longer depend on ASCII-era width guesses for proportional UI text.

The pass routes the main high-churn text surfaces through cached pixel layout: Recent Actions, Tactical Slate, general scrollable panels, clipped child-frame body text, text panels, centered frame text, Look/Auspex detail text, Combat targeting detail text, and Targeted Scavenge. This advances the Phase 4 text wrapping/clipping bucket without reopening Mods/Tools, Sector Editor Audit, or broad world-generation work.

Verification: Java 17 source compile passed; targeted text-layout smoke verified long-token splitting, cached repeat wraps, and pixel bounds; jar rebuilt; Xvfb startup smoke reached the boot/menu runtime; zip integrity passed.

## 0.9.10gb — Phase 4 Bucket List and Command-Surface Containment

0.9.10gb continues the active Phase 4 client-presentation track. The master development plan now contains a clear Phase 4 bucket list: frame ownership and command containment; text wrapping/clipping; reticle and targeting convergence; presentation/z-order; input focus and scroll conflict repair; and verification. The bucket list lives in the master plan rather than a new standalone note, preserving the four-document rule.

The pass also repairs a command-frame containment failure mode left over from older panel layouts. Command-surface buttons are now collected by semantic ownership of the active screen/panel instead of by whether their old rectangle happened to intersect the command frame. This allows oversized Build/Workbench/Trade/Interact command lists to be corralled into the command tablet before drawing, instead of letting off-frame buttons escape down the side of the client.

Ordinary launcher-facing text was also cleaned so deferred tools/modding routes no longer describe internal phase numbers to the player. Phase language remains in the durable documents and developer-only contexts.

Verification: Java 17 source compile passed. Targeted command-containment smoke testing confirmed Workbench command buttons are semantically owned by the command surface and fit inside the command frame after layout. The jar was rebuilt and zip integrity passed.

## 0.9.10ga — Phase 4 Map Viewport Transform Authority

0.9.10ga begins the substantive Phase 4 implementation pass after the Phase 3 closure. The pass focuses on input/rendering containment rather than reopening worldgen or editor tooling. Gameplay map rendering and gameplay mouse-to-world targeting now route through `MapViewportTransformAuthority`, a shared geometry authority that computes the active map frame, tile cell size, camera origin, draw origin, visible columns/rows, and point-to-tile conversion from the same data. This replaces the remaining mismatch where rendering used live font metrics and tile-size options while hit-testing still used older fixed ASCII cell estimates.

The goal is to prevent zoom, GUI scale, tile icon size, borderless presentation, downscale, or font changes from making the cursor drift away from the tile that is visibly under the mouse. The map still uses the virtual render canvas and the existing physical-to-virtual input mapper; this pass narrows the next step by making the gameplay map viewport itself a named authority instead of duplicated arithmetic.

Verification: Java 17 source compile passed after adding the authority. Targeted transform inspection confirmed the authority can map rendered map-cell centers back to the expected world tile and rejects points outside the active map draw area. The jar was rebuilt and an Xvfb startup smoke reached the boot/menu runtime. Zip integrity passed.

## 0.9.10fz — Phase 3 Closure / Phase 4 Opening Refocus

0.9.10fz is a planning and governance correction pass. It reviews the durable roadmap, marks Phase 3 spatial integration complete for scheduling, closes Phase 3.6 as the active development target, and moves the current implementation gate to the beginning of Phase 4: UI, input, rendering, and presentation containment. The Mods / Tools and Sector Editor Audit line is deliberately parked under the later Phase 15 editor/modding phase unless explicitly reopened.

The master development plan now lists Phase 3 as complete, Phase 4 as active, and the current do-next sequence as UI/input/rendering containment rather than further Sector Editor Audit expansion. Standards were updated so Phase 4 work must not keep growing the tools route, and governance now treats local audit/editor tooling as parked future editor infrastructure instead of the current workstream. README and the root briefing were shortened back toward the active player/build surface and Phase 4 resumption instructions.

Verification: documentation containment was checked after the edit and `docs/` still contains only the four durable development files. Java 17 source compile passed, the jar was rebuilt, Xvfb startup smoke reached the main menu, and zip integrity passed.

## 0.9.10fy — Phase 0 Documentation Refocus / Durable Plan Repair

0.9.10fy is a corrective governance pass, not a new gameplay feature pass. It repairs documentation drift that violated the project's own standards: pass-specific Markdown notes had accumulated in `docs/`, README had begun carrying update-log material, and the master development plan had buried the durable phase roadmap beneath recent-version notes.

The repair restores `docs/` to the four durable development files only: master development plan, standards and practices, development history, and master governance revision. The standalone pass notes for UI containment, tile descriptors, road/input/icon stabilization, zone-weight correction, exterior boundary correction, Sector Editor Audit tooling, diagnostic overlays, and reachability semantics were consolidated into the appropriate durable files and then removed from `docs/`.

The master development plan now opens with the durable phase roadmap and contains the Phase 3.6 core-systems tooling gate as the current active implementation target rather than stacking pass notes above the plan. Standards and governance were condensed back into durable rules and doctrine instead of version-by-version addenda. README was reduced to user-facing run/package information. The root new-conversation briefing remains short and points the next session back to Phase 0 hygiene and Phase 3.6 Sector Editor Audit tooling.

Verification: documentation containment check confirmed exactly four Markdown files remain in `docs/`; Java 17 source compile and jar rebuild were run after version-string cleanup; zip integrity passed.

## 0.9.10fx — Phase 3.6 Sector Audit Reachability / Generation Repair Semantics

0.9.10fx follows the master plan's Phase 3.6 instruction to use the Mods / Tools Sector Editor Audit surface as the world-generation workbench before opening additional content detail. The pass corrects the audit's room-reachability semantics: generation/audit reachability now treats doors, hatches, elevators, sewer hatches, and transition seals as layout connectors even if some of those symbols remain blocked for ordinary player movement until opened, unlocked, or bypassed. This prevents valid locked or tool-gated rooms from being falsely reported as detached geometry and stops repair logic from carving needless corridors through intact layouts.

The generation flow now runs a post-boundary reachability repair after the exterior maintenance envelope is applied, so boundary-added maintenance rooms are verified before interstitial mass conversion and population. The audit-visible `m` room glyph is also promoted to a sump fungus / mold overlay descriptor instead of falling through the legacy fallback descriptor path.

Verification: Java 17 source compile passed. A targeted generated Trash Warren sewer slice that previously reported topological room failures now generated with zero sealed-room findings after the post-boundary repair; a manual follow-up repair had zero additional work. A descriptor smoke test confirmed glyph `m` compiles as a fixture overlay with zero fallback descriptors.

## 0.9.10fw — Phase 3.6 Sector Editor Audit Diagnostic Overlays

0.9.10fw continues the Phase 3 systems pivot by making the Sector Editor Audit route actionable. The audit authority now builds an explicit findings snapshot after generated slices compile tile descriptors. It reports and positions unreachable rooms, light fixtures on true road lanes, map objects on true road lanes, fallback tile descriptors, suspicious road intersections, and missing exterior boundary layers. The audit screen adds overlay modes for findings, rooms, roads, exterior boundary layers, and descriptor failures, plus keyboard/button controls to jump between findings. This keeps worldgen debugging inside the local Mods / Tools route without opening external mod loading, save mutation, survival ticking, multiplayer, or headless-server behavior.

## 0.9.10fv — Phase 3.6 Mods / Tools Route and Sector Editor Audit Surface

0.9.10fv pivots away from more immediate worldgen micro-fixes and opens a local Mods / Tools route with a Sector Editor Audit surface. The tool generates selected zone/faction profiles in full-bright cursor-only mode, with no character body, survival loop, fog, save mutation, or ordinary turn simulation. It exposes worldgen weight band, density, dimensions, cursor glyph, room id, faction, compiled tile descriptor, room reachability, roads, sidewalks, exterior bulkheads, void, lights-on-road, NPCs, objects, and fallback descriptor counts. This gives worldgen placement failures a dedicated inspection surface before further content detail is bolted on. External mod loading and multiplayer remain closed.

## 0.9.10fu — Phase 3.6 Exterior Hivewall Boundary / Worldgen Weight Clarification

0.9.10fu continues the correction that 500-1000 is a world-generation weight/variance band rather than literal zone width and height. It also changes the outer hivewall pass from a generic cube-like map wrapper into a layered boundary: interior zone, inner maintenance bulkhead, exterior maintenance corridor, outer maintenance bulkhead, and void beyond the envelope. Corridor-adjacent bulkhead walls now compile as exterior maintenance bulkheads through tile descriptors. The old outside-corridor danger-room pockets are disabled until they can be reintroduced without violating the void boundary.

## 0.9.10ft — Phase 3.6 World-Generation Weight Profile Correction

0.9.10ft corrects the 0.9.10fs zone-scale mistake. The requested 500-1000 clamped world-generation size/variance range is now implemented as a worldgen weight band rather than literal zone width/height. Zone Size settings now map into weighted bands: Compact 500-620, Standard 600-760, Large 720-900, and Sprawling 850-1000. Actual slice dimensions are derived from those weights and remain practical gameplay dimensions. The road, input, item-icon, light placement, combat targeting, and reticle fixes from 0.9.10fs remain intact.

Verification: Java 17 source compile passed, weight-profile smoke tests confirmed dimensions are no longer raw 500+ edges, and a Standard generated-world smoke produced a practical weighted slice with compiled tile descriptors. The existing generation audit can still report unreachable-room warnings in the fallback/lattice path; that remains a deeper generation-connectivity pass rather than this weight-profile correction.

## 0.9.10fs — Phase 3.6 Road/Input/Icon/Zone Stabilization

0.9.10fs responds to live testing of 0.9.10fr. It fixes the road-atlas regression where sidewalks were counted as road-connectivity neighbors and caused ordinary road lanes to render as intersections. Road shape calculation now counts only true road-lane tiles; sidewalks remain visually street-adjacent but do not create intersections except when the sidewalk tile itself is trapped between opposite true road lanes and is deliberately promoted. Zone lighting placement now refuses true road-lane tiles and relocates to nearby sidewalks or legal floor tiles.

The mouse targeting path now uses the same virtual render-layout geometry used by drawGame, so borderless scaling and zoom no longer offset target tiles toward the upper-left of the interaction pane. Look/interact cursor movement is processed before general panel scrolling, restoring vertical Look movement. Mouse clicks can now set Look, Combat, and Interact reticles, with right-click-on-player opening Look targeting from the gameplay view. Direct ranged attacks against empty terrain/object tiles now report a high direct hit chance when in range and not line-blocked.

Inventory thumbnails now resolve through AssetPack6 item-icon semantic buckets instead of the legacy `?`/scrap fallback for every item. The world generation authority was later corrected in 0.9.10ft: 500-1000 is a weight/variance band, not a hard edge ceiling/floor for slice dimensions. Fallback room anchors now respect an interior margin instead of historical border anchors, and emergency connector validation was made less allocation-heavy so larger slices remain practical.

## 0.9.10fr — Phase 3.6 UI Containment, Equipment Transfer, and Portrait Folder Authority

0.9.10fr responds to live testing that found several gameplay panels still escaping their frame bounds after the first UI correction. Recent Actions and Tactical Slate now use compact labels plus wrapped body regions instead of allowing the label to consume the whole panel. Auspex/Look stack, interaction detail, build detail, character dossier, and survival text are bounded by clipped/wrapped child rectangles. Main gameplay and construction command buttons calculate their height and spacing from the command frame so controls do not continue below the frame edge.

This build also corrects a critical inventory failure: equipping an item now transfers an item instance from player inventory into a slot-specific equipment container, returns any replaced item to inventory, updates the slot display, and feeds an equipped armor bonus into damage reduction. Portrait import discipline is hardened again: player portraits are restricted to explicit base/baseline human buckets only, NPC portrait resolution uses exact folder matches, and the generic portrait bin is no longer a valid fallback for players or faction-specific NPCs.

## 0.9.10fq — Phase 3.6 UI Containment and Portrait Pool Discipline

0.9.10fq responds to live UI and character-creation testing. The main menu now computes a dedicated launcher route frame beneath the subtitle image so start/load/options/mod/network/infopedia buttons no longer overwrite the subtitle art. The Exit route is centered at the bottom of that route frame. Options layout now separates the tab row, the visible controls frame, and graphics dropdown popup frames; dropdown values are no longer treated as normal option buttons that get reflowed to the bottom of the menu. The first-impression splash now frame-wraps body text without a final ellipsis pass. Portrait loading now treats named folders as authority: player creation uses approved human folders or legacy baseline-human art only, and generic portrait fallback no longer pulls pets, farm beasts, mutants, cultists, servitors, or other non-human/specialty bins for the player.

Verification scope: Java 17 source compile passed after the refactor. The next live check should inspect main menu button/subtitle separation, options tab/control/dropdown frames, first-impression wrapping, and several character rerolls with imported portraits enabled.

## 0.9.10fp — Phase 3.6 Startup Borderless-Windowed Default Correction

0.9.10fp corrects the live-launch mismatch where the Options default said the game should be in the borderless display mode, but startup still constructed a decorated windowed frame first. The main entry point now delegates initial frame sizing and decoration to `WindowModeSurfaceAuthority.configureInitialFrame(...)` before `setVisible(true)`, then calls `activateInitialFrame(...)` for the exclusive fullscreen case after the frame is visible.

The default `windowMode=1` now launches as Borderless Windowed: an undecorated Swing frame sized to the primary display bounds. Windowed mode remains decorated and resolution-preset based, while Exclusive Fullscreen remains available through the same authority. Runtime Options still apply changes through the existing reconfiguration path, now sharing the same borderless sizing rule. Settings loading clamps `windowMode` to the valid 0..2 range.

Verification: Java 17 source compile passed. Startup display smoke testing under Xvfb confirmed that a fresh default options object produces an undecorated visible frame with `windowMode=1` and primary display bounds. The jar was rebuilt and archive integrity passed.

## 0.9.10fo — Phase 3.6 Narrated Intro Tail-Safety Correction

0.9.10fo responds to live restart testing of the narrated new-world insertion crawl. The prior implementation was very close, but automatic handoff from the crawl screen cut the audio tail at the final spoken word. This pass keeps the existing narration asset and typewriter-crawl design, nudges the text target from 250 seconds to 252 seconds, and adds an explicit 6.5 second audio-tail safety buffer to the automatic handoff deadline.

The intent is not line-by-line synchronization. The text still completes slightly ahead of the audio, but the screen now remains alive long enough for Java Sound startup latency, frame timing, and final waveform tail to complete. Manual skip and screen changes still stop the managed narration clip immediately. Dynamic music remains silent during the narrated crawl.

Verification: source compile passed under Java 17, the bundled WAV remains Java Sound-readable at approximately 258.624 seconds, the jar was rebuilt, and archive integrity passed.

## 0.9.10fn — Phase 3.6 Compiled Tile Descriptor Bridge / Glyph-Decompression Pass

0.9.10fn responds to the live-design conclusion that the project has reached the practical limit of pretending the map is still fundamentally an ASCII tile game. The pass introduces `TileDataCompilationAuthority` and `CompiledTileDescriptor` as a renderer-facing bridge between compact legacy glyphs and richer tile identity. `World.tiles` remains available as the current generation/input grid, but after world population the map is compiled into descriptors that carry base layer, family, road/corridor shape, deterministic variant, primary art key, underlay art key, overlay art key, and semantic tag.

The renderer now resolves visible terrain through those descriptors rather than asking a single character to carry terrain, fixture, semantic, and art-bucket meaning. Transparent marker and fixture glyphs such as bandit-den markers now compile as overlay descriptors with inferred terrain underlays, preserving alpha and preventing black-backed marker tiles. Road cells compile to explicit `road_north_south`, `road_east_west`, `road_corner`, `road_intersection`, or `road_sidewalk` variant keys. Road art is no longer rotated as if it were generic corridor art. The Look stack now exposes the composed tile key so live testing can report descriptor failures directly.

The transition, current flow, known limits, and future rules were consolidated into the durable standards/history/governance files during the 0.9.10fy documentation repair. This pass is deliberately a compatibility bridge rather than a full gameplay migration: existing systems may still read `World.tiles` for walkability, interaction, combat, and door checks, while rendering and inspection begin consuming compiled descriptors. Runtime tile mutations are safely recomposed when their source glyph differs from the cached descriptor; a later pass should add explicit invalidation around mutations that change neighboring road/corridor context without changing the tile's own glyph. This remains client/render/data-identity infrastructure and does not open traffic simulation, autonomous hauling, live mod loading, source acquisition, multiplayer networking, authoritative server ticking, process hot-swap, save migration, or broad worker-threading.

## 0.9.10fm — Phase 3.6 Road Atlas / Sidewalk Crossing Correction

0.9.10fm fixes the live road-readability failure where sidewalk/verge tiles could survive inside a perpendicular road crossing and visually bisect an east/west lane through a north/south road. The road generator now runs a bounded street-crossing normalization after all plaza-origin roads are carved: any sidewalk tile trapped between opposite road-lane neighbors is promoted back into road-lane data before road-frontage fixtures are seeded. The audit summary now reports sidewalk-to-road promotions and road-intersection candidates.

The road-art import now treats the 5x5 `roads_ns_ew_round_intersection_straight` sheet as the user-named source intended it to be: row 1 north/south roads, row 2 east/west roads, row 3 round/corner roads, row 4 intersections, row 5 sidewalks, with columns 1-5 as visual variants. The previous east/west alias incorrectly pointed at a north/south-row cell; that is corrected. The renderer now chooses ordinary sidewalk art for non-bridging sidewalks, road connector art for promoted/bridging sidewalk conditions, and intersection art when road lanes have continuity on both axes. This remains a Phase 3.6 terrain-art and generation-normalization correction only.

Verification: Java 17 source compile passed using plain `javac --release 17`; a package-local road-normalization check promoted a sidewalk trapped between opposite road lanes; a package-local art alias check loaded the N/S, E/W, corner, intersection, and sidewalk road family aliases including fifth-column variants.

## 0.9.10fl — Phase 3.6 Semantic Tile Underlay / Art-Bucket Correction

0.9.10fl fixes the live map-rendering issue where transparent semantic tile glyphs, especially `b` bandit-den / bench / product-storage markers and related junk/fixture cells, drew directly onto an empty black tile surface instead of over floor art. The map renderer now treats non-terrain glyphs as composited overlays: it infers a local underlay from nearby roads/corridors, sewer context, room faction, or fallback underhive flooring, draws that underlay first, and then preserves the transparent icon alpha over the tile. This keeps bandit-only territory markers and other transparent sprite-sheet cells visually grounded instead of black-boxed.

The pass also repairs tile-art key resolution so named aliases are checked after semantic keys and before glyph fallback. This makes wall, floor, corridor, road, and fixture alias bindings reachable rather than silently collapsing to a fallback glyph. Road art now selects from road-specific north/south, east/west, and intersection semantic images based on neighboring road cells. This remains a client art-readability correction only; no live mod loading, multiplayer, server authority, save migration, source acquisition, or broad worker-thread simulation was opened.

Verification: Java 17 source compile passed; jar rebuilt; Xvfb boot smoke reached startup and loaded tile art aliases/glyphs/semantic icons without uncaught exceptions after the renderer and tile-art lookup changes.

## 0.9.10fk — Phase 3.6 New-Game Narration / Crawl Timing Integration

0.9.10fk bolts the supplied ElevenLabs underhive introduction audio into the new-world insertion sequence without attempting brittle line-by-line synchronization. The MP3 source was converted into a bundled Java Sound-compatible WAV at `assets/sound/voice/new_world_intro_crawl_narration.wav`, registered through the existing audio runtime, and played as a managed conversation/voice clip when the intro crawl screen opens. Leaving or skipping the crawl now stops that managed narration clip immediately.

The crawl renderer now derives its typewriter character cadence from the wrapped text body and targets a complete text crawl at roughly 250 seconds, keeping the visual crawl slightly ahead of the 258.6-second narration. Automatic handoff waits for the expected narration length plus a small margin so the track is not cut short during normal playback. Dynamic music is silenced while the narrated crawl is active so the voice file remains intelligible. This remains a Phase 3.6 presentation/audio integration pass only; live mod loading, source acquisition, multiplayer networking, authoritative server ticking, process hot-swap, save migration, and broad worker-thread simulation remain closed.

Verification: Java 17 compile passed; jar rebuilt; converted WAV is Java Sound-readable PCM 44.1 kHz mono 16-bit; xvfb boot smoke reached startup without uncaught exceptions.

## 0.9.10fj — Phase 3.6 New-Game Typewriter Crawl Integration

0.9.10fj replaces the previous broad static intro-scroll text with the new underhive-perspective game introduction crawl supplied for live testing. The crawl remains centered, wraps to the cinematic panel, types each line onto the screen character-by-character, then gently raises completed lines so the next line appears beneath them. The skip control remains pinned to the lower-right crawl surface, and the renderer keeps the cloud/backdrop presentation while constraining all crawl text inside the panel. This is a presentation/content pass only; it does not open live mod loading, source acquisition, multiplayer networking, authoritative server ticking, process hot-swap, save migration, or broad worker-thread simulation.

## 0.9.10fi — Phase 3.6 Live Systemic Playability / Pending-List Implementation

0.9.10fi completes the major pending live-playability items called out after 0.9.10fh. The pass adds right-click mouse movement preview with local path ghosts, red invalid/out-of-range feedback, a travel estimate tooltip, and left-click movement execution for the feasible increment. It adds a full News archive panel for character-visible newspapers, radios, pict screens, and public filings. It advances inventory from text lists toward icon-grid stack cells with stack-count and quality-frame overlays, separates current inventory from target/floor focus, and keeps item detail in the bounded lower info pane. It redraws save/load as a true two-column manual/autosave surface with run information above the save controls. It also strengthens road/floor/corridor/wall semantic art selection and reduces command/tactical chrome to return more central space to the map. This pass remains within client/UI/input/art-readability stabilization and does not implement live mods, source acquisition, multiplayer networking, authoritative server ticking, hot restart, save migration, or broad worker-thread simulation.

## 0.9.10fh — Phase 3.6 Live Playability Triage / Menu Containment / Movement Detour Correction

0.9.10fh continues the Linux/XFCE live-playability correction stream. The pass removes the remaining main-menu blurb that was colliding with launcher buttons, enlarges the title/subtitle presentation, reduces the physical footprint of menu/status controls, improves Generate Hive World and character-creation line spacing, adds a skip control to the intro crawl, filters player-owned movement/inventory preview chatter out of Recent Actions, compacts Look mode so it no longer consumes the central play space, adds headgear to the paper-doll equipment model, begins icon-grid inventory rendering, and adds a shallow right/left detour attempt for blocked ranged movement headings. This remains a client/UI/input correction boundary: live mod loading, source acquisition, multiplayer networking, authoritative server ticking, hot restart, save migration, and broad worker-thread simulation remain closed.

## 0.9.10fg — Phase 3.6 UI Containment Debug Identifier / Menu Scale Correction

0.9.10fg responds to the live Linux/XFCE report that the physical clickable button frames, not merely their text labels, remained too large and continued escaping their owning menu frames. The pass reduces main-menu button footprints, shrinks the launcher runtime status panel, removes redundant helper/blurb text from the main menu, enlarges the title and subtitle splash art region, and changes profile/mod/multiplayer timed-alert surfaces to render above menu buttons so they visually occlude underlying controls.

The pass also adds a universal UI debug identification overlay. Buttons now stamp a small `B###` identifier in their upper-right edge, and framed/window/text surfaces stamp small `F####`, `T####`, or image `I####` identifiers. This is deliberately visible during live test builds so testers can report exact offending frames/buttons by number instead of relying on ambiguous visual descriptions.

World-generation controls were split more aggressively: `GENERATE WORLD` now owns its own bounded section, while the remaining generation actions flow into a two-column bounded action grid. Command/menu/inventory/options surfaces continue to use containment reflow so physical button rectangles are clamped to their owning panel before drawing. This remains a client/UI correction pass only; it does not open live mod loading, source acquisition, multiplayer networking, authoritative server ticking, hot restart, save migration, or broad worker-thread simulation.

## 0.9.10ff — Phase 3.6 Chronology / Age Identity Foundation

0.9.10ff responds to the live-design requirement that NPCs and player characters need randomized, durable ages so the game can distinguish children, teens, young adults, adults, and elders instead of relying only on role text. The pass adds an age/world-time authority with a separate universal `worldTurn` counter alongside the existing player survival `turn` counter. Player survival time remains the run-facing measure of how long the current character has survived; world time is now the universal chronology surface that later multiplayer, server authority, long-term NPC ledgers, and aging checks can share.

Candidates now receive starting ages during character generation, and active characters persist age, birth world-turn, and age band. NPCs receive stable generated ages from their role/faction/zone context, and age is saved/restored with NPC state. Child/schola/youth actors are now governed by age as well as text: under-sixteen actors are treated as faction members without formal rank authority, while actors sixteen and older can progress into young-adult/adult faction portrait and rank handling. Loaded actor ages are derived from birth world-turn plus worldTurn, so one year of universal world time can increase an actor's age without requiring a live real-time aging loop.

This is a chronology/personnel foundation only. It does not implement live multiplayer, authoritative headless server ticking, source acquisition, mod loading, process hot-swap, save migration, or broad worker-thread simulation.

## 0.9.10fe — Phase 3.6 Equipment Paper-Doll / Lighting / Corridor Clearance Correction

0.9.10fe responds to the next live Linux test report after project-wide UI containment. The pass adds a paper-doll equipment slot surface to the character dossier with Armor, Clothing, Left Hand, Right Hand, and Backpack slots, including slot selection and unequip-to-inventory behavior. Equippable inventory items now route into the paper-doll where possible instead of remaining only as ordinary carried inventory text.

The pass also continues the client-readability gate: faction-relation labels are fitted to their columns; the Look stack gains scroll state and prioritizes the actual inspected tile/door/surface before room metadata; movement/inspection UI keeps the selected icon column; wall-edge highlight intensity is reduced; child/schola portraits are restricted to child/youth actors; child/youth actors are treated as faction members without formal rank authority; and starter civilian/hiver districts receive stronger ambient and periodic street-light fallback.

World placement sanitation was expanded so blocking fixtures near corridors/doors maintain clearance rather than allowing vending machines, crates, or machines to close a hallway by corner adjacency. Road/exterior-wall cleanup now trims road glyphs at the map boundary and removes road overlap through exterior hivewall maintenance edges. This remains a live Linux/UI/world-readability correction boundary; it does not open live mods, source acquisition, multiplayer networking, authoritative server ticking, hot restart, save migration, or broad worker-thread simulation.

## 0.9.10fd — Phase 3.6 Project-Wide UI Containment / Inspectability Correction

0.9.10fd responds to continued Linux live-test reports that menu buttons, command buttons, options controls, inventory buttons, and Look-stack text were still overflowing their owning frames. The pass adds a stronger containment layer that clamps and reflows command-surface buttons, inventory action buttons, options controls, and world-generation controls inside their panel-safe rectangles. It also clips generic text panels and Look-stack details so player-facing prose cannot escape the box that owns it.

Recent Actions is moved into the reserved strip above the tactical slate and remains filtered for diegetic player-facing events rather than zoom/debug/window chatter. Inventory inspect descriptions now use the bottom item-information pane with Page Up/Page Down scrolling, while inventory action buttons are reflowed into the Actions column. The Look stack now uses a bounded right-side icon column for the currently selected stack entry, shortens rank presentation into an Infopedia-facing reference line, and preserves tile/object/surface answers as the main inspect purpose.

This pass also corrects the tile-icon look selector offset, suppresses movement-ghost drawing unless a longer-range preview is actively armed, prevents adult/ganger NPCs from falling back to schola-child portrait ranges, and narrows disguise-hostility escalation so non-territorial identity failures can be noticed without automatically creating hostile intent. Live mod loading, source acquisition, multiplayer networking, authoritative server ticking, hot restart, save migration, and broad worker-thread simulation remain closed.

Verification: Java 17 compile and jar rebuild completed; Linux launcher scripts remain executable; docs containment preserved at the required four durable files.

## 0.9.10fc — Phase 3.6 Live Menu / Loading / Movement Bounds Correction

0.9.10fc continues the Linux/XFCE live correction gate after 0.9.10fb. It responds to menu and early-run observations from the first playable smoke path: pause-menu buttons are shifted lower, boot presentation is reorganized so the gear sits beside the title and loading text/progress remain bounded, the main-menu launcher-runtime panel is compacted at the lower menu edge, the secondary title/splash image is displayed beneath The Mechanist title, the world-generation Generate World action is separated from option-cycling controls, character-creation name/candidate/roll/stat text is re-bounded, the intro crawl receives additional line padding, first-impression title/meta text is fitted to its frame, and new-game finalization now reports atlas/world/player/sensory/insertion substeps.

The movement-preview correction fixes a live failure in the first-pass double-tap movement ghost: ordinary key release no longer clears the armed movement ghost, so a second matching direction tap can confirm longer-range movement. This remains a bounded input/UI correction only; it does not open route pathfinding, movement reservation, mouse path selection, live mod loading, multiplayer networking, authoritative server ticking, hot restart, save migration, or broad worker-thread simulation.

Verification: Java 17 compile passes, jar is rebuilt, launcher scripts remain executable, docs containment remains at the four durable files, and the package is ready for renewed Linux Mint/XFCE smoke testing.

## 0.9.10fb — Phase 3.6 Live Inventory / Text Hygiene / Feature Placement Correction

0.9.10fb is a live Linux/XFCE correction pass following 0.9.10fa. It responds to the first in-world inventory and map readability findings: inventory must show inventory rather than migration/explanation text, Recent Actions must remain diegetic and avoid zoom/options chatter, Options controls must stay constrained inside the options panel, doors and machinery need minimum light fallbacks, features/base objects must not sit on door tiles, player movement tweening must not leave the player between tiles, and wall/floor visuals must remain distinct.

Implementation changes: the Inventory panel is rebuilt into three functional columns — current inventory, target/current floor tile, and action buttons — with a top summary band and bottom item-information box. Abstract pools such as Script, supplies, food, water, and parts are summarized in the header but are not injected as carried inventory stacks. A target/floor selector is added so the current tile/feature target can be inspected separately from carried items.

Rendering and feedback corrections: Recent Actions now filters zoom/options/backend chatter; Options buttons are clamped to the options panel rather than merely the screen; door/transition tiles receive a fallback light radius; machine/equipment-like objects receive small off-state or larger active light contribution; generated feature placement is sanitized so map objects and base objects are not left on closed/transition door tiles; player tile-slide interpolation is temporarily snapped to authoritative tile center until a future movement-state gate can guarantee tween completion before input returns.

Closed gates remain closed: no live mod loading, no Steam/GOG/private source acquisition, no multiplayer networking, no authoritative headless-server ticking, no process hot-swap, no save migration, no full movement pathfinding, and no broad simulation worker threading.

## 0.9.10fa — Phase 3.6 Live UI Readability / Load Menu / Look Stack Correction

0.9.10fa is a live Linux/XFCE correction pass built on the 0.9.10ey timer recovery and 0.9.10ez launcher-shell state extraction. The pass responds to live menu/gameplay observations: launcher runtime text was crowding the main menu, Load needed to become a real save/load surface, character-creation panels needed stronger low-resolution anchoring, transition/crawl text needed centered/clipped wrapping, recent actions needed to stay diegetic, and the Look stack needed to report actual tile/object surfaces rather than topology noise.

Implemented corrections include: the launcher/runtime state panel is positioned at the lower menu edge; the main menu route label is now LOAD rather than LOAD AUTO; LOAD opens the save/load panel; manual saves are presented as slots 1-3 and autosaves as a 1-5 column surface with the first two slots bound to hourly and zone-transition autosaves; character portrait/name placement is anchored left/right in the top band; character sheet stat rows are lowered below the panel title; intro crawl text is centered; floor-transition text is clipped/wrapped to its box; recent actions are drawn above the tactical slate and debug/benchmark/panel-open chatter is filtered out; Look stack now includes visible icon strips and tile/surface/door information while omitting local-topology circulation text; road/floor alias bindings were corrected; wall rendering receives a distinct top-priority overlay; and longer-range movement now arms a movement ghost on first directional input and confirms on the matching second input.

Boundary: this pass does not implement live mod loading, source acquisition, multiplayer networking, authoritative headless-server ticking, process hot-swap, save migration, broad worker-thread simulation, or a full mouse path/route selector. Those remain closed Phase 3.6 gates.

Verification: Java 17 compile passed; jar rebuilt; xvfb boot smoke reached startup audits without uncaught exceptions; Linux launch scripts remain executable; docs containment remains the four durable files.

## 0.9.10ey — Phase 3.6 Linux Display Reconfigure / Loader Timer Recovery Correction

- Diagnosed the live Linux freeze log as a runtime-timer shutdown caused by applying Exclusive Fullscreen/window reconfiguration; the frame dispose path fired window close handlers and stopped the Swing timer before the character FINALIZE loading state could advance.
- Added a controlled window-reconfiguration guard so window-mode/resolution changes may dispose/redecorate the frame without treating that operation as application shutdown.
- Added runtime-timer recovery at new-game and transition loading start, plus display-reconfigure end recovery, so loading progress restarts defensively if a prior window operation has stopped the timer.
- Preserved the 0.9.10ex low-resolution UI corrections and loader audit/watchdog behavior; this pass is corrective and does not implement live mod loading, multiplayer networking, authoritative server ticking, hot-swap, save migration, or broad worker-thread simulation.
- Rebuilt the jar with Java 17 and preserved Linux executable launch helpers.

## 0.9.10ex — Phase 3.6 Linux UI Scaling / Loader Audit Correction

- Responded to the first live Linux/XFCE UI smoke-test defect report after low-resolution testing exposed menu/title/button scaling, text overflow, world-select/world-setup/character-creation overlap, heavy CRT softness, and a loader stall at zero progress.
- Reworked the main-menu layout so the Mechanist title image and launcher-shell button grid scale inside the available virtual canvas instead of overdrawing the menu window edges.
- Updated shared button drawing so labels are fit against button interior bounds with dynamic font reduction, preventing long labels from overflowing their frame capsules at low resolution.
- Reduced CRT overlay opacity/spacing impact and forced non-game UI passes to render at full virtual readability with sharper presentation, while preserving downscale behavior for the game scene.
- Reworked Options graphics dropdown behavior so resolution choices render as a bounded nearby list rather than an unbounded column that clamps every paint frame.
- Reworked world selection so world-file text occupies the upper panel and action buttons remain below it.
- Reworked world setup into simulation information and action columns, with a world-size percentage bar showing minimum/maximum size endpoints above the plus/minus controls.
- Reworked character creation into a top action/name band and three bounded columns: thin roll ranges, central character sheet, and right-side job dossier text.
- Added audited loader progress, watchdog warnings, and a background finalization worker so long world-generation finalization cannot silently sit at zero without loader status/log evidence.
- Verified Java 17 compile, rebuilt the jar, and ran an offscreen 960x540 UI smoke check across MENU, WORLD_SELECT, WORLD_SETUP, CHARACTER, OPTIONS-with-dropdown, and LOADING with no button bounds escaping detected.

## 0.9.10ew — Phase 3.6 Launcher Shell / Client-Server Boundary Implementation Start

- Opened the bounded Phase 3.6b implementation gate after the runtime-separation planning pass and Linux launch correction.
- Added `LauncherClientServerRuntimeAuthority` so the main menu can behave as a launcher shell, prepare a local client/server boundary before local play, and route Mods/Multiplayer requests without implementing live mod loading or networking.
- Added `UserProfileAuthority` to detect Steam/GOG-like wrapper environments where possible and otherwise generate a stable SHA-based internal Mechanist profile identifier under `settings/profile.seed`.
- Added a dynamic profile button to the upper-right main menu surface and new Mods/Multiplayer buttons to the launcher shell.
- Converted render downscale into a persisted Options graphics setting under `GameOptions`, wired it into `RenderScalingCrtAuthority`, and kept F10 as a quick render-profile test shortcut.
- Made resolution selection apply through the existing Swing window-mode boundary instead of merely changing stored option text.
- Updated Infopedia/audit surfaces, master plan, standards, governance, README, and conversation briefing for the new bounded implementation gate.
- Rebuilt the jar with Java 17 and preserved Linux launch helpers from 0.9.10ev.

## 0.9.10ev — Phase 3.6 Linux/XFCE Launch Packaging Correction

- Corrected the Linux runnable package after live-test preparation found that the shipped `run_linux.sh` was not executable and no double-click-oriented launcher was present.
- Replaced the minimal launcher with a terminal-safe `run_linux.sh` that checks package layout, checks Java availability, and writes `launch_linux.log` on startup/failure.
- Added `PLAY_THE_MECHANIST_LINUX.sh` for direct double-click-oriented Linux/XFCE launching, `The Mechanist.desktop` as a desktop-entry launcher, and `install_linux_launcher.sh` to refresh executable bits and optionally copy the desktop entry to common Linux locations.
- Updated README and RUN_INSTRUCTIONS with Linux Mint/XFCE launch recovery steps while preserving the four durable docs rule.
- Advanced visible runtime build markers to 0.9.10ev and rebuilt the runnable jar from current source.
- Verified Java 17 compilation, jar rebuild, executable-bit preservation in the zip, jar integrity, and zip integrity.
- Did not implement actual launcher orchestration, headless server execution, mod loading, Steam/private source resolution, process restart handoff, multiplayer, hot swapping, worker-thread simulation, or save migration.

## 0.9.10eu — Phase 3.6 Runtime Separation Refactor Plan / Launcher-Client-Server Boundary Vocabulary

- Opened Phase 3.6 in the master development plan as a runtime-separation refactor planning phase after the 0.9.10et client render/input foundation.
- Added `ApplicationRuntimeMode`, `RuntimeProfile`, and `RuntimeSeparationAuthority` as compile-safe semantic seams for launcher/client/internal-server separation planning.
- Added command-line runtime-profile vocabulary for mode, profile, save, world, mod-manifest path, mod tokens, and internal-server intent while keeping the current desktop client as the only effective runtime.
- Exposed the runtime-separation boundary through startup audit logging plus production/audit Infopedia entries.
- Updated standards to allow Phase 3.6 boundary vocabulary while continuing to forbid premature headless server execution, mod loading, Steam/private-source resolution, classloader hot-swapping, multiplayer, broad worker-thread simulation, and save migration.
- Updated governance to recognize the long-term main menu as a launcher/orchestrator boundary.
- Advanced visible runtime build markers to 0.9.10eu and rebuilt the runnable jar from current source.
- Verification: `javac --release 17` compile passed; jar rebuilt; zip integrity checked; `docs/` still contains exactly the four durable source-of-truth files.

## 0.9.10et — Design Initiative Retarget Foundation / Render Scaling and Unified Input Bridge

- Began the post-Phase-3.5 core-feature retarget from the 0.9.10es readiness checkpoint.
- Added `src/mechanist/RenderScalingCrtAuthority.java` as the Java2D/Swing render-scaling owner for the 960x540 virtual canvas, aspect-correct letterbox/pillarbox presentation, bilinear performance downscale profiles, cached CRT scanline overlay, and physical-to-virtual mouse coordinate mapping.
- Updated `GamePanel` painting so the game now renders through the offscreen render-scaling surface before presentation. F10 and the Graphics options panel cycle the current render profile for 1080p, 720p, 1080p/50%, and Linux XFCE i5/50% testing.
- Added `InputRegistry`, `InputSource`, `InputAction`, `KeyboardInputBridge`, `GenericControllerSchema`, `GamepadControllerSnapshot`, and `GamepadInputEngine` as the first unified input foundation.
- Added a fourth Generic controller schema tab alongside Keyboard, Xbox, PlayStation, and Steam controller documentation.
- Implemented source-aware keyboard/gamepad coexistence: keyboard and gamepad maintain independent registry states so releasing one source does not negate an action still held by the other source.
- Added optional Jamepad activation through a reflection-backed background polling engine. The source still compiles and packages with plain `javac`; Maven users may add the provided JitPack dependency to activate hardware polling.
- Added `pom.xml` with the optional Jamepad dependency block and Java 17 release target.
- Exposed the new render and input systems through startup audit logging, production Infopedia, audit Infopedia, Options/Graphics, and Options/Controls surfaces.
- Advanced visible runtime build markers to 0.9.10et and rebuilt the runnable jar from current source.

Boundary: this pass initializes foundational render/input retarget infrastructure only. It does not implement a headless server, launcher, simulation worker pools, hot-swapping, save-format migration, live controller rebinding persistence, or authoritative client/server separation.

Next recommended step: run Linux Mint XFCE i5 live testing with 720p and 50% performance profiles, then perform a targeted correction pass for any render clipping, Jamepad native/runtime packaging issue, or controller mapping mismatch before deeper client/server or multithreaded simulation retarget work begins.

## 0.9.10es — Phase 3.5 Stability Endpoint / Retarget Readiness Audit

- Added `src/mechanist/RetargetReadinessAuditAuthority.java` as the endpoint audit surface for the Phase 3.5 economic-topology chain.
- Verified the intended chain from shared topology vocabulary through generation bias, cached local topology metadata, preview-only consumers, topology reporting overlay, map/intel bridge display, and retarget-readiness handoff.
- Added runtime storage for the endpoint audit surface and world-generation audit logging under `RETARGET_READINESS_AUDIT`.
- Added production Infopedia and audit Infopedia entries for the retarget-readiness endpoint.
- Added workbench/status visibility for the endpoint checkpoint so active-zone readiness can be inspected alongside topology/logistics advisory surfaces.
- Updated master plan, standards, governance, README, and continuation briefing to make 0.9.10es the Phase 3.5 endpoint checkpoint.
- Advanced visible runtime build markers to 0.9.10es and rebuilt the runnable jar from current source.

Boundary: this endpoint is audit/handoff only. It does not start the next retarget, block construction, override placement legality, pathfind, reserve routes, move goods, assign labor, mutate districts, run markets, or propagate pressure.

Next recommended step: begin the planned core-feature retarget from this checkpoint, or perform a corrective endpoint patch only if verification later finds a concrete compile, packaging, stale-document, or UI wiring defect.

## 0.9.10er — Phase 3.5 Topology Map/Intel Bridge

- Added `src/mechanist/EconomicTopologyMapIntelBridgeAuthority.java` as a display-only bridge from the cached topology reporting overlay into existing map and room-intel surfaces.
- Added world-generation caching and audit logging for the map/intel bridge under `ECONOMIC_TOPOLOGY_MAP_INTEL_BRIDGE`.
- Exposed active-zone topology legends, room-role spread, circulation spread, compact room legend tags, and advisory caution text through the map panel and visible room-intel ledger.
- Added production Infopedia and audit Infopedia entries for the map/intel bridge, plus startup audit logging.
- Updated master plan, standards, governance, README, and continuation briefing to make 0.9.10er the current Phase 3.5 continuation point.
- Added a Phase 3.5 endpoint target: one final stabilization/readiness pass should close the bridge layer before the upcoming core-feature retarget begins.
- Advanced visible runtime build markers to 0.9.10er and rebuilt the runnable jar from current source.
- Boundary preserved: display-only; no legality recolor, construction blocking, route reservation, pathfinding, item movement, labor assignment, district mutation, market stock churn, live economy, or pressure propagation.

## 0.9.10eq — Phase 3.5 Topology Reporting Overlay

- Added `src/mechanist/EconomicTopologyReportingOverlayAuthority.java` as the compact active-zone reporting surface above cached local topology metadata and preview-only construction/logistics consumers.
- Added world-generation caching for the reporting overlay after local topology metadata is built, with startup and worldgen audit lines under `ECONOMIC_TOPOLOGY_REPORTING_OVERLAY`.
- The overlay summarizes zone purpose, infrastructure age, dominant pressure, primary circulation, room-role bucket counts, corridor/circulation bucket counts, selected construction advisory context, and route-readiness cautions.
- Exposed the reporting overlay through production Infopedia, audit Infopedia, workbench logistics/status surfaces, and visible room-intel summaries.
- Updated master plan, standards, governance, README, and continuation briefing to make 0.9.10eq the current Phase 3.5 continuation point.
- Advanced visible runtime build markers to 0.9.10eq and rebuilt the runnable jar from current source.
- Boundary preserved: reporting-only; no construction blocking, route reservation, pathfinding, item movement, labor assignment, district mutation, market stock churn, or pressure propagation.

## 0.9.10ep — Phase 3.5 Topology Preview Consumers

- Added `EconomicTopologyPreviewConsumerAuthority` as the first advisory consumer bridge for the cached local topology metadata surface.
- Construction validation inspection now reports selected-build topology intent, cursor room/corridor context, zone pressure/circulation context, and advisory fit without changing placement legality.
- Build-panel compact validation text now includes a short topology advisory so the player can see whether a build reads as coherent or improvised in the selected local context.
- Logistics route intent records now include cached topology source, destination, and zone context lines without pathfinding, route locking, hauling, item transfer, or production effects.
- Manual-haul readiness preview now adds topology warnings for poor source anchors, awkward destination contexts, non-freight primary circulation, and high-security/black-market pressure where appropriate.
- Production and audit Infopedia surfaces expose the preview-consumer authority, and startup audit logging records the advisory-only bridge.
- Updated master plan, standards, governance, README, and continuation briefing to make 0.9.10ep the current Phase 3.5 continuation point.
- Advanced visible runtime build markers to 0.9.10ep.
- Verification: Java 17 compile passed; jar rebuilt; zip integrity checked; no new durable docs added; no live economy, route reservation, autonomous hauling, labor assignment, district conversion, or pressure propagation added.

## 0.9.10eo — Phase 3.5 Cached Local Topology Metadata Surface

Implemented the cached local topology metadata surface for the Phase 3.5 economic-topology sequence.

Completed work:

- Added `EconomicLocalTopologyMetadataSurfaceAuthority`.
- Added a cached per-zone topology surface containing zone purpose, infrastructure age, dominant pressure, primary circulation, room-role tags, corridor/openwork circulation readings, and circulation counts.
- Integrated the cache after late generation/repair/hazard/trap setup so it records the stabilized local zone rather than early room-pool intent only.
- Added world fields for the local topology surface, summary text, and sample notes.
- Routed Look-panel room inspection and corridor/openwork inspection through the cached surface, with generation-bias corridor inspection retained as fallback.
- Added active-world room-intel lines, production Infopedia entry, audit Infopedia entry, startup audit logging, and world-generation audit logging.
- Advanced visible runtime build markers to 0.9.10eo.
- Updated master plan, standards, governance, README, and new-conversation briefing around the cached local topology metadata stage.

Design boundary:

This pass is still descriptive metadata. It does not move goods, reserve routes, assign workers, pathfind hauling, mutate districts, widen roads, run market stock, convert room ownership, or propagate pressure beyond the loaded zone. Its purpose is to give future preview consumers one stable local answer before any live economy gate opens.

Next recommended step:

Begin connecting selected preview-only authorities to the cache. Construction validation can explain why a room is a good or poor candidate, logistics route readiness can describe expected circulation pressure, and map/room panels can show local industrial meaning. These consumers should remain explanatory/preflight-only until the master plan explicitly authorizes live reservations, labor, goods movement, or district evolution.

## 0.9.10en — Phase 3.5 Economic Generation Bias Bridge

Implemented the first bounded generation consumer for the Phase 3.5 economic-topology schema.

Changed source files:
- Added `EconomicGenerationBiasAuthority`.
- Updated `RoomProfile.forZone(...)` so the local room-profile pool is weighted by zone purpose, dominant pressure, and semantic circulation.
- Updated `World.generate(...)` so generated rooms receive local economic-topology feature notes after faction assignment and contestable room insertion.
- Added active-world economic generation summaries and sample room topology notes to the `World` runtime state.
- Added Look-panel corridor inspection language for semantic circulation classification.
- Added Production and Audit Infopedia entries for the new generation-bias authority.
- Advanced visible runtime build markers to 0.9.10en.

What this does:

The generator now asks the topology authority what the district economically is before selecting and describing rooms. A forge cloister leans harder toward machine, workshop, relay, and storehouse meanings. A rail or train district leans harder toward freight, cargo, depot, warehouse, and throughput rooms. A noble service spine leans harder toward servant, luxury-service, controlled-supply, and hidden-backbone readings. Sewer and scavenger layers lean harder toward decay, salvage, utility, and bypass meanings.

This is still not a live economy. The pass does not move items, reserve routes, spawn workers, widen corridors, run demand propagation, convert districts, or update market stocks. It only biases generation and exposes the decision in room/corridor inspection surfaces.

What this enables next:

The next Phase 3.5 pass should consolidate these local readings into a cached topology metadata surface. Map rendering, construction validation, logistics preview, route intent, and future ledger systems should be able to query one stored per-zone topology result instead of independently re-reading profiles, room text, and corridor glyphs.

Verification:
- `javac --release 17` PASS.
- `TheMechanist.jar` rebuilt from current source.
- `docs/` still contains exactly the four durable governance documents.

## 0.9.10em — Phase 3.5 Economic Topology Foundation Authority

Implemented the first bounded Phase 3.5 source authority: `EconomicTopologyFramework`.

What changed:

- Added faction industrial doctrine profiles for major faction families and exact factions where relevant.
- Added pressure-field vocabulary and 0-10 profile values for industrial, logistics, labor, security, pollution, religious, decay, and black-market pressure.
- Added semantic circulation classes such as freight arteries, cargo corridors, industrial service loops, maintenance tunnels, sewer trunks, noble service boulevards, shrine pilgrimage routes, barracks grids, hidden bypasses, hab branch corridors, and administrative queue lines.
- Added infrastructure-age bands for foundational, inherited, maintained, patched, degraded, and forbidden hidden infrastructure.
- Added zone-purpose identity profiles for every current `ZoneType`, including PDF logistics billets, forge cloisters, freight/transit districts, noble service backbones, utility sewer spines, salvage districts, criminal production turf, law/security nodes, and civilian mixed-industrial districts.
- Wired the authority into startup auditing through `ECONOMIC_TOPOLOGY`.
- Exposed the schema through zone Infopedia details, faction Infopedia details, the production reference surface, and the audit reference surface.
- Updated runtime visible build markers from 0.9.10ak to 0.9.10em.
- Reconciled master-plan, standards, governance, README, and handoff briefing text around Phase 3.5 as the current directed gate.

Design boundary:

This pass is classification-first. It does not start autonomous faction production, district conversion, full-map pressure propagation, hidden market stock churn, or background labor loops. The new authority exists so later generation, logistics, construction, production, and simulation systems can ask one shared source what a district is before they act on it.

What this enables next:

The next Phase 3.5 pass can connect the schema into bounded generation bias. A zone can ask for its purpose identity, circulation class, dominant pressures, and infrastructure age, then use those answers to weight room selection, corridor language, inspection output, and map/readability summaries without running a live economy.

Verification:

- Java 17 compile passed.
- Jar rebuilt from current source.
- Documentation containment preserved: `docs/` still contains only the four durable development documents.

## 0.9.10el — Master Plan Restructure / Phase 3.5 Introduction

Date:
2026-05-20T23:03:26.294626 UTC

Major architectural restructuring performed.

The project roadmap has been revised to formally introduce:

# Phase 3.5 — Economic Topology / Industrial Ecology Framework

This phase now serves as the bridge between:
- procedural topology generation,
and
- dynamic world/civilization simulation.

Core goals introduced:
- faction industrial doctrine,
- infrastructure pressure fields,
- semantic circulation hierarchy,
- infrastructure age metadata,
- industrial demand propagation,
- economically meaningful zone identity,
- dynamic district conversion.

Reason for restructuring:
The circulation/tagging/topology framework has evolved beyond pure geometry generation and now requires semantic industrial infrastructure support before deeper phase expansion proceeds safely.

This restructuring intentionally delays deeper downstream phase advancement to avoid:
- future geometry rewrites,
- tightly coupled temporary assumptions,
- non-semantic districts,
- fragmented industrial simulation architecture.

What needs to happen next:
- define faction doctrine schemas,
- define pressure field architecture,
- define semantic circulation classes,
- define zone-purpose metadata structures,
- begin Phase 3.5 implementation planning.


## 0.9.10ek — Phase 3 First-Site Selection Registry

Continued Phase 3 from circulation dry-run batch reporting.

Patch actions:
- Added first-site migration selection registry and preferred-site selector.

Added:
- `Phase3FirstSiteSelectionRegistryEntry`
- preferred first-site selector
- migration selection hold gate

Check result:
javac syntax/import check passed.

What is being done:
- selecting the safest first real migration site,
- preventing unstable first-site selection.

What needs to be done next:
- populate registry from dry-run candidates,
- wire first real migration site,
- collect migration report.

## 0.9.10ej — Phase 3 Circulation Dry-Run Batch Report / Candidate Selection

Continued Phase 3 from circulation migration dry-run harness.

Patch actions:
- Added dry-run batch report and first-site candidate selection gate.

Added `Phase3CirculationDryRunBatchReport`, dry-run summarizer, and first-site candidate selection gate.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- comparing dry-run candidates before the first real migration,
- preventing migration when all candidate sites are unstable.

What needs to be done next:
- run dry-runs on strongest candidate sites,
- select one first migration site,
- wire real single-site migration.

## 0.9.10ei — Phase 3 Circulation Migration Dry-Run Harness

Continued Phase 3 from first-site circulation migration checklist.

Patch actions:
- Added first-site circulation migration dry-run result and harness.

Added `Phase3CirculationMigrationDryRunResult`, dry-run harness, and proceed-gate for moving from dry-run to real single-site migration.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- enabling preflight testing before first circulation commit migration,
- preventing mutation during dry-run,
- reducing risk before real road/corridor call-site wiring.

What needs to be done next:
- dry-run the safest candidate site,
- wire real single-site migration only after dry-run passes.

## 0.9.10eh — Phase 3 First-Site Circulation Migration Wiring Checklist

Continued Phase 3 from circulation migration test reporting.

Candidate commit references observed:
WorldRuntimeGenerationFramework.java near line 1078:
```java
1073:             candidateRooms.add(new Rectangle(corridor.x+corridor.width, baseY, roomSize, roomSize));
1074:             candidateRooms.add(new Rectangle(corridor.x+corridor.width, baseY+5*(side==2?1:-1), roomSize, roomSize));
1075:         }
1076:         if(!moduleAreaLegal(corridor, candidateRooms)) return false;
1077:         char cg = zoneType.corridorGlyph(r);
1078:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
1079:         Point pd = plazaDoorForModule(plaza, corridor);
1080:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
1081:         String[] labels = {"Apartment Living Room", "Apartment Bedroom", "Apartment Washroom", "Apartment Dining Nook"};
1082:         for(int i=0;i<candidateRooms.size();i++){
1083:             Rectangle rr = candidateRooms.get(i);
1084:             int idx=rooms.size(); carve(rr); rooms.add(rr);
1085:             if(idx < roomProfiles.size()) roomProfiles.set(idx, RoomProfile.apartmentRoom(labels[i], zoneType, r));
1086:             placeApartmentFeatures(rr, i);
1087:             Point d = doorBetweenRoomAndCorridor(rr, corridor);
1088:             if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
1089:         }
```

WorldRuntimeGenerationFramework.java near line 1038:
```java
1033:         }
1034:         // keep only a handful so modules do not overrun the room quota too aggressively
1035:         while(candidateRooms.size()>4) candidateRooms.remove(candidateRooms.size()-1);
1036:         if(!moduleAreaLegal(corridor, candidateRooms)) return false;
1037:         char cg = zoneType.corridorGlyph(r);
1038:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
1039:         Point pd = plazaDoorForModule(plaza, corridor);
1040:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
1041:         for(Rectangle rr: candidateRooms){
1042:             int idx=rooms.size(); carve(rr); rooms.add(rr);
1043:             if(idx < roomProfiles.size()) roomProfiles.set(idx, RoomProfile.dormitoryCell(zoneType, r));
1044:             placeDormitoryFeatures(rr);
1045:             Point d = doorBetweenRoomAndCorridor(rr, corridor);
1046:             if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
1047:         }
1048:         return true;
1049:     }
```

WorldRuntimeGenerationFramework.java near line 1004:
```java
999:         } else if(module.corridorDress.contains("CELL_ROW")){
1000:             for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y) && ((horizontal?x:y)%3==0)) tiles[x][y]='X';
1001:         } else if(module.corridorDress.contains("MARKET_ROW")){
1002:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=3) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]='$';
1003:         } else if(module.corridorDress.contains("DATA_STACK")){
1004:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=2) for(int y=corridor.y; y<corridor.y+corridor.height; y+=2) if(inBounds(x,y)) tiles[x][y]='l';
1005:         }
1006:     }
1007: 
1008:     boolean stampDormitorySegment(Rectangle plaza, int side){
1009:         // Long trunk corridor with small hab-cell rooms directly off the sides.
1010:         // Each dorm cell is constrained to a tiny 3x4 footprint so it behaves like
1011:         // the requested 2x4 micro-room while still leaving at least one interior tile
1012:         // for an inspectable cot/sink/storage feature in the current tile renderer.
1013:         boolean horizontal = side==1 || side==3;
1014:         int len = 18, cw = 3, cellW = horizontal ? 3 : 4, cellH = horizontal ? 4 : 3;
1015:         int cx, cy;
```

WorldRuntimeGenerationFramework.java near line 1002:
```java
997:                 if(inBounds(x, corridor.y+corridor.height-2)) tiles[x][corridor.y+corridor.height-2]='N';
998:             }
999:         } else if(module.corridorDress.contains("CELL_ROW")){
1000:             for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y) && ((horizontal?x:y)%3==0)) tiles[x][y]='X';
1001:         } else if(module.corridorDress.contains("MARKET_ROW")){
1002:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=3) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]='$';
1003:         } else if(module.corridorDress.contains("DATA_STACK")){
1004:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=2) for(int y=corridor.y; y<corridor.y+corridor.height; y+=2) if(inBounds(x,y)) tiles[x][y]='l';
1005:         }
1006:     }
1007: 
1008:     boolean stampDormitorySegment(Rectangle plaza, int side){
1009:         // Long trunk corridor with small hab-cell rooms directly off the sides.
1010:         // Each dorm cell is constrained to a tiny 3x4 footprint so it behaves like
1011:         // the requested 2x4 micro-room while still leaving at least one interior tile
1012:         // for an inspectable cot/sink/storage feature in the current tile renderer.
1013:         boolean horizontal = side==1 || side==3;
```

WorldRuntimeGenerationFramework.java near line 1000:
```java
995:                 int x = corridor.x + corridor.width/2;
996:                 for(int y=corridor.y+1; y<corridor.y+corridor.height-1; y+=3){ if(inBounds(x,y)) tiles[x][y]='T'; if(inBounds(x,y+1)) tiles[x][y+1]='b'; }
997:                 if(inBounds(x, corridor.y+corridor.height-2)) tiles[x][corridor.y+corridor.height-2]='N';
998:             }
999:         } else if(module.corridorDress.contains("CELL_ROW")){
1000:             for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y) && ((horizontal?x:y)%3==0)) tiles[x][y]='X';
1001:         } else if(module.corridorDress.contains("MARKET_ROW")){
1002:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=3) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]='$';
1003:         } else if(module.corridorDress.contains("DATA_STACK")){
1004:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=2) for(int y=corridor.y; y<corridor.y+corridor.height; y+=2) if(inBounds(x,y)) tiles[x][y]='l';
1005:         }
1006:     }
1007: 
1008:     boolean stampDormitorySegment(Rectangle plaza, int side){
1009:         // Long trunk corridor with small hab-cell rooms directly off the sides.
1010:         // Each dorm cell is constrained to a tiny 3x4 footprint so it behaves like
1011:         // the requested 2x4 micro-room while still leaving at least one interior tile
```

WorldRuntimeGenerationFramework.java near line 940:
```java
935:         else { cx = plaza.x+plaza.width/2-cw/2+off; cy = plaza.y+plaza.height; }
936:         Rectangle corridor = horizontal ? new Rectangle(cx, cy, len, cw) : new Rectangle(cx, cy, cw, len);
937:         java.util.List<Rectangle> candidateRooms = moduleCandidateRooms(corridor, side, module);
938:         if(candidateRooms.isEmpty() || !moduleAreaLegal(corridor, candidateRooms)) return 0;
939:         char cg = zoneType.corridorGlyph(r);
940:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
941:         dressModuleCorridor(corridor, module);
942:         Point pd = plazaDoorForModule(plaza, corridor);
943:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
944:         int made = 0;
945:         for(int i=0;i<candidateRooms.size() && i<module.rooms.length;i++){
946:             Rectangle rr = candidateRooms.get(i);
947:             StampedRoomSpec spec = module.rooms[i];
948:             int idx=rooms.size(); carve(rr); rooms.add(rr);
949:             if(idx < roomProfiles.size()) roomProfiles.set(idx, spec.toProfile(zoneType, r));
950:             if(idx < roomFactions.size()) roomFactions.set(idx, spec.faction);
951:             stampRoomPurposeFeatures(rr, spec);
```

WorldRuntimeGenerationFramework.java near line 655:
```java
650:         else if(side==0) nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y-naveH, naveW, naveH);
651:         else nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y+corridor.height, naveW, naveH);
652:         ArrayList<Rectangle> candidateRooms = new ArrayList<>(); candidateRooms.add(nave);
653:         if(!moduleAreaLegal(corridor, candidateRooms)) return false;
654:         char cg = zoneType.corridorGlyph(r);
655:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
656:         Point pd = plazaDoorForModule(plaza, corridor);
657:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
658:         int idx = rooms.size();
659:         carve(nave); rooms.add(nave);
660:         RoomProfile rp = EcclesiarchyTempleApi.templeProfile(zoneType, r);
661:         if(idx < roomProfiles.size()) roomProfiles.set(idx, rp);
662:         if(idx < roomFactions.size()) roomFactions.set(idx, Faction.MINISTORUM);
663:         if(idx < roomSpecials.size()) roomSpecials.set(idx, Boolean.TRUE);
664:         Point d = doorBetweenRoomAndCorridor(nave, corridor);
665:         if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
666:         stampCultImperialisTempleFeatures(nave, horizontal);
```

MediaRuntimeFramework.java near line 202:
```java
197:     }
198: 
199:     void bind(char glyph, String alias) { BufferedImage img = byAlias.get(alias); if (img != null) byGlyph.put(glyph, img); }
200: 
201:     void bindGlyphs() {
202:         bind('.', "floor_bare_underhive"); bind('`', "floor_trash_mutant_rough"); bind(';', "tile_road_north_south"); bind('_', "tile_road_east_west"); bind('+', "floor_industrial_corridor"); bind('=', "floor_maintenance_corridor"); bind(',', "floor_alleyway_cracked"); bind('~', "floor_sewer_pipe_corridor"); bind(':', "floor_padded_service_way"); bind('-', "floor_exterior_hivewall_maintenance"); bind(' ', "void_space");
203:         bind('#', "wall_bulkhead"); bind('%', "wall_support_beam"); bind('&', "wall_gantry_lattice"); bind('^', "wall_buried_conveyor"); bind('8', "wall_pipe_bundle"); bind('0', "wall_cable_column"); bind('/', "door_archway"); bind('|', "door_standard"); bind('L', "door_locked"); bind('V', "door_vent_panel"); bind('X', "door_security"); bind('D', "door_double"); bind('d', "barricade");
204:         bind('*', "debris"); bind('?', "buried_cache"); bind('!', "danger_marker"); bind('R', "rogue_machine"); bind('N', "noisy_machinery"); bind('1', "vending_food"); bind('2', "vending_armor"); bind('3', "vending_weapons"); bind('4', "vending_materials"); bind('5', "vending_survival");
205:         bind('Y', "water_condenser"); bind('J', "emergency_assembler"); bind('B', "emergency_boiler"); bind('K', "micro_lab"); bind('O', "emergency_miner"); bind('Z', "relay_power_grid"); bind('P', "emergency_smelter"); bind('F', "steam_engine"); bind('U', "steam_engine_disabled"); bind('w', "scrap_workbench"); bind('e', "water_condenser"); bind('f', "emergency_smelter"); bind('l', "micro_lab"); bind('x', "security_cogitator"); bind('T', "turret_or_trade"); bind('H', "shrine_or_shield"); bind('G', "logistics_center"); bind('M', "medicae_or_military"); bind('k', "carrying_station"); bind('q', "supply_post");
206:         bind('I', "imperial_shrine"); bind('$', "donation_box"); bind('W', "saint_alcove"); bind('Q', "governor_dais"); bind('C', "clinic"); bind('r', "corpse_loot"); bind('o', "object_generic"); bind('S', "sewer_hatch"); bind('v', "ladder_drain"); bind('E', "elevator"); bind('s', "storage_crate"); bind('c', "sleeping_cot"); bind('u', "water_barrel"); bind('a', "alarm_trap"); bind('p', "arbites_precinct"); bind('b', "bandit_den"); bind('h', "hiver_block"); bind('n', "noble_secure"); bind('t', "table_prop");
207:         bind('g', "bandit_den"); bind('m', "floor_trash_mutant_rough"); bind('A', "arbites_precinct");
208:     }
209: 
210:     boolean corridorArtUsesNorthSouth(char glyph) {
211:         // The imported corridor sheets contain several top-down cells with a clear north/south long-axis.
212:         // These glyphs are treated as oriented art: east/west corridor runs rotate the drawn image at render
213:         // time, while intersections and omni floor clutter keep the unrotated fallback.
```

MapLayerSurfaceAuthority.java near line 117:
```java
112:         phase3TraversableTiles[x][y] = traversable;
113:         phase3BlockedTiles[x][y] = blocked;
114:     }
115: 
116:     public boolean hasPhase3TileState() {
117:         return phase3TraversableTiles != null && phase3BlockedTiles != null && phase3ReservedTraversalTiles != null && phase3RoadTiles != null && phase3CorridorTiles != null;
118:     }
119: 
120:     public boolean isPhase3InBounds(int x, int y) {
121:         return hasPhase3TileState()
122:                 && x >= 0
123:                 && y >= 0
124:                 && x < phase3TraversableTiles.length
125:                 && phase3TraversableTiles.length > 0
126:                 && y < phase3TraversableTiles[0].length;
127:     }
128: 
```

WorldRuntimeGenerationFramework.java near line 2962:
```java
2957:     // playable map material without expensive tracing.
2958:     static final char HIVEWALL_CORRIDOR = '-';
2959: 
2960:     static class Result {
2961:         int inset, corridors, maintenanceRooms, dangerRooms, voidTiles, highWallTiles;
2962:         String summary(){ return "inset="+inset+" corridors="+corridors+" maintenanceRooms="+maintenanceRooms+" dangerRooms="+dangerRooms+" voidTiles="+voidTiles+" highWallTiles="+highWallTiles; }
2963:     }
2964: 
2965:     static String policySummary(){
2966:         return "post-validation bounded hivewall: exterior maintenance loop, one bolted-on maintenance room, 3-4 abandoned high-danger interwall rooms, void abyss outside high-wall envelope";
2967:     }
2968:     static int maintenanceRoomTarget(){ return 1; }
2969:     static int dangerRoomTargetMin(){ return 3; }
2970:     static int dangerRoomTargetMax(){ return 4; }
2971:     static String[] dangerOccupantBands(){ return new String[]{"powerful mutant", "wanted criminal", "rogue automata", "cultist cell", "heretic survivor"}; }
2972: 
2973:     static Result apply(World world, Random r){
```

Patch actions:
- Added first-site circulation migration wiring checklist.

Added `Phase3FirstSiteWiringChecklist`, strict first-site readiness checking, and `mayWirePhase3FirstSiteCirculationMigration(...)`.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing the first real road/corridor migration,
- preventing premature call-site wiring,
- keeping migration staged and testable.

What needs to be done next:
- choose safest commit point,
- wire single-site harness if checklist passes,
- collect migration report.

## 0.9.10eg — Phase 3 Circulation Migration Test Report

Continued Phase 3 from single-site circulation migration harness.

Patch actions:
- Added circulation migration test report and migration-advance gate.

Added `Phase3CirculationMigrationTestReport`, migration result summarizer, and migration-advance gate.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing staged generation testing for road/corridor migration,
- preventing expansion to additional call sites before first-site stability.

What needs to be done next:
- wire one confirmed commit point,
- collect migration results,
- advance only after stable report.

## 0.9.10ef — Phase 3 Single-Site Circulation Migration Harness

Continued Phase 3 from circulation commit readiness.

Patch actions:
- Added single-site circulation migration result and harness.

Added `Phase3SingleSiteCirculationMigrationResult` and `attemptPhase3SingleSiteCirculationMigration(...)`.

This prepares the project to migrate one confirmed road/corridor commit point at a time while preserving explicit retry/fallback behavior.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing first real circulation call-site migration,
- avoiding broad rewrite,
- preserving temporary fallback while testing.

What needs to be done next:
- select one confirmed commit point,
- wire harness,
- run generation tests.

## 0.9.10ee — Phase 3 Circulation Commit Readiness Layer

Continued Phase 3 from circulation commit-point probe / migration wrapper.

Strongest candidate references observed:
MediaRuntimeFramework.java near line 202 in `bindGlyphs` (method line 201):
```java
197:     }
198: 
199:     void bind(char glyph, String alias) { BufferedImage img = byAlias.get(alias); if (img != null) byGlyph.put(glyph, img); }
200: 
201:     void bindGlyphs() {
202:         bind('.', "floor_bare_underhive"); bind('`', "floor_trash_mutant_rough"); bind(';', "tile_road_north_south"); bind('_', "tile_road_east_west"); bind('+', "floor_industrial_corridor"); bind('=', "floor_maintenance_corridor"); bind(',', "floor_alleyway_cracked"); bind('~', "floor_sewer_pipe_corridor"); bind(':', "floor_padded_service_way"); bind('-', "floor_exterior_hivewall_maintenance"); bind(' ', "void_space");
203:         bind('#', "wall_bulkhead"); bind('%', "wall_support_beam"); bind('&', "wall_gantry_lattice"); bind('^', "wall_buried_conveyor"); bind('8', "wall_pipe_bundle"); bind('0', "wall_cable_column"); bind('/', "door_archway"); bind('|', "door_standard"); bind('L', "door_locked"); bind('V', "door_vent_panel"); bind('X', "door_security"); bind('D', "door_double"); bind('d', "barricade");
204:         bind('*', "debris"); bind('?', "buried_cache"); bind('!', "danger_marker"); bind('R', "rogue_machine"); bind('N', "noisy_machinery"); bind('1', "vending_food"); bind('2', "vending_armor"); bind('3', "vending_weapons"); bind('4', "vending_materials"); bind('5', "vending_survival");
205:         bind('Y', "water_condenser"); bind('J', "emergency_assembler"); bind('B', "emergency_boiler"); bind('K', "micro_lab"); bind('O', "emergency_miner"); bind('Z', "relay_power_grid"); bind('P', "emergency_smelter"); bind('F', "steam_engine"); bind('U', "steam_engine_disabled"); bind('w', "scrap_workbench"); bind('e', "water_condenser"); bind('f', "emergency_smelter"); bind('l', "micro_lab"); bind('x', "security_cogitator"); bind('T', "turret_or_trade"); bind('H', "shrine_or_shield"); bind('G', "logistics_center"); bind('M', "medicae_or_military"); bind('k', "carrying_station"); bind('q', "supply_post");
206:         bind('I', "imperial_shrine"); bind('$', "donation_box"); bind('W', "saint_alcove"); bind('Q', "governor_dais"); bind('C', "clinic"); bind('r', "corpse_loot"); bind('o', "object_generic"); bind('S', "sewer_hatch"); bind('v', "ladder_drain"); bind('E', "elevator"); bind('s', "storage_crate"); bind('c', "sleeping_cot"); bind('u', "water_barrel"); bind('a', "alarm_trap"); bind('p', "arbites_precinct"); bind('b', "bandit_den"); bind('h', "hiver_block"); bind('n', "noble_secure"); bind('t', "table_prop");
207:         bind('g', "bandit_den"); bind('m', "floor_trash_mutant_rough"); bind('A', "arbites_precinct");
208:     }
209: 
210:     boolean corridorArtUsesNorthSouth(char glyph) {
211:         // The imported corridor sheets contain several top-down cells with a clear north/south long-axis.
212:         // These glyphs are treated as oriented art: east/west corridor runs rotate the drawn image at render
213:         // time, while intersections and omni floor clutter keep the unrotated fallback.
```

WorldRuntimeGenerationFramework.java near line 1099 in `moduleAreaLegal` (method line 1093):
```java
1094:         // 0.8.25 CORRIDOR SHAPE AUTHORITY:
1095:         // Module corridors may be double-/triple-wide service ways, but they must still read
1096:         // as corridors: their length must exceed their width. This prevents square blobs from
1097:         // being stamped as corridor space and preserves branchable hallway geometry.
1098:         if(!validCorridorRectangleShape(corridor)) return false;
1099:         Rectangle grownC = new Rectangle(corridor.x-1, corridor.y-1, corridor.width+2, corridor.height+2);
1100:         if(!rectInMap(grownC)) return false;
1101:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++){
1102:             if(!inBounds(x,y) || roomIds[x][y] >= 0 || tiles[x][y] != '#') return false;
1103:         }
1104:         for(Rectangle rr: moduleRooms){
1105:             Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
1106:             if(!rectInMap(grown)) return false;
1107:             for(int x=grown.x; x<grown.x+grown.width; x++) for(int y=grown.y; y<grown.y+grown.height; y++){
1108:                 if(!inBounds(x,y)) return false;
1109:                 if(roomIds[x][y] >= 0 || tiles[x][y] != '#') return false;
1110:             }
```

WorldRuntimeGenerationFramework.java near line 1078 in `if` (method line 1059):
```java
1073:             candidateRooms.add(new Rectangle(corridor.x+corridor.width, baseY, roomSize, roomSize));
1074:             candidateRooms.add(new Rectangle(corridor.x+corridor.width, baseY+5*(side==2?1:-1), roomSize, roomSize));
1075:         }
1076:         if(!moduleAreaLegal(corridor, candidateRooms)) return false;
1077:         char cg = zoneType.corridorGlyph(r);
1078:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
1079:         Point pd = plazaDoorForModule(plaza, corridor);
1080:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
1081:         String[] labels = {"Apartment Living Room", "Apartment Bedroom", "Apartment Washroom", "Apartment Dining Nook"};
1082:         for(int i=0;i<candidateRooms.size();i++){
1083:             Rectangle rr = candidateRooms.get(i);
1084:             int idx=rooms.size(); carve(rr); rooms.add(rr);
1085:             if(idx < roomProfiles.size()) roomProfiles.set(idx, RoomProfile.apartmentRoom(labels[i], zoneType, r));
1086:             placeApartmentFeatures(rr, i);
1087:             Point d = doorBetweenRoomAndCorridor(rr, corridor);
1088:             if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
1089:         }
```

WorldRuntimeGenerationFramework.java near line 1038 in `if` (method line 1018):
```java
1033:         }
1034:         // keep only a handful so modules do not overrun the room quota too aggressively
1035:         while(candidateRooms.size()>4) candidateRooms.remove(candidateRooms.size()-1);
1036:         if(!moduleAreaLegal(corridor, candidateRooms)) return false;
1037:         char cg = zoneType.corridorGlyph(r);
1038:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
1039:         Point pd = plazaDoorForModule(plaza, corridor);
1040:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
1041:         for(Rectangle rr: candidateRooms){
1042:             int idx=rooms.size(); carve(rr); rooms.add(rr);
1043:             if(idx < roomProfiles.size()) roomProfiles.set(idx, RoomProfile.dormitoryCell(zoneType, r));
1044:             placeDormitoryFeatures(rr);
1045:             Point d = doorBetweenRoomAndCorridor(rr, corridor);
1046:             if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
1047:         }
1048:         return true;
1049:     }
```

WorldRuntimeGenerationFramework.java near line 1004 in `dressModuleCorridor` (method line 985):
```java
999:         } else if(module.corridorDress.contains("CELL_ROW")){
1000:             for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y) && ((horizontal?x:y)%3==0)) tiles[x][y]='X';
1001:         } else if(module.corridorDress.contains("MARKET_ROW")){
1002:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=3) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]='$';
1003:         } else if(module.corridorDress.contains("DATA_STACK")){
1004:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=2) for(int y=corridor.y; y<corridor.y+corridor.height; y+=2) if(inBounds(x,y)) tiles[x][y]='l';
1005:         }
1006:     }
1007: 
1008:     boolean stampDormitorySegment(Rectangle plaza, int side){
1009:         // Long trunk corridor with small hab-cell rooms directly off the sides.
1010:         // Each dorm cell is constrained to a tiny 3x4 footprint so it behaves like
1011:         // the requested 2x4 micro-room while still leaving at least one interior tile
1012:         // for an inspectable cot/sink/storage feature in the current tile renderer.
1013:         boolean horizontal = side==1 || side==3;
1014:         int len = 18, cw = 3, cellW = horizontal ? 3 : 4, cellH = horizontal ? 4 : 3;
1015:         int cx, cy;
```

WorldRuntimeGenerationFramework.java near line 1002 in `dressModuleCorridor` (method line 985):
```java
997:                 if(inBounds(x, corridor.y+corridor.height-2)) tiles[x][corridor.y+corridor.height-2]='N';
998:             }
999:         } else if(module.corridorDress.contains("CELL_ROW")){
1000:             for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y) && ((horizontal?x:y)%3==0)) tiles[x][y]='X';
1001:         } else if(module.corridorDress.contains("MARKET_ROW")){
1002:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=3) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]='$';
1003:         } else if(module.corridorDress.contains("DATA_STACK")){
1004:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=2) for(int y=corridor.y; y<corridor.y+corridor.height; y+=2) if(inBounds(x,y)) tiles[x][y]='l';
1005:         }
1006:     }
1007: 
1008:     boolean stampDormitorySegment(Rectangle plaza, int side){
1009:         // Long trunk corridor with small hab-cell rooms directly off the sides.
1010:         // Each dorm cell is constrained to a tiny 3x4 footprint so it behaves like
1011:         // the requested 2x4 micro-room while still leaving at least one interior tile
1012:         // for an inspectable cot/sink/storage feature in the current tile renderer.
1013:         boolean horizontal = side==1 || side==3;
```

WorldRuntimeGenerationFramework.java near line 1000 in `dressModuleCorridor` (method line 985):
```java
995:                 int x = corridor.x + corridor.width/2;
996:                 for(int y=corridor.y+1; y<corridor.y+corridor.height-1; y+=3){ if(inBounds(x,y)) tiles[x][y]='T'; if(inBounds(x,y+1)) tiles[x][y+1]='b'; }
997:                 if(inBounds(x, corridor.y+corridor.height-2)) tiles[x][corridor.y+corridor.height-2]='N';
998:             }
999:         } else if(module.corridorDress.contains("CELL_ROW")){
1000:             for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y) && ((horizontal?x:y)%3==0)) tiles[x][y]='X';
1001:         } else if(module.corridorDress.contains("MARKET_ROW")){
1002:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=3) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]='$';
1003:         } else if(module.corridorDress.contains("DATA_STACK")){
1004:             for(int x=corridor.x; x<corridor.x+corridor.width; x+=2) for(int y=corridor.y; y<corridor.y+corridor.height; y+=2) if(inBounds(x,y)) tiles[x][y]='l';
1005:         }
1006:     }
1007: 
1008:     boolean stampDormitorySegment(Rectangle plaza, int side){
1009:         // Long trunk corridor with small hab-cell rooms directly off the sides.
1010:         // Each dorm cell is constrained to a tiny 3x4 footprint so it behaves like
1011:         // the requested 2x4 micro-room while still leaving at least one interior tile
```

WorldRuntimeGenerationFramework.java near line 940 in `if` (method line 934):
```java
935:         else { cx = plaza.x+plaza.width/2-cw/2+off; cy = plaza.y+plaza.height; }
936:         Rectangle corridor = horizontal ? new Rectangle(cx, cy, len, cw) : new Rectangle(cx, cy, cw, len);
937:         java.util.List<Rectangle> candidateRooms = moduleCandidateRooms(corridor, side, module);
938:         if(candidateRooms.isEmpty() || !moduleAreaLegal(corridor, candidateRooms)) return 0;
939:         char cg = zoneType.corridorGlyph(r);
940:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
941:         dressModuleCorridor(corridor, module);
942:         Point pd = plazaDoorForModule(plaza, corridor);
943:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
944:         int made = 0;
945:         for(int i=0;i<candidateRooms.size() && i<module.rooms.length;i++){
946:             Rectangle rr = candidateRooms.get(i);
947:             StampedRoomSpec spec = module.rooms[i];
948:             int idx=rooms.size(); carve(rr); rooms.add(rr);
949:             if(idx < roomProfiles.size()) roomProfiles.set(idx, spec.toProfile(zoneType, r));
950:             if(idx < roomFactions.size()) roomFactions.set(idx, spec.faction);
951:             stampRoomPurposeFeatures(rr, spec);
```

WorldRuntimeGenerationFramework.java near line 655 in `if` (method line 644):
```java
650:         else if(side==0) nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y-naveH, naveW, naveH);
651:         else nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y+corridor.height, naveW, naveH);
652:         ArrayList<Rectangle> candidateRooms = new ArrayList<>(); candidateRooms.add(nave);
653:         if(!moduleAreaLegal(corridor, candidateRooms)) return false;
654:         char cg = zoneType.corridorGlyph(r);
655:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
656:         Point pd = plazaDoorForModule(plaza, corridor);
657:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
658:         int idx = rooms.size();
659:         carve(nave); rooms.add(nave);
660:         RoomProfile rp = EcclesiarchyTempleApi.templeProfile(zoneType, r);
661:         if(idx < roomProfiles.size()) roomProfiles.set(idx, rp);
662:         if(idx < roomFactions.size()) roomFactions.set(idx, Faction.MINISTORUM);
663:         if(idx < roomSpecials.size()) roomSpecials.set(idx, Boolean.TRUE);
664:         Point d = doorBetweenRoomAndCorridor(nave, corridor);
665:         if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
666:         stampCultImperialisTempleFeatures(nave, horizontal);
```

WorldRuntimeGenerationFramework.java near line 651 in `if` (method line 644):
```java
646:         Rectangle corridor = horizontal ? new Rectangle(cx, cy, len, cw) : new Rectangle(cx, cy, cw, len);
647:         Rectangle nave;
648:         if(side==1) nave = new Rectangle(corridor.x+corridor.width, corridor.y+corridor.height/2-naveH/2, naveW, naveH);
649:         else if(side==3) nave = new Rectangle(corridor.x-naveW, corridor.y+corridor.height/2-naveH/2, naveW, naveH);
650:         else if(side==0) nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y-naveH, naveW, naveH);
651:         else nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y+corridor.height, naveW, naveH);
652:         ArrayList<Rectangle> candidateRooms = new ArrayList<>(); candidateRooms.add(nave);
653:         if(!moduleAreaLegal(corridor, candidateRooms)) return false;
654:         char cg = zoneType.corridorGlyph(r);
655:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
656:         Point pd = plazaDoorForModule(plaza, corridor);
657:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
658:         int idx = rooms.size();
659:         carve(nave); rooms.add(nave);
660:         RoomProfile rp = EcclesiarchyTempleApi.templeProfile(zoneType, r);
661:         if(idx < roomProfiles.size()) roomProfiles.set(idx, rp);
662:         if(idx < roomFactions.size()) roomFactions.set(idx, Faction.MINISTORUM);
```

WorldRuntimeGenerationFramework.java near line 648 in `if` (method line 644):
```java
643:         else if(side==3){ cx = plaza.x-len; cy = plaza.y+plaza.height/2-cw/2+offset; }
644:         else if(side==0){ cx = plaza.x+plaza.width/2-cw/2+offset; cy = plaza.y-len; }
645:         else { cx = plaza.x+plaza.width/2-cw/2+offset; cy = plaza.y+plaza.height; }
646:         Rectangle corridor = horizontal ? new Rectangle(cx, cy, len, cw) : new Rectangle(cx, cy, cw, len);
647:         Rectangle nave;
648:         if(side==1) nave = new Rectangle(corridor.x+corridor.width, corridor.y+corridor.height/2-naveH/2, naveW, naveH);
649:         else if(side==3) nave = new Rectangle(corridor.x-naveW, corridor.y+corridor.height/2-naveH/2, naveW, naveH);
650:         else if(side==0) nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y-naveH, naveW, naveH);
651:         else nave = new Rectangle(corridor.x+corridor.width/2-naveW/2, corridor.y+corridor.height, naveW, naveH);
652:         ArrayList<Rectangle> candidateRooms = new ArrayList<>(); candidateRooms.add(nave);
653:         if(!moduleAreaLegal(corridor, candidateRooms)) return false;
654:         char cg = zoneType.corridorGlyph(r);
655:         for(int x=corridor.x; x<corridor.x+corridor.width; x++) for(int y=corridor.y; y<corridor.y+corridor.height; y++) if(inBounds(x,y)) tiles[x][y]=cg;
656:         Point pd = plazaDoorForModule(plaza, corridor);
657:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
658:         int idx = rooms.size();
659:         carve(nave); rooms.add(nave);
```

MediaRuntimeFramework.java near line 62 in `load` (method line 47):
```java
57:             putAlias("floor_trash_mutant_rough", cellRoot + "/floors/floor_panels/floor_panels_r4c2.png");
58:             putAlias("floor_sewer_room", cellRoot + "/floors/floor_panels/floor_panels_r3c4.png");
59:             putAlias("floor_industrial_corridor", cellRoot + "/Corridors/corridors_a/corridors_a_r1c2.png");
60:             putAlias("floor_maintenance_corridor", cellRoot + "/Corridors/corridorsb/corridorsb_r2c3.png");
61:             putAlias("floor_alleyway_cracked", cellRoot + "/floors/floor_panels/floor_panels_r5c1.png");
62:             putAlias("floor_sewer_pipe_corridor", cellRoot + "/Corridors/corridors_a/corridors_a_r3c1.png");
63:             putAlias("floor_padded_service_way", cellRoot + "/Corridors/corridorsb/corridorsb_r4c4.png");
64:             putAlias("floor_exterior_hivewall_maintenance", cellRoot + "/Corridors/corridorsb/corridorsb_r1c4.png");
65:             putAlias("void_space", cellRoot + "/floors/floor_panels/floor_panels_r5c5.png");
66:             putAlias("wall_bulkhead", cellRoot + "/Walls/walls/walls_r1c1.png");
67:             putAlias("wall_support_beam", cellRoot + "/Walls/walls/walls_r1c4.png");
68:             putAlias("wall_gantry_lattice", cellRoot + "/Walls/walls/walls_r3c1.png");
69:             putAlias("wall_buried_conveyor", cellRoot + "/Walls/walls/walls_r2c5.png");
70:             putAlias("wall_pipe_bundle", cellRoot + "/Walls/walls/walls_r2c1.png");
71:             putAlias("wall_cable_column", cellRoot + "/Walls/walls/walls_r5c2.png");
72:             putAlias("door_archway", cellRoot + "/Doors/doors_o/doors_o_r1c1.png");
73:             putAlias("door_standard", cellRoot + "/Doors/coors_c/coors_c_r2c1.png");
```

Patch actions:
- Added circulation commit readiness checks and ready-or-fallback wrapper.

Added `Phase3CirculationCommitReadiness`, readiness checks, and `tryReadyPhase3TaggedCirculationCommit(...)`.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- verifying call-site readiness before migration,
- preventing blind road/corridor commit rewrites,
- preparing one safe commit-point migration.

What needs to be done next:
- wire the ready wrapper at one confirmed irreversible road/corridor commit point,
- preserve retry/fallback,
- run generation tests.

## 0.9.10ed — Phase 3 Circulation Commit Point Probe / Migration Wrapper

Continued Phase 3 from tagged circulation commit bridge.

Candidate road/corridor placement references observed:
WorldRuntimeGenerationFramework.java near line 6503:
```java
6499: 
6500:         if (candidateIsRoad) {
6501:             markPhase3GeneratedRoadRect(surfaceAuthority, x, y, width, height);
6502:         } else if (candidateIsCorridor) {
6503:             markPhase3GeneratedCorridorRect(surfaceAuthority, x, y, width, height);
6504:         } else {
6505:             phase3Debug("TaggedCirculationCommitReject", "candidate was neither road nor corridor");
6506:             return false;
6507:         }
6508: 
6509:         return true;
6510:     }
6511: 
6512:     public static boolean commitPhase3TaggedRoadCandidate(
```

WorldRuntimeGenerationFramework.java near line 6501:
```java
6497:             return false;
6498:         }
6499: 
6500:         if (candidateIsRoad) {
6501:             markPhase3GeneratedRoadRect(surfaceAuthority, x, y, width, height);
6502:         } else if (candidateIsCorridor) {
6503:             markPhase3GeneratedCorridorRect(surfaceAuthority, x, y, width, height);
6504:         } else {
6505:             phase3Debug("TaggedCirculationCommitReject", "candidate was neither road nor corridor");
6506:             return false;
6507:         }
6508: 
6509:         return true;
6510:     }
```

WorldRuntimeGenerationFramework.java near line 6217:
```java
6213: 
6214:         surfaceAuthority.markPhase3RoadRect(x, y, width, height);
6215:     }
6216: 
6217:     public static void markPhase3GeneratedCorridorRect(
6218:             MapLayerSurfaceAuthority surfaceAuthority,
6219:             int x,
6220:             int y,
6221:             int width,
6222:             int height) {
6223:         if (surfaceAuthority == null) {
6224:             phase3Debug("TileState", "Cannot mark corridor rect: surface authority is null");
6225:             return;
6226:         }
```

WorldRuntimeGenerationFramework.java near line 6203:
```java
6199:                 settings);
6200:     }
6201: 
6202: 
6203:     public static void markPhase3GeneratedRoadRect(
6204:             MapLayerSurfaceAuthority surfaceAuthority,
6205:             int x,
6206:             int y,
6207:             int width,
6208:             int height) {
6209:         if (surfaceAuthority == null) {
6210:             phase3Debug("TileState", "Cannot mark road rect: surface authority is null");
6211:             return;
6212:         }
```

WorldRuntimeGenerationFramework.java near line 6098:
```java
6094:     /**
6095:      * Phase 3 local grid-neighborhood scanner.
6096:      *
6097:      * This uses Phase 3 tile state as a conservative proxy for existing corridor/road
6098:      * floor bands. Later passes may replace the proxy with explicit road/corridor tags.
6099:      */
6100:     public static Phase3GridNeighborhoodMetrics scanPhase3GridNeighborhoodMetrics(
6101:             MapLayerSurfaceAuthority surfaceAuthority,
6102:             int x,
6103:             int y,
6104:             int dx,
6105:             int dy,
6106:             int scanRadius) {
6107: 
```

ItemEconomyFramework.java near line 1255:
```java
1251:         add(m,"Melta grenade","weapon/explosive/grenade/melta",52,"anti-armor lockers, Mechanicus sealed issue, rare wargear rooms","compact melta charge for objects too rude to remain solid","thrown melta explosive; high-heat breach profile",true);
1252:         add(m,"Plasma bomb","weapon/explosive/bomb/plasma",70,"Mechanicus sealed vaults, desperate Guard stores, noble black security","unstable plasma charge with a blast radius and a personnel problem","placed/thrown plasma explosive; unstable blast profile",true);
1253:         add(m,"Satchel charge","weapon/explosive/charge",42,"demolition stores, rebel caches, construction theft, Guard engineers","bagged demolition charge for walls, machines, and bad plans","placed explosive charge; breach profile",true);
1254:         add(m,"Tripwire mine","weapon/explosive/mine/tripwire",28,"gang ambush caches, militia stores, bad corridors","wire-triggered mine for hallway arguments","placed trap explosive; trap trigger profile",true);
1255:         add(m,"Motion claymore","weapon/explosive/mine/directional",48,"security hardpoints, noble kill corridors, Guard stores","directional motion-triggered mine with strong opinions about frontage","placed directional explosive; cone blast profile",true);
1256:         add(m,"Bouncing Betty","weapon/explosive/mine/bounding",44,"militia caches, old military stores, gang hardpoints","bounding fragmentation mine that rises before becoming everyone's problem","placed bounding explosive; trap trigger profile",true);
1257:         add(m,"Bolt round magazine","ammo/bolt",24,"officer lockers, Arbites evidence stores, noble security vaults","small magazine of bolt rounds with a provenance trail worth hiding","ammunition for bolt-family weapons",false);
1258:         add(m,"Autogun magazine","ammo/auto",7,"ganger caches, militia racks, pawn counters, security rooms","cheap solid-round magazine for autogun and autopistol families","ammunition for auto-family weapons",false);
1259:         add(m,"Stub cartridge box","ammo/stub",5,"hab drawers, pawn stalls, gang lockers, old security closets","box of solid-shot cartridges for stub weapons","ammunition for stub-family weapons",false);
1260:         add(m,"Needle toxin vial","ammo/toxin",18,"noble assassin stores, medicae black trade, chem shrines","small toxin charge for needle weapons and bad medical ethics","ammunition/reagent for needle-family weapons",false);
1261:         add(m,"Promethium canister","ammo/flame",20,"Guard stores, industrial hazard cages, cult theft, Arbites lockers","sealed fuel canister for flame weapons and other terrible plans","ammunition/fuel for flame-family weapons",false);
1262:         add(m,"Melta charge cell","ammo/melta",32,"anti-armor lockers, Mechanicus custody, rare wargear stores","charge cell for melta weapons; expensive enough to have enemies","ammunition for melta-family weapons",true);
1263:         add(m,"Plasma flask","ammo/plasma",34,"Mechanicus sealed stores, Guard special weapon cages, noble vaults","plasma charge flask with cooling marks and nervous handling tags","ammunition for plasma-family weapons",false);
1264:         add(m,"Web cartridge","ammo/security",16,"Arbites restraint lockers, noble capture teams, evidence cages","web-projector cartridge tagged for controlled issue","ammunition for webber/restraint weapons",false);
```

ZoneGenerationContext.java near line 7:
```java
3: /**
4:  * Phase 3 spatial-generation scaffolding.
5:  *
6:  * This context is intentionally metadata-only for the first implementation pass.
7:  * It does not replace room, road, corridor, plaza, or parking placement yet.
8:  *
9:  * Purpose:
10:  * - classify zone spatial family,
11:  * - derive roaded/roadless behavior,
12:  * - define central-anchor intent,
13:  * - define edge-band policy,
14:  * - expose validation profile,
15:  * - preserve a minimum edge safety band for hive exterior wall systems.
16:  */
```

WorldSimulationFramework.java near line 1755:
```java
1751:         return rows;
1752:     }
1753:     static String[] row(String purpose, String roomType, String productFocus, String people){ return new String[]{purpose, roomType, productFocus, people}; }
1754:     static String noteFor(ZoneType zt, String zoneName, Random r){
1755:         String[] notes = {"established after an old corridor claim was formalized", "expanded during a ration-pressure generation", "rebuilt after a local collapse", "retained because the current controller needs the room to keep functioning", "inherited from an earlier occupation and repainted rather than replaced"};
1756:         if(zt==ZoneType.MECHANICUS_FORGE_CLOISTER || zt==ZoneType.MECHANICUS_RELIC_DUCT) notes = new String[]{"consecrated over older utility machinery", "retained because the conduits still answer", "rebuilt under a maintenance covenant", "expanded after a sanctity audit"};
1757:         if(zt==ZoneType.NOBLE_SERVICE_SPINE || zt==ZoneType.SECTOR_GOVERNORS_MANSION) notes = new String[]{"lavishly appointed over older service decking", "expanded after household inheritance litigation", "kept beautiful enough to hide the machinery beneath", "staffed by inherited service lines"};
1758:         return notes[Math.floorMod(r.nextInt(), notes.length)] + " in " + WorldHistoryApi.shortZoneName(zoneName);
1759:     }
1760: }
1761: 
1762: class CampaignWorldApi {
1763:     static Path worldDir(){ return Paths.get("saves", "worlds"); }
1764:     static Path worldFile(HiveWorldDefinition d){ return worldDir().resolve(d.worldId + ".mechworld"); }
```

WorldRuntimeGenerationFramework.java near line 6456:
```java
6452:     /**
6453:      * Phase 3 tagged circulation commit bridge.
6454:      *
6455:      * This helper should be called only after a road/corridor candidate has passed
6456:      * placement validation. It marks the accepted candidate as explicit road/corridor
6457:      * circulation so future grid-suppression scans do not rely on generic floor tiles.
6458:      */
6459:     public static boolean commitPhase3TaggedRoadCorridorCandidate(
6460:             ZoneGenerationContext context,
6461:             MapLayerSurfaceAuthority surfaceAuthority,
6462:             Phase3GridSuppressionSettings gridSettings,
6463:             boolean candidateIsRoad,
6464:             boolean candidateIsCorridor,
6465:             int x,
```

WorldRuntimeGenerationFramework.java near line 6412:
```java
6408:         return reject;
6409:     }
6410: 
6411:     /**
6412:      * Combined candidate gate for future road/corridor placement call sites.
6413:      *
6414:      * Returns false when the candidate is unsafe or would intensify an unwanted
6415:      * grill pattern. Actual placement remains deferred to confirmed call sites.
6416:      */
6417:     public static boolean shouldAcceptPhase3TaggedRoadCorridorCandidate(
6418:             ZoneGenerationContext context,
6419:             MapLayerSurfaceAuthority surfaceAuthority,
6420:             Phase3GridSuppressionSettings gridSettings,
6421:             boolean candidateIsRoad,
```

WorldRuntimeGenerationFramework.java near line 5829:
```java
5825:      * Phase 3 central-biased fill scaffold.
5826:      *
5827:      * This is not a full WFC replacement for zone generation. It is a constrained
5828:      * post-plaza/post-road/post-corridor helper layer intended to fill productive
5829:      * space near the central plaza while respecting already-placed anchors, roads,
5830:      * corridors, edge bands, reserved traversal, and density targets.
5831:      */
5832:     public static Phase3CentralBiasedFillSettings createPhase3CentralBiasedFillSettings(
5833:             long zoneSeed,
5834:             int centerX,
5835:             int centerY,
5836:             int targetDensityPercent,
5837:             int productiveRadius) {
5838:         long derivedSeed = derivePhase3DeterministicSeed(zoneSeed, "central-biased-fill", centerX, centerY);
```

WorldRuntimeGenerationFramework.java near line 2689:
```java
2685:         for(int t=0; t<corridorTries; t++){
2686:             if(r.nextDouble() > 0.08) continue;
2687:             int x=2+r.nextInt(Math.max(1,w-4)), y=2+r.nextInt(Math.max(1,h-4));
2688:             if(tiles[x][y]=='=' || tiles[x][y]==':' || tiles[x][y]=='+'){
2689:                 // place on a neighboring wall/floor pocket if possible so corridor remains navigable
2690:                 int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}};
2691:                 for(int[] d:dirs){ int px=x+d[0], py=y+d[1]; if(inBounds(px,py) && tiles[px][py]=='#' && roomIds[px][py]<0){ char vg=vendingGlyphForZone(); tiles[px][py]=vg; mapObjects.add(MapObjectState.vending(px,py,vg,zoneType)); vending++; break; } }
2692:             }
2693:         }
2694:         DebugLog.audit("PASSIVE_FIXTURES", "zone="+zoneType.label+" vending="+vending+" shrines="+shrines+" layer="+layerText());
2695:     }
2696: 
2697:     double vendingChanceForZone(){
2698:         if(zoneType==ZoneType.NEUTRAL_RAIL_DEPOT || zoneType==ZoneType.SUMP_MARKET) return 0.55;
```

WorldRuntimeGenerationFramework.java near line 265:
```java
261:         // The user log from the frozen new-game run showed Sewer Conduit generation repeatedly
262:         // failing strict plaza acceptance with 3,000-attempt organic retries. That was not a
263:         // thrown exception; it was an unbounded-feeling generation churn. This fallback no
264:         // longer reuses the same organic branch proposer. It builds a deterministic connected
265:         // plaza lattice directly, then carves controlled corridors from the plaza to each room.
266:         target = WorldGenerationApi.clampRoomTarget(target);
267:         Rectangle plaza = centralPlazaRect();
268:         carve(plaza); rooms.add(plaza);
269:         if(!roomProfiles.isEmpty()) roomProfiles.set(0, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
270:         if(!roomFactions.isEmpty()) roomFactions.set(0, plazaFactionForZone());
271:         if(!roomSpecials.isEmpty()) roomSpecials.set(0, Boolean.TRUE);
272:         decorateCentralPlaza(plaza);
273:         stampCultImperialisTempleNearPlaza(plaza);
274:         seedCompassPlazaCorridors(plaza);
```

TheMechanist.java near line 9035:
```java
9031:             String zoneName = entry.substring(0, entry.length() - " Rooms".length());
9032:             ZoneType z = zoneByLabel(zoneName);
9033:             lines.add("Zone room set");
9034:             lines.add("Scope: describes the room pool for " + zoneName + ", not the whole zone and not a specific room instance.");
9035:             lines.add("Layout rule: rooms are enclosed first, doors are placed on valid room walls, and corridors attach outward from door points.");
9036:             lines.add("Room bias: " + (z==null?"generic underhive rooms":zoneGenerationRole(z)));
9037:             lines.add("Inspectable texture: " + (z==null?InspectableFeatureTable.sampleLine():InspectableFeatureTable.combinedFor(z, new Random(31), "sample").replace("sample; inspectable features: ", "")) + ".");
9038:             lines.add("Special hooks: traders, locked caches, faction access, machinery, shrines, vending machines, or encounter logic are selected from this zone's room pool.");
9039:         } else if (entry.contains(" Generic: ")) {
9040:             String zoneName = entry.substring(0, entry.indexOf(" Generic: ")).trim();
9041:             String name = entry.substring(entry.indexOf(" Generic: ") + " Generic: ".length()).trim();
9042:             ZoneType z = zoneByLabel(zoneName);
9043:             lines.add("Generic room sample");
9044:             lines.add("Zone context: " + zoneName + " — " + (z==null?"generic room pool":zoneGenerationRole(z)) + ".");
```

Patch actions:
- Added tagged circulation migration wrapper for future road/corridor commit call sites.

Added `tryPhase3TaggedCirculationCommitOrLegacyFallback(...)`, a conservative migration wrapper for future road/corridor placement call sites.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing safe migration to tagged road/corridor commits,
- avoiding blind generator rewrites,
- preserving retry/fallback behavior.

What needs to be done next:
- inspect strongest commit candidates,
- wire wrapper at exact irreversible commit points only.

## 0.9.10ec — Phase 3 Tagged Circulation Commit Bridge

Continued Phase 3 from tagged road/corridor candidate scoring.

Patch actions:
- Added tagged road/corridor commit bridge and convenience wrappers.

Added a tagged circulation commit bridge and road/corridor convenience wrappers.

The bridge validates candidate acceptance and then marks explicit road/corridor tags only after acceptance.

Candidate commit references observed:
WorldRuntimeGenerationFramework.java near line 6549:
```java
6546:             int dx,
6547:             int dy,
6548:             int scanRadius) {
6549:         return commitPhase3TaggedRoadCorridorCandidate(
6550:                 context,
6551:                 surfaceAuthority,
6552:                 gridSettings,
6553:                 false,
6554:                 true,
6555:                 x,
6556:                 y,
```

WorldRuntimeGenerationFramework.java near line 6523:
```java
6520:             int dx,
6521:             int dy,
6522:             int scanRadius) {
6523:         return commitPhase3TaggedRoadCorridorCandidate(
6524:                 context,
6525:                 surfaceAuthority,
6526:                 gridSettings,
6527:                 true,
6528:                 false,
6529:                 x,
6530:                 y,
```

WorldRuntimeGenerationFramework.java near line 6459:
```java
6456:      * placement validation. It marks the accepted candidate as explicit road/corridor
6457:      * circulation so future grid-suppression scans do not rely on generic floor tiles.
6458:      */
6459:     public static boolean commitPhase3TaggedRoadCorridorCandidate(
6460:             ZoneGenerationContext context,
6461:             MapLayerSurfaceAuthority surfaceAuthority,
6462:             Phase3GridSuppressionSettings gridSettings,
6463:             boolean candidateIsRoad,
6464:             boolean candidateIsCorridor,
6465:             int x,
6466:             int y,
```

ZoneGenerationContext.java near line 7:
```java
4:  * Phase 3 spatial-generation scaffolding.
5:  *
6:  * This context is intentionally metadata-only for the first implementation pass.
7:  * It does not replace room, road, corridor, plaza, or parking placement yet.
8:  *
9:  * Purpose:
10:  * - classify zone spatial family,
11:  * - derive roaded/roadless behavior,
12:  * - define central-anchor intent,
13:  * - define edge-band policy,
14:  * - expose validation profile,
```

WorldRuntimeGenerationFramework.java near line 6538:
```java
6535:                 scanRadius);
6536:     }
6537: 
6538:     public static boolean commitPhase3TaggedCorridorCandidate(
6539:             ZoneGenerationContext context,
6540:             MapLayerSurfaceAuthority surfaceAuthority,
6541:             Phase3GridSuppressionSettings gridSettings,
6542:             int x,
6543:             int y,
6544:             int width,
6545:             int height,
```

WorldRuntimeGenerationFramework.java near line 6512:
```java
6509:         return true;
6510:     }
6511: 
6512:     public static boolean commitPhase3TaggedRoadCandidate(
6513:             ZoneGenerationContext context,
6514:             MapLayerSurfaceAuthority surfaceAuthority,
6515:             Phase3GridSuppressionSettings gridSettings,
6516:             int x,
6517:             int y,
6518:             int width,
6519:             int height,
```

WorldRuntimeGenerationFramework.java near line 6505:
```java
6502:         } else if (candidateIsCorridor) {
6503:             markPhase3GeneratedCorridorRect(surfaceAuthority, x, y, width, height);
6504:         } else {
6505:             phase3Debug("TaggedCirculationCommitReject", "candidate was neither road nor corridor");
6506:             return false;
6507:         }
6508: 
6509:         return true;
6510:     }
6511: 
6512:     public static boolean commitPhase3TaggedRoadCandidate(
```

WorldRuntimeGenerationFramework.java near line 6503:
```java
6500:         if (candidateIsRoad) {
6501:             markPhase3GeneratedRoadRect(surfaceAuthority, x, y, width, height);
6502:         } else if (candidateIsCorridor) {
6503:             markPhase3GeneratedCorridorRect(surfaceAuthority, x, y, width, height);
6504:         } else {
6505:             phase3Debug("TaggedCirculationCommitReject", "candidate was neither road nor corridor");
6506:             return false;
6507:         }
6508: 
6509:         return true;
6510:     }
```

WorldRuntimeGenerationFramework.java near line 6501:
```java
6498:         }
6499: 
6500:         if (candidateIsRoad) {
6501:             markPhase3GeneratedRoadRect(surfaceAuthority, x, y, width, height);
6502:         } else if (candidateIsCorridor) {
6503:             markPhase3GeneratedCorridorRect(surfaceAuthority, x, y, width, height);
6504:         } else {
6505:             phase3Debug("TaggedCirculationCommitReject", "candidate was neither road nor corridor");
6506:             return false;
6507:         }
6508: 
```

WorldRuntimeGenerationFramework.java near line 6456:
```java
6453:      * Phase 3 tagged circulation commit bridge.
6454:      *
6455:      * This helper should be called only after a road/corridor candidate has passed
6456:      * placement validation. It marks the accepted candidate as explicit road/corridor
6457:      * circulation so future grid-suppression scans do not rely on generic floor tiles.
6458:      */
6459:     public static boolean commitPhase3TaggedRoadCorridorCandidate(
6460:             ZoneGenerationContext context,
6461:             MapLayerSurfaceAuthority surfaceAuthority,
6462:             Phase3GridSuppressionSettings gridSettings,
6463:             boolean candidateIsRoad,
```

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing road/corridor placement call sites to use tag-aware scoring,
- ensuring accepted circulation is explicitly tagged.

What needs to be done next:
- wire exact road/corridor placement commit points,
- replace generic floor marking where safe.

## 0.9.10eb — Phase 3 Tagged Road/Corridor Candidate Scoring

Continued Phase 3 from explicit road/corridor tile tagging.

Patch actions:
- Added tag-aware road/corridor candidate scoring and acceptance gate.

Added tag-aware candidate scoring and rejection helpers that use explicit road/corridor tags to avoid counting generic traversable room/plaza floor as circulation-grid structure.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing road/corridor candidate selection to avoid grill patterns,
- using explicit circulation tags for accurate metrics.

What needs to be done next:
- wire road and corridor placement candidates to the acceptance gate,
- mark accepted circulation tiles with explicit tags.

## 0.9.10ea — Phase 3 Road/Corridor Tile Tagging

Continued Phase 3 from grid neighborhood scanning.

Patch actions:
- Added phase3RoadTiles and phase3CorridorTiles fields.
- Initialized road/corridor tag arrays.
- Added road/corridor tile tagging helpers.
- Added runtime wrappers for road/corridor tile marking.
- Added tag-aware road/corridor grid neighborhood scanner.

Added explicit road/corridor tile tag arrays and helpers on `MapLayerSurfaceAuthority`.

Added runtime wrappers for marking generated road/corridor rectangles and a tag-aware grid-neighborhood scanner.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- separating roads/corridors from generic traversable floor,
- making grill suppression more accurate.

What needs to be done next:
- wire road/corridor placement to tag marking,
- use tag-aware scanner in candidate scoring.

## 0.9.10dz — Phase 3 Grid Neighborhood Scanning

Continued Phase 3 from corridor/road grid suppression scaffold.

Patch actions:
- Added local grid-neighborhood metrics scanner and candidate rejection bridge.

Added `Phase3GridNeighborhoodMetrics`, neighborhood scanning helpers, and a bridge that feeds local metrics into existing grid-suppression rejection logic.

The scanner currently uses Phase 3 traversable tile state as a conservative proxy for road/corridor bands.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- adding local measurement for grill-pattern suppression,
- preparing road/corridor candidate selection to use anti-repetition scoring.

What needs to be done next:
- add explicit road/corridor tile tagging,
- wire scanner into candidate selection.

## 0.9.10dy — Phase 3 Corridor/Road Grid Suppression Scaffold

Continued Phase 3 from central-biased WFC-compatible scaffold.

Design note:
A WFC-style constrained fill layer can help prevent sandwich-grill/infinite-grid road and corridor patterns by penalizing repetitive parallel candidates before placement.

Patch actions:
- Added grid-suppression scaffold for road/corridor repetition control.

Added:
- `Phase3GridSuppressionSettings`
- default grid suppression settings
- repetition penalty scoring
- candidate rejection helper
- central-fill score blended with grid suppression

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing road/corridor generation to avoid sterile repeated grids,
- blending central productive fill with anti-repetition scoring.

What needs to be done next:
- add neighborhood scanning,
- measure parallel runs and spacing,
- wire scoring into candidate selection.

## 0.9.10dx — Phase 3 Central-Biased Constrained Fill / WFC-Compatible Scaffold

Continued Phase 3 from spacer corridor retry planning and WFC feasibility discussion.

Design conclusion:
WFC-style generation is possible, but should initially be implemented as a constrained central-biased fill layer rather than replacing the whole zone generator.

Patch actions:
- Added central-biased constrained-fill / WFC-compatible scaffold and deterministic seed helpers.

Added:
- `Phase3CentralBiasedFillMode`
- `Phase3CentralBiasedFillSettings`
- deterministic seed derivation helper
- central bias score helper
- candidate cell safety helper

Seed strategy:
derive stable subsystem seeds from the base zone seed, phase/layer salt, and coordinates so generation remains reproducible and local changes do not destabilize earlier phases.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing WFC-compatible central fill without replacing the generator,
- biasing productive fill toward the central plaza,
- preserving deterministic seed behavior.

What needs to be done next:
- implement candidate patch selection loop,
- feed candidates into existing validation gates,
- maintain density target and pathability validation.

## 0.9.10dw — Phase 3 Spacer Corridor Retry Planner

Continued Phase 3 from spacer corridor validation bridge.

Patch actions:
- Added spacer corridor retry planner, direction ordering, and apply helper.

Added `Phase3SpacerCorridorRetryPlan`, candidate direction ordering, retry planning, and apply helper.

The planner searches short 2–4 tile spacer corridors from a failed room attachment point and returns a retry anchor beyond the spacer endpoint.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing failed room attachment handling to try spacer corridors,
- preserving smaller-room and closet fallback,
- avoiding uncontrolled long corridor generation.

What needs to be done next:
- wire retry planner into failed attachment loop,
- retry room placement from spacer endpoint.

## 0.9.10dv — Phase 3 Spacer Corridor Validation Bridge

Continued Phase 3 from room attachment spacer-corridor fallback planning.

Patch actions:
- Added spacer corridor validation bridge and floor-marking helper.

Added `Phase3SpacerCorridorValidation`, spacer corridor candidate validation, and spacer corridor floor-marking helper.

Spacer corridors are now constrained to orthogonal 2–4 tile candidates that must respect safe interior bounds, reserved traversal, and blocked tile state when available.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- validating spacer corridors before fallback placement,
- preparing room attachment failure handling to retry from spacer endpoints.

What needs to be done next:
- wire spacer validation into failed attachment handling,
- generate candidate directions,
- retry room placement from accepted spacer endpoint.

## 0.9.10du — Phase 3 Room Attachment Spacer Corridor Fallback

Continued Phase 3 from room corrective-action dispatch.

Patch actions:
- Added room attachment fallback planner with 2–4 tile spacer corridor fallback.

Added `Phase3RoomAttachmentFallback`, `Phase3RoomAttachmentFallbackPlan`, and helpers for planning failed room attachment fallback.

Fallback chain now includes:
- retry original attachment,
- insert 2–4 tile spacer corridor,
- try smaller room type,
- place corridor-stub closet,
- deny invalid location only after fallback exhaustion.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preventing premature denial of blocked room attachments,
- adding spacer corridors as a controlled relief mechanism,
- preserving final denial only after fallback options are exhausted.

What needs to be done next:
- wire fallback planner into attachment failure handling,
- validate spacer corridor generation,
- retry room placement from spacer endpoint.

## 0.9.10dt — Phase 3 Room Corrective Action Dispatcher

Continued Phase 3 from room feature commit guard.

Patch actions:
- Added Phase3RoomCorrectiveActionDispatch and corrective-action dispatcher helpers.

Added `Phase3RoomCorrectiveActionDispatch` and dispatcher helpers to route failed room feature-density commit guards into controlled corrective actions.

The dispatcher records:
- whether correction should be attempted,
- whether commit must be aborted,
- whether revalidation is required,
- correction type,
- and note.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing controlled handling of rejected room-density commits,
- preserving non-destructive behavior,
- requiring revalidation after correction.

What needs to be done next:
- wire dispatcher at confirmed commit point,
- implement actual corrective actions,
- revalidate before commit.

## 0.9.10ds — Phase 3 Room Feature Commit Guard

Continued Phase 3 from the room feature action-response bridge.

Patch actions:
- Added final room commit guard helpers for Phase 3 feature-density directives.

Added final commit guard helpers:
- `canCommitPhase3RoomAfterDirective(...)`
- `canCommitPhase3RoomAfterFeatureValidation(...)`

Commit candidates observed:
RoomProfile.java near line 107:
```java
103:         return rp;
104:     }
105: 
106:     static RoomProfile themedRoom(String name, String descriptor, int chance, Faction faction, String[] loot, char[] contents){
107:         return new RoomProfile(name, descriptor, chance, faction, loot, contents);
108:     }
109: 
110:     static void addInfrastructureRooms(ArrayList<RoomProfile> list, ZoneType z){
111:         // 0.8.61 FACTION INFRASTRUCTURE LIBRARY:
112:         // Each zone now receives its own habitation, kitchen/mess, food storehouse, product
113:         // warehouse, storefront/counter, clinic, workshop, security, sanitation, utility,
114:         // and shrine/social pattern. Similar civic functions must not share identical text
115:         // across factions: a noble dormitory, a civilian dormitory, a Guard barracks, and a
```

RoomProfile.java near line 96:
```java
92:         RoomProfile rp = new RoomProfile(name, desc, 65, fac, loot, contents);
93:         rp.featureText = desc + "; all branch corridors originate from or connect back to this plaza so the zone has a guaranteed accessible nexus.";
94:         return rp;
95:     }
96:     static RoomProfile generic(){ return new RoomProfile("Generic underhive room","unremarkable misery with structural opinions",30,Faction.NONE,new String[]{"scrap bundle","bent nail tin","emergency ration"},new char[]{'p','b','N'}); }
97: 
98:     static RoomProfile neutralContestRoom(ZoneType z, Random r){
99:         String[] names = {"Neutral Empty Room", "Vacant Claim Room", "Unassigned Side Chamber", "Empty Service Lease", "Contestable Utility Room"};
100:         String name = names[(r == null ? 0 : r.nextInt(names.length))];
101:         RoomProfile rp = new RoomProfile(name, "an intentionally unoccupied neutral room left between faction claims; bare floor, stripped fixtures, old paper, used tins, and just enough empty space for possible occupation, ambush, squatters, or room-control schemes", 16, Faction.NONE, new String[]{"Old INN newspaper","Used food tin","cloth scraps","paper mush","Vended scrap"}, new char[]{'o','b','q'});
102:         rp.featureText = "Contestable neutral room: currently unclaimed, lightly littered, and suitable for faction takeover, player occupation, hidden meetings, bank-heist staging, or evidence drops.";
103:         return rp;
104:     }
```

RoomManifestApi.java near line 158:
```java
154:     private static StampedModuleSpec module(String name, String dress, int len, int cw, int rw, int rh, int preferredSide, int laneOffset, StampedRoomSpec... rooms){
155:         return new StampedModuleSpec(name, dress, len, cw, rw, rh, preferredSide, laneOffset, rooms);
156:     }
157:     private static StampedRoomSpec spec(String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){
158:         return new StampedRoomSpec(kind, name, desc, chance, faction, loot, contents);
159:     }
160: 
161:     private static void addDefault(ArrayList<StampedRoomSpec> out, ZoneType z){
162:         Faction f = factionFor(z);
163:         add(out,"DORMITORY",z.label+" Dormitory Variant","zone-specific habitation room shaped by "+z.descriptor+"; this is not a generic dormitory but the local answer to sleep, crowding, and control",34,f,new String[]{"cloth scraps","sealed water ration","cheap trinket"},new char[]{'c','s'});
164:         add(out,"CAFETERIA",z.label+" Eating Hall Variant","zone-specific cafeteria or mess: long eating surfaces, benches, refuse bins, and a service counter adapted to the faction occupying this space",36,f,new String[]{"cheap meal tin","sealed water ration","dirty water"},new char[]{'T','b','N'});
165:         add(out,"FOOD_STORE",z.label+" Food Storehouse","food, water, and ration storage distinct from product warehousing; shelves and crates reflect the local faction economy",40,f,new String[]{"sealed water ration","emergency ration","cheap meal tin"},new char[]{'r','b'});
166:         add(out,"WAREHOUSE",z.label+" Product Warehouse","non-food product storage: salvage, tools, cargo, components, contraband, or manufactured stock depending on the zone",42,f,new String[]{"machine parts","supply crate scrap","trade chit"},new char[]{'b','N'});
```

WorldRuntimeGenerationFramework.java near line 3097:
```java
3093:         for(Rectangle rect: candidates){
3094:             if(made >= desired) break;
3095:             if(!canPlaceInterwallRoom(world, rect, false)) continue;
3096:             Faction f = dangerFaction(world, r, made);
3097:             RoomProfile rp = new RoomProfile(
3098:                 "Abandoned Interwall Danger Room",
3099:                 "scrap-choked forgotten chamber between the active zone and the outer hive wall; old collapse dust, abandoned reserves, and something dangerous have been sealed here too long",
3100:                 82, f,
3101:                 new String[]{"forgotten weapon cache","old armor bundle","tech salvage","sealed reserve crate","wanted poster scrap"},
3102:                 new char[]{'!','?','*','b'});
3103:             int rid = carveAddedRoom(world, rect, rp, f, true);
3104:             if(rid >= 0){ connectDangerRoomToLoop(world, rect, inset); made++; }
3105:         }
```

WorldRuntimeGenerationFramework.java near line 3069:
```java
3065:             new Rectangle(world.w-inset-rh-1, world.h/2-rw/2, rh, rw)
3066:         };
3067:         for(Rectangle rect: candidates){
3068:             if(!canPlaceInterwallRoom(world, rect, true)) continue;
3069:             int rid = carveAddedRoom(world, rect, new RoomProfile(
3070:                 "Hivewall Maintenance Room",
3071:                 "bolted-on high-wall service room attached after the normal zone was built; one door returns to the main hive, while left and right maintenance exits run into the exterior wall loop",
3072:                 58, Faction.NONE,
3073:                 new String[]{"machine parts","wire bundle","rusted tool","sealed water ration"},
3074:                 new char[]{'N','q','b'}), Faction.NONE, true);
3075:             connectRoomToLoop(world, rect, inset);
3076:             connectRoomToNearestInteriorCorridor(world, rect);
3077:             return rid >= 0;
```

RoomProfile.java near line 326:
```java
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
333:     char contentSymbol(Random r){ return contents[r.nextInt(contents.length)]; }
334: }
```

RoomProfile.java near line 325:
```java
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
333:     char contentSymbol(Random r){ return contents[r.nextInt(contents.length)]; }
```

RoomProfile.java near line 324:
```java
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
```

RoomProfile.java near line 323:
```java
319:             list.add(new RoomProfile("Atmospheric Condenser Cell","Martian condenser room: damp coils, intake vanes, and emergency water recovery hardware",44,Faction.MECHANICUS,new String[]{"water bottle","filter cartridge","machine parts"},new char[]{'Y','N','q'}));
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
```

RoomProfile.java near line 322:
```java
318:         if(z==ZoneType.MECHANICUS_FORGE_CLOISTER || z==ZoneType.MECHANICUS_RELIC_DUCT){
319:             list.add(new RoomProfile("Atmospheric Condenser Cell","Martian condenser room: damp coils, intake vanes, and emergency water recovery hardware",44,Faction.MECHANICUS,new String[]{"water bottle","filter cartridge","machine parts"},new char[]{'Y','N','q'}));
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
```

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- creating the final safety gate before room commit,
- preventing invalid room commits,
- preserving corrective action and revalidation.

What needs to be done next:
- wire this guard into the exact room commit point,
- route failed guards to directive handling.

## 0.9.10dr — Phase 3 Room Feature Action Response Bridge

Continued Phase 3 from room feature action-handler scaffolding.

Patch actions:
- Added Phase3RoomGenerationDirective and action-response bridge helpers.

Added `Phase3RoomGenerationDirective` and helpers to convert feature-density action plans/results into one generator-facing directive.

The directive expresses whether a room may commit or whether it must expand, choose a larger stamp, reduce features, retry, or reject.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing the final bridge before room profile/stamp commit wiring,
- preventing invalid rooms from committing after corrective-action decisions,
- keeping all corrective actions explicit.

What needs to be done next:
- wire the directive at the confirmed commit point,
- block commit unless mayCommit is true,
- implement directive actions and revalidation.

## 0.9.10dq — Phase 3 Room Feature Action Handler Scaffold

Continued Phase 3 from room feature resolution action planning.

Patch actions:
- Added Phase3RoomFeatureActionResult and action-handler scaffold.

Added `Phase3RoomFeatureActionResult` and action-handler helpers to convert room feature-density action plans into explicit generator results.

Commit candidates observed:
RoomProfile.java near line 107:
```java
104:     }
105: 
106:     static RoomProfile themedRoom(String name, String descriptor, int chance, Faction faction, String[] loot, char[] contents){
107:         return new RoomProfile(name, descriptor, chance, faction, loot, contents);
108:     }
109: 
110:     static void addInfrastructureRooms(ArrayList<RoomProfile> list, ZoneType z){
111:         // 0.8.61 FACTION INFRASTRUCTURE LIBRARY:
112:         // Each zone now receives its own habitation, kitchen/mess, food storehouse, product
113:         // warehouse, storefront/counter, clinic, workshop, security, sanitation, utility,
114:         // and shrine/social pattern. Similar civic functions must not share identical text
```

RoomProfile.java near line 96:
```java
93:         rp.featureText = desc + "; all branch corridors originate from or connect back to this plaza so the zone has a guaranteed accessible nexus.";
94:         return rp;
95:     }
96:     static RoomProfile generic(){ return new RoomProfile("Generic underhive room","unremarkable misery with structural opinions",30,Faction.NONE,new String[]{"scrap bundle","bent nail tin","emergency ration"},new char[]{'p','b','N'}); }
97: 
98:     static RoomProfile neutralContestRoom(ZoneType z, Random r){
99:         String[] names = {"Neutral Empty Room", "Vacant Claim Room", "Unassigned Side Chamber", "Empty Service Lease", "Contestable Utility Room"};
100:         String name = names[(r == null ? 0 : r.nextInt(names.length))];
101:         RoomProfile rp = new RoomProfile(name, "an intentionally unoccupied neutral room left between faction claims; bare floor, stripped fixtures, old paper, used tins, and just enough empty space for possible occupation, ambush, squatters, or room-control schemes", 16, Faction.NONE, new String[]{"Old INN newspaper","Used food tin","cloth scraps","paper mush","Vended scrap"}, new char[]{'o','b','q'});
102:         rp.featureText = "Contestable neutral room: currently unclaimed, lightly littered, and suitable for faction takeover, player occupation, hidden meetings, bank-heist staging, or evidence drops.";
103:         return rp;
```

RoomManifestApi.java near line 158:
```java
155:         return new StampedModuleSpec(name, dress, len, cw, rw, rh, preferredSide, laneOffset, rooms);
156:     }
157:     private static StampedRoomSpec spec(String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){
158:         return new StampedRoomSpec(kind, name, desc, chance, faction, loot, contents);
159:     }
160: 
161:     private static void addDefault(ArrayList<StampedRoomSpec> out, ZoneType z){
162:         Faction f = factionFor(z);
163:         add(out,"DORMITORY",z.label+" Dormitory Variant","zone-specific habitation room shaped by "+z.descriptor+"; this is not a generic dormitory but the local answer to sleep, crowding, and control",34,f,new String[]{"cloth scraps","sealed water ration","cheap trinket"},new char[]{'c','s'});
164:         add(out,"CAFETERIA",z.label+" Eating Hall Variant","zone-specific cafeteria or mess: long eating surfaces, benches, refuse bins, and a service counter adapted to the faction occupying this space",36,f,new String[]{"cheap meal tin","sealed water ration","dirty water"},new char[]{'T','b','N'});
165:         add(out,"FOOD_STORE",z.label+" Food Storehouse","food, water, and ration storage distinct from product warehousing; shelves and crates reflect the local faction economy",40,f,new String[]{"sealed water ration","emergency ration","cheap meal tin"},new char[]{'r','b'});
```

WorldRuntimeGenerationFramework.java near line 3097:
```java
3094:             if(made >= desired) break;
3095:             if(!canPlaceInterwallRoom(world, rect, false)) continue;
3096:             Faction f = dangerFaction(world, r, made);
3097:             RoomProfile rp = new RoomProfile(
3098:                 "Abandoned Interwall Danger Room",
3099:                 "scrap-choked forgotten chamber between the active zone and the outer hive wall; old collapse dust, abandoned reserves, and something dangerous have been sealed here too long",
3100:                 82, f,
3101:                 new String[]{"forgotten weapon cache","old armor bundle","tech salvage","sealed reserve crate","wanted poster scrap"},
3102:                 new char[]{'!','?','*','b'});
3103:             int rid = carveAddedRoom(world, rect, rp, f, true);
3104:             if(rid >= 0){ connectDangerRoomToLoop(world, rect, inset); made++; }
```

WorldRuntimeGenerationFramework.java near line 3069:
```java
3066:         };
3067:         for(Rectangle rect: candidates){
3068:             if(!canPlaceInterwallRoom(world, rect, true)) continue;
3069:             int rid = carveAddedRoom(world, rect, new RoomProfile(
3070:                 "Hivewall Maintenance Room",
3071:                 "bolted-on high-wall service room attached after the normal zone was built; one door returns to the main hive, while left and right maintenance exits run into the exterior wall loop",
3072:                 58, Faction.NONE,
3073:                 new String[]{"machine parts","wire bundle","rusted tool","sealed water ration"},
3074:                 new char[]{'N','q','b'}), Faction.NONE, true);
3075:             connectRoomToLoop(world, rect, inset);
3076:             connectRoomToNearestInteriorCorridor(world, rect);
```

RoomProfile.java near line 326:
```java
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
333:     char contentSymbol(Random r){ return contents[r.nextInt(contents.length)]; }
```

RoomProfile.java near line 325:
```java
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
```

RoomProfile.java near line 324:
```java
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
```

RoomProfile.java near line 323:
```java
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
```

RoomProfile.java near line 322:
```java
319:             list.add(new RoomProfile("Atmospheric Condenser Cell","Martian condenser room: damp coils, intake vanes, and emergency water recovery hardware",44,Faction.MECHANICUS,new String[]{"water bottle","filter cartridge","machine parts"},new char[]{'Y','N','q'}));
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
```

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- giving the future commit point clear action results,
- preventing silent acceptance of invalid rooms,
- preparing actual expand/larger-stamp/reduce/retry/reject wiring.

What needs to be done next:
- wire the handler into the confirmed room profile/stamp commit point,
- implement the actual result behavior.

## 0.9.10dp — Phase 3 Room Feature Resolution Action Planning

Continued Phase 3 from room feature resolution commit bridge.

Patch actions:
- Added Phase3RoomFeatureActionPlan and resolution action planning helpers.

Added `Phase3RoomFeatureActionPlan` and helper methods to convert room feature-density commit decisions into actionable generator plans.

Action plans can express accept, expand, choose larger stamp, reduce features, downgrade feature set, retry placement, or reject room.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing actual response handling for overburdened rooms,
- keeping the generator response explicit,
- avoiding silent acceptance or silent feature deletion.

What needs to be done next:
- wire action planning into the confirmed room profile/stamp commit point,
- implement action handlers,
- preserve edge-band and traversal protections.

## 0.9.10do — Phase 3 Room Feature Resolution Commit Bridge

Continued Phase 3 from room feature-load resolution policy.

Commit/selection candidates observed:
RoomManifestApi.java near line 185:
```java
182:             default: return Faction.NONE;
183:         }
184:     }
185:     private static void add(ArrayList<StampedRoomSpec> out, String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){ out.add(new StampedRoomSpec(kind,name,desc,chance,faction,loot,contents)); }
186: }
```

RoomManifestApi.java near line 158:
```java
155:         return new StampedModuleSpec(name, dress, len, cw, rw, rh, preferredSide, laneOffset, rooms);
156:     }
157:     private static StampedRoomSpec spec(String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){
158:         return new StampedRoomSpec(kind, name, desc, chance, faction, loot, contents);
159:     }
160: 
161:     private static void addDefault(ArrayList<StampedRoomSpec> out, ZoneType z){
162:         Faction f = factionFor(z);
163:         add(out,"DORMITORY",z.label+" Dormitory Variant","zone-specific habitation room shaped by "+z.descriptor+"; this is not a generic dormitory but the local answer to sleep, crowding, and control",34,f,new String[]{"cloth scraps","sealed water ration","cheap trinket"},new char[]{'c','s'});
164:         add(out,"CAFETERIA",z.label+" Eating Hall Variant","zone-specific cafeteria or mess: long eating surfaces, benches, refuse bins, and a service counter adapted to the faction occupying this space",36,f,new String[]{"cheap meal tin","sealed water ration","dirty water"},new char[]{'T','b','N'});
165:         add(out,"FOOD_STORE",z.label+" Food Storehouse","food, water, and ration storage distinct from product warehousing; shelves and crates reflect the local faction economy",40,f,new String[]{"sealed water ration","emergency ration","cheap meal tin"},new char[]{'r','b'});
```

WorldRuntimeGenerationFramework.java near line 4625:
```java
4622:                 intendedFeatureCount,
4623:                 blockingFeatureCount,
4624:                 interactiveFeatureCount);
4625:         return shouldAcceptPhase3RoomProfileOrStamp(roomWidth, roomHeight, estimatedLoad, roomLabel);
4626:     }
4627: 
4628: 
4629:     public enum Phase3RoomFeatureResolution {
4630:         ACCEPT,
4631:         EXPAND_ROOM,
4632:         CHOOSE_LARGER_STAMP,
```

WorldRuntimeGenerationFramework.java near line 2872:
```java
2869:             RoomProfile rp=roomProfiles.get(i); Faction owner=roomFaction(i);
2870:             out.add("room " + i + ": " + (roomSpecials.size()>i && roomSpecials.get(i)?"SPECIAL ":"") + rp.name + " | owner " + owner.label + " | loot " + rp.scavengeChance + "% | " + rp.descriptor + " | features: " + rp.featureText);
2871:         }
2872:         if(roomProfiles.size()>18) out.add("... " + (roomProfiles.size()-18) + " more rooms exist in this zone.");
2873:         return out;
2874:     }
2875: }
2876: 
2877: 
2878: class HivewallRoomCacheApi {
2879:     private HivewallRoomCacheApi() {}
```

WorldRuntimeGenerationFramework.java near line 1652:
```java
1649:     }
1650: 
1651:     boolean regionTouchesNonWall(Rectangle rr){ for(int x=Math.max(0,rr.x); x<Math.min(w,rr.x+rr.width); x++) for(int y=Math.max(0,rr.y); y<Math.min(h,rr.y+rr.height); y++) if(roomIds[x][y] >= 0 || tiles[x][y]!='#') return true; return false; }
1652:     void carve(Rectangle rr){ int id=rooms.size(); RoomProfile rp = RoomProfile.forZone(zoneType, r); roomProfiles.add(rp); roomFactions.add(Faction.NONE); roomSpecials.add(Boolean.FALSE); char floorChar = zoneType.floorGlyph(r); for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++){ roomIds[x][y]=id; boolean wall = (x==rr.x || y==rr.y || x==rr.x+rr.width-1 || y==rr.y+rr.height-1); tiles[x][y]= wall ? '#' : floorChar; } }
1653: 
1654:     void buildGuaranteedRoomLattice(int target){
1655:         target = WorldGenerationApi.clampRoomTarget(target);
1656:         int cols = 6;
1657:         int rows = 5;
1658:         int cellW = Math.max(10, (w-4) / cols);
1659:         int cellH = Math.max(8, (h-4) / rows);
```

RoomProfile.java near line 326:
```java
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
333:     char contentSymbol(Random r){ return contents[r.nextInt(contents.length)]; }
```

RoomProfile.java near line 325:
```java
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
```

RoomProfile.java near line 324:
```java
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
```

RoomProfile.java near line 323:
```java
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
```

RoomProfile.java near line 322:
```java
319:             list.add(new RoomProfile("Atmospheric Condenser Cell","Martian condenser room: damp coils, intake vanes, and emergency water recovery hardware",44,Faction.MECHANICUS,new String[]{"water bottle","filter cartridge","machine parts"},new char[]{'Y','N','q'}));
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
```

RoomProfile.java near line 321:
```java
318:         if(z==ZoneType.MECHANICUS_FORGE_CLOISTER || z==ZoneType.MECHANICUS_RELIC_DUCT){
319:             list.add(new RoomProfile("Atmospheric Condenser Cell","Martian condenser room: damp coils, intake vanes, and emergency water recovery hardware",44,Faction.MECHANICUS,new String[]{"water bottle","filter cartridge","machine parts"},new char[]{'Y','N','q'}));
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
```

RoomProfile.java near line 320:
```java
317:         list.add(new RoomProfile("Dead-end storage closet", "a short local storage pocket holding whatever this faction forgets, hides, or hoards", 48, Faction.NONE, new String[]{"supply crate scrap","trade chit","machine parts","sealed water ration"}, new char[]{'b','p','N'}));
318:         if(z==ZoneType.MECHANICUS_FORGE_CLOISTER || z==ZoneType.MECHANICUS_RELIC_DUCT){
319:             list.add(new RoomProfile("Atmospheric Condenser Cell","Martian condenser room: damp coils, intake vanes, and emergency water recovery hardware",44,Faction.MECHANICUS,new String[]{"water bottle","filter cartridge","machine parts"},new char[]{'Y','N','q'}));
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
```

Patch actions:
- Added structured Phase3RoomFeatureCommitDecision bridge and overloads.

Added `Phase3RoomFeatureCommitDecision` and `decidePhase3RoomFeatureCommit(...)` overloads.

This provides a structured response for room profile/stamp commit logic:
- accepted,
- resolution,
- suggested minimum side,
- room label.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing room profile/stamp commit wiring,
- giving the generator actionable outcomes,
- preserving expand/larger-stamp/reduce/retry/reject behavior.

What needs to be done next:
- wire the commit decision at the confirmed commit point,
- implement the actual resolution actions,
- keep edge/pathability protections active.

## 0.9.10dn — Phase 3 Room Feature Load Resolution Policy

Continued Phase 3 from room profile/stamp selection gate probing.

Patch actions:
- Added structured Phase3RoomFeatureResolution policy and overload.

Added `Phase3RoomFeatureResolution` and structured resolution helpers so overburdened rooms can resolve as accept, expand, choose larger stamp, reduce features, downgrade feature set, retry placement, or reject.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- converting feature-density failure into a structured generation response,
- preparing profile/stamp commit wiring,
- preserving room expansion/downgrade/reject options.

What needs to be done next:
- wire this policy to the room profile/stamp commit point,
- implement actual expand/larger-stamp/reduce behavior.

## 0.9.10dm — Phase 3 Room Profile / Stamp Selection Gate Probe

Continued Phase 3 from room profile/stamp feature-load attachment preparation.

Selection/commit candidates observed:
WorldRuntimeGenerationFramework.java near line 1652:
```java
1649:     }
1650: 
1651:     boolean regionTouchesNonWall(Rectangle rr){ for(int x=Math.max(0,rr.x); x<Math.min(w,rr.x+rr.width); x++) for(int y=Math.max(0,rr.y); y<Math.min(h,rr.y+rr.height); y++) if(roomIds[x][y] >= 0 || tiles[x][y]!='#') return true; return false; }
1652:     void carve(Rectangle rr){ int id=rooms.size(); RoomProfile rp = RoomProfile.forZone(zoneType, r); roomProfiles.add(rp); roomFactions.add(Faction.NONE); roomSpecials.add(Boolean.FALSE); char floorChar = zoneType.floorGlyph(r); for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++){ roomIds[x][y]=id; boolean wall = (x==rr.x || y==rr.y || x==rr.x+rr.width-1 || y==rr.y+rr.height-1); tiles[x][y]= wall ? '#' : floorChar; } }
1653: 
1654:     void buildGuaranteedRoomLattice(int target){
1655:         target = WorldGenerationApi.clampRoomTarget(target);
1656:         int cols = 6;
1657:         int rows = 5;
1658:         int cellW = Math.max(10, (w-4) / cols);
1659:         int cellH = Math.max(8, (h-4) / rows);
```

RoomProfile.java near line 101:
```java
98:     static RoomProfile neutralContestRoom(ZoneType z, Random r){
99:         String[] names = {"Neutral Empty Room", "Vacant Claim Room", "Unassigned Side Chamber", "Empty Service Lease", "Contestable Utility Room"};
100:         String name = names[(r == null ? 0 : r.nextInt(names.length))];
101:         RoomProfile rp = new RoomProfile(name, "an intentionally unoccupied neutral room left between faction claims; bare floor, stripped fixtures, old paper, used tins, and just enough empty space for possible occupation, ambush, squatters, or room-control schemes", 16, Faction.NONE, new String[]{"Old INN newspaper","Used food tin","cloth scraps","paper mush","Vended scrap"}, new char[]{'o','b','q'});
102:         rp.featureText = "Contestable neutral room: currently unclaimed, lightly littered, and suitable for faction takeover, player occupation, hidden meetings, bank-heist staging, or evidence drops.";
103:         return rp;
104:     }
105: 
106:     static RoomProfile themedRoom(String name, String descriptor, int chance, Faction faction, String[] loot, char[] contents){
107:         return new RoomProfile(name, descriptor, chance, faction, loot, contents);
108:     }
```

RoomProfile.java near line 43:
```java
40:     }
41: 
42:     static RoomProfile dormitoryCell(ZoneType z, Random r){
43:         RoomProfile rp = new RoomProfile("Hab Dormitory Cell", "a cramped worker dormitory cell stamped off a long residence corridor; cot, sink, dresser, and cabinet occupy more space than dignity does", 34, Faction.HIVER, new String[]{"sealed water ration","cloth scraps","cheap trinket","emergency ration"}, new char[]{'c','u','s'});
44:         rp.featureText = "Inspectable dormitory features: narrow cot with patched thermal blanket; stained sink with ration-water residue; small dresser of dented plasteel; cabinet with scratched personal marks.";
45:         return rp;
46:     }
47: 
48:     static RoomProfile apartmentRoom(String label, ZoneType z, Random r){
49:         RoomProfile rp = new RoomProfile(label, "one chamber of a predictable hab apartment block module: compact, repetitive, and designed by someone paid by the square meter saved", 38, Faction.HIVER, new String[]{"sealed water ration","cloth scraps","cheap meal tin","household scrap"}, new char[]{'q','b'});
50:         if(label.contains("Bedroom")) { rp.contents = new char[]{'c','s'}; rp.featureText = "Inspectable apartment features: low cot, storage locker, folded work clothes, and small devotional scratch marks."; }
```

WorldRuntimeGenerationFramework.java near line 3097:
```java
3094:             if(made >= desired) break;
3095:             if(!canPlaceInterwallRoom(world, rect, false)) continue;
3096:             Faction f = dangerFaction(world, r, made);
3097:             RoomProfile rp = new RoomProfile(
3098:                 "Abandoned Interwall Danger Room",
3099:                 "scrap-choked forgotten chamber between the active zone and the outer hive wall; old collapse dust, abandoned reserves, and something dangerous have been sealed here too long",
3100:                 82, f,
3101:                 new String[]{"forgotten weapon cache","old armor bundle","tech salvage","sealed reserve crate","wanted poster scrap"},
3102:                 new char[]{'!','?','*','b'});
3103:             int rid = carveAddedRoom(world, rect, rp, f, true);
3104:             if(rid >= 0){ connectDangerRoomToLoop(world, rect, inset); made++; }
```

WorldRuntimeGenerationFramework.java near line 3069:
```java
3066:         };
3067:         for(Rectangle rect: candidates){
3068:             if(!canPlaceInterwallRoom(world, rect, true)) continue;
3069:             int rid = carveAddedRoom(world, rect, new RoomProfile(
3070:                 "Hivewall Maintenance Room",
3071:                 "bolted-on high-wall service room attached after the normal zone was built; one door returns to the main hive, while left and right maintenance exits run into the exterior wall loop",
3072:                 58, Faction.NONE,
3073:                 new String[]{"machine parts","wire bundle","rusted tool","sealed water ration"},
3074:                 new char[]{'N','q','b'}), Faction.NONE, true);
3075:             connectRoomToLoop(world, rect, inset);
3076:             connectRoomToNearestInteriorCorridor(world, rect);
```

WorldRuntimeGenerationFramework.java near line 623:
```java
620:         // Every generated zone receives a neutral Cult Imperialis temple near the central plaza.
621:         // It is stamped before compass corridors and faction modules so the sanctuary owns space
622:         // close to the plaza instead of being pushed to random room assignment.
623:         if(plaza == null || EcclesiarchyTempleApi.templeAlreadyStamped(roomProfiles)) return;
624:         int[] offsets = {0, -6, 6, -10, 10};
625:         for(int oi=0; oi<offsets.length; oi++){
626:             for(int side=0; side<4; side++){
627:                 if(stampTempleRoomAt(plaza, side, offsets[oi])){
628:                     DebugLog.audit("ECCLESIARCHY_TEMPLE_STAMP", "zone="+zoneType.label+" side="+side+" offset="+offsets[oi]+" rooms="+rooms.size()+" layer="+layerText());
629:                     return;
630:                 }
```

RoomProfile.java near line 328:
```java
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
333:     char contentSymbol(Random r){ return contents[r.nextInt(contents.length)]; }
334: }
```

RoomProfile.java near line 326:
```java
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
333:     char contentSymbol(Random r){ return contents[r.nextInt(contents.length)]; }
```

RoomProfile.java near line 325:
```java
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
332:     String randomLoot(Random r){ return loot[r.nextInt(loot.length)]; }
```

RoomProfile.java near line 324:
```java
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
331:     }
```

RoomProfile.java near line 323:
```java
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
330:         return selected;
```

RoomProfile.java near line 322:
```java
319:             list.add(new RoomProfile("Atmospheric Condenser Cell","Martian condenser room: damp coils, intake vanes, and emergency water recovery hardware",44,Faction.MECHANICUS,new String[]{"water bottle","filter cartridge","machine parts"},new char[]{'Y','N','q'}));
320:             list.add(new RoomProfile("Emergency Assembler Niche","Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions",48,Faction.MECHANICUS,new String[]{"machine parts","wire bundle","construction supplies"},new char[]{'J','N','q'}));
321:             list.add(new RoomProfile("Emergency Boiler Annex","Martian boiler room: hot pipes, pressure gauges, dirty water, and usable fuel",42,Faction.MECHANICUS,new String[]{"dirty water","fuel scrap","machine parts"},new char[]{'B','N','q'}));
322:             list.add(new RoomProfile("Micro Laboratorium Closet","Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess",36,Faction.MECHANICUS,new String[]{"cogitator shard","filter cartridge","sample vial"},new char[]{'K','q','N'}));
323:             list.add(new RoomProfile("Emergency Miner Bay","Martian miner room: bite-head drill assembly and enough noise to invite visitors",38,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","machine parts"},new char[]{'O','N','q'}));
324:             list.add(new RoomProfile("Power Grid Relay Room","Martian power relay room: switch rails, insulated grids, and ownership signs nobody sane ignores",34,Faction.MECHANICUS,new String[]{"wire bundle","machine parts","fuse block"},new char[]{'Z','N','q'}));
325:             list.add(new RoomProfile("Emergency Smelter Cell","Martian smelter room: heat scarred floor plates and ore feed gates",40,Faction.MECHANICUS,new String[]{"raw iron ore","raw copper ore","fuel scrap"},new char[]{'P','N','q'}));
326:             list.add(new RoomProfile("Steam Engine Chamber","Martian steam engine room: piston mass, pressure brass, and a very sabotageable heart",32,Faction.MECHANICUS,new String[]{"fuel scrap","machine parts","dirty water"},new char[]{'F','N','q'}));
327:         }
328:         RoomProfile selected = list.get(r.nextInt(list.size()));
329:         selected.featureText = InspectableFeatureTable.combinedFor(z, r, selected.descriptor);
```

Patch actions:
- Added room profile/stamp acceptance gate helpers.

Added `shouldAcceptPhase3RoomProfileOrStamp(...)` overloads for exact `Phase3RoomFeatureLoad` and fallback feature-count estimation.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- moving room feature-density toward the actual selection gate,
- preserving retry/expand/downgrade behavior,
- avoiding blind constructor rewrites.

What needs to be done next:
- confirm exact selection commit point,
- wire gate before commit,
- feed exact fixture footprints where available.

## 0.9.10dl — Phase 3 Room Profile / Stamp Feature-Load Attachment Preparation

Continued Phase 3 from the room feature-load model.

RoomProfile inspection:
- line 15: `class RoomProfile {`
- line 16: `String name, descriptor, featureText; int scavengeChance; Faction faction; String[] loot; char[] contents;`
- line 17: `RoomProfile(String name,String descriptor,int chance,Faction faction,String[] loot,char[] contents){this.name=name;this.descriptor=descriptor;this.featureText=defaultFeatures(name, descriptor);this.scavengeChance=chance;this.faction=faction;this.loot=loot;this.contents=contents;}`
- line 18: `static String defaultFeatures(String name, String descriptor){ return descriptor; }`
- line 19: `RoomProfile withFeatures(String f){ this.featureText=f; return this; }`
- line 20: `RoomProfile asSpecial(ZoneType z, Random r){`
- line 21: `String[] specialNames={"Trader alcove","Hidden supply cache","Faction-locked chamber","Machinery room","Random encounter room","Shrine-side store","Clinic spillover","Sealed service cell","Watch post"};`
- line 23: `RoomProfile rp=new RoomProfile(sn + " / " + name, descriptor + "; special feature: " + sn.toLowerCase(Locale.ROOT), Math.min(90, scavengeChance+12+r.nextInt(12)), faction, loot, contents);`
- line 24: `rp.featureText = featureText + "; SPECIAL: " + sn + "; inspectable hooks: trader, cache, lock, machinery, or encounter hook.";`
- line 27: `RoomProfile asMachineRoom(String machineName, String machineDescription, char glyph){`
- line 28: `RoomProfile rp = new RoomProfile(machineName, machineDescription, Math.max(45, scavengeChance), faction, loot, contents);`
- line 29: `rp.featureText = machineDescription + " Inspectable Martian emergency machine glyph '" + glyph + "'. Look displays the imported EMM machine profile; Interact handles locks, power, ownership, and machine-specific work.";`
- line 33: `static RoomProfile closetStub(ZoneType z, Random r){`
- line 35: `String[] features=InspectableFeatureTable.featuresFor(z, r);`
- line 36: `String f=features.length==0?"one lonely inspectable thing that probably disappointed someone":features[r.nextInt(features.length)];`
- line 37: `RoomProfile rp = new RoomProfile(names[r.nextInt(names.length)], "single-door stub created when a corridor projection was unsafe", 18+r.nextInt(14), Faction.NONE, new String[]{"bent nail tin","cloth scraps","machine scrap","sealed water ration"}, new char[]{'o','p','b','N','r'});`
- line 38: `rp.featureText = "Closet fallback: " + f + "; generated because the door's intended corridor would have overlapped a room, wall, or existing corridor.";`
- line 42: `static RoomProfile dormitoryCell(ZoneType z, Random r){`
- line 43: `RoomProfile rp = new RoomProfile("Hab Dormitory Cell", "a cramped worker dormitory cell stamped off a long residence corridor; cot, sink, dresser, and cabinet occupy more space than dignity does", 34, Faction.HIVER, new String[]{"sealed water ration","cloth scraps","cheap trinket","emergency ration"}, new char[]{'c','u','s'});`
- line 44: `rp.featureText = "Inspectable dormitory features: narrow cot with patched thermal blanket; stained sink with ration-water residue; small dresser of dented plasteel; cabinet with scratched personal marks.";`
- line 48: `static RoomProfile apartmentRoom(String label, ZoneType z, Random r){`
- line 49: `RoomProfile rp = new RoomProfile(label, "one chamber of a predictable hab apartment block module: compact, repetitive, and designed by someone paid by the square meter saved", 38, Faction.HIVER, new String[]{"sealed water ration","cloth scraps","cheap meal tin","household scrap"}, new char[]{'q','b'});`
- line 50: `if(label.contains("Bedroom")) { rp.contents = new char[]{'c','s'}; rp.featureText = "Inspectable apartment features: low cot, storage locker, folded work clothes, and small devotional scratch marks."; }`
- line 51: `else if(label.contains("Washroom")) { rp.contents = new char[]{'u','N'}; rp.featureText = "Inspectable apartment features: water-stained sink, recycler pipe access, cracked privacy partition, and bad drainage."; }`
- line 52: `else if(label.contains("Dining")) { rp.contents = new char[]{'T','b'}; rp.featureText = "Inspectable apartment features: tiny food-prep counter, battered stool, ration hooks, and stained family table."; }`
- line 53: `else { rp.contents = new char[]{'q','b'}; rp.featureText = "Inspectable apartment features: worn seats, prayer marks, personal debris, and a room that has witnessed too many rent disputes."; }`
- line 59: `static RoomProfile factionRepresentativeBar(ZoneType z, Faction f, Random r){`
- line 62: `RoomProfile rp = new RoomProfile(label,`
- line 67: `rp.featureText = "Continuity bar: recognizable fallback contact point for " + fac.label + ". The representative is invulnerable and untargetable; if the faction controls no other rooms, the rep functions as provisional faction leader. If the faction controls another room, the rank-one leader should reside at that room as the local base of operations.";`
- line 71: `static RoomProfile centralPlaza(ZoneType z, boolean sewer, Random r){`
- line 84: `else if(z==ZoneType.ARBITES_PRECINCT_EDGE){ name="Arbites Precinct Lobby"; desc="a hard-lit precinct lobby with public counters, barred access, queue rails, and armed personnel behind every meaningful door"; fac=Faction.ARBITES; contents=new char[]{'A','q','b','n','N'}; }`
- line 86: `else if(z==ZoneType.ADMINISTRATUM_ARCHIVE){ name="Administratum Queue Plaza"; desc="a central bureaucratic hall of counters, line rails, stamped notices, clerks, and citizens ageing in public"; fac=Faction.ADMINISTRATUM; contents=new char[]{'q','h','b','T','n'}; }`
- line 92: `RoomProfile rp = new RoomProfile(name, desc, 65, fac, loot, contents);`
- line 93: `rp.featureText = desc + "; all branch corridors originate from or connect back to this plaza so the zone has a guaranteed accessible nexus.";`
- line 96: `static RoomProfile generic(){ return new RoomProfile("Generic underhive room","unremarkable misery with structural opinions",30,Faction.NONE,new String[]{"scrap bundle","bent nail tin","emergency ration"},new char[]{'p','b','N'}); }`
- line 98: `static RoomProfile neutralContestRoom(ZoneType z, Random r){`
- line 99: `String[] names = {"Neutral Empty Room", "Vacant Claim Room", "Unassigned Side Chamber", "Empty Service Lease", "Contestable Utility Room"};`
- line 101: `RoomProfile rp = new RoomProfile(name, "an intentionally unoccupied neutral room left between faction claims; bare floor, stripped fixtures, old paper, used tins, and just enough empty space for possible occupation, ambush, squatters, or room-control schemes", 16, Faction.NONE, new String[]{"Old INN newspaper","Used food tin","cloth scraps","paper mush","Vended scrap"}, new char[]{'o','b','q'});`
- line 102: `rp.featureText = "Contestable neutral room: currently unclaimed, lightly littered, and suitable for faction takeover, player occupation, hidden meetings, bank-heist staging, or evidence drops.";`
- line 106: `static RoomProfile themedRoom(String name, String descriptor, int chance, Faction faction, String[] loot, char[] contents){`
- line 107: `return new RoomProfile(name, descriptor, chance, faction, loot, contents);`
- line 110: `static void addInfrastructureRooms(ArrayList<RoomProfile> list, ZoneType z){`
- line 116: `// Mechanicus rest-cell are different rooms with different bodies and values.`
- line 120: `addCivilianRooms(list, z); break;`
- line 122: `addMarketRooms(list); break;`
- line 124: `addGangerRooms(list); break;`
- line 126: `addArbitesRooms(list); break;`
- line 128: `addAdministratumRooms(list); break;`
- line 130: `addInnRooms(list); break;`
- line 132: `addGuardRooms(list); break;`
- line 135: `addMechanicusRooms(list, z); break;`
- line 138: `addNobleRooms(list, z); break;`
- line 141: `addRailRooms(list, z); break;`
- line 143: `addSewerRooms(list); break;`
- line 146: `addMutantRooms(list, z); break;`
- line 148: `addCultistRooms(list); break;`
- line 150: `addTrashRooms(list); break;`
- line 156: `static void addCivilianRooms(ArrayList<RoomProfile> list, ZoneType z){`
- line 158: `list.add(themedRoom("Civilian Dormitory Bay", "stacked cot racks, shared wash basins, cracked privacy curtains, and family bundles wedged into state-approved human shelving", 38, f, new String[]{"sealed water ration","cloth scraps","cheap trinket","emergency ration"}, new char[]{'h','c','u','s','b'}));`
- line 159: `list.add(themedRoom("Communal Kitchen Block", "ration burners, grease-black counters, water recycler taps, and queue marks cut into the floor by hungry feet", 44, f, new String[]{"cheap meal tin","sealed water ration","fuel scrap","ration ticket"}, new char[]{'T','Y','b','q'}));`
- line 160: `list.add(themedRoom("Food Storehouse", "locked racks of ration tins, dry starch sacks, water tokens, and a clerk desk for denying the obvious", 52, f, new String[]{"emergency ration","sealed water ration","ration ticket","cheap meal tin"}, new char[]{'b','T','q','n'}));`
- line 161: `list.add(themedRoom("Civic Product Warehouse", "crated work clothes, household parts, broken furniture, and tagged bundles waiting for a distribution schedule that died three shifts ago", 50, f, new String[]{"household scrap","cloth scraps","machine scrap","trade chit"}, new char[]{'b','p','N','q'}));`
- line 162: `list.add(themedRoom("Permit Storefront", "a narrow counter selling legal patience: form packets, water claims, ration stamps, and small humiliations", 36, Faction.ADMINISTRATUM, new String[]{"permit stub","ration ticket","trade chit","ink cartridge"}, new char[]{'q','h','b'}));`
- line 163: `list.add(themedRoom("Block Clinic Room", "sealed medicae cabinet, stained cot, boiled instruments, and a queue of people negotiating with pain", 30, f, new String[]{"bandage roll","sealed water ration","sample vial","cloth scraps"}, new char[]{'u','c','b','N'}));`
- line 164: `list.add(themedRoom("Laundry and Wash Room", "steam pipes, hanging work clothes, grey suds, drainage stink, and gossip carried faster than disease", 42, f, new String[]{"laundered cloth","cloth scraps","dirty water","soap stub"}, new char[]{'u','Y','p','b'}));`
- line 165: `list.add(themedRoom("Maintenance Workshop", "patched tools, wire reels, spare fasteners, and a civic mechanic's shrine to making bad hardware continue", 52, f, new String[]{"machine scrap","wire bundle","rusted tool","spare bolts"}, new char[]{'N','R','q','b'}));`
- line 166: `list.add(themedRoom("Family Shrine Alcove", "cheap candles, ancestor marks, ration wrappers folded into offerings, and exhausted faith doing local work", 26, f, new String[]{"wax scrap","cheap trinket","cloth scraps"}, new char[]{'c','n','q'}));`
- line 169: `static void addMarketRooms(ArrayList<RoomProfile> list){`
- line 170: `list.add(themedRoom("Sump Market Food Storehouse", "guarded tins, fungus sacks, reclaimed water barrels, and prices adjusted by hunger rather than law", 56, Faction.HIVER, new String[]{"cheap meal tin","sealed water ration","sump fungus","trade chit"}, new char[]{'T','b','Y','q'}));`
- line 171: `list.add(themedRoom("Sump Product Warehouse", "mixed crates of parts, cloth, filters, tools, and cargo tags scraped off previous owners", 58, Faction.HIVER, new String[]{"machine scrap","wire bundle","supply crate scrap","trade chit"}, new char[]{'b','p','N','T'}));`
- line 172: `list.add(themedRoom("Barter Storefront Row", "canvas counters, hanging lamps, chalk prices, and vendors smiling like they know tomorrow's shortages", 44, Faction.HIVER, new String[]{"trade chit","cheap trinket","ration ticket","cloth scraps"}, new char[]{'T','q','b','n'}));`
- line 173: `list.add(themedRoom("Water Merchant Stall", "sealed taps, filter cages, wet ledgers, and guards watching every cup like a jewel", 42, Faction.HIVER, new String[]{"sealed water ration","filter cartridge","dirty water","trade chit"}, new char[]{'Y','q','b'}));`
- line 174: `list.add(themedRoom("Pawn Cage", "barred shelves of watches, tools, boots, knives, heirlooms, and desperation priced by weight", 40, Faction.HIVER, new String[]{"cheap trinket","rusted tool","tiny knife","trade chit"}, new char[]{'b','q','n'}));`
- line 175: `list.add(themedRoom("Market Kitchen Smokehole", "cheap burners, vat grease, suspicious protein, and a crowd pretending the smell is normal", 45, Faction.HIVER, new String[]{"cheap meal tin","spoiled ration","fuel scrap","sump fungus"}, new char[]{'T','B','q'}));`
- line 176: `list.add(themedRoom("Debt Office Back Room", "ledger hooks, intimidation chairs, pawn tags, and an exit placed for the collector rather than the debtor", 28, Faction.BANDIT, new String[]{"trade chit","debt marker","permit scrap"}, new char[]{'q','g','b'}));`
- line 177: `list.add(themedRoom("Repair Booth", "open tool rolls, salvaged components, counterfeit seals, and machines halfway between repair and fraud", 50, Faction.HIVER, new String[]{"machine parts","wire bundle","rusted tool","filter cartridge"}, new char[]{'N','R','q'}));`
- line 180: `static void addGangerRooms(ArrayList<RoomProfile> list){`
- line 181: `list.add(themedRoom("Gang Crash Barracks", "mattresses, stolen blankets, weapon hooks, and bodies sleeping in shifts beside loaded grudges", 34, Faction.BANDIT, new String[]{"damaged bandit colors","cloth scraps","ammo scrap","cheap meal tin"}, new char[]{'g','c','b','p'}));`
- line 182: `list.add(themedRoom("Gang Mess and Chem Kitchen", "hot plates, protein tins, stimulant stains, and drug foil mixed with dinner scraps", 42, Faction.BANDIT, new String[]{"stimulant ampoule","spoiled ration","cheap meal tin","chem vial"}, new char[]{'T','g','b','q'}));`
- line 183: `list.add(themedRoom("Protection Storehouse", "food and water taken as payment for not having worse problems, stacked under painted threats", 50, Faction.BANDIT, new String[]{"emergency ration","sealed water ration","trade chit","ammo scrap"}, new char[]{'b','g','q'}));`

StampedRoomSpec inspection:
- line 15: `class StampedRoomSpec {`
- line 16: `final String kind, name, descriptor; final int scavengeChance; final Faction faction; final String[] loot; final char[] contents;`
- line 17: `StampedRoomSpec(String kind, String name, String descriptor, int scavengeChance, Faction faction, String[] loot, char[] contents){`
- line 20: `RoomProfile toProfile(ZoneType z, Random r){`
- line 21: `RoomProfile rp = RoomProfile.themedRoom(name, descriptor, scavengeChance, faction, loot, contents);`
- line 22: `rp.featureText = InspectableFeatureTable.combinedFor(z, r, descriptor);`

Patch actions:
- Added room profile/stamp feature-load validation and resolution helpers.

Added profile/stamp-facing helpers:
- `validatePhase3RoomProfileFeatureLoad(...)`
- `estimatePhase3RoomFeatureLoadFromCount(...)`
- `suggestPhase3RoomFeatureLoadResolution(...)`

These prepare the 1.5x feature-footprint/access rule for attachment to room profile or stamp selection.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- moving feature-density validation closer to room profile/stamp selection,
- preserving conservative fallback estimation,
- preparing accept/expand/downgrade/reject behavior.

What needs to be done next:
- identify actual room profile/stamp selection path,
- call validation before placement commit,
- count canonical fixture footprints.

## 0.9.10dk — Phase 3 Room Feature Load Model / Profile Attachment Preparation

Continued Phase 3 from room feature-density and accessibility validation.

Patch actions:
- Added Phase3RoomFeatureLoad model and validation helpers.

Added `Phase3RoomFeatureLoad`, a small model for room profile/stamp feature burden:
- feature tile count,
- required access tile count,
- blocking feature tile count,
- interactive feature count.

Added helper validation for feature load and a suggested square-ish minimum room side for debugging/planning.

Candidate profile/feature references:
- EcclesiarchyTempleApi.java:18 `static boolean isTempleRoom(RoomProfile rp) {`
- EcclesiarchyTempleApi.java:24 `static boolean templeAlreadyStamped(java.util.List<RoomProfile> profiles) {`
- EcclesiarchyTempleApi.java:26 `for (RoomProfile rp : profiles) if (isTempleRoom(rp)) return true;`
- EcclesiarchyTempleApi.java:30 `static RoomProfile templeProfile(ZoneType z, Random r) {`
- EcclesiarchyTempleApi.java:33 `RoomProfile rp = RoomProfile.themedRoom("Cult Imperialis Temple Nave", desc, 62, Faction.MINISTORUM,`
- EcclesiarchyTempleApi.java:36 `rp.featureText = "Stamped Ministorum church: nave pillars/columns, relic alcoves, candle racks, prayer nooks, saint icons, donation box, supplicant kitchen, protected head cleric, priests, pilgrims, and Sororitas guard presence.";`
- EcclesiarchyTempleApi.java:44 `for (RoomProfile rp : w.roomProfiles) if (isTempleRoom(rp)) temples++;`
- StampedRoomSpec.java:15 `class StampedRoomSpec {`
- StampedRoomSpec.java:17 `StampedRoomSpec(String kind, String name, String descriptor, int scavengeChance, Faction faction, String[] loot, char[] contents){`
- StampedRoomSpec.java:20 `RoomProfile toProfile(ZoneType z, Random r){`
- StampedRoomSpec.java:21 `RoomProfile rp = RoomProfile.themedRoom(name, descriptor, scavengeChance, faction, loot, contents);`
- StampedRoomSpec.java:22 `rp.featureText = InspectableFeatureTable.combinedFor(z, r, descriptor);`
- StampedModuleSpec.java:19 `StampedModuleSpec(String name, String corridorDress, int corridorLength, int corridorWidth, int roomWidth, int roomHeight, int preferredSide, int laneOffset, StampedRoomSpec... rooms){`
- RoomManifestApi.java:24 `spec("TRAINING","Stamped Guard Drill Room","training stamp with lanes, benches, target marks, and enough open floor for shouted correction",34,Faction.IMPERIAL_GUARD,new String[]{"training manual","fatigue cloth","dummy round"},new char[]{'!','b'})));`
- RoomManifestApi.java:26 `spec("CAFETERIA","Stamped Guard Field Cafeteria","long thin cafeteria spine: ordered tables and benches in ranks, trash bins, ration counter, and military throughput over comfort",54,Faction.IMPERIAL_GUARD,new String[]{"guard ration tin","sealed water ration","meal tray"},new char[]{'T','b','N'}),`
- RoomManifestApi.java:34 `spec("CAFETERIA","Stamped Noble Dining Gallery","controlled dining gallery with aligned servant benches, private side tables, porcelain bins, and discreet service lanes",58,Faction.NOBLE,new String[]{"fine meal tin","silver ration chit","sealed water ration"},new char[]{'T','b','q'}),`
- RoomManifestApi.java:47 `spec("SECURITY","Stamped Holding Cell Row","cell-row stamp: bars, benches, floor drains, camera angles, and architecture designed to make procedure feel inevitable",42,Faction.ARBITES,new String[]{"key chit","cloth scraps","citation token"},new char[]{'X','b'}),`
- RoomManifestApi.java:60 `spec("LEARNING","Stamped Block Learning Room","community learning room with slates, benches, warning posters, and badly copied arithmetic",30,Faction.HIVER,new String[]{"data slate","training manual","pencil stub"},new char[]{'l','q'})));`
- RoomManifestApi.java:74 `static java.util.List<StampedRoomSpec> requiredRoomManifest(ZoneType z){`
- RoomManifestApi.java:75 `ArrayList<StampedRoomSpec> out = new ArrayList<>();`
- RoomManifestApi.java:79 `add(out,"CAFETERIA","Imperial Guard Field Cafeteria","long thin mess corridor with bench lines, fixed tables, trash bins, a ration counter, and a rear kitchen hatch meant to move soldiers through quickly",50,Faction.IMPERIAL_GUARD,new String[]{"guard ration tin","sealed water ration","meal tray"},new char[]{'T','b','N','q'});`
- RoomManifestApi.java:85 `add(out,"TRAINING","Drill Training Room","rectangular training bay with marked lanes, weapons racks, ordered benches, and shouted doctrine baked into the walls",30,Faction.IMPERIAL_GUARD,new String[]{"training baton","fatigue cloth","las-cell crate"},new char[]{'!','b','T'});`
- RoomManifestApi.java:91 `add(out,"CAFETERIA","Noble Service Dining Gallery","long controlled dining gallery with aligned benches for servants, private side tables, porcelain bins, and a hidden service counter to the kitchen",55,Faction.NOBLE,new String[]{"fine meal tin","sealed water ration","silver ration chit"},new char[]{'T','b','q','N'});`
- RoomManifestApi.java:113 `add(out,"CAFETERIA","Communal Cafeteria","long shared eating room with cheap benches, table rows, public bins, water queue marks, and a ration counter leading to a kitchen",44,Faction.HIVER,new String[]{"cheap meal tin","sealed water ration","ration wrapper"},new char[]{'T','b','N'});`
- RoomManifestApi.java:123 `add(out,"CAFETERIA","Precinct Mess Slot","narrow mess with fixed benches, monitored tables, trash bins, and a service slit that discourages lingering",38,Faction.ARBITES,new String[]{"ration tin","sealed water ration","citation token"},new char[]{'T','b','N'});`
- RoomManifestApi.java:127 `add(out,"SECURITY","Holding Cell Row","security room of bars, benches, drain grates, and the smell of procedure becoming violence",35,Faction.ARBITES,new String[]{"key chit","cloth scraps","dirty water"},new char[]{'X','b'});`
- RoomManifestApi.java:141 `add(out,"CAFETERIA","Sump Food Court Corridor","long eating lane with vendor benches, bins, counters, steam, debt marks, and arguments priced by the bowl",42,Faction.NONE,new String[]{"cheap meal tin","dirty water","trade chit"},new char[]{'T','b','N'});`
- RoomManifestApi.java:154 `private static StampedModuleSpec module(String name, String dress, int len, int cw, int rw, int rh, int preferredSide, int laneOffset, StampedRoomSpec... rooms){`
- RoomManifestApi.java:157 `private static StampedRoomSpec spec(String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){`
- RoomManifestApi.java:158 `return new StampedRoomSpec(kind, name, desc, chance, faction, loot, contents);`
- RoomManifestApi.java:161 `private static void addDefault(ArrayList<StampedRoomSpec> out, ZoneType z){`
- RoomManifestApi.java:164 `add(out,"CAFETERIA",z.label+" Eating Hall Variant","zone-specific cafeteria or mess: long eating surfaces, benches, refuse bins, and a service counter adapted to the faction occupying this space",36,f,new String[]{"cheap meal tin","sealed water ration","dirty water"},new char[]{'T','b','N'});`
- RoomManifestApi.java:185 `private static void add(ArrayList<StampedRoomSpec> out, String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){ out.add(new StampedRoomSpec(kind,name,desc,chance,faction,loot,contents)); }`
- RoomProfile.java:15 `class RoomProfile {`
- RoomProfile.java:17 `RoomProfile(String name,String descriptor,int chance,Faction faction,String[] loot,char[] contents){this.name=name;this.descriptor=descriptor;this.featureText=defaultFeatures(name, descriptor);this.scavengeChance=chance;this.faction=faction;this.loot=loot;this.contents=contents;}`
- RoomProfile.java:18 `static String defaultFeatures(String name, String descriptor){ return descriptor; }`
- RoomProfile.java:19 `RoomProfile withFeatures(String f){ this.featureText=f; return this; }`
- RoomProfile.java:20 `RoomProfile asSpecial(ZoneType z, Random r){`
- RoomProfile.java:23 `RoomProfile rp=new RoomProfile(sn + " / " + name, descriptor + "; special feature: " + sn.toLowerCase(Locale.ROOT), Math.min(90, scavengeChance+12+r.nextInt(12)), faction, loot, contents);`
- RoomProfile.java:24 `rp.featureText = featureText + "; SPECIAL: " + sn + "; inspectable hooks: trader, cache, lock, machinery, or encounter hook.";`

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing density validation for actual room profiles/stamps,
- separating feature footprint from access burden,
- making random seating count as blocking density.

What needs to be done next:
- attach feature load to RoomProfile/StampedRoomSpec selection,
- count canonical fixture footprints,
- reject/expand/downgrade overstuffed rooms.

## 0.9.10dj — Phase 3 Room Feature Density / Accessibility Validation

Continued Phase 3 from random seating / blocking fixture safety.

Added room feature-density doctrine:
rooms must contain enough area for both feature tiles and walkable access to those features.

Baseline rule:
contained room area must be at least 1.5x the intended feature tile footprint, with additional access tile requirements where applicable.

Patch actions:
- Added Phase 3 room feature-density and accessibility planning helpers.
- Added FEATURE_ACCESS_AREA_MULTIPLIER constant to ZoneGenerationContext.

Added helpers for:
- minimum room area from feature tile count,
- feature/access suitability checks,
- feature-density rejection logging.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preventing overstuffed rooms,
- ensuring interactive features remain approachable,
- making random seating count as blocking fixture density.

What needs to be done next:
- attach density validation to room profiles/stamps,
- count intended feature footprints,
- estimate access tiles,
- reject/expand/downgrade rooms that are too small.

## 0.9.10di — Phase 3 Random Seating / Blocking Fixture Safety Gate

Continued Phase 3 from direct door receiver reservation.

Patch actions:
- Added tryPlacePhase3RandomSeatingOrBlockingFixture(...) wrapper.
- Added surface helper for validating reserved traversal rectangles remain open.

Added `tryPlacePhase3RandomSeatingOrBlockingFixture(...)`, a wrapper that rejects unsafe seating/blocking fixture placement and marks accepted fixture tiles blocked.

Observed potential random seating / fixture candidates:
Candidate near line 4258:
```java
4255:         return allowed;
4256:     }
4257: 
4258:     public static void markPhase3RandomSeatingOrBlockingFixturePlaced(
4259:             MapLayerSurfaceAuthority surfaceAuthority,
4260:             int x,
4261:             int y,
4262:             int width,
4263:             int height) {
4264:         if (surfaceAuthority == null) {
4265:             phase3Debug("FixturePlacementReject", "cannot mark fixture: surface authority is null");
```

Candidate near line 4239:
```java
4236:      * Seating and other blocking fixtures may be placed only after door receiver tiles
4237:      * and required traversal lanes are reserved.
4238:      */
4239:     public static boolean canPlacePhase3RandomSeatingOrBlockingFixture(
4240:             MapLayerSurfaceAuthority surfaceAuthority,
4241:             int x,
4242:             int y,
4243:             int width,
4244:             int height) {
4245:         if (surfaceAuthority == null) {
4246:             phase3Debug("FixturePlacementReject", "surface authority is null");
```

Candidate near line 159:
```java
156:         DebugLog.audit("ROAD_ADJACENCY_FOUNDATION", roadAdjacencyResult.summary());
157:         RoadFrontageFixtureAuthority.Result roadFrontageResult = RoadFrontageFixtureAuthority.apply(this, r);
158:         DebugLog.audit("ROAD_FRONTAGE_FIXTURES", roadFrontageResult.summary());
159:         RoomFixtureInteractionAuthority.Result roomFixtureResult = RoomFixtureInteractionAuthority.apply(this, r);
160:         DebugLog.audit("ROOM_FIXTURE_INTERACTIONS", roomFixtureResult.summary());
161:         validateSpecialTransitions();
162:         validateWorldgenSelfReport("POST_TRANSITION_STAMP");
163:         int repairs = repairWorldgenValidationIssues("POST_TRANSITION_STAMP");
164:         int widenedCorridors = widenOneTileCorridors("POST_REPAIR_TWO_WIDE_PREFERENCE");
165:         if(widenedCorridors > 0) {
166:             validateSpecialTransitions();
```

Candidate near line 4265:
```java
4262:             int width,
4263:             int height) {
4264:         if (surfaceAuthority == null) {
4265:             phase3Debug("FixturePlacementReject", "cannot mark fixture: surface authority is null");
4266:             return;
4267:         }
4268: 
4269:         surfaceAuthority.markPhase3FixtureRectAsBlocked(x, y, width, height);
4270:     }
4271: 
4272: 
```

Candidate near line 4252:
```java
4249: 
4250:         boolean allowed = surfaceAuthority.canPlacePhase3BlockingFixtureRect(x, y, width, height);
4251:         if (!allowed) {
4252:             phase3Debug("FixturePlacementReject", "fixture would block reserved/invalid traversal tile; rect="
4253:                     + ZoneGenerationContext.rect(x, y, width, height));
4254:         }
4255:         return allowed;
4256:     }
4257: 
4258:     public static void markPhase3RandomSeatingOrBlockingFixturePlaced(
4259:             MapLayerSurfaceAuthority surfaceAuthority,
```

Candidate near line 2547:
```java
2544:     }
2545: 
2546: 
2547:     Point randomOpenPoint(Random rrng){
2548:         Random use = rrng == null ? r : rrng;
2549:         if(!rooms.isEmpty()) {
2550:             for(int tries=0; tries<120; tries++){
2551:                 Rectangle rr = rooms.get(use.nextInt(rooms.size()));
2552:                 Point p = randomOpenPointInRoom(rr);
2553:                 if(p != null && inBounds(p.x,p.y) && walkable(p.x,p.y) && npcAt(p.x,p.y)==null) return p;
2554:             }
```

Candidate near line 160:
```java
157:         RoadFrontageFixtureAuthority.Result roadFrontageResult = RoadFrontageFixtureAuthority.apply(this, r);
158:         DebugLog.audit("ROAD_FRONTAGE_FIXTURES", roadFrontageResult.summary());
159:         RoomFixtureInteractionAuthority.Result roomFixtureResult = RoomFixtureInteractionAuthority.apply(this, r);
160:         DebugLog.audit("ROOM_FIXTURE_INTERACTIONS", roomFixtureResult.summary());
161:         validateSpecialTransitions();
162:         validateWorldgenSelfReport("POST_TRANSITION_STAMP");
163:         int repairs = repairWorldgenValidationIssues("POST_TRANSITION_STAMP");
164:         int widenedCorridors = widenOneTileCorridors("POST_REPAIR_TWO_WIDE_PREFERENCE");
165:         if(widenedCorridors > 0) {
166:             validateSpecialTransitions();
167:             repairs += repairWorldgenValidationIssues("POST_CORRIDOR_WIDEN");
```

Candidate near line 157:
```java
154:         DebugLog.audit("ROAD_GRID_FOUNDATION", roadGridResult.summary());
155:         RoadAdjacencyIntegrationAuthority.Result roadAdjacencyResult = RoadAdjacencyIntegrationAuthority.apply(this, r);
156:         DebugLog.audit("ROAD_ADJACENCY_FOUNDATION", roadAdjacencyResult.summary());
157:         RoadFrontageFixtureAuthority.Result roadFrontageResult = RoadFrontageFixtureAuthority.apply(this, r);
158:         DebugLog.audit("ROAD_FRONTAGE_FIXTURES", roadFrontageResult.summary());
159:         RoomFixtureInteractionAuthority.Result roomFixtureResult = RoomFixtureInteractionAuthority.apply(this, r);
160:         DebugLog.audit("ROOM_FIXTURE_INTERACTIONS", roomFixtureResult.summary());
161:         validateSpecialTransitions();
162:         validateWorldgenSelfReport("POST_TRANSITION_STAMP");
163:         int repairs = repairWorldgenValidationIssues("POST_TRANSITION_STAMP");
164:         int widenedCorridors = widenOneTileCorridors("POST_REPAIR_TWO_WIDE_PREFERENCE");
```

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- routing random seating toward a safe gate,
- preventing seating from blocking reserved traversal,
- preparing final post-fixture validation.

What needs to be done next:
- attach actual random seating call sites to the wrapper,
- mark only accepted fixtures as blocked,
- validate reserved traversal after fixtures,
- then attach direct-door planning to room clustering.

## 0.9.10dh — Phase 3 Direct Door Receiver Reservation Wiring

Continued Phase 3 from reserved traversal / random seating fixture safety.

Patch actions:
- Added direct room-to-room door planning helper that reserves receiver tiles.
- Added reserved traversal post-fixture validation helper.

Added a direct room-to-room door planning helper that reserves receiver tiles immediately after a valid shared-wall door candidate is found.

Added post-fixture reserved traversal validation helper to confirm that random seating or blocking fixtures did not invalidate reserved traversal.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- making receiver reservation part of direct door planning,
- preparing random seating to respect those reservations,
- preserving corridor-linked fallback.

What needs to be done next:
- route random seating through fixture safety gate,
- mark placed seating/fixtures as blocked,
- run reserved traversal validation after fixture placement,
- then attach to room clustering.

## 0.9.10dg — Phase 3 Reserved Traversal / Random Seating Fixture Safety

Continued Phase 3 from tile-state initialization and zone-construction order discussion.

Patch actions:
- Added phase3ReservedTraversalTiles field.
- Initialized phase3ReservedTraversalTiles with fail-safe defaults.
- Added reserved traversal tile and blocking-fixture safety helpers.
- Added runtime helpers for receiver reservations and random seating/blocking fixture safety.

Added reserved traversal tile state and helpers so random seating/blocking fixtures cannot occupy door receiver tiles or required traversal lanes.

Added runtime helpers for:
- reserving door receiver tiles,
- checking random seating/blocking fixture placement,
- marking accepted seating/fixtures as blocked.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- making random seating respect zone construction order,
- preventing seating from invalidating room connections,
- preparing final pathability validation after fixture placement.

What needs to be done next:
- wire receiver reservations into direct door planning,
- route random seating through fixture safety gate,
- mark placed seating as blocked,
- run final pathability validation after seating.

## 0.9.10df — Phase 3 Non-Destructive Tile-State Initialization Wiring

Continued Phase 3 from tile-state initialization probing.

Constructor/setup inspection:
```java
6:     // These arrays are optional until generation populates them; null means fail closed.
7:     private boolean[][] phase3TraversableTiles;
8:     private boolean[][] phase3BlockedTiles;
9: 
10:     private MapLayerSurfaceAuthority() {}
11: 
12:     static int floor(int layer) {
13:         int idx = Math.max(0, Math.min(19, layer));
14:         return idx / 2 + 1;
15:     }
16: 
17:     static boolean isSewer(int layer) {
18:         int idx = Math.max(0, Math.min(19, layer));
19:         return (idx % 2) == 0;
20:     }
21: 
22:     static int displayLayerIndex(int floor, boolean sewer) {
23:         int f = Math.max(1, Math.min(10, floor));
24:         return (f - 1) * 2 + (sewer ? 0 : 1);
25:     }
```

Patch actions:
- Added non-destructive ensurePhase3TileState(...) to MapLayerSurfaceAuthority.
- Updated runtime ensure helper to use MapLayerSurfaceAuthority.ensurePhase3TileState(...).

Added `MapLayerSurfaceAuthority.ensurePhase3TileState(int width, int height)` and updated the runtime ensure helper to delegate to it.

This prepares safe, non-destructive tile-state initialization before accepted floor/blocker marking is wired.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing safe tile-state initialization,
- avoiding destructive reinitialization,
- setting up later floor/blocker marking.

What needs to be done next:
- confirm setup call path,
- wire ensure call,
- mark accepted generated floors and blockers,
- then use real tile data in direct door validation.

## 0.9.10de — Phase 3 Tile-State Initialization Probe

Continued Phase 3 from tile-state initialization / marking helpers.

Searched for surface setup and map-dimension candidates.

Candidate findings:
WorldRuntimeGenerationFramework.java near line 4154:
```java
4151:         }
4152: 
4153:         surfaceAuthority.initializePhase3TileState(mapWidth, mapHeight);
4154:         phase3Debug("TileState", "Initialized Phase 3 tile state width=" + mapWidth + ", height=" + mapHeight);
4155:     }
4156: 
4157:     /**
4158:      * Phase 3 helper for marking a generated ordinary floor rectangle.
4159:      *
4160:      * This should be called only after placement validation accepts the rectangle.
4161:      */
```

WorldRuntimeGenerationFramework.java near line 4153:
```java
4150:             return;
4151:         }
4152: 
4153:         surfaceAuthority.initializePhase3TileState(mapWidth, mapHeight);
4154:         phase3Debug("TileState", "Initialized Phase 3 tile state width=" + mapWidth + ", height=" + mapHeight);
4155:     }
4156: 
4157:     /**
4158:      * Phase 3 helper for marking a generated ordinary floor rectangle.
4159:      *
4160:      * This should be called only after placement validation accepts the rectangle.
```

WorldSimulationFramework.java near line 84:
```java
81:     WorldGenerationScaleProfile(String id, String label, int minWidth, int maxWidth, int minHeight, int maxHeight,
82:                                 int minRooms, int maxRooms, int defaultRoomTarget,
83:                                 int plazaMinSize, int plazaPreferredSize, int edgeMargin, String note){
84:         this.id=id; this.label=label; this.minWidth=minWidth; this.maxWidth=maxWidth; this.minHeight=minHeight; this.maxHeight=maxHeight;
85:         this.minRooms=minRooms; this.maxRooms=maxRooms; this.defaultRoomTarget=defaultRoomTarget;
86:         this.plazaMinSize=plazaMinSize; this.plazaPreferredSize=plazaPreferredSize; this.edgeMargin=edgeMargin; this.note=note;
87:     }
88: }
89: 
90: 
91: class HiveWorldDefinition {
```

WorldRuntimeGenerationFramework.java near line 3648:
```java
3645:             int y,
3646:             int width,
3647:             int height) {
3648:         return isOrdinaryPlacementInsidePhase3InteriorBounds(zoneType, mapWidth, mapHeight, x, y, width, height);
3649:     }
3650: 
3651: 
3652:     /**
3653:      * Phase 3 room-placement failure accounting scaffold.
3654:      *
3655:      * Placement loops should call this when an ordinary room candidate is rejected
```

StampedModuleSpec.java near line 20:
```java
17:     final int corridorLength, corridorWidth, roomWidth, roomHeight, maxRooms, preferredSide, laneOffset;
18:     final StampedRoomSpec[] rooms;
19:     StampedModuleSpec(String name, String corridorDress, int corridorLength, int corridorWidth, int roomWidth, int roomHeight, int preferredSide, int laneOffset, StampedRoomSpec... rooms){
20:         this.name=name; this.corridorDress=corridorDress; this.corridorLength=corridorLength; this.corridorWidth=corridorWidth; this.roomWidth=roomWidth; this.roomHeight=roomHeight; this.preferredSide=preferredSide; this.laneOffset=laneOffset; this.rooms=rooms; this.maxRooms=rooms==null?0:rooms.length;
21:     }
22: }
```

MapLayerSurfaceAuthority.java near line 84:
```java
81:      * This does not mark anything traversable by default. Unknown remains blocked
82:      * until generation explicitly marks safe floor/receiver tiles.
83:      */
84:     public void initializePhase3TileState(int width, int height) {
85:         int safeWidth = Math.max(0, width);
86:         int safeHeight = Math.max(0, height);
87:         phase3TraversableTiles = new boolean[safeWidth][safeHeight];
88:         phase3BlockedTiles = new boolean[safeWidth][safeHeight];
89: 
90:         for (int x = 0; x < safeWidth; x++) {
91:             for (int y = 0; y < safeHeight; y++) {
```

ZoneGenerationContext.java near line 302:
```java
299:         return ConnectionValidationMode.ORTHOGONAL_ONLY;
300:     }
301: 
302:     public static Rect deriveInteriorBounds(int mapWidth, int mapHeight, int edgeBandTiles) {
303:         int band = Math.max(MIN_EDGE_SAFETY_BAND_TILES, edgeBandTiles);
304:         int x = band;
305:         int y = band;
306:         int width = Math.max(0, mapWidth - band * 2);
307:         int height = Math.max(0, mapHeight - band * 2);
308:         return new Rect(x, y, width, height);
309:     }
```

ZoneGenerationContext.java near line 153:
```java
150:                 mapWidth,
151:                 mapHeight,
152:                 edgeBand,
153:                 deriveInteriorBounds(mapWidth, mapHeight, edgeBand));
154:     }
155: 
156:     public static ZoneFamily deriveZoneFamily(ZoneType zoneType) {
157:         if (zoneType == null) {
158:             return ZoneFamily.ROADLESS_TUNNEL_WARREN;
159:         }
160: 
```

ZoneGenerationContext.java near line 139:
```java
136:         this.interiorGenerationBounds = interiorGenerationBounds;
137:     }
138: 
139:     public static ZoneGenerationContext create(ZoneType zoneType, int mapWidth, int mapHeight) {
140:         ZoneFamily family = deriveZoneFamily(zoneType);
141:         int edgeBand = MIN_EDGE_SAFETY_BAND_TILES;
142:         return new ZoneGenerationContext(
143:                 family,
144:                 deriveRoadMode(family),
145:                 deriveAnchorType(family),
146:                 deriveEdgeBandPolicy(family),
```

WorldSimulationFramework.java near line 81:
```java
78:     final int minRooms, maxRooms, defaultRoomTarget;
79:     final int plazaMinSize, plazaPreferredSize, edgeMargin;
80:     final String note;
81:     WorldGenerationScaleProfile(String id, String label, int minWidth, int maxWidth, int minHeight, int maxHeight,
82:                                 int minRooms, int maxRooms, int defaultRoomTarget,
83:                                 int plazaMinSize, int plazaPreferredSize, int edgeMargin, String note){
84:         this.id=id; this.label=label; this.minWidth=minWidth; this.maxWidth=maxWidth; this.minHeight=minHeight; this.maxHeight=maxHeight;
85:         this.minRooms=minRooms; this.maxRooms=maxRooms; this.defaultRoomTarget=defaultRoomTarget;
86:         this.plazaMinSize=plazaMinSize; this.plazaPreferredSize=plazaPreferredSize; this.edgeMargin=edgeMargin; this.note=note;
87:     }
88: }
```

WorldRuntimeGenerationFramework.java near line 3615:
```java
3612:             int y,
3613:             int width,
3614:             int height) {
3615:         ZoneGenerationContext context = createZoneGenerationContext(zoneType, mapWidth, mapHeight);
3616:         return isOrdinaryPlacementInsidePhase3InteriorBounds(context, x, y, width, height);
3617:     }
3618: 
3619: 
3620:     /**
3621:      * Phase 3 room-placement edge-band gate.
3622:      *
```

WorldRuntimeGenerationFramework.java near line 3577:
```java
3574:      * This does not yet modify placement behavior; it exposes the reserved bounds for the next patch.
3575:      */
3576:     public static ZoneGenerationContext.Rect getPhase3InteriorGenerationBounds(ZoneType zoneType, int mapWidth, int mapHeight) {
3577:         ZoneGenerationContext context = createZoneGenerationContext(zoneType, mapWidth, mapHeight);
3578:         phase3Debug("InteriorBounds", context.interiorGenerationBounds.toString());
3579:         return context.interiorGenerationBounds;
3580:     }
3581: 
3582: 
3583:     /**
3584:      * Phase 3 edge-band placement gate.
```

Patch actions:
- Added ensurePhase3SurfaceTileState(...) setup wrapper.

Added `ensurePhase3SurfaceTileState(...)`, an idempotent setup wrapper that initializes Phase 3 tile state only if it does not already exist.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- identifying safe initialization point,
- avoiding blind constructor/setup wiring,
- preserving fail-closed tile state.

What needs to be done next:
- confirm true surface/map setup method,
- wire initialization there,
- then mark accepted floor/blocker placements.

## 0.9.10dd — Phase 3 Tile-State Initialization / Marking Helpers

Continued Phase 3 from surface tile state scaffolding.

Patch actions:
- Added Phase 3 tile marking helpers for floors, walls, blockers, receivers, and rectangles.
- Added WorldRuntimeGenerationFramework helpers for initializing and marking Phase 3 tile state.

Added helper methods to `MapLayerSurfaceAuthority` for marking Phase 3 floor, wall, blocked, receiver, and rectangular tile states.

Added helper methods to `WorldRuntimeGenerationFramework` for initializing Phase 3 tile state and marking generated floor/blocked rectangles.

Unknown tile state remains fail-closed.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- preparing generation to populate real tile pathability data,
- preserving conservative unknown-tile behavior,
- preparing receiver-tile validation to use real data.

What needs to be done next:
- wire initialization into map/surface setup,
- wire floor/blocker marking into accepted placement commits,
- then use real tile data in direct door validation.

## 0.9.10dc — Phase 3 Surface Tile State Scaffold

Continued Phase 3 from surface tile query adapter wiring.

Relevant standards headings reviewed:
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Plaza / Road / Frontage Validation Standards
- ## Alley / Parking / Service Validation Standards
- ## Integrated District-Layout Validation Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## First Phase 3 Code Patch Standards
- ## Ordinary Room Gate Commit-Path Standards
- ## Ordinary Room Commit Path Deep Probe Standards
- ## Traversable Orthogonal Connection Validation Standards
- ## Room Cluster Direct Connection Planning Standards
- ## Direct Door Receiver-Tile Planning Standards
- ## Receiver-Tile Live Pathability Standards
- ## Phase3TileAccess Adapter Standards
- ## MapLayerSurfaceAuthority Tile Adapter Standards
- ## Surface Tile Query Adapter Standards

Existing tile-state inspection:
- line 6: `static int floor(int layer) {`
- line 16: `static int displayLayerIndex(int floor, boolean sewer) {`
- line 17: `int f = Math.max(1, Math.min(10, floor));`
- line 22: `int floorNum = floor(layer);`
- line 23: `return isSewer(layer) ? ("Floor " + floorNum + "B sewer") : ("Floor " + floorNum);`
- line 27: `if (z == ZoneType.NEUTRAL_CIVILIAN_FLOOR) return "C";`
- line 45: `* Phase 3 conservative tile traversal query.`
- line 47: `* This is intentionally conservative until the full live tile grid authority is`
- line 48: `* wired. It provides a confirmed query surface for Phase3TileAccess and fails`
- line 51: `public boolean isPhase3TraversableTile(int x, int y) {`
- line 56: `* Phase 3 conservative tile blockage query.`
- line 58: `* This is intentionally conservative until live tile/object occupancy is wired.`
- line 59: `* Unknown tiles are treated as blocked.`
- line 61: `public boolean isPhase3BlockedTile(int x, int y) {`

Patch actions:
- Added optional phase3TraversableTiles / phase3BlockedTiles fields.
- Added Phase 3 tile state initialization and setter helpers.
- Wired isPhase3TraversableTile(...) / isPhase3BlockedTile(...) to optional Phase 3 tile state arrays.

Added optional Phase 3 tile-state arrays to `MapLayerSurfaceAuthority`:
- `phase3TraversableTiles`
- `phase3BlockedTiles`

Added initialization and setter helpers. Updated tile query methods to use these arrays when initialized while preserving fail-closed behavior for unknown/out-of-bounds tiles.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- creating explicit tile-state storage,
- preserving conservative fail-closed behavior,
- preparing receiver-tile validation to use real data.

What needs to be done next:
- initialize tile state during generation,
- mark generated floor/receiver tiles,
- mark walls/fixtures blocked,
- then use this adapter in direct room-to-room validation.

## 0.9.10db — Phase 3 Surface Tile Query Adapter Wiring

Continued Phase 3 from tile access candidate inspection / adapter scaffold.

Relevant standards headings reviewed:
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Plaza / Road / Frontage Validation Standards
- ## Alley / Parking / Service Validation Standards
- ## Integrated District-Layout Validation Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## First Phase 3 Code Patch Standards
- ## Ordinary Room Gate Commit-Path Standards
- ## Ordinary Room Commit Path Deep Probe Standards
- ## Traversable Orthogonal Connection Validation Standards
- ## Room Cluster Direct Connection Planning Standards
- ## Direct Door Receiver-Tile Planning Standards
- ## Receiver-Tile Live Pathability Standards
- ## Phase3TileAccess Adapter Standards
- ## MapLayerSurfaceAuthority Tile Adapter Standards

Patch actions:
- Added conservative isPhase3TraversableTile(...) and isPhase3BlockedTile(...) to MapLayerSurfaceAuthority.
- Wired createPhase3TileAccess(MapLayerSurfaceAuthority) to confirmed conservative tile query methods.

Added conservative confirmed query methods to `MapLayerSurfaceAuthority`:
- `isPhase3TraversableTile(int x, int y)`
- `isPhase3BlockedTile(int x, int y)`

Wired `createPhase3TileAccess(MapLayerSurfaceAuthority)` to those methods.

Current behavior remains fail-closed:
- traversable returns false,
- blocked returns true,
until real tile grid/state data is connected.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- creating a real adapter shape,
- preserving fail-closed safety,
- preparing for actual tile-state integration.

What needs to be done next:
- inspect actual surface/tile state storage,
- wire query bodies to real data,
- then use real adapter in direct room-to-room validation.

## 0.9.10da — Phase 3 Tile Access Candidate Inspection / Adapter Scaffold

Continued Phase 3 from fail-closed tile access probing.

Inspected strongest likely tile/map/pathability candidate files:
- MapLayerSurfaceAuthority.java
- WorldRuntimeGenerationFramework.java
- WorldSimulationFramework.java
- AbstractDistantZoneSimulation.java

Relevant standards headings reviewed:
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Room Clustering and Frontage Grouping Standards
- ## Plaza / Road / Frontage Validation Standards
- ## Alley / Parking / Service Validation Standards
- ## Integrated District-Layout Validation Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## First Phase 3 Code Patch Standards
- ## Room Placement Fallback / Dynamic Zone Expansion Standards
- ## Ordinary Room Placement Gate Standards
- ## Ordinary Room Gate Call-Site Probe Standards
- ## Ordinary Room Gate Commit-Path Standards
- ## Ordinary Room Commit Path Deep Probe Standards
- ## Traversable Orthogonal Connection Validation Standards
- ## Direct Room-to-Room Shared-Wall Placement Standards
- ## Room Cluster Direct Connection Planning Standards
- ## Direct Door Receiver-Tile Planning Standards
- ## Receiver-Tile Live Pathability Standards
- ## Phase3TileAccess Adapter Standards

Patch actions:
- Added MapLayerSurfaceAuthority Phase3TileAccess adapter scaffold, fail-closed until real queries are confirmed.

Added a `createPhase3TileAccess(MapLayerSurfaceAuthority surfaceAuthority)` adapter scaffold. It intentionally fails closed until confirmed tile traversability and blockage queries are identified.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- locating the actual tile authority,
- preserving fail-closed safety,
- preparing a real adapter path.

What needs to be done next:
- identify or add confirmed tile query methods,
- wire the adapter,
- then use it in direct room-to-room receiver validation.

## 0.9.10cz — Phase 3 Tile Access Adapter Probe / Fail-Closed Pathability

Continued Phase 3 from receiver-tile live pathability scaffold.

Searched the Java source tree for likely tile/map/pathability access points.

Candidate findings:
WorldRuntimeGenerationFramework.java near line 2836:
```java
2833:         }
2834:         return ensurePlayerSpawnPoint();
2835:     }
2836:     boolean walkable(int x,int y){ if(x<0||y<0||x>=w||y>=h)return false; char t=tiles[x][y]; if(t=='#' || InterstitialInfrastructureApi.isInterstitialSolid(t) || t==InterstitialInfrastructureApi.VOID_SPACE) return false; if(t=='|'||t=='L'||t=='X'||t=='V'||t=='1'||t=='2'||t=='3'||t=='4'||t=='5'||t=='I'||t=='H'||"YJBKOZPFUSRW D".replace(" ","").indexOf(t)>=0) return false; return true; }
2837:     int locationKey(){ return (((sectorX*10+sectorY)*10+zoneX)*10+zoneY)*100 + (floor+20)*2 + (sewerLayer?1:0); }
2838:     String zoneCoordText(){ return zoneX + "," + zoneY + " in sector " + sectorX + "," + sectorY; }
2839:     String layerText(){ return "Floor " + floor + (sewerLayer ? "B sewer" : ""); }
2840:     java.util.List<String> mapScaffoldLines(){ return diegeticMapLines(floor); }
2841: 
2842:     java.util.List<String> diegeticMapLines(int layerView){
```

WorldRuntimeGenerationFramework.java near line 1757:
```java
1754:             for(int ox=-1; ox<=1; ox++) for(int oy=-1; oy<=1; oy++){
1755:                 if(Math.abs(ox)+Math.abs(oy)!=1) continue;
1756:                 int nx=p.x+ox, ny=p.y+oy;
1757:                 if(inBounds(nx,ny) && roomIds[nx][ny] < 0 && tiles[nx][ny] != '+' && tiles[nx][ny] != '=' && tiles[nx][ny] != '~' && tiles[nx][ny] != ',' && tiles[nx][ny] != ':' && tiles[nx][ny] != ';') {
1758:                     if(tiles[nx][ny] == '#') tiles[nx][ny] = '#';
1759:                 }
1760:             }
1761:         }
1762:     }
1763: 
```

WorldRuntimeGenerationFramework.java near line 2564:
```java
2561:         return ensurePlayerSpawnPoint();
2562:     }
2563: 
2564:     boolean walkableAdjacentOrSame(int x,int y){ if(walkable(x,y)) return true; int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}}; for(int[] d:dirs) if(walkable(x+d[0],y+d[1])) return true; return false; }
2565: 
2566:     MapObjectState mapObjectAt(int x,int y){
2567:         for(MapObjectState m: mapObjects) if(m.x==x && m.y==y) return m;
2568:         return null;
2569:     }
2570: 
```

MediaRuntimeFramework.java near line 202:
```java
199:     void bind(char glyph, String alias) { BufferedImage img = byAlias.get(alias); if (img != null) byGlyph.put(glyph, img); }
200: 
201:     void bindGlyphs() {
202:         bind('.', "floor_bare_underhive"); bind('`', "floor_trash_mutant_rough"); bind(';', "tile_road_north_south"); bind('_', "tile_road_east_west"); bind('+', "floor_industrial_corridor"); bind('=', "floor_maintenance_corridor"); bind(',', "floor_alleyway_cracked"); bind('~', "floor_sewer_pipe_corridor"); bind(':', "floor_padded_service_way"); bind('-', "floor_exterior_hivewall_maintenance"); bind(' ', "void_space");
203:         bind('#', "wall_bulkhead"); bind('%', "wall_support_beam"); bind('&', "wall_gantry_lattice"); bind('^', "wall_buried_conveyor"); bind('8', "wall_pipe_bundle"); bind('0', "wall_cable_column"); bind('/', "door_archway"); bind('|', "door_standard"); bind('L', "door_locked"); bind('V', "door_vent_panel"); bind('X', "door_security"); bind('D', "door_double"); bind('d', "barricade");
204:         bind('*', "debris"); bind('?', "buried_cache"); bind('!', "danger_marker"); bind('R', "rogue_machine"); bind('N', "noisy_machinery"); bind('1', "vending_food"); bind('2', "vending_armor"); bind('3', "vending_weapons"); bind('4', "vending_materials"); bind('5', "vending_survival");
205:         bind('Y', "water_condenser"); bind('J', "emergency_assembler"); bind('B', "emergency_boiler"); bind('K', "micro_lab"); bind('O', "emergency_miner"); bind('Z', "relay_power_grid"); bind('P', "emergency_smelter"); bind('F', "steam_engine"); bind('U', "steam_engine_disabled"); bind('w', "scrap_workbench"); bind('e', "water_condenser"); bind('f', "emergency_smelter"); bind('l', "micro_lab"); bind('x', "security_cogitator"); bind('T', "turret_or_trade"); bind('H', "shrine_or_shield"); bind('G', "logistics_center"); bind('M', "medicae_or_military"); bind('k', "carrying_station"); bind('q', "supply_post");
206:         bind('I', "imperial_shrine"); bind('$', "donation_box"); bind('W', "saint_alcove"); bind('Q', "governor_dais"); bind('C', "clinic"); bind('r', "corpse_loot"); bind('o', "object_generic"); bind('S', "sewer_hatch"); bind('v', "ladder_drain"); bind('E', "elevator"); bind('s', "storage_crate"); bind('c', "sleeping_cot"); bind('u', "water_barrel"); bind('a', "alarm_trap"); bind('p', "arbites_precinct"); bind('b', "bandit_den"); bind('h', "hiver_block"); bind('n', "noble_secure"); bind('t', "table_prop");
207:         bind('g', "bandit_den"); bind('m', "floor_trash_mutant_rough"); bind('A', "arbites_precinct");
208:     }
```

GameOptionsFramework.java near line 87:
```java
84:                 o.importedPortraits = Boolean.parseBoolean(pr.getProperty("importedPortraits", String.valueOf(o.importedPortraits)));
85:                 o.tileIconRendering = Boolean.parseBoolean(pr.getProperty("tileIconRendering", String.valueOf(o.tileIconRendering)));
86:                 o.artQualityIndex = Math.max(0, Math.min(ART_QUALITY_LABELS.length-1, Integer.parseInt(pr.getProperty("artQualityIndex", String.valueOf(o.artQualityIndex)))));
87:                 o.mapTileSizeIndex = Math.max(0, Math.min(MAP_TILE_SIZE_LABELS.length-1, Integer.parseInt(pr.getProperty("mapTileSizeIndex", String.valueOf(o.mapTileSizeIndex)))));
88:                 o.worldZoomIndex = Math.max(0, Math.min(WORLD_ZOOM_LABELS.length-1, Integer.parseInt(pr.getProperty("worldZoomIndex", String.valueOf(o.worldZoomIndex)))));
89:                 o.hoverHelp = Boolean.parseBoolean(pr.getProperty("hoverHelp", String.valueOf(o.hoverHelp)));
90:                 o.screenSaver = Boolean.parseBoolean(pr.getProperty("screenSaver", String.valueOf(o.screenSaver)));
91:                 o.sfxVolume = Integer.parseInt(pr.getProperty("sfxVolume", String.valueOf(o.sfxVolume)));
92:                 o.musicVolume = Integer.parseInt(pr.getProperty("musicVolume", String.valueOf(o.musicVolume)));
93:                 o.conversationVolume = Integer.parseInt(pr.getProperty("conversationVolume", String.valueOf(o.conversationVolume)));
```

WorldRuntimeGenerationFramework.java near line 4066:
```java
4063: 
4064:         boolean firstTraversable = tileAccess.isTraversable(receivers[0].x, receivers[0].y)
4065:                 && !tileAccess.isBlocked(receivers[0].x, receivers[0].y);
4066:         boolean secondTraversable = tileAccess.isTraversable(receivers[1].x, receivers[1].y)
4067:                 && !tileAccess.isBlocked(receivers[1].x, receivers[1].y);
4068: 
4069:         if (!firstTraversable || !secondTraversable) {
4070:             phase3Debug("DirectRoomDoorReject", "receiver tile pathability failed; first="
4071:                     + receivers[0] + ", firstTraversable=" + firstTraversable
4072:                     + ", second=" + receivers[1] + ", secondTraversable=" + secondTraversable);
```

WorldRuntimeGenerationFramework.java near line 4064:
```java
4061:             return false;
4062:         }
4063: 
4064:         boolean firstTraversable = tileAccess.isTraversable(receivers[0].x, receivers[0].y)
4065:                 && !tileAccess.isBlocked(receivers[0].x, receivers[0].y);
4066:         boolean secondTraversable = tileAccess.isTraversable(receivers[1].x, receivers[1].y)
4067:                 && !tileAccess.isBlocked(receivers[1].x, receivers[1].y);
4068: 
4069:         if (!firstTraversable || !secondTraversable) {
4070:             phase3Debug("DirectRoomDoorReject", "receiver tile pathability failed; first="
```

WorldRuntimeGenerationFramework.java near line 4024:
```java
4021:      */
4022:     public interface Phase3TileAccess {
4023:         boolean isTraversable(int x, int y);
4024:         boolean isBlocked(int x, int y);
4025:     }
4026: 
4027:     /**
4028:      * Live receiver-tile validation for direct room-to-room doors.
4029:      *
4030:      * This extends the earlier geometry-only receiver-tile check by consulting a
```

WorldRuntimeGenerationFramework.java near line 2962:
```java
2959: 
2960:     static class Result {
2961:         int inset, corridors, maintenanceRooms, dangerRooms, voidTiles, highWallTiles;
2962:         String summary(){ return "inset="+inset+" corridors="+corridors+" maintenanceRooms="+maintenanceRooms+" dangerRooms="+dangerRooms+" voidTiles="+voidTiles+" highWallTiles="+highWallTiles; }
2963:     }
2964: 
2965:     static String policySummary(){
2966:         return "post-validation bounded hivewall: exterior maintenance loop, one bolted-on maintenance room, 3-4 abandoned high-danger interwall rooms, void abyss outside high-wall envelope";
2967:     }
2968:     static int maintenanceRoomTarget(){ return 1; }
```

WorldRuntimeGenerationFramework.java near line 2628:
```java
2625:         for (NpcEntity n : npcs) {
2626:             if (n == null) continue;
2627:             String key = occKey(n.x, n.y);
2628:             boolean badTerrain = !inBounds(n.x,n.y) || !walkable(n.x,n.y);
2629:             boolean stacked = (n.x == playerX && n.y == playerY) || occupied.contains(key);
2630:             boolean illegal = badTerrain || stacked;
2631:             if (illegal) {
2632:                 int oldX = n.x, oldY = n.y;
2633:                 Point p = nearestUnoccupiedWalkableTo(n.x, n.y, playerX, playerY, occupied);
2634:                 if (p == null && rrng != null) p = randomOpenPoint(rrng);
```

WorldRuntimeGenerationFramework.java near line 1652:
```java
1649:     }
1650: 
1651:     boolean regionTouchesNonWall(Rectangle rr){ for(int x=Math.max(0,rr.x); x<Math.min(w,rr.x+rr.width); x++) for(int y=Math.max(0,rr.y); y<Math.min(h,rr.y+rr.height); y++) if(roomIds[x][y] >= 0 || tiles[x][y]!='#') return true; return false; }
1652:     void carve(Rectangle rr){ int id=rooms.size(); RoomProfile rp = RoomProfile.forZone(zoneType, r); roomProfiles.add(rp); roomFactions.add(Faction.NONE); roomSpecials.add(Boolean.FALSE); char floorChar = zoneType.floorGlyph(r); for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++){ roomIds[x][y]=id; boolean wall = (x==rr.x || y==rr.y || x==rr.x+rr.width-1 || y==rr.y+rr.height-1); tiles[x][y]= wall ? '#' : floorChar; } }
1653: 
1654:     void buildGuaranteedRoomLattice(int target){
1655:         target = WorldGenerationApi.clampRoomTarget(target);
1656:         int cols = 6;
1657:         int rows = 5;
1658:         int cellW = Math.max(10, (w-4) / cols);
```

WorldRuntimeGenerationFramework.java near line 753:
```java
750:     Point nearestTransitCorridorPoint(int sx, int sy){
751:         Point best=null; int bd=999999;
752:         for(int x=1; x<w-1; x++) for(int y=1; y<h-1; y++){
753:             if(!(isCorridorGlyph(tiles[x][y]) || tiles[x][y]=='=' || tiles[x][y]==':' || tiles[x][y]=='+' || tiles[x][y]=='D')) continue;
754:             int edgeDist = Math.min(Math.min(x, w-1-x), Math.min(y, h-1-y));
755:             if(edgeDist > Math.max(10, Math.min(w,h)/5)) continue;
756:             int d = Math.abs(x-sx)+Math.abs(y-sy);
757:             if(d < bd){ bd=d; best=new Point(x,y); }
758:         }
759:         return best;
```

Relevant standards headings reviewed:
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Room Clustering and Frontage Grouping Standards
- ## Plaza / Road / Frontage Validation Standards
- ## Alley / Parking / Service Validation Standards
- ## Integrated District-Layout Validation Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## First Phase 3 Code Patch Standards
- ## Room Placement Fallback / Dynamic Zone Expansion Standards
- ## Ordinary Room Placement Gate Standards
- ## Ordinary Room Gate Call-Site Probe Standards
- ## Ordinary Room Gate Commit-Path Standards
- ## Ordinary Room Commit Path Deep Probe Standards
- ## Traversable Orthogonal Connection Validation Standards
- ## Direct Room-to-Room Shared-Wall Placement Standards
- ## Room Cluster Direct Connection Planning Standards
- ## Direct Door Receiver-Tile Planning Standards
- ## Receiver-Tile Live Pathability Standards

Patch actions:
- Added fail-closed Phase3TileAccess implementation and factory scaffold.

Added fail-closed `Phase3TileAccess` implementation so live pathability does not silently pass while the actual map/tile adapter is not yet connected.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What is being done:
- locating tile access,
- preserving fail-closed validation,
- preparing real adapter hookup.

What needs to be done next:
- inspect strongest tile/map candidates,
- adapt real tile access to Phase3TileAccess,
- then attach direct-door planning to room clustering.

## 0.9.10cy — Phase 3 Receiver-Tile Live Pathability Scaffold

Continued Phase 3 from queue review and geometry-only direct door receiver-tile planning.

Relevant standards headings reviewed:
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Frontage and Door Orientation Standards
- ## Room Clustering and Frontage Grouping Standards
- ## Plaza / Road / Frontage Validation Standards
- ## Alley / Parking / Service Validation Standards
- ## Integrated District-Layout Validation Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## Central Anchor Placement Implementation Standards
- ## First Phase 3 Code Patch Standards
- ## Room Placement Fallback / Dynamic Zone Expansion Standards
- ## Edge-Bounds Ordinary Placement Gate Standards
- ## Ordinary Room Placement Gate Standards
- ## Ordinary Room Gate Call-Site Probe Standards
- ## Ordinary Room Gate Commit-Path Standards
- ## Ordinary Room Commit Path Deep Probe Standards
- ## Traversable Orthogonal Connection Validation Standards
- ## Direct Room-to-Room Shared-Wall Placement Standards
- ## Room Cluster Direct Connection Planning Standards
- ## Direct Door Receiver-Tile Planning Standards

Patch actions:
- Added Phase3TileAccess adapter and live receiver-tile pathability validation helper.

Added a small `Phase3TileAccess` adapter and live receiver-tile validation helper so later map/tile code can verify that both sides of a direct room-to-room door are actually traversable and unblocked.

The helper fails closed when no tile access is available so live pathability is not silently skipped.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

What was done:
- prepared live pathability bridge,
- preserved geometry-only validation,
- kept direct room-to-room attachment deferred until the room clustering loop is confirmed.

What needs to be done next:
- locate actual map/tile data access,
- adapt it to Phase3TileAccess,
- attach live receiver validation to direct room door candidate acceptance,
- then attach direct-door planning to confirmed room clustering.

## 0.9.10cx — Phase 3 Queue Review / Direct Door Receiver-Tile Planning

Continued Phase 3 after reviewing standards/practices and the master development plan.

Relevant standards headings reviewed:
- ## Continuation reporting standard
- ## Zone Transition / Adjacency Standards
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Frontage and Door Orientation Standards
- ## Edge Safety Generation Standards
- ## Room Clustering and Frontage Grouping Standards
- ## Plaza / Road / Frontage Validation Standards
- ## Alley / Parking / Service Validation Standards
- ## Integrated District-Layout Validation Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## Edge Safety Band Reservation Implementation Standards
- ## Central Anchor Placement Implementation Standards
- ## First Phase 3 Code Patch Standards
- ## Room Placement Fallback / Dynamic Zone Expansion Standards
- ## Context Attachment Correction / Edge-Bounds Scaffold Standards
- ## Edge-Bounds Ordinary Placement Gate Standards
- ## Ordinary Room Placement Gate Standards
- ## Ordinary Room Gate Call-Site Probe Standards
- ## Ordinary Room Gate Commit-Path Standards
- ## Ordinary Room Commit Path Deep Probe Standards
- ## Traversable Orthogonal Connection Validation Standards
- ## Direct Room-to-Room Shared-Wall Placement Standards
- ## Room Cluster Direct Connection Planning Standards

Relevant master plan headings reviewed:
- ### Required edge-band context fields
- ### Suggested edge band policies
- ### Placement rule
- ### Edge-authorized tags
- ### Validation doctrine
- ## Active Phase 3 Bucket 1Q — Code Implementation Planning: Central Anchor Placement
- ### Placement doctrine
- ### Validation doctrine
- ## Active Phase 3 Bucket 1R — Code Implementation Planning: Roaded vs Roadless Circulation Branching
- ### Validation doctrine
- ## Active Phase 3 Bucket 1S — First Code Patch Planning
- ## Active Phase 3 Bucket 1T — Room Placement Fallback Safety / Dynamic Zone Expansion
- ### Edge safety band minimum
- ### Room placement failure doctrine
- ### Suggested placement attempt policy
- ## Active Phase 3 Bucket 1U — Source Inspection / Exact First Patch Plan
- ## Active Phase 3 Bucket 1V — Generator Source Gate / Implementation Blocker Handling
- ## Active Phase 3 Bucket 1W — First Scaffolding Code Patch
- ## Active Phase 3 Bucket 1X — ZoneGenerationContext Entry-Point Attachment
- ## Active Phase 3 Bucket 1Y — Context Attachment Correction / Edge-Bounds Scaffold
- ## Active Phase 3 Bucket 1Z — Edge-Bounds Ordinary Placement Gate
- ## Active Phase 3 Bucket 2A — Standards Check / Ordinary Room Gate Preparation
- ## Active Phase 3 Bucket 2B — Ordinary Room Gate Call-Site Probe
- ## Active Phase 3 Bucket 2C — Ordinary Room Gate Commit-Path Planning
- ## Active Phase 3 Bucket 2D — Ordinary Room Commit Path Deep Probe
- ## Active Phase 3 Bucket 2E — Traversable Orthogonal Connection Validation
- ## Active Phase 3 Bucket 2F — Direct Room-to-Room Shared-Wall Placement
- ## Active Phase 3 Bucket 2G — Room Cluster Direct Connection Planning

Confirmed current place in the Phase 3 queue:
- current phase is Phase 3 spatial integration;
- current sub-chain is room/connection validation and safe placement scaffolding;
- active target is direct shared-wall door receiver-tile planning;
- next targets are room-clustering attachment, ordinary room commit gate wiring, corridor validation, and then broader district-layout validation implementation.

Patch actions:
- Added geometry-only direct door receiver tile planning and safety helpers.
- Integrated geometry-only receiver tile safety check into direct room door candidate planning.

Added geometry-only receiver-tile planning for direct room-to-room doors and integrated safe-bounds receiver validation into direct door candidate planning.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

## 0.9.10cw — Phase 3 Room Cluster Direct Connection Planning

Continued Phase 3 after checking standards and practices.

Relevant standards headings reviewed:
- ## Zone Transition / Adjacency Standards
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Frontage and Door Orientation Standards
- ## Edge Safety Generation Standards
- ## Room Clustering and Frontage Grouping Standards
- ## Plaza / Road / Frontage Validation Standards
- ## Alley / Parking / Service Validation Standards
- ## Integrated District-Layout Validation Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## Edge Safety Band Reservation Implementation Standards
- ## Central Anchor Placement Implementation Standards
- ## First Phase 3 Code Patch Standards
- ## Room Placement Fallback / Dynamic Zone Expansion Standards
- ## Context Attachment Correction / Edge-Bounds Scaffold Standards
- ## Edge-Bounds Ordinary Placement Gate Standards
- ## Ordinary Room Placement Gate Standards
- ## Ordinary Room Gate Call-Site Probe Standards
- ## Ordinary Room Gate Commit-Path Standards
- ## Ordinary Room Commit Path Deep Probe Standards
- ## Traversable Orthogonal Connection Validation Standards
- ## Direct Room-to-Room Shared-Wall Placement Standards

Advanced direct room-to-room adjacency from basic shared-wall validation into room-cluster direct connection planning.

Patch actions:
- Added shared-wall door tile selection and room-cluster direct door planning helpers.
- Added allowsDirectSharedWallRoomAdjacency() to ZoneGenerationContext.
- Gated direct room-to-room feasibility by roomAdjacencyMode.

Added / prepared:
- direct adjacency mode check,
- shared-wall door tile selection helper,
- direct room-to-room door candidate planning helper,
- safe-bounds validation for both rooms and the door tile.

Likely clustering/connection candidates observed:
Candidate near line 2169:
```java
2166:         edgeDoorProblems += validateEdgeDoorConnectorsFor('E', phase);
2167: 
2168:         int totalProblems = unreachableRooms + doorAdjacencyProblems + smallRoomWallDoorViolations + largeRoomSpacingViolations + shortCorridorComponents + squatCorridorComponents + edgeDoorProblems;
2169:         String summary = "phase="+phase+" problems="+totalProblems+" rooms="+rooms.size()+" unreachableRooms="+unreachableRooms+" weakDoors="+doorAdjacencyProblems+" smallRoomDoorWallViolations="+smallRoomWallDoorViolations+" largeDoorSpacingViolations="+largeRoomSpacingViolations+" shortCorridors="+shortCorridorComponents+" squatCorridors="+squatCorridorComponents+" edgeDoorProblems="+edgeDoorProblems+" zone="+zoneType.label+" layer="+layerText()+" seed="+seed;
2170:         if(totalProblems > 0) DebugLog.warn("WORLDGEN_VALIDATE", summary);
2171:         else DebugLog.audit("WORLDGEN_VALIDATE", summary);
2172:     }
2173: 
2174:     String rectText(Rectangle rr){ return rr.x+","+rr.y+","+rr.width+","+rr.height; }
2175: 
```

Candidate near line 1482:
```java
1479:             }
1480:             commitStrictConnection(best);
1481:             connected.add(best.roomB);
1482:             DebugLog.audit("LEVELGEN_CONNECT", "room="+best.roomA+"->"+best.roomB+" path="+best.path.size()+" doorA="+best.doorA.x+","+best.doorA.y+" doorB="+best.doorB.x+","+best.doorB.y);
1483:         }
1484:         return connected.size() == rooms.size();
1485:     }
1486: 
1487:     static class ConnectionChoice {
1488:         int roomA, roomB, score; Point doorA, doorB; java.util.List<Point> path; DoorType typeA, typeB;
```

Candidate near line 2168:
```java
2165:         edgeDoorProblems += validateEdgeDoorConnectorsFor('W', phase);
2166:         edgeDoorProblems += validateEdgeDoorConnectorsFor('E', phase);
2167: 
2168:         int totalProblems = unreachableRooms + doorAdjacencyProblems + smallRoomWallDoorViolations + largeRoomSpacingViolations + shortCorridorComponents + squatCorridorComponents + edgeDoorProblems;
2169:         String summary = "phase="+phase+" problems="+totalProblems+" rooms="+rooms.size()+" unreachableRooms="+unreachableRooms+" weakDoors="+doorAdjacencyProblems+" smallRoomDoorWallViolations="+smallRoomWallDoorViolations+" largeDoorSpacingViolations="+largeRoomSpacingViolations+" shortCorridors="+shortCorridorComponents+" squatCorridors="+squatCorridorComponents+" edgeDoorProblems="+edgeDoorProblems+" zone="+zoneType.label+" layer="+layerText()+" seed="+seed;
2170:         if(totalProblems > 0) DebugLog.warn("WORLDGEN_VALIDATE", summary);
2171:         else DebugLog.audit("WORLDGEN_VALIDATE", summary);
2172:     }
2173: 
2174:     String rectText(Rectangle rr){ return rr.x+","+rr.y+","+rr.width+","+rr.height; }
```

Candidate near line 2759:
```java
2756:         Rectangle spawnRoom = rooms.get(0);
2757:         Point c=center(spawnRoom);
2758:         if(inBounds(c.x,c.y) && walkable(c.x,c.y) && roomIds[c.x][c.y] == 0) return c;
2759:         for(int x=spawnRoom.x;x<spawnRoom.x+spawnRoom.width;x++) for(int y=spawnRoom.y;y<spawnRoom.y+spawnRoom.height;y++) if(inBounds(x,y)&&walkable(x,y)&&roomIds[x][y]==0) return new Point(x,y);
2760:         return ensurePlayerSpawnPoint();
2761:     }
2762:     Point ensurePlayerSpawnPoint(){
2763:         for(int i=0;i<rooms.size();i++){
2764:             Rectangle rr = rooms.get(i);
2765:             Point c = center(rr);
```

Candidate near line 1652:
```java
1649:     }
1650: 
1651:     boolean regionTouchesNonWall(Rectangle rr){ for(int x=Math.max(0,rr.x); x<Math.min(w,rr.x+rr.width); x++) for(int y=Math.max(0,rr.y); y<Math.min(h,rr.y+rr.height); y++) if(roomIds[x][y] >= 0 || tiles[x][y]!='#') return true; return false; }
1652:     void carve(Rectangle rr){ int id=rooms.size(); RoomProfile rp = RoomProfile.forZone(zoneType, r); roomProfiles.add(rp); roomFactions.add(Faction.NONE); roomSpecials.add(Boolean.FALSE); char floorChar = zoneType.floorGlyph(r); for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++){ roomIds[x][y]=id; boolean wall = (x==rr.x || y==rr.y || x==rr.x+rr.width-1 || y==rr.y+rr.height-1); tiles[x][y]= wall ? '#' : floorChar; } }
1653: 
1654:     void buildGuaranteedRoomLattice(int target){
1655:         target = WorldGenerationApi.clampRoomTarget(target);
1656:         int cols = 6;
1657:         int rows = 5;
1658:         int cellW = Math.max(10, (w-4) / cols);
```

Candidate near line 1489:
```java
1486: 
1487:     static class ConnectionChoice {
1488:         int roomA, roomB, score; Point doorA, doorB; java.util.List<Point> path; DoorType typeA, typeB;
1489:         ConnectionChoice(int a,int b,int score,Point da,Point db,java.util.List<Point> path,DoorType ta,DoorType tb){this.roomA=a;this.roomB=b;this.score=score;this.doorA=da;this.doorB=db;this.path=path;this.typeA=ta;this.typeB=tb;}
1490:     }
1491: 
1492:     ConnectionChoice previewBestConnection(int aIdx, int bIdx){
1493:         Rectangle aRoom = rooms.get(aIdx), bRoom = rooms.get(bIdx);
1494:         java.util.List<Point> aDoors = doorCandidates(aRoom, center(bRoom));
1495:         java.util.List<Point> bDoors = doorCandidates(bRoom, center(aRoom));
```

Candidate near line 956:
```java
953:             if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
954:             made++;
955:         }
956:         DebugLog.audit("ROOM_MODULE_STAMP", "zone="+zoneType.label+" module="+module.name+" side="+side+" rooms="+made+" corridor="+corridor.x+","+corridor.y+","+corridor.width+","+corridor.height);
957:         return made;
958:     }
959: 
960:     java.util.List<Rectangle> moduleCandidateRooms(Rectangle corridor, int side, StampedModuleSpec module){
961:         ArrayList<Rectangle> out = new ArrayList<>();
962:         boolean horizontal = side==1 || side==3;
```

Candidate near line 2962:
```java
2959: 
2960:     static class Result {
2961:         int inset, corridors, maintenanceRooms, dangerRooms, voidTiles, highWallTiles;
2962:         String summary(){ return "inset="+inset+" corridors="+corridors+" maintenanceRooms="+maintenanceRooms+" dangerRooms="+dangerRooms+" voidTiles="+voidTiles+" highWallTiles="+highWallTiles; }
2963:     }
2964: 
2965:     static String policySummary(){
2966:         return "post-validation bounded hivewall: exterior maintenance loop, one bolted-on maintenance room, 3-4 abandoned high-danger interwall rooms, void abyss outside high-wall envelope";
2967:     }
2968:     static int maintenanceRoomTarget(){ return 1; }
```

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

Next stages should attach the planning helper to confirmed room clustering, then add live receiver-tile/pathability validation.

## 0.9.10cv — Direct Room-to-Room Adjacency Compile Fix

Corrected the direct room-to-room adjacency scaffolding compile failure introduced during 0.9.10cu.

Patch actions:
- Inserted missing deriveRoomAdjacencyMode(ZoneFamily family) method.

The missing:
`deriveRoomAdjacencyMode(ZoneFamily family)`

method has now been inserted correctly.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

## 0.9.10cu — Phase 3 Direct Room-to-Room Shared-Wall Placement Scaffolding

Continued Phase 3 after reviewing standards and practices.

Relevant standards headings reviewed:
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Edge Safety Generation Standards
- ## Room Clustering and Frontage Grouping Standards
- ## Plaza / Road / Frontage Validation Standards
- ## Alley / Parking / Service Validation Standards
- ## Integrated District-Layout Validation Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## Edge Safety Band Reservation Implementation Standards
- ## Central Anchor Placement Implementation Standards
- ## First Phase 3 Code Patch Standards
- ## Room Placement Fallback / Dynamic Zone Expansion Standards
- ## Context Attachment Correction / Edge-Bounds Scaffold Standards
- ## Edge-Bounds Ordinary Placement Gate Standards
- ## Ordinary Room Placement Gate Standards
- ## Ordinary Room Gate Call-Site Probe Standards
- ## Ordinary Room Gate Commit-Path Standards
- ## Ordinary Room Commit Path Deep Probe Standards
- ## Traversable Orthogonal Connection Validation Standards

Added direct room-to-room shared-wall placement doctrine.

Rooms may now be planned for direct adjacency when they share a valid orthogonal inter-wall and can support a valid door tile for both rooms.

Patch actions:
- Added RoomAdjacencyMode enum to ZoneGenerationContext.
- Added roomAdjacencyMode field plumbing.
- Added direct room-to-room shared wall and door feasibility helpers.

Added:
- `RoomAdjacencyMode` scaffolding,
- room adjacency mode plumbing,
- shared orthogonal wall validation,
- shared wall length helper,
- direct room-to-room door feasibility helper.

Check result:
javac syntax/import check failed or unavailable.

Compiler details if relevant:
/mnt/data/mechanist_phase3_cu/src/mechanist/ZoneGenerationContext.java:149: error: cannot find symbol
                deriveRoomAdjacencyMode(family),
                ^
  symbol:   method deriveRoomAdjacencyMode(ZoneFamily)
  location: class ZoneGenerationContext
1 error

Future stages will attach this to room clustering, door tile selection, receiver-tile traversal validation, and district-wide connectivity sweeps.

## 0.9.10ct — Traversable Connection Validation Compile Fix

Corrected the connection-validation scaffolding compile failure introduced during 0.9.10cs.

Patch actions:
- Inserted missing deriveConnectionValidationMode(ZoneFamily family) method.

The missing:
`deriveConnectionValidationMode(ZoneFamily family)`

method has now been inserted correctly.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

## 0.9.10cs — Phase 3 Traversable Orthogonal Connection Validation

Continued Phase 3 after reviewing standards and practices documentation.

Relevant standards headings reviewed:
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Sewer Pipe-Network Corridor Standards
- ## Edge Safety Generation Standards
- ## Room Clustering and Frontage Grouping Standards
- ## Plaza / Road / Frontage Validation Standards
- ## Alley / Parking / Service Validation Standards
- ## Integrated District-Layout Validation Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## Edge Safety Band Reservation Implementation Standards
- ## Central Anchor Placement Implementation Standards
- ## First Phase 3 Code Patch Standards
- ## Room Placement Fallback / Dynamic Zone Expansion Standards
- ## Context Attachment Correction / Edge-Bounds Scaffold Standards
- ## Edge-Bounds Ordinary Placement Gate Standards
- ## Ordinary Room Placement Gate Standards
- ## Ordinary Room Gate Call-Site Probe Standards
- ## Ordinary Room Gate Commit-Path Standards
- ## Ordinary Room Commit Path Deep Probe Standards

Added new architectural validation doctrine:
ordinary rooms/corridors are not connected through diagonal-only corner contact.

Patch actions:
- Added ConnectionValidationMode enum to ZoneGenerationContext.
- Added connectionValidationMode field plumbing.
- deriveConnectionValidationMode(...) already present.
- Added isPhase3TraversableOrthogonalConnection(...) scaffold.

Added:
- `ConnectionValidationMode` scaffolding,
- connection validation mode plumbing,
- orthogonal connection validation scaffold,
- and topology-focused traversable connection validation preparation.

Check result:
javac syntax/import check failed or unavailable.

Compiler details if relevant:
/mnt/data/mechanist_phase3_cs/src/mechanist/ZoneGenerationContext.java:136: error: cannot find symbol
                deriveConnectionValidationMode(family),
                ^
  symbol:   method deriveConnectionValidationMode(ZoneFamily)
  location: class ZoneGenerationContext
1 error

Future stages will attach this validation to doorway/corridor placement and later district-wide connectivity sweeps.

## 0.9.10cr — Phase 3 Ordinary Room Commit Path Deep Probe

Advanced Phase 3 from ordinary room gate commit-path planning into a source-wide room commit path probe.

Strongest observed candidates:
RoomManifestApi.java near line 185:
```java
181:             case NOBLE_SERVICE_SPINE: case SECTOR_GOVERNORS_MANSION: return Faction.NOBLE;
182:             default: return Faction.NONE;
183:         }
184:     }
185:     private static void add(ArrayList<StampedRoomSpec> out, String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){ out.add(new StampedRoomSpec(kind,name,desc,chance,faction,loot,contents)); }
186: }
```

RoomManifestApi.java near line 158:
```java
154:     private static StampedModuleSpec module(String name, String dress, int len, int cw, int rw, int rh, int preferredSide, int laneOffset, StampedRoomSpec... rooms){
155:         return new StampedModuleSpec(name, dress, len, cw, rw, rh, preferredSide, laneOffset, rooms);
156:     }
157:     private static StampedRoomSpec spec(String kind, String name, String desc, int chance, Faction faction, String[] loot, char[] contents){
158:         return new StampedRoomSpec(kind, name, desc, chance, faction, loot, contents);
159:     }
160: 
161:     private static void addDefault(ArrayList<StampedRoomSpec> out, ZoneType z){
162:         Faction f = factionFor(z);
163:         add(out,"DORMITORY",z.label+" Dormitory Variant","zone-specific habitation room shaped by "+z.descriptor+"; this is not a generic dormitory but the local answer to sleep, crowding, and control",34,f,new String[]{"cloth scraps","sealed water ration","cheap trinket"},new char[]{'c','s'});
164:         add(out,"CAFETERIA",z.label+" Eating Hall Variant","zone-specific cafeteria or mess: long eating surfaces, benches, refuse bins, and a service counter adapted to the faction occupying this space",36,f,new String[]{"cheap meal tin","sealed water ration","dirty water"},new char[]{'T','b','N'});
165:         add(out,"FOOD_STORE",z.label+" Food Storehouse","food, water, and ration storage distinct from product warehousing; shelves and crates reflect the local faction economy",40,f,new String[]{"sealed water ration","emergency ration","cheap meal tin"},new char[]{'r','b'});
```

WorldRuntimeGenerationFramework.java near line 3129:
```java
3125: 
3126:     static int carveAddedRoom(World world, Rectangle rect, RoomProfile profile, Faction faction, boolean special){
3127:         int rid = world.rooms.size();
3128:         world.rooms.add(rect);
3129:         world.roomProfiles.add(profile);
3130:         world.roomFactions.add(faction == null ? Faction.NONE : faction);
3131:         world.roomSpecials.add(special);
3132:         for(int x=rect.x; x<rect.x+rect.width; x++) for(int y=rect.y; y<rect.y+rect.height; y++){
3133:             if(!world.inBounds(x,y)) continue;
3134:             boolean edge = x==rect.x || y==rect.y || x==rect.x+rect.width-1 || y==rect.y+rect.height-1;
3135:             world.tiles[x][y] = edge ? '#' : '.';
3136:             world.roomIds[x][y] = edge ? -1 : rid;
```

WorldRuntimeGenerationFramework.java near line 3128:
```java
3124:     }
3125: 
3126:     static int carveAddedRoom(World world, Rectangle rect, RoomProfile profile, Faction faction, boolean special){
3127:         int rid = world.rooms.size();
3128:         world.rooms.add(rect);
3129:         world.roomProfiles.add(profile);
3130:         world.roomFactions.add(faction == null ? Faction.NONE : faction);
3131:         world.roomSpecials.add(special);
3132:         for(int x=rect.x; x<rect.x+rect.width; x++) for(int y=rect.y; y<rect.y+rect.height; y++){
3133:             if(!world.inBounds(x,y)) continue;
3134:             boolean edge = x==rect.x || y==rect.y || x==rect.x+rect.width-1 || y==rect.y+rect.height-1;
3135:             world.tiles[x][y] = edge ? '#' : '.';
```

WorldRuntimeGenerationFramework.java near line 2872:
```java
2868:         for(int i=0;i<Math.min(18, roomProfiles.size());i++){
2869:             RoomProfile rp=roomProfiles.get(i); Faction owner=roomFaction(i);
2870:             out.add("room " + i + ": " + (roomSpecials.size()>i && roomSpecials.get(i)?"SPECIAL ":"") + rp.name + " | owner " + owner.label + " | loot " + rp.scavengeChance + "% | " + rp.descriptor + " | features: " + rp.featureText);
2871:         }
2872:         if(roomProfiles.size()>18) out.add("... " + (roomProfiles.size()-18) + " more rooms exist in this zone.");
2873:         return out;
2874:     }
2875: }
2876: 
2877: 
2878: class HivewallRoomCacheApi {
2879:     private HivewallRoomCacheApi() {}
```

WorldRuntimeGenerationFramework.java near line 2773:
```java
2769:         // Last-resort repair: carve a tiny insertion room and explicitly assign roomIds so
2770:         // player spawn always has a real room pointer instead of a naked coordinate.
2771:         Rectangle rr = new Rectangle(Math.max(2,w/2-2), Math.max(2,h/2-2), 5, 5);
2772:         int idx = rooms.size();
2773:         carve(rr); rooms.add(rr);
2774:         if(idx < roomProfiles.size()) roomProfiles.set(idx, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
2775:         if(idx < roomFactions.size()) roomFactions.set(idx, Faction.NONE);
2776:         if(idx < roomSpecials.size()) roomSpecials.set(idx, Boolean.TRUE);
2777:         Point c = center(rr);
2778:         DebugLog.error("PLAYER_SPAWN_REPAIR", "Created emergency insertion room id="+idx+" at "+rr.x+","+rr.y+","+rr.width+","+rr.height, null);
2779:         return c;
2780:     }
```

WorldRuntimeGenerationFramework.java near line 2400:
```java
2396:             if(rr.x < 1 || rr.y < 1 || rr.x+rr.width >= w-1 || rr.y+rr.height >= h-1) continue;
2397:             if(regionTouchesNonWall(grown)) continue;
2398:             int before = rooms.size();
2399:             carve(rr);
2400:             rooms.add(rr);
2401:             if(before < roomProfiles.size()) roomProfiles.set(before, RoomProfile.neutralContestRoom(zoneType, r));
2402:             if(before < roomFactions.size()) roomFactions.set(before, Faction.NONE);
2403:             if(before < roomSpecials.size()) roomSpecials.set(before, Boolean.FALSE);
2404:             ConnectionChoice cc = previewBestConnectionToConnectedSet(before);
2405:             if(cc == null){
2406:                 removeRoomAtIndex(before);
2407:                 continue;
```

WorldRuntimeGenerationFramework.java near line 1777:
```java
1773:         tiles[cx][cy] = zoneType.floorGlyph(r);
1774:         roomIds[cx][cy] = id;
1775:         rooms.add(stub);
1776:         RoomProfile closet = RoomProfile.closetStub(zoneType, r);
1777:         roomProfiles.add(closet);
1778:         roomFactions.add(Faction.NONE);
1779:         roomSpecials.add(Boolean.TRUE);
1780:         // Place one inspectable feature if there is a safe tile just beyond the stub.
1781:         int fx = cx + dx, fy = cy + dy;
1782:         if(inBounds(fx,fy) && tiles[fx][fy] == '#') {
1783:             tiles[fx][fy] = closet.contentSymbol(r);
1784:         }
```

WorldRuntimeGenerationFramework.java near line 1775:
```java
1771:         int id = rooms.size();
1772:         Rectangle stub = new Rectangle(cx, cy, 1, 1);
1773:         tiles[cx][cy] = zoneType.floorGlyph(r);
1774:         roomIds[cx][cy] = id;
1775:         rooms.add(stub);
1776:         RoomProfile closet = RoomProfile.closetStub(zoneType, r);
1777:         roomProfiles.add(closet);
1778:         roomFactions.add(Faction.NONE);
1779:         roomSpecials.add(Boolean.TRUE);
1780:         // Place one inspectable feature if there is a safe tile just beyond the stub.
1781:         int fx = cx + dx, fy = cy + dy;
1782:         if(inBounds(fx,fy) && tiles[fx][fy] == '#') {
```

WorldRuntimeGenerationFramework.java near line 1680:
```java
1676:                 Rectangle rr = new Rectangle(x,y,rw,rh);
1677:                 Rectangle grown = new Rectangle(x-1,y-1,rw+2,rh+2);
1678:                 if(regionTouchesNonWall(grown)) continue;
1679:                 carve(rr);
1680:                 rooms.add(rr);
1681:                 placed++;
1682:             }
1683:         }
1684:         boolean connected = connectAllRoomsStrict();
1685:         if(!connected || !allRoomsReachableStrict() || rooms.size() < 20){
1686:             DebugLog.warn("LEVELGEN_LATTICE", "Guaranteed lattice needs post-repair connectivity. rooms="+rooms.size()+" connected="+connected+" reachable="+allRoomsReachableStrict()+" zone="+zoneType.label);
1687:         } else {
```

WorldRuntimeGenerationFramework.java near line 1652:
```java
1648:         }
1649:     }
1650: 
1651:     boolean regionTouchesNonWall(Rectangle rr){ for(int x=Math.max(0,rr.x); x<Math.min(w,rr.x+rr.width); x++) for(int y=Math.max(0,rr.y); y<Math.min(h,rr.y+rr.height); y++) if(roomIds[x][y] >= 0 || tiles[x][y]!='#') return true; return false; }
1652:     void carve(Rectangle rr){ int id=rooms.size(); RoomProfile rp = RoomProfile.forZone(zoneType, r); roomProfiles.add(rp); roomFactions.add(Faction.NONE); roomSpecials.add(Boolean.FALSE); char floorChar = zoneType.floorGlyph(r); for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++){ roomIds[x][y]=id; boolean wall = (x==rr.x || y==rr.y || x==rr.x+rr.width-1 || y==rr.y+rr.height-1); tiles[x][y]= wall ? '#' : floorChar; } }
1653: 
1654:     void buildGuaranteedRoomLattice(int target){
1655:         target = WorldGenerationApi.clampRoomTarget(target);
1656:         int cols = 6;
1657:         int rows = 5;
1658:         int cellW = Math.max(10, (w-4) / cols);
1659:         int cellH = Math.max(8, (h-4) / rows);
```

WorldRuntimeGenerationFramework.java near line 1084:
```java
1080:         if(pd != null && inBounds(pd.x,pd.y)) tiles[pd.x][pd.y] = DoorType.forZone(zoneType, r).symbol;
1081:         String[] labels = {"Apartment Living Room", "Apartment Bedroom", "Apartment Washroom", "Apartment Dining Nook"};
1082:         for(int i=0;i<candidateRooms.size();i++){
1083:             Rectangle rr = candidateRooms.get(i);
1084:             int idx=rooms.size(); carve(rr); rooms.add(rr);
1085:             if(idx < roomProfiles.size()) roomProfiles.set(idx, RoomProfile.apartmentRoom(labels[i], zoneType, r));
1086:             placeApartmentFeatures(rr, i);
1087:             Point d = doorBetweenRoomAndCorridor(rr, corridor);
1088:             if(d != null && inBounds(d.x,d.y)) tiles[d.x][d.y] = DoorType.forZone(zoneType, r).symbol;
1089:         }
1090:         return true;
1091:     }
```

Patch actions:
- StampedRoomSpec field names were not a simple x/y/width/height rectangle; did not add unsafe overload.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

Confirmed that direct room-gate wiring still requires one manually confirmed ordinary room placement commit point.

## 0.9.10cq — Phase 3 Ordinary Room Gate Commit-Path Planning

Advanced Phase 3 from ordinary room gate call-site probe into commit-path planning.

Scanned `WorldRuntimeGenerationFramework.java` for likely room commit structures.

Observed candidates:
Candidate near line 110:
```java
105:     int dirtyHazardRevision = 1;
106:     String hearingFieldSummary = "No cached noise/hearing field generated.";
107:     String trapInteractionSummary = "No trap / booby-trap interaction metadata generated.";
108:     String lightNoiseSummary = "No light/noise metadata generated.";
109:     String hazardVisibilitySummary = "No hazard warning overlays generated.";
110:     ArrayList<RoomProfile> roomProfiles = new ArrayList<>();
111:     ArrayList<Faction> roomFactions = new ArrayList<>();
112:     ArrayList<Boolean> roomSpecials = new ArrayList<>();
113:     ZoneType zoneType = ZoneType.TRASH_WARREN;
114:     int sectorX = 1, sectorY = 1, zoneX = 1, zoneY = 1, floor = 1;
115:     boolean sewerLayer = false;
116:     String hiveName = "Unnamed Hive", sectorName = "Unnamed Sector", zoneName = "Unnamed Zone", zoneHistory = "No compact history recorded.", zoneEpochHistory = "No faction-control epoch history recorded.", zoneFacilityHistory = "No facility establishment history recorded.", zoneProductionHistory = "No production output history recorded.", zoneStockMovementHistory = "No production distribution / stock movement history recorded.", zoneConflictLossHistory = "No conflict, loss, theft, or abandonment history recorded.", zoneMaterializedItemHistory = "No concrete historical item materialization ledger recorded.", zoneLaborAssignmentHistory = "No population work-assignment ledger recorded.";
117:     boolean[][] visitedZones = new boolean[3][3];
118:     World(long seed,int w,int h){this.seed=seed;this.w=w;this.h=h;this.r=new Random(seed);tiles=new char[w][h]; roomIds=new int[w][h]; noiseField=new int[w][h]; for(int x=0;x<w;x++) for(int y=0;y<h;y++) roomIds[x][y]=-1;}
119:     void generate(){
```

Candidate near line 212:
```java
207:         DebugLog.audit("INTERSTITIAL_HIVE_MASS", "zone="+zoneType.label+" layer="+layerText()+" converted="+converted+" buriedFeatures="+buried+" seed="+seed);
208:     }
209: 
210:     void resetGenerationState(long effectiveSeed){
211:         this.r = new Random(effectiveSeed);
212:         rooms.clear(); npcs.clear(); replacementQueue.clear(); roomPopulationLedgers.clear(); mapObjects.clear(); lightSources.clear(); noiseSources.clear(); hazardWarnings.clear(); trapRecords.clear(); noiseFieldTurn = -1; dirtyLightRevision++; dirtyNoiseRevision++; dirtyVisionRevision++; dirtyHazardRevision++; hearingFieldSummary = "No cached noise/hearing field generated."; lightNoiseSummary = "No light/noise metadata generated."; hazardVisibilitySummary = "No hazard warning overlays generated."; trapInteractionSummary = "No trap / booby-trap interaction metadata generated."; roomProfiles.clear(); roomFactions.clear(); roomSpecials.clear();
213:         for(int x=0;x<w;x++) for(int y=0;y<h;y++) roomIds[x][y] = -1;
214:     }
215: 
216:     int targetRoomCount(){
217:         // 0.8.66 API sectioning: room-count policy is now delegated to the
218:         // world-generation scale surface. The current profile intentionally preserves
219:         // the 0.8.61 dense-zone numbers as the minimum reserved scaling tier.
220:         return WorldGenerationApi.targetRoomCount(zoneType, r);
221:     }
```

Candidate near line 227:
```java
222: 
223:     int buildCentralPlazaLayout(int target){
224:         target = WorldGenerationApi.clampRoomTarget(target);
225:         Rectangle plaza = centralPlazaRect();
226:         carve(plaza);
227:         rooms.add(plaza);
228:         if(!roomProfiles.isEmpty()) roomProfiles.set(0, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
229:         if(!roomFactions.isEmpty()) roomFactions.set(0, plazaFactionForZone());
230:         if(!roomSpecials.isEmpty()) roomSpecials.set(0, Boolean.TRUE);
231:         decorateCentralPlaza(plaza);
232:         stampCultImperialisTempleNearPlaza(plaza);
233:         seedCompassPlazaCorridors(plaza);
234:         stampStructuredZoneModules(plaza, target);
235: 
236:         int attempts = 0;
```

Candidate near line 228:
```java
223:     int buildCentralPlazaLayout(int target){
224:         target = WorldGenerationApi.clampRoomTarget(target);
225:         Rectangle plaza = centralPlazaRect();
226:         carve(plaza);
227:         rooms.add(plaza);
228:         if(!roomProfiles.isEmpty()) roomProfiles.set(0, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
229:         if(!roomFactions.isEmpty()) roomFactions.set(0, plazaFactionForZone());
230:         if(!roomSpecials.isEmpty()) roomSpecials.set(0, Boolean.TRUE);
231:         decorateCentralPlaza(plaza);
232:         stampCultImperialisTempleNearPlaza(plaza);
233:         seedCompassPlazaCorridors(plaza);
234:         stampStructuredZoneModules(plaza, target);
235: 
236:         int attempts = 0;
237:         int branch = 0;
```

Candidate near line 247:
```java
242:             Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
243:             if(rr.x < 1 || rr.y < 1 || rr.x+rr.width >= w-1 || rr.y+rr.height >= h-1) continue;
244:             if(regionTouchesNonWall(grown)) continue;
245:             int before = rooms.size();
246:             carve(rr);
247:             rooms.add(rr);
248:             ConnectionChoice cc = previewBestConnectionToConnectedSet(before);
249:             if(cc == null){
250:                 removeRoomAtIndex(before);
251:                 continue;
252:             }
253:             commitStrictConnection(cc);
254:         }
255:         DebugLog.audit("LEVELGEN_PLAZA", "central plaza built rooms="+rooms.size()+" target="+target+" attempts="+attempts+" zone="+zoneType.label+" sewer="+sewerLayer);
256:         return attempts;
```

Candidate near line 268:
```java
263:         // thrown exception; it was an unbounded-feeling generation churn. This fallback no
264:         // longer reuses the same organic branch proposer. It builds a deterministic connected
265:         // plaza lattice directly, then carves controlled corridors from the plaza to each room.
266:         target = WorldGenerationApi.clampRoomTarget(target);
267:         Rectangle plaza = centralPlazaRect();
268:         carve(plaza); rooms.add(plaza);
269:         if(!roomProfiles.isEmpty()) roomProfiles.set(0, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
270:         if(!roomFactions.isEmpty()) roomFactions.set(0, plazaFactionForZone());
271:         if(!roomSpecials.isEmpty()) roomSpecials.set(0, Boolean.TRUE);
272:         decorateCentralPlaza(plaza);
273:         stampCultImperialisTempleNearPlaza(plaza);
274:         seedCompassPlazaCorridors(plaza);
275: 
276:         ArrayList<Rectangle> candidateRooms = deterministicFallbackRooms(plaza, target-1);
277:         for(Rectangle rr: candidateRooms){
```

Candidate near line 269:
```java
264:         // longer reuses the same organic branch proposer. It builds a deterministic connected
265:         // plaza lattice directly, then carves controlled corridors from the plaza to each room.
266:         target = WorldGenerationApi.clampRoomTarget(target);
267:         Rectangle plaza = centralPlazaRect();
268:         carve(plaza); rooms.add(plaza);
269:         if(!roomProfiles.isEmpty()) roomProfiles.set(0, RoomProfile.centralPlaza(zoneType, sewerLayer, r));
270:         if(!roomFactions.isEmpty()) roomFactions.set(0, plazaFactionForZone());
271:         if(!roomSpecials.isEmpty()) roomSpecials.set(0, Boolean.TRUE);
272:         decorateCentralPlaza(plaza);
273:         stampCultImperialisTempleNearPlaza(plaza);
274:         seedCompassPlazaCorridors(plaza);
275: 
276:         ArrayList<Rectangle> candidateRooms = deterministicFallbackRooms(plaza, target-1);
277:         for(Rectangle rr: candidateRooms){
278:             if(rooms.size() >= target) break;
```

Candidate near line 281:
```java
276:         ArrayList<Rectangle> candidateRooms = deterministicFallbackRooms(plaza, target-1);
277:         for(Rectangle rr: candidateRooms){
278:             if(rooms.size() >= target) break;
279:             Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
280:             if(!rectInMap(grown) || regionTouchesNonWall(grown)) continue;
281:             carve(rr); rooms.add(rr);
282:             connectRoomToPlazaFallback(plaza, rr);
283:         }
284: 
285:         if(rooms.size() < target){
286:             // Last-resort compact ring: preserve playability over strict beauty. These rooms are
287:             // intentionally small and placed in predictable spokes where the map has space.
288:             for(int side=0; side<4 && rooms.size()<target; side++){
289:                 for(int n=0; n<5 && rooms.size()<target; n++){
290:                     Rectangle rr = fallbackSpokeRoom(plaza, side, n);
```

Candidate near line 294:
```java
289:                 for(int n=0; n<5 && rooms.size()<target; n++){
290:                     Rectangle rr = fallbackSpokeRoom(plaza, side, n);
291:                     if(rr == null) continue;
292:                     Rectangle grown = new Rectangle(rr.x-1, rr.y-1, rr.width+2, rr.height+2);
293:                     if(!rectInMap(grown) || regionTouchesNonWall(grown)) continue;
294:                     carve(rr); rooms.add(rr);
295:                     connectRoomToPlazaFallback(plaza, rr);
296:                 }
297:             }
298:         }
299: 
300:         if(rooms.size() < target){
301:             buildGuaranteedRoomLattice(target);
302:         }
303:         boolean reachable = allRoomsReachableStrict();
```

Candidate near line 329:
```java
324:             if(out.size() >= needed) break;
325:             int rw = 6 + Math.floorMod(a[0]+a[1]+(int)seed, 4);
326:             int rh = 5 + Math.floorMod(a[0]*3+a[1]+(int)(seed>>4), 3);
327:             int x = Math.max(2, Math.min(w-rw-2, a[0]));
328:             int y = Math.max(2, Math.min(h-rh-2, a[1]));
329:             out.add(new Rectangle(x,y,rw,rh));
330:         }
331:         return out;
332:     }
333: 
334:     Rectangle fallbackSpokeRoom(Rectangle plaza, int side, int n){
335:         int rw=5, rh=4;
336:         if(side==0) return new Rectangle(Math.max(2, Math.min(w-rw-2, plaza.x + 1 + n*3)), Math.max(2, plaza.y - 8 - n), rw, rh);
337:         if(side==2) return new Rectangle(Math.max(2, Math.min(w-rw-2, plaza.x + 1 + n*3)), Math.min(h-rh-2, plaza.y+plaza.height+4+n), rw, rh);
338:         if(side==3) return new Rectangle(Math.max(2, plaza.x - 9 - n), Math.max(2, Math.min(h-rh-2, plaza.y + 1 + n*3)), rw, rh);
```

Patch actions:
- Added shouldAcceptOrdinaryRoomPlacementPhase3(...) decision helper.

Added a decision helper intended to be called immediately before ordinary room candidate commitment:
`shouldAcceptOrdinaryRoomPlacementPhase3(...)`.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

Broad call-site wiring remains deferred until the exact ordinary room commit point is confirmed.

## 0.9.10cp — Phase 3 Ordinary Room Gate Call-Site Probe

Advanced Phase 3 from room gate preparation into ordinary room placement call-site probing.

Inspected `WorldRuntimeGenerationFramework.java` for room-placement-like candidates.

Candidate excerpts:
Candidate near line 1652:
```java
1649:     }
1650: 
1651:     boolean regionTouchesNonWall(Rectangle rr){ for(int x=Math.max(0,rr.x); x<Math.min(w,rr.x+rr.width); x++) for(int y=Math.max(0,rr.y); y<Math.min(h,rr.y+rr.height); y++) if(roomIds[x][y] >= 0 || tiles[x][y]!='#') return true; return false; }
1652:     void carve(Rectangle rr){ int id=rooms.size(); RoomProfile rp = RoomProfile.forZone(zoneType, r); roomProfiles.add(rp); roomFactions.add(Faction.NONE); roomSpecials.add(Boolean.FALSE); char floorChar = zoneType.floorGlyph(r); for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++){ roomIds[x][y]=id; boolean wall = (x==rr.x || y==rr.y || x==rr.x+rr.width-1 || y==rr.y+rr.height-1); tiles[x][y]= wall ? '#' : floorChar; } }
1653: 
1654:     void buildGuaranteedRoomLattice(int target){
1655:         target = WorldGenerationApi.clampRoomTarget(target);
1656:         int cols = 6;
1657:         int rows = 5;
```

Candidate near line 2759:
```java
2756:         Rectangle spawnRoom = rooms.get(0);
2757:         Point c=center(spawnRoom);
2758:         if(inBounds(c.x,c.y) && walkable(c.x,c.y) && roomIds[c.x][c.y] == 0) return c;
2759:         for(int x=spawnRoom.x;x<spawnRoom.x+spawnRoom.width;x++) for(int y=spawnRoom.y;y<spawnRoom.y+spawnRoom.height;y++) if(inBounds(x,y)&&walkable(x,y)&&roomIds[x][y]==0) return new Point(x,y);
2760:         return ensurePlayerSpawnPoint();
2761:     }
2762:     Point ensurePlayerSpawnPoint(){
2763:         for(int i=0;i<rooms.size();i++){
2764:             Rectangle rr = rooms.get(i);
```

Candidate near line 3430:
```java
3427:     static int targetRoomCount(ZoneType zoneType, Random r){
3428:         WorldGenerationScaleProfile p = currentScale();
3429:         int base = p.minRooms + r.nextInt(Math.max(1, p.maxRooms - p.minRooms + 1));
3430:         if(zoneType==ZoneType.SECTOR_GOVERNORS_MANSION || zoneType==ZoneType.NEUTRAL_RAIL_DEPOT || zoneType==ZoneType.TRAIN_SERVICE_YARD) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 4) + r.nextInt(Math.min(5, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 4) + 1))));
3431:         if(zoneType==ZoneType.HAB_STACK || zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR || zoneType==ZoneType.SUMP_MARKET) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 6) + r.nextInt(Math.min(7, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 6) + 1))));
3432:         if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType==ZoneType.MECHANICUS_RELIC_DUCT || zoneType==ZoneType.IMPERIAL_GUARD_BILLET) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 7) + r.nextInt(Math.min(7, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 7) + 1))));
3433:         base = (int)Math.round(base * settings().zoneDensityMultiplier());
3434:         return clampRoomTarget(base);
3435:     }
```

Candidate near line 3432:
```java
3429:         int base = p.minRooms + r.nextInt(Math.max(1, p.maxRooms - p.minRooms + 1));
3430:         if(zoneType==ZoneType.SECTOR_GOVERNORS_MANSION || zoneType==ZoneType.NEUTRAL_RAIL_DEPOT || zoneType==ZoneType.TRAIN_SERVICE_YARD) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 4) + r.nextInt(Math.min(5, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 4) + 1))));
3431:         if(zoneType==ZoneType.HAB_STACK || zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR || zoneType==ZoneType.SUMP_MARKET) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 6) + r.nextInt(Math.min(7, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 6) + 1))));
3432:         if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType==ZoneType.MECHANICUS_RELIC_DUCT || zoneType==ZoneType.IMPERIAL_GUARD_BILLET) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 7) + r.nextInt(Math.min(7, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 7) + 1))));
3433:         base = (int)Math.round(base * settings().zoneDensityMultiplier());
3434:         return clampRoomTarget(base);
3435:     }
3436: 
3437:     static Rectangle centralPlazaRect(int width, int height){
```

Candidate near line 3431:
```java
3428:         WorldGenerationScaleProfile p = currentScale();
3429:         int base = p.minRooms + r.nextInt(Math.max(1, p.maxRooms - p.minRooms + 1));
3430:         if(zoneType==ZoneType.SECTOR_GOVERNORS_MANSION || zoneType==ZoneType.NEUTRAL_RAIL_DEPOT || zoneType==ZoneType.TRAIN_SERVICE_YARD) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 4) + r.nextInt(Math.min(5, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 4) + 1))));
3431:         if(zoneType==ZoneType.HAB_STACK || zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR || zoneType==ZoneType.SUMP_MARKET) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 6) + r.nextInt(Math.min(7, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 6) + 1))));
3432:         if(zoneType==ZoneType.MECHANICUS_FORGE_CLOISTER || zoneType==ZoneType.MECHANICUS_RELIC_DUCT || zoneType==ZoneType.IMPERIAL_GUARD_BILLET) base = Math.max(base, Math.max(p.minRooms, p.maxRooms - 7) + r.nextInt(Math.min(7, Math.max(1, p.maxRooms - Math.max(p.minRooms, p.maxRooms - 7) + 1))));
3433:         base = (int)Math.round(base * settings().zoneDensityMultiplier());
3434:         return clampRoomTarget(base);
3435:     }
3436: 
```

Candidate near line 2542:
```java
2539:         }
2540:         Point c=center(rr);
2541:         if(inBounds(c.x,c.y) && walkable(c.x,c.y) && npcAt(c.x,c.y)==null) return c;
2542:         for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++) if(inBounds(x,y) && walkable(x,y) && roomIds[x][y]>=0 && npcAt(x,y)==null) return new Point(x,y);
2543:         return null;
2544:     }
2545: 
2546: 
2547:     Point randomOpenPoint(Random rrng){
```

Candidate near line 1637:
```java
1634:     void removeRoomAtIndex(int idx){
1635:         if(idx <= 0 || idx >= rooms.size()) return;
1636:         Rectangle rr = rooms.get(idx);
1637:         for(int x=rr.x; x<rr.x+rr.width; x++) for(int y=rr.y; y<rr.y+rr.height; y++) if(inBounds(x,y) && roomIds[x][y]==idx){ roomIds[x][y] = -1; tiles[x][y] = '#'; }
1638:         rooms.remove(idx); roomProfiles.remove(idx); roomFactions.remove(idx); roomSpecials.remove(idx);
1639:         for(int x=0;x<w;x++) for(int y=0;y<h;y++) if(roomIds[x][y] > idx) roomIds[x][y]--;
1640:     }
1641: 
1642:     void cullUnreachableRoomsHard(){
```

Candidate near line 2305:
```java
2302:             if(!ok){
2303:                 isolated++;
2304:                 DebugLog.audit("ROOM_CONNECTIVITY", "isolated room culled id="+i+" rect="+rr.x+","+rr.y+","+rr.width+","+rr.height+" zone="+zoneType.label);
2305:                 for(int x=rr.x;x<rr.x+rr.width;x++) for(int y=rr.y;y<rr.y+rr.height;y++) if(inBounds(x,y) && roomIds[x][y]==i){ roomIds[x][y] = -1; tiles[x][y] = '#'; }
2306:                 roomSpecials.set(i, Boolean.TRUE);
2307:                 roomFactions.set(i, Faction.NONE);
2308:                 roomProfiles.set(i, RoomProfile.closetStub(zoneType, r).withFeatures("Culled unreachable room space; generation authority rejected this room as unattached."));
2309:             }
2310:         }
```

Current implementation action:
- Added recordPhase3RoomPlacementRejectedByEdgeBand(...) failure-accounting scaffold.

The gate was not blindly wired into an uncertain call site. Instead, a room edge-band rejection accounting scaffold was added so the next pass can safely connect confirmed placement loops and count edge-band failures.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

## 0.9.10co — Phase 3 Standards Check / Ordinary Room Gate Preparation

Continued Phase 3 development after checking `docs/STANDARDS_AND_PRACTICES.md`.

Relevant standards headings reviewed:
- ## Continuation reporting standard
- ## Phase 3 Spatial Integration Standards
- ## Phase 3 Spatial Anatomy Standards
- ## Edge Safety Generation Standards
- ## Room Clustering and Frontage Grouping Standards
- ## Phase 3 Spatial Integration Stabilization Standards
- ## Phase 3 Implementation Prioritization Standards
- ## Archive Cleanup / Recoverability Standards
- ## Zone-Family Tagging Implementation Standards
- ## Edge Safety Band Reservation Implementation Standards
- ## Central Anchor Placement Implementation Standards
- ## First Phase 3 Code Patch Standards
- ## Room Placement Fallback / Dynamic Zone Expansion Standards
- ## Context Attachment Correction / Edge-Bounds Scaffold Standards
- ## Edge-Bounds Ordinary Placement Gate Standards

Patch actions:
- Added room-specific ordinary placement gate helpers to WorldRuntimeGenerationFramework.
- Added ZoneGenerationContext.rect(...) convenience factory.

Potential room/placement-related methods observed in `WorldRuntimeGenerationFramework`:
- line 3112: `static boolean canPlaceInterwallRoom(World world, Rectangle rect, boolean maintenance){`
- line 894: `void stampStructuredZoneModules(Rectangle plaza, int targetRooms){`
- line 634: `boolean stampTempleRoomAt(Rectangle plaza, int side, int offset){`
- line 3160: `static void connectDangerRoomToLoop(World world, Rectangle rect, int inset){`
- line 3142: `static void connectRoomToLoop(World world, Rectangle rect, int inset){`
- line 2750: `Rectangle roomRect(int id){`
- line 2414: `Faction factionForGeneratedRoom(int i){`
- line 2204: `ArrayList<Point> doorsOnRoomWall(Rectangle rr, int side){`
- line 2202: `int countDoorsOnRoomWall(Rectangle rr, int side){`
- line 2186: `int countLargeRoomDoorSpacingViolations(Rectangle rr, int roomIndex, String phase, int minSpacing){`
- line 2174: `int countSmallRoomDoorWallViolations(Rectangle rr, int roomIndex, String phase){`
- line 1373: `void placeApartmentFeatures(Rectangle rr, int kind){`
- line 1344: `boolean isRoomWallTile(Rectangle rr, int x, int y){`
- line 1327: `boolean roomDoorHasWallClearance(Rectangle rr, Point candidate, int minSpacing){`
- line 1315: `Point shiftedAlongRoomWall(Rectangle rr, Point p, int side, int delta){`

Added ordinary room placement gate preparation while keeping call-site wiring conservative until the exact placement loop semantics are confirmed.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

Next target: attach `isOrdinaryRoomPlacementInsidePhase3InteriorBounds(...)` to the safest exact ordinary room placement loop and count edge-band failures separately for fallback behavior.

## 0.9.10cn — Phase 3 Edge-Bounds Ordinary Placement Gate Added

Advanced Phase 3 into the first edge-bounds ordinary placement gate.

Patch actions:
- Added allowsOrdinaryPlacement(Rect) to ZoneGenerationContext.
- Added Rect.contains(Rect) and Rect.intersects(Rect).
- Added ordinary placement edge-band gate helpers to WorldRuntimeGenerationFramework.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

This patch adds reusable gate helpers only and does not yet broadly rewrite placement call sites.

## 0.9.10cm — Phase 3 Context Attachment Correction / Edge-Bounds Scaffold

Advanced Phase 3 from ZoneGenerationContext entry-point attachment into context attachment correction and edge-bounds scaffolding.

Patch actions:
- Removed accidental recursive ZoneGenerationContext creation inside helper method.
- Added local phase3Debug helper to replace direct context System.out call site.
- Added getPhase3InteriorGenerationBounds scaffold helper.
- Added hasUsableInteriorBounds validation helper to ZoneGenerationContext.

Confirmed the patch remains metadata/scaffolding only and does not yet replace room, road, corridor, plaza, frontage, parking, or whole-district generation.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

Next implementation target: use exposed `interiorGenerationBounds` to begin gating ordinary placement candidates against the protected 5-tile edge safety band.

## 0.9.10cl — Phase 3 ZoneGenerationContext Entry-Point Attachment

Advanced Phase 3 from first scaffolding code patch into entry-point attachment.

Inspection summary:
- line 3553: `public static ZoneGenerationContext createZoneGenerationContext(ZoneType zoneType, int mapWidth, int mapHeight) {`
- line 3427: `static int targetRoomCount(ZoneType zoneType, Random r){`
- line 2887: `static ArrayList<String> seedCacheItems(RoomProfile rp, ZoneType zt, Random rng){`
- line 3367: `static char featureFor(ZoneType z, Random r, int n){`
- line 3315: `static char structuralBlockerFor(ZoneType z, int x, int y, Random r){`
- line 2416: `Faction factionForGeneratedRoom(int i){`
- line 896: `void stampStructuredZoneModules(Rectangle plaza, int targetRooms){`
- line 3422: `static int clampRoomTarget(int value){`
- line 3415: `static Dimension zoneSliceSize(long sliceSeed){`
- line 3229: `static int populateDangerRooms(World world, Random r){`

Patch actions:
- Attached metadata-only ZoneGenerationContext creation to `createZoneGenerationContext` near line 3553.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

Confirmed continued deferral of road generation, sewer corridor replacement, central anchor placement, room fallback expansion, plaza placement, frontage assignment, parking/loading placement, and whole-district validation implementation.

## 0.9.10ck — Phase 3 First Scaffolding Code Patch Added

Advanced Phase 3 from generator source gate handling into the first scaffolding code patch.

Patch actions:
- Added createZoneGenerationContext helper to WorldRuntimeGenerationFramework.java

Added `src/mechanist/ZoneGenerationContext.java`.

The new context provides:
- ZoneFamily,
- RoadMode,
- AnchorType,
- EdgeBandPolicy,
- ValidationProfile,
- minimum edge safety band constant of 5 tiles,
- interior generation bounds calculation,
- zone-family derivation from ZoneType,
- road-mode derivation,
- anchor-type derivation,
- edge-band policy derivation,
- validation-profile derivation,
- and debug-string output.

Added a non-invasive helper to `WorldRuntimeGenerationFramework` where possible:
`createZoneGenerationContext(ZoneType zoneType, int mapWidth, int mapHeight)`.

This helper creates and logs a ZoneGenerationContext without replacing existing room, corridor, plaza, road, parking, frontage, or validation behavior.

Check result:
javac syntax/import check passed.

Compiler details if relevant:
No compiler errors reported.

Confirmed this remains a scaffolding-only patch.

## 0.9.10cj — Phase 3 Generator Source Gate Logged

Advanced Phase 3 from source inspection / exact first-patch planning into generator source gate handling.

Performed a deliberate archive inspection for likely core world/zone/room generation source files.

Likely candidates found:
- `src/mechanist/AbstractDistantZoneSimulation.java`
- `src/mechanist/MapLayerSurfaceAuthority.java`
- `src/mechanist/NobleEstateStampGenerator.java`
- `src/mechanist/RoomFixtureInteractionAuthority.java`
- `src/mechanist/RoomManifestApi.java`
- `src/mechanist/RoomProfile.java`
- `src/mechanist/StampedRoomSpec.java`
- `src/mechanist/WorldRuntimeGenerationFramework.java`
- `src/mechanist/WorldSimulationFramework.java`
- `src/mechanist/ZoneType.java`
- `tools/generate_art_quality_variants.py`

Conclusion: the current archive does not clearly expose the runtime world-generation source entry point required to safely implement the first Phase 3 code patch.

Logged a source gate: code implementation should not proceed against documentation, art indexes, or asset import tooling when the actual generator source is absent.

Recorded required source categories for implementation:
- zone generation entry point,
- room placement loop,
- corridor/chamber placement logic,
- map bounds/dimension logic,
- district/zone type selection,
- central plaza/anchor placement,
- sewer/utility generation if separate,
- validation/cleanup routines,
- and debug logging helpers.

Reaffirmed that once source is present, the first patch remains a small scaffolding patch adding ZoneGenerationContext, zone_family, road_mode, anchor_type, edge_band_policy, validation_profile, interior_generation_bounds, and debug logging while preserving existing placement behavior.

## 0.9.10ci — Phase 3 Source Inspection / Exact First Patch Plan Initiated

Advanced Phase 3 from room placement fallback / dynamic zone expansion planning into source inspection and exact first-patch planning.

Performed an archive keyword scan for likely generation/source candidates.

Top non-document candidates identified:
- `assets/art/rebase_0_9_06d/graphical_upgrade_base_3_manifest.json`
- `tools/generate_art_quality_variants.py`
- `tools/import_graphical_upgrade_base_3.py`
- `tools/import_asset_pack_6.py`
- `assets/art/rebase_0_9_06d/source/new game Intro crawl text/Text crawl.txt`
- `assets/indexes/asset_pack_6_slicing_index.json`

Defined the first patch attachment objective as adding a zone generation context layer at the earliest practical zone/district/map generation point before rooms, roads, corridors, plazas, alleys, parking, or semantic overlays are placed.

Reaffirmed that the first implementation target remains scaffolding only:
- ZoneGenerationContext,
- zone_family,
- road_mode,
- anchor_type,
- edge_band_policy,
- validation_profile,
- interior_generation_bounds,
- and debug logging.

Added first-patch acceptance criteria:
- generation still runs/imports,
- existing output behavior is preserved as much as possible,
- every generated zone can obtain a context object,
- context fields are visible in debug mode,
- edge-band minimum is represented,
- roaded/roadless mode is explicit,
- and no full road/corridor replacement has been attempted yet.

Added Source Inspection / Exact First Patch Standards requiring a non-invasive scaffolding patch and avoiding broad generator rewrites.

## 0.9.10ch — Phase 3 Room Placement Fallback / Dynamic Zone Expansion Planning Initiated

Advanced Phase 3 from first code patch planning into room placement fallback safety and dynamic zone expansion planning.

Confirmed the edge safety band minimum as 5 tiles unless a zone-specific policy requires a larger protected band.

Defined fallback behavior for repeated room placement failures caused by edge-band overlap or insufficient safe interior generation area.

Established that the generator must not violate the edge safety band to force room placement. Instead, if placement repeatedly fails due to insufficient interior_generation_bounds, the generator should attempt controlled dynamic zone expansion outward from the zone center.

Added room placement failure doctrine requiring tracking of attempt count, edge-band conflict, blocked edge cells, central anchor conflict, road/corridor conflict, and traversal conflict.

Added dynamic zone expansion doctrine requiring expansion to preserve the center/anchor, recalculate edge safety band and interior bounds, preserve roaded/roadless family selection, re-run validation, and log the expansion event.

Added suggested tunable policy values:
- room_place_attempt_limit default 24 attempts per required room,
- edge_failure_threshold default 50 percent edge/interior-bound failures,
- min_edge_safety_band_tiles = 5,
- expansion_step_tiles suggested 4 to 8,
- max_expansion_steps suggested 2 to 3.

Added fallback hierarchy covering retry, alternate orientation, smaller variant, connector/cluster adjustment, center-based zone expansion, edge/interior recalculation, retry, optional-room downgrade, and reject/regenerate fallback.

Added Room Placement Fallback / Dynamic Zone Expansion Standards reinforcing minimum 5-tile edge safety, logging, no silent edge violations, no infinite placement loops, and preservation of future hive exterior wall deployment.

## 0.9.10cg — Phase 3 First Code Patch Planning Initiated

Advanced Phase 3 from roaded vs roadless circulation branching planning into first code patch planning.

Defined the first code patch objective as a small structural scaffolding patch rather than a broad generator rewrite.

The first patch should implement or attach a ZoneGenerationContext-style layer capable of:
- assigning zone_family,
- deriving road_mode,
- deriving anchor_type,
- deriving edge_band_policy,
- deriving validation_profile,
- reserving interior_generation_bounds,
- and logging the resulting generation context.

Defined required first-patch components including zone family constants, road mode constants, anchor type constants, edge-band policy constants, validation profile constants, zone-family derivation helper, edge-band bounds helper, debug logging helper, and a minimal required-field validation check.

Explicitly deferred full road placement, sewer corridor replacement, plaza placement, frontage assignment, door placement, room clustering, parking/loading placement, whole-district validation, NPC behavior, vehicle runtime, and multi-floor systems from the first patch.

Added First Phase 3 Code Patch Standards requiring the first patch to remain small, reversible, debug-visible, non-destructive, and compatible with current generation.

## 0.9.10cf — Phase 3 Roaded vs Roadless Circulation Branching Implementation Plan Initiated

Advanced Phase 3 implementation planning from central anchor placement into roaded vs roadless circulation branching.

Defined the circulation-branching decision layer as the point where the generator chooses roaded civic circulation, roadless corridor-and-chamber circulation, hybrid limited-road circulation, service-only circulation, or exterior-edge circulation.

Established initial circulation modes:
- roads_enabled,
- roads_disabled,
- hybrid_limited_roads,
- service_only,
- exterior_edge_only.

Defined roaded circulation responsibilities for major roads, secondary roads, service roads, plaza approaches, frontage exposure, parking/loading approaches, emergency routes, and road-facing room clusters.

Defined roadless circulation responsibilities for primary corridor trunks, secondary branching corridors, chambers, junction rooms, maintenance nodes, pipe galleries, pump/filter rooms, utility alcoves, sewer crosslinks, tunnel loops, and service warrens.

Defined hybrid circulation responsibilities for underhive, ruined civic, damaged industrial, collapsed public, and faction-contested spaces using broken roads, corridors, alleys, chambers, service lanes, scrap courts, and improvised frontages.

Added branching decision doctrine requiring zone_family, road_mode, anchor_type, edge_band_policy, and validation_profile to exist before circulation generation.

Added Roaded vs Roadless Circulation Branching Implementation Standards requiring explicit circulation mode selection and rejecting hidden fallback modes or roads in pure sewer/utility zones.

## 0.9.10ce — Phase 3 Central Anchor Placement Implementation Plan Initiated

Advanced Phase 3 implementation planning from edge safety band reservation into central anchor placement.

Defined the central anchor as the mandatory navigational core and initial growth seed for every generated zone.

Established required central-anchor context fields:
- anchor_type,
- anchor_rect,
- anchor_center,
- anchor_min_size,
- anchor_max_size,
- anchor_reserved_cells,
- anchor_exit_points,
- anchor_feature_tags,
- anchor_validation_profile,
- anchor_allows_roads,
- anchor_allows_corridors,
- and anchor_degradation_profile.

Mapped zone families to anchor types including civic plazas, noble courts, military parade grounds, market squares, sewer junction chambers, pump halls, utility nexuses, maintenance hubs, industrial service yards, underhive gathering pits, scrap courts, exterior wall access nodes, and story anchors.

Added placement doctrine requiring anchors to remain inside interior_generation_bounds unless edge-authorized, provide enough footprint for circulation seeding, expose exit/seed points, preserve navigation readability, and match zone family.

Added seed doctrine allowing anchors to generate major roads, secondary roads, corridor trunks, service corridors, sewer trunks, utility corridors, alley/service routes, and room cluster attachments according to zone family.

Added Central Anchor Placement Implementation Standards requiring anchors to run after zone-family tagging and edge-band reservation but before circulation generation.

## 0.9.10cd — Phase 3 Edge Safety Band Reservation Implementation Plan Initiated

Advanced Phase 3 implementation planning from zone-family tagging into edge safety band reservation.

Defined edge safety band reservation as the second required generation step, after zone-family tagging and before central-anchor placement or any road/corridor/room/plaza/parking generation.

Established required edge-band context fields:
- edge_band_policy,
- edge_band_width,
- hard_boundary_width,
- maintenance_ring_width,
- edge_room_allowed,
- edge_transition_allowed,
- edge_authorized_feature_tags,
- blocked_edge_cells,
- interior_generation_bounds,
- and edge_validation_profile.

Added initial edge-band policy values:
- standard_perimeter_reserved,
- exterior_wall_heavy,
- sewer_utility_edge,
- noble_secured_edge,
- military_hardened_edge,
- underhive_breached_edge,
- edge_story_override,
- no_edge_override_allowed.

Defined eventual edge-band subregions including raw boundary wall cells, exterior structural shell cells, maintenance corridor cells, edge-authorized room cells, transition/egress cells, and blocked/ruined edge cells.

Added edge-authorized feature tags such as edge_wall, edge_maintenance_corridor, edge_service_room, edge_wall_machinery, edge_buttress, edge_security_checkpoint, edge_transition_anchor, edge_ruin_breach, edge_exterior_observation, and edge_story_feature.

Added Edge Safety Band Reservation Implementation Standards requiring ordinary spatial features to use interior_generation_bounds unless explicitly edge-authorized.

## 0.9.10cc — Phase 3 Zone-Family Tagging Implementation Plan Initiated

Advanced Phase 3 from archive cleanup into code implementation planning for the first priority stack.

Defined zone-family tagging as the first code-facing implementation target because roaded/roadless generation, edge-band rules, central-anchor placement, feature authorization, and validation profiles all depend on the zone family.

Established initial zone-family tags:
- roaded_civic,
- roaded_noble,
- roaded_pdf_military,
- roaded_market_admin,
- roadless_sewer,
- roadless_utility,
- roadless_maintenance,
- roadless_industrial_interior,
- roadless_tunnel_warren,
- hybrid_underhive,
- hybrid_ruined_civic,
- hybrid_industrial_surface,
- edge_exterior_wall,
- special_story_zone.

Defined tag responsibilities including whether roads are allowed, whether corridor/chamber generation is preferred, what central anchor type is used, whether parking/loading/frontage/plaza logic is legal, whether sewer pipe-network crosslinks apply, whether edge-band exceptions are allowed, and which validation profile runs.

Added suggested zone generation context fields:
- zone_family,
- district_type,
- faction_control,
- road_mode,
- anchor_type,
- edge_band_policy,
- allows_parking,
- allows_loading_bays,
- allows_frontage,
- allows_plazas,
- uses_corridor_chambers,
- uses_sewer_crosslinks,
- validation_profile.

Added Zone-Family Tagging Implementation Standards requiring zone-family assignment before edge bands, central anchors, roads, corridors, frontage, plazas, alleys, parking/loading, or validation run.

Established debugging doctrine requiring each generated zone to log selected zone family, road mode, anchor type, edge-band policy, validation profile, and disabled feature families.

## 0.9.10cb — Phase 3 Development History / Archive Cleanup Initiated

Advanced Phase 3 from implementation prioritization review into development-history and archive cleanup.

Reviewed completed Phase 3 work from spatial integration baseline through implementation prioritization review.

Defined the current bucket objective as stabilizing the archive for recoverability before code-facing implementation planning begins.

Reinforced that the current first implementation priority stack is:
1. zone family tagging,
2. edge safety band reservation,
3. central anchor placement,
4. roaded vs roadless circulation branching.

Added archive cleanup doctrine requiring that a future reader can recover:
- current version,
- active phase,
- active bucket,
- completed work,
- next work,
- deferred systems,
- and first implementation priorities.

Added archive safety doctrine preventing new standalone audit/history files, duplicate authority documents, hidden source-file planning, and contradictions against master plan / standards / history hierarchy.

Added Archive Cleanup / Recoverability Standards reinforcing current-state clarity, phase clarity, completed-work history, deferred-system boundaries, implementation priority order, and continuation reporting requirements.

## 0.9.10ca — Phase 3 Implementation Prioritization Review Initiated

Advanced Phase 3 from spatial integration stabilization into implementation prioritization review.

Reviewed completed Phase 3 work from spatial baseline through integrated district-layout validation and stabilization.

Defined the current bucket objective as converting the stabilized Phase 3 spatial pipeline into a practical implementation order.

Established recommended implementation order:
1. zone family tagging,
2. edge safety band reservation,
3. central anchor placement,
4. roaded vs roadless circulation selection,
5. basic roaded layout support,
6. basic roadless corridor/chamber support,
7. frontage orientation metadata,
8. door/threshold placement validation,
9. room cluster grouping,
10. plaza/road/frontage validation,
11. alley/service-route validation,
12. parking/loading validation,
13. whole-district validation,
14. repair/rejection logging.

Established the first implementation priority as zone family selection, edge band reservation, central anchor placement, and roaded vs roadless generation branching.

Added debugging doctrine requiring readable output for zone family selection, edge band reservation, central anchor placement, circulation family selection, validation failures, repair attempts, repair success/failure, and region regeneration.

Added implementation safety doctrine prioritizing structural correctness, traversal correctness, access correctness, validation correctness, semantic richness, and visual detail in that order.

Added Phase 3 Implementation Prioritization Standards reinforcing safe testable layers and avoidance of decorative feature work before structural reliability.

## 0.9.10bz — Phase 3 Spatial Integration Stabilization Initiated

Advanced Phase 3 from integrated district-layout validation into spatial integration stabilization.

Reviewed completed Phase 3 work:
- spatial integration baseline expansion,
- frontage and door orientation systems,
- road hierarchy integration,
- plaza-centered district composition,
- alley and service-routing continuity,
- parking and loading-bay integration,
- roaded vs roadless zone doctrine,
- sewer pipe-network corridor rules,
- edge safety generation band doctrine,
- spatial contradiction cleanup,
- room clustering/frontage grouping,
- plaza/road/frontage validation,
- alley/parking/service validation,
- and integrated district-layout validation.

Defined the current bucket objective as stabilizing Phase 3 spatial systems into a predictable implementation-ready generation pipeline.

Established the stabilized Phase 3 generation sequence:
1. select zone family,
2. reserve edge safety band,
3. place central anchor,
4. generate roaded or roadless circulation family,
5. assign district spatial identity,
6. place major room clusters,
7. assign frontage orientation,
8. place doors and thresholds,
9. attach plazas, alleys, parking, loading, and service spaces,
10. apply Phase 2 semantic overlays,
11. validate local contradictions,
12. validate whole-district coherence,
13. repair safe contradictions,
14. reject/regenerate failed regions when repair worsens layout.

Added stabilization doctrine favoring predictable, readable, navigable spatial anatomy over novelty that breaks traversal or frontage logic.

Added future implementation doctrine prioritizing deterministic placement order, debuggable validation failures, repair logs, zone-family tagging, feature authorization tags, edge-band tests, central-anchor tests, and contradiction reporting.

Added Phase 3 Spatial Integration Stabilization Standards reinforcing ordered generation, local and whole-district validation, repair/rejection behavior, edge safety, central-anchor reliability, and future simulation attachment points.

## 0.9.10by — Phase 3 Integrated District-Layout Validation Initiated

Advanced Phase 3 from alley, parking, and service validation into integrated district-layout validation.

Reviewed completed Phase 3 work:
- spatial integration baseline expansion,
- frontage and door orientation systems,
- road hierarchy integration,
- plaza-centered district composition,
- alley and service-routing continuity,
- parking and loading-bay integration,
- roaded vs roadless zone doctrine,
- sewer pipe-network corridor rules,
- edge safety generation band doctrine,
- spatial contradiction cleanup,
- room clustering/frontage grouping,
- plaza/road/frontage validation,
- and alley/parking/service validation.

Defined the current bucket objective as validating the entire generated district layout as a coherent spatial organism rather than a collection of locally valid subsystems.

Added whole-district validation doctrine covering central anchors, spatial family matching, road/corridor appropriateness, frontage plausibility, plaza connectivity, alley service purpose, parking/loading approach logic, edge-band protection, navigation readability, AI pathing, hazard visibility, and district identity.

Added repair and downgrade doctrine supporting connectors, room-group rotation, road-to-corridor downgrade, isolated plaza-to-chamber conversion, invalid parking-to-service/storage conversion, unsupported loading-to-blocked/ruined conversion, service-alley insertion, blocked-exit opening, and clutter reduction.

Added district identity validation doctrine for civilian, noble, PDF/military, sewer, underhive, and industrial district readability.

Added Integrated District-Layout Validation Standards reinforcing whole-layout validation, repair-first behavior, rejection/regeneration fallback, player orientation, AI navigation, hazard visibility, and future simulation support.

## 0.9.10bx — Phase 3 Alley / Parking / Service Validation Initiated

Advanced Phase 3 from plaza, road, and frontage validation into alley, parking, and service validation.

Reviewed completed Phase 3 work:
- spatial integration baseline expansion,
- frontage and door orientation systems,
- road hierarchy integration,
- plaza-centered district composition,
- alley and service-routing continuity,
- parking and loading-bay integration,
- roaded vs roadless zone doctrine,
- sewer pipe-network corridor rules,
- edge safety generation band doctrine,
- spatial contradiction cleanup,
- room clustering/frontage grouping,
- and plaza/road/frontage validation.

Defined the current bucket objective as validation of rear-facing, service-facing, and vehicle-facing spatial anatomy.

Established validation categories including:
- alley continuity validation,
- rear-door validation,
- service-door validation,
- loading-bay validation,
- parking access validation,
- vehicle recall validation,
- service-road destination validation,
- waste/utility route validation,
- parking/road hierarchy validation,
- and service clutter obstruction validation.

Added alley validation doctrine requiring alleys to connect to meaningful service destinations such as rear doors, loading doors, maintenance rooms, utility hatches, waste rooms, market/service backs, parking/service spaces, and hidden/private entrances.

Added parking validation doctrine requiring usable road/service approaches, open space for intended vehicle scale, plausible entry/exit logic, obstruction-free recall cells, and district-appropriate purpose.

Added loading-bay validation doctrine requiring service-road approach, cargo staging, service-facing room relationship, warehouse/depot/kitchen/industrial connection, and turning/clearance space where appropriate.

Added service separation doctrine for public vs service circulation in markets, noble districts, industrial zones, and military zones.

Added Alley / Parking / Service Validation Standards reinforcing rear-access readability, vehicle approach clarity, service circulation, emergency access, AI navigation, hazard visibility, and future logistics/vehicle extensibility.

## 0.9.10bw — Phase 3 Plaza / Road / Frontage Validation Initiated

Advanced Phase 3 from room clustering and frontage grouping into plaza, road, and frontage validation.

Reviewed completed Phase 3 work:
- spatial integration baseline expansion,
- frontage and door orientation systems,
- road hierarchy integration,
- plaza-centered district composition,
- alley and service-routing continuity,
- parking and loading-bay integration,
- roaded vs roadless zone doctrine,
- sewer pipe-network corridor rules,
- edge safety generation band doctrine,
- spatial contradiction cleanup,
- and room clustering/frontage grouping.

Defined the current bucket objective as validating that plazas, roads, building fronts, doors, and public circulation form coherent public spatial systems.

Established validation categories including:
- plaza access validation,
- road approach validation,
- frontage exposure validation,
- door-to-frontage validation,
- public circulation validation,
- plaza landmark obstruction validation,
- market/shrine/civic frontage validation,
- private/restricted frontage separation,
- blank-wall plaza detection,
- and dead-public-space detection.

Added plaza access doctrine requiring active plazas to have meaningful approaches, readable exits, public-facing opportunities where appropriate, preserved traversal, and clear civic/landmark identity.

Added road/frontage compatibility doctrine tying shop/market, loading, noble ceremonial, military checkpoint, utility, and private housing frontages to appropriate road or buffer contexts.

Added blank-wall prevention doctrine for public roads and plazas.

Added landmark obstruction doctrine requiring plaza landmarks to enhance orientation without collapsing circulation.

Added Plaza / Road / Frontage Validation Standards reinforcing public circulation, approach readability, door readability, frontage coherence, emergency routing, AI navigation, and future crowd/event extensibility.

## 0.9.10bv — Phase 3 Room Clustering and Frontage Grouping Initiated

Advanced Phase 3 from spatial contradiction cleanup into room clustering and frontage grouping.

Reviewed completed Phase 3 work:
- spatial integration baseline expansion,
- frontage and door orientation systems,
- road hierarchy integration,
- plaza-centered district composition,
- alley and service-routing continuity,
- parking and loading-bay integration,
- roaded vs roadless zone doctrine,
- sewer pipe-network corridor rules,
- edge safety generation band doctrine,
- and spatial contradiction cleanup.

Defined the current bucket objective as organizing rooms into coherent building and facility groups rather than isolated independent rectangles.

Established room-cluster categories including:
- shop rows,
- apartment clusters,
- dormitory blocks,
- industrial facility clusters,
- warehouse/depot clusters,
- civic office clusters,
- shrine/chapel support clusters,
- military checkpoint/barracks clusters,
- noble compound clusters,
- market frontage groups,
- service-back clusters,
- and underhive improvised room clusters.

Added frontage grouping doctrine tying public-facing room groups to roads/plazas and service-facing room groups to alleys, service lanes, loading areas, and parking/service spaces.

Added room family adjacency doctrine for kitchens/storage/service access, shops/storage/frontage, barracks/armories/mess halls, depots/loading bays/cargo roads, apartments/sanitation/corridors, and industrial rooms/utility/maintenance routing.

Added cluster boundary doctrine using corridors, alleys, service lanes, courtyards, walls, restricted doors, plaza edges, parking/service yards, and faction/security markers.

Added district cluster differentiation doctrine for underhive, hab, industrial, military, and noble clusters.

Added Room Clustering and Frontage Grouping Standards reinforcing functional adjacency, service-back logic, public/private layering, traversal clarity, AI navigation, and future occupation/simulation attachment points.

## 0.9.10bu — Phase 3 Spatial Contradiction Cleanup Initiated

Advanced Phase 3 into spatial contradiction cleanup.

Reviewed completed Phase 3 work:
- spatial integration baseline expansion,
- frontage and door orientation systems,
- road hierarchy integration,
- plaza-centered district composition,
- alley and service-routing continuity,
- parking and loading-bay integration,
- roaded vs roadless zone doctrine,
- sewer pipe-network corridor rules,
- and edge safety generation band doctrine.

Defined the current bucket objective as validation and repair of contradictions between roads, corridors, frontages, rooms, plazas, alleys, parking/loading spaces, roadless zones, sewer networks, hazards, and edge bands.

Established contradiction categories including:
- frontage contradiction,
- access contradiction,
- road hierarchy contradiction,
- plaza access contradiction,
- alley/service contradiction,
- parking/loading contradiction,
- roaded/roadless family contradiction,
- edge-band contradiction,
- traversal contradiction,
- hazard visibility contradiction,
- and false-interaction contradiction.

Added repair-first doctrine supporting room rotation, doorway movement, connector corridors, road downgrades, roaded-to-corridor conversion, loading/parking relocation, service-lane addition, decorative replacement, or blocked/ruined conversion.

Added rejection doctrine for unreachable rooms, inaccessible doors, isolated plazas, edge-violating ordinary rooms, parking without approach, loading bays without service relationship, and road hierarchy applied to sewer/utility zones.

Added validation order doctrine:
1. zone family selection,
2. edge safety reservation,
3. central anchor placement,
4. road or corridor family generation,
5. frontage orientation,
6. room placement,
7. door/access placement,
8. plaza/alley/parking attachment,
9. hazard/readability validation,
10. contradiction repair or rejection.

Added Spatial Contradiction Cleanup Standards reinforcing repair/rejection behavior, traversal clarity, room accessibility, hazard visibility, AI navigation, emergency access, and future runtime attachment preservation.

## 0.9.10bt — Edge Safety Generation Band Logged

Added Phase 3 spatial safety doctrine for edge-band generation.

Identified that ordinary rooms, corridors, roads, alleys, plazas, parking lots, loading bays, and service spaces should not freely abut the raw map boundary because the map edge must remain available for:
- hive external wall definitions,
- exterior wall maintenance corridors,
- occasional hive exterior rooms,
- external shell infrastructure,
- service access rings,
- defensive/structural boundary features,
- and future map-wrapping or edge-transition behavior.

Established an edge safety generation band separating:
1. the normal interior generation field,
2. the reserved perimeter/exterior-wall band.

Defined edge-authorized feature families including:
- hive exterior walls,
- external maintenance corridors,
- perimeter service tunnels,
- exterior observation/service rooms,
- structural buttress rooms,
- wall machinery rooms,
- external utility access,
- armored boundary corridors,
- emergency perimeter access,
- and special edge-transition anchors.

Added validation doctrine requiring rejection or repair of:
- ordinary rooms touching raw map edges,
- corridors running into boundary voids,
- roads terminating into exterior shell space without transition logic,
- plazas consuming perimeter bands,
- parking lots against the map wall without service logic,
- and edge-adjacent doors opening into invalid exterior cells.

Added Edge Safety Generation Standards to preserve future hive exterior-wall architecture, perimeter maintenance corridors, edge-room placement flexibility, and map wrapping/transition assumptions.

## 0.9.10bs — Sewer Pipe-Network Corridor Rules Added

Expanded Phase 3 roadless zone doctrine with specialized sewer and utility sublevel corridor-network rules.

Established that sewers should feel like vast pipe-network corridors rather than ordinary building hallways or civic road systems.

Added sewer corridor-network objective favoring:
- long primary sewer trunks,
- parallel utility corridors,
- repeated cross-connector corridors,
- pump/filter junction chambers,
- drainage-channel branches,
- pipe-gallery bypasses,
- maintenance loops,
- service alcoves,
- ladder/service access pockets,
- and occasional collapsed or blocked bypasses.

Added parallel corridor doctrine requiring sewer layouts to support nearby parallel corridor systems where appropriate, with attempted cross-connections roughly every 4 to 5 corridor sections when geometry allows.

Added cross-connection doctrine for narrow maintenance links, pipe crossing galleries, drainage side passages, service crawlways, grated connector walks, pump-room shortcuts, inspection tunnels, and collapsed bypasses.

Added sewer chamber doctrine using pump halls, filter cisterns, inspection junctions, overflow tanks, sludge-control rooms, maintenance depots, valve-control rooms, drainage collection pits, and underhive camp pockets as navigation relief.

Added utility-network differentiation doctrine distinguishing sewer, utility sublevel, maintenance warren, and deep underhive corridor behavior.

Added Sewer Pipe-Network Corridor Standards reinforcing cross-link readability, hazard visibility, central-anchor preservation, and avoidance of recursive spaghetti layout collapse.

## 0.9.10br — Roaded vs Roadless Zone Doctrine Logged

Logged a critical Phase 3 spatial integration correction: not every zone family should use road placement.

Established that formal road hierarchy should primarily apply to roaded zone families such as:
- civilian sectors,
- noble sectors,
- PDF / military civic-control sectors,
- formal market districts,
- administrative districts,
- and other planned public movement zones.

Established that sewer, utility, maintenance, deep underhive, tunnel, service labyrinth, and infrastructure-heavy zones should fall back to corridor-and-chamber segmented generation rather than forced road placement.

Defined roadless zone generation around:
- primary corridor trunks,
- secondary branching corridors,
- chambers,
- junction rooms,
- maintenance nodes,
- pump/filter rooms,
- utility alcoves,
- pipe galleries,
- crawlspace-like branches,
- and improvised habitation pockets.

Preserved the central plaza philosophy as a universal navigation anchor rule. In roadless zones, the plaza role becomes a zone-appropriate central anchor such as:
- sewer junction chamber,
- utility nexus,
- pump hall,
- filter cistern,
- maintenance hub,
- underhive gathering pit,
- industrial service yard,
- or structural atrium.

Added corrected Phase 3 integration rule requiring spatial generation family selection before road hierarchy application:
1. roaded civic/administrative/noble/PDF surface layout,
2. roadless corridor-and-chamber infrastructure layout,
3. hybrid underhive/collapsed layout.

Added Roaded vs Roadless Zone Generation Standards reinforcing that roads must not be blindly applied to all zones.

## 0.9.10bq — Phase 3 Parking and Loading-Bay Integration Initiated

Advanced Phase 3 from alley/service-routing continuity into parking and loading-bay integration.

Reviewed completed Phase 3 work:
- Phase 3 initiation,
- spatial integration baseline expansion,
- frontage and door orientation systems,
- road hierarchy integration,
- plaza-centered district composition,
- alley and service-routing continuity,
- public/private threshold doctrine,
- frontage contradiction prevention,
- and road-to-frontage compatibility doctrine.

Defined the current bucket objective as treating parking, loading, and service yards as functional spatial systems rather than decorative open pavement.

Established parking/loading categories including:
- public parking lots,
- residential parking courts,
- noble/private vehicle courts,
- industrial loading bays,
- depot loading yards,
- military motor pools,
- service vehicle pull-ins,
- emergency vehicle access zones,
- underhive improvised parking/scrap courts,
- delivery van/truck bays,
- cargo staging aprons,
- blocked/ruined parking lots,
- and vehicle recall zones.

Added parking purpose doctrine clarifying public, residential, noble, industrial, depot, military, and underhive parking/loading roles.

Added loading bay doctrine requiring loading bays to connect to service roads, alleys, depots, warehouses, kitchens, industrial output rooms, faction stores, cargo corridors, and rear/service doors.

Added vehicle recall doctrine preserving future vehicle runtime attachment points without implementing full vehicle AI yet.

Added Parking and Loading-Bay Integration Standards reinforcing vehicle approach readability, road/service access, loading-bay coherence, recall-zone clarity, emergency routing, AI navigation, and future vehicle-system extensibility.

## 0.9.10bp — Phase 3 Alley and Service-Routing Continuity Initiated

Advanced Phase 3 from plaza-centered district composition into alley and service-routing continuity.

Reviewed completed Phase 3 work:
- Phase 3 initiation,
- spatial integration baseline expansion,
- frontage and door orientation systems,
- road hierarchy integration,
- plaza-centered district composition,
- public/private threshold doctrine,
- frontage contradiction prevention,
- and road-to-frontage compatibility doctrine.

Defined the current bucket objective as treating alleys and service routes as functional circulation systems rather than random narrow corridors.

Established alley/service categories including:
- rear service alleys,
- waste-access alleys,
- utility alleys,
- delivery alleys,
- underhive alleys,
- gang-control alleys,
- market-service alleys,
- shrine/service side alleys,
- industrial maintenance lanes,
- noble servant alleys,
- military restricted service corridors,
- alley alcoves,
- and blocked/ruined alley remnants.

Added alley purpose doctrine clarifying functional roles for rear service, utility, market-service, industrial, underhive, noble servant, and military service alleys.

Added alley-to-frontage doctrine tying alleys primarily to rear doors, service doors, loading doors, maintenance rooms, utility hatches, waste rooms, kitchen/service rooms, and hidden/private entrances.

Added alley continuity doctrine preventing meaningless cracks, repeated dead-end alley spam, service routes that service nothing, and unreadable alley mazes.

Added alcove integration doctrine for service recesses, sleeping niches, waste pockets, shrine corners, vendor storage, security blind spots, maintenance access, and structural damage.

Added Alley and Service-Routing Standards reinforcing rear-access readability, service circulation, player orientation, emergency pathing, AI navigation, hazard visibility, and future stealth/crime/faction extensibility.

## 0.9.10bo — Phase 3 Plaza-Centered District Composition Initiated

Advanced Phase 3 from road hierarchy integration into plaza-centered district composition.

Reviewed completed Phase 3 work:
- Phase 3 initiation,
- spatial integration baseline expansion,
- frontage and door orientation systems,
- road hierarchy integration,
- public/private threshold doctrine,
- frontage contradiction prevention,
- and road-to-frontage compatibility doctrine.

Defined the current bucket objective as treating plazas as district-scale civic anchors rather than random open rooms or decorative squares.

Established plaza categories including:
- central civic plazas,
- secondary neighborhood plazas,
- market plazas,
- shrine/monument plazas,
- industrial muster yards,
- military parade/checkpoint plazas,
- noble ceremonial courts,
- underhive gathering pits,
- transit waiting plazas,
- ration/distribution plazas,
- and ruined/blocked plaza remnants.

Added plaza purpose doctrine clarifying civic, market, shrine, industrial, military, noble, and underhive plaza roles.

Added plaza-to-road doctrine requiring plazas to connect logically to road hierarchy and avoid isolated decorative voids.

Added plaza-to-frontage doctrine tying adjacent rooms to plaza-facing behavior, including shops, shrines, civic counters, ration kiosks, guard posts, noble ceremonial rooms, and military controlled thresholds.

Added plaza landmark doctrine supporting orientation without central obstruction collapse.

Added Plaza-Centered Composition Standards reinforcing public circulation, frontage exposure, landmark readability, route visibility, emergency access, AI navigation, and district identity.

## 0.9.10bn — Phase 3 Road Hierarchy Integration Initiated

Advanced Phase 3 from frontage and door orientation into road hierarchy integration.

Reviewed completed Phase 3 work:
- Phase 3 initiation,
- spatial integration baseline expansion,
- spatial hierarchy doctrine,
- frontage and door orientation systems,
- public/private threshold doctrine,
- and frontage contradiction prevention.

Defined the current bucket objective as treating roads as a hierarchy of movement, access, service, and district identity rather than uniform corridors or decorative pavement.

Established road hierarchy categories including:
- major civic roads,
- secondary district roads,
- service roads,
- industrial cargo roads,
- military convoy roads,
- noble approach drives,
- underhive alleys,
- pedestrian corridors,
- plaza approach roads,
- emergency access lanes,
- depot approach roads,
- parking access lanes,
- and restricted/private roads.

Added road purpose doctrine clarifying the expected role of each road class.

Added road-to-frontage doctrine tying storefronts, loading docks, noble ceremonial fronts, utility rooms, military checkpoints, and underhive habitation to appropriate road types.

Added road continuity doctrine preventing roads that terminate without reason, parking without access, loading docks without cargo approach, blocked emergency lanes, and service roads that service nothing.

Added district road differentiation doctrine for underhive, hab, industrial, military, and noble districts.

Added Road Hierarchy Integration Standards reinforcing route readability, frontage coherence, plaza access, parking/loading access, emergency-route continuity, vehicle approach clarity, AI navigation, and future logistics extensibility.

## 0.9.10bm — Phase 3 Frontage and Door Orientation Systems Initiated

Advanced Phase 3 from spatial integration baseline into frontage and door orientation systems.

Reviewed completed Phase 3 work:
- Phase 3 initiation,
- spatial integration baseline expansion,
- spatial hierarchy doctrine,
- frontage-aware room placement doctrine,
- plaza anchoring doctrine,
- alley/service-lane doctrine,
- alcove distribution doctrine,
- and parking/service-space doctrine.

Defined the current bucket objective as treating rooms and buildings as oriented spatial objects with public-facing, service-facing, private/internal, restricted/security-facing, and no-door edges.

Established frontage/orientation categories including:
- public frontage,
- service frontage,
- private frontage,
- restricted frontage,
- utility frontage,
- loading frontage,
- plaza frontage,
- road frontage,
- alley frontage,
- parking frontage,
- and internal connector frontage.

Added door placement doctrine tying doors to room purpose and frontage context.

Added frontage contradiction prevention rules to avoid shops opening into blank walls, loading docks opening into bedrooms, apartments opening directly onto cargo roads without threshold logic, checkpoints facing non-traversable voids, and plaza fronts without accessible door logic.

Added district-specific frontage doctrine for underhive, hab, industrial, military, and noble districts.

Added public/private threshold doctrine for public, private, restricted, service, ceremonial, improvised, damaged, and breached thresholds.

Added Frontage and Door Orientation Standards reinforcing door readability, room accessibility, public/service/private coherence, traversal clarity, AI navigation, and avoidance of unsupported runtime implications.

## 0.9.10bl — Phase 3 Spatial Integration Baseline Expansion

Continued Phase 3 development after Phase 2 completion.

Expanded the active spatial integration baseline toward frontage-aware and hierarchy-aware district generation.

Defined current Phase 3 work as:
- frontage-aware room placement,
- road-facing room expectations,
- public/private threshold logic,
- plaza anchoring behavior,
- alley insertion logic,
- alcove distribution logic,
- parking/service-space placement,
- and service-lane continuity.

Added frontage doctrine clarifying that rooms/buildings should understand which side faces:
- roads,
- alleys,
- plazas,
- service corridors,
- loading areas,
- and restricted/private spaces.

Added plaza integration doctrine reinforcing plazas as:
- circulation anchors,
- congregation spaces,
- landmark spaces,
- market/shrine/public nodes,
- and road/frontage connectors.

Added alley doctrine reinforcing alleys as:
- service circulation,
- waste access,
- maintenance routing,
- utility corridors,
- emergency bypasses,
- and faction-control spaces.

Added alcove doctrine clarifying that alcoves should emerge naturally from spatial structure rather than random clutter insertion.

Added parking/service-space doctrine reinforcing believable vehicle approach, loading logic, emergency access, and faction/service readability.

Established spatial hierarchy doctrine:
1. major roads,
2. secondary roads,
3. plazas,
4. frontages,
5. alleys/service lanes,
6. alcoves/recesses,
7. interior access,
8. utility/service access.

Added Phase 3 Spatial Anatomy Standards reinforcing frontage clarity, room accessibility, alley/service logic, traversal readability, AI navigation, and continued adherence to Phase 2 semantic doctrine.

## 0.9.10bk — Phase 2 Completed / Phase 3 Spatial Integration Initiated

Marked Phase 2 semantic-foundation implementation buckets as completed.

Phase 2 is now closed as the semantic-foundation and implementation-readiness phase covering:
- Ecclesiarchy/environmental semantics,
- domestic/hab semantics,
- industrial stamp semantics,
- faction occupation semantics,
- utility infrastructure semantics,
- logistics/provenance groundwork,
- transportation/cargo semantics,
- population infrastructure preparation,
- economic-flow groundwork,
- world-generation cohesion,
- documentation consolidation,
- verticality semantic boundary clarification,
- zone transition/adjacency rules,
- and final stabilization/audit doctrine.

Confirmed Phase 2 did not activate deferred runtime systems including active population simulation, provenance runtime tracking, economy simulation, vehicle AI, logistics routing, dynamic faction warfare, live events, or multi-level building runtime architecture.

Initiated Phase 3 — Room, Road, Frontage, Plaza, Alcove, Alley, and Parking Integration.

Defined Phase 3 objective as district-scale spatial integration of:
- rooms,
- roads,
- building frontages,
- plazas,
- alcoves,
- alleys,
- parking lots,
- service lanes,
- loading areas,
- vehicle recall spaces,
- and public/private threshold logic.

Established Phase 3 Spatial Integration Standards reinforcing road/room coherence, frontage readability, plaza access logic, alley service logic, parking entry logic, vehicle approach clarity, AI navigation, emergency access, traversal readability, and continued adherence to Phase 2 semantic doctrine.

## 0.9.10bj — Final Phase 2 Stabilization / Audit Sweep Initiated

Advanced Phase 2 into its final closing target: Stabilization / Audit Sweep.

Reviewed the complete Phase 2 semantic framework including:
- Ecclesiarchy/environmental-semantic stabilization,
- domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- utility infrastructure semantic passes,
- logistics/provenance groundwork,
- transportation/cargo semantic passes,
- population infrastructure preparation,
- economic-flow environmental groundwork,
- world-generation cohesion review,
- documentation consolidation,
- verticality semantic boundary clarification,
- and zone transition/adjacency sweep.

Defined the current target as final semantic stabilization, doctrine auditing, overlap verification, runtime-bandwidth review, deferred-system separation validation, and future extensibility protection.

Established final audit targets including:
- semantic overlap conflicts,
- contradictory doctrine,
- layering-order violations,
- district identity conflicts,
- traversal/readability conflicts,
- transportation continuity,
- utility-routing continuity,
- faction-layer consistency,
- verticality boundary enforcement,
- and deferred-system separation clarity.

Added framework-lock doctrine clarifying that the Phase 2 semantic framework should now be considered structurally coherent, modular, append-friendly, future-extensible, and implementation-ready.

Added deferred-system separation doctrine reinforcing that runtime systems such as population simulation, provenance tracking, economy simulation, vehicle AI, logistics routing, faction warfare, dynamic events, and vertical runtime architecture remain intentionally deferred.

Added Final Phase 2 Stabilization Standards reinforcing semantic consistency, layering clarity, runtime safety, extensibility, maintainability, and prevention of premature runtime assumptions.

Confirmed that future phases should attach runtime systems to the established semantic framework rather than replacing foundational doctrine.

## 0.9.10bi — Zone Transition / Adjacency Sweep Initiated

Advanced Phase 2 into the second of the final four semantic closing targets: Zone Transition / Adjacency Sweep.

Reviewed completed Phase 2 semantic framework work:
- Ecclesiarchy/environmental-semantic stabilization,
- domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- utility infrastructure semantic passes,
- logistics/provenance groundwork,
- transportation/cargo semantic passes,
- population infrastructure preparation,
- economic-flow environmental groundwork,
- world-generation cohesion review,
- documentation consolidation,
- and verticality semantic boundary clarification.

Defined this target as a semantic transition sweep focused on district edges, zone adjacency, transition corridors, faction-control boundaries, and escalation/de-escalation pacing rather than runtime territory simulation.

Established adjacency categories including:
- district edge blending,
- transition corridors,
- gatehouse/checkpoint edges,
- alley-to-road transitions,
- plaza-to-corridor transitions,
- industrial-to-hab edges,
- noble-to-civilian edges,
- underhive-to-maintenance transitions,
- sewer-to-surface transitions,
- military-security escalation edges,
- market-to-logistics transitions,
- utility-to-public-space transitions,
- and faction-control boundary markers.

Added transition pacing doctrine to prevent abrupt semantic collisions unless deliberately dramatic.

Added boundary composition doctrine using signage shifts, flooring changes, lighting shifts, maintenance changes, patrol/checkpoint semantics, utility routing changes, clutter-density shifts, faction markers, and hazard/degradation gradients.

Added Zone Transition / Adjacency Standards reinforcing district readability, traversal clarity, player orientation, door readability, hazard visibility, emergency-route coherence, and deferral of active territory simulation.

Confirmed continued deferral of:
- dynamic territory control,
- active faction expansion,
- runtime border warfare,
- dynamic lockdowns,
- living district migration,
- and active faction-front simulation.

## 0.9.10bh — Verticality Runtime Boundary Logged

Continued Phase 2 verticality discussion and identified a critical architectural boundary.

Confirmed that the current system does not yet support true multi-floor runtime architecture, building-local floor stacks, elevator menu routing, cross-floor NPC following, hostile pursuit across Z boundaries, or persistent stacked-zone pathing.

Reclassified the current Verticality Semantic Sweep as semantic preparation only rather than runtime vertical implementation.

Logged a long-term deferred Multi-Level Architecture / Building Stack Runtime Pass to resolve:
- multi-level buildings,
- stairs,
- elevator menu access for multi-floor elevators,
- cross-floor pathfinding,
- NPC following across Z boundaries,
- hostile pursuit across Z boundaries,
- companion regrouping after vertical transitions,
- building identity tracking across stacked rooms,
- persistence across stacked zones,
- cargo elevation routing,
- hazard propagation across floors,
- and clear player-facing transition expectations.

Added corrected vertical access doctrine:
- controlled vertical access belongs to stairs, freight lifts, service lifts, elevators, secured access cores, and faction-restricted vertical routes;
- uncontrolled or dangerous vertical access belongs to sewers, ladders, broken shafts, collapsed floors, maintenance hatches, and underhive descent routes.

Added explicit caution that elevators cannot be safely patched as simple up/down transitions without a broader architectural model because repeated down-transition chaining could unintentionally allow uncontrolled hive descent.

Added Multi-Level Architecture Deferral Standard to standards documentation.

Confirmed Phase 2 should continue semantic development only.

## 0.9.10bg — Verticality Semantic Sweep Initiated

Advanced Phase 2 into the first of the final four closing targets: Verticality Semantic Sweep.

Reviewed completed Phase 2 semantic framework work:
- Ecclesiarchy/environmental-semantic stabilization,
- domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- utility infrastructure semantic passes,
- logistics/provenance groundwork,
- transportation/cargo semantic passes,
- population infrastructure preparation,
- economic-flow environmental groundwork,
- world-generation cohesion review,
- and documentation consolidation.

Defined this target as semantic preparation for stacked hive-city depth, overhead/underfoot spaces, and future multi-level integration without activating full vertical runtime systems yet.

Established vertical semantic categories including:
- stairwell stamps,
- lift/elevator lobbies,
- freight lift zones,
- maintenance shafts,
- sewer descent points,
- rooftop access hatches,
- gantry/catwalk semantics,
- balcony/overlook markings,
- basement access points,
- sublevel utility entrances,
- ventilation shaft indicators,
- cargo-hoist areas,
- collapsed-floor markers,
- and vertical warning signage.

Added district verticality doctrine for underhive, hab, industrial, military, and noble districts.

Added Verticality Semantic Standards reinforcing current-floor traversal readability, hazard visibility, combat readability, future pathing extensibility, and avoidance of false interactive promises.

Confirmed continued deferral of:
- full multi-level runtime simulation,
- active elevator systems,
- rooftop gameplay,
- vertical AI pathing,
- cargo elevation routing,
- and persistent stacked-floor simulation.

## 0.9.10bf — Documentation Consolidation and Stabilization Initiated

Advanced Phase 2 development into Documentation Consolidation and Stabilization.

Reviewed completed Phase 2 work:
- Ecclesiarchy/environmental-semantic stabilization,
- domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- utility infrastructure semantic passes,
- logistics/provenance groundwork,
- transportation/cargo semantic passes,
- population infrastructure preparation,
- economic-flow environmental groundwork,
- and world-generation cohesion review.

Defined the current bucket objective as long-term stabilization and consolidation of:
- semantic layering doctrine,
- district identity doctrine,
- traversal/readability doctrine,
- runtime-protection doctrine,
- logistics/provenance assumptions,
- transportation semantics,
- population groundwork assumptions,
- faction-layering assumptions,
- infrastructure-routing doctrine,
- and deferred-system boundaries.

Added implementation-readiness doctrine clarifying:
- conceptual systems,
- groundwork-only systems,
- intentionally deferred systems,
- mandatory runtime protections,
- and semantic ordering rules.

Added deferred-system boundary doctrine reinforcing that Phase 2 prepares for but does not yet activate:
- active economy simulation,
- logistics routing,
- crowd simulation,
- advanced schedules,
- provenance runtime tracking,
- dynamic faction warfare,
- vehicle AI,
- and live world events.

Added Documentation Consolidation Standards reinforcing semantic consistency, doctrine clarity, future extensibility, implementation readability, and maintainable long-term planning.

Confirmed continued deferral of:
- active simulation systems,
- advanced AI behaviors,
- runtime provenance tracking,
- dynamic logistics,
- social simulation,
- vehicle AI,
- and live event systems.

## 0.9.10be — World-Generation Cohesion Review Initiated

Advanced Phase 2 development into World-Generation Cohesion Review.

Reviewed completed Phase 2 work:
- Ecclesiarchy/environmental-semantic stabilization,
- hab/domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- utility infrastructure semantic passes,
- logistics/provenance groundwork,
- transportation/cargo semantic passes,
- population infrastructure preparation,
- economic-flow environmental groundwork,
- and readability/runtime governance.

Defined the current bucket objective as unified review and integration validation for the complete Phase 2 semantic world-generation stack.

Established cohesion-review targets including:
- district identity consistency,
- semantic overlap conflicts,
- environmental pacing consistency,
- landmark rarity integrity,
- traversal readability,
- hazard readability,
- transportation continuity,
- utility-routing continuity,
- faction layering compatibility,
- domestic/industrial transition quality,
- and future expansion-space preservation.

Added transition-quality doctrine reinforcing believable transitions between industrial, civilian, noble, military, transport, congregation, and underhive spaces.

Added semantic layering doctrine defining stable environmental stack order:
1. structure,
2. infrastructure,
3. transportation,
4. industrial/commercial,
5. habitation,
6. faction occupation,
7. devotional overlays,
8. hazards,
9. degradation,
10. narrative overlays.

Added World-Generation Cohesion Standards reinforcing scalability, maintainability, readability, future extensibility, and prevention of semantic contradiction.

Confirmed continued deferral of:
- active population simulation,
- provenance/economic runtime systems,
- dynamic logistics,
- vehicle AI,
- social simulation,
- dynamic faction warfare,
- and live world-event systems.

## 0.9.10bd — Economic-Flow Environmental Groundwork Initiated

Advanced Phase 2 development into Economic-Flow Environmental Groundwork.

Reviewed completed Phase 2 work:
- Ecclesiarchy/environmental-semantic stabilization,
- hab/domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- utility infrastructure semantic passes,
- logistics/provenance groundwork,
- transportation/cargo semantic passes,
- population infrastructure preparation,
- and readability/runtime governance.

Defined the current bucket objective as environmental preparation for future economic-flow systems without activating full economy simulation yet.

Established economic semantic categories including:
- market stalls,
- ration kiosks,
- supply counters,
- trade windows,
- merchant storage,
- procurement offices,
- labor hiring points,
- black-market corners,
- informal barter spaces,
- debt-record offices,
- tax/inspection booths,
- industrial procurement stations,
- civilian exchange counters,
- and faction-controlled commercial checkpoints.

Added economic-pressure differentiation doctrine covering:
- underhive economic spaces,
- civilian hab economic spaces,
- industrial economic spaces,
- military economic spaces,
- and noble economic spaces.

Added economic-flow readability doctrine reinforcing that players should infer:
- where goods are acquired,
- where labor exchanges occur,
- where shortages accumulate,
- where factions control supply,
- where black markets operate,
- and where wealth disparity becomes visible.

Added Economic-Flow Environmental Standards reinforcing commerce readability, crowd-routing readability, traversal clarity, faction-commercial readability, and deferral of active simulation.

Confirmed continued deferral of:
- active economy simulation,
- pricing systems,
- labor simulation,
- trader AI,
- dynamic scarcity systems,
- faction resource simulation,
- and provenance/economic runtime tracking.

## 0.9.10bc — Population Infrastructure Preparation Initiated

Advanced Phase 2 development into Population Infrastructure Preparation.

Reviewed completed Phase 2 work:
- Ecclesiarchy/environmental-semantic stabilization,
- hab/domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- utility infrastructure semantic passes,
- logistics/provenance groundwork,
- transportation/cargo semantic passes,
- and readability/runtime governance.

Defined the current bucket objective as environmental and structural preparation for future population simulation systems without activating advanced civilian AI or schedules yet.

Established population-support semantic categories including:
- queue spaces,
- waiting benches,
- ration lines,
- public gathering areas,
- dormitory overflow spaces,
- civilian congregation rooms,
- break areas,
- smoking corners,
- workforce staging areas,
- checkpoint queues,
- public announcement points,
- transit waiting areas,
- mess halls,
- and overflow sleeping arrangements.

Added population-density differentiation doctrine covering:
- underhive population spaces,
- civilian hab spaces,
- industrial workforce spaces,
- military population spaces,
- and noble congregation spaces.

Added human-flow readability doctrine reinforcing that players should infer:
- civilian gathering,
- workforce waiting,
- shift rotation,
- ration pressure,
- rest behavior,
- and crowd accumulation
through environmental composition.

Added Population Infrastructure Preparation Standards reinforcing traversal clarity, crowd-routing readability, hazard visibility, scalable congregation semantics, and deferral of active simulation.

Confirmed continued deferral of:
- active crowd simulation,
- civilian scheduling,
- workforce AI,
- dynamic congregation systems,
- reputation/social systems,
- provenance/economic simulation,
- and advanced population behavior.

## 0.9.10bb — Transportation / Cargo Semantic Pass Initiated

Advanced Phase 2 development into Transportation / Cargo Semantic Passes.

Reviewed completed Phase 2 work:
- Ecclesiarchy/environmental-semantic stabilization,
- hab/domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- utility infrastructure semantic passes,
- logistics/provenance groundwork,
- and readability/runtime governance.

Defined the current bucket objective as visible cargo movement and vehicle-access semantics supporting:
- vehicles,
- loading routes,
- service roads,
- cargo bays,
- depot approaches,
- parking lots,
- alleys,
- delivery corridors,
- vehicle recall spaces,
- and pedestrian/vehicle separation.

Established transportation semantic categories including:
- parking lots,
- alley service lanes,
- delivery roads,
- cargo bay approaches,
- loading ramps,
- vehicle turning areas,
- vehicle recall zones,
- depot entrances,
- pedestrian crossing markings,
- service-road signage,
- cargo lane markings,
- vehicle maintenance bays,
- fuel/charging/service points,
- and security-gated transport chokepoints.

Added vehicle access doctrine preventing blocked approach cells, isolated loading bays, parking lots without entry routes, depots without turning space, and decorative clutter consuming service roads.

Added district transportation differentiation doctrine covering:
- underhive transport,
- civilian hab transport,
- industrial transport,
- military transport,
- and noble transport.

Added Transportation / Cargo Semantic Standards reinforcing vehicle access readability, cargo-flow readability, emergency routes, pedestrian traversal, and scalable modular route semantics.

Confirmed continued deferral of:
- active vehicle AI,
- dynamic traffic simulation,
- cargo routing simulation,
- vehicle ownership systems,
- fuel economy systems,
- and logistics/economic simulation.

## 0.9.10ba — Logistics / Provenance Groundwork Initiated

Advanced Phase 2 development into Logistics / Provenance Groundwork.

Reviewed completed Phase 2 work:
- Ecclesiarchy/environmental-semantic stabilization,
- hab/domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- utility infrastructure semantic passes,
- and readability/runtime governance.

Defined the current bucket objective as environmental and documentation groundwork for future logistics, cargo-flow, faction-store, and item-provenance systems without activating full economic simulation yet.

Established logistics semantic categories including:
- cargo staging areas,
- warehouse storage blocks,
- loading docks,
- unloading bays,
- supply cages,
- faction stores,
- inspection tables,
- manifest desks,
- cargo lifts,
- delivery corridors,
- service roads,
- trade kiosks,
- depot offices,
- ration-distribution points,
- and production-output holding areas.

Added provenance-preparation doctrine supporting future:
- production origin points,
- storage ownership,
- transfer points,
- trader inventory sourcing,
- civilian distribution,
- military supply chains,
- and black-market diversion points.

Added cargo-flow readability doctrine reinforcing that players should infer where goods arrive, where they are stored, who controls them, where inspection happens, and where distribution or theft could occur.

Added Logistics / Provenance Groundwork Standards reinforcing cargo-flow readability, traversal clarity, vehicle access, storage readability, faction ownership readability, and deferral of full simulation.

Confirmed continued deferral of:
- full item provenance tracking,
- economic simulation,
- dynamic logistics routing,
- trader inventory sourcing logic,
- active supply-chain simulation,
- and faction store economy behavior.

## 0.9.10az — Utility Infrastructure Semantic Pass Initiated

Advanced Phase 2 development into Utility Infrastructure Semantic Passes.

Reviewed completed Phase 2 work:
- Ecclesiarchy/environmental-semantic stabilization,
- hab/domestic semantic expansion,
- industrial stamp expansion,
- faction occupation semantic layering,
- and readability/runtime governance.

Defined the current bucket objective as visible infrastructural world support semantics covering:
- power distribution,
- water routing,
- waste handling,
- ventilation,
- sewage,
- cargo movement,
- maintenance access,
- emergency systems,
- and industrial support infrastructure.

Established infrastructure semantic categories including:
- pipe-routing systems,
- ventilation trunks,
- maintenance hatches,
- utility junctions,
- transformer stations,
- sewage routing,
- water-processing rooms,
- waste handling areas,
- emergency shutoff stations,
- cargo utility corridors,
- maintenance access routes,
- industrial support chambers,
- utility signage,
- and infrastructure degradation overlays.

Added infrastructure differentiation doctrine covering:
- underhive infrastructure,
- standard civilian infrastructure,
- industrial infrastructure,
- military infrastructure,
- and noble infrastructure.

Added infrastructure readability doctrine reinforcing that players should infer:
- power flow,
- waste routing,
- ventilation movement,
- maintenance circulation,
- and cargo/service routing
through environmental composition.

Added Utility Infrastructure Semantic Standards reinforcing maintenance readability, hazard visibility, traversal clarity, scalable infrastructure layering, and modular routing reuse.

Confirmed continued deferral of:
- active utility simulation,
- power-grid gameplay,
- logistics simulation,
- dynamic maintenance systems,
- advanced workforce AI,
- provenance/economic infrastructure,
- and infrastructure failure simulation.

## 0.9.10ay — Faction Occupation Semantic Pass Initiated

Advanced Phase 2 development into Faction Occupation Semantic Passes.

Reviewed completed Phase 2 work:
- Ecclesiarchy/environmental-semantic stabilization,
- lighting and hazard governance,
- runtime density balancing,
- atlas consolidation,
- generation distribution rebalance,
- hab/domestic semantic expansion,
- and industrial stamp expansion.

Defined the current bucket objective as environmental territorial readability and faction occupation layering without activating full faction simulation systems.

Established faction semantic categories including:
- occupation signage,
- faction banners,
- patrol routes,
- barricade logic,
- security checkpoints,
- propaganda placement,
- supply staging,
- inspection stations,
- territory markings,
- faction storage,
- devotional overlays,
- and faction-specific maintenance conditions.

Added faction differentiation doctrine covering:
- Imperial civilian control,
- Mechanicus control,
- military control,
- underhive gang control,
- and noble/private control.

Added territorial readability doctrine reinforcing that players should infer:
- stability,
- neglect,
- militarization,
- desperation,
- discipline,
- corruption,
- and privilege
through environmental composition before dialogue or UI confirmation.

Added Faction Occupation Semantic Standards reinforcing scalable territorial readability, traversal clarity, combat readability, hazard visibility, and reusable faction-layer systems.

Confirmed continued deferral of:
- active faction diplomacy,
- reputation systems,
- territory simulation,
- patrol AI systems,
- advanced social simulation,
- provenance/economic systems,
- and dynamic faction warfare behavior.

## 0.9.10ax — Industrial Stamp Expansion Initiated

Advanced Phase 2 development into Industrial Stamp Expansion.

Reviewed completed Phase 2 work:
- Ecclesiarchy/environmental-semantic stabilization,
- lighting and hazard readability governance,
- runtime density balancing,
- atlas consolidation,
- generation distribution rebalance,
- and hab/domestic semantic expansion.

Defined the current bucket objective as scalable industrial environmental grammar supporting:
- factories,
- machine halls,
- assembly spaces,
- refinery blocks,
- utility plants,
- warehouse structures,
- manufactorum floors,
- industrial storage,
- processing facilities,
- and logistics-adjacent infrastructure.

Established industrial semantic categories including:
- machine-line layouts,
- utility routing spaces,
- industrial storage blocks,
- refinery zones,
- fabrication stations,
- worker safety paths,
- maintenance alcoves,
- industrial signage,
- control booths,
- loading/unloading areas,
- machine sanctification crossover spaces,
- and industrial degradation overlays.

Added industrial district differentiation doctrine covering:
- low-grade industrial sectors,
- standard industrial sectors,
- military industrial sectors,
- and noble/private industrial sectors.

Added production-flow readability doctrine reinforcing that industrial layouts should visually imply:
- material movement,
- worker movement,
- servicing paths,
- utility routing,
- loading operations,
- and production hierarchy.

Added Industrial Stamp Expansion Standards reinforcing workflow readability, maintenance access, hazard visibility, runtime efficiency, and scalable industrial semantic reuse.

Confirmed continued deferral of:
- advanced logistics simulation,
- production economy systems,
- live worker schedules,
- industrial ownership simulation,
- advanced AI workforce behavior,
- and provenance/economic infrastructure.

## 0.9.10aw — Hab / Domestic Semantic Expansion Initiated

Advanced Phase 2 development beyond Ecclesiarchy/environmental-semantic stabilization into Hab / Domestic Semantic Expansion.

Reviewed completed Phase 2 stabilization work:
- Ecclesiarchy fixture families,
- chapel grammar and generation rules,
- plaza religious integration,
- underhive shrine variants,
- cathedral elevation doctrine,
- lighting compatibility governance,
- hazard compatibility verification,
- runtime density verification,
- atlas redundancy cleanup,
- and generation distribution rebalance.

Defined the current bucket objective as expansion of civilian/domestic environmental language across:
- dormitories,
- apartments,
- servant quarters,
- kitchens,
- laundries,
- bathrooms,
- communal hab spaces,
- storage rooms,
- worker rest areas,
- and low-class survival habitation.

Established domestic semantic categories including:
- bedding families,
- storage lockers,
- wardrobes,
- kitchen areas,
- sanitation fixtures,
- utility shelves,
- civilian clutter,
- devotional corners,
- servant storage,
- and habitation wear overlays.

Added hab district differentiation doctrine covering:
- underhive survival habitation,
- hab-block housing,
- industrial worker housing,
- military housing,
- and noble habitation spaces.

Added human-presence doctrine emphasizing fatigue, habitation, storage pressure, class distinction, and long-term occupation semantics even before advanced NPC simulation exists.

Added Hab / Domestic Expansion Standards reinforcing believable habitation scale, traversal readability, combat maneuverability, runtime efficiency, and modular semantic reuse.

Confirmed continued deferral of:
- advanced civilian AI routines,
- daily schedules,
- ownership simulation,
- crowd simulation,
- provenance/economic systems,
- and advanced social behavior systems.

## 0.9.10av — Generation Distribution Rebalance Initiated

Advanced Phase 2 environmental-semantic development into Generation Distribution Rebalance.

Reviewed completed Phase 2 work:
- Ecclesiarchy fixture families,
- modular chapel grammar,
- chapel generation rules,
- plaza religious integration,
- improvised underhive shrine variants,
- cathedral elevation doctrine,
- lighting and atmosphere compatibility governance,
- hazard compatibility verification,
- runtime density verification,
- and atlas redundancy cleanup.

Defined the current bucket objective as coherent large-scale distribution balancing for environmental semantic systems across districts, plazas, corridors, industrial sectors, noble zones, military regions, cathedral landmarks, and transitional traversal spaces.

Established rebalance targets including:
- shrine frequency weighting,
- memorial frequency weighting,
- cathedral rarity scaling,
- plaza landmark spacing,
- corridor clutter density,
- devotional-anchor concentration,
- underhive variance distribution,
- environmental quiet-space preservation,
- and district semantic pacing.

Added environmental pacing doctrine requiring deliberate contrast between:
- dense and sparse spaces,
- ceremonial and practical spaces,
- cluttered and readable spaces,
- and tension/recovery traversal rhythms.

Added district identity doctrine reinforcing distinct environmental composition styles for:
- underhive,
- hab,
- industrial,
- military,
- and noble districts.

Added landmark cadence doctrine preventing cathedral spam, shrine oversaturation, memorial overpopulation, and landmark exhaustion.

Added future-proof distribution doctrine preserving semantic generation budgets for later:
- faction systems,
- crowd systems,
- territory control,
- provenance simulation,
- economic systems,
- and dynamic event infrastructure.

Added Generation Distribution Standards reinforcing readability-first procedural pacing, rarity weighting, district distinction, and scalable semantic distribution.

Confirmed continued deferral of:
- crowd simulation,
- territory control gameplay,
- provenance/economic simulation,
- pilgrimage systems,
- priest AI,
- blessing systems,
- confession mechanics,
- and Ecclesiarchy economy behavior.

## 0.9.10au — Atlas Redundancy Cleanup Initiated

Advanced Phase 2 environmental-semantic development into Atlas Redundancy Cleanup.

Reviewed completed Phase 2 work:
- Ecclesiarchy fixture families,
- modular chapel grammar,
- chapel generation rules,
- plaza religious integration,
- improvised underhive shrine variants,
- cathedral elevation doctrine,
- lighting and atmosphere compatibility governance,
- hazard compatibility verification,
- and runtime density verification.

Defined the current bucket objective as consolidation of redundant environmental fixture families, semantic overlays, and fragmented atlas growth while preserving district identity and environmental readability.

Established redundancy cleanup targets including:
- duplicate candle families,
- duplicate reliquary families,
- duplicate pillar sets,
- duplicate flooring variants,
- duplicate hazard-border graphics,
- duplicate shrine overlays,
- duplicate signage families,
- duplicate grime overlays,
- and redundant lighting variants.

Added reuse-first doctrine emphasizing contextual placement, degradation variance, lighting variance, spacing variance, rarity weighting, and layered semantic composition over uncontrolled one-off atlas expansion.

Added atlas consolidation doctrine organizing reusable modular atlas families into:
- shared structural sets,
- shared devotional sets,
- shared hazard overlays,
- shared lighting overlays,
- shared degradation overlays,
- and district-specific accent overlays.

Added Atlas Consolidation Standards reinforcing modular atlas reuse, district readability preservation, silhouette clarity, and maintainable long-term environmental scaling.

Confirmed continued deferral of:
- live congregation systems,
- district prestige simulation,
- active religious rituals,
- priest AI,
- blessing systems,
- confession mechanics,
- and Ecclesiarchy economy behavior.

## 0.9.10at — Runtime Density Verification Initiated

Advanced Phase 2 environmental-semantic development into Runtime Density Verification.

Reviewed completed Phase 2 work:
- Ecclesiarchy fixture families,
- modular chapel grammar,
- chapel generation rules,
- plaza religious integration,
- improvised underhive shrine variants,
- cathedral elevation doctrine,
- lighting and atmosphere compatibility governance,
- and hazard compatibility verification.

Defined the current bucket objective as validation of environmental semantic scalability, readability, and runtime stability under increasing district density and environmental layering.

Established runtime-density verification targets including:
- fixture density,
- overlay density,
- transparency overlap counts,
- lighting overlap counts,
- semantic stamp concentration,
- decorative redundancy,
- pathfinding obstruction risk,
- and district readability degradation.

Added density scaling doctrine ensuring environmental richness scales according to:
- district wealth,
- district importance,
- faction control,
- plaza significance,
- and gameplay-space requirements.

Added semantic repetition doctrine emphasizing contextual variance, degradation variance, lighting variance, spacing variance, rotation variance, and rarity weighting rather than uncontrolled one-off asset growth.

Added Runtime Density Standards reinforcing traversal readability, combat maneuverability, AI navigation reliability, emergency-route visibility, and stable runtime behavior.

Confirmed continued deferral of:
- dynamic crowd simulation,
- live congregation systems,
- active district prestige systems,
- pilgrimage simulation,
- priest AI,
- blessing mechanics,
- and Ecclesiarchy economy behavior.

## 0.9.10as — Hazard Compatibility Verification Initiated

Advanced Phase 2 environmental-semantic development into Hazard Compatibility Verification.

Reviewed completed Phase 2 work:
- Ecclesiarchy fixture families,
- modular chapel grammar,
- chapel generation rules,
- plaza religious integration,
- improvised underhive shrine variants,
- cathedral elevation doctrine,
- and lighting/atmosphere compatibility governance.

Defined the current bucket objective as interoperability validation between environmental semantics and:
- toxic gas,
- sludge,
- electrical hazards,
- freezing zones,
- heat zones,
- contamination overlays,
- darkness,
- emergency-state rendering,
- and future hazard-state systems.

Established required hazard compatibility checks preserving:
- traversal readability,
- hazard readability,
- combat visibility,
- emergency-route visibility,
- interaction visibility,
- loot visibility,
- and door readability during mixed hazard overlap conditions.

Added Hazard Compatibility Standards enforcing readability hierarchy between:
1. traversal,
2. hazards,
3. hostile actors,
4. interaction objects,
5. loot,
6. environmental semantics,
7. decorative atmosphere.

Established environmental hazard storytelling doctrine allowing devotional and memorial semantics to coexist beside dangerous industrial and contaminated spaces without compromising gameplay comprehension.

Confirmed continued deferral of:
- active hazard gameplay escalation systems,
- plague simulation,
- warp contamination mechanics,
- priest AI,
- blessing systems,
- confession mechanics,
- and Ecclesiarchy economy behavior.

## 0.9.10ar — Lighting and Atmosphere Compatibility Pass Initiated

Advanced Phase 2 environmental-semantic development into the Lighting and Atmosphere Compatibility Pass.

Reviewed completed Phase 2 work:
- Ecclesiarchy fixture families,
- modular chapel grammar,
- chapel generation rules,
- plaza religious integration,
- improvised underhive shrine variants,
- and cathedral elevation doctrine.

Defined the current bucket objective as unified readability verification under darkness, emergency lighting, smoke, gas, sludge, spark, low-power, and mixed atmospheric rendering conditions.

Established required compatibility states for shrine, chapel, cathedral, and devotional semantic fixtures across:
- full lighting,
- partial darkness,
- red emergency lighting,
- flickering low-power conditions,
- smoke-heavy rooms,
- gas contamination,
- electrical hazard events,
- and combined hazard overlap conditions.

Added lighting readability doctrine reinforcing that players must retain visibility of:
- traversal corridors,
- doors,
- hostile actors,
- hazards,
- loot,
- interaction points,
- and navigational landmarks.

Added shrine/cathedral lighting rules preventing:
- bloom saturation,
- excessive candle clutter,
- decorative darkness traps,
- smoke-obscured hazards,
- and transparency-stack collapse.

Added Lighting and Atmosphere Compatibility Standards reinforcing readability-first atmospheric rendering and lightweight runtime lighting behavior.

Confirmed continued deferral of:
- live lighting gameplay systems,
- active religious rituals,
- priest AI,
- pilgrimage behavior,
- blessing systems,
- confession mechanics,
- and Ecclesiarchy economy behavior.

## 0.9.10aq — Cathedral Elevation Pass Initiated

Advanced Phase 2 environmental-semantic development into the Cathedral Elevation Pass.

Reviewed and reaffirmed previously completed buckets:
- Ecclesiarchy fixture-family declaration,
- modular chapel stamp grammar,
- chapel generation-rule definition,
- plaza religious integration,
- and improvised underhive shrine variants.

Defined the Cathedral Elevation Pass as a reusable landmark-scale architectural layering system rather than a bespoke handcrafted cathedral-content pipeline.

Established cathedral semantic layering:
1. floor grammar,
2. nave and aisle grammar,
3. pillar spacing,
4. altar anchors,
5. reliquary placement,
6. lighting,
7. memorial/statue passes,
8. prestige overlays,
9. degradation overlays,
10. hazard overlays.

Added required cathedral module classes including:
- nave sections,
- side aisles,
- transept intersections,
- sanctuary endcaps,
- choir sections,
- reliquary chambers,
- memorial halls,
- devotional galleries,
- clergy storage rooms,
- processional paths,
- elevated sermon platforms,
- annex connectors,
- and cathedral plaza transitions.

Added Cathedral Composition Standards reinforcing readability-first cathedral construction, layered semantic reuse, movement clarity, hazard visibility, and runtime efficiency.

Reinstated explicit continuation reporting requirements inside standards documentation:
every continuation pass must explain:
- what has been completed,
- what is currently being worked on,
- what must happen next,
- what remains deferred,
- and what protections/rules were enforced during the pass.

Confirmed continued deferral of:
- live priest AI,
- religion simulation,
- pilgrimage systems,
- blessing systems,
- confession mechanics,
- standing repair,
- and Ecclesiarchy economy behavior.

## 0.9.10ap — Improvised Underhive Shrine Variant Bucket Initiated

Advanced Phase 2 from plaza religious integration into improvised underhive shrine variants.

Defined the underhive shrine bucket as an environmental storytelling pass focused on survival devotion, poverty, contaminated sanctity, improvised orthodoxy, local folk practice, fear, hunger, and social desperation without activating religion simulation systems.

Added underhive shrine variant families including:
- welded scrap shrines,
- pipe-wall saint boards,
- candle piles in drainage corners,
- devotional graffiti walls,
- aquila symbols painted over hazard markings,
- shrine corners built into homeless encampments,
- broken reliquaries repaired with wire and plating,
- improvised offering crates,
- scavenged sermon boxes,
- soot-blackened icon panels,
- saint posters over industrial signage,
- floor-scratched prayer circles,
- shrine alcoves built around broken utility machinery,
- and emergency-lamp devotional corners.

Established the cult ambiguity boundary: poor or damaged devotional spaces may imply doctrinal drift or local reinterpretation, but must not automatically become hostile cult markers unless the zone generator explicitly assigns cult control.

Added placement doctrine for underhive shrines near sleeping areas, food queues, water access, medicae scarcity points, utility failures, alley alcoves, sewer access points, gang boundaries, and marginal gathering spaces.

Added underhive shrine standards to preserve traversal, hazard visibility, hostile actor readability, overlay compatibility, and asset-family reuse.

## 0.9.10ao — Plaza Religious Integration Bucket Initiated

Advanced Phase 2 bucket development from modular chapel generation rules into plaza-facing religious and memorial integration.

Defined the plaza religious integration objective: major district plazas should support religious, memorial, and faction identity anchors without requiring bespoke cathedral construction or compromising public-space utility.

Added plaza anchor categories including:
- minor public shrines,
- major public shrines,
- martyr memorial walls,
- aquila floor medallions,
- sermon plinths,
- candle-bank corners,
- reliquary kiosks,
- offering chests,
- pilgrim waiting areas,
- military oath shrines,
- manufactorum sanctification stations,
- noble processional shrines,
- and underhive improvised public shrines.

Established placement doctrine prioritizing plaza edges, wall-facing recesses, alcove mouths, and faction-control identity zones while avoiding roads, vehicle approach cells, door clusters, market grids, transit nodes, and critical crowd-path corridors.

Added district-specific expression rules for underhive, hab, industrial, military, and noble plaza religious anchors.

Added plaza semantic anchor standards to the standards document, reinforcing that public-space anchors must improve identity and navigation without damaging traversal, vehicle access, market use, or emergency movement.

Confirmed continued Phase 2 restriction: plaza religious anchors remain environmental semantics only and do not activate prayer loops, blessing systems, standing repair, or religion economy behavior.

## 0.9.10an — Chapel Generation Rule Pass Initiated

Continued the modular chapel stamp bucket into formal generation-rule definition and interoperability planning.

Defined required metadata expectations for chapel-related environmental stamps including:
- footprint dimensions,
- walkable path width preservation,
- doorway anchors,
- wall anchor positions,
- lighting nodes,
- degradation support,
- district compatibility,
- faction compatibility,
- and traversal reservation expectations.

Established traversal-preservation doctrine preventing religious semantic clutter from:
- blocking combat flow,
- creating unreachable interaction tiles,
- creating dead-end spam,
- obstructing evacuation routes,
- or producing soft-lock room layouts.

Formalized chapel layering doctrine using:
1. floor grammar,
2. wall grammar,
3. devotional anchors,
4. lighting passes,
5. degradation overlays,
6. district overlays,
7. faction overlays,
8. and hazard compatibility overlays.

Added formal Phase 2 environmental stamp standards reinforcing readability-first generation, modular semantic layering, fixture reuse, and runtime-conscious environmental composition.

Confirmed continued deferral of:
- active congregation simulation,
- live prayer systems,
- faction prestige logic,
- memorial gameplay,
- pilgrimage mechanics,
- and Ecclesiarchy event systems.

## 0.9.10am — Phase 2 Modular Chapel Stamp Bucket Initiated

Advanced Phase 2 from the initial Ecclesiarchy fixture-family declaration into the next finite bucket: modular chapel stamp integration.

Defined the modular chapel stamp system as a reusable environmental grammar layer rather than a one-off room authoring pass. The bucket now prioritizes predictable shrine and chapel footprints, fixture anchors, walkable-cell preservation, district compatibility tags, lighting tags, degradation variants, and future-safe faction compatibility.

Added required stamp classes including:
- 3x3 shrine nooks,
- 4x4 small chapels,
- hallway shrine alcoves,
- alley shrines,
- hab-block prayer corners,
- barracks chapels,
- manufactorum forge-shrines,
- reliquary side rooms,
- candle chambers,
- sermon rooms,
- nave segments,
- sanctuary end-caps,
- chapel storage,
- pilgrim rest corners,
- confession partitions,
- and large temple connector segments.

Reinforced the generation safety rule that shrine and chapel stamps must not create sealed rooms, unreachable loot, blocked doors, dead corridors, or obstructed evacuation paths unless alternate traversal is explicitly reserved.

Confirmed that active religion simulation, prayer mechanics, blessing systems, confession systems, pilgrimage behavior, faith economy, Ecclesiarchy quest generation, and priest AI remain deferred beyond this Phase 2 environmental-semantic bucket.

## 0.9.10al — Phase 2 Ecclesiarchy Environmental Semantic Expansion Initiated

Established the next governed Phase 2 bucket sequence focused on Shrine / Ecclesiarchy environmental semantic expansion. Formalized the transition away from isolated placeholder prop generation toward reusable modular environmental grammar supporting district identity, faction readability, navigational memory, and scalable room semantics.

Added active bucket sequencing for:
- Ecclesiarchy core fixture atlas expansion,
- modular chapel stamp systems,
- plaza religious integration,
- improvised underhive shrine variants,
- cathedral elevation staging,
- lighting compatibility verification,
- hazard compatibility verification,
- runtime density validation,
- atlas redundancy cleanup,
- and generation distribution rebalance.

Reinforced environmental readability protection rules to prevent gothic ornamental saturation from compromising gameplay visibility, pathfinding clarity, hazard readability, or interaction recognition.

Confirmed continuing Phase 2 governance restrictions preventing premature expansion into:
- religion simulation,
- blessing systems,
- confession mechanics,
- pilgrimage systems,
- ritual runtime loops,
- Ecclesiarchy economy systems,
- and live priest AI behavior trees.


## 0.9.10ak — Phase 2 External Artpack Merge and Core Runtime Slimming

Merged the newly integrated Asset Pack 6 material into the provided external artpack structures. Generated Standard 64, Intermediate 128, and High Native artpack ZIPs containing Asset Pack 6 cells and semantic icons in the expected `rebase_0_9_06d/tiles/quality/<tier>/` paths. The High Native artpack also carries the original Asset Pack 6 source sheets under `rebase_0_9_06d/source/asset_pack_6/Entities-features/` so native material is preserved outside the core runtime archive.

Slimmed the core package by removing embedded source/high-artpack ZIP payloads from `assets/artpacks/`. The core runtime now keeps only the LOW 32 Asset Pack 6 cells, semantic icons, operational slicing index, and import tool needed for runtime/default asset availability. Updated the artpack README, master plan, standards, governance, briefing, and runtime build label to enforce the external-artpack boundary.

## 0.9.10aj — Phase 2 Asset Pack 6 Intake and Hab / Domestic Fixture Bucket Promotion

- Ingested Asset Pack 6 through `tools/import_asset_pack_6.py` and `assets/indexes/asset_pack_6_slicing_index.json`, which is consumed by the import/packaging tool rather than acting as prose documentation.
- Sliced five 5x5 sheets into 125 LOW 32 runtime cells and 125 semantic icons for domestic fixtures, generic items, weapons, newspaper, clothing, and armor candidates.
- Kept only LOW 32 downscales in core runtime art and initially packed the high-resolution source sheets separately; 0.9.10ak later moved that native/source material into the external High Native artpack so the core runtime carries no embedded source-artpack ZIP payload.
- Added `DomesticHabFixtureAuthority` and promoted hab cot, Guard cot, worn bed, noble bed, bunk bed, domestic water storage, refrigerator, sink, stove, prep counter, storage cabinet, plank table, round table, mess table, and ornate table as passive domestic fixtures.
- Updated asset canonicalization, room fixture placement, passive inspection behavior, fixture registry entries, infrastructure-promotion metadata, and semantic art routing for the domestic bucket.
- Did not add housing ownership, rent, tenancy, sleep overhaul, family simulation, real weapon/armor equipment behavior, or item economy expansion.

# The Mechanist — Development History

This file records completed work only. Planning belongs in the master development plan. Rules belong in standards and practices. High-level doctrine belongs in governance.

## 0.9.10ai — Phase 2 Food / Farm / Bio Production Fixture Bucket Promotion

- Continued ordered Phase 2 asset promotion with the food / farm / bio production bucket.
- Added `FoodBioProductionFixtureAuthority` to own canonical algae tank, hydroponics bed, animal pen, cloning vat, fungal grow tray, refrigerated food store, and nutrient vat variants.
- Added semantic art routing for `feature_algae_tank`, `feature_hydroponics_bed`, `feature_animal_pen`, `feature_cloning_vat`, and `feature_refrigerator`, with fungal trays and nutrient vats mapped to the closest existing low-resolution semantic art rather than creating duplicate indexes or runtime scan tables.
- Expanded `AssetIntegrationDisciplineAuthority` with canonical food/bio handles and legacy aliases for older algae, hydroponic, animal-pen, cloning-vat, fungal, refrigerator, and nutrient-vat names.
- Updated `RoomFixtureInteractionAuthority` so kitchens, cafeterias, mess halls, food stores, hydroponics rooms, greenhouses, gardens, algae rooms, fungal rooms, animal pens, cloning/bio-vat spaces, refrigerators, freezers, and nutrient-galley contexts can place bounded food/bio variants.
- Added passive inspection, Survival XP feedback, cooldown/sound behavior, fixture-registry entries, infrastructure-promotion links, and shared operation target metadata.
- Preserved Phase 2 boundaries: no full faction food economy, livestock simulation, hydroponics growth loop, spoilage model, staffing shifts, provenance expansion, noble orchard economy, ration plant, or large industrial factory-stamp implementation was added.
- Verification: `javac --release 17` PASS; `TheMechanist.jar` rebuilt as a classes-only jar; class major version 61 confirmed; source leakage audit passed for established blocked planning/status terms; ZIP integrity passed. GUI smoke was not run because the container has no display server.

## 0.9.10ah — Phase 2 Road Frontage / Transit / Parking / Vehicles Closure

- Continued ordered Phase 2 asset promotion with the road frontage / transit / parking / vehicles closure bucket.
- Added `RoadTransitFixtureAuthority` to centralize road alcove, park open space, taxi booth, parking marker, legacy vehicle staging marker, parked civilian car, cargo truck, utility bike, armored car, and tank metadata.
- Added `RoadTransitFixtureInteractionAuthority` so road/transit/parked-vehicle objects have consistent passive inspection, Navigation XP, cooldown, and sound behavior.
- Updated road-grid plaza vehicle markers and road-adjacent parking-lot vehicle hooks to use centralized road-transit vehicle profiles rather than local arrays of labels and stock strings.
- Added semantic art routing for staged/parked road vehicle profiles and preserved legacy staging aliases through `AssetIntegrationDisciplineAuthority`.
- Expanded `FixtureInteractionRegistry` and `InfrastructurePromotionRegistry` with road-transit definitions and service-preview-only handoff metadata for taxi booths, parking markers, and parked vehicle profiles.
- Preserved Phase 2 boundaries: no real vehicle entities, ownership, fuel, cargo storage, boarding, recall, fare execution, traffic simulation, faction vehicle use, vehicle damage, or vehicle combat were implemented.
- Verification: `javac --release 17` PASS; `TheMechanist.jar` rebuilt as a classes-only jar; class major version 61 confirmed; source leakage audit passed for established blocked planning/status terms; ZIP integrity passed.

## 0.9.10ag — Phase 2 Astra Militarum / PDF Defense-Art Reconciliation

Continued ordered Master Development Plan Phase 2 with the next cataloged bucket: Astra Militarum / PDF defense-art reconciliation. Added `GuardPdfDefenseFixtureAuthority` as the compact owning table for PDF wall panel, wall corner, gate, damaged wall, turret Mk I, turret Mk II, turret Mk III, sandbag barricade, sandbag corner, Guard barracks anchor, Guard watch post, and Guard supply post variants. The authority owns canonical handles, semantic art keys, glyphs, labels, room stock metadata, Guard-billet selection rules, inspection text, interaction verbs, and passive defense-preview language.

Expanded `AssetIntegrationDisciplineAuthority` with canonical Guard/PDF fixture handles and legacy aliases for older one-off PDF, Guard, and Astra Militarum defense names. Updated `RoomFixtureInteractionAuthority` so Imperial Guard billet and related military/field-defense room contexts can choose bounded Guard/PDF variants instead of collapsing to generic civic or defense surfaces. Updated `FixtureInteractionRegistry` with a Guard/PDF defense family, and updated `InfrastructurePromotionRegistry` so Guard/PDF variants link to existing reinforced wall, reinforced door, sandbag line, heavy stub turret, powered defense turret, Guard barracks, watch post, and supply post build profiles as preview metadata rather than a new defense economy.

Updated `DefenseSemanticIntegration` with narrow passive archetypes for PDF wall panels, gates, sandbag barricades, and turret marks. This keeps defense-art reconciliation inside existing passive defense/construction authority and explicitly defers live turret targeting, ammo/power/staffing defense upkeep, patrol AI, battlefield simulation, quartermaster economy, and military logistics to later master-plan phases.

Verification: Java 17 compilation passed, `TheMechanist.jar` was rebuilt as a classes-only jar, source-planning leakage audit passed for the established blocked terms, `docs/` still contains exactly the four allowed files, and zip integrity passed. A GUI smoke test was not run because the execution container has no display server.

## 0.9.10af — Phase 2 Noble Estate Security Asset Bucket Promotion

Continued ordered Master Development Plan Phase 2 with the next cataloged bucket: noble estate security assets. Added `NobleEstateSecurityFixtureAuthority` as the compact owning table for noble wall panel, estate gate, corner tower, gilded sentry turret, shield relay, void-shield dome, laser pylon, energy fence, and noble security panel variants. The authority owns canonical handles, semantic art keys, glyphs, labels, room stock metadata, estate-room selection rules, inspection text, interaction verbs, and private-security-preview language.

Expanded `AssetIntegrationDisciplineAuthority` with canonical noble estate security handles and legacy aliases for older one-off noble defense names. Updated `RoomFixtureInteractionAuthority` so noble mansion and noble service-spine rooms can choose bounded estate security variants instead of collapsing to generic civic/medicae fixtures. Updated `FixtureInteractionRegistry` with a noble estate security family, and updated `InfrastructurePromotionRegistry` so noble security variants link to existing reinforced wall, reinforced door, watch post, gilded sentry turret, shield relay, powered defense, security sensor, and security cogitator build profiles as preview metadata rather than a new defense economy.

This pass did not add active trap combat, live turret targeting, burglary resolution, shield-field simulation, alarm escalation, noble legal response, or a full estate-defense economy. Those remain later-phase systems.

Verification: `javac --release 17` PASS. Jar rebuilt. `docs/` still contains exactly four files. Source-planning leakage audit passed for the established blocked terms. ZIP integrity passed. GUI smoke was not run in the container because no display server is available.

## 0.9.10ae — Phase 2 Arbites Precinct Asset Bucket Promotion

Continued ordered Master Development Plan Phase 2 with the next cataloged bucket: Arbites precinct assets. Added `ArbitesPrecinctFixtureAuthority` as the compact owning table for Arbites command desk, sergeant desk, precinct recaf maker, weapon locker, perp bench, interrogation table, holding cell fixture, precinct access door, precinct sign, alarm panel, and evidence locker variants. The authority owns canonical handles, semantic art keys, glyphs, labels, room stock metadata, precinct-room selection rules, inspection text, interaction verbs, and custody/security-preview language.

Expanded `AssetIntegrationDisciplineAuthority` with canonical Arbites precinct handles and legacy aliases for older one-off precinct fixture names. Updated `RoomFixtureInteractionAuthority` so Arbites rooms now choose bounded precinct variants instead of falling back to generic civic fixtures. Updated `FixtureInteractionRegistry` with an Arbites precinct/security family, and updated `InfrastructurePromotionRegistry` so precinct variants link to the existing `Precinct Defensive Fixture Set` build profile as service-preview/security metadata rather than a new build economy.

This pass did not add active enforcement AI, arrest procedures, prisoner simulation, evidence economy, live alarm escalation, weapon access, or combat behavior. Those remain later-phase systems.

Verification: `javac --release 17` PASS. Jar rebuilt. `docs/` still contains exactly four files. Source-planning leakage audit passed for the established blocked terms. ZIP integrity passed. GUI smoke was not run in the container because no display server is available.

## 0.9.10ad — Phase 2 Bar / Market / Social Fixture Bucket Promotion

Continued ordered Master Development Plan Phase 2 with the bar / market / social fixtures bucket. Logged the requested later-stage industrial room-stamp concern in the master plan: once machinery roles become economy-bearing, later room/stamp and industrialization phases should support faction factory stamps, repeated machine clusters for throughput, and compact omni-factory stamps with compatible machine sets, instead of scattering single mismatched machines into arbitrary rooms.

Added `BarMarketSocialFixtureAuthority` as the compact owner for faction bar interior, long bar counter, bar booth, bar stool cluster, bottle shelf, service keg, and market counter variants. Added canonical runtime handles, legacy aliases, semantic art routing, room labels, stock metadata, inspection text, interaction verbs, frontage label/stock support, and service-preview handoff language without adding a full social economy, alcohol/food economy, representative job system, or market ownership simulation.

Updated `RoomFixtureInteractionAuthority`, `FrontageFixtureInteractionAuthority`, `RoadFrontageFixtureAuthority`, `FixtureInteractionRegistry`, `InfrastructurePromotionRegistry`, and `AssetIntegrationDisciplineAuthority` so promoted social fixtures use centralized definitions, bounded room/frontage placement, registry cooldowns/sounds, infrastructure feedback, and existing shop/business-add-on handoff metadata. Updated the master plan, standards, governance, history, README build label, and new-conversation briefing. No standalone development document or asset index was created.

Verification: `javac --release 17` PASS. Jar rebuilt. `docs/` still contains exactly four files. Source-planning leakage audit passed for active blocked terms. ZIP integrity passed. GUI smoke was not run in the container because no display server is available.

## 0.9.10ac — Phase 2 Industrial / Forge Machinery Bucket Promotion

Continued ordered Phase 2 with the industrial / forge machinery bucket. Added `IndustrialForgeFixtureAuthority` so workshop and utility-machine placement no longer collapses into one generic forge-room object. Canonical promoted variants now include the scrap workbench fixture, EMM micro forge fixture, EMM atmospheric condenser fixture, and machine maintenance rack fixture.

Expanded `AssetIntegrationDisciplineAuthority` with canonical industrial / forge handles, legacy aliases for old forge-machine names, and semantic art routing for `build_scrap_workbench`, `build_emm_micro_forge`, `build_emm_atmospheric_condenser`, and `build_business_addon_fixture`. Updated room fixture generation so workshop, workbench, forge, machine, component warehouse, maintenance, condenser, reclamation, and pipe-room contexts select bounded industrial variants instead of the single generic forge handle.

Updated room interaction so industrial / forge fixtures use variant-specific inspection text, cooldown/sound behavior, XP feedback, and infrastructure-promotion feedback through the existing room fixture path. Updated the shared fixture registry, infrastructure promotion registry, and operation target registry so industrial variants have readable registry definitions, build-recipe promotion links, and shared operation target metadata without creating a separate manufacturing system.

Updated the master plan bucket ledger, standards baseline, governance reference pattern, README build label, and new-conversation briefing. No new standalone documentation was created. No new production economy, power/fuel accounting expansion, machine maintenance loop, utility network, autonomous labor, or manufacturing queue expansion was implemented in this Phase 2 bucket.

Verification: `javac --release 17` PASS. Jar rebuilt. `docs/` still contains exactly four files. Source-planning leakage audit found no `Phase [0-9]`, `TODO`, `FIXME`, `coming soon`, `future use`, `roadmap reference`, `next work`, `next target`, or `placeholder output` text in Java source. ZIP integrity performed for the delivered archive. GUI smoke was not run in the container because no display server is available.

## 0.9.10ab — Phase 2 Lab / Chemical Fixture Bucket Promotion

Continued ordered Phase 2 with the lab / chemical fixture bucket. Added `LabChemicalFixtureAuthority` so the lab family no longer hides multiple distinct build surfaces behind one generic laboratory fixture. Canonical promoted variants now include the micro laboratory fixture, crude chem bench, reagent preparation bench, distillation column, fume hood, and injector filling station.

Expanded `AssetIntegrationDisciplineAuthority` with canonical lab / chemical handles, legacy aliases for old fixture names, and semantic art routing for `build_emm_micro_lab`, `build_crude_chem_bench`, `build_reagent_preparation_bench`, `build_distillation_column`, `build_fume_hood`, and `build_injector_filling_station`. Updated room fixture generation so laboratory, chemical, reagent, toxin, solvent, fume, distillation, ampoule, and injector contexts select bounded lab variants instead of the single generic lab handle.

Updated room interaction so lab / chemical fixtures use variant-specific inspection text, cooldown/sound behavior, XP feedback, and infrastructure-promotion feedback through the existing room fixture path. Updated the shared fixture registry, infrastructure promotion registry, and operation target registry so lab variants have readable registry definitions, build-recipe promotion links, and shared operation target metadata without creating a separate chemistry economy.

Updated the master plan bucket ledger, standards baseline, governance reference pattern, README build label, and new-conversation briefing. No new standalone documentation was created. No full chemistry recipes, staffed production expansion, contamination simulation, contraband legality system, or medical/drug economy was implemented in this Phase 2 bucket.

Verification: `javac --release 17` PASS. Jar rebuilt. `docs/` still contains exactly four files. Source-planning leakage audit found no `Phase [0-9]`, `TODO`, `FIXME`, `coming soon`, `future use`, `roadmap reference`, `next work`, `next target`, or `placeholder output` text in Java source. ZIP integrity performed for the delivered archive. GUI smoke was not run in the container because no display server is available.

## 0.9.10aa — Phase 2 Medicae Fixture Bucket Promotion

Continued ordered Master Development Plan Phase 2 with the next cataloged bucket: medicae fixtures. Added `MedicaeFixtureAuthority` as the compact owning table for medicae frontage, generic medicae room fixture, clinic treatment stall, backroom medicae stall, and sterile medicae clean bench. The authority owns variant handles, labels, stock metadata, semantic art keys, inspection text, interaction verbs, and service-preview language so the bucket does not scatter medicae-specific strings through room generation, frontage generation, and panel interaction code.

Expanded `AssetIntegrationDisciplineAuthority` with canonical medicae variant handles and legacy aliases for old clinic/backroom/sterile bench names. Updated room fixture placement so clinic, wound room, aid station, noble surgery, sump patchwork, and backroom clinic contexts choose bounded medicae variants while keeping actual healing/treatment operations outside Phase 2. Updated frontage interaction, room interaction, fixture registry, and infrastructure promotion so medicae variants have readable inspection behavior, registry cooldowns/sounds, semantic art routing, and build/infrastructure handoff metadata.

Verification: `javac --release 17` PASS. Jar rebuilt. `docs/` still contains exactly four files. Source leakage audit found no `Phase [0-9]`, `TODO`, `FIXME`, `coming soon`, `future use`, `roadmap reference`, `next work`, `next target`, or `placeholder output` text in Java source. GUI smoke was not run in the container because no display server is available.

## 0.9.10z-doc1 — Phase 2 Continuation Reporting Standard

Added a durable continuation-reporting rule to `STANDARDS_AND_PRACTICES.md`: every active development pass must explain what is being done and what needs to be done next, anchored to the ordered master development phase, gate, or bucket. This rule is meant to prevent scattered phase drift, vague progress claims, and late-phase feature jumps during Phase 2 bucket work.

Updated `NEW_DEVELOPMENT_CONVERSATION_BRIEFING.md` with the same continuation requirement so a resumed conversation preserves the cadence: current phase, selected bucket/gate, completed work, unchanged or explicitly deferred work, verification performed, and next valid master-plan target. No gameplay source, runtime behavior, asset routing, README content, or master-plan bucket sequencing was changed in this documentation-only pass.

Verification: `docs/` still contains exactly four files. No new standalone development document was created.

## 0.9.10z — Phase 2 Public Service / Vending / Media Bucket Promotion

Continued ordered Master Development Plan Phase 2 with the next cataloged bucket: public service / vending / media fixtures. Added `PublicServiceMediaAuthority` as the compact owning table for public bench, pict screen, cheap radio, information kiosk, public-service counter, and the five existing vending-machine symbols. This keeps Phase 2 efficient by centralizing labels, stock text, vending costs, legal/hacked cooldowns, hacked-cycle limits, hack difficulty, and dispensed stock instead of scattering duplicate switch statements through `GamePanel` and frontage generation.

Updated road-frontage placement so public-service/media fixtures receive labels and stock-state metadata from the shared authority. Updated frontage interactions so bench, kiosk, and public-service counter text come from the shared authority, and registry cooldowns are used consistently for the promoted family. Vending behavior still uses the existing selected interaction path and save keys, but names, previews, costs, stock text, and item selection now route through the new authority. No autonomous service economy, civic permit simulation, or broader media system was added.

Removed surviving player-facing implementation-status language in the touched source surface, including stale `Future:` text in shrine, clinic, look-panel, and disguise-panel strings. Updated the master plan, standards, and governance to record the public-service/media bucket and the efficiency rule that repeated bucket definitions must centralize in one authority rather than multiplying parallel string/switch tables.

Verification: `javac --release 17` PASS. Jar rebuilt. `docs/` still contains exactly four files. Source leakage audit for active source and user-facing root notes found no `Future:`, `future use`, `coming soon`, `placeholder output`, `roadmap reference`, `next work`, `next target`, `TODO`, `FIXME`, or `scaffold` text. GUI smoke was not run in the container because no display server is available.

## 0.9.10y — Phase 2 Waste / Newsprint / Scavenge Bucket Promotion

Continued the ordered master-plan Phase 2 asset-integration discipline after the 0.9.10x bucket catalog. Promoted the waste/newsprint/scavenge container bucket as the first bounded asset-family pass. Added `WasteNewsprintScavengeAuthority` to own the narrow family behavior for public refuse and newsprint search surfaces without creating a broader recycling economy, logistics system, or decomposition chain.

Expanded `AssetIntegrationDisciplineAuthority` with canonical handles for public waste receptacle, public small bin cluster, INN newspaper dispenser, and discarded newsprint source alongside the existing public trash bin. Legacy names such as `newspaper-vending`, `old-newspaper`, and old placeholder-style bin names now canonicalize through the single alias boundary. Semantic art routing now covers all five promoted handles using the existing `feature_public_trash_bin`, `feature_public_receptacle`, `feature_public_small_bin_cluster`, `feature_newspaper_vending`, and `feature_old_newspaper_source` assets.

Updated road-frontage generation so public refuse placement varies among trash bins, waste receptacles, and small bin clusters while preserving deterministic bounded counts. Updated the shared fixture registry and frontage interaction authority so the promoted refuse family has bounded search behavior, cooldowns, sound hooks, player feedback, XP, and item-instance provenance through the existing interaction path. Newspaper vending and discarded-newsprint recovery continue to use the established INN buy/read/recover path, now through canonical promoted handles and legacy-compatible lookup.

Verification: `javac --release 17` PASS. Jar rebuilt. Source-planning leakage audit found no `Phase [0-9]`, `TODO`, `FIXME`, `coming soon`, `future use`, `roadmap reference`, `next work`, `next target`, or `placeholder output` text in Java source. `docs/` still contains exactly four files. GUI smoke was not run in the container because no display server is available.

## 0.9.10x — Phase 2 Bucket Catalog / Asset Promotion Ledger

Resumed ordered Phase 2 after the Phase 1 exit gate by cataloging what Phase 2 has actually completed and what remains. This was a documentation/sequence-control pass, not a new gameplay feature pass. The master development plan now contains the active Phase 2 bucket ledger for waste/newsprint/scavenge containers, public service/media fixtures, roads/vehicles, room service fixtures, medicae, lab/chemical, forge/industrial, bar/social, defenses, Arbites precinct assets, noble estate security, Astra Militarum/PDF defenses, food/farm/bio production, hab/domestic fixtures, shrine/Ecclesiarchy fixtures, and portrait/population art.

The catalog distinguishes art that merely exists from assets that are actually promoted through semantic handles, runtime definitions, inspection/player feedback, placement/generation rules, and interaction/operation targets. It records the recommended next Phase 2 bucket as waste/newsprint/scavenge containers and reinforces that later-phase logistics, autonomous vehicles, active turret combat, full food economy, and social/service systems remain out of scope for Phase 2 unless they are only being prepared through asset promotion surfaces.

Updated the master plan, standards, governance, README build label, and runtime build label without adding standalone documents. Verification: `docs/` contains exactly four files. `javac --release 17` PASS. Jar rebuilt. ZIP integrity performed for the delivered archive. GUI smoke was not run in the container because no display server is available.

## 0.9.10w — Phase 1 Framework Stabilization / Extraction Gate IV

Completed the Phase 1 exit-verification gate and marked Phase 2 asset integration discipline unblocked. This was a verification and boundary-cleanup pass, not a new gameplay feature pass.

Audited source for planning leakage and remaining implementation-status language. Removed surviving player-facing/source wording such as scaffold/deferred-feature phrasing from logistics, staffing, machine-operation, fixture, win-condition, character-finalization, actor-standard, body/death, and atlas text. Renamed `LogisticsReservationScaffoldAuthority` to `LogisticsReservationForecastAuthority` so the live source name describes the current runtime role rather than a development stage. Renamed the world-atlas backing array from `scaffold` to `slices` and updated references accordingly.

Verified the source audit target: no Java source occurrence remains for `Phase [0-9]`, `TODO`, `FIXME`, `coming soon`, `future use`, `placeholder output`, `roadmap reference`, `next work`, or `next target`. The only remaining `placeholder` source occurrences are legacy compatibility aliases in `AssetIntegrationDisciplineAuthority`, where old save/object handles canonicalize to semantic runtime names.

Updated the master development plan to record Phase 1 as complete enough to proceed and set the current directed phase to Phase 2. Updated standards and governance so the Phase 1 exit gate is explicit: later phases may resume, but touched systems still must honor ownership boundaries, documentation containment, and no-planning-in-source rules.

Verification: `docs/` contains exactly four files. `README.md` remains user-facing. `javac --release 17` PASS after the audit edits. Jar rebuilt. Class major version verified as 61. ZIP integrity performed for the delivered archive. GUI smoke was not run in the container because no display server is available.

## 0.9.10v — Phase 1 Framework Stabilization / Extraction Gate III

Completed the ordered Phase 1 persistence/options/UI boundary hardening pass without adding gameplay behavior. Save-slot path, label, and summary handling now routes through `SaveSlotSurfaceApi` rather than carrying save-display construction directly inside the panel. Selected tile and selected coordinate lookup now route through `SelectedContextSurfaceApi`, giving the look/player selected context a named boundary. Option mutation now routes through `OptionsBoundaryAuthority`, and Swing window/resolution application now routes through `WindowModeSurfaceAuthority`.

This pass kept audio owned by `SoundManager`/`DynamicMusicManager`, dirty-region tracking owned by `DirtyRegionTracker`, and UniversalWindow behavior owned by `UniversalWindowAuthority`; it did not move those stable owners back into panel-local helper code. `TheMechanist.java` was reduced from 13,821 lines to 13,752 lines while preserving the existing surface methods as thin bridges where UI event wiring still expects them. Phase 1 remains active; the next ordered gate is Extraction Gate IV: phase-exit verification. Phase 2 asset promotion remains paused until that exit gate is recorded.

Verification: `javac --release 17` PASS after extraction. Jar rebuilt. ZIP integrity performed for the delivered archive. GUI smoke was not run in the container because no display server is available.

## 0.9.10u — Phase 1 Framework Stabilization / Extraction Gate II

Completed the Phase 1 GamePanel surface audit pass without adding gameplay behavior. Audited the remaining central `GamePanel` surface as the launcher/panel coordinator and extracted low-risk repeated helpers into semantic APIs: text wrapping and truncation moved to `TextSurfaceApi`, time labels moved to `TimeSurfaceApi`, map-layer floor/sewer/index/label/icon helpers moved to `MapLayerSurfaceAuthority`, item-name equivalence moved to `ItemQuality.namesMatch`, and container token normalization moved to `ContainerIdentityApi`.

The pass reduced `TheMechanist.java` from 13,894 lines to 13,821 lines and reduced the direct method surface from 835 to 825 methods while keeping behavior unchanged. Phase 1 remains active; the next ordered gate is Extraction Gate III: persistence/options/UI boundary hardening. Phase 2 asset promotion remains paused until the Phase 1 exit gate is recorded.

Verification: `javac --release 17` PASS after extraction. Jar rebuilt. ZIP integrity performed for the delivered archive. GUI smoke was not run in the container because no display server is available.

## 0.9.10t — Phase 1 Framework Stabilization / Extraction Gate I

Returned development to the ordered master-plan sequence after the Phase 2 correction pass exposed that Phase 1 had not been formally closed. This pass did not add gameplay behavior. It established Phase 1 as a four-step gate in the master development plan and completed the first extraction gate.

Moved the remaining package-private runtime/data/authority classes out of `TheMechanist.java` into semantic framework files while preserving package-private access and behavior. New or clarified framework files include `EnvironmentSensesFramework.java`, `UiRuntimeSupportFramework.java`, `KnowledgeQualityFramework.java`, `ProductionAuthorityFramework.java`, `PopulationPersonnelFramework.java`, `ContainerTradeFramework.java`, `FactionServicesFramework.java`, `CombatRuntimeFramework.java`, `PersistenceFramework.java`, `GameOptionsFramework.java`, and `AudioRuntimeFramework.java`. The previous mixed combat/persistence/options/audio conglomerate was split into separate semantic files.

Reduced `TheMechanist.java` from 21,301 lines to 13,894 lines. The remaining central file now carries the launcher and main `GamePanel` surface rather than every top-level support class. Updated runtime versioning to 0.9.10t, revised standards and governance so phase order is treated as an ordered gate sequence, and recorded that Phase 2 remains paused until Phase 1 exits cleanly.

Verification: `javac --release 17` PASS after extraction. Jar rebuilt. ZIP integrity performed for the delivered archive. GUI smoke was not run in the container because no display server is available.

## 0.9.10r — Phase 1 Framework Stabilization / Code Dissection I

Began the master-plan **Phase 1 — Framework stabilization and code dissection** pass. This pass did not add a new gameplay feature. It reduced the oversized central Java surface while preserving behavior and save/load expectations.

Extracted major cohesive top-level systems out of `TheMechanist.java` into semantic framework files: `WorldSimulationFramework.java` for world setup, world definitions, generation progress, naming/history/facility/production/provenance ledgers, and campaign-world persistence; `WorldRuntimeGenerationFramework.java` for world atlas, active world state, hivewall/interstitial infrastructure, and zone generation; `MediaRuntimeFramework.java` for portrait/tile art loading, art-pack resolution, audio-pack resolution, and image caching; `ItemEconomyFramework.java` for item instances, containers, provenance records, item catalog, item ledger, and catalog expansion; and `CharacterIdentityFramework.java` for character creation audit/authority, faction rank/roster profiles, candidates, and job profiles.

The central source file was reduced from roughly 2.23 MB in the prior archive to roughly 1.60 MB while retaining package-private access and avoiding behavior rewrites. Runtime world-generation progress labels were renamed from development-like phase vocabulary to runtime stage vocabulary, and player/runtime strings that referenced project file paths or implementation status were neutralized into in-world/current-state descriptions.

Updated the master development plan, standards, and governance to mark Phase 1 as the current directed stabilization phase and to reinforce mandatory source dissection when a touched subsystem has clear ownership. The next logistics gameplay target was queued after the stabilization cut: station-buffer inspection, reclaim, and machine-specific consumption tightening. That target was later deferred behind the master-plan Phase 2 asset-discipline correction.

Verification: `javac --release 17` PASS after extraction; final jar rebuild and ZIP integrity performed for the delivered archive. GUI smoke was not run in the container because no display server is available.

## 0.9.10q — Selected-Contract Manual Haul Execution

Completed the recovery phase after Phase 0 governance/build hygiene. Added a bounded manual haul execution authority for the latest selected haul contract only. Execution requires the existing logistics chain to be current: delivery intent, source token, route intent, haul contract, and matching preflight. Blocked contracts, cancelled/expired lifecycle records, broken links, missing source items, out-of-bounds anchors, and inaccessible handling tiles stop execution before any item moves.

Implemented `LogisticsManualHaulExecutionAuthority` with saveable recent execution records, status lines, compact summaries, and Infopedia lines. Added a Workbench `EXEC HAUL` action beside cancellation, plus a last-execution report in the Workbench logistics status. The execution path moves concrete item instances through existing container/provenance transfer functions: base storage inputs are picked up into carried inventory first, then delivered to a station input buffer; carried inputs are delivered directly from carried inventory to the station buffer. No global hauling loop, background pathfinding, autonomous labor economy, or hidden item teleportation was added.

Added machine input-buffer containers under the `machine.input.*` namespace and made the production input container authority count and consume station-buffer item instances before falling back to base storage and carried inventory. This prevents delivered inputs from becoming dead storage while keeping the implementation narrow and visible. Added actor-access labeling for machine input buffers and persisted haul execution history/sequence/last-report state through save/load.

Updated the master development plan to mark 0.9.10q complete and set the next gameplay target as station-buffer inspection, reclaim, and machine-specific consumption tightening. Updated standards to require logistics execution to use concrete item-instance transfers through visible source, carried pickup, and station-buffer delivery.

Build verification: Java 17 compilation passed. Jar rebuilt. GUI smoke test not run in the container because no display server is available.

## 0.9.10p — Phase 0 Governance / Documentation / Build Hygiene

Performed a Phase 0 hygiene pass focused on source-of-truth containment rather than new gameplay expansion. Renamed live phase-numbered authority classes to semantic names, including road-grid/frontage, fixture-interaction, infrastructure-promotion, and operation-target authorities. Removed remaining phase labels and planning/status prose from runtime summaries, source comments, logistics/status lines, fixture feedback, and world-generation scale references.

Deleted an unused ultra-hive planning reference and planning-line helper from source code; the long-range world/district goals remain in the master development plan instead of Java. Strengthened the master development plan, standards, and governance documents so planning belongs in the master plan, completed work belongs in history, rules belong in standards, and Java/README/asset readmes cannot become hidden planning stores.

Verification: `javac --release 17` PASS; `TheMechanist.jar` rebuilt; class major version 61 confirmed. ZIP integrity PASS after packaging.

## 0.9.10o — Planning Containment / Source Boundary Hardening

Performed a documentation and source-boundary correction after discovering phase-roadmap prose embedded in Java classes and operational readmes. Relocated the durable Phase 3–6 substance into `MASTER_DEVELOPMENT_PLAN.md`, strengthened the no-sprawl rule in `STANDARDS_AND_PRACTICES.md` and `MASTER_GOVERNANCE_REVISION_II.md`, and removed planning-only Java classes that were not runtime authorities.

Removed unconsumed generated index JSONs from the shipped `assets/indexes/` module so stale asset/sufficiency/unification indexes no longer ride along as parallel truth. Neutralized README and asset-folder notes so they describe runtime/package roles only rather than pass history or future work. Removed source methods such as `phasePlan()`, `vehicleFoundationPlan()`, and `nextPhasePlan()` where they duplicated master-plan content. Player-facing fixture feedback now describes current in-world function instead of advertising "future use" or "next work".

Runtime authorities retained: construction governance, defense semantic integration, phase road/frontage placement, Phase 4 fixture interaction registry, Phase 5 industrialization profiles, and machine operation queue. Planning-only audit/index authorities removed from the compiled source: art placeholder catalog, asset redistribution plan, asset unification utilization, base-art efficiency index, integration efficiency API, master semantic needs index, Phase 2 placeholder allocation catalog, long-horizon framework plan, defensive asset gap registry, and defense art base index.

## 0.9.10n Corrective Boundary Pass — README / Index Markdown Cleanup

- Reinforced the documentation containment rule in the master development plan, standards, and master governance revision.
- Rewrote README back into a user-facing launch/description file and removed changelog, phase, and asset-index commentary from it.
- Reduced the new-conversation briefing to a pointer to the four master documents instead of a duplicated rolling development log.
- Removed prose Markdown asset-index summaries from `assets/indexes/` and kept asset indexes as module data.
- Updated the base art efficiency maintenance script so it writes the machine-readable JSON/catalog data and Java compact catalog without regenerating a Markdown index summary.
- No gameplay execution authority was expanded. The next implementation target remains the selected-contract manual haul execution gate.

## 0.9.10n — True Documentation Consolidation / Phase Roadmap Recovery

- Rebuilt `docs/` to the four-document standard: master development plan, standards and practices, development history, and master governance revision.
- Folded the scattered pass notes, standards addenda, phase notes, construction/defense/staffing/logistics notes, and roadmap fragments into the active master plan, standards, and history.
- Removed the duplicate governance DOCX from `docs/`; the durable governance source is now Markdown.
- Moved generated JSON/index material out of `docs/` into `assets/indexes/` so asset/module data is no longer treated as governance documentation.
- Updated the immediate roadmap around selected-contract manual haul execution and restored the durable phase roadmap from placeholder integration through endgame systems.
- No gameplay execution authority was expanded by this documentation pass.

### Consolidation inventory note

The previous scattered pass notes, standards addenda, phase notes, and asset-index Markdown summaries were folded into the four durable documents or converted back to asset/index module data. The full file-by-file prose inventory was intentionally removed from active history because it duplicated old notes and created the same documentation sprawl the consolidation was meant to stop.

---

## 0.9.08y — Defensive Asset Gap Audit / Physical Defense Readiness

Added `DefensiveAssetGapRegistry`, a lightweight data-only authority that compares future physical defense needs against the current low_32 semantic art payload. The registry classifies existing and missing defense families including reinforced doors, barricades, watch posts, powered turrets, shield relays, security cogitator nodes, alarm traps, planted explosives, alarm panels, gates, sandbag/hard-cover lines, razor wire, sensors, crewed heavy weapons, and future vehicle weapon mounts. Added `docs/DEFENSIVE_ASSET_GAP_AUDIT_0.9.08y.md` and JSON companion. Updated master planning, standards, and continuation briefing to preserve the doctrine that map defenses should be visible in-world entities, not invisible abstract bonuses. Runtime/save version advanced to 0.9.08y. Verification target: Java 17 compile, JAR rebuild, ZIP integrity.

## 0.9.08s — UniversalWindow / UI Cohesion Foundation

Started the next framework consolidation phase with a lightweight `UniversalWindowAuthority`. Added canonical window families, lifecycle states, escape/back behavior categories, and shared capability metadata for inventory, machine operations, construction, dialogue, faction ledgers, map, infopedia, trade, options, save/load, future editor tools, future administrator tools, and future gameplay hints. Added `docs/UNIVERSAL_WINDOW_AUTHORITY_0.9.08s.md` and updated master planning and standards/practices.

Implementation remains deliberately efficient: the authority is data-first, auditable at startup/turn-one, and does not add threads, per-frame scanning, live rendering rewrites, network dependencies, or heavyweight UI observers. Runtime/save version advanced to 0.9.08s. Java compile and JAR rebuild completed.

## 0.9.08r — Long-Horizon Framework Planning / Queue Efficiency Refinement

Added the long-horizon framework goals requested for future multiplayer, administrator mode, headless server hosting, semi-real-time multiplayer-compatible turns, death/respawn/corpse fallback, internet connection technology, multi-sector simulation, dynamic sub-faction generation, non-intrusive gameplay hints, and a later doomed-cultist tutorial sector. Added `docs/LONG_HORIZON_FRAMEWORK_GOALS_0.9.08r.md` and updated the master plan and standards/practices doctrine.

Implementation remains deliberately lightweight: added `LongHorizonFrameworkPlan` as a data-only authority that records future lanes without starting network loops, server threads, tutorial scripts, or multi-sector runtime expansion. Refined `MachineOperationQueue` with bounded history and low-cost work counters so future queue diagnostics can happen without heavy scans. Runtime/save version advanced to 0.9.08r.

Verification target: `javac --release 17`, JAR rebuild, ZIP integrity.
## 0.9.08m — Phase 4 Interaction Expansion II / Room Fixture Use

- Added interior room fixture placement and interaction hooks for medicae, laboratory, forge, faction bar, and civic service fixtures.
- Kept the pass bounded: fixtures are readable and inspectable but are not yet full Phase 5 production/healing/economy systems.
- Added `RoomFixtureInteractionAuthority.java`.
- Runtime/debug version advanced to 0.9.08m.
- Core build policy remains low_32-only for runtime art.

## 0.9.08l — Phase 4 Interaction Expansion I / Frontage Fixture Use

Began Phase 4 by making selected road-frontage placeholders lightly interactive. Added `FrontageFixtureInteractionAuthority` for public trash bins, benches, pict/news screens, cheap radios, info kiosks, public-service counters, medicae frontage, and faction-bar frontage. Trash bins now support bounded scavenging with item provenance and cooldowns; broadcast fixtures produce INN-style reports and audio cues; benches/kiosks/counters/medicae/bar frontage provide readable player feedback and future-service scaffolding. Full vehicle, taxi, medicae treatment, construction recipes, and faction-specific service variants remain deferred to later phases. Core art remains low_32-only.

Verification target: `javac --release 17`, JAR rebuild, ZIP integrity.

## 0.9.08k — Phase 3 Room Integration III / Road Frontage Fixtures

Continued Phase 3 room integration by adding road-frontage civic fixtures to sidewalks and alley/cut-through cells. The new `RoadFrontageFixtureAuthority` pass places lightweight map-object placeholders for public trash bins, INN newspaper vending and newspaper recovery hooks, public benches, pict/news screens, cheap radios, information kiosks, public-service counters, medicae frontage markers, and faction-bar frontage markers. This keeps the road grid readable as civic space while preserving the low_32-only core build and deferring full searching, taxi, vehicle, recipe, medicae service, and faction-specific variants to later phases.

Verification target: `javac --release 17`, JAR rebuild, ZIP integrity.

## 0.9.08i — Phase 3 Room Integration I / Road Grid Foundation

Began Phase 3 by integrating a conservative road-grid foundation into zone generation. The new system creates four-wide underhive street corridors from the central plaza using sidewalk/lane/lane/sidewalk structure, preserves rooms and transition entities, and seeds harmless vehicle staging placeholders for later vehicle/entity passes. Core art containment remains low_32-only. Verification target: javac --release 17, JAR rebuild, ZIP integrity.

## 0.9.08h — Phase 2 Placeholder Allocation / Room Fixture Preparation

Implemented the second-half Phase 2 allocation pass. Indexed art and semantic needs are now connected through stable placeholder entity/fixture families for waste/newsprint, medicae, forge, chemical laboratory, public-service vending/media, roads/vehicles, bar/social, food/bio production, hab/domestic, and civic/shrine families. No broad live gameplay rewrite was performed; this pass prepares Phase 3 room integration while preserving the low_32 core-art containment standard.

## 0.9.08f — Core Art Containment / Low-32 Runtime Build Pass

Reduced the core game payload by restricting bundled runtime graphics to the canonical low_32 tier and leaving higher-resolution mirrors to external art packs. Runtime defaults now prefer low_32 when no art quality override is present. Title, background, and intro-crawl assets remain in core because they are directly referenced by the launcher/new-game flow.
# 0.9.08e — Asset Redistribution / Current Entity Art Allocation Plan

Added a metadata-only allocation bridge between the current art indexes and the current semantic needs index. This pass creates `docs/ASSET_REDISTRIBUTION_CURRENT_ENTITY_ALLOCATION_PLAN.md`, `.json`, and `AssetRedistributionAllocationPlan.java`. It identifies P0/P1/P2 fixture families, proposed entity names, room targets, behavior staging, recipe/decomposition staging, and future implementation passes. No mass live remap was performed; this is the plan that prevents art redistribution from becoming a destructive rewrite. Java 17 compile passed; JAR rebuilt; ZIP integrity passed.

# 0.9.08e — Asset Redistribution / Current Entity Art Allocation Plan

Connected Graphical Upgrade Base 3, the base low_32 art index, and the Master Semantic Needs Index into a staged allocation plan. Added `docs/ASSET_REDISTRIBUTION_CURRENT_ENTITY_ALLOCATION_PLAN.md`, `docs/ASSET_REDISTRIBUTION_CURRENT_ENTITY_ALLOCATION_PLAN.json`, and `AssetRedistributionAllocationPlan.java`. This is a metadata/planning pass: it assigns art candidates to semantic families, entity roles, current room-feature needs, and staged implementation waves without yet rewriting live gameplay. Runtime/save version updated to 0.9.08e; Java 17 compile passed; JAR rebuilt; ZIP integrity passed.

# 0.9.08d — Base Art Efficiency Index

Applied the art-efficiency indexing process to the older/core base art packet using only the canonical `low_32` tier as the discovery authority. Higher-resolution folders are treated as mirror outputs and were not separately scanned, preventing duplicate-resolution noise. The pass indexed 800 base low_32 cells outside Graphical Upgrade Base 3, identified 68 cells already bound through direct tile aliases, and created 732 named low_32 placeholder copies for available-but-unmapped base art.

Added `docs/BASE_ART_EFFICIENCY_INDEX.md`, `docs/BASE_ART_EFFICIENCY_INDEX.json`, and `BaseArtEfficiencyIndex.java`. The new catalog separates implemented alias art from unmapped candidate art, highlights priority implementation buckets for machinery, lab/chem equipment, shop fixtures, vending/public machines, portraits, floors, walls, doors, and corridors, and preserves current gameplay behavior. Runtime/save version updated to 0.9.08d; Java 17 compile passed; JAR rebuilt; ZIP integrity passed.

# 0.9.08b — Graphical Art Efficiency / Placeholder Asset Index

Implemented the first art-efficiency indexing pass over Graphical Upgrade Base 3. The pass scans the 300 deterministic LOW 32 cells, assigns stable descriptive placeholder keys, records the likely implementation kind for each cell, compares placeholder/semantic availability against current source references, and copies named placeholder assets into every supported quality tier under `semantic_placeholders/`.

This does not claim that all assets are wired into gameplay. It creates the named catalog required to later promote unused art into tiles, entities, room objects, medical facilities, industrial machinery, roads, vehicles, gardens, farms, bars, public-service fixtures, train/logistics pieces, and other content without losing track of what art already exists.

Verification: `javac --release 17` passed, jar rebuilt, zip integrity passed. GUI runtime smoke was not run in the headless container.

# 0.9.08a — Graphical Upgrade Base 3 / Road Vehicle Medical Asset Foundation

Implemented the Graphical Upgrade Base 3 asset ingestion pass. Imported twelve supplied 5x5 atlases, preserved their source sheets, sliced 300 cells into low_32, standard_64, intermediate_128, and high_native quality tiers, and added semantic bridge icons for existing build objects plus future vehicles, roads, bar fixtures, medicae, public-service, farm, park, and train objects.

Runtime behavior remains conservative: existing save glyphs and base objects continue to function, while selected old placeholders now resolve to better medical, table, storage, water, cot, and public-service art where available. New road and vehicle assets are staged for future entity/worldgen passes but are not yet full gameplay vehicles or road networks. `javac --release 17` passed and the JAR was rebuilt.

# 0.9.07v — Faction Continuity / Replacement Queue Integrity Pass

Inserted an integrity safeguard pass before continuing scheduler/optimization work. Protected continuity anchors are now explicitly non-targetable/non-destructible: level-transition tiles resist stray/explosive terrain damage, faction representatives are flagged as protected continuity anchors, and Ministorum head clerics continue to be protected. This protects fallback reputation repair, faction contact, and map traversal from accidental destruction.

Personnel replacement queues now prune stale reinforcement debt against current living faction actors and room-population ledger capacity before replacements mature. If a faction loses support rooms or population capacity, old queued reinforcements are discarded instead of endlessly trying to refill a no-longer-supported footprint. A one-person continuity floor remains so a faction can survive conceptually through protected public contacts and off-map officials. Runtime/save version updated to 0.9.07v.

Verification: `javac --release 17` PASS, JAR rebuilt, ZIP integrity PASS. GUI smoke not run in container because no X11 display is available.

# 0.9.07u — Dirty Region Cache / Local Simulation Optimization Pass

Implemented a conservative dirty-region cache foundation for local simulation efficiency. Added a `DirtyRegionTracker` with cache-family revision tracking for light, noise, vision, and hazard invalidation; local dirty-region marking helpers; light/noise rebuild counters; visibility local/full clear counters; and simulation-efficiency audit output that includes cache rebuild and dirty-region summaries. Visibility clearing now uses a local current/previous window plus dirty bounds where safe, falling back to a full clear for oversized regions. Light and noise rebuilds remain conservative but now have a shared invalidation authority for later partial rebuild work. Runtime/save version updated to 0.9.07u.

Verification: `javac --release 17` PASS, JAR rebuilt, ZIP integrity PASS. GUI smoke not run in container because no X11 display is available.

## 0.9.07t — Documentation Consolidation / Architecture Cleanup Pass

Performed a maintenance pass focused on recovery efficiency, documentation hygiene, and long-term roadmap clarity. Runtime/save version was bumped to 0.9.07t, but no gameplay behavior was intentionally changed.

Updated README/RUN/master/standards/history/new-conversation briefing so each document has a narrower job. Removed obsolete standalone handoff/pass notes whose substance was already represented in durable docs, reducing stale context and dead-space risk. Recorded the long-term in-game content editor goal for zone stamps, features, entities, factions, machinery, recipes, items, and tiles. Recorded the long-term localization/text-key goal for moving player-facing text into keyed language files.

Verification: `javac --release 17` PASS, JAR rebuilt, ZIP integrity PASS. GUI smoke not run in container because no X11 display is available. Runtime/save version updated to 0.9.07t.

## 0.9.07s — Ambient Soundscape / Distance Cue Foundation Pass

Expanded the generated core soundscape with additional tiny mono WAV fallback cues: factory press, servo movement, water drip, sludge burble, noble/administrative chime, far alarm, and door servo. Registered these cues in the sound manager.

Ambient cues are now zone-weighted rather than a small fixed random set: sewers favor pipe/vent/drip/sludge, noble and governor areas favor TV/radio/chime/servitor/door-servo, and industrial/mechanicus/factory areas favor machinery/spark/press/servo/alarm. Distant hearing pings can now trigger rate-limited distance-scaled audio cues that correspond to the text label, so offscreen movement, wet motion, machinery, impacts, vents, and reloads become audible as well as textual.

Verification: `javac --release 17` PASS, JAR rebuilt. GUI smoke not run in container because no X11 display is available. Runtime/save version updated to 0.9.07s.

## 0.9.07r — Movement Interpolation / Core Soundscape Foundation Pass

Added visual tile-slide interpolation for player and nearby NPC movement. The simulation remains grid-authoritative; movement records only affect rendering offsets for a short readable duration. This improves player feedback without introducing continuous physics or pathing complexity.

Added generated core WAV placeholders in `assets/sound/core/`: metal, grate, sludge, and debris footsteps plus distant machinery, radio/TV static, spark, pipe, and vent cues. Player movement now plays surface-aware footstep cues, nearby NPC movement can emit quiet movement cues, and the game periodically plays low-cost distant ambient cues while in active play.

Documentation was consolidated in line with the Documentation Efficiency Standard: README and RUN instructions were reduced toward current package/run truth, master development was reset around the current authority build, and the previous standalone simulation-efficiency pass note was removed after its substance was preserved in durable docs.

Verification: `javac --release 17` PASS, JAR rebuilt, ZIP integrity PASS. GUI smoke not run in container because no X11 display is available. Runtime/save version updated to 0.9.07r.

## 0.9.07q — Simulation Efficiency / Turn Budget + Strict Input Pacing Pass

Implemented the first conservative efficiency substrate for old i5-class targets. Keyboard movement now uses a strict world-action pacing gate: one active move may have only one pending held-key move behind it, key release clears the pending movement, blocked movement clears the pending movement, and run/sprint modes cannot create deep buffered sprint chains. Timer ticks now process the pending held movement only when the pacing interval allows it.

Added deferred-simulation debt counters and a bounded budget-spending scaffold. This does not yet run distant-zone simulation, dirty-region light/noise rebuilds, or provenance indexing; it provides the safe budget gate where those later systems can be attached. Added profiling counters and debug audit records for turn advancement, local NPC tick cost, deferred budget work, and input pacing state.

Verification: `javac --release 17` PASS, JAR rebuilt, ZIP integrity PASS. GUI smoke not run in container because no X11 display is available. Runtime/save version updated to 0.9.07q.

## 0.9.07g — Bank Alarm / Vault Guard / Lockbox Quest Refinement

Extended the bank-heist foundation into a first playable security event. Bank branch generation now attempts to place alarm panels and branch/vault guards in addition to bank terminals, vaults, and managers. Alarm panels can be tampered with through Intellect, Agility, stealth posture, data spikes, lockpicks, and tool bundles; success reduces local vault alarm response, while failure raises bank alarm pressure, suspicion, guard alert state, and authority inspection pressure. Vault stock state now records locked/open/looted/alarmed state more explicitly. Bank alarms alert local bank security guards, vault guards, branch guards, and bank managers, shifting them into alarm response states with appropriate weapons and ammunition. Bank manager schedules now cycle through vault desk, counting-script, vault-inspection, and lockbox-review states.

Added first-stage noble-bank lockbox retrieval jobs through bank terminals. These jobs request property such as sealed bank lockboxes, House gold ingots, stock certificate bundles, Noble Commerce Permits, or trade chits from vault loot and pay tracked Imperial Script when turned in. Heist actions continue to feed suspicion, authority pressure, and delayed INN/public-news hooks.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, ZIP integrity PASS. Runtime/save version updated to 0.9.07g.

## 0.9.07c — Renamed Portrait Artpack Rebuild / Core LOW 32 Packaging Refresh

- Rebuilt Standard 64, Intermediate 128, and High Native optional art packs from the renamed `Protraits.zip` portrait authority.
- Replaced old timestamp/ChatGPT portrait folders in optional art tiers with semantic categories matching the 0.9.07b LOW 32 core portrait pool.
- Confirmed the core keeps LOW 32 tile and portrait graphics, title/intro assets, and current weapon SFX while higher-resolution graphics remain optional overlays.
- Confirmed source MP3s and full-size portrait source sheets are not bundled in runtime packages.

Verification target: `javac --release 17` PASS; JAR rebuilt; optional art packs each contain 475 renamed portrait cells and no old timestamp portrait folders.

## 0.9.07b — Renamed Portrait Rebase / Animal and Servant Placement

- Removed the ambiguous timestamp-named LOW 32 imported portrait runtime folders and regenerated the portrait pool from the renamed `Protraits.zip` source authority.
- Added 475 LOW 32 runtime portrait cells in meaningful category folders and recorded the source mapping in `assets/art/portraits/RENAMED_PORTRAIT_SOURCE_INDEX.*`.
- Added/activated `AnimalPopulationApi` seeding for sewer/abandoned wild creatures, urban vermin, farm/food-room animals, kennel/security animals, faction/wealth-weighted pets, and noble household servants.
- Added animal metadata to NPC state: `creatureKind`, `animalProfileId`, and `companionOf`, with save/load persistence.
- Added animal-specific render treatment, look-panel text, simple pet following, animal ambient/hearing pings, and prevention of ordinary humanoid weapon/ammo loadout for animals.
- Preserved 0.9.07a rank overlays and neutral contest rooms.

Verification: javac --release 17 PASS; JAR rebuilt with Main-Class mechanist.TheMechanist; LOW 32 renamed portrait cells = 475; source timestamp portrait dirs removed.

## 0.9.07a — Leadership Rank Overlays / Neutral Contest Rooms

- Added rank-colored frames around NPC/entity tiles and small upper-right 16x16-style rank insignia for ranks 1-8.
- Added persistent NPC rank metadata: rank number, title, and scope, inferred from faction roster names and saved with NPC state.
- Added custom rank badge PNG reference assets under assets/art/ui/rank_badges while rendering matching vector badges for LOW32 reliability.
- Added 2-5 extra neutral contest rooms per generated zone when geometry allows, creating unoccupied room-control space for later faction capture/back-and-forth.
- Updated look-mode detail panel to display faction rank line before intent-read information.

## 0.9.06z — Arbites Custody Report / Patrol Inspection Cooldowns

Added a dedicated Arbites custody report screen after nonfatal capture. The report displays why the player was taken, severity, time passed, fine assessed/paid/unpaid, carried/banked/base cash changes, evidence confiscation count, stat damage, fatigue/sleep/pain/wound trauma, and cooldown end times.

Civil and noble sectors now support occasional Arbites patrol sweeps when heat or suspicion is high. Patrol interrogation respects a short inspection cooldown, and post-custody jail processing uses a longer several-day cooldown to prevent immediate repeated lockup after release. Cooldown state and the last detailed custody report persist through save/load.

Verification: javac --release 17 PASS; JAR rebuilt with Main-Class mechanist.TheMechanist; class major version 61.

## 0.9.06x — Trauma Sleep Debt / Arbites Capture / Item Ledger Audit

Bleeding wounds now increase sleep debt both at impact time and during hourly medical progression. Shock, taser, stun, arc, webber/control, and sleep-gas style weapons also add sleep-debt/fatigue pressure on damage. This connects combat injury back to survival needs instead of allowing trauma to remain a flat wound counter.

Added first-stage Arbites crime-and-punishment processing. Hard failed Arbites interrogation or nonfatal incapacitation by Arbites authority can now trigger capture: jail time passes, fines are assessed and collected from carried Script and ordinary non-protected bank accounts, limited carried items are confiscated into Arbites evidence lockup, one or more random stats may suffer permanent loss, and the player is released at a precinct/safe point. Varn Crown Trust remains protected from ordinary Arbites fine seizure hooks.

Imperial Script begging now explicitly gives tracked cash rather than a physical item. Legacy physical Script purging is reinforced. Added `ItemLedgerAuthority` to audit item/container/provenance parity, dangling references, physical Script leakage, and player/base compatibility lists. This is an authority-surface audit, not yet a full source-code split.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, source MP3 folder absent, ZIP integrity PASS. Runtime/save version updated to 0.9.06x. Next queued build remains semantic render keys and rank overlays unless legal/banking/heist systems are extended first.

## 0.9.06w — Tracked Script / Unconscious Theft Correction

Imperial Script is now a tracked cash statistic rather than a physical carried inventory item. Script no longer occupies inventory bulk, no longer appears as a normal item stack, and legacy saves migrate physical `Imperial Script` stacks from player inventory/base storage into tracked carried/base-stashed script counters. The inventory display now exposes carried, banked, and base-stashed script as top-level resources. Bank deposits and withdrawals move tracked cash rather than item stacks, and claimed bases now support stashing all carried script and withdrawing up to 100 script from the base cash stash.

Unconsciousness no longer wipes the entire carried inventory. If theft risk applies, the player loses roughly 10-15% of carried script and about 10% of carried items, capped so the event hurts without becoming total liquidation. Banked script remains protected by the selected bank account rules; base-stashed script remains vulnerable to future base raid/heist systems rather than unconscious pocket theft.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, source MP3 folder absent, ZIP integrity PASS. Runtime/save version updated to 0.9.06w. Next queued build remains semantic render keys and rank overlays unless banking/heist work is extended first.

## 0.9.06v — Imperial News Network / Newspaper Public News Foundation

Added the Imperial News Network (INN) as an always-neutral public-information faction with a guaranteed upper-hive bureau zone on floor 6. The INN now appears in visible faction authority, faction strategy plans, personnel generation, room ownership, production-site seeding, and zone generation. Its rooms include editorial bullpens, printing rooms, broadcast centers, circulation offices, censor review, archives, and public dispenser alcoves.

Daily INN issues are generated from the public-discovery layer of faction strategic plans and schemes. Articles summarize discovered or rumored faction goals such as assassinations, theft, sabotage, stockpiling, trade-route movement, factory conversion, recruitment, and long-term pressure. The NEWS command reads the latest broadcast/newspaper issue. Newspaper vending machines sell fresh papers for Imperial Script, discarded old papers can be recovered from civic/trash/news zones, and fresh/yesterday/old/useless paper items can be read from inventory. Old papers and used food tins now seed into trash and relevant room loot as common hive refuse.

Save/load now persists INN daily issues, last issue day, and latest issue text. The implementation remains core-only Java/Swing with LOW 32 art retained, optional packs unchanged, ASCII fallback preserved, and no source MP3s reintroduced.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, INN smoke test PASS, ZIP integrity PASS. Runtime/save version updated to 0.9.06v.

## 0.9.06t — Grenade / Explosive Action / Corpse Ecology / Repair Foundation

Implemented the first playable explosive action layer. G opens explosive targeting from world mode; G/Enter in combat targeting throws or plants the first carried explosive depending on range/type. Grenades create a blinking `*` thrown object with a fuse and then detonate; planted mines/satchels/claymores create passive detectable `!` hazards that can be spotted by sensory checks, triggered by actors, or disarmed through INTERACT for a minimum one-turn action. Explosions generate spreading `O` ASCII blast animation, radius damage, terrain/object/entity/player splash damage, smoke-hook pings, and use the existing weapon sound routing.

Corpse ecology now has first-stage timers. Public corpses noticed by enough non-hostile witnesses are collected by authorities, stripped into Arbites evidence lockup, and routed into nutrient reclamation stock as corpse-starch. Hidden/unnoticed corpses decay into skeleton piles with degraded belongings; junk-like goods collapse toward scrap. Damaged wall/debris ecology now includes a faction repair/hive-quake foundation: controlling factions may patch damaged structural tiles from stock in future passes, while intra-wall breaches can be filled by hive-quake debris.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, ZIP integrity PASS. Runtime/debug version updated to 0.9.06t. Additional verification target: core-only Java/Swing, LOW 32 graphics retained, source MP3 absent, `javac --release 17` required.

## 0.9.06s — Faction Strategy / Population Provenance Economy Foundation

Began the full population/provenance economy simulation pass by adding `FactionStrategicPlan` and `FactionStrategySimulationApi`. Faction leaders now maintain persistent strategic ledgers with leader/deputy names, immediate goals, long-term goals, personal ambitions, target rooms, target items, possible treacherous schemes, target factions, planning/execution/cooldown phases, success/failure counts, last outcome, and journal history. Goals include room control, factory construction/upgrade, recipe production, strategic stockpiling, follower recruitment, enemy attack, theft, assassination, bribery, trade-route opening, facility repair, sabotage, food/water security, ammunition security, and guard recruitment. Execution can materialize provenance-tracked faction stock into persistent faction stock containers, increase production-site stock, improve facility/machine levels, add workers, and raise market pressure on rival factions. Added public-news seed text and FACTIONS-panel strategic summaries. Faction journal objects can be placed in rooms owned by their faction and read through interaction to reveal recorded plans and outcomes. Save/load now persists faction strategic plans and last public simulation report. Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, strategy smoke test PASS, ZIP integrity PASS. Runtime/debug version updated to 0.9.06s.

## 0.9.06r — NPC Ammunition / Faction Loadouts / Explosives Catalog Foundation

Extended the explicit ammunition/reload model to NPCs. `NpcEntity` now persists first-stage combat loadouts: melee weapon, ranged weapon, armor, optional explosive, intellect score, equipment tier, loaded shots, and reserve reloads. NPC equipment selection is faction-biased and intellect-weighted: lower-intellect actors may grab readily available weapons, while smarter/disciplined actors prefer higher-value/longer-range weapons and better protection. Armed guards, gangers, military, Arbites, Sororitas, and noble security receive multiple reserve reloads. Hostile NPCs can now fire ranged weapons at the player when line of sight/range allow, consume loaded shots, reload from their carried reserve, play weapon/reload SFX, use obstruction/cover hit math, and miss into the existing stray-impact terrain/entity/object system. Added explosive catalog/economy entries for frag, krak, smoke, melta grenade, plasma bomb, satchel charge, tripwire mine, motion claymore, and bouncing betty. Armory/munition room loot and some faction traders can surface explosives, but full thrown/placed grenade/mine detonation gameplay remains queued. Verification: `javac --release 17` PASS, NPC equipment smoke PASS, JAR rebuilt, class major version 61. Runtime/save version updated to 0.9.06r.

## 0.9.06q — Ammo Reload / ASCII Weapon Animation / Stray Impact Foundation

Removed the previously bundled source MP3 weapon-effect originals from `assets/sound/effects/source_mp3/` to keep the core package efficient; only runtime WAV effects remain in core. Added generic family ammunition/reload handling for ranged weapons: reload with `X`, consume matching ammo item, fill simple magazine/cell capacity, and require loaded shots before firing. Combat targeting now reports ammo/load state alongside hit odds, range, fire mode, cover penalty, and shield estimate. SNAP/AIMED consume one shot; BURST consumes multiple shots. Added first-stage world-space ASCII attack animations for blade spin, solid-round tracers, las waves, flame sprays, plasma/arc variants, and miss spray. Missed ranged shots can now impact nearby terrain, base objects, or entities; terrain integrity is tracked for the later destructible-cover/intervening-projectile pass. Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61. Runtime/debug version updated to 0.9.06q.

## 0.9.06p — Core Weapon Sound Effects Integration

Integrated the uploaded `Effects.zip` weapon effects into the base package. Preserved the source MP3 files under `assets/sound/effects/source_mp3/` and generated Java-compatible mono WAV runtime files under `assets/sound/effects/`. Added runtime sound handles for bash, slice, shot, las, plasma, flame/melta, lightning/arc, reload, and thundering/heavy-impact effects. Player targeted attacks, player adjacent attacks, and enemy adjacent attacks now trigger mapped weapon-family SFX. `RELOAD` is bundled but reserved for the future explicit reload/ammunition action.

Next planned pass: weapon attack animation hooks using the same weapon-family mapping. Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61. Runtime/debug version updated to 0.9.06p.

## 0.9.06o — Ranged Targeting / Obstruction / Detection Fairness Foundation

Extended the tactical sensory/combat layer before semantic rank overlays. The COMBAT command and `F` hotkey now open a red targeting reticle rather than immediately performing only adjacent combat. Melee remains adjacent. Ranged weapons expose weapon range, fire mode, dashed attack line, preliminary hit percentage, intervening-cover penalty, and shield mitigation estimate. Fire modes currently include SNAP, AIMED, and BURST as first-stage behavior modifiers.

Added first-stage line-obstruction math shared by combat and detection. Doors, walls, room features, machines, base objects, and intervening entities reduce ranged hit chance, with cover near the firing origin penalizing the shooter less than cover near the target. Hard blockers can drop hit chance to zero. Shield relays in/near the defender room provide an estimated mitigation hook.

NPCs now carry selected/active movement states using the same SNEAK/WALK/RUN/SPRINT vocabulary as the player. NPC state chooses motion tactically or pseudo-randomly, affects sound/vision/hearing/evasion estimates, and appears in stronger intent reads. Detection and hearing now apply obstruction penalties and doorway-exposure boosts so sneaking actors crossing doorways are easier to catch, preventing unfair “invisible enemy walked through the watched doorway” moments.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61. Runtime/debug version updated to 0.9.06o.

## 0.9.06n — Facing Vision / Hearing Events / Intent Read Foundation

Implemented the next sensory/tactical foundation before rank overlays. Player vision now uses the last movement/bump direction as facing and computes an off-center ellipsoid awareness field instead of a flat circular radius. Motion state changes the field: stationary is broad, walking is careful, running narrows awareness, sprinting tunnels it, and sneaking remains concealed but not omnidirectional.

Expanded hearing from static noisy glyph checks into a more eventful scan that also hears nearby NPC movement/activity, producing sound pings and summarized direction/distance reports.

Added first-stage LOOK-mode intent reads for visible NPCs. Successful reads can reveal posture/intent text, estimated vision/hearing ranges, projected one-step movement, and dashed hostile attack lines. This is a one-turn tactical estimate and the groundwork for later line-of-sight, intervening-entity, room-feature cover, and ranged-damage obstruction systems.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61. Runtime/debug version updated to 0.9.06n.

## 0.9.06m — Motion-State Senses, Combat Effects, and Sneak Foundation

Added SNEAK as a selectable movement mode and separated selected movement mode from active motion state. Active motion now affects vision range, detailed vision range, hearing range, player attack accuracy, player evasion/defense, disguise credibility, hostile detection, and stealth XP. WAIT now spends a no-move turn and settles prior movement into STATIONARY. Hostiles that fail to detect a sneaking player can produce suspicion barks such as “what was that?” without immediately acquiring target lock.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61. Runtime/save version updated to 0.9.06m.

## 0.9.06l — Movement Modes, Movement Ghost, NPC Crowd Push-Past, and Lighting Pre-Plan

Extended the movement/corridor stage before semantic rank overlays. Player movement now has WALK/RUN/SPRINT modes, cycled with `R` or the command-panel MOVE MODE button. Movement mode changes directional range and per-tile time cost: walk is deliberate/slower, run is the normal pace, and sprint is faster/noisier/fatiguing. Directional movement remains conservative: longer movement is resolved one tile at a time so occupancy, shove, push-past, hostile body-check, trap/contact, sensory update, and turn advancement logic are not bypassed.

The tactical viewport now draws a translucent movement ghost/path preview in the last chosen movement direction using the current mode range. This is a preview only; actual movement authority still comes from tile-by-tile resolution. Non-hostile NPCs gained a limited NPC-vs-NPC crowd push-past swap so civilian traffic can untangle in one-wide corridors without stacking or turning corridor use into mandatory violence. Protected clerics and hostile actors remain excluded from this congestion-relief rule.

Added durable lighting-system planning: future zone generation should use zone-specific light profiles with room/corridor placement chance, exclusion radius, intensity/color/flicker data, switch linkage, power dependency, and cached light/noise fields. Full lighting gradients are not yet implemented.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61. Runtime/save version updated to 0.9.06l.

## 0.9.06c — Faction Roster / Name Revision Recovery + Art Rebase Ready

Recovered the lost faction-roster/name-revision pass into the accessible 0.9.06b art-rebase base before beginning new art insertion. The supplied recovery note named the target counts and authority surfaces, but did not contain the full original source arrays, so this pass reconstructs the functionality and counts rather than claiming byte-for-byte recovery of the missing prior source.

`CharacterCreationAuthority` now exposes explicit `IMPERIAL_MALE_NAMES`, `IMPERIAL_FEMALE_NAMES`, `IMPERIAL_NEUTRAL_GIVEN_NAMES`, `IMPERIAL_FAMILY_NAMES`, and `IMPERIAL_SINGULAR_NAMES` pools while preserving the older compatibility surfaces. Verified pool counts are 148 male given names, 144 female given names, 158 neutral given names, 450 merged first-name compatibility entries, 160 family/surname entries, and 123 singular/sobriquet entries.

Added `FactionRankEntry`, `FactionRosterProfile`, and `FactionRosterAuthority`. Every visible faction now has an eight-rank profile with command slots 1/2/4/8/16/32/64/128 and scope text running from full faction-held-zone authority down to a single room, post, door, desk, shrine, apparatus, or local work point. NPC generation now routes through `FactionRosterAuthority.rankedName(...)`, producing rank-aware names for Arbites, Ministorum, Mechanicus, gangers, cultists, mutants, hivers, nobles, Guard, Sororitas, Administratum, scavengers, rogue-machine factions, and heretics.

Verification: `javac --release 17` passed; `TheMechanist.jar` rebuilt; smoke output confirmed the name counts, `factionRosterProfiles=32`, `visibleFactions=32`, and rank slots `1/2/4/8/16/32/64/128`.

## 0.9.06b Rebase Prep — Art Asset Integration Handoff

Prepared the accessible 0.9.06b package for a new-conversation art asset integration pass. Added an `assets/art/` staging tree, `docs/ART_ASSET_INTEGRATION_REBASE_PLAN.md`, `docs/NEW_CONVERSATION_ART_ASSET_REBASE_HANDOFF.md`, and a prep log. This is a documentation/staging rebase; no gameplay code was changed.

Important note: a 0.9.06c faction-roster/name-revision pass was discussed, but no matching artifact/source was present in the workspace during preparation. Carry it forward as pending unless the user provides the newer base.

## 0.9.06b — World Selection / World Generation Settings / Character Flow Reorder

Starting a new run now routes through world selection/generation before character creation. Existing `saves/worlds/*.mechworld` definitions can be selected from the new world selector; if none exist, the game opens generation settings directly. New world setup exposes NPC density, maximum zone size, zone density, world price difficulty, crafting recipe difficulty, Hoarder mode, and simulation age. Generated worlds are saved before character candidates are selected, and the finalized character now enters the selected/generated world instead of silently creating a fresh world after the character screen.

World setup settings persist into `.mechworld` files and run saves. NPC density affects population pressure, zone size/density feeds `WorldGenerationApi`, price difficulty affects trader buy/sell prices, crafting difficulty scales ordinary recipe supply/part costs, Hoarder mode decouples personal inventory from Strength/Endurance, and simulation age controls how many history batches run before world start.

Verification: `javac --release 17` passed, jar rebuilt, world setup encode/decode smoke passed, runtime scale profile smoke passed, class major version 61.

## Early Direction
The project began as an Android/touch concept, then pivoted to a lightweight Java/Swing desktop prototype for older Linux Mint/Xfce hardware. The design settled into grim-dark underhive survival: procedural rooms, factions, law pressure, gangs, bases, recruitment, augmentation/mutation ambitions, trading, and eventual sector escape.

## 0.2–0.5 Foundation
Established desktop packaging, runnable jar workflow, procedural ASCII exploration, room/corridor generation, basic UI panels, debug logging, character candidates, jobs, inventory, interaction scaffolds, faction suspicion, sensory feedback, and early world persistence. The priority was a runnable prototype rather than a complete content set.

## 0.6–0.8.19 Playability Foundation
Expanded character creation, job statistics, sensory model, generated locations, faction fixtures, clinics, trade depots, shrines, hostile entities, player feedback, and lazy world generation. The project moved from static screens toward a persistent underhive loop.

## 0.8.20–0.8.28 Generation and Standards Stabilization
Audited GUI lifecycle, reduced loose menu state, fixed major worldgen failure cases, added plaza/corridor attachment rules, large-room and door-clearance rules, logical edge transitions, standards/code reconciliation, and worldgen validation. Zone generation became room-authoritative and traversal-safety oriented.

## 0.8.29–0.8.36 Item, Survival, Medical, and Knowledge Foundations
Added the item catalog and quality tiers, vendor/shop/Infopedia support, action feedback HUD, combat feedback, slower survival time, body injury/medical states, heat/suspicion decay, XP/knowledge progression, and machine/research integration. These passes made inventory, pressure, combat, and progression more legible.

## 0.8.37–0.8.40 Production Authority
Introduced faction manufacturing profiles, production recipes, quality ceilings, machine identity, crafting recipe registry, recruit capacity, recruit-operated machines, and crew production shifts. Production became a shared authority path rather than a set of unrelated actions.

## 0.8.41–0.8.45 Bases, Businesses, and Strategic Pressure
Added the BASES management ledger, recruit roster, object ledger, production ledger, base storage reporting, business permits, legal/illegal businesses, income collection, business heat, raid checks, defense assets, powered defenses, decor/business add-ons, cargo infrastructure, and faction market pressure. Player growth now creates both income and retaliation.

## 0.8.46–0.8.48 Sector Objective and Faction Contracts
Added the Sector Governor's Mansion, governor audience dais, off-world ticket bribe/violence paths, governor signet, rail depot ticket validation, sector rail relocation, and representative-issued bounty/fetch contracts. This created a first long-form objective and bounded faction work loop.

## 0.8.49 — Deep Systems Compatibility / Item Provenance Foundation
Added `ItemProvenanceRecord` and save/load support. Provenance began recording maker/source, faction, location, input summary, route, and turn. It was wired into crafting, recruit production, condenser/forge output, trader purchases, vending, threshold finds, contract retrieval, and starting kits. Inventory inspection began reporting known item origin.

## 0.8.50 — Item Provenance Expansion
Expanded provenance to emergency machinery, governor ticket outcomes, contract chips, scavenging, permits, trade chits, and currency rewards. Added provenance-aware removal and transfer helpers for inventory/base storage movement. Stashing and recovering items now create transfer records instead of dropping trace entirely.

## 0.8.51 — NPC/Faction Production Parity Foundation
Added persistent NPC/faction production-site records, hourly stock generation, trader-session links to faction supply sites, purchase records from site-backed traders, and save/load support for faction production sites. This began replacing arbitrary shop fountains with simulated supply origins.

## 0.8.52 — Provenance Handoff Chain Pass
Added unit markers and readable chain history to provenance records, preserved backward compatibility for older records, allowed trade offers to carry provenance, and recorded site stock creation → trader shelf loading → player purchase. Purchase feedback now summarizes origin. Main limitation remained name-list inventory rather than direct item-instance inventory.

## 0.8.53 — Documentation Efficiency Sweep
Collapsed scattered one-pass reports into the durable documentation set. Trimmed standards, master development, development history, asset notes, and the next-conversation briefing. Removed redundant standalone pass reports after their substance was preserved in this history and the master/standards files. Added the documentation efficiency rule: future development should update durable files instead of spawning new audit documents unless explicitly requested.

## 0.8.54 — Architecture Migration Planning
Added the next systems-pass plan to move from scattered implementation piles toward distinct data/database registries, service-code authority, actor records, and API-mediated mutation. The plan preserves the lightweight Java/Swing target, rejects unnecessary external database/framework dependencies, and sequences migration through ID authority, inventory/provenance APIs, container storage, actor records, production/economy services, generation-time records, and only then file/package splitting.

## 0.8.55 — Function Surface Mapping Plan
Documented the existing function piles and their target authority surfaces before code rearrangement. Added a master-plan map from current `GamePanel`, `World`, trader, production, provenance, base, faction, contract, combat, survival, save, and presentation methods into future APIs such as inventory, provenance, container/storage, actor, production, faction economy, trader, contract, base, combat, survival/medical, world generation/query, save migration, and presentation. Added the rule that files should not be split before authority boundaries are proven.

## 0.8.56 — Operational Format Migration Foundation
Began the first code migration from anonymous inventory/storage string piles toward the documented operational format. Added `ItemInstance` and `ContainerRecord` compatibility registries beside the existing `inventory` and `baseStorage` name lists. Central add/remove paths now create item-instance records, attach provenance/unit IDs, place items into explicit containers, and keep the legacy lists alive as display/save compatibility surfaces. Save/load now writes and reads `item.instances`, `item.containers`, and `item.nextInstanceSeq`, while older saves rebuild the new registries from legacy inventory/storage plus provenance data. This is intentionally a compatibility layer, not a full UI or economy rewrite.

## 0.8.57 — Container API Integration Pass
Advanced the operational-format migration from parallel registries toward API-shaped item movement. Added container ID constants, item-name matching helpers, container item counting, same-instance container transfer helpers, structured `ItemActionResult` scaffolding, and save/read parity audits. Stash and take now transfer the same `ItemInstance` between player inventory and base storage, append provenance handoff records, maintain the legacy display lists, and audit container/list parity instead of removing and recreating item identity. Runtime/save version updated to 0.8.57.

## 0.8.58 — Inventory Action Surface Integration
Widened the operational-format migration from base stash/take into the player inventory action surface. Added consumed-item and trader-shelf container IDs, trader shelf ID helpers, structured add/remove inventory result helpers, and null-legacy destination support for container transfers. Drop now transfers a carried item instance into `void.discarded`; ration/food/water/stimulant use transfers the instance into `void.consumed`; selling to a trader transfers the same instance into that trader's shelf container before payment. Runtime/save version updated to 0.8.58 while preserving old save compatibility and legacy display lists.

## 0.8.59 — Trader Acquisition Surface Integration
Widened the operational-format migration from selling into buying. Trader offers now support an item-instance bridge: a buy action materializes the selected offer as an `ItemInstance` in that trader's generated `trader.shelf.*` container, transfers that same instance into `player.inventory`, appends a provenance handoff route, preserves legacy inventory display compatibility, and emits structured `ItemActionResult` audit detail. Purchase failure after payment authorization now returns funds and logs the failure. Runtime/save version updated to 0.8.59.

## 0.8.60 — Infopedia Deduplication and Specificity
Revised the Infopedia detail generator so separate entries no longer share broad duplicate bodies. Knowledge index entries now distinguish system overview, known doctrine ledger, purchasing rules, training sources, and roadmap; item/weapon reference entries remain catalog-specific; rooms distinguish zone room sets, generic room samples, and special room samples; survival, combat, body, medical, heat, XP, crafting, NPC, and faction entries now present entry-specific rules and current-state context. Added the Infopedia Specificity Standard to prevent future duplicate player-facing reference text. Runtime/save version updated to 0.8.60.

## 0.8.61 — Faction Infrastructure and Dense Zone Generation
Expanded room generation from one or two generic profiles per zone into faction-specific infrastructure libraries. Added distinct habitation, mess/kitchen, food storehouse, product warehouse, storefront/counter, clinic/medical, workshop/utility, security, sanitation, shrine/social, and logistics patterns across civilian, sump market, ganger, Arbites, Administratum, Imperial Guard, Mechanicus, noble, rail, sewer, mutant, cultist, and trash zones. Increased zone slice dimensions, centered the plaza in the larger footprint, raised the target to 20-30 rooms, shortened plaza spokes, and reduced organic branch gaps for denser room placement. Runtime/save version updated to 0.8.61.

## 0.8.62 — Emergency Corridor Network Repair

Changed deterministic fallback room connection so emergency corridors attach to the nearest existing pathable corridor network instead of carving long direct connectors back toward the plaza. Added a shortest repair-path helper from room door candidates to corridor glyphs, fixed ordinary `+` corridor glyph recognition, updated dense-zone documentation, and preserved the 20-30 room target. Runtime/save version updated to 0.8.62.

## 0.8.64 — World-Generation API Sectioning
Started the next class-sectioning/API migration pass by moving emergency corridor path-selection policy behind a new `WorldGenerationApi` surface. `World` still owns live tile, room, and reachability state, but the rule for selecting nearest-corridor orthogonal repair paths is now delegated through an API-shaped class instead of being purely embedded in the world object. This implements the previous 0.8.63 corridor-repair rule as a stable surface: fallback repair still attaches to the nearest reachable corridor network using straight runs and 90-degree corners, while the next migration target should move room-library manifests and guaranteed faction infrastructure requirements behind the same world-generation API surface. Runtime/save version updated to 0.8.64.

## 0.8.63 — Orthogonal Emergency Corridor Repair
Emergency corridor repair now uses orthogonal architectural paths only: straight corridor runs with explicit 90-degree corners. The helper still targets the nearest reachable corridor network rather than the plaza center, but it no longer uses flood-fill predecessor paths that can snake through dense wall mass. Direct L-shaped paths and bounded dogleg paths are tested, scored by length and turn count, carved through solid wall only, and audited when applied. Runtime/save version updated to 0.8.63.

## 0.8.65 — World-Generation Scale API Sectioning
Continued the API/class-sectioning migration by moving zone slice dimensions, room-count target policy, room-target clamps, and central plaza sizing/centering behind `WorldGenerationApi` and a `WorldGenerationScaleProfile`. The active profile preserves the existing dense-zone behavior as `current.minimum`: 132-164 by 82-105 slices with 20-30 rooms and a centered 15x15 preferred plaza. Added a planning reference for a future ultra-hive high-density preset around 800x800 zones with greatly inflated room counts and corridor-riddled space usage, but did not expose it as a runtime option. Runtime/save version updated to 0.8.65.

## 0.8.66 — Room Manifest API Sectioning
Continued the API/class-sectioning transition by adding `RoomManifestApi` and `StampedRoomSpec`. Dense zone generation now applies a guaranteed faction infrastructure manifest to early non-plaza rooms before random special overlays are applied. Manifest entries include faction-specific barracks/dormitories/apartments, cafeterias/messes, kitchens, food storehouses, product warehouses, storefronts/issue counters, clinics, security spaces, libraries/learning rooms, daycare/creche/training equivalents, and specialty rooms. The pass also adds first-pass room-purpose tile dressing so cafeterias, barracks, warehouses, learning rooms, security rooms, and other stamped purposes visually differ inside the glyph map. Runtime/save version updated to 0.8.66.

## 0.8.67 — Structured Room Module API Pass
Promoted selected `RoomManifestApi` entries from profile-only stamps into true physical architectural modules. Added `StampedModuleSpec`, a `RoomManifestApi.structuredModulesFor()` surface, and world-side module stamping that carves faction-specific corridor/module clusters while preserving `World` as the owner of live tile arrays and collision checks. First stamped modules cover Guard barracks/drill and field cafeteria/kitchen, noble apartment/bath and dining/kitchen suites, Mechanicus nutrient/diagnostic and data-chapel/rest-cell spines, Arbites holding-cell rows, Administratum dead-file library spines, civilian apartment/creche/learning clusters, and Sump Market storefront rows. This advances the API model without a cosmetic file split: purpose and module selection live in the manifest API; physical carving remains in `World`. Runtime/save version updated to 0.8.67.

## 0.8.68 — Item Catalog API and Bounded Hive-Wall Planning Pass
Continued the API/class-sectioning migration by adding `ItemCatalogExpansionApi` and `TraderStockExpansionApi`. The item catalog now receives a first large expansion of faction-specific goods through a dedicated content registration surface: civilian meal vouchers, Guard field rations and charge packs, Mechanicus nutrient ampoules and diagnostic tools, noble luxuries and signet kits, market/rail commerce tools, cult contraband, sewer food, daily-life tokens, training slates, armor, clothing, and a wider weapon/ammunition set. Trader stock now receives zone/faction-specific additions through the trader expansion surface instead of duplicating generic shelves everywhere.

Added `BoundedOuterHiveWallApi` as a future policy surface for the proposed post-validation bounded outer high-wall feature: one hivewall maintenance room bolted to an exterior corridor, left/right exterior maintenance corridors tracing around the zone exterior, and a few abandoned high-danger interwall rooms outside the ordinary room quota. Runtime/save version updated to 0.8.68.

## 0.8.69 — Room Loot API Integration Pass
Added `RoomLootApi` as a scavenge-content authority surface. Targeted scavenging now resolves room/zone/faction purpose into cataloged items instead of returning only generic hard-coded food, water, machine parts, or raw room loot strings. Legacy room loot hints are normalized into catalog entries where possible, strengthening item provenance by giving scavenged items more specific room and zone identities. Runtime/save version updated to 0.8.69; Java 17 build and jar rebuild verified.

## 0.8.70 — Persistent Room Container API Integration Pass
Added `RoomContainerApi` and routed successful room scavenging through lazy persistent room caches. First touch of a room seeds a `ContainerRecord` with cataloged items selected from room profile and zone identity through `RoomLootApi`; successful scavenging transfers an existing `ItemInstance` from that room cache to `player.inventory`, preserving provenance and reducing script-spawned anonymous loot. Runtime/save version updated to 0.8.70; Java 17 build and jar rebuild verified.

## 0.8.71 — Room Cache Inspection API Pass
Continued the room-container API integration by adding a player-facing `CACHE LEDGER` command to the scavenge panel. The command reads or lazily seeds the current room's persistent `ContainerRecord`, summarizes its `ItemInstance` contents, and exposes category/provenance information without transferring items. This validates the next container authority surface while preserving efficient lazy generation and legacy compatibility. Runtime/save version updated to 0.8.71; Java 17 build and jar rebuild verified.

## 0.8.72 — Personnel Provenance API Foundation
Added `PersonnelProvenanceRecord`, `PersonnelProvenanceApi`, and `PersonnelReplacementRequest` as the next API/model transition surface. NPCs now receive origin/backstory records covering hive birth, creche/barracks/household upbringing, rail-hub arrival, faction transfer, origin room/zone, and current population pool. Conversation faction lines include the origin summary. Combat deaths now audit personnel provenance and schedule a delayed faction replacement from a relevant source pool; matured replacements spawn with their own provenance. Runtime/save version updated to 0.8.72; Java 17 build and jar rebuild verified.

## 0.8.73 — Personnel Population Ledger API Integration
Continued the personnel-provenance API transition by adding `RoomPopulationLedger` and `PersonnelPopulationApi`. Generated zones now seed compact room-backed population ledgers from barracks, creches, dormitories, rail intake rooms, noble households, cult/gang/mutant rooms, Mechanicus forge creches, cafeterias, and other staffed rooms. Initial NPC staffing and delayed replacements can draw from those ledgers, and replacement requests now persist source room/source ledger identity for later arrival. Saved world state now includes `world.populationLedgers`. Runtime/save version updated to 0.8.73; Java 17 build and jar rebuild verified.

## 0.8.74 — Campaign Hive World Definition API
- Added a separate generated-world definition layer beside active run saves.
- Added `HiveWorldDefinition`, `WorldNamingApi`, and `CampaignWorldApi`.
- A hive now has a deterministic name, reusable world ID, named sectors, named zone-slices, and compact zone-history seed text.
- New games and loaded games now create/load `saves/worlds/<world-id>.mechworld` from the atlas seed so multiple runs can later share a generated hive structure.
- Current implementation stores compact metadata, names, and history scaffolds; full pre-generated population/item history simulation remains a later loading-phase feature.

## 0.8.76 — Progress-Aware World Generation Framework
Added `WorldGenerationProgressRecord` and `WorldGenerationProgressApi`. `HiveWorldDefinition` now persists generation progress metadata into the reusable `.mechworld` file, including named phases for hive/sector/zone naming, compact zone-history seeding, lazy physical slice generation, and planned future population/item history synthesis. `WorldAtlas.createSlice()` now marks lazy physical slice progress and saves the world definition after generating a slice. Runtime/save version updated to 0.8.76; Java 17 build and jar rebuild verified.

## 0.8.76 — Faction-Control Epoch History API
- Added `WorldHistoryApi`.
- Added persistent `zoneEpochs` to `HiveWorldDefinition` and `.mechworld` save output.
- Added a new world-generation progress phase: faction-control epoch synthesis.
- Added bounded batch generation for compact zone ownership/occupation histories.
- Current slices generate their epoch ledger on demand so gameplay does not wait for full-hive history synthesis.
- Diegetic map panel now exposes hive ID, progress phases, named sector/zone, compact history, and faction-control epochs.
- Updated runtime/save version to 0.8.76.

## 0.8.77 — Interstitial Hive Infrastructure API
Added `InterstitialInfrastructureApi` and a late world-generation pass that converts deep unused solid mass between generated rooms/corridors from anonymous `#` wall into non-walkable infrastructure blockers: support beams, gantry lattice, sealed conveyor ways, pipe bundles, cable conduit columns, collapsed debris pockets, buried cache pockets, and dangerous buried reserves. Bulkhead walls adjacent to rooms/corridors remain `#`. Interstitial blockers now block movement and line-of-sight, have LOOK descriptions/colors, and each generated zone seeds roughly a dozen buried feature markers for future hidden cache/breach/hivewall systems. Runtime/save version updated to 0.8.77; Java 17 build and jar rebuild verified.

## 0.8.78 — Zone Facility History API Integration
Added `ZoneFacilityRecord` and `ZoneFacilityHistoryApi` as the next progress-aware world-simulation layer. The reusable `.mechworld` definition now persists `zoneFacilities`, a compact facility-establishment ledger per named zone. Facility history is generated in bounded batches after faction-control epochs, updates the world-generation progress ledger, saves through `CampaignWorldApi`, and is also generated on demand for the active slice. The diegetic map panel now displays facility establishment ledgers beside compact history and faction-control epochs. This bridges historical control into practical infrastructure: barracks, mess halls, kitchens, warehouses, clinics, learning spaces, cult cells, rail intake, Mechanicus galleys, noble service rooms, and other room systems now have a durable historical establishment layer that later personnel and item provenance can cite. Runtime/save version updated to 0.8.78; Java 17 build and jar rebuild verified.

## 0.8.79 — Bounded Outer Hive-Wall Generation API
Promoted `BoundedOuterHiveWallApi` from planning surface into a real late world-generation pass. Generated slices now stamp a bounded high-wall envelope after normal validation/repair, flood-fill outside-edge abyss as void space, carve an exterior hivewall maintenance corridor loop, add one Hivewall Maintenance Room outside the ordinary room quota, and add three or four Abandoned Interwall Danger Rooms. Interwall danger rooms are populated after normal NPC population with hostile high-threat entities drawn from mutant, cultist/heretic, rogue machine, or wanted-criminal bands. The implementation uses a conservative rectangular envelope for efficiency; future passes can replace it with tighter contour tracing and attach persistent caches/provenance to interwall rooms. Runtime/save version updated to 0.8.79; Java 17 build, jar rebuild, class audit, and bounded-hivewall smoke test verified.

## 0.8.80 — Hivewall Room Cache + History Integration

Implemented the first material integration pass for bounded outer hivewall rooms. Hivewall Maintenance Rooms and Abandoned Interwall Danger Rooms now seed persistent `RoomContainerApi` caches with tools, weapons, armor, sealed supplies, tech relics, ration reserves, contraband, and salvage. Cache provenance cites the active zone's faction-control epoch and facility-history ledger so recovered items are no longer anonymous wall loot. Interwall danger-room descriptions now explain why the chamber was sealed, abandoned, occupied, or forgotten, and hostile interwall entities receive personnel provenance describing rogue automata service origins, criminal hideouts, mutant occupation pockets, or cult/heretic shelter routes. Runtime/save version advanced to 0.8.80.

## 0.8.81 — Facility-Linked Personnel Provenance Integration
Connected room population ledgers to the zone facility-history layer. `RoomPopulationLedger` now persists facility identity, purpose, controller/founding text, product focus, and historic note alongside capacity/available/assigned/dead counts. Added `ZoneFacilityLedgerEntry` parsing/matching helpers so `PersonnelPopulationApi` can match barracks, creches, messes, rail intake, households, cult cells, Mechanicus rest/galleys/chapels, and other source rooms to the current zone's facility establishment ledger. NPC provenance and delayed replacement provenance now include linked facility summaries in upbringing/backstory text. Runtime/save version updated to 0.8.81; Java 17 build and jar rebuild verified.

## 0.8.82 — Facility-Linked Room Cache / Item Provenance Integration
Connected ordinary persistent room caches to the zone facility-history layer. Added `FacilityLinkedRoomCacheApi`, which matches a room profile and zone identity against the current zone's `ZoneFacilityHistoryApi` ledger, biases cache seeding toward the matched facility, and writes item provenance that cites facility ID/purpose/controller/product focus, faction-control epochs, named hive/sector/zone, room ID, and persistent cache route. Hivewall/interwall rooms still use their specialized history-aware cache path. Runtime/save version updated to 0.8.82; Java 17 build and jar rebuild verified. Next target: 0.8.83 Production Facility Output Simulation.

## 0.8.83 — GUI Layout Authority Integration
Began the GUI/menu containment pass requested after repeated overlap/dropdown issues. Options controls now use `GuiLayoutApi` bounded screen-derived columns instead of raw fixed offsets, graphics dropdowns align to their invoking columns, hover/help/click hit testing now treats open dropdowns as modal blockers, and obscured controls underneath active dropdowns no longer glow or activate through keyboard confirmation. Added the GUI Layout Authority Standard. Production Facility Output Simulation remains the next queued simulation pass. Java 17 build and jar rebuild verified.

## 0.8.84 — Production Facility Output Simulation
- Added `ZoneProductionOutputRecord`.
- Added `ProductionFacilityOutputSimulationApi`.
- Added persistent `HiveWorldDefinition.zoneProduction` and `.mechworld` save/load support.
- Added world-generation progress phase `06.production-facility-output`.
- World atlas now advances production-output ledgers in bounded batches after facility histories.
- Active slices synthesize missing production-output history on demand.
- Diegetic map panel now displays the current production-output ledger.
- Ordinary room cache seeding may add matching facility output samples.
- Facility-linked item provenance now cites the production output ledger and production chain.
- Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.85 — Production Distribution / Stock Movement API
- Added `ZoneStockMovementRecord`.
- Added `ProductionDistributionApi`.
- Added persistent `HiveWorldDefinition.zoneStockMovements` and `.mechworld` save/load support.
- Added world-generation progress phase `07.production-distribution`.
- World atlas now advances compact stock-movement ledgers in bounded batches after production-output ledgers.
- Active slices synthesize missing stock-movement history on demand.
- Diegetic map panel now displays the current stock movement ledger.
- Facility-linked room caches now use stock-movement records when seeding ordinary room inventories.
- Item provenance now cites the matched stock movement route between production output and persistent room cache.
- Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.86 — Faction Store + Trader Supply Chain Integration
- Added `TraderSupplyChainApi`.
- Trader sessions now pull traceable shelf goods from the active zone's production-output and stock-movement ledgers after normal faction/site stock is applied.
- Existing trader offers receive supply-chain provenance when their item name matches a stock movement sample; new offers are added when the movement ledger provides additional cataloged goods.
- Trader shelf provenance now cites source facility ID, movement destination, movement kind, controller text, production/distribution ledger samples, and the named zone location.
- Trade UI now shows a compact supply-chain summary when the engaged trader has ledger-backed stock.
- Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.87 — Historical Conflict, Loss, Theft, and Abandonment API
Added `ZoneConflictLossRecord` and `HistoricalConflictLossApi`. Hive world definitions now persist `zoneConflictLosses` and expose progress phase `08.conflict-loss-abandonment`. Active slices synthesize missing conflict/loss history on demand. Ordinary room-cache item provenance now cites matched conflict/loss records in addition to facility, production, and stock-movement records. Cache seeding can now pull small samples from theft, seizure, collapse, cult diversion, rogue automata abandonment, and other loss histories.

## 0.8.87 Planning Note — World Age Technology Scaling
Recorded a future design rule for generated hive age: longer/deeper world-generation histories should unlock more advanced recipe/technology context, richer production outputs, higher-quality equipment, and denser conflict/loss relics, while short/fast histories should remain more basic and junk-heavy. The rule also records a progression cap: ordinary world advancement should stop two tiers below maximum technology so player research, faction laboratories, and rare lost relics still matter. This was documented only; no code implementation was added in this note.

## 0.8.88 — Concrete Historical Item Materialization Rules

- Added `ZoneMaterializedItemRecord`.
- Added `HistoricalItemMaterializationApi`.
- Added persistent `.mechworld` support for `worlddef.zoneMaterializedItems`.
- Added world-generation progress phase `09.concrete-historical-item-materialization`.
- Active slices synthesize missing materialized item ledgers on demand after conflict/loss history.
- Map panel displays current materialized item ledger.
- Ordinary room caches can pull selected materialized historical items into persistent `RoomContainerApi` caches.
- Facility-linked provenance now cites the materialized item record.
- Ordinary world-history materialization quality caps at Masterwork, preserving Archeotech for rare/player/faction-lab pathways.

## 0.8.89 — Population Work Assignment / Facility Labor Simulation

- Added `ZoneLaborAssignmentRecord`.
- Added `PopulationWorkAssignmentApi`.
- Added persistent `.mechworld` support for `worlddef.zoneLaborAssignments`.
- Added progress phase `10.population-work-assignment`.
- Active slices synthesize missing labor-assignment ledgers on demand after concrete historical item materialization.
- Facility labor records assign workers, vacancies, source population pools, roles, and output effects to facility histories.
- Facility-linked item provenance now cites labor assignment records along with facility, production, stock movement, conflict/loss, and materialized item records.
- Expanded food-production facilities and catalog items: hydroponics, nutrient vats, algae/reclamation outputs, noble artificial-sun bio-gardens/orchards, military triglyceride gel and amino-porridge, recaf/caf, ploin juice/wobble, amasec quality tiers, wall-rat protein, soylens viridian, corpse-starch, marsh-rice, protein grain, vorder leaves, caba nuts, and luxury bio-garden food.
- Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.90 — Weapon Catalog / Underhive Armory Integration

- Imported the user-provided base weapon seed list into canonical catalog families while deliberately excluding named relic handling from base registration. `Foehammer` is not a base item; future relic systems should wrap ordinary weapon families with provenance, quality, and unique history.
- Added broad melee, pistol, rifle, special weapon, heavy weapon, and ammunition entries to `ItemCatalogExpansionApi`.
- Added underhive improvised weapon families for hivers, gangers, mutants, scavengers, and cultists so local scrap weapons exist beyond generic junk versions.
- Expanded production-output sampling for Guard, Arbites, Mechanicus, noble, gang, mutant, and cult facilities so armories, evidence stores, stolen-stock rooms, scrap hoards, and ritual stores can emit faction-appropriate weapons.
- Expanded room loot, trader stock, and crafting surfaces with zip pistols, pipe shotguns, rebar mauls, emergency cutters, Webbers, Arc Rifles, improvised mutant/cult weapons, and ammunition families.
- Combat skill routing now uses `ItemCatalog.isFirearmLike()` so pistols, rifles, bolters, flamers, meltas, webbers, stubbers, las weapons, carbines, and heavy weapons train/use Firearms rather than falling back to Melee because their names do not contain `gun`.
- Runtime/save version updated to 0.8.90; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.91 — Industrialized Itemization Framework

- Added the first reusable industrial component catalog entries for weapon bodies, receivers, barrels, emitters, capacitors, lenses, fasteners, stocks, optics, chain-weapon parts, power-field parts, pressure/flame parts, high-energy containment parts, toxin/web/arc systems, water reclamation inputs, fertilizer, vat slurry, raw earth, and distilled water.
- Added `DraftIndustrialRecipe`, `IndustrialRecipeProfile`, `RecipeDecompositionApi`, `RecipeGraphAuditApi`, and `RecipeGraphAudit`.
- `RecipeDecompositionApi.generatedDraftRecipes()` now scans the live `ItemCatalog` and generates draft bills of material for weapons, tools, food provisions, and water goods without adding them to the player-facing `CraftingRecipe.all()` list.
- Las-family recipes now distinguish pistol, carbine, rifle, precision longlas, and jury-rigged laslock patterns by body, emitter, lens, capacitor, socket, stock/grip, trigger group, heat sink, and optic needs.
- Component recipes now express the requested pistol/carbine/rifle/heavy body hierarchy: pistol bodies use fewer armament components than rifle bodies, and heavy bodies require much larger bracing demand.
- Water and food precursor routes were seeded through wastewater, sump sludge, dirty/filtered/potable/distilled water, raw earth, waste biomass, fertilizer, vat nutrient slurry, hydroponic grain, and fruit mash.
- Added `logs/industrial_itemization_audit_0.8.91.txt`; the audit generated 154 draft recipes across 58 families and passed with zero missing inputs, zero missing outputs, zero circular recipes, and zero absurd direct-input widths.
- Runtime/save version updated to 0.8.91; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.92 — Component Catalog Expansion

- Expanded `ItemCatalogExpansionApi.industrialComponentItems()` with a broader reusable component vocabulary before full weapon-family decomposition. New groups include processed metal stock, hardened metal stock, plasteel shavings, polymer sheet, ceramic insulators, optic blanks, conductive filament, contact strips, capacitor wafers, circuit wafers, data sockets, sensor crystals, seals, hoses, pipe couplings, bearings, gears, motor coils, actuators, servo linkages, firearm action parts, ammunition precursors, flame/plasma/melta/arc/power-field parts, chain-weapon parts, tool-head parts, water-filter/reclamation components, agriculture trays/lights/nutrients, and food packaging inputs.
- Expanded `RecipeDecompositionApi.componentRecipes()` so many intermediate components now have draft lower-level ancestry instead of floating as isolated names.
- Updated `RecipeGraphAuditApi` and `RecipeGraphAudit` to report industrial/component catalog entry counts and category groups alongside recipe families and graph-health checks.
- Fixed the food-provisioning classifier so substring matches such as `calibration` no longer trigger the `ration` food route; `Mechanicus calibration probe` correctly remains a general tool draft recipe.
- Added `logs/industrial_itemization_audit_0.8.92.txt`; the audit generated 237 draft recipes across 135 families, counted 163 industrial/component catalog entries across 45 groups, and passed with zero missing inputs, zero missing outputs, zero circular recipes, and zero absurd direct-input widths.
- Runtime/save version updated to 0.8.92; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.92a — Clothing / Armor / Textile Component Expansion

- Added `ItemCatalogExpansionApi.clothingArmorTextileComponentItems()` with reusable textile, garment, armor, seal, faction-marking, and harness components.
- Added `ItemCatalogExpansionApi.clothingArmorVarietyItems()` with broad final-good variety for workers, gangers, scavengers, mutants, cults, Guard, Arbites, Mechanicus, nobles, and void/environmental workers.
- Extended `RecipeDecompositionApi.isDraftDecomposable()` so clothing and armor item categories receive draft industrial bills of material.
- Added clothing/armor branches to `IndustrialRecipeProfile.profileFor()` for flak armor, riot gear, scrap armor, concealed mesh garments, sealed protective suits, Guard uniforms, civilian workwear, Mechanicus robes/harnesses, cult garments, noble garments, gang clothing, coats, headgear, limbwear, harnesses/sashes, and generic garments.
- Kept generated clothing/armor recipes draft/audit-facing; no generated recipe flood was added to `CraftingRecipe.all()`.
- Added `logs/industrial_itemization_audit_0.8.92a.txt`; the audit generated 378 draft recipes across 182 families, counted 209 industrial/component catalog entries across 48 groups, and passed with zero missing inputs, zero missing outputs, zero circular recipes, and zero absurd direct-input widths.
- Runtime/save version updated to 0.8.92a; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.93 — Weapon Family Decomposition

- Refined `IndustrialRecipeProfile.profileFor()` with family-specific draft weapon profiles for las, stub, auto, bolt, shotgun, marksman, chain, power, force, shock, flamer, melta, plasma, arc, webber, toxin/needle, chemical, heavy, tool-weapon, and improvised underhive weapons.
- Added new reusable weapon subassemblies to `ItemCatalogExpansionApi.industrialComponentItems()`: pistol/rifle/heavy firearm actions, bolt weapon action, shotgun breech action, revolver cylinder assembly, heavy feed action group, heavy weapon recoil cradle, military/civilian/improvised weapon casing, compact charge cradle, plasma/melta discharge assemblies, heavy energy heat exchanger, pressure weapon hose harness, needle/web discharge assemblies, arc discharge head, field emitter array, force focus lattice, mono-edge strip, toxin delivery channel, shock head assembly, chain cutter rail assembly, and heavy cutter carriage.
- Added matching draft component recipes in `RecipeDecompositionApi.componentRecipes()` so the new subassemblies have lower-level ancestry instead of floating as isolated names.
- Tightened family distinctions: revolvers separate from pistols; force weapons separate from ordinary blades; shock batons/mauls separate from generic blunt tools; pipe/sawed shotguns use improvised casing routes; hunting rifles use civilian casing while military marksman rifles use military casing.
- Kept generated weapon recipes draft/audit-facing; no generated recipe flood was added to `CraftingRecipe.all()`.
- Added `logs/industrial_itemization_audit_0.8.93.txt`; the audit generated 407 draft recipes across 234 families, counted 235 industrial/component catalog entries across 50 groups, saw 117 weapon items, and passed with zero missing inputs, zero missing outputs, zero circular recipes, and zero absurd direct-input widths.
- Runtime/save version updated to 0.8.93; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.94 — Water / Food / Waste / Agriculture Chains

- Added `ItemCatalogExpansionApi.waterFoodWasteAgricultureChainItems()` with raw and intermediate survival-economy entries for atmospheric condensate, greywater, strained sump water, reclamation brine, toxin slurry, reclaimed mineral cake, clean/sterile water, compost substrate, sterilized grow medium, hydroponic nutrient solution, crop/culture trays, noble orchard inputs, vat bases, protein/amino/lipid intermediates, soylens/corpse-starch/ration pastes, amasec intermediates, ploin concentrate, recaf roast, and wall-rat protein stock.
- Extended `RecipeDecompositionApi.isDraftDecomposable()` to include drink, agriculture, organic waste, and chemical-waste categories in the draft graph.
- Expanded `IndustrialRecipeProfile.profileFor()` with explicit water, food, waste, and agriculture branches: filtration, potable finishing, distillation, sterile sealing, brine/toxin byproducts, fertilizer/grow-medium chains, hydroponics, algae/soylens, fungus, wall-rat meat, civilian meals, emergency rations, noble orchard goods, ploin juice, recaf, low/high amasec, corpse-starch, Mechanicus nutrient ampoules, and military triglyceride/amino food routes.
- Updated `RecipeGraphAuditApi` to count broader survival-economy catalog groups instead of only the earliest water/raw and food/raw categories.
- Kept generated survival-economy recipes draft/audit-facing; no generated recipe flood was added to `CraftingRecipe.all()`.
- Added `logs/industrial_itemization_audit_0.8.94.txt`; the audit generated 473 draft recipes across 300 families, counted 282 industrial/component/survival catalog entries across 58 groups, saw 589 total catalog items, and passed with zero missing inputs, zero missing outputs, zero circular recipes, and zero absurd direct-input widths.
- Runtime/save version updated to 0.8.94; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.94a — Quality / Knowledge / Machine Authority Alignment

- Implemented the feasibility-plan bridge layer required before factional recipe variants. Added centralized quality authority, production-quality capping, knowledge-tree registration, explicit machine-tier profiles, and expanded faction manufacturing identities.
- Added `QualityAuthorityProfile` / `QualityAuthorityApi` with eight quality tiers, stat modifiers, and a single production capping rule. Existing `ItemQuality.detailLines()` now routes through the quality authority layer for player-facing reference text.
- Added `KnowledgeTreeApi` with 11 production knowledge categories across seven doctrine bands: Food Processing, Water Purification, Metallurgy, Textile Fabrication, Medical Processing, Chemical Synthesis, Ballistics, Energy Systems, Industrial Maintenance, Agricultural Processing, and Salvage Processing. The bands are Junk, Common, Serviceable, Fine, Masterwork, Noble, and Archeotech.
- Added `MachineTierProfile` / `MachineTierAuthority` so built machines now resolve quality ceiling, efficiency, throughput, breakdown risk, durability, power demand, and worker capacity from one table. Production quality now caps through the central authority instead of local-only recipe logic.
- Expanded `FactionManufacturingProfile` so 0.8.95 can mutate real behavior and not merely labels. Factions now carry durability, comfort, prestige, efficiency, maintenance, power demand, repairability, reliability, defect risk, and aesthetic identity values.
- Added `AuthorityAlignmentAuditApi` / `AuthorityAlignmentAudit`; `logs/authority_alignment_audit_0.8.94a.txt` reports PASS with 8 quality tiers, 77 generated knowledge-tree nodes, 8 machine-tier profiles, 9 faction identity profiles, and zero issues.
- `logs/industrial_itemization_audit_0.8.94a.txt` reports the draft recipe graph still passes: 473 draft recipes, zero missing inputs, zero missing outputs, zero circular recipes, and zero absurd direct-input widths.
- Runtime/save version updated to 0.8.94a; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.95 — Faction Manufacturing Identity Variants

- Implemented generated faction manufacturing variants on top of 0.8.94a's quality, knowledge, machine, and faction identity authority layer.
- Added `FactionRecipeVariant`, `FactionRecipeVariantApi`, `FactionRecipeVariantAuditApi`, and `FactionRecipeVariantAudit`.
- Added `ItemCatalogExpansionApi.factionRecipeVariantIdentityItems()` for small identity/finish inputs: quartermaster stamps, serialized casing tags, Mechanicus calibration seals, shrine-etched control tags, house hallmark plates, gilding foil, gang color scraps, intimidation spikes, contraband cipher tags, profane mark stencils, mutant fitment straps, salvage tags, reclaimed repair brackets, and civic inspection chits.
- Generated faction variants now transform base draft recipes by faction rather than hand-authoring duplicate recipes. Variants carry output quality, recipe prefix, knowledge category, machine tier, lawful/contraband status, transformed inputs, and stat bias summaries.
- Guard, Arbites, Mechanicus, noble, hiver, ganger, scavver, cult, and mutant production now differs by durability, comfort, prestige, efficiency, reliability, repairability, defect risk, maintenance pressure, power demand, lawful status, and input substitutions.
- Fixed a quality-prefix collision during audit: `Noble hallmark plate` was renamed to `House hallmark plate` because item lookup strips `Noble` as a quality prefix.
- Kept all generated faction variants draft/audit/provenance-facing; no mass recipe injection into `CraftingRecipe.all()`.
- `logs/faction_recipe_variant_audit_0.8.95.txt` reports PASS with 473 base draft recipes scanned, 241 variant-eligible base recipes, 1810 generated faction recipe variants, and zero missing transformed inputs, uncovered eligible bases, absurd widths, or issues.
- `logs/industrial_itemization_audit_0.8.95.txt` reports PASS with 473 draft recipes and 296 industrial/component catalog entries.
- Runtime/save version updated to 0.8.95; Java 17 compile passed; jar rebuilt; class major version 61.
## 0.8.95a — Chem / Narcotic / Intoxicant Production Chains
Inserted a final production-chain bridge before Infopedia/ledger display. Integrated the expanded underhive chem, narcotic, intoxicant, medicae, labor-control, cult, mutant, noble, void, security, vice, and rare campaign-substance list into the catalog. Added reusable chemical precursors and draft decomposition routes for smoke goods, stimulants, sedatives, combat chems, hallucinogens, medicae compounds, interrogation drugs, forbidden perception drugs, cult sacraments, sump tonics, void/ash-waste intoxicants, and lower-hive trash drugs. Extended faction recipe variants so chem goods receive faction-specific transformed inputs, law/contraband status, knowledge category, and machine hints. Extended trader stock, room loot, and facility-output samples so chems appear through clinics, gang chem kitchens, noble salons, cult shrines, mutant warrens, labor spaces, markets, security rooms, and medicae contexts. Audits passed with 812 catalog entries, 167 chem/medical-chem final goods, 42 chem precursor components, 682 draft recipes, 408 variant-eligible bases, 2729 generated faction variants, and zero missing/circular/absurd graph issues.

## 0.8.95b — Laboratory / Chemical Equipment Production Requirements
- Inserted a bridge pass before 0.8.96 because the chem economy needed explicit apparatus, room placement, manning, quality, knowledge, and machine-tier requirements before being exposed in Infopedia / ledgers.
- Added `LaboratoryEquipmentProfile`, `ChemicalEquipmentAuthority`, `ChemicalEquipmentAuditApi`, and `ChemicalEquipmentAudit`.
- Expanded `DraftIndustrialRecipe` with non-consumed `equipmentRequirements`, `roomRequirements`, `processType`, `placementNote`, `manningRequirement`, and `minimumMachineTier`.
- Registered 31 laboratory / chemical equipment catalog items, including crude chem benches, reagent benches, filtration racks, stills, fermentation tubs, fungal trays, drying racks, pellet presses, injector filling stations, sterile medicae benches, cold storage, aerosol fillers, pressure vessels, fume hoods, toxin lockboxes, interrogation dosing cradles, labor dosing dispensers, nootropic assay desks, calibrated assay shrines, spire perfumery glassware, ritual censer kilns, forbidden preparation chambers, warp-containment mirror boxes, mutant adaptation racks, voidship galley stills, ash-waste mineral leachers, and chemical waste traps.
- Annotated 225 chemical, medicae-chemical, precursor, intoxicant, alcohol, lho, forbidden, labor, and related recipes with process type, required apparatus, room group, manning note, and minimum machine tier.
- Added faction equipment preferences so scavver/ganger variants can substitute crude benches and stills, Mechanicus variants can require calibrated assay shrines, noble variants can add perfumery/vialing gear, Arbites variants can require interrogation/security apparatus, cult variants can require ritual/forbidden apparatus, mutant variants can require adaptation racks, and Guard variants can lean on injector/cold-storage gear.
- Updated room loot, trader stock, and facility output samples so lab equipment can appear in relevant chem rooms, clinics, gang kitchens, noble apothecaries, Mechanicus labs, cult shrines, mutant warrens, and security/interrogation spaces.
- Audit logs: `laboratory_chemical_equipment_audit_0.8.95b.txt`, `industrial_itemization_audit_0.8.95b.txt`, `faction_recipe_variant_audit_0.8.95b.txt`, and `authority_alignment_audit_0.8.95b.txt` report PASS.
- Runtime/save version updated to 0.8.95b; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.96 — Infopedia / Audit / Production Ledger Display

- Added `ProductionLedgerDisplayApi`, `ProductionLedgerDisplayAuditApi`, and `ProductionLedgerDisplayAudit` to expose the generated production truth through player-readable Infopedia surfaces and machine-readable audit logs.
- Added two Infopedia tabs: `PRODUCTION` and `AUDITS`. The production tab renders overview, draft recipe graph, faction variant ledger, chemical equipment ledger, authority ledger, law/contraband status ledger, recipe ancestry samples, laboratory process samples, base production ledger, and dynamic drill-down entries for sample generated recipes and variants.
- Added audit tab renderers for industrial itemization, faction variants, chemical equipment, authority alignment, and production ledger display coverage.
- Kept generated recipes and generated faction variants display/audit/provenance-facing only; the full graph is still not injected wholesale into `CraftingRecipe.all()` or the ordinary workbench UI.
- Production display audit reports PASS with 149 production tab entries, 6 audit tab entries, 682 display-visible draft recipes, 2729 faction variants, 225 chemical apparatus recipes, and 31 lab equipment profiles.
- Regenerated companion logs: `production_ledger_display_audit_0.8.96.txt`, `production_ledger_display_overview_0.8.96.txt`, `production_ledger_display_samples_0.8.96.txt`, `industrial_itemization_audit_0.8.96.txt`, `faction_recipe_variant_audit_0.8.96.txt`, `laboratory_chemical_equipment_audit_0.8.96.txt`, and `authority_alignment_audit_0.8.96.txt`.
- Runtime/save version updated to 0.8.96; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.97 — Live Production Placement / Apparatus Validation

- Added `LiveProductionPlacementAuthority`, `LiveProductionPlacementAuditApi`, and `LiveProductionPlacementAudit`.
- Added buildable laboratory apparatus recipes for Crude chem bench, Reagent preparation bench, Distillation column, Sterile medicae clean bench, Fume hood, Injector filling station, Fungal grow tray bank, and Ritual censer kiln.
- Added `L` laboratory base-object support. Built apparatus receives machine-tier authority and displays process type, knowledge category, room family, manning requirement, and placement notes.
- Added live placement validation to build placement: lab apparatus must be placed in compatible claimed-room/faction contexts and must meet quality-tier expectations.
- Added generated chemical recipe readiness checks against installed apparatus, room validity, machine tier, and worker availability.
- Added live placement readiness lines to the base production ledger and Infopedia entries.
- Added `logs/live_production_placement_audit_0.8.97.txt`; live placement audit reported PASS with 31 lab equipment profiles, 8 buildable lab apparatus recipes, and 225 chemical recipes checked. Runtime/save version updated to 0.8.97; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.8.98 — Controlled Generated Production Job Assignment / Manual Operation Fallback
Added controlled generated/factional production job assignment without flooding the ordinary workbench recipe list. Machines can now store generated variant assignment keys, validate doctrine, machine/apparatus compatibility, installed equipment, quality tier, inputs, and labor context. Added a manual player-operation fallback for eligible machines: when no recruit worker is available, the player can operate the assigned machine personally for deterministic turn and fatigue costs, with outputs routed to base storage and operator provenance logged as manual operation. Added controlled job audit/display lines to the production ledger and standards.

- Added `logs/controlled_production_job_audit_0.8.98.txt`; audit reports PASS.

## 0.8.99 — Controlled Production Job UI / Queue / Input Forecasting

- Deepened the controlled generated/factional production job surface added in 0.8.98.
- Added workbench category filters for generated jobs: all, weapons, chems/medicae, food/water/agriculture, clothing/armor, salvage/maintenance, and energy/power.
- Added workbench status filters for generated jobs: all valid, ready now, missing inputs, needs apparatus, contraband, manual eligible, and crew ready.
- Added `productionQueueTarget` and `productionQueueRemaining` to `BaseObject`; generated job assignment now seeds the machine queue and generated production decrements remaining runs.
- Added `QUEUE -` / `QUEUE +` controls to adjust selected machine queue targets.
- Added input forecasting for generated variants, using base storage plus player inventory counts against consumed recipe inputs.
- Added queue forecasting that reports assignment validity, ready runs, remaining runs, missing input kinds, and crew/manual fallback status.
- Updated base production ledger, workbench panel, and Infopedia production display to show filters, queue counts, and readiness forecasts.
- Updated persistence for base object queue target/remaining fields while preserving older save lines.
- Added `logs/controlled_production_job_audit_0.8.99.txt`; audit reports PASS with 2729 generated variants, 979 apparatus-backed variants, 2729 manual-eligible variants, 2729 forecastable variants, 962 contraband/restricted/status-filter variants, and 14 filter buckets.
- Regenerated companion audits for production display, live placement, recipe graph, faction variants, laboratory chemical equipment, and authority alignment. Runtime/save version updated to 0.8.99; Java 17 compile passed; jar rebuilt; class major version 61.

Next recommended target: **0.9.00 — Facility Capacity / Output Modifier Integration**. Controlled production jobs are now filterable, assignable, forecastable, and queued; the next pass should apply facility throughput, quality, worker capacity, breakdown, room role, and faction-output modifiers to that live job surface.

## 0.9.00 — Facility Capacity / Output Modifier Integration

- Added `FacilityOutputModifierAuthority`, `FacilityOutputRunEstimate`, `FacilityOutputModifierAuditApi`, and `FacilityOutputModifierAudit`.
- Generated/factional production now estimates output count, turn cost, queue throughput, machine wear, breakdown chance, machine throughput, faction efficiency, worker capacity, and machine quality ceiling before materializing output.
- Manual player-operation fallback remains active for eligible jobs and now uses the same facility output modifier authority.
- Crew production benefits from machine tier worker capacity and throughput; high-throughput results can decrement more than one queued run.
- Production provenance and logs now include facility modifier summaries and breakdown-strain audit records.
- Infopedia/Audits gained Facility Output Modifier Ledger and Facility Output Modifier Audit entries.
- Added durable roadmap intent for combat/health/death review, API dissection of the single Java file, deeper population simulation, and component-aware build requirements for machinery/facilities.
- Audit reports PASS; runtime/save version updated to 0.9.00; Java 17 compile passed; jar rebuilt; class major version 61.

## 0.9.01 — Industrial Component Build Requirements / Machinery Cost Refactor

- Added named component-cost support to `BuildRecipe` through `componentCosts`, `withComponent`, and `withComponents`.
- Build tooltips and placement summaries now include component requirements.
- Build validation now checks generic supplies, machine parts, named component availability, stats, workbench, knowledge, faction, and live-placement rules.
- Build confirmation consumes named components from base storage/player inventory through the existing production-input path before consuming abstract supplies and machine parts.
- Componentized the current build recipe set, including storage, workbench, barricades, water barrels, alarms, guard/security fixtures, EMM machines, logistics/cargo fixtures, shop/clinic fixtures, powered defenses, shield relays, and all buildable lab apparatus.
- Added `BuildRequirementCostAuditApi` and `BuildRequirementCostAudit`. Audit reported 30 build recipes checked, 21 machinery/facility recipes checked, 30 recipes with named component costs, 0 generic-only recipes, 32 unique named build components, 0 missing catalog components, 0 bad quantities, and PASS.
- Infopedia Production gained a Build Requirement Cost Ledger; Audits gained a Build Requirement Cost Audit. Production display audit remains PASS.
- Runtime/save version updated to 0.9.01; Java 17 compile passed; jar rebuilt; class major version 61.

Next recommended target: **0.9.02 — Real ItemInstance Input Consumption / Production Container Authority Expansion**.

## 0.9.02 — Real ItemInstance Input Consumption / Production Container Authority Expansion

Implemented a production-container authority layer beneath generated production and componentized construction. Production/build input checks now route through `ProductionContainerAuthority`; consumption prefers concrete `ItemInstance` records from claimed base storage first and player inventory second. `ProductionInputConsumptionRecord` records consumed names, instance IDs, source containers, and provenance summaries, and generated output provenance now carries an input-instance summary. Equipment requirements remain installed/non-consumed apparatus, separate from consumed item inputs.

New audit/display surfaces: `ProductionContainerAuthorityAuditApi`, `ProductionContainerAuthorityAudit`, Infopedia Production → Production Container Ledger, and Infopedia Audits → Production Container Authority Audit. Legacy name-list fallback remains only for migration/parity recovery.

Next planned step: 0.9.03 Persistent Contract / Corpse / Faction Stock Container Expansion, pushing contract retrievals, corpse loot, faction-site stock, fixed room caches, and stock-movement events further into the persistent item/container authority.

## 0.9.03 — Persistent Contract / Corpse / Faction Stock Container Expansion

- Added `PersistentStockContainerAuthority`, `PersistentStockContainerAuditApi`, and `PersistentStockContainerAudit`.
- Added persistent container prefixes for contract objects, corpse loot, and faction stock.
- Fetch-contract sealed objects now seed their required turn-in item into a persistent contract container and retrieve by transferring the concrete `ItemInstance` into player inventory.
- NPC combat death now creates a corpse map object backed by a persistent corpse-loot container. Corpse loot transfers one item instance at a time and removes the marker when empty.
- Contract bounty ident chips now enter the corpse container instead of being a purely instant inventory materialization.
- Faction production-site exports can now materialize into persistent faction stock containers; trader purchase flow can transfer those instances to trader shelves and then to player inventory.
- Added Infopedia production/audit entries for persistent stock containers.
- Runtime/save version updated to 0.9.03; Java 17 compile passed; jar rebuilt; class major version 61; headless audit smoke passed.

Next recommended target: **0.9.04 — Contract / Corpse / Faction Stock UX and Container Pickup Polish**, unless the roadmap pivots back to combat/health review, API dissection, or population simulation.

## 0.9.04 — Contract / Corpse / Faction Stock UX and Container Pickup Polish
Polished the player-facing UX for the persistent stock container authority. Contract-object and corpse-loot markers now present richer interaction previews, including linked container IDs, item counts, next visible item, and provenance summaries. Added interaction-panel **INSPECT STOCK** and **TAKE ALL** actions for persistent contract/corpse stock while keeping CONFIRM as a one-item transfer. Added shared empty-marker cleanup, persistent stock pickup UX ledger/audit surfaces, and audit logs confirming inspect/take-all/cleanup behavior.

## 0.9.04a — Ecclesiarchy / Cult Imperialis Temple Integration

Inserted a bridge pass before 0.9.06 to integrate the missing neutral Imperial civil-religious faction layer. `MINISTORUM` and `SORORITAS` factions now exist. Every generated zone attempts to stamp a Cult Imperialis temple near the central plaza before ordinary corridor/module stamping. The temple is a long/wide nave with pillars, saint alcoves, candle/relic markers, prayer nooks, donation box, and a supplicant kitchen. Temple population adds priests/pilgrims, Sister of Battle guards, and a protected immortal/non-targetable head cleric. The head cleric can always be spoken to and offers a 24-hour Imperial forgiveness prayer service that costs food/water, fatigue, and sleep debt while restoring some civil Imperial standing and lowering suspicion/heat.

Planning note: continue next to **0.9.06 — Actor Access / Permission Checks for Containers and Production** unless deliberately pivoting to queued combat/health review, API dissection, deeper population simulation, or further temple-specific expansion.

## 0.9.04b — Character Creation / Imperial Name Generation

Inserted a bridge pass before 0.9.06 to harden player creation. Candidate stat generation no longer rolls every stat independently between 4 and 11. `CharacterCreationAuthority` now assigns each candidate a shuffled bounded spread of high/mid/low values (`11, 10, 9, 8, 8, 7, 7, 6, 6, 5, 5, 4`) so rerolling can change where strengths land but cannot produce all-high stat lines. Added `CharacterCreationAuditApi` and `CharacterCreationAudit` to check candidate count, name-pool size, stat bounds, and required high/low distribution.

The character creation screen now includes a clickable/editable name box and an `EDIT NAME` button. Name editing accepts letters, spaces, hyphens, apostrophes, and periods; Enter/Escape confirms; Backspace deletes; blank names are replaced with a generated Imperial name before start.

Expanded Imperial-style name generation to 60 first names, 60 last names, and 50 singular/sobriquet names. NPC naming now routes through the same authority, with faction-sensitive titles for Ministorum, Sororitas, Arbites, and Mechanicus NPCs. Runtime/save version updated to 0.9.04b; Java 17 compile passed; jar rebuilt; class major version 61.

Planning note: resume next at **0.9.06 — Actor Access / Permission Checks for Containers and Production**.

## 0.9.04c — Dynamic Music / Combat Audio Manager

Inserted a bridge pass before 0.9.06 to integrate the uploaded Mechanist music pack. Added `DynamicMusicManager` beneath `SoundManager`, converted 29 MP3 tracks to Java Sound-compatible WAV assets, and preserved uploaded voice OGG blips for future conversation/voice routing. The dynamic music layer routes main menu, character creation, current room/zone ambience, standard combat, and sector-governor battle contexts through named playlists inferred from filenames.

Combat music is triggered by player attacks, hostile NPC attacks, hostile intent acquisition, and governor guard spawns. The combat channel persists through a timeout while hostile pressure remains nearby, then fades back into the current ambient zone track. Sector governor hostility uses the dedicated governor battle track. Music transitions use fade-out/fade-in gain ramps, and the existing options music toggle/volume controls now affect the live dynamic music channel.

Verification: `javac --release 17` passed, jar rebuilt, dynamic music audit passed, WAV compatibility smoke test passed. Next target remains **0.9.06 — Actor Access / Permission Checks for Containers and Production**.
## 0.9.06 — Actor Access / Permission Checks for Containers and Production

- Added `Rust CrownP-1.mp3` as the base Sector Governor palace ambience and converted it to `assets/music/wav/Rust CrownP-1.wav`.
- Added `GOVERNOR_PALACE` dynamic music routing for non-combat Governor Mansion ambience while preserving `GOVERNOR_BATTLE` for active governor fights.
- Added `ActorAccessAuthority`, `ActorAccessAuditApi`, and `ActorAccessAudit`.
- Container transfers now pass through route permission checks for player/base containers, room caches, contract objects, corpse loot, trader shelves, faction stock, and void ledgers.
- Direct faction-stock-to-player transfer is denied; faction stock must route through authorized trader or faction interfaces.
- Controlled generated/factional production assignment now checks claimed-room ownership, machine ownership/authorization, and contraband-production cover.
- Added Infopedia Production and Audit entries for actor access permissions.
- Verification: `javac --release 17` PASS; class major version 61; actor access permission audit PASS; production ledger display audit PASS; music playlist audit includes `GOVERNOR_PALACE=1`.

## 0.9.06 — Combat / Health / Unconsciousness / Death Review
- Player defeat flow is now centralized: body damage, attrition, bleeding, infection, pain, fatigue, and sleep collapse can resolve into unconsciousness or death.
- Unconsciousness sends the player to their claimed base if available, otherwise the nearest Medicae/clinic facility, otherwise a safe fallback point.
- Death opens a YOU LOST screen showing killer, method/weapon, fatal cause, location, turn survived, and a weighted run score.
- Score categories include kills, things crafted/built/produced, knowledge learned, sectors/zone types seen, zone instances visited, NPCs talked to, wealth, rooms owned as bases, recruits, XP, and unconsciousness penalty.
- Autosave now writes to saves/autosave.mechsave every 500 active turns and manual saves support six slots. The loss screen does not overwrite autosave.
- Next preserved roadmap target: continue post-combat roadmap review and then resume the queued API dissection / population simulation tracks as planned.

## 0.9.06a — Save Menu / Autosave Column / Death Load Flow

This bridge pass corrects the death/load flow and save presentation after the 0.9.06 defeat-screen work. The You Lost screen now exposes a LOAD SAVE action that opens the save/load panel rather than directly loading autosave or manual slot 1. Manual saves are intentionally limited to four slots. Autosaves are displayed as their own column with separate hourly and zone-transition save files. Autosaves run every in-game hour and after completed zone transitions, while the death screen does not overwrite them. Save summaries now report character name, world file, seed, turn/time, sector/zone/floor, zone name, and save timestamp.

## 0.9.06d — Tile Icon Rendering / Art Asset Rebase Intake

Integrated the uploaded `art.zip` bundle as the first runtime tile-icon rendering pass. Original source art is preserved under `assets/art/rebase_0_9_06d/source/`, while deterministic 5x5 sheet slices are normalized into 64x64 runtime cells under `assets/art/rebase_0_9_06d/tiles/cells/`. The runtime manifest records sheet source paths, original dimensions, normalized grid metadata, and semantic aliases for glyph-to-art mapping.

Added `TileArtSystem` beneath `ImageCache` and wired 75 existing placement glyphs to tile images. The game map now defaults to tile-icon rendering when assets are available while preserving the ASCII fallback and a display-options toggle. Visible and remembered terrain still obey the existing fog-of-war model. Claimed/faction-owned rooms receive light ownership tinting over floor tiles, matching the earlier room/character ownership-color language.

Player map rendering now uses the selected character portrait as the on-map player icon when imported portraits are enabled. NPCs use portrait cells with light faction-color tinting so affiliation remains legible at map scale. The new CRT-green portrait sheets from the art pass are loaded into the NPC portrait pool for later organization-specific refinement.

Title, backdrop, cloud-layer, and intro-crawl assets are preserved for a later New World intro/start sequence. That later pass should use the backdrop plus slow/fast cloud overlays and the supplied crawl text after the tile renderer is stable. A future lighting/sensory pass is also documented: distributed light sources should affect vision range, and noisy/anomalous machinery should affect the hearing environment rather than only acting as static room flavor.

Verification: `javac --release 17` PASS, jar rebuilt, class major version 61, tile-art smoke PASS with 75 glyph mappings, 30/30 sampled glyph checks, 475 NPC portrait cells, and rebase title asset present. Runtime/save version updated to 0.9.06d.

## 0.9.06e — Deterministic Graphics Quality / Semantic Icon Disambiguation

Added a graphics-quality pass on top of the 0.9.06d art rebase. The preserved 5x5 art sheets now produce four deterministic runtime caches: `low_32`, `standard_64`, `intermediate_128`, and `high_native`. Each tier contains the same 800 tile/portrait cells, allowing antiquated hardware to load smaller source images while stronger machines can load native proportional crops from the original imported sheets. The standard tier keeps the prior 64x64 normalized cells as the default.

Added persistent Display options for `ART QUALITY` and `TILE SIZE`. `ART QUALITY` controls which tile/portrait source cache is loaded, while `TILE SIZE` controls the on-screen map footprint independently: compact 24px, normal 28px, intermediate 40px, and high 64px before GUI scaling. Existing ASCII fallback and the tile-art on/off toggle remain intact.

Added deterministic semantic icon generation for overloaded glyph cases. This does not replace the save/grid characters. Instead, built base objects can render from a semantic key derived from their object name, such as `build_crude_chem_bench`, before falling back to the old glyph icon. This begins resolving collisions such as `L` meaning both locked door and lab apparatus, or `B` meaning both emergency boiler and shop counter. Seventeen semantic icons are generated per quality tier as the first pass.

Added `tools/generate_art_quality_variants.py` so the quality caches and small semantic icons can be regenerated from the source art using Pillow/PIL. Verification: `javac --release 17` PASS, class major version 61, art-quality smoke PASS across all four tiers with 75 glyph mappings, 17 semantic icons, and 475 rebase portrait cells per tier.

## 0.9.06f - Global In-Game Viewport Zoom

Implemented a deterministic tactical viewport zoom layer for the tile-art transition. The visible world now uses a persistent `worldZoomIndex` separate from source-art quality and base map tile size. Zoom tiers are FAR 70%, WIDE 85%, NORMAL 100%, CLOSE 125%, INSPECT 150%, and MAX 200%. Mouse wheel zooms the active world view, Home zooms in, and End zooms out. The behavior applies to the ordinary character/world view and to the world-backed overlay modes used by look, interact, scavenge, build, and workbench placement; the combat view benefits because combat remains on the same tactical world viewport.

Preserved existing panel behavior where it mattered: non-world panels continue to scroll normally, and Shift+mouse wheel remains available for the diegetic sector-map layer control. Controller bumpers are documented as reserved for viewport zoom once native controller polling is actually implemented; no false live gamepad binding was added.

## 0.9.06g — Modular Art-Pack Loader / New World Intro Crawl / Package Slimming

Added a modular asset-pack path to prevent the runnable build from ballooning every time a new art pass is imported. Runtime art can now live in ZIP files dropped into `assets/artpacks/`. On launch, the game unpacks those packs into `cache/artpacks/` and resolves a root containing `tiles/quality/` plus `source/Title/`. If no external art pack is present, the game falls back to bundled art if available, then to ASCII/generated UI. This keeps ASCII fallback intact and avoids making the core Java package dependent on huge image folders.

Retained the title art, subtitle art, New World backdrop, slow/fast cloud images, and intro-crawl text as runtime art-pack assets. The original raw tile source sheets under `source/TILES/` are no longer required in the runnable package because the game consumes generated quality caches instead. The raw source sheets remain build/reference material for future art regeneration, not core runtime material.

Implemented the New World insertion crawl after character finalization and world generation. The sequence uses the imported hive-city background, scrolls the supplied crawl text upward, and layers the slow and fast cloud images across the top of the backdrop at different speeds. The sequence can be skipped with Enter, Space, Escape, or mouse click. After the crawl it proceeds to the first-impression zone splash when applicable, otherwise to ordinary game mode.

Added an equivalent audio-pack resolver for `assets/audiopacks/`, allowing large WAV music folders to be retained as optional external ZIPs instead of being forced into the core build. Dynamic music remains silent if no music pack or bundled `assets/music/wav/` folder is present.

Verification: `javac --release 17` PASS, jar rebuilt, class major version 61. Runtime package now supports small core distribution plus external art/audio ZIP packs.

## 0.9.06h — Split Built-in Low Graphics and Optional Art Tiers

- Corrected the art-pack split so the core build now includes the deterministic LOW 32 tile/portrait cache as the guaranteed built-in graphical fallback.
- Preserved title art, subtitle art, city backdrop, slow cloud layer, fast cloud layer, and intro crawl text inside core so the title screen and New World intro crawl do not require a high-resolution art pack.
- Converted higher-resolution art into optional overlay packs: STANDARD 64, INTERMEDIATE 128, and HIGH NATIVE.
- Updated art-pack resolution so optional packs can contain only one quality tier rather than duplicating title/background/source art.
- If the selected art tier is absent, the renderer now falls back to bundled LOW 32 before falling back to ASCII.
- This keeps old hardware viable while still allowing stronger machines to install higher-resolution visual tiers.

## 0.9.06i — Core Documentation Authority / Near-Term Systems Planning Sweep

Updated the durable project documentation without changing gameplay code or JAR bytecode. This pass corrects stale 0.9.06c/0.9.06g/0.9.04c references in the README, run instructions, master development plan, and new-conversation briefing so the current core base is accurately described as the 0.9.06h runnable package with 0.9.06i documentation authority updates.

Recorded the next near-term systems requirements: two actors may not occupy the same tile; player movement into an occupied creature tile should resolve through shove/pushback/contact logic rather than stacking; abandoned/sewer spaces need more wild creatures; noble districts should use the servant/chef/butler/household portrait assets for real servant populations; ranked creatures should gain upper-right rank symbols plus rank-colored frames/borders; and future rank overlays must remain readable at LOW 32 while allowing higher-resolution art-pack versions.

Added the safe core code-split plan to the master planning and standards documents. The recommended first extractions are assets, UI helpers, options, and logging infrastructure. High-risk world, economy, production, save/load, population, and combat splits remain deferred until their authority boundaries are stable.

Verification: documentation-only pass. The 0.9.06h JAR remains the runnable core; no source or bytecode behavior was changed.

## 0.9.06j — Occupancy and Shove/Pushback Foundation

Implemented the first concrete build segment after the 0.9.06i documentation sweep. Player movement now checks for NPC occupancy before entering a target tile. If the target tile contains a protected head cleric, movement is blocked; if it contains a hostile actor, the actor body-checks/attacks rather than allowing the player to stack; if it contains a neutral/friendly actor, the game attempts to shove that actor into a legal adjacent tile before moving the player in. Failed shove attempts deny movement with explicit feedback.

Added NPC occupancy repair to clean stacked NPCs or NPCs that end up on the player tile after generation, older saves, replacement arrivals, or simulation edge cases. Hostile NPC pursuit now avoids stepping directly onto the player tile, preserving adjacent combat contact instead of overlap. Room random-open-point fallback now refuses occupied center/fallback tiles rather than quietly generating stacked residents.

This pass deliberately does not implement wildlife expansion, servant portrait placement, rank overlays, or the broader population simulation. Those remain next work items. The next recommended implementation remains semantic render keys and rank overlay foundation, because the renderer now needs entity identity and rank symbology on top of the improved occupancy rules.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, `OccupancySmoke` PASS. Runtime/save version updated to 0.9.06j.

## 0.9.06k — Push-Past, Two-Wide Corridor Preference, and NPC Emergency Repath

Extended the 0.9.06j occupancy stage before moving on to rank overlays. Friendly/neutral blockers still use ordinary shove first. If no legal shove destination exists in a one-wide choke, the same directional movement attempt now arms and then executes a high-time-cost push-past swap, moving the player through the blocker while moving the NPC into the player's old tile. Protected head clerics remain immovable and hostile NPCs still body-check/attack.

Added a conservative world-generation corridor widening pass after transition repair. It prefers two-wide corridor travel where anonymous wall mass permits, but never cuts through rooms, doors, machines, special infrastructure, or void. One-wide corridors remain legal fallback architecture.

Strengthened NPC invalid-position recovery. NPCs found on walls, non-traversable doors, void, the player tile, or stacked creature tiles now emergency-repath to the nearest unoccupied walkable tile with audit logging.

Updated corridor art handling so imported north/south corridor cells rotate on east/west corridor topology. This is a renderer-side correction, not a gameplay-grid change. Future art manifests should tag corridor cells by orientation.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, `OccupancyPushPastSmoke` PASS. Runtime/save version updated to 0.9.06k.

## 0.9.06v — INN Journal Bounties / Banking Foundation

Implemented a first playable financial layer focused on core-only development: stolen faction leader journals can be lifted from faction journal objects and sold to the Imperial News Network for major bounties, with rank-one leader journals paying about 5000 Imperial Script. The inventory panel now exposes carried cash, banked cash, and base-stashed cash at the top level. Three noble-aligned banks now exist as world terminals/branches: SumpLedger Mutual Credit under House Toll for lower-hive high-risk ATM coverage, Kastor Civic Credit for mid-hive civilian service, and Varn Crown Trust under House Varn for secure upper-hive branch banking. Accounts have opening fees, deposits, withdrawals, ATM/branch fees, persistence, and world placement. Bank heist content is scaffolded through bank manager keycards, sealed lockboxes, stock certificate bundles, and House gold ingots; full vault intrusion, alarms, noble quests, and heist consequences remain future work. Next queued build after this financial pass is semantic render keys and rank overlays.

## 0.9.06y — Arbites Custody Death Threshold

Implemented fatal police-custody outcome: repeated Arbites capture beatings can reduce stats to 0. If this occurs, the player dies in custody and the death screen records the Arbites custody cause.

## 0.9.07d — INN Broadcast / Bar News Service Foundation

Implemented the first playable broadcast extension for the Imperial News Network. INN/civic/rail/hab/bar/noble/guard/law spaces can now seed broadcast receiver objects (`broadcast-device`) using radio, pict-screen, or bar-vox profiles. Interacting with them produces a short INN bulletin drawn from faction strategic plans and public discovery chance. The bulletin is deliberately shorter and less detailed than a printed newspaper, while fresh and old papers remain the fuller record.

Portable `Radio set` and `Public pict-screen tube` inventory items can now be used to monitor INN bulletins. Broadcast report persistence was added through `inn.lastBroadcastReport`, and the faction strategy panel now displays the latest broadcast alongside the public news seed and latest daily issue.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, broadcast smoke PASS, source MP3 folder absent, core ZIP integrity PASS.

## 0.9.07e — INN Editorial Bias / Player News / Authority Inspection Hooks

Refined the INN newspaper and broadcast layer so reports vary by faction family. Noble, Arbites, military, civil, hiver, and Mechanicus factions now have distinct editorial framing, while gangs, mutants, cultists, heretics, and bandits are reported through criminalized outside-observer/public-safety language.

Added a persistent player-news event ledger. Public-worthy player actions now include prominent faction-member kills, room claims, facility construction, service/business openings, and generated production assignment. INN issues and broadcasts can surface these events within roughly one to two in-game days as a small part of the broader news mix. Added authority facility-inspection hooks so public reporting in civil/noble contexts can trigger permit/license checks, fines, suspicion, and heat pressure with cooldowns.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, INN editorial/player-news smoke PASS, source MP3 folder absent, core ZIP integrity PASS. Runtime/save version updated to 0.9.07e.
## 0.9.07f — Bank Heist / Vault Foundation

Implemented the first bank-heist/vault gameplay layer. Bank branch offices now seed vault fixtures and bank manager NPCs. Bank managers can be targeted for keycard theft. Bank vaults can be opened with a manager keycard, Secure vault key, Data spike, Lockpicks, hacking tools, or strong Intellect/Agility checks. Success grants tracked Imperial Script and physical vault loot such as Sealed bank lockbox, Stock certificate bundle, House gold ingot, Noble Commerce Permit, Secure vault key, Trade chit, or Bank manager keycard depending on bank tier. Failed and successful intrusion raises suspicion, bank alarm cooldown pressure, authority-inspection pressure, and player-news events for delayed INN reporting.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, ZIP integrity PASS. Runtime/save version updated to 0.9.07f.

## 0.9.07g — Bank Alarm / Vault Guard / Lockbox Quest Refinement

Refined the bank-heist layer with bank alarm panels, tamper checks, bank/vault guards, bank manager schedule states, explicit vault state reporting, local alarm response, and noble-bank lockbox retrieval contracts. Failed tamper/keycard/vault actions raise suspicion, bank alarm pressure, authority inspection pressure, and guard alarm posture. Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, ZIP integrity PASS. Runtime/save version updated to 0.9.07g.

## 0.9.07h — Portable Light Items / Dropped and Thrown Light Foundation

Inserted the portable-light item layer ahead of the full lighting/noise metadata pass. Added catalog entries for Flashlight, Glow stick, Lantern, Stub light, Mining helmet, Scavenging helmet, Electrician's rig, Phosphor bulb, and Swamp lantern. These items now have first-stage light profiles with radius, duration, throw range, wearable/consumable flags, and notes for future lighting-system integration.

Inventory USE activates a selected light as carried or worn illumination; DROP leaves an active light on the floor; THROW LIGHT tosses the selected light in the current facing direction and leaves it as a temporary world-space light. Active carried/worn lights affect the player's immediate visibility, and dropped/thrown lights illuminate nearby tiles with line-of-sight checks. The state persists through save/load using `light.activeItem`, `light.activeExpires`, `light.activeWorn`, and `light.instances`.

Room loot/scavenge pools now seed stub lights, glow sticks, flashlights, electrician rigs, swamp lanterns, and organic phosphor bulbs in appropriate trash, maintenance, sewer, abandoned, and swamp/fungal contexts. This pass deliberately does not implement the final room/corridor lighting generator, wall-switch logic, power dependency, flicker gradients, or cached global light/noise fields. Those remain the next lighting/noise metadata phase.

Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, source MP3 folder absent, source smoke checks PASS, ZIP integrity PASS. Runtime/save version updated to 0.9.07h.

## 0.9.07i — Zone Lighting / Noise Metadata Foundation

This build adds the first systemic zone-lighting and environmental-noise metadata layer. Generated rooms and corridors can now receive light fixtures with zone-dependent profiles for chance, radius, intensity, color, flicker, power state, and switch control. Local light switches can toggle linked fixture groups. The SENSES panel now reports zone fixture and noise-source counts. Machinery, broadcast receivers, shop/vending points, alarms, and select fixtures seed noise-source records used by hearing checks and local noise penalty calculations. This is the metadata and first visibility/hearing integration pass; the full cached light-field renderer and deeper darkness presentation remain queued for the next lighting pass.

## 0.9.07j — Environmental Hazard / Noble Estate Planning Update

Documentation-only planning pass. No Java bytecode changed from 0.9.07i. Added durable planning and standards for environmental hazards and noble-estate compound generation before proceeding further into light rendering and darkness/stealth interaction.

Recorded hazard families: noxious gas, poison fumes, caustic sewer sludge, shorted wires, jagged squeeze passages, overheated factory floors, freezer rooms, torture/interrogation rooms, tripwires, mines, blade traps, shock traps, pressure plates, smoke, and other zone-specific dangers. Hazards should become profile-driven records with exposure rates, sensory warnings, protective equipment hooks, damage/stat effects, save/load state, and entity/NPC interaction.

Recorded noble-estate direction: upper noble districts should eventually place large stamped estate compounds with at least a dozen variants over time. Estate stamps should include servants, kitchens, studies, bedrooms, safe rooms, hidden compartments, dogs/pets, guards, patrols, alarms, traps, security switches, vault/safe objects, and grimly absurd aristocratic murder-fixtures where appropriate.

Next recommended sequence: light rendering/darkness visibility, environmental hazard metadata, noble estate stamp foundation, trap/booby-trap interaction foundation, noise/hearing field pass, then safe infrastructure code split.

## 0.9.07k — Light Rendering / Darkness Visibility Pass

Implemented the first visible lighting pass over the 0.9.07i lighting/noise metadata. Added a per-tile light-level cache, ambient zone light baselines, fixture/portable light contribution, flicker-aware recalculation, visible darkness/dim tile shading, LOOK/SENSES light readouts, darkness penalties for ranged targeting, NPC ranged fire, hostile detection, and intent reads. ASCII fallback remains supported. Verification: `javac --release 17` PASS, JAR rebuilt, class major version 61, lighting render smoke PASS, ZIP integrity PASS. Runtime/save version updated to 0.9.07k.
## 0.9.07l — Environmental Hazard Visibility / Warning Overlay Foundation

Implemented a saveable environmental hazard warning metadata layer for world zones. The new `EnvironmentalHazardRecord` and `EnvironmentalHazardVisibilityApi` seed gas, caustic sludge, shorted-wire, and thermal hazard warning records after world generation. The map renderer now draws vector fallback hazard warnings over visible/remembered danger tiles: a red border, corner warning triangle with `!`, and three-state animated family tells for coiling gas, surging sludge, crackling wires, and temperature shimmer/fog. LOOK/SENSES text now reports hazard labels, families, severity, and warning descriptions. Save/load now preserves `world.hazardWarnings` and the hazard visibility summary.

Build: `javac --release 17` succeeded and `TheMechanist.jar` was rebuilt. Runtime GUI smoke could not launch inside the headless container because no X11 display was available.

## 0.9.07n — Trap / Booby-Trap Interaction Foundation

Implemented a first-pass trap and booby-trap metadata/interaction layer on top of the 0.9.07m noble-estate stamp foundation.

- Added `TrapRecord` metadata with hidden/revealed/disarmed/triggered states.
- Added `TrapInteractionApi` world-generation seeding for pressure plates, tripwire alarms, gas-release traps, and electrified floor traps.
- Linked trap density and ownership to noble/governor/upper-hive contexts.
- Added save/load persistence for trap records and trap summary text.
- Added player movement trigger checks with detection before triggering.
- Added trap noise-source propagation hooks when traps fire.
- Added first visual trap-warning overlay for revealed or detected traps.
- Added damage hooks through the existing body-part damage system.

Build: `javac --release 17` succeeded. JAR rebuilt.

## 0.9.07o — Noise/Hearing Field Pass

Implemented the first cached noise/hearing field layer on top of the light, hazard, noble-estate, and trap foundations. Worlds now maintain a per-tile `noiseField` cache with turn-based dirty tracking and a `hearingFieldSummary` exposed in the SENSES panel. Hearing penalties now read from the cached field rather than recalculating every machinery/noise source every time `noisePenaltyAt` is queried.

`NoiseHearingFieldApi` blends machinery drones, tile machinery, broadcast/shop/alarm devices, environmental hazards, and trap mechanisms into a per-turn field. Propagation now applies simple wall/interstitial muffling so sound bleeds through the map with penalties instead of acting as an unshaped radius. Environmental hazards add appropriate sound signatures: crackling wires, surging sludge, coiling gas hiss, and thermal machinery wash. Trap records add quiet armed-mechanism or taut-wire ticks where appropriate, while triggered traps can still emit louder event pings through the existing trap consequence path.

Build: `javac --release 17` succeeded. JAR rebuilt. GUI runtime smoke could not launch in the headless container because no X11 display was available.
## 0.9.07p — Safe Core Code Split

Performed a conservative source split intended to improve future editing efficiency without altering gameplay behavior. `TheMechanist.java` still contains the public launcher and the large `GamePanel` runtime, but stable support surfaces were extracted into dedicated files under `src/mechanist/`: door/zone/faction enums, clothing, logging, ecclesiarchy temple API, stamped-room/module APIs, room manifest/profile support, inspectable feature tables, and noble-estate stamp definitions.

What changed: smaller source authority surfaces, a standalone `DebugLog.java`, standalone world/room/faction metadata files, and a runtime version bump to 0.9.07p. What deliberately did not change: game rules, trap damage execution, hearing propagation behavior, hazard exposure damage, save format semantics, or GUI flow.

Build: `javac --release 17` succeeded. JAR rebuilt. ZIP integrity passed. GUI runtime smoke could not launch in the headless container because no X11 display was available.

## 0.9.07w — Faction Representative Bar / Leadership Fallback Pass
- Added a recognizable faction-aligned representative bar stamp near level-transition infrastructure where placement is legal.
- Bars include a long counter, stools, refrigerator/cooler, service keg machinery, cheap radio, pict/news viewer, and open patron space.
- Seeded protected faction representatives inside continuity bars.
- If the faction controls no other rooms, the representative is promoted to provisional rank-one local leadership.
- If the faction controls another room, a rank-one faction leader is seeded into a non-bar controlled room as the local base of operations.
- Preserves the 0.9.07v continuity rule that factions are not permanently erased and replacement demand must stay bounded by current room capacity.

## 0.9.07x — Integration/API Efficiency Development Target Pass

Implemented a conservative integration-planning surface rather than a risky gameplay rewrite. Added `IntegrationEfficiencyApi` as a side-effect-free catalog of future authority boundaries, execution lanes, dirty cache families, and result-object conventions. This gives upcoming optimization passes a named target for routing actor movement, NPC batch thinking, abstract distant-zone simulation, faction continuity refreshes, cache invalidation, provenance indexing, render/audio cues, localization lookups, and future editor validation.

The pass also bumps runtime/save identity to 0.9.07x and records that optimization should proceed through explicit authority endpoints instead of scattered direct edits inside the monolithic panel logic.

## 0.9.07y — NPC Turn Budget Scheduler Pass

Implemented a conservative rotating NPC turn-budget scheduler. The previous local NPC guard capped active work but still favored the early list order. The new scheduler selects close dangerous actors first, then nearby priority actors, then a rotating fair slice of the active-zone bubble. Far and budget-limited actors are recorded as deferred simulation debt instead of being processed tile-by-tile immediately.

This pass adds `NpcTurnBudgetScheduler`, updates runtime identity to 0.9.07y, adds scheduler profiling/audit fields, and documents the lane model in `docs/NPC_TURN_BUDGET_SCHEDULER.md`. It does not intentionally change NPC combat rules or movement authority; `GamePanel` still applies authoritative mutations.

Next recommended target: **0.9.07z — Abstract Distant Zone Simulation**, converting offscreen population/faction activity into ledgers rather than dormant individual tile actors.

## 0.9.07z — Abstract Distant Zone Simulation Pass

Implemented the first safe distant-zone ledger layer. Added `AbstractDistantZoneSimulation`, a conservative abstract simulation surface that spends deferred simulation debt on compact zone ledgers rather than leaving the budget loop as a no-op or forcing every far NPC to run full tile logic.

The active zone remains authoritative and grid-based. The new ledger records distant NPC/faction pressure, patrol drift, production drift, unrest, estimated population, estimated room count, and a small bounded event tail. `GamePanel.spendDeferredSimulationBudget` now routes work through the ledger surface and audits `ABSTRACT_DISTANT_ZONE` periodically. Simulation-efficiency audits now include abstract tick/work totals.

This pass does not remove actors, run offscreen combat, rewrite faction ownership, or thread world mutation. It is the foundation for future offscreen economy, patrol, population, and provenance simulation while preserving local responsiveness on old hardware.

Build: `javac --release 17` succeeded. JAR rebuilt. ZIP integrity passed.

## 0.9.08d — Master Semantic Needs Index

Completed Phase 1 needs-side indexing for art/entity redistribution. Added `docs/MASTER_SEMANTIC_NEEDS_INDEX.md`, `docs/MASTER_SEMANTIC_NEEDS_INDEX.json`, and `src/mechanist/MasterSemanticNeedsIndex.java`. The index records current zone/faction/room/build/crafting/map-object/inspectable surfaces and defines semantic families for waste/newsprint, medicae, chemistry/labs, forge machinery, retail/vending, food/water/agriculture, security/traps, logistics/vehicles/rail, faction continuity, and domestic/hab/noble service fixtures. No gameplay objects were promoted yet; this pass intentionally prepares safe allocation and parity planning.

## 0.9.08g — Art Unification / Efficiency Utilization Stage 1

- Added the high-durability efficiency/modularization/core-size standard.
- Promoted selected low_32 placeholder/base art into semantic runtime keys without copying higher-resolution tiers into the core build.
- Added map-object semantic art routing for INN newspaper dispensers, old newspaper sources, broadcast receivers, bank terminals/vaults/alarm panels, light switches/fixtures, planted explosives, public service counters, governor/public info fixtures, and faction journals.
- Added `AssetUnificationEfficiencyUtilization.java` and durable docs for the next staged fixture redistribution work.

## 0.9.08j — Phase 3 Room Integration II / Road Adjacency Foundation

Implemented the second road-integration foundation. Roads now gain adjacent civic/service structures rather than existing only as four-wide corridors: sidewalk tiles are explicitly treated as corridor-locatable routing targets, road-facing alcoves and foot-traffic alley cut-throughs are carved where safe, parking-lot/set-down bay placeholders are seeded beside roads, taxi/toll booth placeholders are placed near parking infrastructure, and park/open-space placeholders are staged as future road-attached civic rooms.

Vehicle gameplay is intentionally not complete in this pass. New vehicle hooks are map-object placeholders with semantic art routing for car, truck, bike, and armored-car assets. Future vehicle work must add actual vehicle entity records with health, armor, seats, cargo, ownership, boarding/disembark tiles, weapons for armed vehicles, recall/taxi UI, pathing rules, and faction/NPC use.

Build: `javac --release 17` succeeded. JAR rebuilt. ZIP integrity passed.

## 0.9.08n — Phase 4 Interaction Expansion III / Fixture Registry Consolidation

- Added shared `FixtureInteractionRegistry`.
- Catalogued fixture families for waste/news, benches, broadcast, civic service, medicae, laboratory, forge, faction bars, road/transit, and hazards.
- Added shared cooldown/sound/future-gate definitions so fixture behavior does not sprawl into duplicate per-object systems.
- Kept Phase 4 bounded as interaction/readability/service-preview work only.
- Preserved low_32-only core art discipline.

## 0.9.08o — Phase 5 Industrialization I / Constructible Infrastructure Promotion

- Began Phase 5 industrialization using shared registry surfaces instead of one-off fixture systems.
- Added `InfrastructurePromotionRegistry.java`.
- Promoted selected medicae, lab, chemical, and forge fixtures to constructible/operable infrastructure targets.
- Reused existing `BuildRecipe` definitions and connected them to decomposition classes, knowledge gates, quality floors, and player/faction parity lanes.
- Preserved low_32-only core build policy and did not reintroduce high-resolution art into the core archive.

## 0.9.08p — Phase 5 Recovery Briefing / Operation Queue Plan

- Refreshed `NEW_DEVELOPMENT_CONVERSATION_BRIEFING.md` to remove stale recovery clutter and center the latest Phase 5 state.
- Added `InfrastructurePromotionRegistryIPlan.java` as a lightweight planning surface for upcoming machine operation queues.
- Added `docs/PHASE_5_INDUSTRIALIZATION_II_OPERATION_QUEUE_PLAN.md`.
- Preserved low_32 core-art containment and did not add new large assets.
- No live production behavior was intentionally added in this pass.

## 0.9.08q — Governance Integration / Shared Operation Queue Scaffold

- Added `docs/MASTER_GOVERNANCE_REVISION_II.md` and bundled `docs/The_Mechanist_Master_Governance_Revision_II.docx` inside the project archive.
- Condensed the useful objectives of the temporary design assistance directives into durable master planning and standards guidance; the source directives zip is intentionally not included.
- Updated `docs/STANDARDS_AND_PRACTICES.md` with framework consolidation, machine operation queue, and simulation tier standards.
- Updated `docs/MASTER_DEVELOPMENT.md` with the 0.9.08q pass record and the next target of 0.9.08r UniversalWindow/UI cohesion.
- Added `src/mechanist/MachineOperationQueue.java` as the first shared operation authority scaffold.
- Registered Phase 5 operation targets from `InfrastructurePromotionRegistryIPlan` into shared operation profiles.
- Added stable queue states: QUEUED, PENDING, ACTIVE, PAUSED, INTERRUPTED, BLOCKED, FAILED, COMPLETED, and ABANDONED.
- Added conservative queue processing hooks to the turn loop without creating duplicate medicae/lab/forge/chemistry mini-systems.

## 0.9.08t — Construction Governance Authority Foundation

- Added `src/mechanist/ConstructionGovernanceAuthority.java`.
- Added canonical construction metadata categories: room roles, utility tags, and validation tags.
- Added room governance specs for hab, medicae, laboratory, forge/workshop, road frontage, and utility rooms.
- Added blueprint governance specs for basic room stamps, machine placement, hazard features, road/parking hooks, and future tutorial/hint anchors.
- Hooked construction governance audit output into startup and first-turn framework audits.
- Updated runtime/save identity to 0.9.08t.
- Added `docs/CONSTRUCTION_GOVERNANCE_AUTHORITY_0.9.08t.md`.
- Updated standards and master planning to make construction validation a shared authority layer.
- No full construction rewrite, utility-network simulation, multiplayer runtime, or heavy per-frame validation was added in this pass.

## 0.9.08u — Construction Placement Governance Bridge

- Advanced runtime/save identity to 0.9.08u.
- Updated `ConstructionGovernanceAuthority` to 0.9.08u.
- Connected active build placement to construction governance result explanation.
- Added lightweight blueprint-family classification for existing build recipes.
- Routed build-placement planning through the UniversalWindow construction authority.
- Added player-facing construction governance advisory text during placement planning.
- Connected blocked confirm-build attempts to the construction window focus path.
- Preserved legacy placement behavior while standardizing validation language.
- Avoided render-loop governance spam by keeping map-preview tinting on raw placement checks.
- Recompiled successfully.

## 0.9.08v — Machine Operation Status Bridge

- Advanced runtime/save identity to 0.9.08v.
- Added `src/mechanist/MachineOperationStatusBridge.java`.
- Added `docs/MACHINE_OPERATION_STATUS_BRIDGE_0.9.08v.md`.
- Connected the Workbench panel to the `UniversalWindowAuthority` machine-operation surface.
- Added bounded player-facing machine-operation status lines showing shared queue counts, selected machine assignment, existing production queue values, and readiness/blocking status.
- Added startup/audit logging for the new bridge.
- Preserved all existing production outcomes, input consumption, output creation, manual operation costs, recruit production behavior, and construction rules.

## 0.9.08w — Production Queue Record Bridge

- Advanced runtime/save identity to 0.9.08w.
- Added `src/mechanist/ProductionQueueRecordBridge.java`.
- Added `docs/PRODUCTION_QUEUE_RECORD_BRIDGE_0.9.08w.md`.
- Extended `MachineOperationQueue` with bounded external completion records.
- Recorded successful workbench actions into shared queue history.
- Recorded manual crafting and manual machine production completions into shared queue history.
- Recorded recruit production completions into shared queue history.
- Recorded generated production completions into shared queue history.
- Updated `MachineOperationStatusBridge` to report bridge-backed queue history.
- Preserved all existing production outcomes and avoided adding render-loop or map-scan overhead.
- Recompiled successfully.

## 0.9.08x — Queue History Persistence / Physical Defense Planning Note
- Added bounded queue-history save/load support.
- Added recent queue-history display to the machine-operation status surface.
- Preserved legacy/manual production as source of truth.
- Added physical in-world map-defense doctrine to long-term planning: defenses should exist as real map objects when locally relevant, with distant-sector abstraction only used as a performance ledger.
- Recompiled and rebuilt TheMechanist.jar successfully.

## 0.9.08z — Defense Art Base Package 5 Indexing Pass

- Imported `Defenses.zip` as source art material.
- Sliced seven 5x5 defensive atlases into 175 normalized cells.
- Exported bundled low_32 runtime cells under `DefenseArtBase5`.
- Exported 58 selected semantic defense/precinct icons.
- Created optional `The_Mechanist_Base_Package_5_Defenses.zip` art-pack with low_32 and standard_64 tiers.
- Added `DefenseArtBase5Index` data-only authority.
- Updated `DefensiveAssetGapRegistry` to recognize newly available sandbag, razor wire, sensor, gate, and turret/heavy weapon art.
- Preserved current doctrine: no active turrets, no target loops, no new map scans, no construction rewrite.

## 0.9.09a — Defense Semantic Integration

- Added DefenseSemanticIntegration as the authority connecting defensive art assets to semantic entity profiles.
- Added construction recipe scaffolds for sandbags, razor wire, reinforced wall panels, security sensors, Arbites reinforced doors, light/heavy/Arbites/Noble turret profiles, and precinct defensive fixture sets.
- Added decomposition classes, faction bindings, infopedia topics, and future combat-stat records for defensive entities.
- Added named construction components for defense production chains.
- Extended construction governance blueprint families for defensive obstruction, defensive wall, area denial, sensors, access control, turrets, and precinct fixtures.
- Turret automation remains intentionally deferred until ownership, ammo, power, staffing, hostility, save/load, and multiplayer-safe rules are implemented.

## 0.9.09b — Defense Build Selection / Infopedia Bridge

- Added `DefenseBuildSelectionAuthority`.
- Added direct build-menu entry points for sandbags, razor wire, reinforced walls, Arbites doors, sensors, and light turret profiles.
- Added a dedicated Infopedia `DEFENSES` tab.
- Exposed defense semantic profiles, faction bindings, recipe names, blueprint families, decomposition classes, and combat-stat hooks through Infopedia detail lines.
- Expanded built-object configuration for sandbags, wire, reinforced wall panels, sensors, Arbites doors, turret profiles, and precinct fixtures.
- Kept autonomous turret targeting disabled. No target scans, projectile logic, ammo consumption, power network behavior, or live hostile-fire rules were added.
- Recompiled successfully.

## 0.9.09c — Passive Defense Effects Bridge
- Added `PassiveDefenseEffectsAuthority` for table-driven passive defense behavior.
- Defensive objects now expose cover, raid-resistance, and inspection semantics.
- Ranged cover calculations now consume passive defense cover profiles.
- Base security rating now consumes passive defense resistance profiles.
- Infopedia now includes a Passive Defense Effects entry.
- Live turret targeting remains intentionally dormant.
- Recompiled and rebuilt successfully.

## 0.9.09d — Construction Access / No Self-Entombment Safety Pass
- Added `SelfEntombmentConstructionAuthority`.
- Connected it to build placement validation.
- Blocks defensive/machine placements that would seal the player away from a room door or exit.
- Preserves existing placement rules and production outcomes.
- Kept validation bounded to placement checks only.

## 0.9.09e — Construction Feedback / Efficiency Pass

- Added `ConstructionPlacementFeedbackAuthority`.
- Added last-placement validation cache to reduce duplicate placement/path checks for identical construction cursor state.
- Routed construction summaries and blocked placement messages through shared formatting.
- Preserved the 0.9.09d no-self-entombment rule while keeping it placement-only.
- Added audit counters for construction feedback requests, cache hits, misses, and blocked results.
- Reaffirmed that defensive/machine placement validation must not run in render or turn loops.

## 0.9.09f — Construction Validation Inspection / Efficiency Pass
- Added `ConstructionValidationInspectionAuthority`.
- Added BUILD-panel compact validation display.
- Added BUILD-panel `VALIDATE` button routed into Infopedia AUDITS.
- Added `Construction Validation Inspection` audit entry.
- Exposed current recipe, blueprint family, raw/governed placement result, access-blocking classification, requirements, and recent cache history.
- Preserved placement-only validation and avoided render-loop or turn-loop overhead.
- Recompiled and rebuilt successfully.

## 0.9.10a — Staffing / Labor Bridge Rebuild
- Rebuilt the failed upload surface package from the last known-good 0.9.09f archive.
- Added `StaffingLaborBridgeAuthority` as an observational, bounded staffing layer.
- Added machine and defense staffing/readiness summaries.
- Added labor role tags for operator, loader, guard, technician, and maintenance.
- Connected staffing summaries to Workbench, Base management, and Infopedia surfaces.
- Preserved production, hauling, and automation ownership in existing systems.
- Kept the pass efficient: no render-loop scan, no global labor simulation, no autonomous staffing loop.

## 0.9.10b — Manual Staffing Assignment Bridge
- Added `ManualStaffingAssignmentAuthority`.
- Added manual worker/station assignment and clear operations.
- Added Workbench buttons for staffable station selection, worker selection, assignment, and clearing.
- Added manual staffing status lines to the Workbench panel.
- Added Manual Staffing Assignment Bridge infopedia/audit entries.
- Reused `BaseObject.assignedWorker` for persistence instead of adding a heavy new save structure.
- Preserved legacy production outcomes; staffing still does not automate production, hauling, turret targeting, or worker movement.
- Efficiency rule preserved: no global staffing loop, no render-loop staffing scan, no pathfinding scheduler.

## 0.9.10c — Staffing Role / Skill Validation Bridge
- Added `StaffingRoleSkillValidationAuthority` as assignment-time validation for manual staffing.
- Manual staffing now rejects workers below the minimum role skill for the selected station.
- Duty mismatches are currently warnings, not hard failures, so early playtesting remains flexible.
- Staffing/status/Infopedia surfaces now show validation details for the selected station/worker pair.
- Efficiency boundary preserved: no global labor scans, no pathfinding, no autonomous staffing loop, no production ownership transfer.
- Next planned phase: logistics/hauling reservation scaffolding with intent/status only.

## 0.9.10d — Logistics / Hauling Reservation Scaffold
- Added `LogisticsReservationScaffoldAuthority` as the first logistics/hauling bridge.
- Added selected-machine input reservation forecasting for assigned known recipes and generated/faction production jobs.
- Added Workbench `LOGISTICS` action and logistics forecast status lines.
- Added Infopedia/Audit entry for logistics reservation scaffolding.
- Logistics remains intent/status only: no actor movement, pathfinding, item transfer, route reservation, production ownership transfer, or autonomous hauling loop.
- Efficiency boundary preserved: selected-machine only, bounded display, existing input counters, no render-loop scans, no turn-loop scans, and no global hauling simulation.
- Next planned phase: storage reservation tokens and delivery intent records after this scaffold proves stable.

## 0.9.10e — Logistics Delivery Intent Bridge
- Added `LogisticsDeliveryIntentAuthority`.
- Added bounded, saveable delivery-intent records generated from selected-machine logistics forecasts.
- Added Workbench `INTENT` action, Infopedia/status lines, and persistence for recent logistics intents.
- Preserved efficiency boundaries: no global scans, no pathfinding, no route locks, no actor movement, no item transfer, and no production ownership transfer.
- Advanced runtime/save version to 0.9.10e.

## 0.9.10f — Logistics Storage Source Candidate Display
- Added `LogisticsStorageCandidateAuthority`.
- Latest logistics delivery intent now exposes candidate source counts from base storage and carried inventory.
- Added Workbench/status/Infopedia feedback for source readiness and missing input units.
- Preserved logistics boundaries: no hauling AI, no pathfinding, no global scans, no item transfer, no production authority transfer.
- Advanced runtime/save version to 0.9.10f.

## 0.9.10g — Logistics Source Reservation Tokens
- Added `LogisticsSourceReservationAuthority`.
- Added bounded, saveable source-token records for the latest delivery intent.
- Added Workbench `SOURCE TOKEN` action.
- Added Infopedia/status feedback for source-ready, partial-source, source-missing, and no-inputs-required states.
- Reused the existing delivery-intent and storage-candidate authorities instead of duplicating input logic.
- Preserved hard boundaries: no item locks, no route locks, no hauling, no transfer, no production consumption, and no autonomous logistics loop.
- Advanced runtime/save version to 0.9.10g.

## 0.9.10h — Logistics Delivery Route Intent Display
- Added `LogisticsRouteIntentAuthority`.
- Added bounded route-intent history and save/load persistence.
- Added Workbench `ROUTE INTENT` action.
- Added Infopedia/status route display.
- Preserved hard boundaries: no pathfinding, no route locks, no hauling, no item transfer, no production ownership transfer.
- Recompiled and rebuilt successfully.

## 0.9.10i — Logistics Route Readiness / Manual Haul Preview
- Added a bounded route-readiness/manual haul preview authority.
- Added player feedback for stale logistics records, invalid route anchors, missing handling access, long routes, and zero/unknown route estimates.
- Preserved efficiency doctrine: selected/latest route only; no pathfinding, no global scan, no hauling, no item transfer, no production ownership transfer.
- Next planned step: manual haul-order record contract before any actor movement or automatic hauling.

## 0.9.10j — Logistics Manual Haul Contract Bridge

- Added `LogisticsManualHaulContractAuthority`.
- Added Workbench `HAUL CONTRACT` action.
- Added bounded, saveable manual haul contract records.
- Contracts retain warnings and blockers from the latest logistics chain.
- Added Infopedia/status reporting for haul contracts.
- Preserved efficiency boundaries: no pathfinding, no global scans, no actor movement, no item transfer, no item locks, no production ownership transfer.
- Advanced runtime/save version to 0.9.10j.

## 0.9.10k — Logistics Haul Fulfillment Preflight

Added the non-executing haul fulfillment preflight authority. The Workbench can now run a bounded `PREFLIGHT` check against the latest haul contract. The check reuses existing logistics records, reports stale links, missing source readiness, worker availability, and handling-tile problems, then saves a compact preflight record. This improves player feedback and future logistics API consolidation without adding hauling, pathfinding, item locks, transfers, production ownership, global scans, or turn-loop labor overhead.

## 0.9.10l — Logistics Contract Lifecycle Authority
Added `LogisticsContractLifecycleAuthority` as the expiry/cancellation/staleness layer for manual haul contracts. The Workbench now exposes LIFECYCLE and CANCEL HAUL actions. Lifecycle records are bounded, saveable, and player-facing while remaining non-executing: no pathfinding, actor movement, item locking, item transfer, hauling automation, global scans, or production authority transfer. Verification: `javac --release 17` PASS; JAR rebuilt.

## 0.9.10m — Documentation Consolidation / Authority Drift Correction

- Performed a documentation consolidation run after consulting `MASTER_DEVELOPMENT.md` and the accumulated standards addenda.
- Archived the previous expanded standards file as `ARCHIVED_STANDARDS_AND_PRACTICES_PRE_0.9.10m.md`.
- Rebuilt `STANDARDS_AND_PRACTICES.md` as a concise active governance document organized by authority ownership, efficiency, player feedback, construction, defenses, staffing, logistics, queue/production, UI, art packaging, and documentation boundaries.
- Added `DOCUMENTATION_CONSOLIDATION_AUTHORITY_0.9.10m.md`.
- No gameplay execution authority was expanded in this pass. Logistics remains non-executing until a later selected-contract execution gate.

## 0.9.10s — Phase 2 Placeholder and Asset Integration Discipline I

- Corrected the development track back to the master-plan phase order: Phase 2 asset/placeholder discipline before further Phase 7 logistics detail work.
- Added `AssetIntegrationDisciplineAuthority` as the single semantic asset promotion and legacy-alias boundary.
- Converted newly generated road frontage fixtures, road-adjacent transit/vehicle profiles, and room-interior service fixtures to canonical semantic runtime handles rather than new placeholder-suffixed type names.
- Updated frontage interaction, fixture registry, infrastructure promotion, and media semantic art lookup to canonicalize older placeholder-style save/object names through the shared boundary.
- Removed the obsolete base-art placeholder index generator and unconsumed generated placeholder-copy directories from the shipped core archive.
- Replaced remaining phase-numbered debug audit labels in runtime world generation with semantic audit labels.
- Updated the master plan, standards, and governance documents only; no new development document was created.
- Verification: `javac --release 17` PASS; `TheMechanist.jar` rebuilt; ZIP integrity passed.

## 0.9.10ak Handoff Readiness — New Conversation Resume Boundary
- Updated `NEW_DEVELOPMENT_CONVERSATION_BRIEFING.md` to make the clean conversation-reset state explicit after the Asset Pack 6 intake/export pass.
- Confirmed Phase 2 remains the active ordered phase and the next valid bucket is Shrine / Ecclesiarchy fixtures.
- Corrected the Phase 2 recommended-order list so Hab/domestic fixtures are not listed twice after Asset Pack 6 completion.
- No gameplay source, runtime assets, README content, or artpack payloads were changed in this handoff pass.


## 0.9.10gl — Server Authority Phases C-F Integration

Returned to the lettered server-authority plan and implemented phases C, D, E, and F as one bounded continuation pass. Added immutable `WorldSnapshot` publication after authoritative commits, carrying player state, compiled visible/remembered tile descriptors, visible NPCs, visible objects, recent actions, and UI state. The render loop now records the latest snapshot version it consumed so stale/render-order issues have a diagnostic surface. Turn modes remain explicit through `WorldTurnManager`, with strict turn-based default and slow continuous mode available through host authority. Long-action progress remains server-gated through `PlayerActionRegistry`, now using ASCII-safe progress bars and named completion commands. The console now enters the server side as `ConsoleCommandRequest`, and state-mutating admin commands route through named `WorldCommandRequest` records for money, turn advancement, item issuance, and teleportation. Command rejection diagnostics now record gated-player, invalid-target, and foreign-session cases rather than silently falling through. Verification: Java 17 source compile passed; targeted AuthoritativePhasesCdefSmoke passed; jar rebuilt; Xvfb startup reached boot/menu runtime; zip integrity passed.


## 0.9.10gm — Server Authority Phase G/H Extension and Server Runtime Separation

Continued the user-extended launcher/client/server track beyond phases C-F. Long-duration server-gated actions now expose a compact clock/countdown overlay above the player map icon, sourced from `PlayerActionRegistry` rather than a loose UI timer. The overlay reports remaining server-action ticks while ordinary input authority is withheld. Added `ServerRuntimePaths` as the save-path authority for separated desktop and server namespaces. Single-player manual/autosave files now resolve under `saves/singleplayer/`, and generated single-player world definitions resolve under `saves/singleplayer/worlds/`. Added a separate headless server executable entry point, `MechanistServerMain`, packaged as `TheMechanistServer.jar`, with Linux/Windows launch helpers and a root `SERVER_README.md`. The server runtime initializes `saves/server/server_state.properties`, `saves/server/worlds/`, and four separate server slot paths under `saves/server/slots/` without sharing single-player files. Expanded `PlayerLifecycleService` with a server-world respawn mode that resolves owned bed/base first and world-seed spawn second, while single-player death remains a loss-screen outcome. Manual save slots were aligned to four slots. Verification: Java 17 source compile passed; targeted `ServerRuntimeAndCountdownSmoke` passed; desktop jar and server jar rebuilt; Xvfb startup reached boot/menu runtime; server executable status run passed; zip integrity passed.


## 0.9.10gn — Save Efficiency Catalog / Slot-World Definition Separation

Began the persistence-efficiency pass by adding `SaveEfficiencyAuthority`. New slot writes no longer embed duplicated `worlddef.*` generated-world ledgers when a separate `.mechworld` exists. Slot saves now carry explicit `save.schema`, `save.worldId`, `save.worldSeed`, `save.worldFile`, and `save.worldDefinitionEmbedded=false` metadata. `.mechworld` files remain the authority for generated hive-world definition data. Added `/save_catalog` and `/persistence_catalog` for compact key-count, approximate-size, namespace, and embedded-world-definition audits. Verification: Java 17 source compile passed; `SaveEfficiencySmoke` passed; desktop and server jars rebuilt; Xvfb desktop startup reached boot/menu runtime; zip integrity passed.

## 0.9.10go — Save Efficiency Itemized Persistence Catalog

Continued the save-efficiency pass without pruning. `SaveEfficiencyAuthority` now produces a single itemized persistence list for active saves, split into `.mechsave` and `.mechworld` layers. Each namespace line reports key count, approximate character weight, representative keys, ownership purpose, and first-pass review guidance. Added host console commands `/save_itemized` and `/persistence_itemized` to expose the list during an active run. The first catalog identifies `world.*`, `worlddef.*`, logistics histories/reports, faction/news/bank/crime report text, machine queue history, legacy inventory/equipment bridges, and settings snapshots as the main review targets for later split/prune/compress decisions. Verification: Java 17 source compile passed; `SaveItemizedCatalogSmoke` passed; `SaveEfficiencySmoke` passed.

## 0.9.10gp — Character Slot / World-State Separation

- Split new save writes into a character/player slot authority and a mutable world-state authority.
- New `.mechsave` writes use `slot-v3-character-world-reference` and strip both `worlddef.*` and `world.*` before storing the character slot.
- `.mechworld` files now carry both generated `worlddef.*` ledgers and current mutable `world.*` active-world state.
- Load now merges `world.*` from the referenced `.mechworld` into the runtime properties before reconstructing the active world, preserving backward compatibility with older saves that still carried `world.*` in the slot.
- Added stable `char.playerId` metadata and mirrored initial player-faction continuity records into the world file so a player-created faction can belong to the world and remain associated with the returning character ID.
- Added `/save_architecture` and `/persistence_architecture` developer commands to summarize the current slot/world ownership model.
- Verified with save-efficiency, itemized-catalog, and slot/world-separation smoke tests.

## 0.9.10gq — Single-Player Bundled Snapshot / Server Split Preservation

- Reconciled the save model after the multiplayer-oriented character/world split.
- Single-player manual/autosave writes now use `slot-v4-singleplayer-bundled-world-snapshot` and deliberately embed both generated `worlddef.*` data and mutable `world.*` state in the `.mechsave` slot, alongside the character/player state, so loading the single-player slot restores the world at save time.
- The separate `.mechworld` file is still written as an external world continuity/reference copy, but embedded single-player snapshots are preferred on load when present.
- Server/multiplayer separation remains intact: character slots can remain lightweight attachments, while the server world file is sufficient for the server runtime to continue without a player save being present.
- `/save_architecture` now describes both models explicitly: bundled single-player saves and separated server/multiplayer player attachments.
- Verified with Java 17 source compile and `SinglePlayerBundledSaveSmoke`.

## 0.9.10gr — Server Character Slot / World-Owned Namespace Split

- Preserved the single-player bundled save model: `.mechsave` files continue to carry character state plus embedded `worlddef.*`, `world.*`, and world-owned runtime records so a single-player slot reloads the world exactly as it was saved.
- Tightened the server/multiplayer model: character slots now strip world-owned runtime namespaces (`base`, `factions`, `npc`, `inn`, `bank`, `crime`, `scavenge`, `machine`, and `logistics`) after copying them into the referenced `.mechworld` authority.
- Added explicit player-faction continuity records under world state, including stable player ID membership, reserved player command slot, player command track, separate NPC command track, and autonomy policy.
- Updated merge behavior so server/multiplayer character-slot loading can pull both `world.*` and the world-owned runtime namespaces from the referenced world file.
- Added and passed `ServerCharacterWorldOwnedNamespaceSmoke` to verify server slots keep character data while world files retain base/faction/news state and player-faction leadership continuity.

## 0.9.10gs — Player Faction World Ledger / Reserved Player Command Slots

- Continued the persistence architecture line after the server world-owned namespace split.
- Added `PlayerFactionWorldAuthority` as the named world authority for player-created faction continuity.
- World files now write a versioned `world.playerFaction.*` ledger with faction identity, base anchor, autonomy policy, reserved stable player ID membership, and separate player-command versus NPC-command tracks.
- Character/server slots now keep only the character attachment pointer into that world faction authority: stable `char.playerId`, last known faction ID, and resume rule.
- Added `/faction_continuity` and `/player_faction` developer console summaries so a run can report whether the current character has a reserved player-command slot in the world faction ledger.
- Preserved single-player bundled snapshots while ensuring server-style character slots do not leak the world player-faction ledger.
- Verification: Java 17 source compile passed; `PlayerFactionWorldAuthoritySmoke` passed under Xvfb.

## 0.9.10gt — Player Faction Autonomy World Ledger

Continued the persistence/faction continuity line after the player-faction world ledger. Added `PlayerFactionAutonomyAuthority` as the world-side owner for autonomous player-founded faction plans when the founding player is absent. The `.mechworld` runtime state now records whether the player faction is active, reserved player presence, base room/position, worker/security/labor counts, base asset/storage counts, production asset count, trade asset count, and compact production, trade, defense, news, NPC-command, and player-command continuity plans. Added host console summaries `/faction_autonomy` and `/player_assignments` so the current ledger can be inspected without merging the player and NPC command tracks. Server/multiplayer character slots continue to strip `world.*` player-faction autonomy records, while single-player bundled saves still carry them as part of the save-time world snapshot. Verification: Java 17 source compile passed; targeted `PlayerFactionAutonomyAuthoritySmoke` passed under Xvfb.

## 0.9.10gu — Player Faction Autonomous Tick Ledger

Continued the player-faction/world persistence line by adding `PlayerFactionAutonomousTickAuthority`. Player-founded faction autonomy now emits compact world-owned tick outcomes for elapsed autonomous time: production, trade, defense, morale, stock estimate, risk, reserved player IDs, and public continuity note. `/faction_tick [turns]` previews this ledger from current runtime state. Server/multiplayer character slots continue to strip world-owned faction autonomy and autonomous tick state; single-player bundled saves carry it only as part of the exact save-time world snapshot.

Verification: Java 17 source compile passed; `PlayerFactionAutonomousTickAuthoritySmoke`, `PlayerFactionAutonomyAuthoritySmoke`, `PlayerFactionWorldAuthoritySmoke`, and save smoke checks passed; desktop and server jars rebuilt; server status run passed; Xvfb desktop startup reached runtime; zip integrity passed.

## 0.9.10gv — Player/NPC Command Rank Parity Ledger

Continued the faction/personnel parity pass. Added `PlayerNpcCommandParityAuthority` as the world-owned authority that keeps player command ranks and NPC command ranks on separate rosters while binding them to a shared numeric command-tier scale. The founder is now recorded as a unique tier-0 player slot that recruited players cannot receive. Recruited player ranks are normalized onto tiers 1-5, and each tier maps to the equivalent NPC command tier, so a third-rank player has third-rank NPC command authority without becoming the founder or collapsing into the NPC roster. Player-faction world ledgers now write parity metadata, tier rows, member command tiers, NPC-equivalent rank names, and authority lines. Added `/rank_parity` and `/personnel_parity` host console summaries for parity inspection. Verification: Java 17 source compile passed; targeted `PlayerNpcCommandParityAuthoritySmoke` passed.

The same pass added `UniversalManagementWindowAuthority` as the first code-owned descriptor layer for the uploaded unified GUI guidance. It does not replace Swing panels yet, but it defines the shared contextual window modules that later UI extraction must use: dialogue/standings, container/cargo routing, machine/recipe production, and faction/personnel management. The faction/personnel module explicitly preserves separate player and NPC rosters while consuming the shared command-tier parity rule. Added `/ui_framework` and `/management_ui` developer summaries. Verification: `UniversalManagementWindowAuthoritySmoke` passed.


## 0.9.10gw — Display Density Authority

Implemented a desktop display-density pass for the pure Java 17 Swing client. Added `DisplayDensityAuthority`, which applies Java2D/Swing display properties before GUI startup, forces native 1:1 Java2D UI scaling, requests JVM text antialiasing, scales standard Swing Look-and-Feel fonts through `FontUIResource`, and applies shared Java2D hints for tiny custom-panel text rendering. Existing font and GUI scale options now feed the display-density authority, and their editable range expands to 50%-200% for denser layouts. Added `DisplayDensityAuthoritySmoke` to verify system-property setup, Swing font-default scaling, LCD text antialiasing, fractional metrics, and audit output.

## 0.9.10gx — Display / Graphics Menu Reorganization and Dynamic Mode Detection

Reorganized the options surface so Display owns monitor/window concerns and density, while Graphics owns render cost, frame pacing, art quality, motion, and palette treatment. Added `DisplayResolutionAuthority`, which enumerates runtime-supported `GraphicsDevice` display modes where available, deduplicates mode labels, preserves safe windowed fallback sizes, and feeds the resolution dropdown instead of relying on the old fixed preset array. Display now exposes window mode, detected/safe resolution, font/text scale, GUI/chrome scale, hover help, screensaver, and apply-display controls. Graphics now exposes render downscale, target FPS, render-quality profile, art quality, tile size, tile-art rendering, imported portraits, reduced motion, render profile, palette, color target, and brightness controls. Added target-FPS persistence and best-effort Swing timer delay application while keeping simulation/server authority independent from FPS. Added render-quality and reduced-motion options as persisted graphics settings. Verification: Java 17 source compile passed and `DisplayGraphicsMenuReorgSmoke` passed under Xvfb.

## 0.9.10gz — Diagnostics Overlay and JVM Runtime Profile Authority

Integrated the non-redundant portions of the uploaded engine guidance after the visual-lighting pass. Added `PerformanceDiagnosticsOverlayAuthority` as an F3-style render diagnostic overlay drawn over the final Swing/Java2D surface. It reports FPS, paint timing, JVM memory use, GC event counts, render profile, internal buffer size, target frame pacing, visual-lighting status, server lane status, screen/turn state, and the gameplay-light separation invariant. F3 toggles the overlay at runtime and the Graphics options page includes a persistent F3 diagnostics toggle.

Added `JvmRuntimeProfileAuthority` as the no-external-library owner for JVM runtime profile configuration. It loads and saves `settings/jvm_runtime.properties`, defines client graphics and server headless modes, compiles restart JVM argument lists for selected heap/GC/string-deduplication/manual override settings, and applies a server memory profile to the headless server runtime. The active process reports selected and active JVM context but does not silently restart itself.

Preserved the existing Swing timer/render pipeline and the render-only lighting separation from 0.9.10gy. Verification: Java 17 source compile passed; `PerformanceDiagnosticsJvmRuntimeSmoke` passed under Xvfb.

## 0.9.10ha — JVM Options Restart Panel and Runtime Profiles

- Extended the JVM runtime profile authority into launcher/client/thin-client/server runtime targets while keeping the active process restart-bound for heap, garbage collector, headless, and Java2D pipeline changes.
- Added OS-aware Java2D pipeline flag compilation: Windows graphical profiles may request Direct3D/transparent blit flags, Linux graphical profiles may request OpenGL/transparent blit flags, and server/headless profiles suppress rendering pipeline flags.
- Added a JVM options tab to the in-game Options surface with a top restart-required warning, profile cycling, GC cycling, memory adjustment, Java2D pipeline cycling, string deduplication, transparent blit, NOAA toggle, and a direct Accept + Restart button.
- Startup now applies saved Java2D property preferences before Swing initializes, while still reporting that heap and collector changes require a restarted process.
- The headless server now uses an effective server JVM profile for reporting instead of overwriting the user-selected saved launcher/client profile.
- Verification: Java 17 source compile passed; `JvmOptionsRestartProfilesSmoke` passed; desktop and server jars rebuilt; server status run passed; Xvfb desktop startup reached runtime; zip integrity passed.

## 0.9.10hb — Single-Player Combined JVM Profiles and Accessibility Compatibility

- Added single-player combined JVM runtime profiles for the current embedded architecture where the local graphical client, local host, and authoritative server lane share one process. The normal combined profile reserves more heap for ordinary single-player; the heavy combined profile reserves more heap for larger worlds and heavier render/simulation work.
- Updated JVM profile cycling and explanatory text so launcher/main-menu, graphical client, thin network client, single-player combined, single-player combined heavy, and headless server are distinct runtime targets of the same program. Headless server profiles still suppress graphical Java2D pipeline flags.
- Added `AccessibilityCompatibilityAuthority` as the render-side owner for color-vision compatibility, AccessibleContext narration updates, high-contrast text support, instant text support, and screen-shake intensity settings.
- Added a new Accessibility options tab with controls for color-vision correction, high-contrast text containers, instant conversation text, screen-shake intensity, and a manual narration update command.
- Added four additional diegetic color palettes: Protan Ember, Deutan Steel, Tritan Brass, and Legibility Slate.
- Added render-side Daltonization over the composited Java2D backbuffer. This does not alter gameplay light, tile, faction, combat, or server-authoritative state.
- Verification: Java 17 source compile passed and `JvmSinglePlayerCompatibilitySmoke` passed.


## 0.9.10hc — Fallback Profile Management and Migration

- Added `FallbackProfileManagementAuthority` for local profile management when no wrapping store/platform environment is detected.
- Main-menu Profile now opens a guarded local Profile Management window for internal profiles instead of only reporting the generated identifier.
- Local profile metadata is stored at `profiles/active_profile.properties` with a generated profile ID, hardware signature, wrapper/provider state, portrait index, runtime memory/profile anchors, color-vision mode, diagnostics state, and internal render bounds.
- Added profile migration ZIP export/import for profile/options/JVM/profile-seed metadata with staging, entry allow-listing, size cap, and atomic replacement where supported.
- Profile migration intentionally excludes character saves and world saves; those remain owned by the persistence/world authorities.
- Added targeted fallback-profile smoke coverage.


## 0.9.10hd — Structured Project Re-evaluation and Goal Replanning

- Added `ProjectReevaluationAuthority` to make the evaluation sequence repeatable from developer/host console commands instead of depending on a multi-turn prompt ritual.
- Added `/project_evaluation`, `/gap_analysis`, `/replanned_goals`, `/order_of_operations`, and `/hidden_dependency` summaries through the server-authoritative admin dispatcher.
- Rewrote the near-term roadmap around command conversion, faction/personnel assignment parity, UniversalWindow migration, persistence ownership audit v2, JVM/profile validation, accessibility indicator work, and a future server networking planning gate.
- Recorded the main hidden dependency: legacy UI/helper code that still assumes direct access to the live `World` object instead of submitting commands and consuming snapshots/modules.
- Verification: Java 17 compile and targeted project re-evaluation smoke test.

## 0.9.10hf — Name-Locked Profile Portrait Integration

- Added a partitioned name-locked profile portrait authority for the 25 uploaded CRT-green creator portraits.
- Sliced the provided 5x5 portrait sheet into individual 128x128 assets under `assets/art/portraits/name_locked/` while keeping them outside normal/random player portrait generation.
- Character creation now seeds the first candidate from the active wrapper/local profile name when that profile name matches the registered name-locked set.
- A name-locked portrait is used only when the active profile name and the in-game character name resolve to the same registered entry; mismatched names fall back to ordinary baseline-human portrait authority.
- Finalizing a matching profile/character grants small profile dossier items and a knowledge entry so the match has an in-game function rather than being only cosmetic.
- Added save/load preservation for `char.nameLockedProfileKey`.
- Added `NameLockedProfilePortraitAuthoritySmoke` to verify registry matching, mismatch rejection, and 128x128 asset loading.

## 0.9.10hg — Name-Locked Profile Unlock Notification and Noble Seeding

Implemented the second name-locked profile integration pass. Matching active profile and finalized character names now trigger a timed profile-unlock notification listing granted dossier items/perk flags, add a dedicated Profile Special Unlocks knowledge entry, and expose unlocked profile dossiers through Infopedia. Name-locked portraits remain partitioned from random character generation. Inactive registered profiles may now seed rarely as visiting noble NPCs in noble/governor zones, while the active operator profile key is excluded so a player does not encounter their own duplicate noble. Added persistence for NPC name-locked profile keys and a targeted smoke test for active-profile exclusion, profile dossier Infopedia lines, and deterministic noble seeding.

## 0.9.10hj — Gameplay Defaults and Quality-of-Life Options

- Added `GameplayQualityOfLifeAuthority` as the owner for default gameplay comfort and friction-prevention settings drawn from the uploaded gaming-defaults guidance.
- Added a QOL options tab covering subtitles, repeat splash skipping, auto-loot preference, omni-directional ghost build, hold-to-repeat construction, smart storage filters, proxy crafting, production blocker warnings, market alerts, favored-item protection, and item safety profile strictness.
- Added persisted options for construction trap warnings, machine output auto-routing, global scarcity warnings, recipe HUD pinning, low-quality pickup warnings, no mixed-quality stacking, under-attack supply lock, safe worker priorities, named death alerts, and local/global price hints.
- Added `/qol_defaults` and `/gameplay_defaults` developer/admin summaries through the existing server-authoritative console path.
- Verification: Java 17 compile passed and `GameplayQualityOfLifeAuthoritySmoke` passed.

## 0.9.10hk — blueprint construction/editor foundation
- Added `BlueprintConstructionAuthority` as the named owner for room-object blueprint data, hollow-box room generation, relative-offset cells, connection anchors, resource-cost estimates, invalid-placement validation, and collisionless ghost construction plans.
- Extended QOL/options reporting with construction/editor integration contracts for invalid placement diagnostics, ghost stamps, preflight checklists, anchor snapping, sandbox blueprint design, capture tooling, and material substitution prompts.
- Added developer console summaries `/blueprint_editor` and `/construction_validation` plus targeted smoke coverage.

## 0.9.10hl — Hollow Box Room Recipe Evaluation

- Evaluated the Hollow Box room tool's build recipe layer and replaced abstract cost buckets with itemized construction inputs that map to the existing economy catalog.
- Added per-tile `TileBuildRecipe` and `BuildComponent` records for tier-1 floors, tier-1 walls, basic doors, logic/utility I/O cells, machine mounts, furnishings, and fallback generic construction.
- Hollow Box cost estimates now report real build inputs such as `Construction supplies`, `Scrap plate`, `Rivet set`, and `Bearing set`, plus grouped labor-turn estimates by tile recipe.
- Blueprint/editor summaries now include a Hollow Box recipe evaluation so construction testing can inspect cell counts, itemized cost, labor, ghost placement rules, and clearance requirements.
- Verification: Java 17 source compile passed and `BlueprintConstructionAuthoritySmoke` passed with itemized recipe assertions.

## 0.9.10hm — Progressive Construction State

Implemented staged construction sites for base construction. Confirmed builds now place an under-construction site that can receive components over time instead of requiring every component to be consumed before placement. Under-construction tiles persist as base objects with final-symbol metadata, required/inserted component ledgers, labor progress, and visual progress. Player interaction with a staged site contributes available components and tool-adjusted labor. Construction visuals now fade from pale blue ghost coloration toward the final built object color as progress increases, and under-construction tiles draw a per-tile progress bar. Added tool-aware deconstruction time estimates for terrain/wall/door clearance and a Progressive Construction audit surface.

Verification: Java 17 compile passed; `ProgressiveConstructionAuthoritySmoke` passed; `BlueprintConstructionAuthoritySmoke` passed.

## 0.9.10hn — Fallback Profile Legal Adult Certification

- Added a required legal adult certification checkbox to the fallback local profile management flow.
- Persisted the certification flag as `profile.isLegalAdult` in `profiles/active_profile.properties`.
- Added service-style validation that rejects profile sync/export when certification is not set.
- Added smoke coverage for rejection, persistence, and audit reporting.


## 0.9.10ho — Secure Chat Window Foundation

- Added a bounded, sanitized non-blocking chat window opened from the in-game command menu and the Y hotkey.
- Added client-side async local chat logging with profile/wrapper identifiers recorded only in the log payload, not in the displayed chat line.
- Added Java 17 chat payload records, 256-character document hard cap, control-character/tag stripping, duplicate resend suppression, send-rate limiting, UI log trimming, and log rotation.
- Routed server console command text through the same sanitization boundary before command dispatch.

## 0.9.10hp — EULA Gate and Gameplay Console Command Audit

- Added a fail-closed EULA consent authority that blocks boot/menu/game access until local consent is accepted and persisted as `eula_consented=true` in `settings/legal.properties`.
- Added a registered gameplay/debug/server command authority covering the uploaded gameplay, diagnostics, moderation, identity-enforcement, and server-maintenance command list with rank gating and fail-closed handling for remote/destructive commands whose multiplayer transport is not yet open.
- Routed newly registered commands through the existing server-authoritative console dispatcher instead of creating a parallel console path.
- Added smoke coverage for EULA persistence and gameplay console command registration/execution.

## 0.9.10hq — EULA Intellectual Property Disclaimer Preservation

- Added the Games Workshop / Warhammer unofficial fan-project acknowledgment to the runtime EULA notice, including non-ownership, trademark recognition, zero-commerce/non-profit, non-infringement, unaffiliated/unendorsed, and removal-on-official-objection language.
- Expanded the EULA gate into a scrollable overlay so the longer legal notice can be read without truncating behind the acceptance buttons.
- Added an explicit **I DO NOT AGREE** button beside **EXIT**; either refusal path still exits immediately, while **I AGREE** persists `eula_consented=true` locally before unlocking the runtime.
- Added a root `EULA.md` copy beside `README.md` and recorded the preservation rule in `STANDARDS_AND_PRACTICES.md` without adding extra documents inside `docs/`.
- Extended the EULA/gameplay console smoke test to verify that the runtime EULA and root EULA copy both preserve the Games Workshop disclaimer and consent-persistence language.

## 0.9.10hr — Native Java E2EE Chat Packet Foundation

Added a native Java 17 end-to-end-encryption foundation for the multiplayer chat line. `HybridEncryptionManager` now owns RSA-3072 identity-key generation, AES-256 temporary payload-key generation, RSA-OAEP-SHA256 AES-key encapsulation, AES/GCM/NoPadding message sealing, public/private key Base64 import/export helpers, key fingerprinting, and precise cryptographic failure reporting. `SecureChatPacket` now provides the relay-safe packet record with sender/recipient identifiers, key ID, encrypted AES key, IV, ciphertext payload, timestamp, wire-string round trip, and authenticated metadata binding.

Updated `ChatRuntimeAuthority` so local sends prepare an opaque E2EE relay packet and log only relay-safe packet summaries rather than plaintext message bodies. Added `SecureChatE2eeSmoke` to validate Player A to Player B encryption/decryption, wire serialization, AES-GCM authenticated metadata tamper rejection, and wrong-recipient private-key rejection. Public network hosting remains closed; this pass establishes the cryptographic packet seam for the later central blind relay.

## 0.9.10hs — Headless Multiplayer Binding and Menu Foundation

- Added a native Java 17 multiplayer networking foundation that bridges world-generation settings into immutable `ServerConfig` records carrying seed, world name, world id, difficulty summary, max players, selected port, bind address, protocol state, and encoded world setup.
- Added `NetworkPortAuthority` with the configured custom game range `25500-25599`, avoidance of well-known system ports and Steam query ports, first-available port scanning, and direct endpoint parsing for IPv4 plus bracketed IPv6 forms such as `[2001:db8::1]:25565`.
- Added layered host binding through `MultiplayerHostBindingService`: Steam environment/wrapper detection first, optional Netty classpath probing second, IPv6 native relay binding third, then IPv4 native relay fallback if IPv6 is unavailable.
- Added `NativeTcpRelayServer`, a closeable headless TCP relay that accepts bounded client counts and relays opaque encrypted line packets without decrypting them.
- Expanded `MechanistServerMain` so the server jar can initialize status as before or bind hosting with `--host`, `--host-once`, `--world-name`, `--seed`, `--max-players`, `--port`, `--bind`, `--setup`, and `--no-steam` arguments.
- Added `MultiplayerMenuController` and a launcher multiplayer screen with direct address editing, recent connection history, persistent favorites, Steam friend handoff status, and host-from-current-world binding.
- Added `MultiplayerNetworkSmoke` covering port selection, IPv4/IPv6 endpoint parsing, world-setting-to-server-config bridging, host binding, and menu history/favorite persistence.

## 0.9.10ht — Program Icon Integration

- Incorporated the uploaded Java/Linux icon pack as the program icon source set under `assets/app/icons/`, preserving 16, 32, 48, 64, 128, 256, and 512 pixel PNG exports plus the production sheet.
- Added `AppIconAuthority` so the Swing client loads the same icon set for the application window and attempts a supported taskbar icon update without crashing on desktops that reject taskbar icon changes.
- Updated the Linux `.desktop` launcher to use `Icon=the-mechanist` and expanded `install_linux_launcher.sh` to install the icon PNGs into the user hicolor icon theme before copying the desktop launcher.
- Added a targeted icon smoke test to verify that all expected icon sizes load and the generated ICO bridge asset is present.



## 0.9.10hu — Simulation Editor Suite / Mod Packaging Exporter

Implemented the native Java 17 Swing Simulation Editor Suite and reopened the Mods / Tools route as a real editor surface. Added seven dedicated editor tabs for sectors, rooms, factions, items, knowledge, infopedia, and mod packaging. Added an editor event bus, undo/redo command runner, repository-backed property tables, link-aware combo-box editors, bounded numeric spinner editors, and scope-selection tables. Added `ModDeploymentManager` with background `SwingWorker` deployment, ZIP fallback packaging, generated `manifest.json`, data JSON export, SHA-256 structural checksums, Steam wrapper detection, and a reflected SteamUGC publication seam that fails honestly when no compatible Steam runtime is present. Added `SimulationEditorSuiteSmoke` covering command apply/undo/redo and ZIP manifest export.

## 0.9.10hv — Modding API Template and Documentation Suite

Added a public `mechanist.modapi` seam for modder-facing integration examples. The new API includes `SimulationContext`, `SimulationModRuntime`, `ModIntegrationHook`, `SectorInstance`, `RoomNode`, `FactionProfile`, `ItemTemplate`, `ResearchTree`, `LoreDatabase`, typed records for coordinates, vectors, dimensions, placement nodes, research nodes, lore entries, diplomacy changes, and sealed `SimulationEvent` payloads.

Added six complete compile-checked example integrations: anomalous cosmic sector navigation drift, reinforced hydroponics lab atmosphere stabilization, cybernetic collector faction diplomacy/economy behavior, localized gravity anchor item charge/durability decay, ancient xenobiology non-linear knowledge branch, and precursor Infopedia taxonomy/cross-linking. Each example has a valid `manifest.json` under `modding/examples/` and a Java entrypoint mirrored under `src/mechanist/modapi/examples/` for compilation and smoke verification.

Added `modding/API_REFERENCE.md` with the required warning block, subsystem model relationships, lifecycle callback documentation, manifest schema, and code snippets for Sector, Room, Faction, Item, Knowledge, and Infopedia integrations. Added `ModdingApiTemplateSmoke` to verify registration and runtime callback behavior across all six example systems.

## 0.9.10hw — Secure Server Architecture Foundation

Implemented the first staged zero-trust headless-server security foundation from the uploaded secure networking instruction set. Added `NetworkThrottlingManager` for active-player/download-client accounting and adaptive per-download byte-rate decay; `SecureHandshakeStateMachine` to forbid world snapshot serialization during manifest delivery/acquisition/integrity phases; `AuthoritativeAeadPacketValidator` for AES-GCM metadata-bound packet validation and replay rejection; `PacketRateLimiter`/`NetworkPacketPolicer` for dual-direction packet ceilings; `SecurityPathGuard` for canonical root path enforcement; `ServerPerformanceMonitor` with a degraded-state background-I/O gate; `AfkSessionWatchdog` with SecureRandom timeout selection; `PlayerBehaviorMonitor` with fixed-ring telemetry and macro JSON logging; `SecureLocalSaveValidationManager` for HMAC-SHA256 local tamper detection; `MasterlistAuthenticationClient` for signed HTTPS heartbeat payloads; `SecureModClassLoader` and bytecode constant-pool scanning; `NettyResourceSafetyBridge` as an optional reflection seam for leak detector/reference release when Netty is actually packaged; `PlayerIdentity`, `CharacterSaveManager`, and `CharacterStateRecord` for server-authoritative profile persistence with atomic saves; and login lifecycle helpers for offline state retention, spiral spawn collision resolution, and temporary login protection.

Updated the native Java NIO relay fallback to use 64KB bounded line frames, socket read timeouts, per-client packet policing, load-aware outbound byte budgeting, and strict handshake progression before any live-world state could be considered eligible. `MechanistServerMain` now initializes and closes the secure server core alongside the existing runtime namespace. Added `SecureServerArchitectureSmoke` to validate throttling decay, phase gating, AEAD replay rejection, path traversal denial, signed-save tamper rejection, macro detection, character profile persistence, spawn fallback, login protection cancellation, and session deregistration.



## 0.9.10hx — Mod Sandbox, Intrusion Detection, and Transaction Defiler Guard

Implemented a feasibility-filtered hardening pass from the latest pasted security requirements. Added `ModPackageValidator`, a zero-dependency ZipInputStream bytecode/package scanner that rejects unsafe zip paths, oversized entries, banned constant-pool references, suspicious binary/text patterns, file/process/reflection/network/native/internal JDK access, and jars without compiled classes. Reworked `SecureModClassLoader` around a strict Java allow-list and public `mechanist.modapi`-only engine access rule, while delegating jar verification to the stronger validator.

Added sealed data-only mod payload records through `ModContentPayload` and `ModDataMappingVerifier` so rooms, items, factions, sectors, knowledge nodes, and lore can be mapped into immutable records the engine interprets without granting unchecked low-level hooks. Added `IntrusionDetectionEngine` with instrumented StackWalker boundary checks, class-resource hash drift verification, rolling exception/fuzzing detection, panic/lockdown target callbacks, async admin logging, and honest runtime behavior that avoids pretending Java can externally StackWalk arbitrary threads.

Added `InventoryTransactionGuard` with bounded/time-evicting per-player idempotency-token caches, cached replay responses, server-side currency and merchant-proximity validation, and desynchronization logging. Added `PacketSequenceValidator` for strict monotonic per-session gameplay packet ordering and wired optional `SEQ|id|payload` validation into the native TCP relay without breaking legacy/unsequenced opaque relay frames. Updated `SecureServerNetworkingCore` to expose the new mod validator, intrusion detector, and transaction guard. Added `ModSandboxIntrusionTransactionSmoke` to verify safe/malicious jar scanning, loader denial of `java.lang.Runtime`, sealed payload mapping, intrusion panic callbacks, idempotent purchase replay behavior, and packet replay disconnect behavior.

## 0.9.10hy — Network Optimization, Local Host Conversion, NAT/Recovery Infrastructure

Implemented the next staged networking and local-hosting pass from `local network.txt`. Added `SnapshotDeltaCompressor` with per-client historical ring buffers, fixed-point quantization, entity spawn/destroy/change deltas, and binary encode/decode helpers. Added `PredictedLocalController` for client-side movement prediction and authoritative reconciliation, and `EntityInterpolationBuffer` with delayed remote-entity snapshot queues, LERP position blending, and SLERP quaternion rotation blending.

Added `NatTraversalManager` with zero-dependency UDP STUN binding-request parsing, WAN/LAN profile reporting, generated UDP punch tokens, and a lightweight reciprocal hole-punch loop for fallback direct-hosting. Added Linux deployment assets under `deploy/systemd/` and `deploy/linux/`, including hardened service-unit settings and an explicit socket-activation feasibility note because Java 17 standard APIs cannot safely adopt systemd's inherited listening file descriptor without native bindings.

Added `ServerDisasterRecoveryEngine` as a global uncaught-exception panic handler that pauses network acceptance hooks, enumerates active sessions through a provider seam, force-saves authoritative `CharacterStateRecord` blocks via atomic writes, and publishes `crash_dump.json` plus timestamped crash dumps. Integrated it into `SecureServerNetworkingCore` so the headless server installs the handler during boot.

Added `WorldSimulationClock` for input-driven single-player ticks and conversion to a locked continuous multiplayer tick loop without reloading the in-memory world. Added `LocalHostAuthGate`, `LocalHostSettingsPanel`, and `HostDashboardOverlay` for password/host-key admission control, readable SecureRandom eight-character host keys, NAT/Steam/direct profile display, roster display, and kick callbacks. Added `MultiplayerConversionWarningDialog`, `LocalWorldSaveStateSeparator`, `SaveConversionException`, `IntegratedLocalMultiplayerHost`, and `MultiplayerRespawnManager` so local multiplayer conversion requires an explicit irreversible warning, transactionally splits host player data from world data, and uses multiplayer respawn placement instead of single-player game-over rewind logic.

Added `NetworkOptimizationLocalHostSmoke` covering delta compression/binary round-trip, prediction reconciliation, interpolation sampling, host-key admission, clock conversion, save-state splitting, respawn fallback, emergency crash dumping, and NAT token generation.


## 0.9.10hz — Native Packaging Pipeline and User Storage Manager

Implemented the Java 17 release-engineering pass for native package production and user-data isolation. Replaced the legacy minimal Maven descriptor with a full `pom.xml` that declares Netty, retains the optional Jamepad bridge, compiles the existing `src/` tree, bundles resources, and emits dependency-shaded desktop/server fat jars: `target/TheMechanist-all.jar` and `target/TheMechanistServer-all.jar`. Added jlink module profiles for desktop and server runtimes, Linux and Windows jpackage scripts, Linux DEB/RPM packaging support where host tools are available, Windows MSI packaging with WiX Toolset expectations, and a GitHub Actions matrix workflow that builds Linux and Windows installer artifacts on native runners before assembling a unified installer ZIP.

Added `PACKAGING_PIPELINE.md` as the requested root release guide rather than a fifth durable document under `docs/`. Added `GameStorageManager`, a Java 17 NIO.2 storage authority that routes mutable data outside the install path into the active user's platform directory, enforces `saves/`, `saves/data/profiles/`, `export/`, `mods/`, and `modsarchived/`, rejects traversal/absolute child paths, archives live mods atomically, and writes exported mod archives atomically. Updated `ServerRuntimePaths` to route save namespaces through `GameStorageManager` so server and single-player persistence stop defaulting to the extracted application folder. Added `GameStorageAndPackagingSmoke` to verify directory routing, traversal rejection, mod archiving, mod export, server path routing, character-profile persistence under the new root, and packaging-file presence.


## 0.9.10ia — Handshake hot-restart lifecycle

Added the explicit `CLIENT_HOT_RESTART` join phase between acquisition/sync and integrity challenge. `SecureHandshakeStateMachine` now models the seven-state lifecycle from identity verification to access granted, blocks world serialization until hot restart and challenge validation pass, and carries manifest/challenge records. Added `EngineHotLauncher` for background client-side registry purge and secure mod classloader remounting without owning or closing the persistent transport. Added `HandshakeNetworkFrame` and `HandshakeHotRestartCoordinator.ServerGate` for typed post-restart handoff frames, server-side challenge issuance, and strict world-stream gating. Added `HandshakeHotRestartSmoke` and updated native relay/smoke paths for the new state order.

## 0.9.10ib — ProGuard Obfuscation / Secure Mapping Pipeline

Added the release obfuscation phase to the Maven/native packaging path. `pom.xml` now runs `proguard-maven-plugin` after the Maven shade outputs are created, producing `target/TheMechanist-obfuscated.jar` and `target/TheMechanistServer-obfuscated.jar`. Added separate client/server ProGuard configuration files under `config/proguard/`, including Java 17 record/sealed keep attributes, stable public `mechanist.modapi` keep rules, launch-entrypoint keep rules, Netty/Jamepad guard rules, aggressive dictionary-backed obfuscation for internals, and raw mapping output routed to `target/proguard/*/mapping.raw.txt`.

Added `tools/obfuscation/MappingEncryptionTool.java` plus Linux/Windows wrapper scripts to compress and AES-GCM encrypt mapping files into `dist/secure-maps/` before raw mappings are deleted. Added `tools/obfuscation/SensitiveStringTableGenerator.java`, `config/obfuscation/sensitive-strings.properties`, and the generated `ObfuscatedStringTable` so high-signal security strings can be stored as encrypted byte arrays decoded only when called. Packaging scripts now regenerate the sensitive string table, run Maven package, encrypt mappings, and feed the obfuscated client jar into jpackage.



## 0.9.10ic — Crash Log De-Obfuscator Admin Tool

Implemented the asynchronous ProGuard crash-log de-obfuscation toolchain. Added `ProGuardMapParser` for class/member parsing, line-aware method mapping, and bi-directional lookup structures; `StackTraceDeobfuscator` for background regex reconstruction of obfuscated stack frames with inline `MappingMissingAnomalie` tags; `CrashDeobfuscatorEngine` for versioned map caching and encrypted `MECHANIST-MAPPING-V1` map loading; and `AdminDeobfuscatorPanel` as a dual-pane Swing admin surface with mapping chooser, encrypted-key selector, build-version selection, background workers, and progress/status reporting. The Simulation Editor Suite toolbar now exposes the Crash De-Obfuscator window. Added `CrashDeobfuscatorEngineSmoke` covering plain mapping parse metrics, method line translation, missing-map anomalies, throwable-header rewriting, encrypted mapping loading, and panel construction under headless-safe conditions.


## 0.9.10id — Experimental doom Mode / First-Person Viewport

Implemented the first staged experimental first-person rendering pass from `doom mode.txt`. Added a disabled-by-default Quality of Life toggle labeled exactly `doom` mode with a confirmation warning that the feature is highly experimental and may cause instability. Added persistent `GameOptions` fields for `doomModeEnabled` and bounded `doomModeFovDegrees`, plus QoL audit/summary output.

Added `FirstPerson3DFramework.java`, including a Java2D software raycast viewport, camera/mouse-look handling, billboard sprite rendering for NPCs and objects, DDA wall/door raycasting with door-plane interaction checks, right-click forward-ray targeting, ghost-movement floor projection, light attenuation, and particle impact splashes. Added a host-settings FOV slider and default Maven LWJGL dependency declarations for core/GLFW/OpenGL plus Linux/Windows native classifiers, while keeping the active renderer honest as Java2D fallback until a real LWJGL backend is initialized. Added `DoomModeViewportSmoke` to verify default lock state, FOV clamping, render output, key consumption, and QoL audit integration.


## 0.9.10ie — Visual Juice / HUD / Effects Bridge

Implemented the next staged visual feedback pass from `more effects.txt`. Added `VisualJuiceFramework`, `GameHudOverlay`, `PlayerState`, `CameraJuiceSystem`, `ScreenGlitchEffect`, `PrimitiveParticleEmitter2D`, `WeaponFireProfile`, `LightmapRenderer2D`, `Collidable`, `AabbCollisionSystem`, and `Projectile2DPool`. The shared HUD now renders over both normal 2D mode and experimental `doom` mode.

The first-person viewport now applies local-only camera view bobbing and recoil offsets, screen-space horizontal-row glitch/static corruption, full shared HUD composition, and a real cross-quad sightline/tracer layer. Right-click targeting and door impact interactions now trigger recoil and short glitch/impact feedback without changing the authoritative grid movement model. Normal mode now applies high-quality Graphics2D hints, a reusable primitive-array particle layer, and a darkness/lightmap overlay pass. Added `VisualJuiceHudSmoke` to exercise HUD rendering, particles, lightmap composition, AABB projectile collision, camera juice, glitch pixels, and first-person render/attack integration.


## 0.9.10if — Continuous Grid Movement / Doom Collision Pass

Added `ContinuousGridMovementFramework`, including `ContinuousGridWorld`, `ContinuousGridPlayer`, `ContinuousCollisionGrid`, `MechanistContinuousCollisionGrid`, and `ContinuousMovementGamePanel`. The new movement authority stores true double-precision player position/velocity, supports mouse-look yaw, WASD forward/back/strafe input relative to that yaw, computes the current logical tile from the player center, and resolves rigid radius/AABB collision against blocked grid tiles with axis-by-axis sliding.

Integrated the framework into experimental `doom` mode so first-person movement no longer queues one-tile movements on each keypress. The viewport now keeps movement-key state across frames, advances continuous motion on the Swing timer, collision-checks against the existing `World.walkable` grid plus NPC full-tile blockers, updates the player's logical tile only after a valid center crossing, and reports the current logical tile in the first-person status overlay. Added `ContinuousGridMovementSmoke` to verify standalone wall/entity blocking, sliding movement, and the doom-mode adapter path.

## 0.9.10ig — Doom Fog / Java2D Acceleration Pass

Added selectable first-person fog modes for the experimental `doom` viewport: linear perpendicular Z-depth fog for stable wall-column depth and radial Euclidean fog for circular visibility. Added `DoomDepthFog`, `DoomFogSettings`, and `DoomFogMode`, and routed wall/sprite fog through the shared fog authority instead of ad-hoc darkening. Updated client JVM graphics profiles to request OS-specific Java2D acceleration hints and moved the lightmap overlay to a `VolatileImage` surface when a graphics configuration is available, with a `BufferedImage` fallback for headless/incompatible environments. Added `DoomFogAccelerationSmoke`.

## 0.9.10ih — Runtime Audit / Frame Limiter / Stress Diagnostics

Conducted a targeted static audit over the expanded 2D/3D Swing renderer, first-person viewport, HUD, particle, lightmap, and frame-pacing paths. Added `tools/audit/runtime_performance_audit_0_9_10ih.md` with an actionable checklist, problematic snippets, optimized snippets, and remaining cleanup targets. The scan found no active TODO/FIXME markers, but did identify legacy empty catches, remaining broad `new Color` paint allocations, `Point` pathing allocations, and lower-priority `String.format` uses outside the newly touched hot loops.

Added `FramePacingAndStressFramework`, `FrameLimiterEngine`, `HighPrecisionFramePulseLoop`, `FrameTelemetrySnapshot`, and `RenderStressTestCoordinator`. `GameOptions` now persists `isFrameLimited`; the Graphics tab exposes `FRAME LIMIT ON/OFF`; and the existing target-FPS setting now cooperates with a nanoTime pacing gate while still allowing an explicit uncapped diagnostic mode. The diagnostics overlay now shows rolling 60-frame FPS, ms/frame, peak FPS, variance, active stress elements, and limited/uncapped state.

Added a bounded render stress mode reachable from QoL as `RENDER STRESS`. The stress coordinator floods the renderer with primitive-array particles, projectile/collidable updates, lightmap work, and synthetic ray-intersection load, then prints a final telemetry report when stopped. Normal-mode lightmap rendering no longer allocates a new `LightSource` and `Color` every frame for the HUD light, and the shared HUD no longer uses `String.format` for endurance text.

Updated the experimental first-person viewport so light objects on blocked wall tiles render as wall-mounted overlays/tints on the wall column, while light objects on walkable tiles continue to render as ordinary billboarded objects. Optimized first-person interaction and lighting math by replacing hot `Math.hypot` checks with squared-distance comparisons where exact Euclidean distance was not required. Added `FramePacingStressAuditSmoke` to verify limiter behavior, uncapped mode, metrics, and the render stress coordinator.

## 0.9.10ij — Loot/drop authority and zone container injection

Added `LootDropSystemAuthority` as the central death/container loot resolver. NPC deaths now adapt carried armor/equipment/ammo/medical drops by faction, role, and sector-zone tier; noble and servant profiles trend toward lower item drops and higher Imperial Script, while mutants/cultists/heretics carry zero script and use the high raw-drop profile. Corpse markers now store one-time Imperial Script in their marker state and clear it after collection. Ground-drop dispositions are represented through persistent `ground-loot` containers placed on nearby walkable tiles where possible, falling back into the corpse container if no safe adjacent tile exists. Room/container cache seeding now has a separate secure 2% zone-purpose injection path for manufacturing, Guard/PDF, noble, trash/scavenge, cult/mutant, and civilian tables. Added `LootDropSystemSmoke` covering faction scaling, script rules, table selection, NPC inventory adaptation, and corpse integration.

## 0.9.10im — Windows Java 17 Launcher Discovery Repair

- Reviewed uploaded `launch-client.log` from Windows testing. The direct launcher was resolving `C:\Program Files (x86)\Common Files\Oracle\Java\java8path\java.exe`, which was Java 8.
- Replaced the client and server Windows launchers with `.bat` wrappers that delegate to PowerShell launch scripts.
- Added version-aware Java discovery that checks bundled runtime, `JAVA_HOME`, common Java 17 vendor install folders, then PATH, rejecting any candidate below Java 17 before loading game classes.
- Updated Windows quick-start documentation with the exact Java 8 / class-file-version 61 failure signature and the expected fix path.
- Kept the direct zip route as the primary Windows debugging path until native installer/runtime bundling is validated on Windows.


## 0.9.10in — Windows Java Version Probe Repair

Patched the Windows direct launchers after user logs showed valid installed Java candidates (`jdk-17`, `jdk-26`, and Oracle `javapath`) being reported as `version=unreadable :: major=-1`. The root cause was launcher-side probing: Java writes `-version` output to stderr, and strict PowerShell error handling can treat that stderr stream as a failed native command. Client and server PowerShell launchers now use `System.Diagnostics.ProcessStartInfo` to capture stdout/stderr as ordinary text, parse the version safely, and write UTF-8 logs. Updated `WindowsDirectLauncherSmoke` to reject reintroduction of the brittle `XshowSettings`/stderr-redirection probe path.

## 0.9.10iu — Portrait Gating / Options Containment / Sector Audit Tile Repair

Repaired name-locked portrait leakage by making standard non-celebrity portraits the default for fallback profiles, character generation, and ordinary NPC rendering. The `name_locked` portrait partition now resolves only through the explicit matching profile/character gate or through the existing noble-zone seeded NPC exception carrying `nameLockedProfileKey`; generic player/NPC fallback paths no longer select celebrity/name-locked portraits.

Reworked the Options surface so tabs stay attached to the top of the owning options frame, the former empty text-backer panel is now a real unified controls container, and explanatory paragraphs render inside one containing text panel instead of drawing separate per-line backing strips. Added a dedicated Text tab for text scale, GUI/chrome scale, text crispness, and hover-help density. Display now keeps windowed/borderless/exclusive fullscreen, resolution selection, apply display, and screensaver controls. Quality-of-life controls are kept in their own QOL tab rather than appearing as a repeated button inside other settings pages.

Restored floor-tile variation in Sector Audit and normal tile-icon rendering by binding deterministic floor variant aliases and compiling floor descriptors to variant art keys instead of collapsing most ordinary floors to one flat repeated cell. This preserves the descriptor model while making the audit view useful again for visual diagnosis.

Verification: Java 17 source compile passed. Ran `NameLockedPortraitLeakRepairSmoke`, `OptionsContainmentTabsSmoke` under Xvfb, and the updated `DisplayGraphicsMenuReorgSmoke` under Xvfb. Zip integrity was checked after packaging.


## 0.9.10iv — Portrait Partition / Save-Menu Containment / Look-Witness Repair

Restored strict portrait-folder partitioning after the name-locked repair exposed a bad all-pool fallback path. Player/profile portraits now resolve only from explicit player-human/profile buckets, with the ordinary administratum human bucket as the only packaged fallback when no explicit human folder exists. Ordinary human player creation no longer draws from noble, Arbites/enforcer, mutant, cultist, animal, servitor, faction, or name-locked celebrity folders. NPC portrait resolution remains folder-authoritative by faction/category: nobles draw from nobles, Arbites from enforcer_arebites, mutants from mutants, cultists/heretics/genestealer cult from their own buckets, and other factions from their named partitions. Name-locked portraits remain unlocked only by explicit matching profile/character logic or the already-approved rare noble seeded exception.

Reworked Save/Load button layout so the buttons are placed from the same panel geometry used by the renderer, are intentionally smaller, sit inside their owning manual/autosave columns, and no longer use full-column strips over the save summaries. The shared button renderer also clips chrome, text, icons, and debug IDs to each button rectangle so accidental old coordinates cannot visually spill outside an owning frame.

Tightened disguise pressure so suspicion/interrogation/hostility checks require actual nearby living faction witnesses with range and line-of-sight, rather than triggering from faction/region state alone. Expanded Auspex/look output to use more of its available panel space and surface tile, ownership, light/noise, room, fixture, base object, NPC, and stack information as character-facing information rather than a one-line minimum render.

Updated durable standards for portrait partition authority and player-facing text discipline. No new standalone repair/audit documents were added.

Verification: Java 17 source compile passed. Ran `PlayerHumanPortraitPartitionSmoke`, `SaveLoadContainmentSmoke`, `NameLockedPortraitLeakRepairSmoke`, `OptionsContainmentTabsSmoke`, `DoomModeViewportSmoke`, `SectorAuditDoorAccessPopulationSmoke`, `SectorAuditDensityRoadSeparationSmoke`, `EulaAndGameplayConsoleSmoke`, `FallbackProfileManagementAuthoritySmoke`, `TextDensityCompactDefaultSmoke`, and `DisplayGraphicsMenuReorgSmoke` under Xvfb where Swing was required.

## 0.9.10ix — LWJGL Runtime Classpath / Doom Backend Dependency Preparation

- Added a real `lib/` runtime dependency drop point and changed the Windows/Linux direct launchers to launch through an explicit classpath so jars under `lib/**/*.jar` are visible to the client and startup preflight.
- Expanded LWJGL preflight reporting from a single class check to core, GLFW, and OpenGL class checks with explicit `lwjgl.classpath`, `lwjgl.status`, `doom.renderer`, and `lwjgl.required` output.
- Added a fail-loud require mode via `MECHANIST_REQUIRE_LWJGL=true` or `-Dmechanist.requireLwjgl=true`; the current client still falls back to Java2D when LWJGL is not required.
- Added LWJGL runtime fetch helper scripts under `tools/runtime/` and documented the expected `lib/lwjgl/` runtime jar drop point.
- Updated Maven dependency declarations to include LWJGL core, GLFW, OpenGL, STB, and Windows/Linux native classifiers for package builds that can access Maven Central.
- Updated installer packaging scripts to copy `lib/` into package input when runtime jars are present.
- Added `LwjglClasspathPackagingSmoke` to verify the dependency probe reports truthfully instead of silently claiming OpenGL support.

Note: this repair wires and verifies the dependency authority path, but the local archive does not contain downloaded LWJGL jars because this execution environment could not fetch binary Maven artifacts. Run the included fetch script or package with Maven access to populate `lib/lwjgl/`.

## 0.9.10iy — LWJGL first-boot bootstrap repair

- Added client-launcher LWJGL bootstrap on Windows and Linux. If the pinned runtime jars are missing, the launcher attempts to install LWJGL 3.4.1 into `lib/lwjgl/` before startup preflight.
- The desktop client now runs preflight with `mechanist.requireLwjgl=true` after bootstrap. Missing LWJGL fails loudly instead of silently falling back while graphics features assume a native backend exists.
- Added a reusable runtime bootstrap tool at `tools/runtime/bootstrap_lwjgl_runtime.py` plus PowerShell compatibility script.
- Server launch remains headless and does not require LWJGL.
- The build archive does not fake-bundle binaries when the packaging environment cannot fetch Maven artifacts; it now installs them on first boot from the pinned Maven Central coordinates.


## 0.9.10iz — Road Shape / Sector Audit Tile-Art Repair

Repaired road descriptor classification after Sector Audit showed ordinary four-wide road lanes being promoted into intersections. The road-shape resolver no longer uses `count >= 3` as an intersection rule; a true intersection now requires opposed road-lane continuity on both axes. Three-neighbor lane cells created by adjacent parallel lanes remain directional north/south or east/west roads instead of becoming intersection tiles.

Aligned renderer-side road semantic selection with the compiled tile descriptor rules so normal map rendering and Sector Audit resolve the same road shape for the same road cell. Road-neighbor checks now fail closed against room-owned cells instead of treating any semicolon inside a room context as valid road continuity.

Forced Sector Audit visual inspection to use packaged tile art whenever tile art is loaded, even if ordinary gameplay tile-icon rendering is toggled off. This prevents the audit map from falling back to flat/gradient ASCII color blocks when the purpose of the audit is to inspect generated road/floor art.

Added `SectorAuditRoadShapeTileArtSmoke` covering three-neighbor directional road lanes, true cross intersections, and required road/floor tile-art alias loading. Updated the existing road separation smoke to the current tile descriptor version.

Verification: Java 17 source compile passed. Ran `SectorAuditRoadShapeTileArtSmoke`, `SectorAuditDensityRoadSeparationSmoke`, and `SectorAuditDoorAccessPopulationSmoke`. Client and server jars were updated and zip integrity was checked after packaging.


## 0.9.10ja — Windows path-length / short runtime asset path repair

Rebased the bundled runtime art root from `assets/art/rebase_0_9_06d` to the shorter `assets/a/r`, shortened repeated 5x5 cell filenames to `rNcM.png`, and renamed the overlong road sheet directory to `roads`. Runtime tile aliases now resolve old long semantic paths to the short packaged files so existing descriptor logic can keep its meaning while Windows Explorer no longer trips over deeply nested repeated names. The ZIP root is also shortened to `m/` and the archive file name is shortened for safer extraction. Added the durable packaging rule that shipped runtime asset paths must remain Windows Explorer-safe and must use folder architecture plus aliases rather than repeated long filenames.

## 0.9.10jb — Windows LWJGL launcher argument repair

- Repaired the Windows PowerShell launcher so the required LWJGL JVM property is passed as a literal argument through array splatting instead of inline native-command text.
- The previous launcher could download LWJGL successfully, then fail preflight because PowerShell/Java argument parsing allowed `.requireLwjgl=true` to be treated as the main class.
- Preflight and client launch now build explicit Java argument arrays before invoking `java.exe`, preserving `-Dmechanist.requireLwjgl=true`, `-cp`, the runtime classpath, and the main class as separate tokens.
- The launch log now records the resolved preflight/client argument list for future diagnosis without requiring the transient console error to be photographed.
- No standalone repair-note document was added.

## 0.9.10jc — Road Direction / Crossing-Gap / Multiplayer Text-Block Repair

Repaired the road atlas direction regression exposed by Sector Audit. Row 3 of the road sheet is now treated as direction-specific rounded/corner art rather than five random variants. Compiled road descriptors now preserve specific corner/end shapes such as `corner_east_south` and `end_north`, and renderer overlay guide-lines draw only toward the actual connected road sides instead of turning every corner into an implicit cross.

Restored limited crossing-gap promotion for sidewalk tiles trapped between opposed true road lanes. Ordinary road shoulders remain sidewalks and still do not contribute to road-neighbor/intersection detection, but a sidewalk caught directly between two road lanes now compiles and normalizes as road continuity so intersecting streets no longer leave sidewalk gaps through the carriageway.

Repaired the Multiplayer menu text block in frame F5002. Connection/history/favorite text now sits in one unified backing panel instead of drawing a separate rounded text backer for every individual line. The status area likewise uses one contained backdrop and plain text lines, reducing stacked-line visual collisions.

Verification: Java 17 source compile passed. Ran updated `SectorAuditRoadShapeTileArtSmoke` and `SectorAuditDensityRoadSeparationSmoke`. Client and server jars were rebuilt.

## 0.9.10jd — Sector Audit Generation Trace / Road Variant Cohesion Repair

Added a Sector Audit generation-trace recorder that captures lightweight world snapshots while a sector is being generated. The audit surface can now replay generation at roughly one captured step per second, with TRACE/STEP controls, cursor focus on the current placement, and a red X marker for sampled rejected placements. Captured phases include reset, plaza/room carving, corridor connection, faction/special assignment, transition stamping, core roads, road-adjacent structure, frontage fixture sweeps, room fixture sweeps, boundary/interwall passes, population, and tile descriptor compilation.

Changed road visual variant policy so each generated zone uses one map-wide road variant set rather than hashing each road tile into unrelated visual variants. Direction and shape still come from descriptors, but north/south, east/west, sidewalk, and intersection variants now stay visually cohesive within a zone. Direction-specific corner/end cells remain direction-specific atlas entries rather than random variants.

Increased road-frontage density for visible roadside infrastructure. The frontage sweep now seeds more vending/newspaper fixtures, more roadside lumen fixtures, and Imperial shrine frontage objects on legal sidewalk/frontage cells while still respecting doorway clearance, road-lane protection, room envelopes, and object-spacing checks.

Updated the Sector Audit command rail with trace playback controls without creating a new standalone repair note. Added `SectorGenerationTraceReplaySmoke` to verify the trace captures room/corridor/road/tile-compile phases and that straight road descriptors use one map-wide road variant.

Verification: Java 17 source compile passed. Ran `SectorAuditDensityRoadSeparationSmoke`, `SectorAuditDoorAccessPopulationSmoke`, `SectorAuditRoadShapeTileArtSmoke`, and `SectorGenerationTraceReplaySmoke`. Client and server jars were rebuilt and zip integrity checked.

## 0.9.10je — Road-First Sector Seed / Faction Room Stamp / Population Support Repair

Replaced the default sector generation order with a road-first seed path. Core streets and road-aligned sector-edge double doors now generate before ordinary rooms. Rooms are then placed into the remaining carveable wall mass from street frontage anchors, connected back to legal road/sidewalk/corridor frontage, and rejected when their grown rectangle touches existing roads, corridors, transitions, rooms, or protected space. Sector Audit trace now shows the new order as reset → roads → road-aligned transitions → rooms/corridors → faction manifest → frontage/interwall/population.

Raised room density pressure and added a road-first room quota pass so Neutral Civilian Floor and similar civic zones generate many more faction-purpose rooms instead of sparse corridor-first scratches. Faction room stamp usage now has more functional variety through habitation, food, logistics, machinery/work, clinic, storefront, training, shrine/social, and warehouse categories, and room-purpose feature stamping now supports machinery, logistics, and shrine rooms directly.

Doubled the passive population target and raised the cap. Population support now attempts to ensure resident/faction rooms carry basic habitation and food support in the room fabric: cots/sleep points and ration/food/source tiles are seeded where a populated room lacks them. Road frontage density was also raised for lumens, vending/news machines, service fronts, bars, medicae fronts, and Imperial shrine fixtures while preserving doorway and road-lane protection.

Kept the Sector Audit generation trace observational: the new trace makes the order visible and marks rejected placements, but game generation remains owned by the world-generation authorities. No standalone repair-note document was added.

Verification: Java 17 source compile passed. Ran `SectorRoadFirstGenerationSmoke`, `SectorGenerationTraceReplaySmoke`, `SectorAuditDensityRoadSeparationSmoke`, `SectorAuditDoorAccessPopulationSmoke`, `SectorAuditRoadShapeTileArtSmoke`, `DisplayGraphicsMenuReorgSmoke`, `OptionsContainmentTabsSmoke`, `SaveLoadContainmentSmoke`, `FallbackProfileManagementAuthoritySmoke`, and `DoomModeViewportSmoke` under Xvfb where Swing was required.

## 0.9.10jf — Specialized corridor / noble terrain tile-set repair

- Bound sewer corridor glyphs to the imported sewer-specific corridor/intersection cells instead of generic corridor/floor fallback art.
- Bound exterior maintenance / void-corridor glyphs to the imported column-5 corridor cells so exterior maintenance ways stop using ordinary interior corridor art.
- Added noble-specific room floor, corridor, and wall aliases from the packaged noble/posh floor and wall pools.
- Updated tile descriptor compilation so noble room floors and adjacent room walls resolve from noble families, while sewer contexts resolve through sewer corridor/wall families.
- Kept interwall/hidden mesh variation separate from room-owned walls: room walls now prefer room/faction context, while interstitial hive mass may continue variant distribution.
- Added `SpecializedTerrainTileSetAuthoritySmoke` to verify sewer corridor, exterior maintenance corridor, noble floor, noble wall, and specialized alias loading.

## 0.9.10jg — Infopedia Tile Ledger / Scroll Target Repair

Repaired the Infopedia tile reference surface so terrain, corridor, wall, road, door, void, and key fixture tile families can be audited from inside the game. Added a new `TILES` Infopedia tab listing floor families, sewer corridors and intersections, exterior maintenance / void corridors, noble corridors, road directional shapes, wall families, doors, and high-value fixture overlays. Each tile entry exposes the glyph, compiled descriptor family, assignment rule, primary icon alias, allowed variants, and runtime alias-load status.

Updated the tile icon preview so the TILES tab draws the actual resolved tile art instead of a generic reference glyph. Variant-backed entries show a compact strip of the first available variants, which makes it possible to see exactly which icon family a room floor, room wall, road shape, sewer corridor, or noble wall is drawing from.

Repaired Infopedia scrolling target behavior by adding explicit LIST and DETAIL scroll-target buttons plus DETAIL up/down controls. This allows the entry index and detail pane to be scrolled deliberately instead of fighting over the same mouse wheel target.

Verification: Java 17 source compile passed. Ran `TileInfopediaScrollAndIconSmoke`, `SpecializedTerrainTileSetAuthoritySmoke`, `SectorAuditRoadShapeTileArtSmoke`, `SectorGenerationTraceReplaySmoke`, and `OptionsContainmentTabsSmoke` under Xvfb where Swing was required. Client and server jars were rebuilt.

## 0.9.10jh — 2D Presentation / Inventory Equipment / Tactical Slate Repair

Repaired 2D lighting containment so rendered light is clipped to the world viewport, respects line of sight, illuminates the blocking wall tile but does not bleed through wall mass, and uses a smoother brighter Euclidean falloff instead of a flat square-like radius. Gameplay visibility now remains player-vision authoritative: distant illuminated tiles no longer become explored or visible merely because a light source exists there.

Changed `doom` mode back to disabled by default in packaged options and repaired first-person mouse-look recentering to use relative deltas, preventing the pitch from getting trapped at the ceiling after the lock cursor recenters. The feature remains experimental and opt-in under the existing warning path.

Added a delete action to the generated-world selector so selected `.mechworld` definitions can be removed from the list safely from inside the world management flow. The delete path refuses paths outside the generated-world directory and refreshes the selector after removal.

Repaired panel input and containment issues from the 2D run: Enter now calls the same adjacent-interaction confirmation path as the confirm button, the look-frame preview icon now sits in a left preview column with text shifted right, look-stack page scrolling is slowed, the panel back button for command-surface panels is owned by the command frame instead of overlaying the top-left world view, tactical slate F6291 only appears on F1, recent events F6731 expands into the freed space, and recent events filters out the player's own action chatter.

Reworked inventory/equipment behavior so starting clothing is materialized into the clothing equipment container instead of duplicated into carried inventory. Equipping from inventory moves the item to the paper-doll equipment container, replacing equipment returns the old item to carried inventory, and unequipping returns the selected equipment item to inventory. The inventory icon grids now accept mouse clicks for highlight control.

Re-indexed several semantic item and fixture art bindings: water-barrel tiles now resolve to barrel art, sleeping cots use bed/cot art instead of the child mat, scrap knives resolve to knife art, and firearm/melee weapon families are split across the correct weapon icon sheets. Room-purpose and support stamping no longer use corpse glyphs as ordinary food/storehouse props; corpse tiles remain reserved for actual dead-entity loot containers and later decay.

Verification: Java 17 source compile passed. Ran `WindowsLaunchHealthCheck`, `VisualLightingAuthoritySmoke`, `DoomModeViewportSmoke`, `SectorRoadFirstGenerationSmoke`, `TileInfopediaScrollAndIconSmoke`, `OptionsContainmentTabsSmoke`, and `PresentationRepairSmoke` under Xvfb where Swing was required. Client and server jars were rebuilt.

## 0.9.10ji — Sector Audit Containment / Plaza-First Density / Maintenance Transition Repair

Repaired the Sector Audit command surface so the auditable sector-type selector no longer renders twice. The free-floating text dropdown was removed, the sector-type buttons are now contained inside the command frame, scroll within that command area, and actually switch the active target generation sector type before rerolling/regenerating the audited surface. The sector-generation controls in F9186 were lowered under the COMMANDS header instead of overwriting the label.

Changed the current road-first sector seed into a plaza-first, road-second seed. Every generated zone now claims room 0 as the central plaza before roads, ordinary rooms, fixtures, or transitions. The existing road-first sequence is then bumped down behind that central civic anchor, and plaza decoration runs after roads so the plaza itself remains the sole first physical generation claim.

Reduced accidental double-wall room separation by removing the extra grown-rectangle rejection halo from road-frontage room placement and reducing the street-frontage gap to one wall layer. This preserves one outer wall layer around generated rooms instead of encouraging two-tile wall bands between rooms and roads.

Doubled the current room-density pressure for generated zones while retaining map-size safety caps. Sector Audit smoke coverage was updated to expect the new plaza-first trace order and higher room count.

Expanded Sector Audit overlays and tile inspection. Overview modes now include interactables, containers, lights, traps, entities, and transition objects in addition to the prior rooms/roads/boundary/descriptor views. The audit look panel now reports the observed tile glyph/layer/family, room profile and owner, NPC presence, map objects, light state, trap state, hazards, and transition contents where present.

Repaired maintenance-corridor orientation logic so maintenance/exterior corridor glyphs orient against nearest same-family corridor tiles rather than inheriting generic road/corridor assumptions. This keeps exterior maintenance corridor runs aligned like roads while still using the proper maintenance-corridor art family.

Isolated and removed the duplicated bounded-hivewall maintenance-band pass that was stamping a second top/bottom maintenance-corridor band. The second call to the exterior maintenance bulkhead and transition-throat restoration pass was removed; the remaining bounded-hivewall pass is the single authority for that envelope.

Moved double-door map transitions off the literal map edge. `D` transitions are now stamped at road ends abutting the inner maintenance bulkhead after the bounded maintenance envelope exists. The legacy edge-door repair path no longer recreates obsolete map-edge double doors, and transition direction resolves by nearest zone boundary so the internal bulkhead door still routes north/south/east/west correctly.

Clarified the Infopedia tile ledger status: it remains a tile/glyph/descriptor/alias ledger for floors, corridors, roads, walls, doors, void, and key fixture overlays. It is not yet a complete index of every graphic in the entire game, especially item, weapon, portrait, object, and UI graphics.

Verification: Java 17 source compile passed. Ran updated `SectorRoadFirstGenerationSmoke`, `SectorAuditRoadShapeTileArtSmoke`, `SectorAuditDensityRoadSeparationSmoke`, `SectorGenerationTraceReplaySmoke`, `TileInfopediaScrollAndIconSmoke`, and `PresentationRepairSmoke`. Client and server jars were rebuilt and zip integrity was checked after packaging.

## 0.9.10jj — Character Knowledge Tree Framework / Infopedia Purchase Detachment

Added the first compiled character-owned Knowledge Tree framework. The pass introduces `KnowledgeNode`, `KnowledgeTree`, `TreeCanvasPanel`, and `KnowledgeMenu`: a Java 17 data model, point/prerequisite unlock authority, zoomable/pannable Swing canvas, and three-pane character progression menu with branch tabs, a points panel, node inspector, and unlock button.

The character dossier now exposes a lower-left `KNOWLEDGE` button. It opens the new modeless Knowledge menu against the active character's persisted `knowledgeCredits` and `unlockedKnowledges`, so purchases mutate the existing character save authority rather than creating a second progression store.

Detached knowledge purchases from the Infopedia surface. The Infopedia knowledge tab remains a reference and audit index for doctrine descriptions, but the former purchase action is replaced by an `OPEN TREE` route into the character-owned Knowledge Tree.

Added default live branches over existing doctrine data: Core Doctrine, Production Doctrines, Faction & Commerce, and Security & Fieldcraft. Generated production pattern knowledge is laid out as sequential quality-band rows per category, while core/social/security branches expose hand-positioned prerequisite chains. This establishes the framework for later perk payloads, recipe grants, construction permissions, services, and character-progression effects without requiring those payloads in this pass.

Added a `GameTest` wrapper for standalone canvas testing and `KnowledgeTreeFrameworkSmoke` for model, prerequisite, point-accounting, branch-construction, and headless canvas-render verification.

Verification: Java 17 source compile passed. Ran `KnowledgeTreeFrameworkSmoke`, `WindowsLaunchHealthCheck`, and `TheMechanistServer.jar --status`. Client and server jars were rebuilt and zip integrity was checked after packaging.

## 0.9.10jk — Knowledge Debug Console Commands

Added admin/debug gameplay-console commands for the new character-owned Knowledge Tree framework: `/knowledge_status`, `/knowledge_add_credits <amount>`, `/knowledge_set_credits <amount>`, `/knowledge_unlock <knowledge name>`, `/knowledge_lock <knowledge name>`, and `/knowledge_list [filter]`. These commands mutate the same `knowledgeCredits` and `unlockedKnowledges` state used by the character dossier Knowledge Tree, refresh any open Knowledge Tree menu, repaint the client, and log the mutation through the debug audit stream.

Knowledge names are canonicalized against the registered `KnowledgeDef` ledger where possible, so commands accept normal spaced doctrine names such as `Condensation Handling` and still return the canonical entry. Unknown names are allowed as raw debug unlock IDs so future nodes can be force-tested before final data registration.

Verification: Java 17 source compile passed. Ran `KnowledgeDebugCommandSmoke` under Xvfb.

## 0.9.10jl — Zone Audit Selector Restoration / Multiplayer Menu Recolumn Repair

Corrected the previous audit-menu interpretation. User-facing launcher/menu/report surfaces now refer to the tool as **Zone Audit** rather than Sector Audit. The Mods/Tools route label is `ZONE AUDIT`, and the audit report frame/title/log route now uses Zone Audit wording while the existing lower-level `SectorAuditRuntimeAuthority` classes remain internal implementation names.

Restored the intended zone-type selector behavior. The command rail now has one `ZONE: <type> ▼` button; pressing it opens a separate bounded pop-up selector instead of embedding all zone choices in the command rail. The pop-up creates a clickable button for every `ZoneType`, highlights the active zone, changes the active audit target when clicked, closes after selection, and regenerates the audit slice using that target. Zone Audit mouse handling now gives the open selector priority over map-tile picking so selector buttons cannot be swallowed by the map click path.

Kept the Zone Audit command controls lowered below the `COMMANDS` header and removed the temporary command-rail zone-button scroller from the previous pass. The command rail remains focused on generation/action controls while exact zone selection lives in its own pop-up.

Enlarged the Multiplayer launcher frame and reorganized F9384 into a bounded two-area layout: the left content area owns direct address, recent servers, favorites, and status/host text; the right third owns the multiplayer command panel. Connection action buttons are arranged in two columns inside the right command column so they do not compete with the explanatory/status text.

Verification: Java 17 source compile passed. Ran `ZoneAuditMultiplayerLayoutSmoke`, `KnowledgeTreeFrameworkSmoke`, `KnowledgeDebugCommandSmoke`, `SectorAuditRoadShapeTileArtSmoke`, `SectorGenerationTraceReplaySmoke`, `SectorRoadFirstGenerationSmoke`, `MultiplayerNetworkSmoke`, and `WindowsLaunchHealthCheck` under Xvfb where a display session was required. Existing world-generation validator warnings about weak doors/short corridors still appear in the generation smokes and were not part of this UI correction pass.
## 0.9.10jm — In-game Knowledge Screen and UI Lighting Containment Repair

- Rejected the detached knowledge-tree `JDialog` player flow. The character Knowledge button now opens an owned in-game Knowledge screen rendered through the main Java2D game surface.
- Added in-engine Knowledge Tree layout, branch tabs, point summary, inspector panel, unlock command, node selection, panning, and mouse-wheel zoom inside the existing game UI pipeline.
- Kept knowledge purchases bound to the existing character-owned `knowledgeCredits` and `unlockedKnowledges` state.
- Updated the compatibility `GameTest` entrypoint so it launches the real game shell instead of a detached knowledge-tree test window.
- Deprecated the detached `KnowledgeMenu.openFor` route so old calls route back into the main game screen rather than showing a separate window.
- Disabled the external Simulation Editor Suite launcher from the Mods/Tools surface until that editor can be converted into an in-game owned screen.
- Removed the normal-mode screen-space warm light bloom that was drawing over the lower-center menu/UI chrome. Map lighting remains owned by the world viewport rendering path.
- Added the durable in-game UI ownership rule to Standards and Practices.


## 0.9.10jn — In-Game Editor Reincorporation / Resolution Default / Render Cycle Removal

Reincorporated the first editor surface into the main game client instead of exposing the legacy detached Simulation Editor Suite window. The Mods/Tools route now opens an owned in-game Editor screen rendered through the main Java2D surface. The legacy external `SimulationEditorSuite.openWindow()` path is disabled for player-facing builds and logs a warning instead of constructing a detached JFrame.

Added a game-owned editor layout with editor tabs, entity selection, universal NEW entry creation, undo/redo buttons, a helper toggle, a bounded inspector/status area, and a Room Editor grid. The Room Editor grid uses model-first mutation: left-click or left-drag places the selected palette item into the underlying `layoutCells` model, right-click or right-drag erases it, and every edit schedules repaint through the main game panel. The editor repository now has safe blank-entry creation plus add/remove helpers so companion editors can share the same NEW-entry contract.

Kept the uploaded editor guidance as the contract for future editor reincorporation: right-side command/palette containment, scroll-contained long selection lists, locked/unavailable entries visible but inert, persistent selected-cost/status display boxes for construction-style menus, helper-banner toggles, and mouse-driven grid editing with clean model/view separation.

Changed the packaged display policy so a launch with no explicit user-selected resolution migrates to the highest detectable supported display mode. The packaged options file records `resolutionUserSelected=false` and the display policy version so older or default low-resolution preferences no longer pin a new install to retro dimensions. If a user explicitly selects a resolution through options, that selection is preserved.

Removed the player-facing render-profile/CRT cycling control path. The Graphics menu no longer exposes the Render Profile button, F10 no longer cycles render profiles, and the old `cycleRenderProfile()` UI method was removed. Lower-level render-scaling support remains available to the client, but the tiring/brittle CRT profile cycling control is not exposed as a normal option.

Updated Standards and the Master Development Plan with the next cleanup gate: perform a dedicated review for stale detached-window code, editor launchers, dialog stubs, and player-facing routes that bypass the main game UI surface before broadening editor functionality.

Verification: Java 17 source compile passed. Ran `InGameEditorInternalSmoke`, `SimulationEditorSuiteSmoke`, `DisplayGraphicsMenuReorgSmoke`, `KnowledgeTreeFrameworkSmoke`, `KnowledgeDebugCommandSmoke`, `VisualLightingAuthoritySmoke`, `ZoneAuditMultiplayerLayoutSmoke`, `MultiplayerNetworkSmoke`, and `WindowsDirectLauncherSmoke`. Client and server jars were rebuilt and zip integrity was checked after packaging.

## 0.9.10jo — Java 17 Classfile Compatibility Launch Hotfix

Rebuilt the client and server jars with `javac --release 17` after the 0.9.10jn package was found to contain Java 21 classfiles. The Windows launcher correctly selected the user's Java 17 runtime, but the preflight failed with a `LinkageError` while loading `mechanist.WindowsLaunchHealthCheck` because the jar classes were emitted as major version 65 instead of Java 17's major version 61.

No gameplay behavior was intentionally changed in this hotfix. This pass preserves the 0.9.10jn in-game editor, highest-resolution launch default, render-cycle removal, in-game Knowledge screen, and Zone Audit corrections while repairing the packaged bytecode target so Java 17 can load the game.

Verification: client and server jars were rebuilt with `--release 17`; jar classfile scan reports max major version 61 for both jars; `WindowsLaunchHealthCheck` passed; `TheMechanistServer.jar --status` passed; ran `WindowsDirectLauncherSmoke`, `DisplayGraphicsMenuReorgSmoke`, `InGameEditorInternalSmoke`, `KnowledgeTreeFrameworkSmoke`, `KnowledgeDebugCommandSmoke`, and `VisualLightingAuthoritySmoke` from Java-17-targeted smoke classes under Xvfb where required. Zip integrity was checked after packaging.

## 0.9.10jp — Standards Reinforcement / Java 17 Release-Gate Discipline

Reviewed the active Standards and Practices file after the 0.9.10jn Java 21 classfile packaging failure. The pre-existing Build and Delivery standard already required Java 17 compatibility, source compilation, jar rebuilds, zip rebuilds, integrity checks, targeted smoke tests, and honest manual-test disclosure. The later Windows direct-launch and Java 17 classfile-gate rules also required Java 17+ runtime detection and classfile major-version validation.

The failure was not a missing standard; it was a process failure: a manual direct-compile path used the local JDK default target instead of enforcing `--release 17`, and launcher smoke testing under a newer JDK was treated as too much proof before the Windows Java 17 launch path exposed the LinkageError. The standard has now been reinforced so every direct `javac` invocation for source, smokes, launch probes, or hotfix classes must use `--release 17`, every shipped jar/class directory must be scanned for classfile major version <= 61, and the verification summary must explicitly report that scan.

Added release-gate helper scripts at `tools/build/verify_java17_classfiles.py` and `tools/build/verify_java17_classfiles.ps1`. These scan jars, loose classfiles, or class directories and fail if any classfile exceeds Java 17 major version 61. This gives future manual/non-Maven packaging passes a concrete stop sign instead of relying on memory, intent, or a successful launch under the wrong JDK.

Verification: ran the new Python classfile verifier against `TheMechanist.jar` and `TheMechanistServer.jar`; it scanned 1824 classfiles, reported highest major version 61, and passed the Java 17 classfile gate. No gameplay behavior was intentionally changed in this documentation/tooling reinforcement pass.

## 0.9.10jq — Zone Generation Diagnostics / Plaza Apron / Room Shell Repair

Repaired the next observed Zone Audit generation defects without reopening broad worldgen expansion as a separate phase. The central plaza now cuts a one-tile street apron immediately after plaza placement and links its four sides into adjacent street fabric after the road pass, reducing the visual double-wall effect around the initial plaza and making the road/plaza relationship explicit in the trace.

Road-first room placement now treats sidewalks and corridors as valid frontage controls and refuses true road lanes as room anchors. Sidewalk frontage chooses only the wall-facing side instead of random inward road-facing directions, and road-first room placement now leaves a one-tile access-corridor gap between the street frontage and room shell so corridor attempts are visible rather than collapsing into direct sidewalk doors.

Added a room-shell normalization pass after special/emergency room placement. It restores boundary cells to wall/door authority where fixture or floor stamping softened a room edge, and it carves non-room corridor access halos outside ordinary rooms so generated rooms present as one authoritative wall shell rather than anonymous double-thick wall masses. This is a diagnostic/legibility repair for the currently observed audit failures; later facility-scale worldgen can replace the halo rule with deeper block-planning once the generation order is fully audited.

Added deterministic seed-owned Perlin noise support for tile-art variation through `PerlinNoiseAuthority`. Floor tile descriptor variants now use smooth level-seed noise instead of purely blocky coordinate hashes, establishing a scalable path for later walls, sidewalks, floor grime, and material variation without changing gameplay placement authority.

Expanded Zone Audit trace presentation. Generation trace steps now carry optional ghost rectangles for placement attempts; rejected placement samples draw a temporary red ghost rectangle/X on the replay step only, so the failed proposal is visible and disappears on the next step rather than remaining as terrain. Zone Audit also draws an upper-left scrolling generation log overlay inside the map frame, with the current trace step highlighted and prior/recent generation steps scrolling upward during playback.

Added `ZoneGenerationDiagnosticsSmoke` covering plaza-before-roads trace order, rejected placement ghost retention, central plaza apron carving, room shell wall/door enforcement, corridor/access-halo density, and seeded Perlin tile variant bounds.

Verification: Java 17 source compile passed with `javac --release 17`. Ran `ZoneGenerationDiagnosticsSmoke`, `SectorGenerationTraceReplaySmoke`, `SectorRoadFirstGenerationSmoke`, `SectorAuditRoadShapeTileArtSmoke`, `ZoneAuditMultiplayerLayoutSmoke` under Xvfb, and `VisualLightingAuthoritySmoke`. Existing validator warnings for squat/short corridor shapes and occasional close large-room doors still appear and remain visible through the audit trace rather than being hidden.

## 0.9.10jr — Knowledge Tree Composition / Balance / Reachability Audit

Implemented the confirmed Knowledge Tree composition plan as an in-game progression-structure pass rather than a new gameplay-payload pass. The active character-owned Knowledge surface now builds eight major branches: Survival & Habitation, Fabrication Doctrine, Infrastructure & Utilities, Industry & Production, Security & Fieldcraft, Medicine & Biology, Civic/Faction/Commerce, and Mechanist/Archeotech.

Expanded the registered `KnowledgeDef` ledger from the earlier framework list into a composed doctrine library covering survival food/water/habitation/salvage, fabrication quality gates, infrastructure/roads/maintenance/lighting/sewers, industrial workflow, security weapons/armor/traps, medicine/sanitation/pharmaceutical practice, civic/legal/faction access, and Mechanist/archeotech/data doctrine. Added the previously missing live registry entries used elsewhere by gameplay, including `Scavenged Schematics` and `Profile Special Unlocks`, so debug unlocks and character-owned discoveries are no longer orphan IDs.

Added `KnowledgeBranchDefinitions` as the branch-composition authority. It owns the eight-branch layout, category/tier/visibility/payload metadata enums, broad quality-spine placement, cross-branch prerequisites, and composition audits. Fabrication Doctrine now has a shared quality spine: Junk, Common, Serviceable, Fine, Masterwork, Noble, Archeotech Pattern Recognition, then Archeotech Production Rites. Category production paths now require both their previous category node and the matching broad quality gate, with diegetic cross-discipline roots such as food requiring ration knowledge, water requiring potable-water discipline, ballistics requiring stub-weapon familiarity, energy requiring power-cell recognition, armor requiring padded-protection literacy, machinery requiring basic machine operation, and so on.

Expanded production doctrine categories to include Tools, Melee Weapons, Armor, Machinery, and Construction Materials in addition to the prior production categories. Production band costs now use the confirmed scaling curve: Junk 1, Common 2, Serviceable 4, Fine 6, Masterwork 8, Noble 11, Archeotech 14. Full unique visible unlock cost is currently 1632 knowledge credits across 306 registered definitions; this is intentionally a long-campaign breadth target, not an expected early-game completion target.

Removed the old demo `Magic Tree` / placeholder demo branch from the remaining preview method; developer preview now returns the same composed in-game branches. The Knowledge menu and in-game Knowledge screen still use character-owned `knowledgeCredits` and `unlockedKnowledges`, and purchases remain detached from the Infopedia.

Added `KnowledgeCompositionBalanceSmoke`, which fails if any registered active knowledge is unplaced, any prerequisite definition is missing, any prerequisite points to an unplaced node, any branch is empty, or any branch contains a cycle. The composition audit currently reports 306 definitions, 306 placed unique definitions, 8 branches, total unique cost 1632, and 4 explicitly cross-listed shared-unlock nodes.

Verification: project source was compiled with direct `javac --release 17` in smaller source chunks because one monolithic compile exceeded the container execution window. Client and server jars were rebuilt from the Java-17-targeted class output. Java 17 classfile scan passed with max major version 61 across both jars and loose build output. Ran `KnowledgeCompositionBalanceSmoke`, `KnowledgeTreeFrameworkSmoke`, `KnowledgeDebugCommandSmoke`, `VisualLightingAuthoritySmoke`, `ZoneAuditMultiplayerLayoutSmoke`, `SectorGenerationTraceReplaySmoke`, `SectorRoadFirstGenerationSmoke`, `WindowsDirectLauncherSmoke`, `WindowsLaunchHealthCheck` under Xvfb, and `TheMechanistServer.jar --status`. Existing world-generation validator warnings about door spacing/weak doors/squat corridors still appear and were not part of this knowledge-composition pass.

## 0.9.10js — Staged Semantic Asset Integration Plan

Created `docs/STAGED_ASSET_INTEGRATION_PLAN.md` as the user-ordered durable migration checklist for the semantic asset overhaul. The file records why the asset migration must be staged, identifies Stage 1 as the immediate implementation target, and lays out stages for registry foundation, in-game Infopedia browsing, high-error category indexing, UI/item preview migration, tile/world render migration, object/machine/construction migration, direct-path audits, portrait/entity partitioning, fallback retirement, and future mod/art-pack registry extension.

Updated the documentation containment standard to recognize this one specialized asset-migration file as an explicit exception rather than opening the door to general planning sprawl. Updated the asset authority standard to require new graphical references to move toward exact 8-character semantic asset IDs, player-facing registry entries with names/types/descriptions, in-game Infopedia auditability, and adherence to the staged plan before render-path replacement.

Updated the Master Development Plan Phase 2 text so the semantic asset migration now has an active sub-plan and Stage 1 is the next ordered target. No runtime code, source behavior, or jar bytecode was changed in this pass.

Verification: documentation-only pass; no Java source changes were made and no jars were rebuilt. Ran the Java 17 classfile verifier against the existing packaged client/server jars to confirm the carried-forward bytecode still targets major version 61, and checked zip integrity after repackaging.


## 0.9.10jt — Semantic Asset Registry Foundation

Implemented Stage 1 of `docs/STAGED_ASSET_INTEGRATION_PLAN.md` as a backend foundation pass, not a renderer migration. Added the Java 17 semantic asset model and loader package under `src/mechanist/assets/`: `AssetMetadata`, `AssetType`, `AssetRegistry`, and `AssetManager`. The registry loads tab-separated semantic asset rows from `assets/indexes/semantic_asset_registry.tsv`, groups them by type, resolves file paths relative to the project root, supports case-insensitive exact ID lookup and simple text search, and exposes an audit for unknown types, blank descriptions, duplicate IDs, and missing paths.

Added `AssetManager.getAsset(String assetId)` as the cached image access point for future migration stages. It returns cached `ImageIcon` instances for valid IDs, attempts lazy default registry loading from the working project root, and returns a generated magenta missing-asset fallback instead of throwing when an ID or image path is invalid. This establishes the semantic-ID backend while leaving existing game rendering on legacy routes until later controlled stages.

Added the starter semantic registry at `assets/indexes/semantic_asset_registry.tsv` with 26 entries across 13 categories. The starter set covers the current high-error families and bootstrap categories: water barrel versus supply shelf, hab cot and worn bed, scrap knife versus bolter/heavy ranged weapons, armor and newspaper icons, bulkhead/sewer/maintenance walls, roads, sidewalks, industrial/sewer/exterior-maintenance corridors, corpse loot/decay containers, machine fixtures, UI icon, and a safe bootstrap portrait-class sigil.

Added `SemanticAssetRegistrySmoke` and `SemanticAssetImageSmoke`. These verify exact 8-character IDs, required type coverage, non-empty names/descriptions, case-insensitive lookup, search behavior, path existence, image decode validity, cached icon reuse, and missing-asset fallback behavior.

Updated the staged asset plan, Master Development Plan, Standards and Practices, and new-conversation briefing so the durable next target is Stage 2: the in-game Infopedia Semantic Asset Browser. No old direct image path calls were migrated in this pass, and the world renderer was not altered.

Verification: Java source compiled with `javac --release 17`; `SemanticAssetRegistrySmoke` and `SemanticAssetImageSmoke` passed with the headless Java2D flag; client and server jars were rebuilt from Java-17-targeted classes; `WindowsLaunchHealthCheck` passed under Xvfb; `TheMechanistServer.jar --status` passed; Java 17 classfile verifier passed with max major version 61; zip integrity was checked after packaging.


### 0.9.10ju — Stage 2 Semantic Asset Infopedia Browser

Implemented the next stage of the staged semantic asset program. Added `SemanticAssetInfopediaAuthority` and an in-game `ASSETS` tab to the existing Infopedia surface. The browser reads from `AssetManager.registry()`, lists registry-backed asset rows, cycles asset purpose/type filters, accepts a focused text filter for ID/name/path/semantic-description search, displays exact 8-character IDs, type/purpose, path/URI, semantic description, and renders the selected image preview through `AssetManager.getAsset(id)`. This is intentionally game-owned UI, not a detached Swing dialog. Added `InfoPediaSemanticAssetBrowserSmoke`. No broad item/tile/world renderer migration was done in this stage; Stage 3 is high-error category indexing and semantic reconciliation.


## 0.9.10jw — Stage 4 Semantic Asset UI/Item Preview Migration

- Continued the staged asset integration program with Stage 4.
- Added `ItemSemanticAssetAuthority` as the migration bridge from carried item/player-facing labels to exact semantic asset IDs.
- Routed inventory/carry-stack mini icons through `AssetManager.getAsset(assetId)` before falling back to the legacy icon alias classifier.
- Added semantic asset summaries to cataloged item Infopedia/detail output so item entries expose the resolved ID/type/name.
- Added `ITEM-G01` as a visible generic item fallback row in the semantic registry.
- Added `SemanticAssetItemPreviewMigrationSmoke` to prevent high-error regressions: scrap knife versus bolter, water barrel versus shelf, cot/bed, clothing/armor, and newspaper/paper rows.
- No map tile/world renderer migration was performed in this pass; Stage 5 remains the next planned asset-integration target.
## 0.9.10jx — Stage 5 Semantic Asset Tile Descriptor Migration

Implemented Stage 5 of the staged semantic asset integration program. Added `TileSemanticAssetAuthority` to map compiled tile-art aliases to stable eight-character Semantic Asset Registry IDs. `CompiledTileDescriptor` now records primary, underlay, and overlay asset IDs and includes them in its inspect line, which makes Zone Audit tile look/report output show semantic identity beside glyph/descriptor identity. Tile Infopedia details now show semantic ID/type summaries for every allowed alias it lists.

Expanded `assets/indexes/semantic_asset_registry.tsv` from 278 carried-forward Stage 4 entries to 334 entries by adding floor-panel rows, noble/posh floor rows, and active door/transition fixture rows. These rows support floor, void, noble surface, and double-door transition inspection by ID rather than by hidden alias path.

The Java2D tile renderer now tries registry-backed semantic tile images first for compiled descriptor primary/overlay/underlay art, then falls back to the older tile-art alias and glyph route if the semantic ID is unmapped or missing. This keeps world rendering stable while migrating road, sidewalk, corridor, wall, floor, door, and common overlay/fixture art toward registry ownership. Added `SemanticAssetTileDescriptorMigrationSmoke` to verify alias-to-ID coverage, registry image loading, descriptor asset fields, water-barrel overlay resolution, and Tile Infopedia semantic reporting.

Verification: Java source compiled with `javac --release 17`. Ran `SemanticAssetTileDescriptorMigrationSmoke`, `SemanticAssetRegistrySmoke`, `SemanticAssetImageSmoke`, `InfoPediaSemanticAssetBrowserSmoke`, `SemanticAssetHighErrorReconciliationSmoke`, `SemanticAssetItemPreviewMigrationSmoke`, `SectorAuditRoadShapeTileArtSmoke`, `VisualLightingAuthoritySmoke`, `ZoneAuditMultiplayerLayoutSmoke`, `WindowsDirectLauncherSmoke`, `WindowsLaunchHealthCheck`, and `TheMechanistServer.jar --status`. Existing world-generation validator warnings about weak doors, large-room door spacing, and squat corridors remain visible and were not part of this asset migration pass.


## 0.9.10jy — Semantic Asset Stage 6: Object, Machine, Fixture, Construction, and Editor Palette Migration

- Added `ObjectSemanticAssetAuthority` as the Stage 6 semantic resolver for construction recipes, built base objects, map fixtures/interactables, traps, lights, and in-game editor palette entries.
- Expanded `assets/indexes/semantic_asset_registry.tsv` with Stage 6 `BLD-*` construction/build rows and `FTR-*` fixture rows for storage crates, workbenches, barricades, alarms, counters, turrets, lab fixtures, light fixtures, switches, sensors, explosives, and terminals.
- Construction button tips and build-detail panels now expose semantic object asset summaries and registry-backed previews for selected construction.
- In-game Room Editor palette buttons/grid painting now show registry previews and asset IDs for selected palette entries.
- Map object rendering, look-stack previews, and Zone Audit object/light/trap inspection now try semantic registry previews/IDs before legacy tile aliases.
- Added `SemanticAssetObjectFixtureMigrationSmoke`; the smoke audits all current build recipes, editor palette items, selected base object/fixture/light/trap mappings, and missing-icon behavior.


## 0.9.10jz — Stage 7 Semantic Asset Direct Path Audit / Enforcement

Advanced `docs/STAGED_ASSET_INTEGRATION_PLAN.md` through Stage 7. Added `mechanist.assets.SemanticAssetPathAudit`, a source scanner that detects direct graphical image literals and image-loading API usage, classifies them through an explicit allow-list, and compares remaining references against a generated legacy baseline. Added `assets/indexes/semantic_asset_direct_path_allowlist.tsv` for approved low-level exceptions such as `AssetManager`, `AssetRegistry`, `AssetMetadata`, the audit itself, `MediaRuntimeFramework` as the temporary legacy media bridge, and `AppIconAuthority` as the bootstrap icon loader. Added `assets/indexes/semantic_asset_direct_path_baseline.tsv` to quantify current migration debt without allowing silent new drift.

Added `SemanticAssetDirectPathAuditSmoke`. The smoke fails if any unbaselined direct graphical path reference is introduced outside approved low-level surfaces, and it also re-runs semantic registry path-existence auditing. This is intentionally a guardrail stage rather than a wholesale fallback-retirement stage: remaining legacy usages are counted and baselined, not hidden. Future feature work should route graphical references through semantic IDs or deliberately update the baseline as an acknowledged migration-debt decision.

Verification: Java source compiled with `javac --release 17`; `SemanticAssetDirectPathAuditSmoke` passed with 263 findings, 222 approved low-level/media-bridge findings, 41 baselined legacy findings, and 0 unbaselined runtime references; registry path audit reported zero missing paths. Existing Stage 1–6 semantic asset smokes and selected gameplay smokes were rerun; client/server jars were rebuilt from Java-17-targeted output; Java 17 classfile scan passed with max major version 61; zip integrity was checked after packaging.

## 0.9.10ka — Semantic Asset Stage 8: portrait/entity partitioning

- Added `PortraitSemanticAssetAuthority` and `assets/indexes/semantic_portrait_entity_partitions.tsv`.
- Expanded the semantic asset registry with partitioned portrait/entity-art rows for human/civic, faction, creature, servitor/automata, mutant, child, servant, medical, military, religious, noble, and name-locked profile portrait families.
- Preserved the rule that ordinary character creation may only draw from explicitly allowed human/profile partitions; name-locked, creature, servitor, mutant, child, and hostile/nonhuman partitions remain separate.
- InfoPedia portrait asset detail now reports Stage 8 partition metadata.
- Added `SemanticAssetPortraitPartitionSmoke` to verify registry placement, partition flags, name-locked coverage, and no restricted-art leakage into the player pool.
## 0.9.10kb — Semantic Asset Stage 9: legacy fallback retirement and registry hardening

- Added `AssetRegistryHardeningAuthority` and `SemanticAssetRegistryHardeningSmoke`.
- Added typed missing-art fallbacks through `AssetManager` so missing migrated assets expose their expected family instead of collapsing into one generic fallback or unrelated legacy art.
- Narrowed item preview fallback: item previews now resolve semantic ID/typed missing art directly instead of consulting loose legacy item glyph aliases.
- Narrowed object/map-object and look-stack fallback: registry/typed missing art is now the player-facing path for migrated object and fixture previews.
- Hardened tile rendering: a descriptor with a semantic asset ID now produces typed missing tile art when the registry image fails; legacy tile aliases remain only for descriptors that have not yet received a semantic ID.
- Added `assets/indexes/semantic_asset_deferred_legacy.tsv` to identify the remaining bounded legacy graphical bridge areas with owner, reason, and next action.
- Updated the Stage 5 tile smoke to accept both original `ROAD-*` and expanded `ROD-*` semantic road IDs.


### 0.9.10kc Phase D — Manifest-Driven Runtime Asset Loading Bridge

Promoted the Phase C generated-path registry preview into the active semantic registry, preserved the previous legacy registry as a backup, copied the runtime/tier manifests into `assets/indexes`, added `GeneratedAssetRuntime`, and routed `AssetManager` path resolution through the generated-art tier selector.  The project now includes the promoted low_32 generated payload subset needed by the active registry while keeping the full generated art body external for later packaging.

# The Mechanist - Repository Workspace

This repository is the development workspace for The Mechanist. It is not the final installed client layout.

The intended user delivery path is:

```text
PACKAGE_installer -> PACKAGE_launcher -> PACKAGE_client -> packaged server payload
```

## Top-Level Workspace Map

- `PACKAGE_client/` - shipped client/runtime package boundary. Client launch files, runtime assets, config, bundled libraries, server payload material, modding examples, and client-facing notes belong here.
- `PACKAGE_launcher/` - thin launcher and package-orchestration workspace. Launcher code, launcher resources, package acquisition, verification, diagnostics, update, rollback, and launch handoff belong here.
- `PACKAGE_installer/` - installer/native packaging workspace. Installer scripts, wrapper material, packaging notes, and installer-facing docs belong here.
- `ROOT_docs/` - durable governance, standards, development history, milestone planning, handoff, and archived planning material.
- `ROOT_SRC_assets/` - protected source asset vault. These files are upstream source material and must not be edited in place.
- `ROOT_tools/` - developer tooling and verification helpers that are not part of the shipped runtime package.
- `scripts/` - current build/package automation that has not yet been moved under a tighter owning workspace.
- `src/` - Java source tree while the build remains Maven-rooted.

## Asset Rule

Do not use documentation manifests as a substitute for the real file architecture.

`ROOT_SRC_assets/` is the preserved source vault. When an asset is transformed, renamed, compressed, resized, cleared for runtime use, or otherwise made game-ready, place the resulting file in the folder where the consuming runtime actually loads it, such as `PACKAGE_client/assets/` or `PACKAGE_launcher/java/src/main/resources/assets/`. Documentation may describe the rule, but it must not create a pointer-only layer that pretends assets have moved.

Generated indexes may exist only when code, tooling, or packaging consumes them. They are not the authority for where files belong.

## Current Development Focus

The current line is public-safe rebase and delivery-path cleanup. Older prototype identity is being replaced by original civic-industrial setting architecture while preserving useful simulation, client, launcher, and server systems.

## Durable Governance

Read these before code, package, or asset work:

- `ROOT_docs/MASTER_DEVELOPMENT_PLAN.md`
- `ROOT_docs/STANDARDS_AND_PRACTICES.md`
- `ROOT_docs/DEVELOPMENT_HISTORY.md`
- `ROOT_docs/MASTER_GOVERNANCE_REVISION_II.md`

## Developer test facility

In a running local game, enter `/devroom` in the authoritative admin console. This pauses the campaign and opens an isolated test character in a facility with player-only teleport doors. `/devroom exit` or the return portal resumes the original campaign; test inventory, currency and faction changes stay in the facility. Saving a developer session over campaign saves is blocked.

- `/devroom combat` opens the hostile arena. Physical controls spawn humanoids, rogue machines and animals, select equipment tier and batch size, and clear the arena. `/devroom spawn CULTIST 3 2` and `/devroom animal sump-rat-swarm 2 1` are examples.
- `/devroom systems` opens exhibits from every build recipe and registered hardware fixture type. The console can grant workshop knowledge, licensed blueprints and test resources; exhibits use the existing machinery and service interfaces.
- `/devroom trade` opens the pinned merchant gallery, indexed by every visible faction, seven vendor categories and eight qualities. Its console selects a merchant, teleports beside it, sets standing for gate tests, and purchases five standing for 100 Script in the test session. Ordinary trade access and prices remain active.
- `/devroom supplies` teleports to category containers containing five of every registered item at every quality. `/devroom restock` replenishes withdrawals.
- `/devroom help` lists the full command syntax.

Start a controlled match with `/devroom match 5 mixed 71000` (size, preset, seed). Sizes are 1, 5, 10 and 20. Presets are `knives`, `rifles`, `mixed`, `heavy`, `intellect`, `wall` and `skills`. `/devroom watch` toggles automatic observation, `/devroom step 1` advances one turn, and `/devroom combatstats` reports execution/refusal counters. Teams engage each other while the player observes. The combat console exposes these controls.

Supply a team with `/devroom support A Medkit` or `/devroom support B Street Stimm`; each living member receives one finite dose. `/devroom armor A Guard flak vest` and `/devroom armor A Guard flak helmet` set worn test protection. NPCs spend actions on treatment, augmentation and equipping carried armor. Normal player Use/equip actions share the same treatment, timed-effect and armor authorities. See the [healing, stims and armor test gallery](diagnostics/combat-support-20261026/index.html) for registry/quality coverage, recorded fights and balance limits.

The [combat execution gallery](diagnostics/combat-execution-20261026/index.html) contains 560-case results, recorded fights, event diagnostics and validation limits. The [earlier baseline](diagnostics/combat-scenarios-20261026/index.html) records the missing execution paths before the fixes. Player and NPC attacks now resolve through the shared actor combat authority, with skill, range/line checks, finite ammunition and damage. This is initial combat tuning, not finished balance certification; obstacle navigation, broader combat effects and complete defeat/loot presentation need further work. Some registered hardware exposes inspection or service previews through its existing owner.

## Cybernetic limbs and recorded test batches

Character > Medical > Cybernetic now fits, replaces, removes and services arm, leg, hand and foot assemblies. Choose the player or an adjacent patient; supplies must be carried by that patient. Installation requires Cybernetic Surgery, Mechanics/Intellect 6, a nearby medicae stall or sterile bench, a sterile surgical kit and calibration tools. Military parts additionally require Emergency Medical Authority. Removed assemblies become salvage; they cannot be refitted as fresh parts. Repair kits restore hardware, power cells recharge powered limbs, and reusable tools clear calibration faults. Organic medicine cannot repair hardware or regrow limbs. Bandages stabilize; Stitchdream and Pale Nurse recover tissue over later turns. The rare regenerative lore medicine is the **Draught (stabilized Cineric Vitae)**; its regeneration execution remains reserved.

`/devroom cybernetics 25` sets a seeded per-candidate augmentation chance for subsequent arena matches and humanoid spawns; use 0 for organic controls or 100 for fully fitted pools. This is an isolated test setting, not a campaign population rule. [Watch native battle videos with sound](diagnostics/native-battles-20261026/index.html), or run `/devroom match 20 rifles 71000` followed by `/devroom watch` for live observation. The native videos use the actual tile and Doom renderers and packaged positional WAV cues. The earlier [state-diagram recordings](diagnostics/cybernetics-20261026/index.html) remain available for detailed event and hardware inspection.

Use `/devroom cover 16 81000` for seeded Sandbag Line and Scrap Barricade layouts, or `cover 0 81000` for open controls. The combat console includes cover controls. The `pursuit` preset starts knife-equipped groups apart to test closing and routing; blocked firing lanes now permit repositioning. Controlled matches grant the isolated observer Vision 42 within the existing 14-tile cap and face the observer toward combat. Normal visibility and wall culling still apply.

The [test batch ledger](diagnostics/test-batches-20261026.json) links historical baseline failures, fixes, scenario results, seeds, source/build hashes, raw root-level logs and rerun scripts. Commit reports and raw outcome/event data with the supporting code; keep generated classes, runtime user data and large distribution JARs out of evidence commits. Each report describes its tested build; earlier results are retained as historical evidence rather than relabeled as current validation.

Weapon audio follows catalog families, so Light Rifle uses the existing las sound, chain weapons use the existing chainsaw strike, and plasma/flame/electrical/heavy/slicing/blunt weapons retain their dedicated effects. [Audition the existing weapon sounds](diagnostics/native-battles-20261026/weapon-audio/index.html) and inspect the complete binding/PCM audit. The rebuilt client and refreshed native videos use these mappings; an already running older client does not hot-reload Java changes.

## Distribution Notes

A user should not need the whole development repository to run the launcher. The installer should install the thin launcher; the launcher should acquire and verify client, server, and support-library packages. Package manifests are allowed for acquisition and integrity checks, but they must not replace the physical runtime asset layout. The game client may verify package completeness, but it must not download dependencies during game launch.

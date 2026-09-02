# Development History

This is the active development ledger for The Mechanist after the continuity reset of 2026-07-17.

The complete prior active ledger is preserved unchanged at:

`ROOT_docs/archive/DEVELOPMENT_HISTORY_MILESTONE_LEDGER_ARCHIVE_2026-07-17.md`

Earlier archives remain at:

- `ROOT_docs/archive/DEVELOPMENT_HISTORY_MILESTONE_LEDGER_ARCHIVE_2026-06-05.md`
- `ROOT_docs/archive/DEVELOPMENT_HISTORY_PRE_MILESTONE_DEVELOPMENT.md`

## Ledger purpose and authority

This file records only new completed work after this reset. It is not the roadmap and must not be used by itself to determine the current milestone.

Milestone authority is resolved in this order:

1. `MASTER_DEVELOPMENT_PLAN.md` and the dedicated milestone documents indexed by `ROOT_docs/MILESTONE_INDEX.md`.
2. Implemented source authorities and their focused smoke tests.
3. Registration in `Gate3PlayerFacingTextSmokeSuite` and the persistent GitHub validation workflows.
4. This active ledger, which records the verified slices completed after the reset.
5. Archived development-history files, which provide historical context but do not define the current development position.

Milestone headings in an archive may be interleaved because work was completed across several system lanes. The last heading in an archived file is therefore not proof of the active milestone.

## Current development boundary

Active development is **Milestone 06: vehicle systems**.

Milestones 01 through 05 are treated as the completed foundation for current work. Older audit-only Milestone 03 construction-contract chains remain in source and Gate 3 where they provide regression coverage, but they are not the active sequential development lane.

The Milestone 06 implementation boundary currently includes:

- An authoritative persistent vehicle runtime schema attached to existing `MapObjectState` save authority.
- Generated vehicle identity, class, manufacturer, model, variant, production batch, ownership, legal class, condition, components, access, history, purchase, repair, salvage, and seizure behavior.
- Vehicle commerce and interaction routed through authoritative ownership and access decisions.
- Constrained local vehicle transit, parking, route validation, cursor feedback, and operation-state reporting.
- Strategic vehicle transit readiness, source-coordinate persistence, and atomic strategic transfer commits.
- Persistent fuel or power accounting used by route readiness and execution.
- Persistent maintenance, damage, loss, recovery, seizure, repair, salvage, and faction-strategy integration.
- Persistent driver, operational crew, passenger, and cargo-custody manifests, including capacity and permission enforcement.
- A shared structural-scale combat authority for vehicles, machines, durable fixtures, doors, walls, and structural terrain that leaves actor combat on the established actor-combat lane rather than creating parallel combat state.
- Structural attack previews and confirmations that account for weapon force, penetration, range, ammunition, target armor, and target integrity; below-scale attacks report readable ineffectiveness instead of inventing damage.
- Structural impact persistence routed back into the owning vehicle component, `BaseObject` integrity, `MapObjectState` structural stock, or existing terrain-integrity ledger, with dirty-region mutation emitted only when authoritative damage actually changes state.
- Vehicle operation feedback backed by the existing physical vehicle state, including bounded transient pulse/headlight feedback and distance-aware ambient operating sound that stops when the physical operation session ends.
- Live vehicle dashboard and Infopedia dossier text built from existing runtime, fuel, manifest, motor-pool, transit, loss, access, and operation-feedback authorities rather than a parallel presentation ledger.
- Bounded local-route and vehicle-history retention so player-facing inspection remains useful without unbounded save-state growth.
- Bounded fuel, motor-pool, strategic-transit, and manifest histories so repeated vehicle operations cannot grow those persistent string ledgers without limit.
- Vehicle-aware faction market contracts that distinguish fuel, ordinary repair, critical repair, salvage, and persistent target ownership instead of generating generic unbound maintenance work.

Current validation registration includes the Milestone 06 vehicle runtime foundation, transit, operation-feedback, access, strategic-transit, loss, maintenance, manifest, and structural-scale combat smoke chains through `Gate3PlayerFacingTextSmokeSuite`.

The source boundary immediately before this reset was commit `0bd08d1aaebcbf6825524496a2c3284ae9eb518e`. The preserved prior ledger blob was `d73e42f8d9d1e904f22d0be67c7bbf20890a6793`.

### Recovered post-reset structural-combat ledger evidence

Repository history shows that the structural-scale combat slice was implemented after the 2026-07-17 continuity reset but was omitted from this active ledger. This reconciliation records existing repository truth; it does not represent a new runtime verification run.

- `305adcc1ef451dac4392d6feedadb81425c26f4d` added `StructuralScaleCombatAuthority` as the shared large-target combat boundary and reused vehicle, object, base-object, and terrain integrity owners.
- `c3cd02db244d0dcf92be7e5c20390b1ee929e17d` added the focused structural-scale combat smoke.
- `4542c51707b35567aef6c0d5d32401e1e986b1ce` routed large-target combat controls through the structural authority while preserving the existing actor-combat lane.
- `e25b924095c2af01a0a9bb296c7a13b053f41731` registered the structural-scale combat smoke into the existing Milestone 06 vehicle validation chain.
- `f0f2c010838488f20265e09728b45b468a64d24f` hardened structural impact persistence and avoided dirty-region mutation for ineffective/no-change impacts.

### Recovered post-reset vehicle presentation, history, and contract evidence

Further repository reconciliation shows that several later Milestone 06 slices were also implemented after the continuity reset but were not represented in this active ledger. As above, this section records source and commit evidence already present on `main`; it is not a substitute for a fresh Java 17 runtime verification pass.

- `e1f9e2e7dd744dc94f37a9ee21d2a2c908f0068b` extended `VehicleOperationFeedbackAuthority` with bounded ambient running-audio refresh tied to the loaded physical vehicle and the same authoritative operation session that drives visual feedback.
- `ca67000d2ec227126d819b599ce0e1abb13c81fa` added focused proof for the ambient running-audio lifecycle, followed by `e2cbaf324aef3ce5fbf569c163a9684cbe7045ab` and `36f1ede0c7e8f92a94394c49534ba0f444469a34` to bound audible range and verify cleanup behavior.
- `0803fcbd2939d27dac56b2a2e8b835ebbf92ecd7` added `VehicleInfopediaBridgeAuthority`, building class dossiers and live dashboards from the existing runtime, fuel, manifest, motor-pool, strategic-transit, loss, and feedback authorities.
- `ca5ec25c9bfe1530ddc5828ed614a8e49e6ea8b8` routed vehicle inspection through that live dashboard and bounded local route-history retention instead of allowing indefinite stock-state growth.
- `42d58d8fe1fcddeedd4cb3e3341af4964fdd85dc` and `8f3ff062f67677688c47fdae7783f8919a66fbae` added and registered focused dashboard/dossier and route-retention proof; later commits hardened history limits and removed raw dashboard identifiers from player-facing output.
- `785049c9d62c6e376e53fb127c7d2b17c8ab08ad` updated `FactionMarketContractAuthority` so critical disabled/wrecked/damaged vehicles are prioritized ahead of ordinary repair work while preserving existing doctrine/readiness and persistent vehicle ownership data.
- `b91c773f04aab2c83ed13bd53d201527a31e82b6` and `52e966cdb488383ef6c9d67d2fd5c1e5cc55a493` verified and registered the vehicle contract-priority behavior.

### Recovered post-reset bounded persistence evidence

The same post-reset commit window contains explicit persistence-growth safeguards that were not called out in the active ledger. These are existing source protections, not newly executed runtime proof in this reconciliation pass.

- `68b974ae21b7a40b50871eaca7caf890bd1243f9` bounds `VehicleFuelAuthority` history strings to the newest 12 entries; `9e95b67b20cb6a201f3674f909f7fe64fbd33731` and `283e38d00de86a123e29f2df32d2aeae1e48d17c` provide the associated focused proof and registration.
- `aab3c247d1e19a7839deafd5711a6fb65aa54bbc` applies the same 12-entry bound to persistent `VehicleMotorPoolAuthority` history; `b5533ba40ce81c05a41847ccd26d35609d455b89` and `a3d7106c444236cdbfe27002a2fbff8a19e3ba5c` prove and register that boundary.
- `61c9126418e1388f9b954f7d72882cdb557dfda8` bounds persistent `VehicleStrategicTransitAuthority` history to 12 entries; `9828096e79e053ac25752503d0856a3e8913b854` and `abdd3c9ea9609958f808bad071245ee4db15f899` prove and register it.
- `9b4e54e401b2317831e60840739c193aa45d1ec0` bounds persistent `VehicleManifestAuthority` histories to 12 entries; `52cf3cc2b1ace957cf73c42696ea1b83c2de6a39` and `3c7a9f0653fae4f1c28d850e3306999afaec5030` prove and register the manifest-history boundary.

## Incremental development protocol

Each new development run must:

1. Read the dedicated Milestone 06 plan and inspect the live source boundary before selecting work.
2. Choose one coherent, user-visible or systemic implementation slice rather than extending an unbounded audit chain.
3. Reuse existing ownership, persistence, economy, faction, movement, UI, and world authorities instead of creating parallel state.
4. Add or expand a focused smoke that proves success, refusal, persistence, and non-mutation boundaries where applicable.
5. Register the smoke through the appropriate Milestone 06 chain and Gate 3.
6. Push sequentially to the single `main` branch.
7. Evaluate `Milestone Validation and Smoke` and `Java 17 Verify and Release` before beginning the next slice.
8. Record the slice here only after the available objective verification has passed; do not claim a release or remote success without exact Actions evidence.

## Current CI and release note

The repository now retains two persistent project workflows: `Milestone Validation and Smoke` and `Java 17 Verify and Release`. Obsolete self-modifying one-shot workflows were retired. The release workflow includes Linux x64 and Windows x64 package construction, Gate 3, synthetic extracted-install tests, final certification, and guarded prerelease publication.

This ledger reset does not itself certify those workflows or publish a release. Their exact run results must be checked after each implementation push.

## Next work

Resolve the next incomplete Milestone 06 requirement from the dedicated milestone documentation and the implemented vehicle authority boundary. Do not infer the next task from archived history ordering, and do not reimplement structural-scale combat, operation feedback, ambient vehicle audio, live vehicle dashboard/Infopedia integration, bounded route-history retention, bounded fuel/motor-pool/strategic-transit/manifest histories, or vehicle contract-priority behavior already present on `main`.

Before choosing the next gameplay expansion, continue reconciling the post-reset Milestone 06 commit boundary against the ordered phase requirements until the first genuinely incomplete dependency-valid requirement is identified. Prefer that implementation slice over adding another audit-only chain.
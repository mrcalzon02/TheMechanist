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

Current validation registration includes the Milestone 06 vehicle runtime foundation, transit, operation-feedback, access, strategic-transit, loss, maintenance, and manifest smoke chains through `Gate3PlayerFacingTextSmokeSuite`.

The source boundary immediately before this reset was commit `0bd08d1aaebcbf6825524496a2c3284ae9eb518e`. The preserved prior ledger blob was `d73e42f8d9d1e904f22d0be67c7bbf20890a6793`.

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

Resolve the next incomplete Milestone 06 requirement from the dedicated milestone documentation and the implemented vehicle authority boundary. Do not infer the next task from archived history ordering. The next entry in this file should describe the first newly completed Milestone 06 slice after this reset and its exact verification evidence.

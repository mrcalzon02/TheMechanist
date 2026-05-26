# New Conversation Handoff - The Mechanist

## Current checkpoint

The active checkpoint is Phase 4 publish-safe client containment and package/handoff hygiene.

Before any code or packaging pass, read:

1. `docs/MASTER_DEVELOPMENT_PLAN.md`
2. `docs/STANDARDS_AND_PRACTICES.md`
3. `docs/MASTER_GOVERNANCE_REVISION_II.md`
4. `docs/DEVELOPMENT_HISTORY.md`
5. `docs/MILESTONE_INDEX.md`
6. `docs/LEGACY_MILESTONE_SOURCE_MAP.md`

## Gate status

Gate 1 documentation and repository hygiene has been checked. The master plan stays concise and points to `MILESTONE_INDEX.md`; the index lists the ordered `00` through `10` milestones; the legacy source map explains where old topical milestone files belong; README remains user-facing; and stale root command-file clutter was removed.

The pre-milestone development history has been archived at `docs/archive/DEVELOPMENT_HISTORY_PRE_MILESTONE_DEVELOPMENT.md`. New completed milestone work should be recorded in the fresh active `docs/DEVELOPMENT_HISTORY.md`.

Gate 2 has started and remains unfinished. The first slices retired the active full-repository Git launcher path, moved native package entrypoints toward `packages/launcher/MechanistLauncher.jar`, and added launcher-side manifest verification for client/server/support packages. The launcher now discovers nearby app-image package layouts, supports install-root and package-seed overrides for staged verification/acquisition, verifies support-library hashes from the runtime manifest, installs from a verified local seed, can repair from rollback, and rejects wrong-schema or wrong-platform manifests. Remaining Gate 2 work is publish-safe authentication for private artifact access or a public-safe artifact channel, remote acquisition/update policy, package trust/signature metadata, and full Maven/jpackage/native packaging verification.

Gate 3 has begun with small player-facing readability slices. Faction contract display lines now avoid internal contract IDs, raw target-zone keys, and generated ident-chip IDs, replacing them with readable contract type, faction, route, and turn-in wording. `FactionContractDisplaySmoke` covers this behavior. Inventory, workshop logistics, event log, save/load, command, diagnostics, loading, trade/conversation, options, look/auspex inspection, error handling, and reference-panel text have also been tightened to avoid raw/debug/token/atlas/log-plumbing/registry phrasing in ordinary UI. The launcher server-join identity bridge was updated to the current special-profile package API, leaving old celebrity package names only as legacy aliases.

## Active boundaries

- Keep detailed planning in the ordered milestone sequence rather than adding new loose planning files.
- Treat legacy topical milestone files as source/archive material only.
- Keep client launchers under `client/launchers/`, launcher bootstrap work under `launcher/`, installer work under `installer/`, server launch material under `client/server/`, and developer tooling under `tools/` or `scripts/`.
- Do not expand into broad worldgen, live external mod loading, public multiplayer networking, or unrelated simulation systems during the current package/handoff line.

## Next pass

Continue the Gate 2/Gate 3 bridge without pretending Gate 2 is closed: keep remote/private artifact acquisition marked open until publish-safe authentication exists, and continue local/offline-verifiable package and player-facing polish slices. Next package work should make the installer -> thin launcher -> client/server payload chain executable from the native app-image without relying on the full development repository layout.

# The Mechanist Graphics Rebase Workspace

This project package intentionally expunges the historical graphical asset payload.

Use this folder as the intake point for the clean source-first rebuild:

- `source/inbox/` — newly uploaded raw source images before classification.
- `source/sheets/` — approved sheet sources after classification.
- `source/singletons/` — approved one-off source images.
- `manifests/` — slice specs, source inventory, semantic mapping, and tier manifest.
- `generated/` — rebuilt quality tiers generated from approved source only.
- `runtime/active/` — final resolved runtime assets after manifest/loader retargeting.
- `runtime/fallback/` — intentional fallback assets only, not old archaeology.

This package is expected to be visually incomplete until the new clean source images are imported, sliced, and regenerated.

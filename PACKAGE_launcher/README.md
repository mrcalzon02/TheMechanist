# The Mechanist — Launcher Workspace

This folder owns thin-launcher and orchestration work.

The intended user path is:

```text
installer → thin launcher → client → server
```

The launcher is not the game client. It is the installed orchestrator responsible for manifest verification, package acquisition, update, rollback, diagnostics, and launching the correct client/server/runtime-support package set.

## Launcher responsibilities

The launcher owns:

- package manifests and compatibility checks,
- client package acquisition/update/rollback,
- server package acquisition/update/rollback,
- support-library acquisition/update/rollback,
- runtime/JVM profile selection before launch,
- visible diagnostics when package identity or dependency verification fails,
- handoff into the graphical client and local/internal server lane.

The launcher must not require the full development repository to exist on the user's machine. It should run from the installed launcher package and acquire verified package artifacts through manifest-controlled routes.

## Non-responsibilities

The launcher should not own live gameplay state, world mutation, saves, character inventory, faction state, or server-authoritative decisions. Those belong to the client/server/runtime authorities after launch.

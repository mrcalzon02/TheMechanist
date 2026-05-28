# The Mechanist — Headless Server Package

This folder owns the server-side runtime materials that ship with the client distribution but must remain independently executable.

The server is not a GUI helper. It is a separate headless executable target that can initialize server storage, report server runtime paths, and host or prepare hosted worlds without requiring the graphical client to be running.

## Intended relationship

```text
installer → thin launcher → client → server
```

The installer installs the thin launcher. The launcher stages and verifies the client package, the headless server package, and required support libraries. The graphical client may start or connect to the local/internal server lane, but the server package must also remain runnable on its own for dedicated or headless use.

## Server-owned contents

- `TheMechanistServer.jar` after build/package staging.
- `launchers/` for server-only launch helpers.
- Server runtime notes and hosting instructions.
- Server storage namespace documentation.

## Runtime separation rule

Server state and server/world files remain separate from single-player client saves. The server package must not assume a graphical display, Swing canvas, or desktop UI. If any server path accidentally depends on client graphics, that is a bug to isolate.

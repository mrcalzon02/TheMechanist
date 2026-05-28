# The Mechanist — Client Distribution

This folder owns client/runtime-facing package material. It is the boundary for files that should be considered part of the client distribution environment rather than repository governance, installer tooling, or launcher implementation.

The intended installed user path is:

```text
installer → thin launcher → client → server
```

The client package is launched by the thin launcher after manifest verification. The client may verify required runtime support libraries, but it must not download dependencies during game launch. Client/server/support package acquisition belongs to the thin launcher and package manifest layer.

## Runtime contents owned by the client package

A built client package should contain or receive through launcher-managed acquisition:

- `TheMechanist.jar` — graphical client runtime.
- `TheMechanistServer.jar` — local/internal headless server runtime distributed with the client package.
- `lib/` or launcher-managed `packages/support/lib/` — runtime support libraries such as LWJGL, platform native LWJGL jars, Netty when used, and controller/input bridges such as Jamepad or a successor.
- `assets/` — runtime art/audio/index assets promoted into the client package.
- `settings/` — default settings templates or packaged defaults only. Mutable user settings must route to user storage.
- `profiles/` — packaged/default profile templates only. Mutable profile data must route to user storage.
- `modding/` — public modding API references and templates when shipped with the client distribution.
- client launch helpers and client/server readme files.

## Storage rule

Mutable saves, profiles, active mods, exported mods, archived mods, logs, generated worlds, and server state must not be written into an installer-controlled application directory. Runtime data belongs under the user-storage authority.

## Current segmentation note

This folder is the new distribution boundary. Some source/build inputs still live at repository root while the build system is being refactored. Do not add new root-level client runtime files; place them here or in the launcher/installer folder that owns them.

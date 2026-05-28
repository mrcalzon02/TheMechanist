# The Mechanist — Local Server Runtime

The server executable is distributed with the client package because the graphical client uses a local/internal server authority lane for single-player and future hosted play.

The intended delivery path is:

```text
installer → thin launcher → client → server
```

The launcher acquires and verifies the client package, server package, and support libraries before the client starts. The server package has its own manifest identity, runtime profile, and storage namespace.

## Run

Linux/macOS:

```bash
./RUN_MECHANIST_SERVER_LINUX.sh --status
```

Windows:

```bat
RUN_MECHANIST_SERVER_WINDOWS.bat --status
```

Direct Java:

```bash
java -jar TheMechanistServer.jar --status
```

## Current authority boundary

The desktop game uses a local internal server lane for single-player world mutation. The separate headless server executable currently initializes and reports the server runtime namespace and supports controlled host-binding checks where implemented.

Remote public hosting, remote client authority, external mod hot-loading, and remote console authority remain closed unless a later verified networking pass explicitly opens them.

## Save locations

Single-player files remain separate from server files:

```text
saves/singleplayer/slot1.mechsave
saves/singleplayer/slot2.mechsave
saves/singleplayer/slot3.mechsave
saves/singleplayer/slot4.mechsave
saves/singleplayer/autosave_hourly.mechsave
saves/singleplayer/autosave_zone_transition.mechsave
saves/singleplayer/worlds/*.mechworld
```

Server state and server/world files remain separate:

```text
saves/server/server_state.properties
saves/server/slots/server_slot1.mechsave
saves/server/slots/server_slot2.mechsave
saves/server/slots/server_slot3.mechsave
saves/server/slots/server_slot4.mechsave
saves/server/worlds/*.mechworld
```

The server state file is the basic authoritative bookkeeping file for the headless runtime. The four server slots mirror the four manual single-player slots but do not share file paths with them.

## Handshake gate

The intended authoritative join lifecycle is:

```text
identity verification → manifest delivery → acquisition/sync → client hot restart → integrity challenge → live world initialization → access granted
```

The server must not stream entity, chunk, room, sector, or simulation tick state during manifest delivery, asset acquisition, or hot restart. A validated client receives live world data only after its mounted content set passes the post-restart integrity challenge.

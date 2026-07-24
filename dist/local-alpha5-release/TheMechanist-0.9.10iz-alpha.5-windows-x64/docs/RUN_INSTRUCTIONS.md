# The Mechanist — Run Instructions

Use only a complete verified limited-alpha package. Do not move individual JAR files away from their manifests, runtime, support libraries, or launch scripts.

Every approved candidate includes its own verified Java 17 runtime. A separate system Java installation is not required and must not be substituted for the bundled runtime during certification.

## Portable package layout

The extracted root contains:

- `launcher/MechanistLauncher.jar`
- `packages/client/TheMechanist.jar`
- `packages/server/TheMechanistServer.jar`
- `packages/support/lib/`
- `runtime/`
- `manifests/`
- `docs/`
- Platform launch scripts

Paths containing spaces are supported.

## Normal launcher and single-player

### Windows

Double-click or run:

```text
Run-The-Mechanist.cmd
```

### Linux

Make the script executable if required, then run:

```text
chmod +x run-the-mechanist.sh
./run-the-mechanist.sh
```

The launcher verifies the bundled package before enabling launch. The initial package source must be `limited-alpha-bundled`.

## Direct client start

Direct start is intended for controlled diagnostics when the launcher itself is under investigation.

### Windows

```text
Run-Client-Direct.cmd
```

### Linux

```text
./run-client-direct.sh
```

The direct client still requires the complete verified package and support-library directory.

## Independent-host remote lobby

The remote lobby is a separate entry point and does not open single-player.

### Windows

```text
Run-Remote-Client.cmd
```

Equivalent explicit command:

```text
runtime\bin\java.exe -cp "packages\client\TheMechanist.jar;packages\support\lib\*" mechanist.RemoteClientMain
```

### Linux

```text
./run-remote-client.sh
```

Equivalent explicit command:

```text
./runtime/bin/java -cp "packages/client/TheMechanist.jar:packages/support/lib/*" mechanist.RemoteClientMain
```

Optional defaults:

```text
--host=127.0.0.1 --port=25565 --server-key=limited-alpha/primary-host --profile=alpha.tester.0001
```

The window keeps these values editable before connection. The server key should remain stable for the same approved host relationship. Profile identity must contain 8–128 letters, numbers, dots, underscores, colons, or dashes.

Allowed game ports are `25500` through `25599`, excluding any port reserved by the runtime policy.

The lobby provides authenticated presence, readiness, typing state, connected-only roster updates, bounded relay messages, stable reconnect identity, and status inspection. It provides no remote world gameplay.

## Headless server

### Windows

Show help and initialize storage:

```text
Run-Server.cmd --help
```

Start an exact loopback host:

```text
Run-Server.cmd --host --no-steam --bind=127.0.0.1 --port=25565 --world-id=alpha-world --world-name="Alpha World" --max-players=4
```

### Linux

Show help and initialize storage:

```text
./run-server.sh --help
```

Start an exact loopback host:

```text
./run-server.sh --host --no-steam --bind=127.0.0.1 --port=25565 --world-id=alpha-world --world-name="Alpha World" --max-players=4
```

Use `--host-once` or `--bind-check` for a bind-and-exit verification. A normal `--host` process remains active until interrupted.

Do not use `0.0.0.0` or `::` unless the approved test specifically requires network-wide listening and the operator understands the firewall and exposure implications.

## Mutable storage

Do not place saves, settings, logs, server ledgers, or remote-client token records inside the application installation.

The remote lobby resolves a user-owned mutable root. It may be overridden for controlled tests with:

```text
-Dmechanist.client.storage.root=<approved writable path>
```

The server test harness may use its separately governed storage override. Do not point client and server storage overrides at the same directory.

## Exit and recovery

- Exit the game and lobby through their normal window controls.
- Stop the server with `Ctrl+C` unless running a one-shot check.
- Preserve the previous candidate and back up mutable data before upgrading.
- A corrupted client resume-token record fails closed; do not edit it to force recovery.
- A corrupted server session ledger prevents that world host from binding until deliberately repaired or removed.

## What to report

Report:

- Candidate version, commit, and platform.
- Exact launch command or script.
- Host address and port after redacting private network information when required.
- Expected and actual behavior.
- Reproduction steps and severity.
- Whether the issue reproduces after clean restart, in a new save, or with a fresh remote profile.

Never include a reusable resume token or attach its custody file.

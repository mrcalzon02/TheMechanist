# The Mechanist — Headless Server Limited-Alpha Guide

The separately packaged server is an exact-address authenticated lobby and bounded relay host for the current limited-alpha line. It owns its own mutable storage namespace, persistent remote-session ledger, stable player identities, token-gated reconnect, hosted-lobby command accounting, and clean shutdown.

It does not own an authoritative remote game world. It does not process movement, combat, inventory, character, map, position, simulation, or hosted-world persistence commands.

## Start and help

Use the platform script from the complete package root.

Windows:

```text
Run-Server.cmd --help
```

Linux:

```text
./run-server.sh --help
```

The help command initializes the dedicated server storage namespace and prints the resolved paths and networking policy.

## Start an approved loopback host

Windows:

```text
Run-Server.cmd --host --no-steam --bind=127.0.0.1 --port=25565 --world-id=alpha-world --world-name="Alpha World" --max-players=4
```

Linux:

```text
./run-server.sh --host --no-steam --bind=127.0.0.1 --port=25565 --world-id=alpha-world --world-name="Alpha World" --max-players=4
```

The normal host remains active until interrupted. Stop it with `Ctrl+C` and allow the shutdown hook to close the binding and flush session state.

## Bind verification

Use either form to bind, report status, close, and exit:

```text
Run-Server.cmd --host-once --no-steam --bind=127.0.0.1 --port=25565 --world-id=bind-check
```

```text
./run-server.sh --bind-check --no-steam --bind=127.0.0.1 --port=25565 --world-id=bind-check
```

An explicit address must not widen to `0.0.0.0` or `::` after failure. Report any widened bind as a blocker.

## Supported options

```text
--help | -h
--host | --serve
--host-once | --bind-check
--world-name=NAME
--world-id=ID
--seed=N
--difficulty=TEXT
--max-players=1..64
--port=25500..25599
--bind=127.0.0.1 | 0.0.0.0 | :: | another exact local address
--setup=encoded-world-setup
--no-steam
```

When no port is supplied, the server selects the first available port in the governed game range.

## Network exposure

Use `127.0.0.1` for same-machine evaluation. Use a LAN address only for an approved multi-machine test with an explicit firewall plan.

`0.0.0.0` and `::` listen broadly. They must not be used casually for private alpha testing. The current host does not provide production matchmaking, public account authentication, server browsing, abuse moderation, or an internet deployment service.

## Authentication and session continuity

Before `RELAY_ONLY` access, every client must complete:

1. Protocol and access-class acknowledgement.
2. Profile identity submission.
3. Base manifest delivery.
4. Acquisition confirmation.
5. Restart completion acknowledgement.
6. Server-issued integrity challenge and valid response.
7. Stable session attachment or token-gated resume.

The host rejects pre-authentication payloads, invalid challenges, invalid resume tokens, simultaneous duplicate attachment, stale connection generations, replayed relay frames, out-of-order hosted commands, and unsupported world verbs.

## Hosted lobby authority

Authenticated clients may change only:

- Ready or not ready.
- Presence: available, away, or busy.
- Chat state: idle or typing.

The host publishes immutable deterministic connected-only rosters. Offline retained identities remain private. Public rosters are capped at 64 visible players and are delivered as asynchronous `MECH` control frames separate from `SEQ` relay payloads.

Readiness, presence, and typing are living-connection state and reset after disconnect or restart. Stable identity, connection generation, lifetime accepted relay count, and accepted hosted-command accounting survive a clean restart.

## Persistent storage

Remote session ledgers are written under the dedicated server save namespace. They store SHA-256 resume-token hashes, never reusable plaintext tokens.

A corrupted, unsupported-schema, or world-mismatched ledger fails closed and prevents that world host from binding. Do not silently delete a failed ledger during triage. Preserve a copy, record the candidate identity, and follow the approved recovery procedure.

Do not point server storage at the application installation. Do not share the same mutable root with the desktop client.

## Client connection

Approved testers use:

Windows:

```text
Run-Remote-Client.cmd
```

Linux:

```text
./run-remote-client.sh
```

Clients must enter the same host endpoint and server key while using distinct assigned profile identities.

## What success means

A successful limited-alpha host test proves exact binding, authentication, stable identity, token-gated reconnect, session persistence, authoritative lobby state, connected-only roster broadcasts, bounded sequenced relay, and supervised shutdown.

It does not prove remote world gameplay. Do not represent the headless host as a completed multiplayer game server until remote world snapshots and gameplay-command authority are separately implemented and certified.
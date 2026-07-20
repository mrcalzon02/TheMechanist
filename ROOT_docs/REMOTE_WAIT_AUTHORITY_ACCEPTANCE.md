# Remote Wait Authority Acceptance Boundary

This document defines the first network-exposed authoritative gameplay slice for The Mechanist limited alpha.

## Accepted capability

An authenticated independent-host client may submit exactly one world command:

`MECH|WORLD_COMMAND|<command-id>|WAIT`

The host resolves that request as the existing `WaitCommand` through the shared `WorldCommandRequest` contract and the single-writer `AuthoritativeWorldRuntime` lane. A successful commit advances the authenticated player's turn and the global world turn, publishes immutable snapshots, and atomically persists the result under the server's remote-world-authority namespace.

The wait-command sequence is independent from hosted-lobby command IDs and `SEQ` relay IDs. It begins at zero for each authenticated connection generation. Replay, skipped command IDs, stale generations, pre-authentication use, malformed acknowledgements, timeouts, and unsupported verbs fail closed.

## Reconnect and restart behavior

A valid resume credential restores the same server-owned player identity. The connection generation advances, the wait-command sequence restarts at zero, and the persisted player/world turn totals continue.

A clean server-process restart restores the wait ledger before the host accepts clients. Corrupt, world-mismatched, or unsupported ledger data prevents the authority from mounting. Successful reconnect after restart continues the persisted turn totals.

## Player-facing surface

The remote lobby exposes a dedicated **Wait / Advance Turn** button and displays the last accepted player and world turn counters. The control is disabled before authentication. A failed client session is torn down once and requires explicit reconnect.

## Capabilities not granted

This slice does not grant:

- Movement or character position authority.
- Remote map delivery or map navigation.
- Interaction, combat, inventory, equipment, economy, or teleport authority.
- Arbitrary or generic world-command submission.
- Full remote world simulation or a certified multiplayer gameplay session.

The transport ACCESS label remains `RELAY_ONLY` because ordinary `SEQ` payloads are still relay data. WAIT is a separate authenticated `MECH` control capability and must not be represented as full gameplay authority.

## Acceptance evidence required before distribution

A candidate may claim this slice only when Windows and Linux packaged tests prove:

- Pre-authentication WAIT denial.
- Exact command ordering and replay rejection.
- First-connect, living-host resume, and clean host-restart continuity.
- Atomic turn persistence outside the installation.
- Correct client acknowledgement parsing and fail-closed mismatch handling.
- Player-facing lobby availability.
- Movement, map, generic commands, full-world authority, and complete gameplay certification remain false.

This engineering acceptance boundary does not replace asset-clearance, legal, code-signing, or security review.

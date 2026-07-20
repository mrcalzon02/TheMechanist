package mechanist;

/** Fast deterministic checks for process-local remote session ownership and reconnect rules. */
final class RemoteSessionLedgerAuthoritySmoke {
    public static void main(String[] args) {
        RemoteSessionLedgerAuthority ledger =
                new RemoteSessionLedgerAuthority("remote-ledger-smoke-world");

        RemoteSessionLedgerAuthority.Attachment first =
                ledger.attach("profile.alpha.0001", "", "connection-one");
        require(!first.resumed(), "new remote session must not report resumed state");
        require(first.connectionGeneration() == 1L,
                "new remote session must begin at generation one");
        require(first.playerId().matches("remote-[a-f0-9]{20}"),
                "server-owned player id has the wrong format");
        require(first.resumeToken().matches("[a-f0-9]{64}"),
                "server-owned resume token has the wrong format");

        expectFailure(
                () -> ledger.attach(
                        "profile.alpha.0001",
                        first.resumeToken(),
                        "split-brain-connection"),
                "already connected");
        expectFailure(
                () -> ledger.attach(
                        "profile.alpha.0001",
                        "0".repeat(64),
                        "invalid-token-connection"),
                "resume token");

        RemoteSessionLedgerAuthority.SessionSnapshot accepted =
                ledger.noteRelayFrameAccepted(first, 0L);
        require(accepted.connected(), "accepted relay frame lost connected state");
        require(accepted.acceptedRelayFrames() == 1L
                        && accepted.lastConnectionSequence() == 0L,
                "accepted relay frame was not recorded in the immutable snapshot");

        ledger.disconnect(first, "smoke disconnect");
        RemoteSessionLedgerAuthority.SessionSnapshot offline =
                ledger.snapshotForProfile("profile.alpha.0001");
        require(offline != null && !offline.connected(),
                "disconnect did not retain an offline resumable session");

        RemoteSessionLedgerAuthority.Attachment resumed =
                ledger.attach(
                        "profile.alpha.0001",
                        first.resumeToken(),
                        "connection-two");
        require(resumed.resumed(), "valid token did not resume the existing session");
        require(resumed.playerId().equals(first.playerId()),
                "resume changed the stable server-owned player id");
        require(resumed.connectionGeneration() == 2L,
                "resume did not advance the connection generation");
        require(resumed.snapshotVersion() > first.snapshotVersion(),
                "resume did not advance the immutable snapshot version");

        ledger.disconnect(first, "stale connection close");
        require(ledger.snapshot(resumed).connected(),
                "stale attachment disconnected the active resumed session");
        expectFailure(
                () -> ledger.noteRelayFrameAccepted(first, 1L),
                "not the active connection");

        RemoteSessionLedgerAuthority.SessionSnapshot resumedFrame =
                ledger.noteRelayFrameAccepted(resumed, 0L);
        require(resumedFrame.acceptedRelayFrames() == 2L,
                "lifetime relay accounting did not survive reconnect");
        require(resumedFrame.lastConnectionSequence() == 0L,
                "resumed connection did not restart its per-connection sequence");

        RemoteSessionLedgerAuthority.Attachment secondProfile =
                ledger.attach("profile.beta.0002", "", "connection-beta");
        require(!secondProfile.playerId().equals(first.playerId()),
                "distinct profiles received the same server-owned player id");
        require(ledger.totalSessionCount() == 2
                        && ledger.activeSessionCount() == 2,
                "ledger counts do not match active remote sessions");

        RemoteSessionLedgerAuthority restarted =
                new RemoteSessionLedgerAuthority("remote-ledger-smoke-world");
        expectFailure(
                () -> restarted.attach(
                        "profile.alpha.0001",
                        first.resumeToken(),
                        "post-restart-old-token"),
                "does not match any");
        RemoteSessionLedgerAuthority.Attachment freshAfterRestart =
                restarted.attach(
                        "profile.alpha.0001",
                        "",
                        "post-restart-fresh-session");
        require(freshAfterRestart.playerId().equals(first.playerId()),
                "deterministic profile player identity changed across host restart");
        require(freshAfterRestart.connectionGeneration() == 1L
                        && !freshAfterRestart.resumed(),
                "process restart incorrectly retained reconnect state");

        System.out.println("RemoteSessionLedgerAuthoritySmoke PASS "
                + ledger.auditSummary());
    }

    private static void expectFailure(Runnable action, String expectedText) {
        try {
            action.run();
            throw new AssertionError("expected failure containing: " + expectedText);
        } catch (RuntimeException expected) {
            String message = expected.getMessage() == null ? "" : expected.getMessage();
            require(message.toLowerCase().contains(expectedText.toLowerCase()),
                    "unexpected failure: " + message);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private RemoteSessionLedgerAuthoritySmoke() { }
}

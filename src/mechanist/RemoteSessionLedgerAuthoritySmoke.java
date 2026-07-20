package mechanist;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.stream.Stream;

/** Fast deterministic checks for remote session, hosted-command, reconnect, and persistence rules. */
final class RemoteSessionLedgerAuthoritySmoke {
    public static void main(String[] args) throws Exception {
        verifyProcessLocalRules();
        verifyAtomicPersistenceRules();
        System.out.println("RemoteSessionLedgerAuthoritySmoke PASS"
                + " processLocalRules=true"
                + " hostedSessionCommands=true"
                + " immutableHostedRoster=true"
                + " unsupportedWorldCommandsRejected=true"
                + " atomicPersistence=true"
                + " tokenStorage=sha256-only"
                + " corruptLedgerRejected=true"
                + " worldAuthority=false");
    }

    private static void verifyProcessLocalRules() throws Exception {
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

        RemoteSessionLedgerAuthority.HostedSessionCommandResult ready =
                ledger.applyHostedSessionCommand(first, 0L, "READY", "true");
        require(ready.playerSnapshot().ready(),
                "READY command did not update the player snapshot");
        RemoteSessionLedgerAuthority.HostedSessionCommandResult presence =
                ledger.applyHostedSessionCommand(first, 1L, "PRESENCE", "away");
        require("away".equals(presence.playerSnapshot().presence()),
                "PRESENCE command did not update the player snapshot");
        RemoteSessionLedgerAuthority.HostedSessionCommandResult chat =
                ledger.applyHostedSessionCommand(first, 2L, "CHAT_STATE", "typing");
        require("typing".equals(chat.playerSnapshot().chatState()),
                "CHAT_STATE command did not update the player snapshot");
        require(chat.playerSnapshot().acceptedHostedCommands() == 3L,
                "hosted command accounting is incorrect");
        require(chat.hostedSnapshot().roster().size() == 1
                        && !chat.hostedSnapshot().worldAuthority(),
                "hosted roster did not preserve the closed world-authority boundary");

        expectFailure(
                () -> ledger.applyHostedSessionCommand(
                        first, 4L, "READY", "false"),
                "sequence mismatch");
        expectFailure(
                () -> ledger.applyHostedSessionCommand(
                        first, 3L, "MOVE", "north"),
                "world authority is closed");
        RemoteSessionLedgerAuthority.HostedSessionCommandResult unready =
                ledger.applyHostedSessionCommand(first, 3L, "READY", "false");
        require(!unready.playerSnapshot().ready()
                        && unready.playerSnapshot().lastConnectionCommandId() == 3L,
                "failed hosted command incorrectly advanced or blocked the sequence");

        RemoteSessionLedgerAuthority.SessionSnapshot accepted =
                ledger.noteRelayFrameAccepted(first, 0L);
        require(accepted.connected(), "accepted relay frame lost connected state");
        require(accepted.acceptedRelayFrames() == 1L
                        && accepted.lastConnectionSequence() == 0L,
                "accepted relay frame was not recorded in the immutable snapshot");

        RemoteSessionLedgerAuthority.Attachment secondProfile =
                ledger.attach("profile.beta.0002", "", "connection-beta");
        require(!secondProfile.playerId().equals(first.playerId()),
                "distinct profiles received the same server-owned player id");
        RemoteSessionLedgerAuthority.HostedSessionSnapshot roster =
                ledger.hostedSessionSnapshot();
        require(roster.totalSessions() == 2
                        && roster.activeSessions() == 2
                        && roster.roster().size() == 2,
                "hosted roster counts do not match active remote sessions");
        require(roster.roster().get(0).playerId().compareTo(
                        roster.roster().get(1).playerId()) < 0,
                "hosted roster is not deterministically ordered");

        ledger.disconnect(first, "smoke disconnect");
        RemoteSessionLedgerAuthority.SessionSnapshot offline =
                ledger.snapshotForProfile("profile.alpha.0001");
        require(offline != null && !offline.connected(),
                "disconnect did not retain an offline resumable session");
        require(!offline.ready()
                        && "offline".equals(offline.presence())
                        && "idle".equals(offline.chatState()),
                "disconnect retained stale hosted-lobby liveness state");

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

        ledger.disconnect(first, "stale connection close");
        require(ledger.snapshot(resumed).connected(),
                "stale attachment disconnected the active resumed session");
        expectFailure(
                () -> ledger.noteRelayFrameAccepted(first, 1L),
                "not the active connection");
        expectFailure(
                () -> ledger.applyHostedSessionCommand(
                        first, 4L, "READY", "true"),
                "not the active connection");

        RemoteSessionLedgerAuthority.HostedSessionCommandResult resumedCommand =
                ledger.applyHostedSessionCommand(resumed, 0L, "PRESENCE", "busy");
        require(resumedCommand.playerSnapshot().acceptedHostedCommands() == 5L,
                "lifetime hosted command accounting did not survive reconnect");
        require(resumedCommand.playerSnapshot().lastConnectionCommandId() == 0L,
                "resumed connection did not restart its command sequence");
        RemoteSessionLedgerAuthority.SessionSnapshot resumedFrame =
                ledger.noteRelayFrameAccepted(resumed, 0L);
        require(resumedFrame.acceptedRelayFrames() == 2L,
                "lifetime relay accounting did not survive reconnect");

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
                "nonpersistent ledger incorrectly retained reconnect state");
        ledger.close();
        restarted.close();
    }

    private static void verifyAtomicPersistenceRules() throws Exception {
        Path root = Files.createTempDirectory("mechanist-remote-ledger-smoke ");
        Path ledgerFile = root.resolve("world alpha.sessions.properties");
        Path corruptFile = root.resolve("corrupt.sessions.properties");
        try {
            String profile = "profile.persisted.0003";
            String token;
            String playerId;
            long versionBeforeRestart;
            try (RemoteSessionLedgerAuthority persistent =
                         new RemoteSessionLedgerAuthority(
                                 "persistent-smoke-world", ledgerFile)) {
                RemoteSessionLedgerAuthority.Attachment first =
                        persistent.attach(profile, "", "persistent-connection-one");
                token = first.resumeToken();
                playerId = first.playerId();
                persistent.applyHostedSessionCommand(
                        first, 0L, "READY", "true");
                persistent.applyHostedSessionCommand(
                        first, 1L, "PRESENCE", "away");
                persistent.applyHostedSessionCommand(
                        first, 2L, "CHAT_STATE", "typing");
                persistent.noteRelayFrameAccepted(first, 0L);
                persistent.disconnect(first, "clean persistence smoke disconnect");
                RemoteSessionLedgerAuthority.SessionSnapshot offline =
                        persistent.snapshotForProfile(profile);
                require(offline != null && !offline.connected(),
                        "persistent ledger did not retain the offline session");
                require(offline.acceptedRelayFrames() == 1L
                                && offline.acceptedHostedCommands() == 3L,
                        "persistent ledger lost accounting before restart");
                versionBeforeRestart = offline.version();
            }

            require(Files.isRegularFile(ledgerFile),
                    "atomic persistence did not create the ledger file");
            String disk = Files.readString(ledgerFile, StandardCharsets.ISO_8859_1);
            require(disk.contains("schema=2"),
                    "persistent ledger did not write schema two");
            require(!disk.contains(token),
                    "persistent ledger wrote a reusable plaintext resume token");
            require(disk.contains(sha256Hex(token)),
                    "persistent ledger omitted the resume-token hash");
            require(disk.contains("acceptedHostedCommands=3"),
                    "persistent ledger omitted hosted command accounting");
            require(!hasTemporarySibling(ledgerFile),
                    "atomic persistence left a temporary sibling behind");

            long versionAfterResume;
            try (RemoteSessionLedgerAuthority restored =
                         new RemoteSessionLedgerAuthority(
                                 "persistent-smoke-world", ledgerFile)) {
                RemoteSessionLedgerAuthority.SessionSnapshot restoredOffline =
                        restored.snapshotForProfile(profile);
                require(restoredOffline != null && !restoredOffline.connected(),
                        "restored session was not forced offline");
                require(restoredOffline.playerId().equals(playerId),
                        "restored session changed the stable player id");
                require(restoredOffline.acceptedRelayFrames() == 1L
                                && restoredOffline.acceptedHostedCommands() == 3L,
                        "restored session lost lifetime accounting");
                require(!restoredOffline.ready()
                                && "offline".equals(restoredOffline.presence())
                                && "idle".equals(restoredOffline.chatState()),
                        "restored session retained stale liveness state");
                require(restoredOffline.version() > versionBeforeRestart,
                        "ledger restoration did not advance the snapshot version");
                expectFailure(
                        () -> restored.attach(
                                profile,
                                "f".repeat(64),
                                "persistent-invalid-token"),
                        "resume token");
                RemoteSessionLedgerAuthority.Attachment resumed =
                        restored.attach(
                                profile,
                                token,
                                "persistent-connection-two");
                require(resumed.resumed(),
                        "persistent ledger did not accept its valid resume token");
                require(resumed.connectionGeneration() == 2L,
                        "persistent resume did not advance the connection generation");
                RemoteSessionLedgerAuthority.HostedSessionCommandResult command =
                        restored.applyHostedSessionCommand(
                                resumed, 0L, "READY", "true");
                require(command.playerSnapshot().acceptedHostedCommands() == 4L,
                        "persistent resume lost hosted command accounting");
                RemoteSessionLedgerAuthority.SessionSnapshot accepted =
                        restored.noteRelayFrameAccepted(resumed, 0L);
                require(accepted.acceptedRelayFrames() == 2L,
                        "persistent resume lost lifetime relay accounting");
                versionAfterResume = accepted.version();
                restored.disconnect(resumed, "second clean disconnect");
            }

            try (RemoteSessionLedgerAuthority restoredAgain =
                         new RemoteSessionLedgerAuthority(
                                 "persistent-smoke-world", ledgerFile)) {
                RemoteSessionLedgerAuthority.SessionSnapshot snapshot =
                        restoredAgain.snapshotForProfile(profile);
                require(snapshot != null && !snapshot.connected(),
                        "second restoration did not keep the session offline");
                require(snapshot.connectionGeneration() == 2L,
                        "second restoration lost connection-generation continuity");
                require(snapshot.acceptedRelayFrames() == 2L
                                && snapshot.acceptedHostedCommands() == 4L,
                        "second restoration lost lifetime accounting");
                require(snapshot.version() > versionAfterResume,
                        "second restoration did not preserve monotonic versions");
                RemoteSessionLedgerAuthority.Attachment resumedAgain =
                        restoredAgain.attach(
                                profile,
                                token,
                                "persistent-connection-three");
                require(resumedAgain.connectionGeneration() == 3L,
                        "second persistent resume did not advance generation");
            }

            String corrupt = Files.readString(
                    ledgerFile, StandardCharsets.ISO_8859_1)
                    .replaceFirst("schema=2", "schema=999");
            Files.writeString(corruptFile, corrupt, StandardCharsets.ISO_8859_1);
            expectCheckedFailure(
                    () -> new RemoteSessionLedgerAuthority(
                            "persistent-smoke-world", corruptFile),
                    "unsupported");
            expectCheckedFailure(
                    () -> new RemoteSessionLedgerAuthority(
                            "different-world", ledgerFile),
                    "world mismatch");
        } finally {
            deleteRecursively(root);
        }
    }

    private static boolean hasTemporarySibling(Path ledgerFile) throws Exception {
        Path parent = ledgerFile.getParent();
        if (parent == null || !Files.isDirectory(parent)) return false;
        String prefix = ledgerFile.getFileName() + ".tmp-";
        try (Stream<Path> paths = Files.list(parent)) {
            return paths.anyMatch(path -> path.getFileName().toString().startsWith(prefix));
        }
    }

    private static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(
                digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (root == null || !Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception failure) {
                            throw new RuntimeException(failure);
                        }
                    });
        }
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

    private static void expectCheckedFailure(
            ThrowingRunnable action,
            String expectedText
    ) {
        try {
            action.run();
            throw new AssertionError("expected checked failure containing: " + expectedText);
        } catch (Exception expected) {
            String message = expected.getMessage() == null ? "" : expected.getMessage();
            require(message.toLowerCase().contains(expectedText.toLowerCase()),
                    "unexpected checked failure: " + message);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private RemoteSessionLedgerAuthoritySmoke() { }
}

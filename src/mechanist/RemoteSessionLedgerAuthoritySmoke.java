package mechanist;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.stream.Stream;

/** Fast deterministic checks for remote session ownership, reconnect, and persistence rules. */
final class RemoteSessionLedgerAuthoritySmoke {
    public static void main(String[] args) throws Exception {
        verifyProcessLocalRules();
        verifyAtomicPersistenceRules();
        System.out.println("RemoteSessionLedgerAuthoritySmoke PASS"
                + " processLocalRules=true"
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
                require(persistent.persistenceEnabled(),
                        "persistent ledger did not report persistence enabled");
                require(ledgerFile.equals(persistent.persistenceFile()),
                        "persistent ledger changed its configured file");
                RemoteSessionLedgerAuthority.Attachment first =
                        persistent.attach(profile, "", "persistent-connection-one");
                token = first.resumeToken();
                playerId = first.playerId();
                persistent.noteRelayFrameAccepted(first, 0L);
                persistent.disconnect(first, "clean persistence smoke disconnect");
                RemoteSessionLedgerAuthority.SessionSnapshot offline =
                        persistent.snapshotForProfile(profile);
                require(offline != null && !offline.connected(),
                        "persistent ledger did not retain the offline session");
                require(offline.acceptedRelayFrames() == 1L,
                        "persistent ledger lost relay accounting before restart");
                versionBeforeRestart = offline.version();
            }

            require(Files.isRegularFile(ledgerFile),
                    "atomic persistence did not create the ledger file");
            String disk = Files.readString(ledgerFile, StandardCharsets.ISO_8859_1);
            require(!disk.contains(token),
                    "persistent ledger wrote a reusable plaintext resume token");
            require(disk.contains(sha256Hex(token)),
                    "persistent ledger omitted the resume-token hash");
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
                require(restoredOffline.acceptedRelayFrames() == 1L,
                        "restored session lost lifetime relay accounting");
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
                require(resumed.playerId().equals(playerId),
                        "persistent resume changed the stable player id");
                require(resumed.connectionGeneration() == 2L,
                        "persistent resume did not advance the connection generation");
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
                require(snapshot.acceptedRelayFrames() == 2L,
                        "second restoration lost lifetime relay accounting");
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
                    .replaceFirst("schema=1", "schema=999");
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

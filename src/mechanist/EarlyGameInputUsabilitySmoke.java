package mechanist;

import java.awt.event.KeyEvent;

/** Regression for early-stage input routing and full-range signed world seeds. */
final class EarlyGameInputUsabilitySmoke {
    private EarlyGameInputUsabilitySmoke() { }

    public static void main(String[] args) throws Exception {
        check(WorldStartFlowAuthority.WorldStartFlowOverlay.portraitKeyDirection(
                false, KeyEvent.VK_OPEN_BRACKET) == -1,
                "portrait previous key was swallowed by the primary start-flow dispatcher");
        check(WorldStartFlowAuthority.WorldStartFlowOverlay.portraitKeyDirection(
                false, KeyEvent.VK_CLOSE_BRACKET) == 1,
                "portrait next key was swallowed by the primary start-flow dispatcher");
        check(WorldStartFlowAuthority.WorldStartFlowOverlay.portraitKeyDirection(
                true, KeyEvent.VK_OPEN_BRACKET) == 0
                && WorldStartFlowAuthority.WorldStartFlowOverlay.portraitKeyDirection(
                true, KeyEvent.VK_CLOSE_BRACKET) == 0,
                "editing a character name must not also change the selected portrait");
        check(WorldStartFlowAuthority.WorldStartFlowOverlay.portraitKeyDirection(
                false, KeyEvent.VK_P) == 0,
                "unrelated keyboard input should not browse portraits");

        String maximum = digits(Long.toString(Long.MAX_VALUE));
        String minimum = "-" + digits(Long.toString(Long.MIN_VALUE).substring(1));
        check(maximum.equals(Long.toString(Long.MAX_VALUE)),
                "positive signed 64-bit seed was truncated");
        check(minimum.equals(Long.toString(Long.MIN_VALUE)),
                "negative signed 64-bit seed was truncated");
        check(Long.parseLong(maximum) == Long.MAX_VALUE
                && Long.parseLong(minimum) == Long.MIN_VALUE,
                "valid extreme world seeds do not parse");
        check(maximum.equals(WorldStartFlowAuthority.WorldStartFlowOverlay
                .appendSeedCharacter(maximum, '0')),
                "positive world seed editor accepted an extra digit");
        check(minimum.equals(WorldStartFlowAuthority.WorldStartFlowOverlay
                .appendSeedCharacter(minimum, '0')),
                "negative world seed editor accepted an extra digit");
        check("-".equals(WorldStartFlowAuthority.WorldStartFlowOverlay
                .appendSeedCharacter("", '-')),
                "negative seed sign cannot be entered at the start");
        check("".equals(WorldStartFlowAuthority.WorldStartFlowOverlay
                .appendSeedCharacter("", 'x')),
                "world seed editor accepted an unrelated typed character");
        check("1".equals(WorldStartFlowAuthority.WorldStartFlowOverlay
                .appendSeedCharacter("1", '-')),
                "world seed editor accepted an embedded minus sign");
        verifyWorldDeletionSafety();
        System.out.println("EarlyGameInputUsabilitySmoke PASS portraitKeys=true"
                + " nameEditingIsolation=true fullSigned64BitSeeds=true worldDeletionExactIdentity=true");
    }

    /** Exercise the actual deletion gate with isolated temporary world-definition files. */
    private static void verifyWorldDeletionSafety() throws Exception {
        java.nio.file.Path sandbox = java.nio.file.Files.createTempDirectory("mechanist-world-delete-");
        try {
            java.nio.file.Path directory = java.nio.file.Files.createDirectory(sandbox.resolve("worlds"));
            java.nio.file.Path selected = directory.resolve("alpha.mechworld");
            java.nio.file.Path other = directory.resolve("beta.mechworld");
            long seed = 12345L;
            String setup = WorldSetupSettings.standard().encode();
            writeWorldDefinition(selected, seed, "alpha", "Arcology Alpha", setup);
            writeWorldDefinition(other, 123456L, "beta", "Arcology Alpha", setup);
            check(selected.toAbsolutePath().normalize().equals(
                    WorldStartFlowAuthority.WorldStartFlowOverlay.verifiedWorldDefinitionPath(
                            directory, selected, seed, "alpha", "Arcology Alpha", setup)),
                    "selected exact world definition rejected");
            check(!tryDelete(directory, selected, 1234L, "alpha", "Arcology Alpha", setup),
                    "partial seed matched a different world");
            check(!tryDelete(directory, selected, seed, "beta", "Arcology Alpha", setup),
                    "different world identity was accepted");
            check(!tryDelete(directory, selected, seed, "alpha", "Wrong Arcology", setup),
                    "changed world name was accepted");
            check(!tryDelete(directory, selected, seed, "alpha", "Arcology Alpha", setup + "-changed"),
                    "changed world settings were accepted");
            check(!tryDelete(directory, directory.resolve("missing.mechworld"),
                    seed, "missing", "Arcology Alpha", setup),
                    "missing world definition was accepted");
            java.nio.file.Path outside = sandbox.resolve("alpha.mechworld");
            writeWorldDefinition(outside, seed, "alpha", "Arcology Alpha", setup);
            check(!tryDelete(directory, outside, seed, "alpha", "Arcology Alpha", setup),
                    "path outside world directory was accepted");
            java.nio.file.Path wrongExtension = directory.resolve("alpha.txt");
            writeWorldDefinition(wrongExtension, seed, "alpha", "Arcology Alpha", setup);
            check(!tryDelete(directory, wrongExtension, seed, "alpha", "Arcology Alpha", setup),
                    "non-world-definition file was accepted");
            java.nio.file.Path linked = directory.resolve("linked.mechworld");
            java.nio.file.Path target = sandbox.resolve("linked-target.properties");
            writeWorldDefinition(target, seed, "linked", "Arcology Alpha", setup);
            try {
                java.nio.file.Files.createSymbolicLink(linked, target);
                check(!tryDelete(directory, linked, seed, "linked", "Arcology Alpha", setup),
                        "symbolic-link world definition was accepted");
            } catch (UnsupportedOperationException | SecurityException | java.io.IOException noSymlinks) {
                // Filesystems without symlink permission still run the other refusal cases.
            }
            check(java.nio.file.Files.isRegularFile(selected)
                    && java.nio.file.Files.isRegularFile(other)
                    && java.nio.file.Files.isRegularFile(outside)
                    && java.nio.file.Files.isRegularFile(target),
                    "unsafe deletion changed an unrelated or valid world");
            check(tryDelete(directory, selected, seed, "alpha", "Arcology Alpha", setup),
                    "exact selected world deletion failed");
            check(!java.nio.file.Files.exists(selected) && java.nio.file.Files.exists(other),
                    "selected-world deletion affected another world");
            check(!tryDelete(directory, selected, seed, "alpha", "Arcology Alpha", setup),
                    "stale world list guessed a replacement after the selected file disappeared");
            check(java.nio.file.Files.exists(other), "stale deletion damaged another world");
        } finally {
            try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(sandbox)) {
                for (java.nio.file.Path path : walk.sorted(java.util.Comparator.reverseOrder()).toList())
                    java.nio.file.Files.deleteIfExists(path);
            }
        }
    }

    private static void writeWorldDefinition(java.nio.file.Path path,
            long seed, String id, String name, String setup) throws java.io.IOException {
        java.util.Properties data = new java.util.Properties();
        data.setProperty("worlddef.seed", Long.toString(seed));
        data.setProperty("worlddef.worldId", id);
        data.setProperty("worlddef.hiveName", name);
        data.setProperty("worlddef.setup", setup);
        try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(path)) {
            data.store(out, "isolated world deletion smoke");
        }
    }

    private static boolean tryDelete(java.nio.file.Path directory, java.nio.file.Path selected,
            long seed, String id, String name, String setup) throws java.io.IOException {
        return WorldStartFlowAuthority.WorldStartFlowOverlay.deleteVerifiedWorldDefinition(
                directory, selected, seed, id, name, setup);
    }

    private static String digits(String text) {
        String result = "";
        for (char digit : text.toCharArray()) {
            result = WorldStartFlowAuthority.WorldStartFlowOverlay.appendSeedCharacter(result, digit);
        }
        return result;
    }

    private static void check(boolean ok, String detail) {
        if (!ok) throw new AssertionError(detail);
    }
}

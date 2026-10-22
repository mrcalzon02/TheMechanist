package mechanist;

import java.awt.event.KeyEvent;

/** Regression for early-stage input routing and full-range signed world seeds. */
final class EarlyGameInputUsabilitySmoke {
    private EarlyGameInputUsabilitySmoke() { }

    public static void main(String[] args) {
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
        System.out.println("EarlyGameInputUsabilitySmoke PASS portraitKeys=true"
                + " nameEditingIsolation=true fullSigned64BitSeeds=true");
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

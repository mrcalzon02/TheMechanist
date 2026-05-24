package mechanist;

/**
 * Developer entrypoint kept for compatibility. It now launches the real game
 * shell rather than a detached knowledge-tree test window, because progression
 * menus must live inside the owned game UI.
 */
public final class GameTest {
    private GameTest() {}
    public static void main(String[] args) { TheMechanist.main(args); }
}

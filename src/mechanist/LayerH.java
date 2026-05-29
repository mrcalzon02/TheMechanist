package mechanist;

public class LayerH {
    public LayerH() {}

    static String controlProfileTitle(GamePanel panel) {
        switch (panel.controlsTab) {
            case 1: return "XBOX CONTROL REFERENCE";
            case 2: return "PLAYSTATION CONTROL REFERENCE";
            case 3: return "STEAM DECK CONTROL REFERENCE";
            case 4: return "GENERIC CONTROLLER REFERENCE";
            default: return "KEYBOARD AND MOUSE CONTROL REFERENCE";
        }
    }
}

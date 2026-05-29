package mechanist;

public class LayerI {
    public LayerI() {}

    static String stateText(boolean value) {
        if (value) return "ON";
        return "OFF";
    }
}

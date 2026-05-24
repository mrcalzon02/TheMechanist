package mechanist.modapi;

public record RoomDimensions(int width, int height) {
    public RoomDimensions {
        width = Math.max(1, Math.min(512, width));
        height = Math.max(1, Math.min(512, height));
    }
}

package mechanist;

/**
 * Pure, hardware-independent positional stereo for local sound cues.
 * World tile coordinates are interpreted relative to the player's facing;
 * audio is not tied to the graphics camera or to the view cursor.
 */
final class SpatialAudioAuthority {
    record Mix(double left, double right, double distance, double pan,
               boolean audible) { }

    private SpatialAudioAuthority() { }

    static Mix mix(int listenerX, int listenerY, int facingX, int facingY,
                   int sourceX, int sourceY, int volumePercent,
                   int rangeTiles, boolean occluded) {
        int dx = sourceX - listenerX;
        int dy = sourceY - listenerY;
        double distance = Math.hypot(dx, dy);
        int range = Math.max(1, rangeTiles);
        if (volumePercent <= 0 || distance >= range) {
            return new Mix(0, 0, distance, 0, false);
        }
        double facingLength = Math.hypot(facingX, facingY);
        double fx = facingLength == 0 ? 1 : facingX / facingLength;
        double fy = facingLength == 0 ? 0 : facingY / facingLength;
        // Screen/world Y grows downward: positive cross means source is right.
        double pan = distance == 0 ? 0
                : clamp((fx * dy - fy * dx) / distance * 0.90, -0.90, 0.90);
        double taper = clamp((range - distance) / (range * 0.35), 0, 1);
        double attenuation = taper / (1.0 + Math.pow(distance / 5.0, 2.0));
        if (occluded) attenuation *= 0.38;
        double level = clamp(volumePercent / 100.0, 0, 1) * attenuation;
        double angle = (pan + 1.0) * Math.PI / 4.0;
        return new Mix(level * Math.cos(angle), level * Math.sin(angle),
                distance, pan, level > 0.00001);
    }

    /**
     * Expand 16-bit signed little-endian mono/stereo PCM to stereo with
     * independent channel gains. This deliberately does not require an
     * optional mixer PAN or BALANCE hardware control.
     */
    static byte[] stereoPcm16(byte[] source, int channels, Mix mix) {
        if (source == null || (channels != 1 && channels != 2))
            throw new IllegalArgumentException("16-bit mono/stereo PCM required");
        int frames = source.length / (channels * 2);
        byte[] output = new byte[frames * 4];
        for (int i = 0; i < frames; i++) {
            int sourceIndex = i * channels * 2;
            int mono = sample(source, sourceIndex);
            if (channels == 2)
                mono = (mono + sample(source, sourceIndex + 2)) / 2;
            write(output, i * 4, scaled((short) mono, mix.left()));
            write(output, i * 4 + 2, scaled((short) mono, mix.right()));
        }
        return output;
    }

    private static short sample(byte[] data, int index) {
        return (short) ((data[index] & 0xff)
                | ((data[index + 1] & 0xff) << 8));
    }

    private static short scaled(short sample, double gain) {
        long value = Math.round(sample * gain);
        return (short) Math.max(Short.MIN_VALUE,
                Math.min(Short.MAX_VALUE, value));
    }

    private static void write(byte[] data, int offset, short sample) {
        data[offset] = (byte) (sample & 0xff);
        data[offset + 1] = (byte) ((sample >>> 8) & 0xff);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

package wily.legacy.Skins.skin;

final class SkinChunkAccumulator {
    private final byte[][] parts;
    private int received;
    SkinChunkAccumulator(int total) { parts = new byte[total][]; }
    void put(int index, byte[] data) {
        if (index < 0 || index >= parts.length || parts[index] != null) return;
        parts[index] = data == null ? new byte[0] : data;
        received++;
    }
    boolean isComplete() { return received >= parts.length; }
    byte[] assemble() {
        int len = 0;
        for (byte[] part : parts) { if (part != null) len += part.length; }
        byte[] out = new byte[len];
        int offset = 0;
        for (byte[] part : parts) {
            if (part == null) continue;
            System.arraycopy(part, 0, out, offset, part.length);
            offset += part.length;
        }
        return out;
    }
}

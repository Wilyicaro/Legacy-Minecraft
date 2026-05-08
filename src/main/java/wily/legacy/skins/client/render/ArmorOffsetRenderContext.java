package wily.legacy.skins.client.render;

public final class ArmorOffsetRenderContext {
    private static final ThreadLocal<Offsets> SUBMIT = new ThreadLocal<>();
    private static final ThreadLocal<Offsets> RENDER = new ThreadLocal<>();

    private ArmorOffsetRenderContext() {
    }

    public static void setSubmitOffsets(Offsets offsets) {
        if (offsets == null) SUBMIT.remove();
        else SUBMIT.set(offsets);
    }

    public static Offsets submitOffsets() {
        return SUBMIT.get();
    }

    public static void clearSubmitOffsets() {
        SUBMIT.remove();
    }

    public static void setRenderOffsets(Offsets offsets) {
        if (offsets == null) RENDER.remove();
        else RENDER.set(offsets);
    }

    public static Offsets renderOffsets() {
        return RENDER.get();
    }

    public static void clearRenderOffsets() {
        RENDER.remove();
    }

    public record Offsets(float[][] renderOffsets, float[][] scales) {
    }

    public interface PartAccess {
        void consoleskins$setRenderOffset(float[] offset);
    }

    public interface SubmitAccess {
        Offsets consoleskins$getArmorOffsets();
    }
}

package wily.legacy.skins.client.render.boxloader;

public record PlaneDef(
        int[] uv,
        float[] origin,
        float[] size,
        String face,
        boolean mirror,
        Boolean visible,
        int armorMask
) {
}

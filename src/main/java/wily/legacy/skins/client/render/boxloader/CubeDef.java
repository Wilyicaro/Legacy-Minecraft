package wily.legacy.skins.client.render.boxloader;

public record CubeDef(
        int[] uv,
        float[] origin,
        float[] size,
        float inflate,
        boolean mirror,
        Boolean visible
) { }

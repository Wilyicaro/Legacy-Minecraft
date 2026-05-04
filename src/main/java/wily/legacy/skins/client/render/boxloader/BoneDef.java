package wily.legacy.skins.client.render.boxloader;

import java.util.List;

public record BoneDef(
        String name,
        String parent,
        AttachSlot attach,
        float[] pivot,
        float[] rotation,
        List<CubeDef> cubes,
        Boolean visible
) {
}

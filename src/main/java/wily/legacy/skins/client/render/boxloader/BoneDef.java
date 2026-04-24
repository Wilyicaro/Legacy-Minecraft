package wily.legacy.skins.client.render.boxloader;

import java.util.List;

public record BoneDef(
        String name,
        AttachSlot attach,
        List<CubeDef> cubes,
        Boolean visible
) {
}

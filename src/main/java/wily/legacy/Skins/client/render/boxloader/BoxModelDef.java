package wily.legacy.Skins.client.render.boxloader;

import java.util.List;

public record BoxModelDef(
        TextureDef texture,
        List<BoneDef> bones,
        List<AttachSlot> hide
) {
    public record TextureDef(int width, int height) {
    }
}

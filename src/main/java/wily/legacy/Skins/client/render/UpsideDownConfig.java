package wily.legacy.Skins.client.render;

public final class UpsideDownConfig {
    private UpsideDownConfig() {
    }

    public static boolean isUpsideDownSkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;        return SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.UPSIDE_DOWN, skinId);
    }
}

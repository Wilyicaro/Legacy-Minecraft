package wily.legacy.Skins.client.render;

public final class SkiingConfig {
    private SkiingConfig() {
    }

    public static boolean isSkiingSkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;        return SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.SKIING, skinId);
    }
}

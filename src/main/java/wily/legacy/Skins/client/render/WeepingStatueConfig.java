package wily.legacy.Skins.client.render;

public final class WeepingStatueConfig {
    private WeepingStatueConfig() {
    }

    public static boolean isWeepingStatueSkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;
        return SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.WEEPING_STATUE, skinId);
    }
}

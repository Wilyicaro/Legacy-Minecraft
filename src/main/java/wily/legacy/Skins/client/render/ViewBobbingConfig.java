package wily.legacy.Skins.client.render;

public final class ViewBobbingConfig {
    private ViewBobbingConfig() {
    }

    public static boolean isViewBobbingDisabled(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;
        return SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.DISABLE_VIEW_BOBBING, skinId) || StiffLegsConfig.isStiffLegsSkin(skinId);
    }

    public static void reloadNow() {
    }
}

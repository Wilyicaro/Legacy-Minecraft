package wily.legacy.Skins.client.render;

public final class StiffLegsConfig {
    private StiffLegsConfig() {
    }

    public static boolean isStiffLegsSkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;        return SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.STIFF_LEGS, skinId);
    }
}

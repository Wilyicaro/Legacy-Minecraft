package wily.legacy.Skins.client.render;

public final class StiffArmsConfig {
    private StiffArmsConfig() {
    }

    public static boolean isStiffArmsSkin(String id) {
        if (id == null || id.isBlank()) return false;
        return SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.STIFF_ARMS, id);
    }
}

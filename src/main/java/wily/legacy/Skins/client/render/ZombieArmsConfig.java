package wily.legacy.Skins.client.render;

public final class ZombieArmsConfig {
    private ZombieArmsConfig() {
    }

    public static boolean isZombieArmsSkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;        return SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.ZOMBIE_ARMS, skinId);
    }
}

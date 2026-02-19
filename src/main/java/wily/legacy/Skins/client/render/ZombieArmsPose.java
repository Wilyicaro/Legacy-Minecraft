package wily.legacy.Skins.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class ZombieArmsPose {
    private ZombieArmsPose() {
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        String id = a.consoleskins$getSkinId();
        return ZombieArmsConfig.isZombieArmsSkin(id);
    }

    public static void apply(PlayerModel model) {
        model.rightArm.xRot = -1.55F;
        model.leftArm.xRot = -1.55F;
        model.rightArm.yRot = -0.15F;
        model.leftArm.yRot = 0.15F;
        model.rightArm.zRot = 0.0F;
        model.leftArm.zRot = 0.0F;
    }
}

package wily.legacy.Skins.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class WeepingStatuePose {
    private WeepingStatuePose() {
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        String id = a.consoleskins$getSkinId();
        return WeepingStatueConfig.isWeepingStatueSkin(id);
    }

    public static void apply(PlayerModel model) {
        model.rightArm.xRot = -3.1415927F;
        model.rightArm.yRot = 0.0F;
        model.rightArm.zRot = 0.0F;

        model.rightSleeve.xRot = model.rightArm.xRot;
        model.rightSleeve.yRot = model.rightArm.yRot;
        model.rightSleeve.zRot = model.rightArm.zRot;
    }
}

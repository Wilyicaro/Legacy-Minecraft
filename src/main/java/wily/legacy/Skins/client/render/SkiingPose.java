package wily.legacy.Skins.client.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import wily.legacy.Skins.client.gui.DollRenderIds;

public final class SkiingPose {
    private SkiingPose() {
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        String id = a.consoleskins$getSkinId();
        if (!SkiingConfig.isSkiingSkin(id)) return false;
        return state.id == DollRenderIds.MENU_DOLL_ID || a.consoleskins$isMoving();
    }

    public static void apply(PlayerModel model, AvatarRenderState state, float t) {
        if (model == null) return;
        float speed = 18.0F;
        if (state != null && state.id != DollRenderIds.MENU_DOLL_ID && state instanceof RenderStateSkinIdAccess a) {
            float s = a.consoleskins$getMoveSpeedSq();
            if (s > 0.020F) speed = 34.0F;
            else if (s > 0.004F) speed = 26.0F;
        }
        speed *= 0.2F;
        float swing = (float) Math.sin(t * speed) * 0.65F;
        float base = -0.32F;

        model.rightArm.xRot = base + swing;
        model.leftArm.xRot = base + swing;
        model.rightArm.yRot = 0.0F;
        model.leftArm.yRot = 0.0F;
        model.rightArm.zRot = 0.0F;
        model.leftArm.zRot = 0.0F;

        model.rightSleeve.xRot = model.rightArm.xRot;
        model.rightSleeve.yRot = model.rightArm.yRot;
        model.rightSleeve.zRot = model.rightArm.zRot;
        model.leftSleeve.xRot = model.leftArm.xRot;
        model.leftSleeve.yRot = model.leftArm.yRot;
        model.leftSleeve.zRot = model.leftArm.zRot;
    }
}

package wily.legacy.skins.pose;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import wily.legacy.skins.client.gui.GuiDollRender;
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;

public final class SyncLegsPose {
    private SyncLegsPose() { }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess access)) return false;
        String skinId = access.consoleskins$getSkinId();
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.SYNC_LEGS, skinId)) return false;
        if (state.id == GuiDollRender.MENU_DOLL_ID) return true;
        if (state.pose != Pose.STANDING && state.pose != Pose.CROUCHING) return false;
        return access.consoleskins$isMoving() || access.consoleskins$getMoveSpeedSq() > 1.0E-4F;
    }

    public static void apply(PlayerModel model) {
        if (model == null) return;
        model.leftLeg.xRot = model.rightLeg.xRot;
        model.leftLeg.yRot = model.rightLeg.yRot;
        model.leftLeg.zRot = model.rightLeg.zRot;
        model.leftPants.xRot = model.leftLeg.xRot;
        model.leftPants.yRot = model.leftLeg.yRot;
        model.leftPants.zRot = model.leftLeg.zRot;
    }
}

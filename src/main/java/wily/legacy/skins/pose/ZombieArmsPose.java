package wily.legacy.skins.pose;

<<<<<<< Updated upstream
=======
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.effects.SpearAnimations;
>>>>>>> Stashed changes
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
<<<<<<< Updated upstream
=======
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.SwingAnimationType;
>>>>>>> Stashed changes
import wily.legacy.skins.client.render.RenderStateSkinIdAccess;

public final class ZombieArmsPose {
    private ZombieArmsPose() {
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess access)) return false;
        String skinId = access.consoleskins$getSkinId();
        if (!SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.ZOMBIE_ARMS, skinId)) return false;
        Player player = ArmPoseSupport.getPlayer(state);
        return player == null || player.getPose() != Pose.SWIMMING;
    }

    public static void apply(PlayerModel model, AvatarRenderState state) {
        Player player = ArmPoseSupport.getPlayer(state);
        if (player != null && player.getPose() == Pose.SWIMMING) return;

        ArmPoseSupport.ArmState rightState = ArmPoseSupport.ArmState.capture(model.rightArm, model.rightSleeve);
        ArmPoseSupport.ArmState leftState = ArmPoseSupport.ArmState.capture(model.leftArm, model.leftSleeve);
        ArmPoseSupport.ArmFlags blocking = ArmPoseSupport.getShieldBlockingArms(player, false);

        float baseX = state != null && state.isCrouching ? -1.2F : -1.55F;
        if (!blocking.right()) {
            model.rightArm.xRot = baseX;
            model.rightArm.yRot = -0.15F;
            model.rightArm.zRot = 0.0F;
        }
        if (!blocking.left()) {
            model.leftArm.xRot = baseX;
            model.leftArm.yRot = 0.15F;
            model.leftArm.zRot = 0.0F;
        }

        float attackTime = state == null ? 0.0F : state.attackTime;
        ArmPoseSupport.ArmFlags holding = ArmPoseSupport.getHoldingArms(player);
        float holdScale = 1.0F - attackTime;
        if (holdScale > 0.0F && (holding.right() || holding.left())) {
            float targetX = Math.max(baseX, -1.3F);
            float adjust = (targetX - baseX) * holdScale;
            if (holding.right() && !blocking.right()) model.rightArm.xRot += adjust;
            if (holding.left() && !blocking.left()) model.leftArm.xRot += adjust;
        }

        ArmPoseSupport.applyIdleSway(
                model,
                ArmPoseSupport.getAgeInTicks(state),
                attackTime,
                0.95F,
                0.02F,
                0.85F,
                0.015F,
                0.75F,
                0.012F,
                blocking.right(),
                blocking.left()
        );
        if (isSpearJabbing(state, attackTime)) SpearAnimations.thirdPersonAttackHand(model, state);
        else ArmPoseSupport.applyAttackSwing(model, state, attackTime);

        if (blocking.right()) rightState.restore(model.rightArm, model.rightSleeve);
        else rightState.syncSleeve(model.rightArm, model.rightSleeve);

        if (blocking.left()) leftState.restore(model.leftArm, model.leftSleeve);
        else leftState.syncSleeve(model.leftArm, model.leftSleeve);
    }
<<<<<<< Updated upstream
=======

    private static boolean isUsingTrident(Player player) {
        return player != null && player.isUsingItem() && player.getUseItem().getUseAnimation() == ItemUseAnimation.TRIDENT;
    }

    private static boolean isSpearJabbing(AvatarRenderState state, float attackTime) {
        return state != null && attackTime > 0.0F && state.swingAnimationType == SwingAnimationType.STAB;
    }

    private static void applyTrident(PlayerModel model, AvatarRenderState state, Player player) {
        float baseX = state != null && state.isCrouching ? -1.2F : -1.55F;
        model.rightArm.xRot = baseX;
        model.rightArm.yRot = -0.15F;
        model.rightArm.zRot = 0.0F;
        model.leftArm.xRot = baseX;
        model.leftArm.yRot = 0.15F;
        model.leftArm.zRot = 0.0F;

        HumanoidArm arm = ArmPoseSupport.getUsedArm(player);
        ModelPart modelArm = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float crouchOffset = state != null && state.isCrouching ? CROUCH_OFFSET : 0.0F;
        float targetX = Mth.clamp(model.head.xRot - TRIDENT_BACK_X_ROT - crouchOffset, -4.4F, 2.6F);
        modelArm.xRot = Mth.lerp(getTridentRaiseProgress(getTridentUseTicks(state, player)), baseX, targetX);
        modelArm.yRot = model.head.yRot + (arm == HumanoidArm.RIGHT ? -CROUCH_OFFSET : CROUCH_OFFSET);
        modelArm.zRot = 0.0F;

        ArmPoseSupport.ArmState.capture(model.rightArm, model.rightSleeve).syncSleeve(model.rightArm, model.rightSleeve);
        ArmPoseSupport.ArmState.capture(model.leftArm, model.leftSleeve).syncSleeve(model.leftArm, model.leftSleeve);
    }

    private static float getTridentRaiseProgress(int ticksUsingItem) {
        return switch (Mth.clamp(ticksUsingItem, 0, TRIDENT_RAISE_TICKS)) {
            case 0 -> 0.55F;
            case 1 -> 0.72F;
            case 2 -> 0.82F;
            case 3 -> 0.90F;
            case 4 -> 0.96F;
            case 5 -> 0.99F;
            default -> 1.0F;
        };
    }

    private static int getTridentUseTicks(AvatarRenderState state, Player player) {
        int ticks = player.getTicksUsingItem();
        if (state != null && state.isUsingItem) ticks = Math.max(ticks, Math.round(state.ticksUsingItem));
        return ticks;
    }
>>>>>>> Stashed changes
}

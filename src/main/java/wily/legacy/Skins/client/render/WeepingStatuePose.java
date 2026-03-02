package wily.legacy.Skins.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class WeepingStatuePose {
    private WeepingStatuePose() {
    }

    private static Player getPlayer(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return null;
        try {
            UUID uuid = a.consoleskins$getEntityUuid();
            if (uuid == null) return null;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return null;
            return mc.level.getPlayerByUUID(uuid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isArmHolding(Player player, HumanoidArm arm) {
        if (player == null || arm == null) return false;
        try {
            HumanoidArm main = player.getMainArm();
            boolean mainItem = !player.getMainHandItem().isEmpty();
            boolean offItem = !player.getOffhandItem().isEmpty();

            if (player.isUsingItem()) {
                InteractionHand used = player.getUsedItemHand();
                HumanoidArm usedArm = used == InteractionHand.MAIN_HAND ? main : (main == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
                if (usedArm == arm) return true;
            }

            if (mainItem) {
                if ((main == HumanoidArm.RIGHT && arm == HumanoidArm.RIGHT) || (main == HumanoidArm.LEFT && arm == HumanoidArm.LEFT)) return true;
            }

            if (offItem) {
                if ((main == HumanoidArm.RIGHT && arm == HumanoidArm.LEFT) || (main == HumanoidArm.LEFT && arm == HumanoidArm.RIGHT)) return true;
            }

            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean shouldApply(AvatarRenderState state) {
        if (!(state instanceof RenderStateSkinIdAccess a)) return false;
        String id = a.consoleskins$getSkinId();
        if (!WeepingStatueConfig.isWeepingStatueSkin(id)) return false;
        Player p = getPlayer(state);
        if (p != null && isArmHolding(p, HumanoidArm.RIGHT)) return false;
        return true;
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

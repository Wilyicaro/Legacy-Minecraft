package wily.legacy.skins.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Player;
import wily.legacy.client.LegacyOptions;
import wily.legacy.skins.pose.SkinPoseRegistry;
import wily.legacy.skins.skin.ClientSkinCache;

public final class ViewBobbingSkinOverride {
    private static Boolean savedUserBobView;
    private static boolean forcing;

    private ViewBobbingSkinOverride() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null) return;
        Options options = minecraft.options;
        if (options == null) return;

        if (shouldForceOff(minecraft)) {
            forceOff(options);
            return;
        }
        restore(options);
    }

    public static void reset(Minecraft minecraft) {
        if (!forcing) {
            savedUserBobView = null;
            return;
        }
        restore(minecraft != null ? minecraft.options : null);
    }

    private static boolean shouldForceOff(Minecraft minecraft) {
        if (minecraft.level == null || !LegacyOptions.customSkinAnimation.get()) return false;
        Player player = minecraft.player;
        if (player == null) return false;
        String skinId = ClientSkinCache.get(player.getUUID());
        return SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.DISABLE_VIEW_BOBBING, skinId)
                || SkinPoseRegistry.hasPose(SkinPoseRegistry.PoseTag.STIFF_LEGS, skinId);
    }

    private static void forceOff(Options options) {
        if (options == null) return;
        if (!forcing) {
            savedUserBobView = options.bobView().get();
            forcing = true;
        }
        if (options.bobView().get()) {
            options.bobView().set(false);
        }
    }

    private static void restore(Options options) {
        if (options != null && savedUserBobView != null) {
            options.bobView().set(savedUserBobView);
        }
        forcing = false;
        savedUserBobView = null;
    }
}

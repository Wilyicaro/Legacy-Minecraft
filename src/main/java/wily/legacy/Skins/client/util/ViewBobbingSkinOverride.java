package wily.legacy.Skins.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.player.Player;
import wily.legacy.Skins.client.render.ViewBobbingConfig;
import wily.legacy.Skins.skin.ClientSkinCache;
public final class ViewBobbingSkinOverride {
    private static Boolean savedUserBobView = null;
    private static boolean forcing = false;

    private ViewBobbingSkinOverride() {
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft == null) return;

        Options options;
        try {
            options = minecraft.options;
        } catch (Throwable t) {
            return;
        }
        if (options == null) return;

        boolean animationsEnabled;
        try {
            animationsEnabled = ConsoleSkinsClientSettings.isSkinAnimations();
        } catch (Throwable t) {
            animationsEnabled = true;
        }

        boolean shouldForceOff = false;
        try {
            Player p = minecraft.player;
            if (animationsEnabled && minecraft.level != null && p != null) {
                String skinId = ClientSkinCache.get(p.getUUID());
                shouldForceOff = ViewBobbingConfig.isViewBobbingDisabled(skinId);
            }
        } catch (Throwable ignored) {
            shouldForceOff = false;
        }

        try {
            if (shouldForceOff) {
                if (!forcing) {
                    try {
                        savedUserBobView = options.bobView().get();
                    } catch (Throwable ignored) {
                        savedUserBobView = null;
                    }
                    forcing = true;
                }
                try {
                    if (options.bobView().get()) options.bobView().set(false);
                } catch (Throwable ignored) {
                }
            } else if (forcing) {
                try {
                    if (savedUserBobView != null) options.bobView().set(savedUserBobView);
                } catch (Throwable ignored) {
                }
                forcing = false;
                savedUserBobView = null;
            }
        } catch (Throwable ignored) {
        }
    }

    public static void reset(Minecraft minecraft) {
        if (!forcing) {
            savedUserBobView = null;
            return;
        }
        try {
            if (minecraft != null && minecraft.options != null && savedUserBobView != null) {
                minecraft.options.bobView().set(savedUserBobView);
            }
        } catch (Throwable ignored) {
        }
        forcing = false;
        savedUserBobView = null;
    }
}

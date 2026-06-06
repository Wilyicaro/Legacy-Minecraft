package wily.legacy.skins.skin;

import net.minecraft.client.Minecraft;
import wily.factoryapi.FactoryAPIClient;

public final class SkinFairness {
    private SkinFairness() {
    }

    public static String effectiveSkinId(Minecraft client, String skinId) {
        String id = SkinIdUtil.normalize(skinId);
        if (SkinIdUtil.isBlankOrAutoSelect(id)) return id;
        return shouldUseSafeSkin(client, id) ? SkinIdUtil.AUTO_SELECT : id;
    }

    public static boolean isFair(String skinId) {
        if (SkinIdUtil.isBlankOrAutoSelect(skinId)) return true;
        SkinEntry entry = SkinPackLoader.getSkin(skinId);
        return entry == null || entry.fair();
    }

    public static boolean shouldUseSafeSkin(Minecraft client, String skinId) {
        return isRestrictedServer(client) && !isFair(skinId);
    }

    public static boolean isRestrictedServer(Minecraft client) {
        return false; // Mods can change this via Mixin
    }

}

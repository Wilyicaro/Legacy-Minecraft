package wily.legacy.Skins.client.compat.cpm;

import wily.legacy.Skins.client.cpm.CpmModelManager;
import wily.legacy.Skins.client.gui.GuiCpmPreviewCache;
import wily.legacy.Skins.skin.SkinIdUtil;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;

public final class CpmCompat {

    private CpmCompat() {
    }

    public static void applyOrClear(GameProfile profile, String skinId) {
        CpmModelManager.applyToProfile(profile, skinId);
    }

    public static void prewarmPreview(String skinId, ResourceLocation previewSkin) {
        if (!SkinIdUtil.isCpm(skinId) || previewSkin == null) return;
        GuiCpmPreviewCache.prewarmMenuPreview(skinId, previewSkin);
    }

    public static boolean isPreviewResolved(String skinId, ResourceLocation previewSkin) {
        if (!SkinIdUtil.isCpm(skinId) || previewSkin == null) return false;
        return GuiCpmPreviewCache.isResolved(skinId, previewSkin);
    }
}

package wily.legacy.Skins.client.compat;

import net.minecraft.client.gui.GuiGraphics;
import wily.legacy.Skins.client.compat.legacyskins.LegacySkinsCompat;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.skin.SkinPackSourceKind;

final class LegacySkinsProviderCompat implements ExternalSkinSelectionProvider, ExternalSkinPreviewProvider {

    @Override
    public String providerId() {
        return "LegacySkins";
    }

    @Override
    public SkinPackSourceKind sourceKind() {
        return SkinPackSourceKind.LEGACY_SKINS;
    }

    @Override
    public boolean ownsSkinId(String skinId) {
        return LegacySkinsCompat.isLegacySkinId(skinId);
    }

    @Override
    public ExternalSkinProviderCapabilities capabilities() {
        boolean modPresent = LegacySkinsCompat.isPresent();
        boolean coreAvailable = LegacySkinsCompat.isAvailable();
        boolean previewAvailable = LegacySkinsCompat.isPreviewSupported();
        return new ExternalSkinProviderCapabilities(
                modPresent,
                coreAvailable,
                coreAvailable,
                false,
                false,
                true,
                previewAvailable,
                LegacySkinsCompat.isPreviewPoseSupported()
        );
    }

    @Override
    public String getCurrentSelectedSkinId() {
        return LegacySkinsCompat.getCurrentSelectedSkinId();
    }

    @Override
    public boolean applySelectedSkin(String skinId) {
        return LegacySkinsCompat.applySelectedSkin(skinId);
    }

    @Override
    public void clearSelectedSkin() {
        LegacySkinsCompat.clearSelectedSkin();
    }

    @Override
    public void importCurrentSelectionIfAbsent() {
        LegacySkinsCompat.importCurrentSelectionIfAbsent();
    }

    @Override
    public boolean canResolveSelection(String skinId) {
        return LegacySkinsCompat.canResolveSelectedSkinId(skinId);
    }

    @Override
    public boolean canRenderPreview(String skinId) {
        return LegacySkinsCompat.canRenderPreview(skinId);
    }

    @Override
    public boolean renderPreview(PlayerSkinWidget owner,
                                 GuiGraphics guiGraphics,
                                 String skinId,
                                 float rotationX,
                                 float rotationY,
                                 boolean crouching,
                                 boolean punchLoop,
                                 float partialTick,
                                 int left,
                                 int top,
                                 int right,
                                 int bottom) {
        return LegacySkinsCompat.renderPreview(owner, guiGraphics, skinId, rotationX, rotationY, crouching, punchLoop, partialTick, left, top, right, bottom);
    }

    @Override
    public void resetPreviewCache() {
        LegacySkinsCompat.resetPreviewCache();
    }
}

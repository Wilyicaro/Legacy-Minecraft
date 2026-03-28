package wily.legacy.Skins.client.compat;

import net.minecraft.client.gui.GuiGraphics;
import wily.legacy.Skins.client.compat.bedrockskins.BedrockSkinsCompat;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.skin.SkinPackSourceKind;

import java.util.ArrayList;
import java.util.List;

final class BedrockSkinsProviderCompat implements ExternalSkinSelectionProvider, ExternalSkinPreviewProvider, ExternalSkinPackProvider {

    @Override
    public String providerId() {
        return "BedrockSkins";
    }

    @Override
    public SkinPackSourceKind sourceKind() {
        return SkinPackSourceKind.BEDROCK_SKINS;
    }

    @Override
    public boolean ownsSkinId(String skinId) {
        return BedrockSkinsCompat.isBedrockSkinId(skinId);
    }

    @Override
    public ExternalSkinProviderCapabilities capabilities() {
        boolean modPresent = BedrockSkinsCompat.isPresent();
        boolean coreAvailable = BedrockSkinsCompat.isAvailable();
        boolean previewAvailable = BedrockSkinsCompat.isPreviewSupported();
        return new ExternalSkinProviderCapabilities(
                modPresent,
                coreAvailable,
                coreAvailable,
                true,
                coreAvailable,
                true,
                previewAvailable,
                BedrockSkinsCompat.isPreviewPoseSupported()
        );
    }

    @Override
    public String getCurrentSelectedSkinId() {
        return BedrockSkinsCompat.getCurrentSelectedSkinId();
    }

    @Override
    public boolean applySelectedSkin(String skinId) {
        return BedrockSkinsCompat.applySelectedSkin(skinId);
    }

    @Override
    public void clearSelectedSkin() {
        BedrockSkinsCompat.clearSelectedSkin();
    }

    @Override
    public void importCurrentSelectionIfAbsent() {
        BedrockSkinsCompat.importCurrentSelectionIfAbsent();
    }

    @Override
    public boolean canResolveSelection(String skinId) {
        return BedrockSkinsCompat.canResolveSelectedSkinId(skinId);
    }

    @Override
    public boolean canRenderPreview(String skinId) {
        return BedrockSkinsCompat.canRenderPreview(skinId);
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
        return BedrockSkinsCompat.renderPreview(owner, guiGraphics, skinId, rotationX, rotationY, crouching, punchLoop, partialTick, left, top, right, bottom);
    }

    @Override
    public void resetPreviewCache() {
        BedrockSkinsCompat.resetPreviewCache();
    }

    @Override
    public List<ExternalSkinPackDescriptor> loadPackDescriptors() {
        List<BedrockSkinsCompat.PackDescriptor> descriptors = BedrockSkinsCompat.loadPackDescriptors();
        if (descriptors == null || descriptors.isEmpty()) return List.of();

        ArrayList<ExternalSkinPackDescriptor> externalDescriptors = new ArrayList<>(descriptors.size());
        for (BedrockSkinsCompat.PackDescriptor descriptor : descriptors) {
            if (descriptor == null) continue;
            ArrayList<ExternalSkinDescriptor> skins = new ArrayList<>();
            if (descriptor.skins() != null) {
                for (BedrockSkinsCompat.SkinDescriptor skin : descriptor.skins()) {
                    if (skin == null) continue;
                    skins.add(new ExternalSkinDescriptor(skin.id(), skin.name()));
                }
            }
            externalDescriptors.add(new ExternalSkinPackDescriptor(
                    descriptor.id(),
                    descriptor.name(),
                    descriptor.type(),
                    descriptor.icon(),
                    List.copyOf(skins),
                    descriptor.sortIndex()
            ));
        }
        return List.copyOf(externalDescriptors);
    }
}

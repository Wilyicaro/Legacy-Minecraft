package wily.legacy.mixin.base.client.gui;

import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyGuiElements;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(SpectatorGui.class)
public abstract class SpectatorGuiMixin {

    @Shadow
    private SpectatorMenu menu;

    @Shadow
    protected abstract void extractPage(GuiGraphicsExtractor arg, float f, int i, int j, SpectatorPage arg2);

    @Redirect(method = "extractHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;extractPage(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FIILnet/minecraft/client/gui/spectator/categories/SpectatorPage;)V"))
    public void renderHotbarPage(SpectatorGui instance, GuiGraphicsExtractor k, float arg, int f, int i, SpectatorPage j) {
        this.extractPage(k, arg * LegacyRenderUtil.getHUDOpacity(), f, k.guiHeight() - 22, j);
    }

    @Inject(method = "extractPage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V", ordinal = 1))
    private void renderHotbarSelection(GuiGraphicsExtractor GuiGraphicsExtractor, float f, int i, int j, SpectatorPage spectatorPage, CallbackInfo ci) {
        FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, f);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.HOTBAR_SELECTION, 24, 24, 0, 23, GuiGraphicsExtractor.guiWidth() / 2 - 91 - 1 + spectatorPage.getSelectedSlot() * 20, j + 22, 24, 1);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).clearBlitColor();
    }

    @Inject(method = "onHotbarSelected", at = @At("HEAD"))
    public void onHotbarSelected(int i, CallbackInfo ci) {
        if (menu != null && i != menu.getSelectedSlot()) LegacyGuiElements.lastHotbarSelectionChange = Util.getMillis();
    }
}

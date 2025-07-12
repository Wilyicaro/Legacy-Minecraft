package wily.legacy.mixin.base.client.gui;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
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

    @Shadow protected abstract void renderPage(GuiGraphics arg, float f, int i, int j, SpectatorPage arg2);

    @Shadow private SpectatorMenu menu;

    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;renderPage(Lnet/minecraft/client/gui/GuiGraphics;FIILnet/minecraft/client/gui/spectator/categories/SpectatorPage;)V"))
    public void renderHotbarPage(SpectatorGui instance, GuiGraphics k, float arg, int f, int i, SpectatorPage j) {
        this.renderPage(k,arg* LegacyRenderUtil.getHUDOpacity(),f,k.guiHeight()-22,j);
    }
    @Inject(method = "renderPage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIII)V", ordinal = 1))
    private void renderHotbarSelection(GuiGraphics guiGraphics, float f, int i, int j, SpectatorPage spectatorPage, CallbackInfo ci) {
        FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f,1.0f,1.0f, f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.HOTBAR_SELECTION,24,24,0,23,guiGraphics.guiWidth() / 2 - 91 - 1 + spectatorPage.getSelectedSlot() * 20, j + 22, 24, 1);
        FactoryGuiGraphics.of(guiGraphics).clearBlitColor();
    }
    @Inject(method = "onHotbarSelected", at = @At("HEAD"))
    public void onHotbarSelected(int i, CallbackInfo ci) {
        if (menu != null && i != menu.getSelectedSlot()) LegacyGuiElements.lastHotbarSelectionChange = Util.getMillis();
    }
}

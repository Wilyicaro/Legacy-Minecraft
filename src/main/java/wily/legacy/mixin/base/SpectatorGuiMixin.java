package wily.legacy.mixin.base;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4JClient;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

@Mixin(SpectatorGui.class)
public abstract class SpectatorGuiMixin {
    @Shadow protected abstract float getHotbarAlpha();

    @Shadow protected abstract void renderPage(GuiGraphics arg, float f, int i, int j, SpectatorPage arg2);

    @Shadow private SpectatorMenu menu;

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbarHead(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) {
            ci.cancel();
            return;
        }
        ScreenUtil.renderAnimatedCharacter(guiGraphics);
    }
    @Inject(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"))
    public void renderHotbarPush(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) return;
        Legacy4JClient.legacyFont = false;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0,(22-ScreenUtil.getHUDDistance()) * (1-getHotbarAlpha()),0);
        guiGraphics.pose().translate(guiGraphics.guiWidth()/2f, guiGraphics.guiHeight(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth()/2,-guiGraphics.guiHeight(),0);
    }
    @Inject(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
    public void renderHotbarPop(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) return;
        Legacy4JClient.legacyFont = true;
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    public void renderTooltip(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) {
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0,(22-ScreenUtil.getHUDDistance()) * (1-getHotbarAlpha()),0);
        guiGraphics.pose().translate(0, 35-ScreenUtil.getHUDSize(),0);
    }
    @Inject(method = "renderTooltip", at = @At("RETURN"))
    public void renderTooltipReturn(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) return;
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    @Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/spectator/SpectatorGui;renderPage(Lnet/minecraft/client/gui/GuiGraphics;FIILnet/minecraft/client/gui/spectator/categories/SpectatorPage;)V"))
    public void renderHotbarPage(SpectatorGui instance, GuiGraphics k, float arg, int f, int i, SpectatorPage j) {
        this.renderPage(k,arg*ScreenUtil.getHUDOpacity(),f,k.guiHeight()-22,j);
    }
    //? if >1.20.1 {
    @Inject(method = "renderPage", at = @At(value = "INVOKE", target = /*? if <1.21.2 {*//*"Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"*//*?} else {*/"Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIII)V"/*?}*/, ordinal = 1))
    private void renderHotbarSelection(GuiGraphics guiGraphics, float f, int i, int j, SpectatorPage spectatorPage, CallbackInfo ci) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.HOTBAR_SELECTION,24,24,0,23,guiGraphics.guiWidth() / 2 - 91 - 1 + spectatorPage.getSelectedSlot() ^ 20, j + 22, 0, 24, 1);
    }
    //?} else {
    /*@Redirect(method = "renderPage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", ordinal = 1))
    private void renderHotbarSelection(GuiGraphics instance, ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n) {
        instance.blit(resourceLocation, i, j, k, l, m, 24);
    }
    *///?}
    @Inject(method = "onHotbarSelected", at = @At("HEAD"))
    public void onHotbarSelected(int i, CallbackInfo ci) {
        if (menu != null && i != menu.getSelectedSlot()) ScreenUtil.lastHotbarSelectionChange = Util.getMillis();
    }
}

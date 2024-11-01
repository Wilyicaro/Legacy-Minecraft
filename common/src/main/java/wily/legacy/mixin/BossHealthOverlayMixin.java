package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.util.ScreenUtil;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @Shadow @Final private Minecraft minecraft;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    public int drawString(GuiGraphics graphics, Font font, Component component, int i, int j, int k) {
        Legacy4JClient.applyFontOverrideIf(minecraft.getWindow().getHeight() <= 720, LegacyIconHolder.MOJANGLES_11_FONT, b->{
            Legacy4JClient.forceVanillaFontShadowColor = true;
            graphics.pose().pushPose();
            graphics.pose().translate(graphics.guiWidth() / 2f,j,0);
            if (!b) graphics.pose().scale(2/3f,2/3f,2/3f);
            graphics.pose().translate(-font.width(component) / 2f,0,0);
            graphics.drawString(font,component,0,0,k);
            graphics.pose().popPose();
            Legacy4JClient.forceVanillaFontShadowColor = false;
        });
        return 0;
    }

    @Shadow protected abstract void drawBar(GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, int k, ResourceLocation[] resourceLocations, ResourceLocation[] resourceLocations2);
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f, ScreenUtil.getInterfaceOpacity());

    }
    @Inject(method = "render", at = @At("RETURN"))
    public void renderReturn(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0), ordinal = 1)
    public int render(int i) {
        return (int) (12 + 16 * LegacyOption.hudDistance.get());
    }
    @Inject(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V", at = @At("HEAD"))
    private void drawBar(GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, CallbackInfo ci) {
        guiGraphics.pose().pushPose();
        RenderSystem.enableBlend();
        guiGraphics.pose().translate((guiGraphics.guiWidth() - 203) / 2f,j,0);
        guiGraphics.pose().scale(0.5f,0.5f,0.5f);
    }
    @Inject(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V", at = @At("RETURN"))
    private void drawBarReturn(GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, CallbackInfo ci) {
        RenderSystem.disableBlend();
        guiGraphics.pose().popPose();
    }
    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/ResourceLocation;[Lnet/minecraft/resources/ResourceLocation;)V", ordinal = 0))
    private void drawBar(BossHealthOverlay instance, GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, int k, ResourceLocation[] resourceLocations, ResourceLocation[] resourceLocations2) {
        drawBar(guiGraphics,0,0,bossEvent,406,resourceLocations,resourceLocations2);
    }
    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/ResourceLocation;[Lnet/minecraft/resources/ResourceLocation;)V", ordinal = 1))
    private void drawBarProgress(BossHealthOverlay instance, GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, int k, ResourceLocation[] resourceLocations, ResourceLocation[] resourceLocations2) {
        guiGraphics.pose().translate(3f,0,0);
        drawBar(guiGraphics,0,0,bossEvent, Mth.lerpDiscrete(bossEvent.getProgress(), 0, 400),resourceLocations,resourceLocations2);
    }
    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/ResourceLocation;[Lnet/minecraft/resources/ResourceLocation;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V"))
    private void drawBar(GuiGraphics guiGraphics, ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n, int o, int p) {
        guiGraphics.blitSprite(resourceLocation,o <= 400 ? 400 : 406,j * 3,k,l,m,n,o,p * 3);
    }
}

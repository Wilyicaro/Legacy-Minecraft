package wily.legacy.mixin.base;

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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.util.ScreenUtil;

import java.util.function.Function;

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
    //? if >1.20.1 {
    @Shadow protected abstract void drawBar(GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, int k, ResourceLocation[] resourceLocations, ResourceLocation[] resourceLocations2);
    //?}
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
    //? if >1.20.1 {
    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/ResourceLocation;[Lnet/minecraft/resources/ResourceLocation;)V", ordinal = 0))
    private void drawBar(BossHealthOverlay instance, GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, int k, ResourceLocation[] resourceLocations, ResourceLocation[] resourceLocations2) {
        drawBar(guiGraphics,0,0,bossEvent,406,resourceLocations,resourceLocations2);
    }
    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/ResourceLocation;[Lnet/minecraft/resources/ResourceLocation;)V", ordinal = 1))
    private void drawBarProgress(BossHealthOverlay instance, GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, int k, ResourceLocation[] resourceLocations, ResourceLocation[] resourceLocations2) {
        guiGraphics.pose().translate(3f,0,0);
        drawBar(guiGraphics,0,0,bossEvent, Mth.lerpDiscrete(bossEvent.getProgress(), 0, 400),resourceLocations,resourceLocations2);
    }
    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/ResourceLocation;[Lnet/minecraft/resources/ResourceLocation;)V", at = @At(value = "INVOKE", target = /*? if <1.21.2 {*//*"Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V"*//*?} else {*/"Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V"/*?}*/))
    private void drawBar(GuiGraphics guiGraphics, /*? if >=1.21.2 {*/Function function, /*?}*/ ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n, int o, int p) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(resourceLocation,o <= 400 ? 400 : 406,j * 3,k,l,m,n,0,o,p * 3);
    }
    //?} else {
    /*@Unique
    private static final ResourceLocation[] BAR_BACKGROUND_SPRITES = new ResourceLocation[]{new ResourceLocation("boss_bar/pink_background"), new ResourceLocation("boss_bar/blue_background"), new ResourceLocation("boss_bar/red_background"), new ResourceLocation("boss_bar/green_background"), new ResourceLocation("boss_bar/yellow_background"), new ResourceLocation("boss_bar/purple_background"), new ResourceLocation("boss_bar/white_background")};
    @Unique
    private static final ResourceLocation[] BAR_PROGRESS_SPRITES = new ResourceLocation[]{new ResourceLocation("boss_bar/pink_progress"), new ResourceLocation("boss_bar/blue_progress"), new ResourceLocation("boss_bar/red_progress"), new ResourceLocation("boss_bar/green_progress"), new ResourceLocation("boss_bar/yellow_progress"), new ResourceLocation("boss_bar/purple_progress"), new ResourceLocation("boss_bar/white_progress")};
    @Unique
    private static final ResourceLocation[] OVERLAY_BACKGROUND_SPRITES = new ResourceLocation[]{new ResourceLocation("boss_bar/notched_6_background"), new ResourceLocation("boss_bar/notched_10_background"), new ResourceLocation("boss_bar/notched_12_background"), new ResourceLocation("boss_bar/notched_20_background")};
    @Unique
    private static final ResourceLocation[] OVERLAY_PROGRESS_SPRITES = new ResourceLocation[]{new ResourceLocation("boss_bar/notched_6_progress"), new ResourceLocation("boss_bar/notched_10_progress"), new ResourceLocation("boss_bar/notched_12_progress"), new ResourceLocation("boss_bar/notched_20_progress")};

    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;II)V", ordinal = 0))
    private void drawBar(BossHealthOverlay instance, GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, int k, int l) {
        drawBar(guiGraphics,0,0,bossEvent,406,BAR_BACKGROUND_SPRITES,OVERLAY_BACKGROUND_SPRITES);
    }
    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;II)V",ordinal = 1))
    private void drawBarProgress(BossHealthOverlay instance, GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, int k, int l) {
        guiGraphics.pose().translate(3f,0,0);
        drawBar(guiGraphics,0,0,bossEvent, lerpDiscrete(bossEvent.getProgress(), 0, 400),BAR_PROGRESS_SPRITES,OVERLAY_PROGRESS_SPRITES);
    }
    private void drawBar(GuiGraphics guiGraphics, int i, int j, BossEvent bossEvent, int k, ResourceLocation[] resourceLocations, ResourceLocation[] resourceLocations2) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(resourceLocations[bossEvent.getColor().ordinal()], k <= 400 ? 400 : 406, 15, 0, 0, i, j, k, 15);
        if (bossEvent.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
            RenderSystem.enableBlend();
            FactoryGuiGraphics.of(guiGraphics).blitSprite(resourceLocations2[bossEvent.getOverlay().ordinal() - 1], k <= 400 ? 400 : 406, 15, 0, 0, i, j, k, 15);
            RenderSystem.disableBlend();
        }

    }
    private static int lerpDiscrete(float f, int i, int j) {
        int k = j - i;
        return i + Mth.floor(f * (float)(k - 1)) + (f > 0.0F ? 1 : 0);
    }
    *///?}
}

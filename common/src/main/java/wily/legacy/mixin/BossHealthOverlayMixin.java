package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyGuiGraphics;
import wily.legacy.util.ScreenUtil;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    private static final ResourceLocation[] BAR_BACKGROUND_SPRITES = new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/pink_background"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/blue_background"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/red_background"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/green_background"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/yellow_background"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/purple_background"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/white_background")};
    private static final ResourceLocation[] BAR_PROGRESS_SPRITES = new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/pink_progress"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/blue_progress"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/red_progress"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/green_progress"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/yellow_progress"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/purple_progress"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/white_progress")};
    private static final ResourceLocation[] OVERLAY_BACKGROUND_SPRITES = new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/notched_6_background"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/notched_10_background"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/notched_12_background"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/notched_20_background")};
    private static final ResourceLocation[] OVERLAY_PROGRESS_SPRITES = new ResourceLocation[]{new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/notched_6_progress"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/notched_10_progress"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/notched_12_progress"), new ResourceLocation(Legacy4J.MOD_ID,"boss_bar/notched_20_progress")};

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    public int drawString(PoseStack graphics, Font font, Component component, int i, int j, int k) {
        graphics.pose().pushPose();
        graphics.pose().translate((graphics.guiWidth() - font.width(component) * 2/3f) / 2,j,0);
        graphics.pose().scale(2/3f,2/3f,2/3f);
        graphics.pose().translate(-i,-j,0);
        int draw = graphics.drawString(font,component,i,j,k);
        graphics.pose().popPose();
        return draw;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f, ScreenUtil.getInterfaceOpacity());

    }
    @Inject(method = "render", at = @At("RETURN"))
    public void renderReturn(PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        RenderSystem.setShaderColor(1.0f,1.0f,1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
    @ModifyVariable(method = "render", at = @At(value = "STORE", ordinal = 0), ordinal = 1)
    public int render(int i) {
        return 28;
    }
    @Inject(method = "drawBar(Lcom/mojang/blaze3d/vertex/PoseStack;IILnet/minecraft/world/BossEvent;)V", at = @At("HEAD"))
    private void drawBar(PoseStack poseStack, int i, int j, BossEvent bossEvent, CallbackInfo ci) {
        poseStack.pose().pushPose();
        RenderSystem.enableBlend();
        poseStack.pose().translate((poseStack.guiWidth() - 203) / 2f,j,0);
        poseStack.pose().scale(0.5f,0.5f,0.5f);
    }
    @Inject(method = "drawBar(Lcom/mojang/blaze3d/vertex/PoseStack;IILnet/minecraft/world/BossEvent;)V", at = @At("RETURN"))
    private void drawBarReturn(PoseStack poseStack, int i, int j, BossEvent bossEvent, CallbackInfo ci) {
        RenderSystem.disableBlend();
        poseStack.pose().popPose();
    }
    @Redirect(method = "drawBar(Lcom/mojang/blaze3d/vertex/PoseStack;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lcom/mojang/blaze3d/vertex/PoseStack;IILnet/minecraft/world/BossEvent;II)V", ordinal = 0))
    private void drawBar(BossHealthOverlay instance, PoseStack poseStack, int i, int j, BossEvent bossEvent, int k, int l) {
        drawBar(poseStack,0,0,bossEvent,406,BAR_BACKGROUND_SPRITES,OVERLAY_BACKGROUND_SPRITES);
    }
    @Redirect(method = "drawBar(Lcom/mojang/blaze3d/vertex/PoseStack;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lcom/mojang/blaze3d/vertex/PoseStack;IILnet/minecraft/world/BossEvent;II)V", ordinal = 1))
    private void drawBarProgress(BossHealthOverlay instance, PoseStack poseStack, int i, int j, BossEvent bossEvent, int k, int l) {
        poseStack.pose().translate(3f,0,0);
        drawBar(poseStack,0,0,bossEvent, lerpDiscrete(bossEvent.getProgress(), 0, 400),BAR_PROGRESS_SPRITES,OVERLAY_PROGRESS_SPRITES);
    }
    private void drawBar(PoseStack poseStack, int i, int j, BossEvent bossEvent, int k, ResourceLocation[] resourceLocations, ResourceLocation[] resourceLocations2) {
        LegacyGuiGraphics.of(poseStack).blitSprite(resourceLocations[bossEvent.getColor().ordinal()], k <= 400 ? 400 : 406, 15, 0, 0, i, j, k, 15);
        if (bossEvent.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
            RenderSystem.enableBlend();
            LegacyGuiGraphics.of(poseStack).blitSprite(resourceLocations2[bossEvent.getOverlay().ordinal() - 1], k <= 400 ? 400 : 406, 15, 0, 0, i, j, k, 15);
            RenderSystem.disableBlend();
        }

    }
    private static int lerpDiscrete(float f, int i, int j) {
        int k = j - i;
        return i + Mth.floor(f * (float)(k - 1)) + (f > 0.0F ? 1 : 0);
    }
}

package wily.legacy.mixin.base.client.bosshealth;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyFontUtil;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    public void drawString(GuiGraphicsExtractor graphics, Font font, Component component, int i, int j, int k) {
        LegacyFontUtil.applySmallerFont(LegacyFontUtil.MOJANGLES_11_FONT, b -> {
            LegacyFontUtil.forceVanillaFontShadowColor = true;
            graphics.pose().pushMatrix();
            graphics.pose().translate(graphics.guiWidth() / 2f, j);
            if (!b) graphics.pose().scale(2 / 3f, 2 / 3f);
            graphics.pose().translate(-font.width(component) / 2f, 0);
            graphics.text(font, component, 0, 0, k);
            graphics.pose().popMatrix();
            LegacyFontUtil.forceVanillaFontShadowColor = false;
        });
    }

    //? if >1.20.1 {
    @Shadow
    protected abstract void extractBar(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, BossEvent bossEvent, int k, Identifier[] resourceLocations, Identifier[] resourceLocations2);

    //?}
    @ModifyVariable(method = "extractRenderState", at = @At(value = "STORE", ordinal = 0), ordinal = 1)
    public int render(int i) {
        return (int) (12 + 16 * LegacyOptions.hudDistance.get());
    }

    @Inject(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At("HEAD"))
    private void drawBar(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, BossEvent bossEvent, CallbackInfo ci) {
        GuiGraphicsExtractor.pose().pushMatrix();
        FactoryScreenUtil.enableBlend();
        GuiGraphicsExtractor.pose().translate((GuiGraphicsExtractor.guiWidth() - 203) / 2f, j);
        GuiGraphicsExtractor.pose().scale(0.5f, 0.5f);
    }

    @Inject(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At("RETURN"))
    private void drawBarReturn(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, BossEvent bossEvent, CallbackInfo ci) {
        FactoryScreenUtil.disableBlend();
        GuiGraphicsExtractor.pose().popMatrix();
    }

    //? if >1.20.1 {
    @Redirect(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/Identifier;[Lnet/minecraft/resources/Identifier;)V", ordinal = 0))
    private void drawBar(BossHealthOverlay instance, GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, BossEvent bossEvent, int k, Identifier[] resourceLocations, Identifier[] resourceLocations2) {
        extractBar(GuiGraphicsExtractor, 0, 0, bossEvent, 406, resourceLocations, resourceLocations2);
    }

    @Redirect(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/Identifier;[Lnet/minecraft/resources/Identifier;)V", ordinal = 1))
    private void drawBarProgress(BossHealthOverlay instance, GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, BossEvent bossEvent, int k, Identifier[] resourceLocations, Identifier[] resourceLocations2) {
        GuiGraphicsExtractor.pose().translate(3f, 0);
        extractBar(GuiGraphicsExtractor, 0, 0, bossEvent, Mth.lerpDiscrete(bossEvent.getProgress(), 0, 400), resourceLocations, resourceLocations2);
    }

    @Redirect(method = "extractBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;I[Lnet/minecraft/resources/Identifier;[Lnet/minecraft/resources/Identifier;)V", at = @At(value = "INVOKE", target = /*? if <1.21.2 {*//*"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lnet/minecraft/resources/Identifier;IIIIIIII)V"*//*?} else {*/"Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIIIII)V"/*?}*/))
    private void drawBar(GuiGraphicsExtractor instance, RenderPipeline renderPipeline, Identifier arg, int i, int j, int k, int l, int m, int n, int o, int p  /*? if >=1.21.2 {*/ /*?}*/) {
        FactoryGuiGraphics.of(instance).blitSprite(arg, o <= 400 ? 400 : 406, j * 3, k, l, m, n, 0, o, p * 3);
    }
    //?} else {
    /*@Unique
    private static final Identifier[] BAR_BACKGROUND_SPRITES = new Identifier[]{new Identifier("boss_bar/pink_background"), new Identifier("boss_bar/blue_background"), new Identifier("boss_bar/red_background"), new Identifier("boss_bar/green_background"), new Identifier("boss_bar/yellow_background"), new Identifier("boss_bar/purple_background"), new Identifier("boss_bar/white_background")};
    @Unique
    private static final Identifier[] BAR_PROGRESS_SPRITES = new Identifier[]{new Identifier("boss_bar/pink_progress"), new Identifier("boss_bar/blue_progress"), new Identifier("boss_bar/red_progress"), new Identifier("boss_bar/green_progress"), new Identifier("boss_bar/yellow_progress"), new Identifier("boss_bar/purple_progress"), new Identifier("boss_bar/white_progress")};
    @Unique
    private static final Identifier[] OVERLAY_BACKGROUND_SPRITES = new Identifier[]{new Identifier("boss_bar/notched_6_background"), new Identifier("boss_bar/notched_10_background"), new Identifier("boss_bar/notched_12_background"), new Identifier("boss_bar/notched_20_background")};
    @Unique
    private static final Identifier[] OVERLAY_PROGRESS_SPRITES = new Identifier[]{new Identifier("boss_bar/notched_6_progress"), new Identifier("boss_bar/notched_10_progress"), new Identifier("boss_bar/notched_12_progress"), new Identifier("boss_bar/notched_20_progress")};

    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;II)V", ordinal = 0))
    private void drawBar(BossHealthOverlay instance, GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, BossEvent bossEvent, int k, int l) {
        drawBar(GuiGraphicsExtractor,0,0,bossEvent,406,BAR_BACKGROUND_SPRITES,OVERLAY_BACKGROUND_SPRITES);
    }
    @Redirect(method = "drawBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/world/BossEvent;II)V",ordinal = 1))
    private void drawBarProgress(BossHealthOverlay instance, GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, BossEvent bossEvent, int k, int l) {
        GuiGraphicsExtractor.pose().translate(3f,0,0);
        drawBar(GuiGraphicsExtractor,0,0,bossEvent, lerpDiscrete(bossEvent.getProgress(), 0, 400),BAR_PROGRESS_SPRITES,OVERLAY_PROGRESS_SPRITES);
    }
    private void drawBar(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, BossEvent bossEvent, int k, Identifier[] resourceLocations, Identifier[] resourceLocations2) {
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(resourceLocations[bossEvent.getColor().ordinal()], k <= 400 ? 400 : 406, 15, 0, 0, i, j, k, 15);
        if (bossEvent.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
            FactoryScreenUtil.enableBlend();
            FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(resourceLocations2[bossEvent.getOverlay().ordinal() - 1], k <= 400 ? 400 : 406, 15, 0, 0, i, j, k, 15);
            FactoryScreenUtil.disableBlend();
        }

    }
    private static int lerpDiscrete(float f, int i, int j) {
        int k = j - i;
        return i + Mth.floor(f * (float)(k - 1)) + (f > 0.0F ? 1 : 0);
    }
    *///?}
}

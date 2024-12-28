package wily.legacy.mixin.base;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
//? if >1.20.2 {
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
//?} else {
/*import net.minecraft.world.scores.Score;
*///?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.BufferSourceWrapper;
import wily.legacy.client.LegacyOption;
import wily.legacy.network.TopMessage;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.ScreenUtil;
//? if forge {
/*import net.minecraftforge.client.extensions.common.IClientItemExtensions;
*///?} else if neoforge {
/*import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
 *///?}

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static wily.legacy.client.screen.ControlTooltip.MORE;


@Mixin(Gui.class)

public abstract class GuiMixin implements ControlTooltip.Event {
    @Shadow @Final
    private Minecraft minecraft;
    @Shadow
    private ItemStack lastToolHighlight;
    @Shadow
    private int toolHighlightTimer;
    //? if >1.20.2 {
    @Final
    @Shadow
    private static Comparator<? super PlayerScoreEntry> SCORE_DISPLAY_ORDER;
    //?}

    @Shadow public abstract Font getFont();

    @Unique
    private int lastHotbarSelection = -1;

    @Shadow private long healthBlinkTime;


    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getPopTime()I"))
    public int renderSlot(ItemStack instance) {
        return 0;
    }
    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    public void renderVignette(GuiGraphics guiGraphics, Entity entity, CallbackInfo ci) {
        if (minecraft.screen != null || !LegacyOption.vignette.get())
            ci.cancel();
    }
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    public void renderCrosshair(GuiGraphics guiGraphics/*? if >=1.21 {*/, DeltaTracker deltaTracker/*?}*/, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(guiGraphics.guiWidth()/2f,guiGraphics.guiHeight()/2f,0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth()/2,-guiGraphics.guiHeight()/2,0);
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, ScreenUtil.getHUDOpacity());
    }
    //? if <1.21.2 {
    /*@Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"/^? if forge || neoforge {^//^, remap = false^//^?}^/))
    public void renderCrosshairBlendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor, GlStateManager.SourceFactor sourceFactor2, GlStateManager.DestFactor destFactor2, GuiGraphics guiGraphics) {
        if (LegacyOption.invertedCrosshair.get()) RenderSystem.blendFuncSeparate(sourceFactor,destFactor,sourceFactor2,destFactor2);
    }
    *///?} else {
    @ModifyArg(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    public Function<ResourceLocation, RenderType> renderCrosshair(Function<ResourceLocation, RenderType> function) {
        return LegacyOption.invertedCrosshair.get() ? function : RenderType::guiTextured;
    }
    @ModifyArg(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V"))
    public Function<ResourceLocation, RenderType> renderCrosshairAttackIndicator(Function<ResourceLocation, RenderType> function) {
        return LegacyOption.invertedCrosshair.get() ? function : RenderType::guiTextured;
    }
    //?}
    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    public void renderCrosshairReturn(GuiGraphics guiGraphics/*? if >=1.21 {*/, DeltaTracker deltaTracker/*?}*/, CallbackInfo ci) {
        if (minecraft.screen != null)
            return;
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f,1.0f,1.0f,1.0f);
        guiGraphics.pose().popPose();
    }
    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    public void renderEffects(GuiGraphics guiGraphics/*? if >=1.21 {*/, DeltaTracker deltaTracker/*?}*/, CallbackInfo ci) {
        ci.cancel();
        Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
        if (minecraft.screen != null || collection.isEmpty()) {
            return;
        }
        int i = 0;
        int j = 0;
        float backAlpha = ScreenUtil.getHUDOpacity();
        MobEffectTextureManager mobEffectTextureManager = this.minecraft.getMobEffectTextures();
        for (MobEffectInstance mobEffectInstance : Ordering.natural().reverse().sortedCopy(collection)) {
            Holder<MobEffect> mobEffect = /*? if <1.20.5 {*//*BuiltInRegistries.MOB_EFFECT.wrapAsHolder(mobEffectInstance.getEffect())*//*?} else {*/mobEffectInstance.getEffect()/*?}*/;
            if (!mobEffectInstance.showIcon()) continue;
            int k = guiGraphics.guiWidth() - 55;
            int l = 18;
            if (this.minecraft.isDemo()) {
                l += 15;
            }
            if (mobEffect.value().isBeneficial()) {
                k -= 24 * ++i;
            } else {
                k -= 24 * ++j;
                l += 24;
            }
            float f = 1.0f;
            FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, backAlpha);
            ScreenUtil.renderPointerPanel(guiGraphics, k, l, 24, 24);
            if (mobEffectInstance.endsWithin(200)){
                int m = mobEffectInstance.getDuration();
                f = Mth.clamp((float)m / 10.0f / 5.0f * 0.5f, 0.0f, 0.5f) + Mth.cos((float)m * (float)Math.PI / 5.0f) * Mth.clamp((10 - m / 20) / 10.0f * 0.25f, 0.0f, 0.25f);
            }
            RenderSystem.enableBlend();
            TextureAtlasSprite textureAtlasSprite = mobEffectTextureManager.get(mobEffect/*? if <1.20.5 {*//*.value()*//*?}*/);
            FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, f * backAlpha);
            FactoryGuiGraphics.of(guiGraphics).blit(k + 3, l + 3, 0, 18, 18, textureAtlasSprite);
            RenderSystem.disableBlend();
        }
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    //? if >1.20.1 {
    @Inject(method = /*? if >=1.20.5 {*/"renderItemHotbar"/*?} else {*//*"renderHotbar"*//*?}*/, at = @At(value = "INVOKE", target = /*? if <1.21.2 {*//*"Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"*//*?} else {*/"Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"/*?}*/, ordinal = 1))
    private void renderHotbarSelection(/*? if <1.20.5 {*//*float f, *//*?}*/GuiGraphics guiGraphics/*? if >=1.20.5 {*/, DeltaTracker deltaTracker/*?}*/, CallbackInfo ci) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.HOTBAR_SELECTION,24,24,0,23,guiGraphics.guiWidth() / 2 - 91 - 1 + minecraft.player.getInventory().selected * 20, guiGraphics.guiHeight(), 0,24, 1);
    }
    //?} else {
    /*@Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", ordinal = 1))
    private void renderHotbarSelection(GuiGraphics instance, ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n) {
        instance.blit(resourceLocation, i, j, k, l, m, 24);
    }
    *///?}
    @Inject(method = /*? if >=1.20.5 {*/"renderItemHotbar"/*?} else {*//*"renderHotbar"*//*?}*/, at = @At("HEAD"), cancellable = true)
    public void renderHotbar(/*? if <1.20.5 {*//*float f, *//*?}*/GuiGraphics guiGraphics/*? if >=1.20.5 {*/, DeltaTracker deltaTracker/*?}*/, CallbackInfo ci) {
        if (minecraft.screen != null) {
            ci.cancel();
            return;
        }
        ScreenUtil.renderAnimatedCharacter(guiGraphics);
        int newSelection = minecraft.player != null ? minecraft.player.getInventory().selected : -1;
        if (lastHotbarSelection >= 0 && lastHotbarSelection != newSelection) ScreenUtil.lastHotbarSelectionChange = Util.getMillis();
        lastHotbarSelection = newSelection;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth()/2f, guiGraphics.guiHeight(),0.0F);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth()/2,-guiGraphics.guiHeight(),0);
        if (ScreenUtil.getHUDOpacity() < 1.0) {
            FactoryGuiGraphics.of(guiGraphics).pushBufferSource(BufferSourceWrapper.translucent(FactoryGuiGraphics.of(guiGraphics).getBufferSource()));
        }
    }
    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void displayScoreboardSidebar(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
        ci.cancel();
        if (minecraft.screen != null) return;
        Scoreboard scoreboard = objective.getScoreboard();
        //? if >1.20.2 {
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        List<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective).stream().filter((playerScoreEntry) -> !playerScoreEntry.isHidden()).sorted(SCORE_DISPLAY_ORDER).limit(15L).toList();
        //?} else {
        /*List<Score> scores = scoreboard.getPlayerScores(objective).stream().filter((scorex) -> scorex.getOwner() != null && !scorex.getOwner().startsWith("#")).limit(15L).toList();;
        *///?}
        Component component = objective.getDisplayName();
        int i = this.getFont().width(component);
        int k = this.getFont().width(": ");
        int j = Math.max(i,scores.stream().mapToInt(lv-> {
            int w = getFont().width(/*? if >1.20.2 {*/lv.formatValue(numberFormat)/*?} else {*//*"" + ChatFormatting.RED + lv.getScore()*//*?}*/);
            return this.getFont().width(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(lv./*? if >1.20.2 {*/owner/*?} else {*//*getOwner*//*?}*/()), /*? if >1.20.2 {*/lv.ownerName()/*?} else {*//*Component.literal(lv.getOwner())*//*?}*/)) + (w > 0 ? k + w : 0);
        }).max().orElse(0));

        Objects.requireNonNull(this.getFont());
        int l = scores.size() * 9;
        int m = guiGraphics.guiHeight() / 2 + l / 3;
        int x = guiGraphics.guiWidth() - 8;
        int o = x - j;
        int p = x + 2;
        Objects.requireNonNull(this.getFont());
        int s = m - scores.size() * 9;
        Objects.requireNonNull(this.getFont());
        ScreenUtil.renderPointerPanel(guiGraphics, o - 6,s - 16,j + 12, scores.size() * 9 + 22);
        Font var18 = this.getFont();
        int var10003 = o + j / 2 - i / 2;
        Objects.requireNonNull(this.getFont());
        guiGraphics.drawString(var18, component, var10003, s - 9, -1, false);

        for(int t = 0; t < scores.size(); ++t) {
            /*? if >1.20.2 {*/PlayerScoreEntry/*?} else {*//*Score*//*?}*/ lv = scores.get(t);
            x = scores.size() - t;
            Objects.requireNonNull(this.getFont());
            int u = m - x * 9;
            guiGraphics.drawString(this.getFont(), PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(lv./*? if >1.20.2 {*/owner/*?} else {*//*getOwner*//*?}*/()), /*? if >1.20.2 {*/lv.ownerName()/*?} else {*//*Component.literal(lv.getOwner())*//*?}*/), o, u, -1, false);
            Component score = /*? if >1.20.2 {*/lv.formatValue(numberFormat)/*?} else {*//*Component.literal("" + ChatFormatting.RED + lv.getScore())*//*?}*/;
            guiGraphics.drawString(this.getFont(), score, p - getFont().width(score), u, -1, false);
        }


    }
    @Inject(method = /*? if >=1.20.5 {*/"renderItemHotbar"/*?} else {*//*"renderHotbar"*//*?}*/, at = @At("RETURN"))
    public void renderHotbarTail(/*? if <1.20.5 {*//*float f, *//*?}*/GuiGraphics guiGraphics/*? if >=1.21 {*/, DeltaTracker deltaTracker/*?}*/, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        if (ScreenUtil.getHUDOpacity() < 1.0)
            FactoryGuiGraphics.of(guiGraphics).popBufferSource();
        ScreenUtil.finalizeHUDRender(guiGraphics);
        if (minecraft.player != null) ControlTooltip.Renderer.of(this).render(guiGraphics,0,0,/*? if >=1.20.5 {*/deltaTracker.getRealtimeDeltaTicks()/*?} else {*//*f*//*?}*/);
        renderTopText(guiGraphics,TopMessage.small,21,1.0f,false);
        renderTopText(guiGraphics,TopMessage.medium,37,1.5f,false);
    }
    //? if <1.20.5 {
    /*@Redirect(method = "renderExperienceBar", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I", ordinal = 0))
    public int renderExperienceLevel(LocalPlayer instance, GuiGraphics guiGraphics) {
        if (minecraft.screen != null) return 0;
    *///?} else {
    @Shadow protected abstract boolean isExperienceBarVisible();
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    public void renderExperienceLevel(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ci.cancel();
        if (minecraft.screen != null) return;
    //?}
        int i = this.minecraft.player.experienceLevel;
        if (/*? if >=1.20.5 {*/this.isExperienceBarVisible() && /*?}*/i > 0) {
            //? if >=1.20.5 {
            ScreenUtil.prepareHUDRender(guiGraphics);
            guiGraphics.pose().translate(guiGraphics.guiWidth() /2f, guiGraphics.guiHeight(),0);
            ScreenUtil.applyHUDScale(guiGraphics);
            //?}
            FactoryAPI.getProfiler().push("expLevel");
            String exp = "" + i;
            int hudScale = LegacyOption.hudScale.get();
            boolean is720p = minecraft.getWindow().getHeight() % 720 == 0;
            guiGraphics.pose().translate(0,-36f,0);
            if (!is720p && hudScale != 1) guiGraphics.pose().scale(7/8f,7/8f,7/8f);
            ScreenUtil.drawOutlinedString(guiGraphics,getFont(), Component.literal(exp),-this.getFont().width(exp) / 2,-2,8453920,0,is720p && hudScale == 3 || !is720p && hudScale == 2 || hudScale == 1 ? 1/2f : 2/3f);
            FactoryAPI.getProfiler().pop();
            //? if >1.20.5
            ScreenUtil.finalizeHUDRender(guiGraphics);
        }
        //? if <=1.20.5
        /*return 0;*/
    }
    //? if >=1.20.5 {
    @Inject(method = "renderOverlayMessage", at = @At(value = "HEAD"), cancellable = true)
    public void renderOverlayMessage(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, 72 - LegacyOption.selectedItemTooltipSpacing.get() - ScreenUtil.getHUDSize() - (this.lastToolHighlight.isEmpty() || this.toolHighlightTimer <= 0 || ScreenUtil.getSelectedItemTooltipLines() == 0 ? 0 : (Math.min(ScreenUtil.getSelectedItemTooltipLines() + 1,ScreenUtil.getTooltip(lastToolHighlight).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * LegacyOption.selectedItemTooltipSpacing.get()),0);
    }
    @Inject(method = "renderOverlayMessage", at = @At(value = "RETURN"))
    public void renderOverlayMessageReturn(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (minecraft.screen != null) return;

        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    //?} else if fabric {
    /*@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I", ordinal = 0))
    public int renderOverlayMessage(GuiGraphics guiGraphics, Font font, Component component, int i, int j, int k) {
        if (minecraft.screen != null) return i;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, 63 - ScreenUtil.getHUDSize() - (this.lastToolHighlight.isEmpty() || this.toolHighlightTimer <= 0 ? 0 : (Math.min(4,lastToolHighlight.getTooltipLines(minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * 9),0);
        int r = guiGraphics.drawString(font,component,i,j,k);
        ScreenUtil.finalizeHUDRender(guiGraphics);
        return r;
    }
    @Shadow protected abstract void drawBackdrop(GuiGraphics par1, Font par2, int par3, int par4, int par5);
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;drawBackdrop(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;III)V", ordinal = 0))
    public void renderOverlayMessageReturn(Gui instance, GuiGraphics guiGraphics, Font font, int i, int j, int k) {
        if (minecraft.screen != null) return;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, 63 - ScreenUtil.getHUDSize() - (this.lastToolHighlight.isEmpty() || this.toolHighlightTimer <= 0 ? 0 : (Math.min(4,lastToolHighlight.getTooltipLines(minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * 9),0);
        drawBackdrop(guiGraphics,font,i,j,k);
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    *///?}
    @Inject(method = {"renderVehicleHealth","renderPlayerHealth"}, at = @At("HEAD"), cancellable = true)
    public void renderHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth() / 2, -guiGraphics.guiHeight(),0);
    }
    @Inject(method = {"renderVehicleHealth","renderPlayerHealth"}, at = @At("RETURN"))
    public void renderHealthReturn(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    //? if >=1.20.5 && neoforge {
    /*@Inject(method = {"renderHealthLevel","renderArmorLevel","renderFoodLevel","renderAirLevel"}, at = @At("HEAD"), cancellable = true, remap = false)
    public void renderNeoForgeHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth() / 2, -guiGraphics.guiHeight(),0);
    }
    @Inject(method = {"renderHealthLevel","renderArmorLevel","renderFoodLevel","renderAirLevel"}, at = @At("RETURN"), remap = false)
    public void renderNeoForgeHealthReturn(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    *///?}
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth() / 2, -guiGraphics.guiHeight(),0);
    }
    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    public void renderExperienceBarReturn(GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }
    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    public void renderJumpMeter(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null){
            ci.cancel();
            return;
        }
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
        ScreenUtil.applyHUDScale(guiGraphics);
        guiGraphics.pose().translate(-guiGraphics.guiWidth() / 2, -guiGraphics.guiHeight(),0);
    }
    @Inject(method = "renderJumpMeter", at = @At("RETURN"))
    public void renderJumpMeterReturn(PlayerRideableJumping playerRideableJumping, GuiGraphics guiGraphics, int i, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }

    @Inject(method = "renderSavingIndicator", at = @At("HEAD"), cancellable = true)
    public void renderAutoSaveIndicator(GuiGraphics guiGraphics,/*? if >=1.21 {*/ DeltaTracker deltaTracker,/*?}*/ CallbackInfo ci) {
        ci.cancel();
    }

    @Unique
    public void renderTopText(GuiGraphics guiGraphics, TopMessage topMessage, int height, float scale, boolean shadow){
        if (topMessage != null) {
            RenderSystem.disableDepthTest();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f,height,0);
            guiGraphics.pose().scale(scale,scale,scale);
            guiGraphics.pose().translate(-minecraft.font.width(topMessage.message()) / 2f,0,0);
            guiGraphics.drawString(minecraft.font, topMessage.message(),0,0, topMessage.baseColor(),shadow);
            guiGraphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean tick(ItemStack instance) {
        return !ScreenUtil.getTooltip(instance).equals(ScreenUtil.getTooltip(minecraft.player.getInventory().getSelected()));
    }
    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object tick(OptionInstance<Double> instance) {
        return Math.min(ScreenUtil.getSelectedItemTooltipLines(),ScreenUtil.getTooltip(minecraft.player.getInventory().getSelected()).size()) * instance.get();
    }



    @Inject(method = /*? if forge || neoforge {*/ /*"renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V" *//*?} else {*/"renderSelectedItemName"/*?}*/, at = @At("HEAD"), cancellable = true/*? if forge || neoforge {*//*, remap = false*//*?}*/)
    public void renderSelectedItemName(GuiGraphics guiGraphics, /*? if forge || neoforge {*/ /*int shift, *//*?}*/ CallbackInfo ci) {
        ci.cancel();
        if (minecraft.screen != null || ScreenUtil.getSelectedItemTooltipLines() == 0) return;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, guiGraphics.guiHeight() - Math.max(/*? if forge || neoforge {*/ /*shift *//*?} else {*/0/*?}*/, ScreenUtil.getHUDSize()),0);
        FactoryAPI.getProfiler().push("selectedItemName");
        if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            Font font = /*? if forge || neoforge {*//*Objects.requireNonNullElse(IClientItemExtensions.of(this.lastToolHighlight).getFont(this.lastToolHighlight, IClientItemExtensions.FontContext.SELECTED_ITEM_NAME),getFont())*//*?} else {*/ getFont()/*?}*/;
            List<Component> tooltip = ScreenUtil.getTooltip(lastToolHighlight);
            tooltip.removeIf(c->c.getString().isBlank());
            Object2IntMap<Component> tooltipLines = tooltip.stream().limit(ScreenUtil.getSelectedItemTooltipLines()).map(c-> tooltip.indexOf(c) == ScreenUtil.getSelectedItemTooltipLines() - 1 && LegacyOption.itemTooltipEllipsis.get() ? MORE : c).collect(Collectors.toMap(Function.identity(),font::width,(a,b)->b, Object2IntLinkedOpenHashMap::new));
            int l = Math.min((int)((float)this.toolHighlightTimer * 256.0f / 10.0f),255);
            if (l > 0) {
                int height = LegacyOption.selectedItemTooltipSpacing.get() * (tooltipLines.size() -1);
                guiGraphics.pose().translate(0, -height, 0);
                if (!minecraft.options.backgroundForChatOnly().get()) {
                    int backgroundWidth = tooltipLines.values().intStream().max().orElse(0) + 4;
                    int backgroundX = (guiGraphics.guiWidth() - backgroundWidth) / 2;
                    FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f,l / 255f);
                    ScreenUtil.renderPointerPanel(guiGraphics, backgroundX, -4, backgroundWidth, height + 15);
                    FactoryGuiGraphics.of(guiGraphics).clearColor();
                }
                tooltipLines.forEach((mutableComponent, width) -> {
                    int x = (guiGraphics.guiWidth() - width) / 2;
                    guiGraphics.drawString(this.getFont(), mutableComponent, x, 0, 0xFFFFFF + (l << 24));
                    guiGraphics.pose().translate(0, LegacyOption.selectedItemTooltipSpacing.get(), 0);
                });
            }
        }
        FactoryAPI.getProfiler().pop();
        ScreenUtil.finalizeHUDRender(guiGraphics);
    }

    //? if >=1.20.5 || fabric {
    @Redirect(method=/*? if neoforge {*//*"renderHealthLevel"*//*?} else {*/"renderPlayerHealth"/*?}*/, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;healthBlinkTime:J", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void renderPlayerHealth(Gui instance, long value) {
        healthBlinkTime = value - 6;
    }
    //?}

}

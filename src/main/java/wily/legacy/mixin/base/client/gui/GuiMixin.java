package wily.legacy.mixin.base.client.gui;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.AttackIndicatorStatus;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
//? if >1.20.2 {
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.PlayerScoreEntry;
//?} else {
/*import net.minecraft.world.scores.Score;
*///?}
//? if <1.21.5 {
import com.mojang.blaze3d.platform.GlStateManager;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.util.FactoryGuiElement;
import wily.legacy.client.LegacyOptions;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.ScreenUtil;

import java.util.*;
import java.util.function.Function;


@Mixin(Gui.class)
public abstract class GuiMixin implements ControlTooltip.Event {
    @Shadow @Final
    private Minecraft minecraft;
    //? if >1.20.2 {
    @Final
    @Shadow
    private static Comparator<? super PlayerScoreEntry> SCORE_DISPLAY_ORDER;
    //?}

    @Shadow public abstract Font getFont();

    @Shadow private long healthBlinkTime;


    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getPopTime()I"))
    public int renderSlot(ItemStack instance) {
        return 0;
    }

    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 1))
    public Object renderCrosshair(OptionInstance<AttackIndicatorStatus> instance) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) ? AttackIndicatorStatus.OFF : instance.get();
    }

    @Redirect(method = /*? if >=1.20.5 {*/"renderItemHotbar"/*?} else {*//*"renderHotbar"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    public Object renderItemHotbar(OptionInstance<AttackIndicatorStatus> instance) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) ? AttackIndicatorStatus.OFF : instance.get();
    }

    //? if <1.21.2 {
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"/*? if forge || neoforge {*//*, remap = false*//*?}*/))
    public void renderCrosshairBlendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor, GlStateManager.SourceFactor sourceFactor2, GlStateManager.DestFactor destFactor2) {
        if (LegacyOptions.invertedCrosshair.get()) RenderSystem.blendFuncSeparate(sourceFactor,destFactor,sourceFactor2,destFactor2);
    }
    //?} else {
    /*@ModifyArg(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    public Function<ResourceLocation, RenderType> renderCrosshair(Function<ResourceLocation, RenderType> function) {
        return LegacyOptions.invertedCrosshair.get() ? function : RenderType::guiTextured;
    }
    @ModifyArg(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V"))
    public Function<ResourceLocation, RenderType> renderCrosshairAttackIndicator(Function<ResourceLocation, RenderType> function) {
        return LegacyOptions.invertedCrosshair.get() ? function : RenderType::guiTextured;
    }
    *///?}

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    public void renderEffects(GuiGraphics guiGraphics/*? if >=1.21 {*/, DeltaTracker deltaTracker/*?}*/, CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderGuiEffects(guiGraphics);
    }
    //? if >1.20.1 {
    @Inject(method = /*? if >=1.20.5 {*/"renderItemHotbar"/*?} else {*//*"renderHotbar"*//*?}*/, at = @At(value = "INVOKE", target = /*? if <1.21.2 {*/"Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"/*?} else {*//*"Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"*//*?}*/, ordinal = 1))
    private void renderHotbarSelection(/*? if <1.20.5 {*//*float f, *//*?}*/GuiGraphics guiGraphics/*? if >=1.20.5 {*/, DeltaTracker deltaTracker/*?}*/, CallbackInfo ci) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.HOTBAR_SELECTION,24,24,0,23,guiGraphics.guiWidth() / 2 - 91 - 1 + minecraft.player.getInventory()./*? if <1.21.5 {*/selected/*?} else {*//*getSelectedSlot()*//*?}*/ * 20, guiGraphics.guiHeight(), 0,24, 1);
    }
    //?} else {
    /*@Redirect(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", ordinal = 1))
    private void renderHotbarSelection(GuiGraphics instance, ResourceLocation resourceLocation, int i, int j, int k, int l, int m, int n) {
        instance.blit(resourceLocation, i, j, k, l, m, 24);
    }
    *///?}
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
    //? if <1.20.5 {
    /*@Redirect(method = "renderExperienceBar", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I", ordinal = 0))
    public int renderExperienceLevel(LocalPlayer instance, GuiGraphics guiGraphics) {
    *///?} else {
    @Shadow protected abstract boolean isExperienceBarVisible();

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    public void renderExperienceLevel(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ci.cancel();
        if (!FactoryGuiElement.EXPERIENCE_BAR.isVisible((UIAccessor) this)) return;
    //?}
        int i = this.minecraft.player.experienceLevel;
        if (/*? if >=1.20.5 {*/this.isExperienceBarVisible() && /*?}*/i > 0) {
            //? if >1.20.5
            ScreenUtil.prepareHUDRender(guiGraphics);
            guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight(),0);
            //? if >1.20.5
            ScreenUtil.applyHUDScale(guiGraphics);
            FactoryAPIClient.getProfiler().push("expLevel");
            String exp = "" + i;
            int hudScale = LegacyOptions.hudScale.get();
            boolean is720p = minecraft.getWindow().getHeight() % 720 == 0;
            guiGraphics.pose().translate(0,-36f,0);
            if (!is720p && hudScale != 1) guiGraphics.pose().scale(7/8f,7/8f,7/8f);
            ScreenUtil.drawOutlinedString(guiGraphics,getFont(), Component.literal(exp),-this.getFont().width(exp) / 2,-2,8453920,0,is720p && hudScale == 3 || !is720p && hudScale == 2 || hudScale == 1 ? 1/2f : 2/3f);
            FactoryAPIClient.getProfiler().pop();
            //? if >1.20.5
            ScreenUtil.finalizeHUDRender(guiGraphics);
        }
        //? if <=1.20.5
        /*return 0;*/
    }

    @Inject(method = "renderSavingIndicator", at = @At("HEAD"), cancellable = true)
    public void renderAutoSaveIndicator(GuiGraphics guiGraphics,/*? if >=1.21 {*/ DeltaTracker deltaTracker,/*?}*/ CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean tick(ItemStack instance) {
        return !ScreenUtil.getTooltip(instance).equals(ScreenUtil.getTooltip(minecraft.player.getInventory()./*? if <1.21.5 {*/getSelected()/*?} else {*//*getSelectedItem()*//*?}*/));
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object tick(OptionInstance<Double> instance) {
        return Math.min(ScreenUtil.getSelectedItemTooltipLines(),ScreenUtil.getTooltip(minecraft.player.getInventory()./*? if <1.21.5 {*/getSelected()/*?} else {*//*getSelectedItem()*//*?}*/).size()) * instance.get();
    }

    @Inject(method = /*? if forge || neoforge {*/ /*"renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V" *//*?} else {*/"renderSelectedItemName"/*?}*/, at = @At("HEAD"), cancellable = true/*? if forge || neoforge {*//*, remap = false*//*?}*/)
    public void renderSelectedItemName(GuiGraphics guiGraphics, /*? if forge || neoforge {*/ /*int shift, *//*?}*/ CallbackInfo ci) {
        ci.cancel();
        ScreenUtil.renderHUDTooltip(guiGraphics, /*? if forge || neoforge {*/ /*shift *//*?} else {*/0/*?}*/);
    }

    //? if >=1.20.5 || fabric {
    @Redirect(method=/*? if neoforge {*//*"renderHealthLevel"*//*?} else {*/"renderPlayerHealth"/*?}*/, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;healthBlinkTime:J", opcode = Opcodes.PUTFIELD))
    private void renderPlayerHealth(Gui instance, long value) {
        healthBlinkTime = value - (LegacyOptions.legacyHearts.get() ? 6 : 0);
    }
    //?}

    // For some reason the Gui::renderHeart isn't being a valid target on <1.20.5, certainly a bug
    //? if <1.20.5 {

    //?} else {
    @WrapWithCondition(method = "renderHearts", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHeart(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Gui$HeartType;IIZZZ)V", ordinal = 2))
    private boolean noFlashingHeart(Gui instance, GuiGraphics arg, Gui.HeartType arg2, int i, int j, boolean bl, boolean bl2, boolean bl3) {
        return !LegacyOptions.legacyHearts.get();
    }

    @ModifyArg(method = "renderHearts", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHeart(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Gui$HeartType;IIZZZ)V", ordinal = 3), index = 5)
    private boolean renderRemainingAsFlashing(boolean original, @Local(ordinal = 0, argsOnly = true) boolean flash) {
        return LegacyOptions.legacyHearts.get() ? flash : original;
    }
    //?}

}

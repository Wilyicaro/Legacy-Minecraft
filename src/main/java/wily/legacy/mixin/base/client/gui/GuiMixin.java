package wily.legacy.mixin.base.client.gui;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.AttackIndicatorStatus;
//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
//? if >1.20.2 {
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerScoreEntry;
//?} else {
/*import net.minecraft.world.scores.Score;
 *///?}
//? if <1.21.5 {
/*import com.mojang.blaze3d.platform.GlStateManager;
 *///?}
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
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.client.LegacyGuiItemRenderer;
import wily.legacy.client.LegacyOptions;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.*;


@Mixin(Gui.class)
public abstract class GuiMixin implements ControlTooltip.Event {
    @Final
    @Shadow
    private static Comparator<? super PlayerScoreEntry> SCORE_DISPLAY_ORDER;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private long healthBlinkTime;

    @Shadow
    public abstract Font getFont();

    @Redirect(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getPopTime()I"))
    public int renderSlot(ItemStack instance) {
        return 0;
    }

    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    public Object renderCrosshair(OptionInstance<AttackIndicatorStatus> instance) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) ? AttackIndicatorStatus.OFF : instance.get();
    }

    @Redirect(method = "renderItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    public Object renderItemHotbar(OptionInstance<AttackIndicatorStatus> instance) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) ? AttackIndicatorStatus.OFF : instance.get();
    }

    @ModifyArg(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    public RenderPipeline renderCrosshair(RenderPipeline renderPipeline) {
        return LegacyOptions.invertedCrosshair.get() ? renderPipeline : RenderPipelines.GUI_TEXTURED;
    }

    @ModifyArg(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V"))
    public RenderPipeline renderCrosshairAttackIndicator(RenderPipeline renderPipeline) {
        return LegacyOptions.invertedCrosshair.get() ? renderPipeline : RenderPipelines.GUI_TEXTURED;
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    public void renderEffects(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ci.cancel();
        LegacyRenderUtil.renderGuiEffects(guiGraphics);
    }

    @Inject(method = "renderItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 1))
    private void renderHotbarSelection(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.HOTBAR_SELECTION, 24, 24, 0, 23, guiGraphics.guiWidth() / 2 - 91 - 1 + minecraft.player.getInventory()./*? if <1.21.5 {*//*selected*//*?} else {*/getSelectedSlot()/*?}*/ * 20, guiGraphics.guiHeight(), 0, 24, 1);
    }

    @WrapMethod(method = "renderSlot")
    void renderSlotWithTransparency(GuiGraphics graphics, int i, int j, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int k, Operation<Void> original) {
        LegacyGuiItemRenderer.secureTranslucentRender(true, LegacyRenderUtil.getHUDOpacity(), b -> {
            original.call(graphics, i, j, deltaTracker, player, itemStack, k);
        });
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void displayScoreboardSidebar(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
        ci.cancel();
        if (minecraft.screen != null) return;
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        List<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective).stream().filter((playerScoreEntry) -> !playerScoreEntry.isHidden()).sorted(SCORE_DISPLAY_ORDER).limit(15L).toList();
        Component component = objective.getDisplayName();
        int i = this.getFont().width(component);
        int k = this.getFont().width(": ");
        int j = Math.max(i, scores.stream().mapToInt(lv -> {
            int w = getFont().width(lv.formatValue(numberFormat));
            return this.getFont().width(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(lv.owner()), lv.ownerName())) + (w > 0 ? k + w : 0);
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
        LegacyRenderUtil.renderPointerPanel(guiGraphics, o - 6, s - 16, j + 12, scores.size() * 9 + 22);
        Font var18 = this.getFont();
        int var10003 = o + j / 2 - i / 2;
        Objects.requireNonNull(this.getFont());
        guiGraphics.drawString(var18, component, var10003, s - 9, -1, false);

        for (int t = 0; t < scores.size(); ++t) {
            PlayerScoreEntry lv = scores.get(t);
            x = scores.size() - t;
            Objects.requireNonNull(this.getFont());
            int u = m - x * 9;
            guiGraphics.drawString(this.getFont(), PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(lv.owner()), lv.ownerName()), o, u, -1, false);
            Component score = lv.formatValue(numberFormat);
            guiGraphics.drawString(this.getFont(), score, p - getFont().width(score), u, -1, false);
        }
    }

    @Inject(method = "renderSavingIndicator", at = @At("HEAD"), cancellable = true)
    public void renderAutoSaveIndicator(GuiGraphics guiGraphics,/*? if >=1.21 {*/ DeltaTracker deltaTracker,/*?}*/ CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean tick(ItemStack instance) {
        return !LegacyRenderUtil.getTooltip(instance).equals(LegacyRenderUtil.getTooltip(minecraft.player.getInventory()./*? if <1.21.5 {*//*getSelected()*//*?} else {*/getSelectedItem()/*?}*/));
    }

    @Redirect(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object tick(OptionInstance<Double> instance) {
        return Math.min(LegacyRenderUtil.getSelectedItemTooltipLines(), LegacyRenderUtil.getTooltip(minecraft.player.getInventory()./*? if <1.21.5 {*//*getSelected()*//*?} else {*/getSelectedItem()/*?}*/).size()) * instance.get();
    }

    @Inject(method = /*? if forge || neoforge {*/ /*"renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V" *//*?} else {*/"renderSelectedItemName"/*?}*/, at = @At("HEAD"), cancellable = true/*? if forge || neoforge {*//*, remap = false*//*?}*/)
    public void renderSelectedItemName(GuiGraphics guiGraphics, /*? if forge || neoforge {*/ /*int shift, *//*?}*/ CallbackInfo ci) {
        ci.cancel();
        LegacyRenderUtil.renderHUDTooltip(guiGraphics, /*? if forge || neoforge {*/ /*shift *//*?} else {*/0/*?}*/);
    }

    @Redirect(method = /*? if neoforge {*//*"renderHealthLevel"*//*?} else {*/"renderPlayerHealth"/*?}*/, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;healthBlinkTime:J", opcode = Opcodes.PUTFIELD))
    private void renderPlayerHealth(Gui instance, long value) {
        healthBlinkTime = value - (LegacyOptions.legacyHearts.get() ? 6 : 0);
    }

    @WrapWithCondition(method = "renderHearts", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHeart(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Gui$HeartType;IIZZZ)V", ordinal = 2))
    private boolean noFlashingHeart(Gui instance, GuiGraphics arg, Gui.HeartType arg2, int i, int j, boolean bl, boolean bl2, boolean bl3) {
        return !LegacyOptions.legacyHearts.get();
    }

    @ModifyArg(method = "renderHearts", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHeart(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Gui$HeartType;IIZZZ)V", ordinal = 3), index = 5)
    private boolean renderRemainingAsFlashing(boolean original, @Local(ordinal = 0, argsOnly = true) boolean flash) {
        return LegacyOptions.legacyHearts.get() ? flash : original;
    }
}

package wily.legacy.neoforge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

import java.util.List;

import static wily.legacy.client.screen.ControlTooltip.MORE;

@Mixin(Gui.class)

public abstract class GuiMixin {
    @Shadow @Final protected Minecraft minecraft;


    @Shadow public abstract Font getFont();


    @Shadow protected int toolHighlightTimer;

    @Shadow protected ItemStack lastToolHighlight;

    @Shadow private long healthBlinkTime;

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    public void renderSelectedItemName(GuiGraphics guiGraphics, int shift, CallbackInfo ci) {
        ci.cancel();
        if (minecraft.screen != null) return;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, -Math.max(shift, ScreenUtil.getHUDSize()),0);
        this.minecraft.getProfiler().push("selectedItemName");
        if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            List<Component> tooltipLines = this.lastToolHighlight.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).toList();
            for (int i = 0; i < tooltipLines.size(); i++) {
                int l;
                Component mutableComponent = i >= 4 ? MORE : tooltipLines.get(i);
                int width = this.getFont().width(mutableComponent);
                int j = (guiGraphics.guiWidth() - width) / 2;
                int k = guiGraphics.guiHeight() - getFont().lineHeight * (Math.min(4,tooltipLines.size()) - 1 - i);
                if ((l = (int)((float)this.toolHighlightTimer * 256.0f / 10.0f)) > 255) {
                    l = 255;
                }
                if (l > 0) {
                    guiGraphics.fill(j - 2, k - 2, j + width + 2, k + this.getFont().lineHeight + 2, this.minecraft.options.getBackgroundColor(0));
                    Font font = IClientItemExtensions.of(this.lastToolHighlight).getFont(this.lastToolHighlight, IClientItemExtensions.FontContext.SELECTED_ITEM_NAME);
                    if (font == null) {
                        guiGraphics.drawString(this.getFont(), mutableComponent, j, k, 0xFFFFFF + (l << 24));
                    } else {
                        j = (guiGraphics.guiWidth() - font.width(mutableComponent)) / 2;
                        guiGraphics.drawString(font, mutableComponent, j, k, 16777215 + (l << 24));
                    }
                }
                if (i >= 4) break;
            }
        }
        this.minecraft.getProfiler().pop();
        ScreenUtil.finishHUDRender(guiGraphics);
    }
    @Inject(method = {"renderHealthLevel","renderArmorLevel","renderFoodLevel","renderAirLevel"}, at = @At("HEAD"), cancellable = true)
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
    @Inject(method = {"renderHealthLevel","renderArmorLevel","renderFoodLevel","renderAirLevel"}, at = @At("RETURN"))
    public void renderHealthReturn(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (minecraft.screen != null) return;
        ScreenUtil.finishHUDRender(guiGraphics);
    }
    @Redirect(method="renderHealthLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;healthBlinkTime:J", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void renderPlayerHealth(Gui instance, long value) {
        healthBlinkTime = value - 5;
    }
}

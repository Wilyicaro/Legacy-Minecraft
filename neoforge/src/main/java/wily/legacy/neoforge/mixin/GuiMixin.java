package wily.legacy.neoforge.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

import java.util.List;

import static wily.legacy.client.screen.ControlTooltip.MORE;

@Mixin(Gui.class)

public abstract class GuiMixin {
    @Shadow @Final protected Minecraft minecraft;

    @Shadow protected int screenHeight;

    @Shadow public abstract Font getFont();

    @Shadow protected int screenWidth;

    @Shadow protected int toolHighlightTimer;

    @Shadow protected ItemStack lastToolHighlight;

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    public void renderSelectedItemName(GuiGraphics guiGraphics, int shift, CallbackInfo ci) {
        ci.cancel();
        if (minecraft.screen != null) return;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0,ScreenUtil.getHUDDistance() - Math.max(shift, ScreenUtil.getHUDSize()),0);
        this.minecraft.getProfiler().push("selectedItemName");
        if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            List<Component> tooltipLines = this.lastToolHighlight.getTooltipLines(minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).toList();
            for (int i = 0; i < tooltipLines.size(); i++) {
                int l;
                Component mutableComponent = i >= 4 ? MORE : tooltipLines.get(i);
                int width = this.getFont().width(mutableComponent);
                int j = (this.screenWidth - width) / 2;
                int k = this.screenHeight - getFont().lineHeight * (Math.min(4,tooltipLines.size()) - 1 - i);
                if ((l = (int)((float)this.toolHighlightTimer * 256.0f / 10.0f)) > 255) {
                    l = 255;
                }
                if (l > 0) {
                    guiGraphics.fill(j - 2, k - 2, j + width + 2, k + this.getFont().lineHeight + 2, this.minecraft.options.getBackgroundColor(0));
                    Font font = IClientItemExtensions.of(this.lastToolHighlight).getFont(this.lastToolHighlight, IClientItemExtensions.FontContext.SELECTED_ITEM_NAME);
                    if (font == null) {
                        guiGraphics.drawString(this.getFont(), mutableComponent, j, k, 0xFFFFFF + (l << 24));
                    } else {
                        j = (this.screenWidth - font.width(mutableComponent)) / 2;
                        guiGraphics.drawString(font, mutableComponent, j, k, 16777215 + (l << 24));
                    }
                }
                if (i >= 4) break;
            }
        }
        this.minecraft.getProfiler().pop();
        guiGraphics.pose().popPose();
    }

}

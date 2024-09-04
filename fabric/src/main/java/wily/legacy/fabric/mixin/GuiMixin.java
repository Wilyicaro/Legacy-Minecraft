package wily.legacy.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

    @Shadow
    private ItemStack lastToolHighlight;

    @Shadow protected abstract void drawBackdrop(GuiGraphics guiGraphics, Font font, int i, int j, int k);

    @Shadow protected long healthBlinkTime;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I", ordinal = 0))
    public int renderOverlayMessage(GuiGraphics guiGraphics, Font font, Component component, int i, int j, int k) {
        if (minecraft.screen != null) return i;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, 63 - ScreenUtil.getHUDSize() - (this.lastToolHighlight.isEmpty() || this.toolHighlightTimer <= 0 ? 0 : (Math.min(4,lastToolHighlight.getTooltipLines(minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * 9),0);
        int r = guiGraphics.drawString(font,component,i,j,k);
        ScreenUtil.finishHUDRender(guiGraphics);
        return r;
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;drawBackdrop(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;III)V", ordinal = 0))
    public void renderOverlayMessageReturn(Gui instance, GuiGraphics guiGraphics, Font font, int i, int j, int k) {
        if (minecraft.screen != null) return;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, 63 - ScreenUtil.getHUDSize() - (this.lastToolHighlight.isEmpty() || this.toolHighlightTimer <= 0 ? 0 : (Math.min(4,lastToolHighlight.getTooltipLines(minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * 9),0);
        drawBackdrop(guiGraphics,font,i,j,k);
        ScreenUtil.finishHUDRender(guiGraphics);
    }
    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    public void renderSelectedItemName(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
        if (minecraft.screen != null) return;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, -ScreenUtil.getHUDSize(),0);
        this.minecraft.getProfiler().push("selectedItemName");
        if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            List<Component> tooltipLines = this.lastToolHighlight.getTooltipLines(minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).toList();
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
                    guiGraphics.drawString(this.getFont(), mutableComponent, j, k, 0xFFFFFF + (l << 24));
                }
                if (i >= 4) break;
            }
        }
        this.minecraft.getProfiler().pop();
        ScreenUtil.finishHUDRender(guiGraphics);
    }

    @Redirect(method="renderPlayerHealth", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;healthBlinkTime:J", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void renderPlayerHealth(Gui instance, long value) {
        healthBlinkTime = value - 5;
    }
}

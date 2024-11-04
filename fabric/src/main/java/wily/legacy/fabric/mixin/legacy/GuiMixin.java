package wily.legacy.fabric.mixin.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
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
import wily.legacy.client.LegacyOption;
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


    @Shadow private long healthBlinkTime;

    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    public void renderSelectedItemName(GuiGraphics guiGraphics, CallbackInfo ci) {
        ci.cancel();
        if (minecraft.screen != null || ScreenUtil.getSelectedItemTooltipLines() == 0) return;
        ScreenUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(0, -ScreenUtil.getHUDSize(),0);
        this.minecraft.getProfiler().push("selectedItemName");
        if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty() && ScreenUtil.getSelectedItemTooltipLines() > 0) {
            List<Component> tooltipLines = this.lastToolHighlight.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player, TooltipFlag.NORMAL).stream().filter(c->!c.getString().isEmpty()).toList();
            for (int i = 0; i < tooltipLines.size(); i++) {
                int l;
                Component mutableComponent = i >= ScreenUtil.getSelectedItemTooltipLines() - 1 && LegacyOption.itemTooltipEllipsis.get() ? MORE : tooltipLines.get(i);
                int width = this.getFont().width(mutableComponent);
                int j = (guiGraphics.guiWidth() - width) / 2;
                int k = guiGraphics.guiHeight() - LegacyOption.selectedItemTooltipSpacing.get() * (Math.min(ScreenUtil.getSelectedItemTooltipLines(),tooltipLines.size()) - 1 - i);
                if ((l = (int)((float)this.toolHighlightTimer * 256.0f / 10.0f)) > 255) {
                    l = 255;
                }
                if (l > 0) {
                    guiGraphics.fill(j - 2, k - 2, j + width + 2, k + LegacyOption.selectedItemTooltipSpacing.get() + 2, this.minecraft.options.getBackgroundColor(0));
                    guiGraphics.drawString(this.getFont(), mutableComponent, j, k, 0xFFFFFF + (l << 24));
                }
                if (i >= ScreenUtil.getSelectedItemTooltipLines() - 1) break;
            }
        }
        this.minecraft.getProfiler().pop();
        ScreenUtil.finishHUDRender(guiGraphics);
    }

    @Redirect(method="renderPlayerHealth", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;healthBlinkTime:J", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void renderPlayerHealth(Gui instance, long value) {
        healthBlinkTime = value - 6;
    }
}

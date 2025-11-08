package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOptions;

@Mixin(ClientTextTooltip.class)

public class ClientTextTooltipMixin {
    @Shadow
    @Final
    private FormattedCharSequence text;

    @Inject(method = "renderText", at = @At("HEAD"), cancellable = true)
    public void renderText(GuiGraphics guiGraphics, Font font, int i, int j, CallbackInfo ci) {
        if (!LegacyOptions.legacyItemTooltips.get()) return;
        ci.cancel();
        guiGraphics.drawString(font, this.text, i, j, -1, false);
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    public void getHeight(CallbackInfoReturnable<Integer> cir) {
        if (LegacyOptions.legacyItemTooltips.get()) cir.setReturnValue(LegacyOptions.getUIMode().isSD() ? 9 : 12);
    }
}

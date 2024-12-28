package wily.legacy.mixin.base;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.client.LegacyOption;

@Mixin(ClientTextTooltip.class)

public class ClientTextTooltipMixin {
    @Shadow @Final private FormattedCharSequence text;

    @Inject(method = "renderText", at = @At("HEAD"), cancellable = true)
    public void renderText(Font font, int i, int j, Matrix4f matrix4f, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        if (!LegacyOption.legacyItemTooltips.get()) return;
        ci.cancel();
        font.drawInBatch(this.text, (float)i, (float)j, -1, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }
    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    public void getHeight(CallbackInfoReturnable<Integer> cir) {
        if (LegacyOption.legacyItemTooltips.get()) cir.setReturnValue(12);
    }
}

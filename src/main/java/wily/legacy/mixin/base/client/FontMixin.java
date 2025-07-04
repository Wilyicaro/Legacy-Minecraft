package wily.legacy.mixin.base.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonValue;

@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public class FontMixin {
    @ModifyArg(method = "getShadowColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;scaleRGB(IF)I", ordinal = 0))
    private float getShadowDim(float dim) {
        return !Legacy4JClient.legacyFont || Legacy4JClient.forceVanillaFontShadowColor ? 0.25f : CommonValue.LEGACY_FONT_DIM_FACTOR.get();
    }
}

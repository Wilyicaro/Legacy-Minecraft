//? if <=1.20.4 {
/*package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.blaze3d.font.TrueTypeGlyphProvider$Glyph")
public class TrueTypeGlyphBearingMixin {
    @Shadow
    @Final
    @Mutable
    private float bearingX;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void correctBearing(TrueTypeGlyphProvider provider, int x1, int x2, int y1, int y2, float advance, float bearingX, int glyphIndex, CallbackInfo ci) {
        this.bearingX = (float) x1 / ((TrueTypeGlyphProviderAccessor) provider).l4j$getOversample();
    }
}
*///?}

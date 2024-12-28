package wily.legacy.mixin.base;

import net.minecraft.client.gui.Font;
import org.objectweb.asm.Opcodes;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonValue;

@Mixin(Font.StringRenderOutput.class)
public class FontMixin {

    //? if <1.21.4 {
    /*@Mutable
    @Shadow @Final private float dimFactor;
    @Shadow @Final private boolean dropShadow;

    @Redirect(method = /^? if <1.21.2 {^//^"<init>"^//^?} else {^/ "<init>(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/MultiBufferSource;FFIIZLorg/joml/Matrix4f;Lnet/minecraft/client/gui/Font$DisplayMode;IZ)V"/^?}^/, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Font$StringRenderOutput;dimFactor:F", opcode = Opcodes.PUTFIELD))
    private void init(Font.StringRenderOutput instance, float value){
        dimFactor = dropShadow ? Legacy4JClient.legacyFont && !Legacy4JClient.forceVanillaFontShadowColor ? CommonValue.LEGACY_FONT_DIM_FACTOR.get() : 0.25f : 1.0f;
    }
    *///?} else {
    @ModifyArg(method = "getShadowColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;scaleRGB(IF)I", ordinal = 0))
    private float getShadowDim(float dim) {
        return !Legacy4JClient.legacyFont || Legacy4JClient.forceVanillaFontShadowColor ? 0.25f : CommonValue.LEGACY_FONT_DIM_FACTOR.get();
    }
    //?}
}

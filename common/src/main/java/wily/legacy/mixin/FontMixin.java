package wily.legacy.mixin;

import net.minecraft.client.gui.Font;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.Legacy4JClient;

@Mixin(Font.StringRenderOutput.class)
public class FontMixin {
    @Mutable
    @Shadow @Final private float dimFactor;
    @Shadow @Final private boolean dropShadow;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Font$StringRenderOutput;dimFactor:F", opcode = Opcodes.PUTFIELD))
    private void init(Font.StringRenderOutput instance, float value){
        dimFactor = dropShadow ? Legacy4JClient.legacyFont && !Legacy4JClient.forceVanillaFontShadowColor ? 0f : 0.25f : 1.0f;
    }
}

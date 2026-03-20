//? if >=1.21.11 {
/*package wily.legacy.mixin.base.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.client.CommonValue;

@Mixin(targets = "net.minecraft.client.gui.GuiGraphics$RenderingTextCollector")
public class RenderingTextCollectorMixin {
    @ModifyArg(method = "accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiTextRenderState;<init>(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;Lorg/joml/Matrix3x2fc;IIIIZZLnet/minecraft/client/gui/navigation/ScreenRectangle;)V"), index = 7)
    private boolean textShadow(boolean shadow) {
        return CommonValue.WIDGET_TEXT_SHADOW.get();
    }
}
*///?}

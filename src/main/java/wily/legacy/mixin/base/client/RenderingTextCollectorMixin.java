//? if >=1.21.11 {
package wily.legacy.mixin.base.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.CommonValue;

@Mixin(value = GuiGraphics.RenderingTextCollector.class)
public abstract class RenderingTextCollectorMixin {
    @ModifyArg(method = "accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/state/GuiTextRenderState;<init>(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;Lorg/joml/Matrix3x2fc;IIIIZZLnet/minecraft/client/gui/navigation/ScreenRectangle;)V"), index = 7)
    private boolean textShadow(boolean shadow) {
        return CommonValue.WIDGET_TEXT_SHADOW.get();
    }

    @Redirect(method = "acceptScrolling", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics$RenderingTextCollector;defaultScrollingHelper(Lnet/minecraft/network/chat/Component;IIIIIIILnet/minecraft/client/gui/ActiveTextCollector$Parameters;)V"))
    private void defaultScrollingHelper(GuiGraphics.RenderingTextCollector instance, Component component, int i, int j, int k, int l, int m, int n, int o, ActiveTextCollector.Parameters parameters) {
        int p = (l + m - o) / 2 + 1;
        int q = k - j;
        if (n > q) {
            int r = n - q;
            double d = (double) Util.getMillis() / (double)1000.0F;
            double e = Math.max((double)r * (double)0.5F, 3.0F);
            double f = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * d / e)) / (double)2.0F + (double)0.5F;
            double g = Mth.lerp(f, 0.0F, r);
            ActiveTextCollector.Parameters parameters2 = parameters.withScissor(j, k, l, m);
            instance.accept(TextAlignment.LEFT, j - (int)g, p, parameters2, component.getVisualOrderText());
        } else {
            int r = Mth.clamp(i, j + n / 2, k - n / 2);
            instance.accept(TextAlignment.LEFT, r, p - n / 2, component);
        }
    }
}
//?}

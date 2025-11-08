package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.GuiGraphics;
//? if <1.21.4 {
/*import net.minecraft.client.gui.components.AbstractScrollWidget;
 *///?} else {
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.util.LegacySprites;

@Mixin(/*? if <1.21.4 {*//*AbstractScrollWidget*//*?} else {*/AbstractTextAreaWidget/*?}*/.class)
public abstract class AbstractScrollWidgetMixin extends AbstractWidget {

    public AbstractScrollWidgetMixin(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    @Inject(method = "renderBorder", at = @At("HEAD"), cancellable = true)
    private void renderBorder(GuiGraphics guiGraphics, int i, int j, int k, int l, CallbackInfo ci) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.TEXT_FIELD, i, j, k, l);
        if (isHoveredOrFocused())
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.HIGHLIGHTED_TEXT_FIELD, i - 1, j - 1, k + 2, l + 2);
        ci.cancel();
    }
}

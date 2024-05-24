package wily.legacy.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.util.ScreenUtil;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {

    @Shadow protected boolean isHovered;


    private boolean alreadyHovered = false;
    @Inject(method = "render",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractWidget;renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void render(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci){
        if (isHovered) {
            if (!alreadyHovered){
                ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(),1.0f);
                alreadyHovered = true;
            }
        }else alreadyHovered = false;
    }
    @Redirect(method = "nextFocusPath", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractWidget;active:Z"))
    public boolean nextFocusPath(AbstractWidget instance) {
        return true;
    }
}

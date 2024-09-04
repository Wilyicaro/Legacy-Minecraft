package wily.legacy.mixin;

import net.minecraft.Util;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Unique
    long lastTimePressed;

    @Redirect(method = "nextFocusPath", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/components/AbstractWidget;active:Z"))
    public boolean nextFocusPath(AbstractWidget instance) {
        return true;
    }

    @Inject(method = "setFocused", at = @At("HEAD"))
    private void setFocused(boolean bl, CallbackInfo ci){
        if (bl) lastTimePressed = Util.getMillis();
    }

}

package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.ComponentPath;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.NavigationElement;

@Mixin(ComponentPath.Path.class)
public class ComponentPathMixin {

    @Shadow @Final private ComponentPath childPath;

    @Inject(method = "applyFocus", at = @At("HEAD"), cancellable = true)
    private void applyFocus(boolean bl, CallbackInfo ci) {
        NavigationElement.of(childPath.component()).applyFocus((ComponentPath.Path) (Object) this, bl);
        ci.cancel();
    }
}

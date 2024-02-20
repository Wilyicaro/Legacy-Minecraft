package wily.legacy.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {
    @Shadow private int framebufferWidth;

    @Shadow private int framebufferHeight;
    @Inject(method = "calculateScale", at = @At("HEAD"), cancellable = true)
    public void calculateScale(int i, boolean bl, CallbackInfoReturnable<Integer> cir) {
        int j;
        for (j = 1; j != i && j < this.framebufferWidth && j < this.framebufferHeight && this.framebufferWidth / (j + 1) >= 160 && this.framebufferHeight / (j + 1) >= 120; ++j) {
        }
        if (bl && j % 2 != 0) {
            ++j;
        }
        cir.setReturnValue(j);
    }

}

package wily.legacy.mixin;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.util.ScreenUtil;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow public int width;

    @Shadow public int height;

    @Inject(method = "changeFocus",at = @At("HEAD"))
    private void render(ComponentPath componentPath, CallbackInfo ci){
        ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(),1.0f);
    }
    @Inject(method = "onClose",at = @At("HEAD"))
    private void onClose(CallbackInfo ci){
        ScreenUtil.playSimpleUISound(LegacySoundEvents.BACK.get(),1.0f);
    }
    @Inject(method = "renderTransparentBackground",at = @At("HEAD"), cancellable = true)
    public void renderTransparentBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, -1073741824, -805306368);
        ci.cancel();
    }
}

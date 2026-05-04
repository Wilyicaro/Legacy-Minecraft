package wily.legacy.mixin.base;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.ScreenUtil;

@Mixin(Window.class)
public class WindowMixin {
    @ModifyVariable(method = "setGuiScale", at = @At("HEAD"), argsOnly = true)
    public double setGuiScale(double d) {
        return Minecraft.getInstance().options.guiScale().get() == 0 ? Math.round(ScreenUtil.getAutoGuiScale()) : d;
    }

    @Inject(method = "calculateScale", at = @At("HEAD"), cancellable = true)
    public void calculateScale(int i, boolean bl, CallbackInfoReturnable<Integer> cir) {
        if (Minecraft.getInstance().options.guiScale().get() == 0) cir.setReturnValue(Math.round(ScreenUtil.getAutoGuiScale()));
    }
}

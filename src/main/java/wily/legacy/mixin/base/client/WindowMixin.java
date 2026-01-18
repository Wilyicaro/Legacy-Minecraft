package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(Window.class)
public class WindowMixin {
    @ModifyVariable(method = "setGuiScale", at = @At("HEAD"), argsOnly = true)
    public int setGuiScale(int original) {
        return Minecraft.getInstance().options.guiScale().get() == 0 ? Math.round(LegacyRenderUtil.getAutoGuiScale()) : original;
    }

    @Inject(method = "calculateScale", at = @At("HEAD"), cancellable = true)
    public void calculateScale(int i, boolean bl, CallbackInfoReturnable<Integer> cir) {
        if (Minecraft.getInstance().options.guiScale().get() == 0) cir.setReturnValue(Math.round(LegacyRenderUtil.getAutoGuiScale()));
    }
}

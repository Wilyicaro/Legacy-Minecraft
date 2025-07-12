package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(Window.class)
public class WindowMixin {
    @ModifyVariable(method = "setGuiScale", at = @At("HEAD"), argsOnly = true)
    public int setGuiScale(int d) {
        return (int) Math.round(LegacyRenderUtil.getGuiScale());
    }
}

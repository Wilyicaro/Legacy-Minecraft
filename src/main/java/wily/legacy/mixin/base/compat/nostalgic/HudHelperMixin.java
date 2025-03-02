//? if (>=1.21 && (fabric || neoforge)) || (1.20.1 && (forge || fabric)) {
package wily.legacy.mixin.base.compat.nostalgic;

import mod.adrenix.nostalgic.helper.candy.hud.HudHelper;
import mod.adrenix.nostalgic.util.client.gui.GuiUtil;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.ScreenUtil;

@Mixin(HudHelper.class)
public class HudHelperMixin {
    @ModifyArg(method = "apply", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1)
    private static float apply(float f) {
        return f * 3 / ScreenUtil.getHUDScale();
    }

    //? if forge || neoforge {
    /*@Inject(method = "renderArmor", at = @At("HEAD"), remap = false)
    private static void renderArmor(GuiGraphics graphics, int offsetHeight, CallbackInfo ci) {
        float offsetWidth = (GuiUtil.getGuiWidth() / 2f + 90);
        graphics.pose().translate( offsetWidth - offsetWidth * ScreenUtil.getHUDScale() / 3, 0.0F, 0.0F);
    }
    @Inject(method = "renderAir", at = @At("HEAD"), remap = false)
    private static void renderAir(GuiGraphics graphics, int offsetHeight, CallbackInfo ci) {
        float offsetWidth = (GuiUtil.getGuiWidth() / 2f - 100);
        graphics.pose().translate( offsetWidth - offsetWidth * ScreenUtil.getHUDScale() / 3, 0.0F, 0.0F);
    }
    *///?}

}
//?}

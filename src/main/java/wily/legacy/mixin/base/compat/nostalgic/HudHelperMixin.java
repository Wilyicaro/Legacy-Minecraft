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
import com.llamalad7.mixinextras.sugar.Local;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(HudHelper.class)
public class HudHelperMixin {
    @ModifyArg(method = "apply", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1)
    private static float apply(float f) {
        return f * LegacyRenderUtil.getHUDScale();
    }

    //? if forge || neoforge {
    /*@Inject(method = "renderArmor", at = @At("HEAD"), remap = false)
    private static void renderArmor(CallbackInfo ci, @Local GuiGraphics graphics) {
        float offsetWidth = (GuiUtil.getGuiWidth() / 2f + 90);
        graphics.pose().translate(offsetWidth - offsetWidth * LegacyRenderUtil.getHUDScale() / 3, 0.0F);
    }
    @Inject(method = "renderAir", at = @At("HEAD"), remap = false)
    private static void renderAir(CallbackInfo ci, @Local GuiGraphics graphics) {
        float offsetWidth = (GuiUtil.getGuiWidth() / 2f - 100);
        graphics.pose().translate(offsetWidth - offsetWidth * LegacyRenderUtil.getHUDScale() / 3, 0.0F);
    }
    *///?}

}
//?}

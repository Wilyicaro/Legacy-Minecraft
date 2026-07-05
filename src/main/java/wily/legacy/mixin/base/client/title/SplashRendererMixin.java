package wily.legacy.mixin.base.client.title;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.ScreenUtil;

@Mixin(SplashRenderer.class)
public class SplashRendererMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 0)
    public float renderTranslateX(float x) {
        return LegacyOptions.getUIMode().isSD() ? x - 45 : x;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1)
    public float renderTranslateY(float y) {
        float offset = LegacyOptions.getUIMode().isSD() ? -30 : 0;
        if (ScreenUtil.hasMinecraftLogoResource()) offset += 8;
        return y + offset;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), index = 0)
    public float renderScaleX(float scale) {
        return scale * (LegacyOptions.getUIMode().isSD() ? 0.75f : 1.5f);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"), index = 1)
    public float renderScaleY(float scale) {
        return scale * (LegacyOptions.getUIMode().isSD() ? 0.75f : 1.5f);
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void render(GuiGraphics guiGraphics, int i, Font font, int j, CallbackInfo ci) {
        Legacy4JClient.legacyFont = false;
    }
    @Inject(method = "render", at = @At("RETURN"))
    public void renderReturn(GuiGraphics guiGraphics, int i, Font font, int j, CallbackInfo ci) {
        Legacy4JClient.legacyFont = true;
    }
}

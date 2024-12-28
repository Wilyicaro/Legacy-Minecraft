package wily.legacy.mixin.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.util.ScreenUtil;

@Mixin(SplashRenderer.class)
public class SplashRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", shift = At.Shift.AFTER))
    public void renderAfterScale(GuiGraphics guiGraphics, int i, Font font, int j, CallbackInfo ci) {
        guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
        if (Minecraft.getInstance().getResourceManager().getResource(ScreenUtil.MINECRAFT).isPresent()) guiGraphics.pose().translate(0,8,0);
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

package wily.legacy.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.LegacyOptions;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastComponent;render(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void render(ToastComponent instance, GuiGraphics graphics){
        instance.render(graphics);
        if (!((LegacyOptions)minecraft.options).legacyGamma().get()) return;
        float gamma = minecraft.options.gamma().get().floatValue();
        if (gamma != 0.5) {
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 4400f);
            float fixedGamma;
            if (gamma> 0.5) {
                fixedGamma = (gamma - 0.5f) / 4f;
                RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ONE);
            }else {
                fixedGamma = 1 - (0.5f- gamma);
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.SRC_COLOR);
            }
            RenderSystem.setShaderColor(fixedGamma, fixedGamma, fixedGamma, 1.0f);
            graphics.blit(new ResourceLocation(LegacyMinecraft.MOD_ID, "textures/gui/gamma.png"), 0, 0, 0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (gamma > 0.5f ? gamma - 0.5f : 0.5f - gamma) / 4f);
            RenderSystem.defaultBlendFunc();
            graphics.blit(new ResourceLocation(LegacyMinecraft.MOD_ID, "textures/gui/gamma.png"), 0, 0, 0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
            graphics.pose().popPose();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }
}

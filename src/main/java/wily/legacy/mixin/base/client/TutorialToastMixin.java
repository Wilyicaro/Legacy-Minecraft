package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.function.Function;

@Mixin(TutorialToast.class)
public abstract class TutorialToastMixin implements Toast {
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    public void render(GuiGraphics instance, RenderPipeline renderPipeline, ResourceLocation resourceLocation, int i, int j, int k, int l) {
        LegacyRenderUtil.renderPointerPanel(instance,0,0,width(),height());
    }
}

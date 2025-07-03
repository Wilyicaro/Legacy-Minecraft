package wily.legacy.mixin.base.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(ContextualBarRenderer.class)
public class ExperienceBarRendererMixin {

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private static void renderExperienceLevel(GuiGraphics guiGraphics, Font font, int i, CallbackInfo ci) {
        ci.cancel();

        LegacyRenderUtil.prepareHUDRender(guiGraphics);
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight());
        LegacyRenderUtil.applyHUDScale(guiGraphics);
        String exp = "" + i;
        int hudScale = LegacyOptions.hudScale.get();
        boolean is720p = Minecraft.getInstance().getWindow().getHeight() % 720 == 0;
        guiGraphics.pose().translate(0,-36f);
        if (!is720p && hudScale != 1) guiGraphics.pose().scale(7/8f,7/8f);
        LegacyRenderUtil.drawOutlinedString(guiGraphics, font, Component.literal(exp),-font.width(exp) / 2,-2,8453920,0,is720p && hudScale == 3 || !is720p && hudScale == 2 || hudScale == 1 ? 1/2f : 2/3f);
        FactoryAPIClient.getProfiler().pop();
        LegacyRenderUtil.finalizeHUDRender(guiGraphics);
    }
}

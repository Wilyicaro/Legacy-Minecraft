package wily.legacy.mixin.base.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.client.LegacyRenderUtil;

@Mixin(Gui.class)
public class ExperienceBarRendererMixin {

    @Redirect(method = /*? if forge || <26.1 {*/"extractHotbarAndDecorations"/*?} else {*/ /*"extractExperienceLevel" *//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    private void renderExperienceLevel(GuiGraphicsExtractor GuiGraphicsExtractor, Font font, int i) {
        if (!LegacyRenderUtil.canDisplayHUD()) return;

        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(GuiGraphicsExtractor.guiWidth() / 2f, GuiGraphicsExtractor.guiHeight());
        String exp = String.valueOf(i);
        int hudScale = LegacyOptions.hudSize.get();
        boolean hd = LegacyOptions.getUIMode().isHDOrLower();
        GuiGraphicsExtractor.pose().translate(0, -36f);
        if (!hd && hudScale != 1) GuiGraphicsExtractor.pose().scale(7 / 8f, 7 / 8f);
        LegacyRenderUtil.drawOutlinedString(GuiGraphicsExtractor, font, Language.getInstance().getVisualOrder(FormattedText.of(exp)), -font.width(exp) / 2, -2, ColorUtil.withAlpha(CommonColor.EXPERIENCE_TEXT.get(), LegacyRenderUtil.getHUDOpacity()), ColorUtil.withAlpha(0xFF000000, LegacyRenderUtil.getHUDOpacity()), hd && hudScale == 3 || !hd && hudScale == 2 || hudScale == 1 ? 0.51f : 2 / 3f);
        GuiGraphicsExtractor.pose().popMatrix();
    }
}

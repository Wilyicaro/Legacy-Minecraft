package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.util.Mth;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

public class RGBPreviewWidget extends AbstractButton {
    public RGBPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height, LegacyComponents.CONTROLLER_LED_PREVIEW);
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.RGB_PREVIEW, getX(), getY(), getWidth(), getHeight());
        GuiGraphicsExtractor.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2, ColorUtil.colorFromInt(LegacyOptions.controllerLedRed.get(), LegacyOptions.controllerLedGreen.get(), LegacyOptions.controllerLedBlue.get(), 255));
        int k = LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused());
        LegacyRenderUtil.renderScrollingString(GuiGraphicsExtractor, Minecraft.getInstance().font, getMessage(), getX() + 2, getY(), getX() + getWidth() - 2, getY() + getHeight(), k | Mth.ceil(this.alpha * 255.0f) << 24, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}

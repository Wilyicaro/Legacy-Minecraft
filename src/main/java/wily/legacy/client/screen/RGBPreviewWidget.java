package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.control.LegacyControlsOptions;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;

public class RGBPreviewWidget extends AbstractButton {
    public RGBPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height, LegacyComponents.CONTROLLER_LED_PREVIEW);
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int i, int j, float f) {
        FactoryGuiGraphics.of(graphics).blitSprite(LegacySprites.RGB_PREVIEW, getX(), getY(), getWidth(), getHeight());
        graphics.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2, ColorUtil.colorFromInt(LegacyControlsOptions.controllerLedRed.get(), LegacyControlsOptions.controllerLedGreen.get(), LegacyControlsOptions.controllerLedBlue.get(), 255));
        extractDefaultLabel(graphics.textRenderer());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}

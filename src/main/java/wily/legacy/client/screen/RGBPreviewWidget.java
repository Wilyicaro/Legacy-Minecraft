package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.LegacyOptions;
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
    protected void renderContents(GuiGraphics guiGraphics, int i, int j, float f) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.RGB_PREVIEW, getX(), getY(), getWidth(), getHeight());
        guiGraphics.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2, ColorUtil.colorFromInt(LegacyOptions.controllerLedRed.get(), LegacyOptions.controllerLedGreen.get(), LegacyOptions.controllerLedBlue.get(), 255));
        renderDefaultLabel(guiGraphics.textRenderer());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}

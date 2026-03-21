package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.util.Mth;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.ColorUtil;
import wily.legacy.client.LegacyOptions;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

public class RGBPreviewWidget extends AbstractWidget {
    public RGBPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height, LegacyComponents.CONTROLLER_LED_PREVIEW);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.RGB_PREVIEW, getX(), getY(), getWidth(), getHeight());
        guiGraphics.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2, ColorUtil.colorFromInt(LegacyOptions.controllerLedRed.get(), LegacyOptions.controllerLedGreen.get(), LegacyOptions.controllerLedBlue.get(), 255));
        int k = LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused());
        //? if <1.21.11 {
        this.renderScrollingString(guiGraphics, minecraft.font, 2, k | Mth.ceil(this.alpha * 255.0f) << 24);
         //?} else {
        /*this.renderScrollingStringOverContents(guiGraphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE), this.getMessage().copy().withColor(k), 2);
        *///?}
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}

package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
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
    public void onPress() {
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        alpha = active ? 1 : 0.8f;
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.RGB_PREVIEW, getX(), getY(), getWidth(), getHeight());
        guiGraphics.fill(getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2, ColorUtil.colorFromInt(LegacyOptions.controllerLedRed.get(), LegacyOptions.controllerLedGreen.get(), LegacyOptions.controllerLedBlue.get(), 255));
        int color = LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused());
        renderScrollingString(guiGraphics, Minecraft.getInstance().font, 2, color | Mth.ceil(alpha * 255.0f) << 24);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}

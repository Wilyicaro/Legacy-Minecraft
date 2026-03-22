package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import wily.legacy.util.client.LegacyRenderUtil;

/// helper class to transition from 1.21.10 to 1.21.11
public abstract class AbstractLegacyButton extends AbstractButton {
    protected AbstractLegacyButton(int i, int j, int k, int l, Component component) {
        super(i, j, k, l, component);
    }

    protected void /*? if >=1.21.11 {*/renderContents/*?} else {*//*renderWidget*//*?}*/(GuiGraphics guiGraphics, int i, int j, float f) {
        renderButton(guiGraphics, i, j, f);
    }

    protected void renderButton(GuiGraphics guiGraphics, int i, int j, float f) {
        //? if >=1.21.11 {
        this.renderDefaultSprite(guiGraphics);
        this.renderString(guiGraphics, Minecraft.getInstance().font, LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()) | Mth.ceil(this.alpha * 255.0f) << 24);
        //?} else {
        /*super.renderWidget(guiGraphics, i, j, f);
        *///?}
    }

    //? if >=1.21.11 {
    public void renderString(GuiGraphics guiGraphics, Font font, int i) {
        this.renderScrollingString(guiGraphics, font, 2, i);
    }

    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int k, int l) {
        this.renderScrollingStringOverContents(guiGraphics.textRenderer(GuiGraphics.HoveredTextEffects.NONE), this.getMessage().copy().withColor(k), k);
    }
    //?}
}

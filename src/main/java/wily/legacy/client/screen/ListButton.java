package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

public abstract class ListButton extends AbstractButton {
    protected final RenderableVList list;

    public ListButton(RenderableVList list, int x, int y, int width, int height, Component component) {
        super(x, y, width, height, component);
        this.list = list;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraft = Minecraft.getInstance();
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, this.alpha);
        FactoryScreenUtil.enableBlend();
        FactoryScreenUtil.enableDepthTest();
        boolean focused = isHoveredOrFocused();
        ResourceLocation sprite = focused
                ? ScreenUtil.getSpriteOrFallback(LegacySprites.LIST_BUTTON_HIGHLIGHTED, LegacySprites.BUTTON_HIGHLIGHTED)
                : ScreenUtil.getSpriteOrFallback(LegacySprites.LIST_BUTTON, LegacySprites.BUTTON);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        renderScrollingString(guiGraphics, minecraft.font, 2, ScreenUtil.getDefaultTextColor(!focused));
    }

    @Override
    protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int color) {
        String name = listName();
        int textX = getX() + list.accessor.getInteger(name + ".buttonMessage.xOffset", 35);
        int textY = getY() + list.accessor.getInteger(name + ".buttonMessage.yOffset", 0);
        int textRight = getX() + list.accessor.getInteger(name + ".buttonMessage.right", getWidth() - 2);
        int textBottom = getY() + list.accessor.getInteger(name + ".buttonMessage.bottom", getHeight());
        ScreenUtil.applySDFont(ignored -> ScreenUtil.renderScrollingString(guiGraphics, font, getMessage(), textX, textY, Math.max(textX + 1, textRight), Math.max(textY + 1, textBottom), color, true));
    }

    protected String listName() {
        return list.name == null ? "renderableVList" : list.name;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}

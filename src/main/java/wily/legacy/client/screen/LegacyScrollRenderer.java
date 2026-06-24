package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.util.LegacySprites;

public class LegacyScrollRenderer {
    public static final ResourceLocation[] SCROLLS = new ResourceLocation[]{LegacySprites.SCROLL_UP, LegacySprites.SCROLL_DOWN, LegacySprites.SCROLL_LEFT, LegacySprites.SCROLL_RIGHT};
    public final long[] lastScrolled = new long[4];
    public long lastScroll = 0;
    public ScreenDirection lastDirection;

    public void updateScroll(ScreenDirection direction){
        lastDirection = direction;
        lastScroll = (lastScrolled[direction.ordinal()] = Util.getMillis());
    }

    public void renderSmallScroll(GuiGraphics graphics, boolean up, int x, int y) {
        renderScroll(graphics, up ? ScreenDirection.UP : ScreenDirection.DOWN, x, y, up ? LegacySprites.SCROLL_UP_SMALL : LegacySprites.SCROLL_DOWN_SMALL, 7, 4);
    }

    public void renderScroll(GuiGraphics graphics, ScreenDirection direction, int x, int y){
        boolean h = direction.getAxis() == ScreenAxis.HORIZONTAL;
        renderScroll(graphics, direction, x, y, SCROLLS[direction.ordinal()], h ? 6 : 13, h ? 11 : 7);
    }

    public float getAlpha(long last) {
        float f = (Util.getMillis() - last) / 320f;
        return Math.min(1.0f, f < 0.5f ? 1 - f * 2f : (f - 0.5f) * 2f);
    }

    public void renderScroll(GuiGraphics graphics, ScreenDirection direction, int x, int y, ResourceLocation sprite, int width, int height) {
        FactoryScreenUtil.enableBlend();
        long l = lastScrolled[direction.ordinal()];
        if (l > 0)
            FactoryGuiGraphics.of(graphics).setColor(1.0f,1.0f,1.0f, getAlpha(l));
        FactoryGuiGraphics.of(graphics).blitSprite(sprite,x,y, width, height);
        if (l > 0)
            FactoryGuiGraphics.of(graphics).clearColor();
        FactoryScreenUtil.disableBlend();
    }

}

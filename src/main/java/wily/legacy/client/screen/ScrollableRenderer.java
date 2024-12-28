package wily.legacy.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import org.joml.Math;
import wily.factoryapi.base.Stocker;

public class ScrollableRenderer {
    public Stocker.Sizeable scrolled = new Stocker.Sizeable(0);
    public float visualLabelY = 0;
    public ScrollableRenderer(){
        this(new LegacyScrollRenderer());
    }

    public ScrollableRenderer(LegacyScrollRenderer renderer){
        scrollRenderer = renderer;
    }
    public final LegacyScrollRenderer scrollRenderer;
    public void render(GuiGraphics graphics, int x, int y, int width, int height, Runnable scrollable){
        graphics.enableScissor(x,y, x + width, y + height);
        graphics.pose().pushPose();
        float s = Math.min(1.0f,(Util.getMillis() - scrollRenderer.lastScroll) / 480f);
        visualLabelY += (scrolled.get() - visualLabelY) * s;
        graphics.pose().translate(0, -visualLabelY * 12,0);
        scrollable.run();
        graphics.pose().popPose();
        graphics.disableScissor();
        if (scrolled.max > 0){
            if (scrolled.get() < scrolled.max) scrollRenderer.renderScroll(graphics, ScreenDirection.DOWN,x + width - 13, y + 3 + height);
            if (scrolled.get() > 0) scrollRenderer.renderScroll(graphics, ScreenDirection.UP, x + width - 29, y + 3 + height);
        }
    }
    public boolean mouseScrolled(double g){
        if (scrolled.max > 0){
            int i = scrolled.add((int)-Math.signum(g));
            if (i != 0) {
                scrollRenderer.updateScroll(i > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                return true;
            }
        }
        return false;
    }
}

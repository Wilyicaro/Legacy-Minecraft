package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.util.Mth;
import org.joml.Math;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;

public class ScrollableRenderer {
    public Stocker.Sizeable scrolled = new Stocker.Sizeable(0);
    public float lineHeight = 12f;
    public final LegacyScrollRenderer scrollRenderer;
    protected float oldSmoothScrolled;
    protected float smoothScrolled;

    public ScrollableRenderer(){
        this(new LegacyScrollRenderer());
    }

    public ScrollableRenderer(LegacyScrollRenderer renderer){
        scrollRenderer = renderer;
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height, Runnable scrollable){
        FactoryGuiGraphics.of(graphics).enableScissor(x,y, x + width, y + height);
        oldSmoothScrolled = smoothScrolled;
        smoothScrolled = Mth.lerp(FactoryAPIClient.getPartialTick() * 0.5f, oldSmoothScrolled, scrolled.get());
        graphics.pose().pushMatrix();
        graphics.pose().translate(0, -getYOffset(),0);
        scrollable.run();
        graphics.pose().popMatrix();
        graphics.disableScissor();
        if (scrolled.max > 0){
            if (scrolled.get() < scrolled.max) scrollRenderer.renderScroll(graphics, ScreenDirection.DOWN,x + width - 13, y + 3 + height);
            if (scrolled.get() > 0) scrollRenderer.renderScroll(graphics, ScreenDirection.UP, x + width - 29, y + 3 + height);
        }
    }

    public float getYOffset(){
        return smoothScrolled * lineHeight;
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

    public boolean keyPressed(int key){
        boolean up;
        if (((up = key == InputConstants.KEY_UP) || key == InputConstants.KEY_DOWN) && scrolled.max > 0){
            int i = scrolled.add(up ? -1 : 1);
            if (i != 0) {
                scrollRenderer.updateScroll(i > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                return true;
            }
        }
        return false;
    }
}

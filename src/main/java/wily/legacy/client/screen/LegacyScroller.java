package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.phys.Vec2;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.function.Supplier;

public abstract class LegacyScroller extends SimpleLayoutRenderable {
    public static final float SCROLLER_HEIGHT_OFFSET = 13.5f;
    public final LegacyScrollRenderer scrollRenderer;
    public Vec2 offset = Vec2.ZERO;
    public boolean dragged = false;

    public LegacyScroller(LegacyScrollRenderer scrollRenderer) {
        this.scrollRenderer = scrollRenderer;
    }

    public static LegacyScroller create(int height, Supplier<Stocker.Sizeable> scrollSupplier) {
        return create(13, height, scrollSupplier);
    }

    public static LegacyScroller create(int width, int height, Supplier<Stocker.Sizeable> scrollSupplier) {
        LegacyScroller scroller = new LegacyScroller(new LegacyScrollRenderer()) {
            @Override
            public Stocker.Sizeable getScroll() {
                return scrollSupplier.get();
            }
        };
        scroller.size(width, height);
        return scroller;
    }

    public float getScrollerHeight() {
        return getHeight() - SCROLLER_HEIGHT_OFFSET;
    }

    public void offset(Vec2 offset) {
        this.offset = offset;
    }

    public abstract Stocker.Sizeable getScroll();

    @Override
    public void extractRenderState(GuiGraphicsExtractor GuiGraphicsExtractor, int i, int j, float f) {
        GuiGraphicsExtractor.pose().pushMatrix();
        GuiGraphicsExtractor.pose().translate(this.getX() + offset.x, getY() + offset.y);
        Stocker.Sizeable scroll = getScroll();
        if (scroll.max > 0) {
            GuiGraphicsExtractor.pose().pushMatrix();
            GuiGraphicsExtractor.pose().translate((getWidth() - 13) / 2f, 0);
            if (scroll.get() != scroll.max)
                scrollRenderer.renderScroll(GuiGraphicsExtractor, ScreenDirection.DOWN, 0, getHeight() + 4);
            if (scroll.get() > 0)
                scrollRenderer.renderScroll(GuiGraphicsExtractor, ScreenDirection.UP, 0, -11);
            GuiGraphicsExtractor.pose().popMatrix();
        } else FactoryGuiGraphics.of(GuiGraphicsExtractor).setBlitColor(1.0f, 1.0f, 1.0f, 0.5f);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, 0, 0, width, height);
        GuiGraphicsExtractor.pose().translate(-2f, -1f + (scroll.max > 0 ? scroll.get() * getScrollerHeight() / scroll.max : 0));
        FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.PANEL, 0, 0, 16, 16);
        FactoryGuiGraphics.of(GuiGraphicsExtractor).clearBlitColor();
        GuiGraphicsExtractor.pose().popMatrix();
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        return LegacyRenderUtil.isMouseOver(mouseX, mouseY, this.getX() + offset.x, this.getY() + offset.y, this.getWidth(), this.getHeight());
    }

    public boolean mouseScrolled(double g) {
        int i = (int) -Math.signum(g);
        Stocker.Sizeable scroll = getScroll();
        if (scroll.max > 0 || scroll.get() > 0) {
            int lastScrolled = scroll.get();
            scroll.set(Math.max(0, Math.min(scroll.get() + i, scroll.max)));
            if (lastScrolled != scroll.get()) {
                scrollRenderer.updateScroll(i > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                return true;
            }
        }
        return false;
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        if (isHovered(event.x(), event.y())) {
            dragged = true;
            return mouseDragged(event.y());
        }
        return false;
    }

    public void mouseReleased(MouseButtonEvent event) {
        if (dragged) {
            dragged = false;
        }
    }

    public boolean mouseDragged(double mouseY) {
        if (dragged) {
            Stocker.Sizeable scrolledList = getScroll();
            int lastScroll = scrolledList.get();
            scrolledList.set((int) Math.round(scrolledList.max * (mouseY - y) / getHeight()));
            if (lastScroll != scrolledList.get()) {
                scrollRenderer.updateScroll(scrolledList.get() - lastScroll > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                return true;
            }
        }
        return false;
    }
}

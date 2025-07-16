package wily.legacy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.function.Supplier;

public abstract class LegacyScroller extends SimpleLayoutRenderable {
    public final LegacyScrollRenderer scrollRenderer;
    public Vec3 offset = Vec3.ZERO;
    public boolean dragged = false;

    public LegacyScroller(LegacyScrollRenderer scrollRenderer){
        this.scrollRenderer = scrollRenderer;
    }

    public static LegacyScroller create(int height, Supplier<Stocker.Sizeable> scrollSupplier){
        return create(13, height, scrollSupplier);
    }

    public static LegacyScroller create(int width, int height, Supplier<Stocker.Sizeable> scrollSupplier){
        LegacyScroller scroller = new LegacyScroller(new LegacyScrollRenderer()) {
            @Override
            public Stocker.Sizeable getScroll() {
                return scrollSupplier.get();
            }
        };
        scroller.size(width, height);
        return scroller;
    }

    public float getScrollerHeight(){
        return getHeight() - 13.5f;
    }

    public void offset(Vec3 offset){
        this.offset = offset;
    }

    public abstract Stocker.Sizeable getScroll();

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(this.getX() + (float) offset.x, getY() + (float) offset.y);
        Stocker.Sizeable scroll = getScroll();
        if (scroll.max > 0) {
            if (scroll.get() != scroll.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN,0, getHeight() + 4);
            if (scroll.get() > 0)
                scrollRenderer.renderScroll(guiGraphics,ScreenDirection.UP, 0, -11);
        } else FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f, 1.0f, 1.0f, 0.5f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, 0, 0, width, height);
        guiGraphics.pose().translate(-2f, -1f + (scroll.max > 0 ? scroll.get() * getScrollerHeight() / scroll.max : 0));
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL,0,0, 16,16);
        FactoryGuiGraphics.of(guiGraphics).clearBlitColor();
        guiGraphics.pose().popMatrix();
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        return LegacyRenderUtil.isMouseOver(mouseX, mouseY, this.getX() + offset.x, this.getY() + offset.y, this.getWidth(), this.getHeight());
    }

    public boolean mouseScrolled(double g){
        int i = (int) -Math.signum(g);
        Stocker.Sizeable scroll = getScroll();
        if (scroll.max > 0 || scroll.get() > 0){
            int lastScrolled = scroll.get();
            scroll.set(Math.max(0,Math.min(scroll.get() + i,scroll.max)));
            if (lastScrolled != scroll.get()) {
                scrollRenderer.updateScroll(i > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                return true;
            }
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if(isHovered(mouseX, mouseY)) {
            dragged = true;
            return mouseDragged(mouseY);
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button){
        if (dragged) {
            dragged = false;
        }
    }

    public boolean mouseDragged(double mouseY){
        if (dragged){
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

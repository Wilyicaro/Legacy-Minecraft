package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class RenderableVList {
    protected final Stocker.Sizeable scrolledList = new Stocker.Sizeable(0);
    protected boolean canScrollDown = false;
    public final List<Renderable> renderables = new ArrayList<>();
    public Screen screen;
    protected int vRenderablesCount;
    protected LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();

    protected Function<LayoutElement,Integer> layoutSeparation = w-> 3;

    public RenderableVList addRenderables(Renderable... renderables){
        this.renderables.addAll(List.of(renderables));
        return this;
    }
    public RenderableVList addRenderable(Renderable renderable){
        renderables.add(renderable);
        return this;
    }
    public RenderableVList addOptions(OptionInstance<?>... optionInstances){
        return addRenderables(Arrays.stream(optionInstances).map(i-> i.createButton(Minecraft.getInstance().options, 0,0,0)).toList().toArray(AbstractWidget[]::new));
    }
    public RenderableVList layoutSpacing(Function<LayoutElement,Integer> layoutSeparation){
        this.layoutSeparation = layoutSeparation;
        return this;
    }

    public void init(Screen screen, int leftPos, int topPos,int listWidth, int listHeight) {
        this.screen = screen;
        boolean allowScroll = listHeight > 0;
        if (allowScroll) screen.renderables.add(((guiGraphics, i, j, f) -> {
            if (scrolledList.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP, leftPos + listWidth - 40, topPos + listHeight - 27);
            if (canScrollDown)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN,leftPos+ listWidth - 24, topPos + listHeight - 27);
        }));
        canScrollDown = false;
        int yDiff = 0;
        vRenderablesCount = 0;
        for (int i = scrolledList.get(); i < renderables.size(); i++) {
            int y = topPos + yDiff;
            Renderable r = renderables.get(i);
            if (!allowScroll || !(r instanceof LayoutElement l) || y + l.getHeight() + 30 <= topPos + listHeight) {
                vRenderablesCount++;
                if (r instanceof LayoutElement layoutElement) {
                    layoutElement.setX(leftPos);
                    layoutElement.setY(y);
                    yDiff += layoutElement.getHeight() + layoutSeparation.apply(layoutElement);
                }
                if (r instanceof AbstractWidget w)
                    w.setWidth(listWidth);
                if (r instanceof GuiEventListener g)
                    screen.children.add(g);
                if (r instanceof NarratableEntry e)
                    screen.narratables.add(e);
                screen.renderables.add(r);
            }else {
                canScrollDown = true;
                break;
            }
        }
        if (allowScroll && vRenderablesCount > 0) {
            scrolledList.max = (renderables.size() / vRenderablesCount - 1) * vRenderablesCount + renderables.size() % vRenderablesCount;
        }
    }



    public void mouseScrolled(double d, double e, double f, double g) {
        int scroll = (int) -Math.signum(g);
        if ((canScrollDown && scroll > 0) || (scrolledList.get() > 0 && scroll < 0)){
            int setScroll = Math.max(0,Math.min(scrolledList.get() + scroll,scrolledList.max));
            if (setScroll != scrolledList.get()) {
                scrollRenderer.updateScroll(scroll > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                scrolledList.set(setScroll);
                int focused =  screen.getFocused() instanceof Renderable r ? renderables.indexOf(r) : -1;
                screen.repositionElements();
                if (focused >= 0) screen.setFocused((GuiEventListener) renderables.get(focused));
            }
        }
    }
    public GuiEventListener getFirstFocusable(){
        for (int i = scrolledList.get(); i < scrolledList.get() + vRenderablesCount; i++)
            if (renderables.get(i) instanceof GuiEventListener l) return l;
        return null;
    }
    public GuiEventListener getLastFocusable(){
        for (int i = vRenderablesCount - 1; i >= 0; i--)
            if (renderables.get(scrolledList.get() + i) instanceof GuiEventListener l) return l;
        return null;
    }
    public boolean keyPressed(int i, boolean cyclic){
        boolean clicked = false;
        if (vRenderablesCount > 0) {
            if (i == InputConstants.KEY_DOWN) {
                if (screen.getFocused() == getLastFocusable()) {
                    if (canScrollDown) {
                        mouseScrolled(0, 0, 0, -1);
                        GuiEventListener l = getLastFocusable();
                        if (l != null)
                            screen.setFocused(l);
                        clicked = true;
                    } else if (cyclic) {
                        if (scrolledList.get() > 0) {
                            scrolledList.set(0);
                            screen.repositionElements();
                        }
                        GuiEventListener l = getFirstFocusable();
                        if (l != null)
                            screen.setFocused(l);
                        clicked = true;
                    }
                }
            }
            if (i == InputConstants.KEY_UP) {
                if (screen.getFocused() == getFirstFocusable()) {
                    if (scrolledList.get() > 0) {
                        mouseScrolled(0, 0, 0, 1);
                        GuiEventListener l = getFirstFocusable();
                        if (l != null)
                            screen.setFocused(l);
                        clicked = true;
                    } else if (cyclic){
                        while (canScrollDown)
                            mouseScrolled(0, 0, 0, -1);
                        GuiEventListener l = getLastFocusable();
                        if (l != null)
                            screen.setFocused(l);
                        clicked = true;
                    }
                }
            }
        }
        if (clicked) ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(), 1.0f,1.0f);
        return clicked;
    }

}

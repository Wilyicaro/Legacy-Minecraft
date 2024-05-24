package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
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
    protected final Stocker<Integer> scrolledList = Stocker.of(0);
    protected boolean canScrollDown = false;
    public boolean forceWidth = true;
    protected int leftPos;
    protected int topPos;
    protected int listWidth;
    protected int listHeight;
    public final List<Renderable> renderables = new ArrayList<>();
    public Screen screen;
    protected int renderablesCount;
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
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.listWidth = listWidth;
        this.listHeight = listHeight;
        boolean allowScroll = listHeight > 0;
        if (allowScroll) screen.renderables.add(((guiGraphics, i, j, f) -> {
            if (scrolledList.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP, leftPos + listWidth - 40, topPos + listHeight - 27);
            if (canScrollDown)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN,leftPos+ listWidth - 24, topPos + listHeight - 27);
        }));
        canScrollDown = false;
        int yDiff = 0;
        int xDiff = 0;
        renderablesCount = 0;
        for (int i = scrolledList.get(); i < renderables.size(); i++) {
            int x = leftPos + xDiff;
            int y = topPos + yDiff;
            Renderable r = renderables.get(i);
            if (!allowScroll || !(r instanceof LayoutElement l) || y + l.getHeight() + 30 <= topPos + listHeight) {
                if (r instanceof LayoutElement l) {
                    l.setX(x);
                    l.setY(y);
                    boolean changeRow = forceWidth || xDiff + l.getWidth() + 30 > listWidth;
                    xDiff = changeRow ? 0 : xDiff + l.getWidth() + layoutSeparation.apply(l);
                    yDiff += changeRow ? l.getHeight() + layoutSeparation.apply(l) : 0;
                }
                renderablesCount++;
                if (r instanceof AbstractWidget w && forceWidth)
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
    }
    public int getLineAmount(int scroll){
        if (forceWidth) return scroll;
        int xDiff = 0;
        int rowAmount = 0;
        for (int i = scrolledList.get() + renderablesCount - 1; i >= scrolledList.get(); i--) {
            if (renderables.get(i) instanceof LayoutElement e) xDiff += (xDiff == 0 ? 0 : layoutSeparation.apply(e)) + e.getWidth();
            rowAmount += scroll;
            if (xDiff + 30 > listWidth) break;
        }
        return rowAmount;
    }


    public void mouseScrolled(double g) {
        int scroll = (int) -Math.signum(g);
        if ((canScrollDown && scroll > 0) || (scrolledList.get() > 0 && scroll < 0)){
            int setScroll = Math.max(0,scrolledList.get() + getLineAmount(scroll));
            if (setScroll != scrolledList.get()) {
                scrollRenderer.updateScroll(scroll > 0 ? ScreenDirection.DOWN : ScreenDirection.UP);
                scrolledList.set(setScroll);
                screen.repositionElements();
            }
        }
    }
    public boolean isFocused(ScreenDirection direction){
        ComponentPath path = screen.nextFocusPath(new FocusNavigationEvent.ArrowNavigation(direction));
        return renderables.contains(screen.getFocused()) && (path == null || path.component() == screen.getFocused());
    }
    public boolean keyPressed(int i, boolean cyclic){
        if (renderablesCount > 1) {
            if (i == InputConstants.KEY_DOWN) {
                if (isFocused(ScreenDirection.DOWN)) {
                    if (canScrollDown) {
                        while (canScrollDown && isFocused(ScreenDirection.DOWN)) mouseScrolled( -1);
                    } else if (cyclic) {
                        if (scrolledList.get() > 0) {
                            scrolledList.set(0);
                            screen.repositionElements();
                        }
                        screen.setFocused(null);
                    }
                }
            }
            if (i == InputConstants.KEY_UP) {
                if (isFocused(ScreenDirection.UP)) {
                    if (scrolledList.get() > 0) {
                        while (scrolledList.get() > 0 && isFocused(ScreenDirection.UP)) mouseScrolled( 1);
                    } else if (cyclic){
                        while (canScrollDown)
                            mouseScrolled( -1);
                        screen.setFocused(null);
                    }
                }
            }
            if (forceWidth) return false;
            if (i == InputConstants.KEY_RIGHT && isFocused(ScreenDirection.RIGHT) || i == InputConstants.KEY_LEFT && isFocused(ScreenDirection.LEFT)){
                int focused = screen.children().indexOf(screen.getFocused()) + (i == InputConstants.KEY_RIGHT ? 1 : -1);
                if (i == InputConstants.KEY_LEFT && isFocused(ScreenDirection.UP) || i == InputConstants.KEY_RIGHT && isFocused(ScreenDirection.DOWN)) keyPressed(i == InputConstants.KEY_LEFT ? InputConstants.KEY_UP : InputConstants.KEY_DOWN,false);
                GuiEventListener newFocus;
                if (focused >= 0 && focused < screen.children().size() && renderables.contains(newFocus = screen.children().get(focused))){
                    screen.setFocused(newFocus);
                    ScreenUtil.playSimpleUISound(LegacySoundEvents.FOCUS.get(),1.0f);
                    return true;
                }
            }
        }
        return false;
    }

}

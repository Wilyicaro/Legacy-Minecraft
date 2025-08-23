package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

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
        return addRenderables(-1,renderables);
    }
    public RenderableVList addRenderables(int indexOffset, Renderable... renderables){
        for (Renderable renderable : renderables) this.renderables.add(indexOffset < 0 ? this.renderables.size() : indexOffset, renderable);
        return this;
    }
    public RenderableVList addRenderable(Renderable renderable){
        renderables.add(renderable);
        return this;
    }
    public <T> RenderableVList addLinkedOptions(int indexOffset, OptionInstance<T> dependency, Predicate<OptionInstance<T>> activeDependent, OptionInstance<?> dependent){
        Options options = Minecraft.getInstance().options;
        AbstractWidget dependentWidget = dependent.createButton(options,0,0,0);
        dependentWidget.active = activeDependent.test(dependency);
        AbstractWidget dependencyWidget = dependency.createButton(options,0,0,0,b-> dependentWidget.active = activeDependent.test(dependency));
        addRenderables(indexOffset,dependentWidget,dependencyWidget);
        return this;
    }
    public RenderableVList addOptions(OptionInstance<?>... optionInstances){
        return addOptions(-1,optionInstances);
    }
    public RenderableVList addOptions(int indexOffset, OptionInstance<?>... optionInstances){
        return addRenderables(indexOffset,Arrays.stream(optionInstances).map(i-> i.createButton(Minecraft.getInstance().options, 0,0,0)).toList().toArray(AbstractWidget[]::new));
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
                if (r instanceof GuiEventListener g) {
                    screen.children.add(g);
                    if (g.isFocused()) screen.setFocused(g);
                }
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
        for (int i = scrolledList.get(); i < scrolledList.get() + renderablesCount; i++) {
            if (renderables.get(i) instanceof LayoutElement e) xDiff += (xDiff == 0 ? 0 : layoutSeparation.apply(e)) + e.getWidth();
            rowAmount += scroll;
            if (xDiff + 30 > listWidth) break;
        }
        return rowAmount;
    }

    public void mouseScrolled(double g) {
        mouseScrolled((int) -Math.signum(g) > 0);
    }
    public void mouseScrolled(boolean down) {
        if ((canScrollDown && down) || (scrolledList.get() > 0 && !down)){
            int setScroll = Math.max(0,scrolledList.get() + getLineAmount(down ? 1 : -1));
            if (setScroll != scrolledList.get()) {
                scrollRenderer.updateScroll(down ? ScreenDirection.DOWN : ScreenDirection.UP);
                scrolledList.set(setScroll);
                screen.repositionElements();
            }
        }
    }
    public boolean isFocused(ScreenDirection direction, boolean allowExternalListener){
        ComponentPath path = getDirectionalNextFocusPath(direction);
        return path == null || allowExternalListener && !renderables.contains(path.component());
    }
    public boolean isFocused(ScreenDirection direction){
        return isFocused(direction,false);
    }
    public ComponentPath getDirectionalNextFocusPath(ScreenDirection direction){
        return screen.nextFocusPathInDirection(screen.getFocused().getRectangle(),direction,screen.getFocused(),new FocusNavigationEvent.ArrowNavigation(direction));
    }
    public void setLastFocusInDirection(ScreenDirection direction){
        if (screen.getFocused() != null) {
            ComponentPath path;
            while ((path = getDirectionalNextFocusPath(direction)) != null) screen.setFocused(path.component());
        }
    }
    public boolean keyPressed(int i, boolean cyclic){
        if (screen.getFocused() == null || !renderables.contains(screen.getFocused())) return false;
        if (renderablesCount > 1) {
            if (i == InputConstants.KEY_DOWN) {
                ComponentPath path = getDirectionalNextFocusPath(ScreenDirection.DOWN);
                if (path == null || !renderables.contains(path.component())) {
                    if (canScrollDown) {
                        while (canScrollDown && isFocused(ScreenDirection.DOWN,true)) mouseScrolled(true);
                    } else if (cyclic && path == null) {
                        if (scrolledList.get() > 0) {
                            scrolledList.set(0);
                            screen.repositionElements();
                            setLastFocusInDirection(ScreenDirection.DOWN);
                        }
                    }
                }
            }
            if (i == InputConstants.KEY_UP) {
                ComponentPath path = getDirectionalNextFocusPath(ScreenDirection.UP);
                if (path == null || !renderables.contains(path.component())) {
                    if (scrolledList.get() > 0) {
                        while (scrolledList.get() > 0 && isFocused(ScreenDirection.UP,true)) mouseScrolled(false);
                    } else if (cyclic && path == null){
                        while (canScrollDown)
                            mouseScrolled(true);
                    }
                }
            }
            if (listHeight > 0) {
                GuiEventListener lastFocus = screen.getFocused();
                if (i == InputConstants.KEY_PAGEDOWN) {
                    if (isFocused(ScreenDirection.DOWN)) while (screen.children().contains(lastFocus) && canScrollDown) mouseScrolled(true);
                    setLastFocusInDirection(ScreenDirection.DOWN);
                    return true;
                }
                if (i == InputConstants.KEY_PAGEUP) {
                    if (isFocused(ScreenDirection.UP)) while (screen.children().contains(lastFocus) && scrolledList.get() > 0) mouseScrolled(false);
                    setLastFocusInDirection(ScreenDirection.UP);
                    return true;
                }
            }
            if (forceWidth) return false;
            if (i == InputConstants.KEY_RIGHT && isFocused(ScreenDirection.RIGHT) || i == InputConstants.KEY_LEFT && isFocused(ScreenDirection.LEFT)){
                int focused = renderables.indexOf(screen.getFocused()) + (i == InputConstants.KEY_RIGHT ? 1 : -1);
                if (i == InputConstants.KEY_LEFT && isFocused(ScreenDirection.UP) || i == InputConstants.KEY_RIGHT && isFocused(ScreenDirection.DOWN)) keyPressed(i == InputConstants.KEY_LEFT ? InputConstants.KEY_UP : InputConstants.KEY_DOWN,false);
                if (focused >= 0 && focused < renderables.size() && renderables.get(focused) instanceof GuiEventListener newFocus){
                    screen.setFocused(newFocus);
                    ScreenUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(),true);
                    return true;
                }
            }
        }
        return false;
    }
    public interface Access {
        void renderableVListInit();
        RenderableVList getRenderableVList();
    }
}

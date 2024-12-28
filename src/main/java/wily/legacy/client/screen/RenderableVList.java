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
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4J;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class RenderableVList {
    protected final Stocker<Integer> scrolledList = Stocker.of(0);
    protected boolean canScrollDown = false;
    public boolean forceWidth = true;
    protected int leftPos;
    protected int topPos;
    protected int listWidth;
    protected int listHeight;
    public final List<Renderable> renderables = new ArrayList<>();
    public final UIDefinition.Accessor accessor;
    protected int renderablesCount;
    protected LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    public boolean cyclic = true;
    protected Function<LayoutElement,Integer> layoutSeparation = w-> 3;

    public RenderableVList(UIDefinition.Accessor accessor){
        this.accessor = accessor;
    }
    public RenderableVList(Screen screen){
        this(UIDefinition.Accessor.of(screen));
    }

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

    public Screen getScreen(){
        return accessor.getScreen();
    }
    public <S> S getScreen(Class<S> screenClass){
        return screenClass.cast(accessor.getScreen());
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
        return addOptions(indexOffset,Arrays.stream(optionInstances));
    }

    public RenderableVList addOptions(int indexOffset, Stream<OptionInstance<?>> optionInstances){
        return addRenderables(indexOffset,optionInstances.map(i-> i.createButton(Minecraft.getInstance().options, 0,0,0)).toList().toArray(AbstractWidget[]::new));
    }

    public RenderableVList layoutSpacing(Function<LayoutElement,Integer> layoutSeparation){
        this.layoutSeparation = layoutSeparation;
        return this;
    }

    public RenderableVList forceWidth(boolean forceWidth){
        this.forceWidth = forceWidth;
        return this;
    }

    public RenderableVList cyclic(boolean cyclic){
        this.cyclic = cyclic;
        return this;
    }

    public void focusRenderable(Renderable renderable){
        if (renderables.isEmpty()) return;
        if (getScreen().getFocused() == null && renderables.get(0) instanceof GuiEventListener l) getScreen().setFocused(l);
        while (getScreen().getFocused() != renderable){
            if (forceWidth){
                ComponentPath path = getDirectionalNextFocusPath(ScreenDirection.DOWN);
                if (isInvalidFocus(path,true)) {
                    if (canScrollDown) while (canScrollDown && isInvalidFocus(getDirectionalNextFocusPath(ScreenDirection.DOWN),true)) mouseScrolled(true);
                    else break;
                } else getScreen().setFocused(path.component());
            } else {
                GuiEventListener listener = getNextHorizontalRenderable();
                if (listener == null) {
                    ComponentPath path = getDirectionalNextFocusPath(ScreenDirection.RIGHT);
                    if (isInvalidFocus(path,true)){
                        break;
                    } else getScreen().setFocused(path.component());
                }
                else getScreen().setFocused(listener);
            }
        }
    }

    public void init(String name, int leftPos, int topPos, int listWidth, int listHeight) {
        init(accessor.getInteger(name+".x",leftPos),accessor.getInteger(name+".y",topPos),accessor.getInteger(name+".width",listWidth),accessor.getInteger(name+".height",listHeight));
    }

    public void init(int leftPos, int topPos, int listWidth, int listHeight) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.listWidth = listWidth;
        this.listHeight = listHeight;
        boolean allowScroll = listHeight > 0;
        if (allowScroll) accessor.getRenderables().add(((guiGraphics, i, j, f) -> {
            if (scrolledList.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP, leftPos + listWidth - 29, topPos + listHeight - 8);
            if (canScrollDown)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN,leftPos+ listWidth - 13, topPos + listHeight - 8);
        }));
        canScrollDown = false;
        int yDiff = 0;
        int xDiff = 0;
        renderablesCount = 0;
        for (int i = scrolledList.get(); i < renderables.size(); i++) {
            Renderable r = renderables.get(i);
            if (!allowScroll || !(r instanceof LayoutElement l) || yDiff + l.getHeight() + (i == renderables.size() - 1 && scrolledList.get() == 0 ? 0 : 12) <= listHeight) {
                if (r instanceof LayoutElement l) {
                    boolean changeRow = forceWidth || xDiff + l.getWidth() > listWidth;
                    l.setX(leftPos + xDiff);
                    l.setY(topPos + yDiff);
                    xDiff = changeRow ? 0 : xDiff + l.getWidth() + layoutSeparation.apply(l);
                    yDiff += changeRow ? l.getHeight() + layoutSeparation.apply(l) : 0;
                    if (changeRow && !forceWidth) {
                        i--;
                        continue;
                    }
                }
                renderablesCount++;
                if (r instanceof AbstractWidget w && forceWidth)
                    w.setWidth(listWidth);
                if (r instanceof GuiEventListener l) {
                    accessor.addChild(accessor.getChildren().size(),l,false,true);
                }
                accessor.addRenderable(accessor.getRenderables().size(),r);
            } else {
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
                accessor.reloadUI();
            }
        }
    }

    public boolean isHovered(double x, double y){
        return ScreenUtil.isMouseOver(x,y,leftPos,topPos,listWidth,listHeight == 0 ? getScreen().height : listHeight);
    }

    public boolean isInvalidFocus(ComponentPath path, boolean allowExternalListener){
        return path == null || allowExternalListener && !renderables.contains(path.component());
    }

    public boolean isDirectionFocused(ScreenDirection direction, boolean allowExternalListener){
        return isInvalidFocus(getDirectionalNextFocusPath(direction), allowExternalListener);
    }

    public boolean isDirectionFocused(ScreenDirection direction){
        return isDirectionFocused(direction,false);
    }

    public ComponentPath getDirectionalNextFocusPath(ScreenDirection direction){
        return getScreen().nextFocusPathInDirection(getScreen().getFocused().getRectangle(),direction, getScreen().getFocused(),new FocusNavigationEvent.ArrowNavigation(direction));
    }

    public void setLastFocusInDirection(ScreenDirection direction){
        if (getScreen().getFocused() != null) {
            ComponentPath path;
            while (!isInvalidFocus(path = getDirectionalNextFocusPath(direction),true)) {
                getScreen().setFocused(path.component());
            }
        }
    }

    public boolean keyPressed(int i){
        return keyPressed(i,cyclic);
    }

    public boolean keyPressed(int i, boolean cyclic){
        if (renderables.contains(getScreen().getFocused()) && renderablesCount > 1) {
            if (i == InputConstants.KEY_DOWN) {
                ComponentPath path = getDirectionalNextFocusPath(ScreenDirection.DOWN);
                if (isInvalidFocus(path,true)) {
                    if (canScrollDown) {
                        while (canScrollDown && isDirectionFocused(ScreenDirection.DOWN,true)) mouseScrolled(true);
                    } else if (cyclic && path == null) {
                        if (scrolledList.get() > 0) {
                            scrolledList.set(0);
                            accessor.reloadUI();
                            setLastFocusInDirection(ScreenDirection.DOWN);
                        }
                    }
                }
            }
            if (i == InputConstants.KEY_UP) {
                ComponentPath path = getDirectionalNextFocusPath(ScreenDirection.UP);
                if (isInvalidFocus(path,true)) {
                    if (scrolledList.get() > 0) {
                        while (scrolledList.get() > 0 && isDirectionFocused(ScreenDirection.UP,true)) mouseScrolled(false);
                    } else if (cyclic && path == null){
                        while (canScrollDown)
                            mouseScrolled(true);
                    }
                }
            }
            if (listHeight > 0) {
                GuiEventListener lastFocus = getScreen().getFocused();
                if (i == InputConstants.KEY_PAGEDOWN) {
                    if (isDirectionFocused(ScreenDirection.DOWN)) while (accessor.getChildren().contains(lastFocus) && canScrollDown) mouseScrolled(true);
                    setLastFocusInDirection(ScreenDirection.DOWN);
                    return true;
                }
                if (i == InputConstants.KEY_PAGEUP) {
                    if (isDirectionFocused(ScreenDirection.UP)) while (accessor.getChildren().contains(lastFocus) && scrolledList.get() > 0) mouseScrolled(false);
                    setLastFocusInDirection(ScreenDirection.UP);
                    return true;
                }
            }
            GuiEventListener nextHorizontalRenderable;
            if (!forceWidth && (nextHorizontalRenderable = getNextHorizontalRenderable(i == InputConstants.KEY_LEFT,i == InputConstants.KEY_RIGHT)) != null){
                getScreen().changeFocus(ComponentPath.path(nextHorizontalRenderable,getScreen()));
                return true;
            }
        }
        return false;
    }
    public GuiEventListener getNextHorizontalRenderable() {
        return getNextHorizontalRenderable(false, true);
    }
    public GuiEventListener getNextHorizontalRenderable(boolean left, boolean right) {
        if (right && isDirectionFocused(ScreenDirection.RIGHT) || left && isDirectionFocused(ScreenDirection.LEFT)){
            int focused = renderables.indexOf(getScreen().getFocused()) + (right ? 1 : -1);
            if (left && isDirectionFocused(ScreenDirection.UP) || right && isDirectionFocused(ScreenDirection.DOWN)) keyPressed(left ? InputConstants.KEY_UP : InputConstants.KEY_DOWN,false);
            if (focused >= 0 && focused < renderables.size() && renderables.get(focused) instanceof GuiEventListener newFocus) {
                return newFocus;
            }
        }
        return null;
    }

    public interface Access {
        void renderableVListInit();

        default RenderableVList getRenderableVList(){
            return getRenderableVLists().get(0);
        }

        default void focusRenderable(Predicate<Renderable> renderablePredicate, IntConsumer changeSelectedRenderableVList){
            for (int i = 0; i < getRenderableVLists().size(); i++) {
                RenderableVList renderableVList = getRenderableVLists().get(i);
                for (Renderable renderable : renderableVList.renderables) {
                    if (renderablePredicate.test(renderable)) {
                        changeSelectedRenderableVList.accept(i);
                        renderableVList.focusRenderable(renderable);
                        return;
                    }
                }
            }
        }

        List<RenderableVList> getRenderableVLists();

        default RenderableVList getRenderableVListAt(double x, double y){
            RenderableVList mainRenderableVList = getRenderableVList();
            if (mainRenderableVList.isHovered(x,y)) return mainRenderableVList;
            for (RenderableVList renderableVList : getRenderableVLists()) {
                if (renderableVList == mainRenderableVList) continue;
                if (renderableVList.isHovered(x,y)) return renderableVList;
            }
            return null;
        }
    }
}

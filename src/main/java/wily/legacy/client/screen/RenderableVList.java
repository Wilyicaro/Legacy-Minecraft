package wily.legacy.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.client.CommonColor;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.*;
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
    public final UIAccessor accessor;
    protected int renderablesCount;
    protected LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    public boolean cyclic = true;
    protected Function<LayoutElement,Integer> layoutSeparation = w-> 3;

    public RenderableVList(UIAccessor accessor){
        this.accessor = accessor;
    }
    public RenderableVList(Screen screen){
        this(UIAccessor.of(screen));
    }

    public RenderableVList addRenderables(Renderable... renderables){
        return addRenderables(this.renderables.size(), renderables);
    }

    public RenderableVList addRenderables(int indexOffset, Renderable... renderables){
        this.renderables.addAll(indexOffset, List.of(renderables));
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

    public <T> RenderableVList addLinkedOptions(int index, FactoryConfig<T> dependency, Predicate<FactoryConfig<T>> activeDependent, FactoryConfig<?>... dependents){
        List<AbstractWidget> dependentWidgets = new ArrayList<>();
        for (FactoryConfig<?> dependent : dependents) {
            AbstractWidget dependentWidget = LegacyConfigWidgets.createWidget(dependent);
            dependentWidget.active = activeDependent.test(dependency);
            dependentWidgets.add(dependentWidget);
        }
        renderables.addAll(index, ImmutableList.<AbstractWidget>builder().add(LegacyConfigWidgets.createWidget(dependency,0,0,0, b-> dependentWidgets.forEach(dependentWidget-> dependentWidget.active = activeDependent.test(dependency)))).addAll(dependentWidgets).build());
        return this;
    }

    public <T> RenderableVList addLinkedOptions(FactoryConfig<T> dependency, Predicate<FactoryConfig<T>> activeDependent, FactoryConfig<?>... dependents) {
        return addLinkedOptions(renderables.size(), dependency, activeDependent, dependents);
    }

    public RenderableVList addDependentOptions(int index, boolean enabled, FactoryConfig<?>... dependents){
        if (enabled) addOptions(index, dependents);
        else renderables.addAll(index, Arrays.stream(dependents).map(option-> {
            AbstractWidget widget = LegacyConfigWidgets.createWidget(option);
            widget.active = false;
            return widget;
        }).toList());
        return this;
    }

    public RenderableVList addDependentOptions(boolean enabled, FactoryConfig<?>... dependents){
        return addDependentOptions(this.renderables.size(), enabled, dependents);
    }

    public RenderableVList addOptions(FactoryConfig<?>... optionInstances){
        return addOptions(this.renderables.size(), optionInstances);
    }

    public RenderableVList addOptions(int indexOffset, FactoryConfig<?>... optionInstances){
        return addOptions(indexOffset,Arrays.stream(optionInstances));
    }

    public RenderableVList addOptions(Stream<FactoryConfig<?>> optionInstances) {
        return addOptions(this.renderables.size(), optionInstances);
    }

    public RenderableVList addOptions(int indexOffset, Stream<FactoryConfig<?>> optionInstances){
        renderables.addAll(indexOffset,optionInstances.map(LegacyConfigWidgets::createWidget).toList());
        return this;
    }

    public RenderableVList addMultSliderOption(FactoryConfig<?> optionInstance, double rangeMultiplier) {
        return addRenderable(LegacyConfigWidgets.createSliderWidget(optionInstance, rangeMultiplier));
    }

    public RenderableVList addOptionsCategory(Component title, FactoryConfig<?>... optionInstances) {
        addCategory(title);
        return addOptions(optionInstances);
    }

    public RenderableVList addCategory(Component title) {
        addRenderable(SimpleLayoutRenderable.createDrawString(title, 1, 4, listWidth, 13, CommonColor.INVENTORY_GRAY_TEXT.get(), false));
        return this;
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
        if (renderable instanceof GuiEventListener l && getScreen().children().contains(l)){
            getScreen().setFocused(l);
            return;
        }
        if (scrolledList.get() > 0) {
            scrolledList.set(0);
            accessor.reloadUI();
        }
        if (renderables.get(0) instanceof GuiEventListener l && getScreen().getFocused() != l) getScreen().setFocused(l);
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


    public void init(int leftPos, int topPos, int listWidth, int listHeight) {
        init("renderableVList", leftPos, topPos, listWidth, listHeight);
    }

    public void init(String name, int leftPos, int topPos, int listWidth, int listHeight) {
        this.leftPos = accessor.getInteger(name+".x",leftPos);
        this.topPos = accessor.getInteger(name+".y",topPos);
        this.listWidth = accessor.getInteger(name+".width",listWidth);
        this.listHeight = accessor.getInteger(name+".height",listHeight);
        boolean allowScroll = this.listHeight > 0;
        if (allowScroll) accessor.getChildrenRenderables().add(((guiGraphics, i, j, f) -> {
            if (scrolledList.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.UP, this.leftPos + this.listWidth - 29, this.topPos + this.listHeight - 8);
            if (canScrollDown)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.DOWN,this.leftPos + this.listWidth - 13, this.topPos + this.listHeight - 8);
        }));
        canScrollDown = false;
        int yDiff = 0;
        int xDiff = 0;
        renderablesCount = 0;
        for (int i = scrolledList.get(); i < renderables.size(); i++) {
            Renderable r = renderables.get(i);
            if (!allowScroll || !(r instanceof LayoutElement l) || yDiff + l.getHeight() + (i == renderables.size() - 1 && scrolledList.get() == 0 ? 0 : 12) <= this.listHeight) {
                if (r instanceof LayoutElement l) {
                    boolean changeRow = forceWidth || xDiff + l.getWidth() > this.listWidth;
                    l.setX(this.leftPos + xDiff);
                    l.setY(this.topPos + yDiff);
                    xDiff = changeRow ? 0 : xDiff + l.getWidth() + layoutSeparation.apply(l);
                    yDiff += changeRow ? l.getHeight() + layoutSeparation.apply(l) : 0;
                    if (changeRow && !forceWidth) {
                        i--;
                        continue;
                    }
                }
                renderablesCount++;
                if (r instanceof AbstractWidget w && forceWidth)
                    w.setWidth(this.listWidth);
                if (r instanceof GuiEventListener l) {
                    accessor.addChild(accessor.getChildren().size(),l,false,true);
                }
                accessor.addRenderable(accessor.getChildrenRenderables().size(),r);
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
        return LegacyRenderUtil.isMouseOver(x,y,leftPos,topPos,listWidth,listHeight == 0 ? getScreen().height : listHeight);
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
                    } else if (cyclic) {
                        if (path == null && scrolledList.get() > 0) {
                            scrolledList.set(0);
                            accessor.reloadUI();
                            setLastFocusInDirection(ScreenDirection.DOWN);
                        }
                    } else if (path == null) return true;
                }
            }
            if (i == InputConstants.KEY_UP) {
                ComponentPath path = getDirectionalNextFocusPath(ScreenDirection.UP);
                if (isInvalidFocus(path,true)) {
                    if (scrolledList.get() > 0) {
                        while (scrolledList.get() > 0 && isDirectionFocused(ScreenDirection.UP,true)) mouseScrolled(false);
                    } else if (cyclic){
                        if (path == null) {
                            while (canScrollDown)
                                mouseScrolled(true);
                        }
                    } else if (path == null) return true;
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

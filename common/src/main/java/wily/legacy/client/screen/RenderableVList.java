package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.LegacyMinecraft;
import wily.legacy.util.Stocker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class RenderableVList {
    protected static ResourceLocation SCROLL_DOWN = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_down");
    protected static ResourceLocation SCROLL_UP = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_up");
    protected final Stocker.Sizeable scrolledList = new Stocker.Sizeable(0);
    protected boolean canScroll = false;
    protected int panelOptionsCount = 0;
    protected final List<Renderable> vRenderables = new ArrayList<>();
    public Runnable repositionElements = ()->{};

    protected Function<LayoutElement,Integer> layoutSeparation = w-> 3;

    public RenderableVList addRenderables(Renderable... renderables){
        vRenderables.addAll(List.of(renderables));
        return this;
    }
    public RenderableVList addRenderable(Renderable renderable){
        vRenderables.add(renderable);
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
        repositionElements = screen::repositionElements;
        boolean allowScroll = listHeight > 0;
        if (allowScroll) screen.renderables.add(((guiGraphics, i, j, f) -> {
            if (canScroll) {
                if (scrolledList.get() > 0)
                    guiGraphics.blitSprite(SCROLL_UP, leftPos + listWidth - 40, topPos + listHeight - 27, 13, 7);
                if (scrolledList.get() < scrolledList.max)
                    guiGraphics.blitSprite(SCROLL_DOWN, leftPos+ listWidth - 24, topPos + listHeight - 27, 13, 7);
            }
        }));
        int yDiff = 0;
        int optionsCount = 0;
        for (int i = scrolledList.get(); i < vRenderables.size(); i++) {
            int y = topPos + yDiff;
            Renderable r = vRenderables.get(i);
            if (!allowScroll || !(r instanceof LayoutElement l) || y + l.getHeight() + 30 <= topPos + listHeight) {
                optionsCount++;
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
                canScroll = true;
                break;
            }
        }
        if (allowScroll && scrolledList.get() == 0 && optionsCount > 0) {
            panelOptionsCount = optionsCount;
            scrolledList.max = (vRenderables.size() / panelOptionsCount - 1) * panelOptionsCount + vRenderables.size() % panelOptionsCount;
        }
    }



    public void mouseScrolled(double d, double e, double f, double g) {
        int scroll = (int) -Math.signum(g);
        if (canScroll){
            int lastScrolled = scrolledList.get();
            scrolledList.set(Math.max(0,Math.min(scrolledList.get() + scroll,scrolledList.max)));
            if (lastScrolled != scrolledList.get()) {
                repositionElements.run();
            }
        }
    }

}

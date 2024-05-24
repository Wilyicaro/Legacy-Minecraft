package wily.legacy.client.screen;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class PanelVListScreen extends PanelBackgroundScreen{
    protected final RenderableVList renderableVList = new RenderableVList();
    public PanelVListScreen(Function<Screen,Panel> panelConstructor, Component component) {
        super(panelConstructor, component);
    }
    public PanelVListScreen(Screen parent, int imageWidth, int imageHeight, Component component) {
        super(imageWidth, imageHeight, component);
        this.parent = parent;
    }
    public PanelVListScreen(Screen parent, int imageWidth, int imageHeight, Component component, Renderable... renderables) {
        this(parent,imageWidth, imageHeight, component);
        renderableVList.addRenderables(renderables);
    }
    public PanelVListScreen(Screen parent, int imageWidth, int imageHeight, Component component, OptionInstance<?>... optionInstances) {
        this(parent,imageWidth, imageHeight, component);
        renderableVList.addOptions(optionInstances);
    }
    @Override
    protected void init() {
        super.init();
        getRenderableVList().init(this,panel.x + 10,panel.y + 10,panel.width - 20,panel.height);
    }

    public RenderableVList getRenderableVList() {
        return renderableVList;
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        getRenderableVList().mouseScrolled(g);
        return super.mouseScrolled(d, e, f, g);
    }
    public boolean renderableKeyPressed(int i){
        return getRenderableVList().keyPressed(i, true);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableKeyPressed(i)) return true;
        return super.keyPressed(i, j, k);
    }
}

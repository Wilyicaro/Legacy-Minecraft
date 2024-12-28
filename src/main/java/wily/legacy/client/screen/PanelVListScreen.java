package wily.legacy.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class PanelVListScreen extends PanelBackgroundScreen implements RenderableVList.Access {
    protected final RenderableVList renderableVList = new RenderableVList(accessor);
    public final List<RenderableVList> renderableVLists = new ArrayList<>(Collections.singleton(renderableVList));
    public Consumer<PanelVListScreen> onClose = s->{};

    public PanelVListScreen(Function<Screen,Panel> panelConstructor, Component component) {
        super(panelConstructor, component);
    }

    public PanelVListScreen(Screen parent, Function<Screen,Panel> panelConstructor, Component component) {
        super(parent, panelConstructor, component);
    }

    public PanelVListScreen(Screen parent, int imageWidth, int imageHeight, Component component) {
        super(parent, imageWidth, imageHeight, component);
    }

    @Override
    protected void init() {
        super.init();
        if (!getRenderableVLists().isEmpty()) renderableVListInit();
    }
    public void renderableVListInit(){
        getRenderableVList().init("renderableVList",panel.x + 10,panel.y + 10,panel.width - 20,panel.height-20);
    }
    @Override
    public void onClose() {
        super.onClose();
        onClose.accept(this);
    }

    @Override
    public List<RenderableVList> getRenderableVLists() {
        return renderableVLists;
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        RenderableVList vList = getRenderableVListAt(d,e);
        if (vList != null && vList.renderables.stream().anyMatch(children::contains)) vList.mouseScrolled(g);
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        for (RenderableVList vList : getRenderableVLists()) {
            if (vList.keyPressed(i)) return true;
        }
        return super.keyPressed(i, j, k);
    }
}

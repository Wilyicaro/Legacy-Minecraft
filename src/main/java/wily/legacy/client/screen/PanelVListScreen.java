package wily.legacy.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
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
    public Consumer<PanelVListScreen> onClose = s -> {
    };

    public PanelVListScreen(Panel.Constructor<PanelVListScreen> panelConstructor, Component component) {
        super(panelConstructor.cast(), component);
    }

    public PanelVListScreen(Screen parent, Panel.Constructor<PanelVListScreen> panelConstructor, Component component) {
        super(parent, panelConstructor.cast(), component);
    }

    public PanelVListScreen(Screen parent, int imageWidth, int imageHeight, Component component) {
        super(parent, imageWidth, imageHeight, component);
    }

    @Override
    protected void init() {
        super.init();
        if (!getRenderableVLists().isEmpty()) renderableVListInit();
    }

    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 10, panel.y + 10, panel.width - 20, panel.height - 20);
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
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (super.mouseScrolled(d, e, f, g)) return true;
        RenderableVList vList = getRenderableVListAt(d, e);
        if (vList != null && vList.renderables.stream().anyMatch(children::contains)) vList.mouseScrolled(g);
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        for (RenderableVList vList : getRenderableVLists()) {
            if (vList.keyPressed(keyEvent.key())) return true;
        }
        return super.keyPressed(keyEvent);
    }
}

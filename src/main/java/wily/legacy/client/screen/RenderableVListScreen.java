package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIDefinition;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RenderableVListScreen extends LegacyScreen implements RenderableVList.Access{
    protected final RenderableVList renderableVList = new RenderableVList(accessor).layoutSpacing(l->5);
    private final List<RenderableVList> renderableVLists = Collections.singletonList(renderableVList);

    public RenderableVListScreen(Component component, Consumer<RenderableVList> vListBuild) {
        super(component);
        vListBuild.accept(renderableVList);
    }

    public RenderableVListScreen(Screen parent, Component component, Consumer<RenderableVList> vListBuild) {
        super(component);
        this.parent = parent;
        vListBuild.accept(renderableVList);
    }

    public static Button.Builder openScreenButton(Component component, Supplier<Screen> supplier) {
        return Button.builder(component, button -> Minecraft.getInstance().setScreen(supplier.get()));
    }

    @Override
    public List<RenderableVList> getRenderableVLists() {
        return renderableVLists;
    }

    public void repositionElements() {
        super.repositionElements();
    }
    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i)) return true;
        return super.keyPressed(i, j, k);
    }

    @Override
    protected void init() {
        renderableVListInit();
    }

    @Override
    public void renderableVListInit() {
        renderableVList.init(width / 2 - 112,this.height / 3 + 10,225,0);
    }
}

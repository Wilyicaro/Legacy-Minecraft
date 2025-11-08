package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.client.LegacyOptions;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RenderableVListScreen extends LegacyScreen implements RenderableVList.Access {
    protected final RenderableVList renderableVList = new RenderableVList(accessor).layoutSpacing(l -> LegacyOptions.getUIMode().isSD() ? 4 : 5);
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
    public boolean keyPressed(KeyEvent keyEvent) {
        if (renderableVList.keyPressed(keyEvent.key())) return true;
        return super.keyPressed(keyEvent);
    }

    @Override
    protected void init() {
        renderableVListInit();
    }

    @Override
    public void renderableVListInit() {
        initRenderableVListHeight(20);
        renderableVList.init(width / 2 - 112, this.height / 3 + 5, 225, 0);
    }
}

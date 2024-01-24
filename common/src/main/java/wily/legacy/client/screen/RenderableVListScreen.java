package wily.legacy.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class RenderableVListScreen extends DefaultScreen{
    protected final RenderableVList renderableVList = new RenderableVList().layoutSpacing(l->5);

    public RenderableVListScreen(Component component, Consumer<RenderableVList> vListBuild) {
        super(component);
        vListBuild.accept(renderableVList);
    }
    public RenderableVListScreen(Screen parent,Component component, Consumer<RenderableVList> vListBuild) {
        super(component);
        this.parent = parent;
        vListBuild.accept(renderableVList);
    }
    public Button.Builder openScreenButton(Component component, Supplier<Screen> supplier) {
        return Button.builder(component, button -> this.minecraft.setScreen(supplier.get()));
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i,true)) return true;
        return super.keyPressed(i, j, k);
    }

    @Override
    protected void init() {
        renderableVList.init(this,width / 2 - 112,this.height / 3 + 10,225,0);
    }
}

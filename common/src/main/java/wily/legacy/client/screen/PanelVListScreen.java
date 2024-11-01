package wily.legacy.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Function;

public class PanelVListScreen extends PanelBackgroundScreen implements RenderableVList.Access{
    protected final RenderableVList renderableVList = new RenderableVList();
    public Consumer<PanelVListScreen> onClose = s->{};
    public PanelVListScreen(Function<Screen,Panel> panelConstructor, Component component) {
        super(panelConstructor, component);
    }
    public PanelVListScreen(Screen parent, int imageWidth, int imageHeight, Component component) {
        super(imageWidth, imageHeight, component);
        this.parent = parent;
    }
    @Override
    protected void init() {
        super.init();
        renderableVListInit();
    }
    public void renderableVListInit(){
        getRenderableVList().init(this,panel.x + 10,panel.y + 10,panel.width - 20,panel.height-20);
    }
    @Override
    public void onClose() {
        super.onClose();
        onClose.accept(this);
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

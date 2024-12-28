package wily.legacy.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class PanelBackgroundScreen extends LegacyScreen {
    protected final Panel panel;

    public PanelBackgroundScreen(Screen parent, Function<Screen,Panel> panelConstructor, Component component) {
        super(parent, component);
        panel = panelConstructor.apply(this);
    }
    public PanelBackgroundScreen(Function<Screen,Panel> panelConstructor, Component component) {
        super(component);
        panel = panelConstructor.apply(this);
    }
    public PanelBackgroundScreen(Screen parent, int imageWidth, int imageHeight, Component component){
        this(parent, s-> Panel.centered(s,imageWidth,imageHeight),component);
    }
    protected void panelInit(){
        addRenderableOnly(panel);
        panel.init();
    }
    @Override
    protected void init() {
        super.init();
        panelInit();
    }
}

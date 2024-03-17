package wily.legacy.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class PanelBackgroundScreen extends LegacyScreen {
    protected final Panel panel;
 
    public PanelBackgroundScreen(Function<Screen,Panel> panelConstructor, Component component) {
        super(component);
        panel = panelConstructor.apply(this);
    }
    public PanelBackgroundScreen(int imageWidth, int imageHeight, Component component){
        this(s-> Panel.centered(s,imageWidth,imageHeight),component);
    }
    @Override
    protected void init() {
        super.init();
        addRenderableOnly(panel);
        panel.init();
    }
}

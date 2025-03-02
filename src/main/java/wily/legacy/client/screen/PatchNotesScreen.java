package wily.legacy.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.util.LegacyComponents;

public class PatchNotesScreen extends PanelBackgroundScreen {
    protected final ScrollableRenderer scrollableRenderer = new ScrollableRenderer();
    private final ResourceLocation uiDefinitionID;
    public static final ResourceLocation NEWER_VERSION = Legacy4J.createModLocation("ui_definitions/newer_version_notes.json");
    public static final ResourceLocation NEWER_MINECRAFT_VERSION = FactoryAPI.createVanillaLocation("ui_definitions/newer_version_notes.json");

    public PatchNotesScreen(Screen parent, ResourceLocation uiDefinitionID) {
        super(parent, s-> Panel.centered(s, 370, 237, 0, 24), CommonComponents.EMPTY);
        this.uiDefinitionID = uiDefinitionID;
    }

    public static PanelBackgroundScreen createNewerVersion(Screen parent){
        return new PatchNotesScreen(parent, NEWER_VERSION);
    }
    public static PanelBackgroundScreen createNewerMinecraftVersion(Screen parent){
        return new PatchNotesScreen(parent, NEWER_MINECRAFT_VERSION);
    }

    @Override
    public String toString() {
        return uiDefinitionID.toString();
    }

    public ScrollableRenderer getScrollableRenderer(){
        return accessor.getElementValue("scrollable_renderer", scrollableRenderer, ScrollableRenderer.class);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (getScrollableRenderer().keyPressed(i)) return true;
        return super.keyPressed(i, j, k);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (getScrollableRenderer().mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.tooltips.remove(2);
    }

    @Override
    protected void init() {
        accessor.putStaticElement("scrollable_renderer",scrollableRenderer);
        super.init();
        addRenderableWidget(Button.builder(LegacyComponents.ACCEPT, b-> onClose()).bounds(panel.getX() + (panel.getWidth() - 200) / 2, panel.getY() + panel.getHeight() - 30, 200, 20).build());
    }
}

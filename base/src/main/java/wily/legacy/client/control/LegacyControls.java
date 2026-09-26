package wily.legacy.client.control;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.FactoryEvent;
import wily.legacy.client.control.tooltip.GuiControlTooltip;

public class LegacyControls {

    public static final GuiControlTooltip.Manager guiControlTooltipManager = new GuiControlTooltip.Manager();

    public static void init() {
//        FactoryAPIClient.setup(LegacyControls::setup);
        FactoryEvent.registerReloadListener(PackType.CLIENT_RESOURCES, guiControlTooltipManager);
    }

    public static void setup(Minecraft minecraft) {
        ControllerManager.getInstance().setup(minecraft);
        ControllerBinding.setupDefaultBindings(minecraft);
        LegacyControlsOptions.STORAGE.load();
        ControllerManager.getInstance().afterConfigLoad();
    }
}

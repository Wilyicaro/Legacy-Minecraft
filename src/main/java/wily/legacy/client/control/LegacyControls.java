package wily.legacy.client.control;

import net.minecraft.client.Minecraft;
import wily.factoryapi.FactoryAPIClient;

public class LegacyControls {

    public static void init() {
        FactoryAPIClient.setup(LegacyControls::setup);
    }

    public static void setup(Minecraft minecraft) {
        ControllerManager.getInstance().setup(minecraft);
        ControllerBinding.setupDefaultBindings(minecraft);
        LegacyControlsOptions.STORAGE.load();
        ControllerManager.getInstance().afterConfigLoad();
    }
}

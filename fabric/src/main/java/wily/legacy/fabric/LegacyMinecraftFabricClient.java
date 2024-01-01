package wily.legacy.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import wily.legacy.LegacyMinecraftClient;

public class LegacyMinecraftFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LegacyMinecraftClient.init();
        LegacyMinecraftClient.enqueueInit();
        ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> LegacyMinecraftClient.registerExtraModels(out));

    }
}

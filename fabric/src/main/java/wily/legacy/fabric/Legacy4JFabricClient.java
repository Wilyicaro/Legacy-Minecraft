package wily.legacy.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import wily.legacy.Legacy4JClient;

public class Legacy4JFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Legacy4JClient.init();
        Legacy4JClient.enqueueInit();
        ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> Legacy4JClient.registerExtraModels(out));

    }
}

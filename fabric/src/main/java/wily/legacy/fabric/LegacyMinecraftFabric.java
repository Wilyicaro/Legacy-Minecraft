package wily.legacy.fabric;

import net.fabricmc.api.ModInitializer;
import wily.legacy.LegacyMinecraft;


public class LegacyMinecraftFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        LegacyMinecraft.init();
    }
}

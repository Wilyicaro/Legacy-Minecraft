package wily.legacy.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.LegacyMinecraft;


public class LegacyMinecraftFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        LegacyMinecraft.registerBuiltInPacks((path, name, displayName, position, enabledByDefault) -> ResourceManagerHelperImpl.registerBuiltinResourcePack(new ResourceLocation(LegacyMinecraft.MOD_ID, name),path, FabricLoader.getInstance().getModContainer(LegacyMinecraft.MOD_ID).orElseThrow(), displayName, enabledByDefault ? ResourcePackActivationType.DEFAULT_ENABLED : ResourcePackActivationType.NORMAL));
        LegacyMinecraft.init();
    }
}

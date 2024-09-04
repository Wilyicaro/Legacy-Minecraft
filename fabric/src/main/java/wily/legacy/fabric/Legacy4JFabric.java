package wily.legacy.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;
import wily.legacy.network.CommonNetwork;


public class Legacy4JFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Legacy4J.registerBuiltInPacks((path, name, displayName, position, enabledByDefault) -> ResourceManagerHelperImpl.registerBuiltinResourcePack(new ResourceLocation(Legacy4J.MOD_ID, name),path, FabricLoader.getInstance().getModContainer(Legacy4J.MOD_ID).orElseThrow(), displayName, enabledByDefault ? ResourcePackActivationType.DEFAULT_ENABLED : ResourcePackActivationType.NORMAL));
        CommonNetwork.registerPayloads((client, type, codec, consumer) -> {
            if (client) ServerPlayNetworking.registerGlobalReceiver(type, (m,l,h,b,s) -> consumer.apply(codec.apply(b),Legacy4J.SECURE_EXECUTOR,()->h.player));
        });
        Legacy4J.init();
        CommonLifecycleEvents.TAGS_LOADED.register((l,t)->{
            Legacy4J.setup();
        });
        ServerTickEvents.START_SERVER_TICK.register(Legacy4J::preServerTick);
        CommandRegistrationCallback.EVENT.register(Legacy4J::registerCommands);
    }
}

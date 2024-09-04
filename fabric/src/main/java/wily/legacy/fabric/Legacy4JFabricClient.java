package wily.legacy.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.fabric.compat.ModMenuCompat;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Legacy4JFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Legacy4JClient.init();
        if (FabricLoader.getInstance().isModLoaded("modmenu")) ModMenuCompat.init();
        ResourceManagerHelper managerHelper = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        Legacy4JClient.registerReloadListeners(l->managerHelper.registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,l.getName().toLowerCase(Locale.ENGLISH) + l.hashCode());
            }

            @Override
            public String getName() {
                return l.getName();
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2) {
                return l.reload(preparationBarrier,resourceManager,profilerFiller,profilerFiller2,executor,executor2);
            }
        }));
        Legacy4JClient.registerKeyMappings(KeyBindingHelper::registerKeyBinding);
        ClientTickEvents.START_CLIENT_TICK.register(Legacy4JClient::preTick);
        ClientTickEvents.END_CLIENT_TICK.register(Legacy4JClient::postTick);
        ClientPlayConnectionEvents.JOIN.register((h,s,m) -> Legacy4JClient.clientPlayerJoin(m.player));
        ClientLifecycleEvents.CLIENT_STARTED.register(l->Legacy4JClient.setup());
        Legacy4JClient.registerItemColors(ColorProviderRegistry.ITEM::register);
        Legacy4JClient.registerBlockColors(ColorProviderRegistry.BLOCK::register);
        Legacy4JClient.registerScreen(MenuScreens::register);
    }
}

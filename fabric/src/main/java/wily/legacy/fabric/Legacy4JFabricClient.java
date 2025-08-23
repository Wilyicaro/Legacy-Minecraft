package wily.legacy.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.fabric.compat.ModMenuCompat;
import wily.legacy.network.CommonNetwork;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class Legacy4JFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Legacy4JClient.init();
        if (FabricLoader.getInstance().isModLoaded("modmenu")) ModMenuCompat.init();
        CommonNetwork.registerPayloads(new CommonNetwork.PayloadRegister(){
            @Override
            public <T extends CustomPacketPayload> void register(boolean client, ResourceLocation type, Function<FriendlyByteBuf, T> codec, CommonNetwork.Consumer<T> consumer) {
                if (!client) ClientPlayNetworking.registerGlobalReceiver(type, (m, l, b, s) -> consumer.apply(codec.apply(b),Legacy4JClient.SECURE_EXECUTOR,()->m.player));
            }
        });
        ResourceManagerHelper managerHelper = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        Legacy4JClient.registerReloadListeners(l->managerHelper.registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return new ResourceLocation(Legacy4J.MOD_ID,l.getName().toLowerCase(Locale.ENGLISH) + l.hashCode());
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

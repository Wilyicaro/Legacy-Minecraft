package wily.legacy.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.network.CommonNetwork;


public class Legacy4JFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Legacy4J.registerBuiltInPacks((path, name, displayName, position, enabledByDefault) -> ResourceManagerHelperImpl.registerBuiltinResourcePack(ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, name),path, FabricLoader.getInstance().getModContainer(Legacy4J.MOD_ID).orElseThrow(), displayName, enabledByDefault ? ResourcePackActivationType.DEFAULT_ENABLED : ResourcePackActivationType.NORMAL));
        CommonNetwork.registerPayloads(new CommonNetwork.PayloadRegister(){
            @Override
            public <T extends CustomPacketPayload> void register(boolean client, CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, CommonNetwork.Consumer<T> apply) {
                if (client) {
                    PayloadTypeRegistry.playC2S().register(type,codec);
                    ServerPlayNetworking.registerGlobalReceiver(type, (payload, context)-> apply.apply(payload,Legacy4J.SECURE_EXECUTOR, context::player));
                }else {
                    PayloadTypeRegistry.playS2C().register(type,codec);
                    if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->apply.apply(payload, Legacy4JClient.SECURE_EXECUTOR,context::player));
                }
            }
        });
        Legacy4J.init();
        CommonLifecycleEvents.TAGS_LOADED.register((l,t)->{
            Legacy4J.setup();
        });
        ServerTickEvents.START_SERVER_TICK.register(Legacy4J::preServerTick);
        ServerLifecycleEvents.AFTER_SAVE.register((l, fl, f)-> Legacy4J.serverSave(l));
        CommandRegistrationCallback.EVENT.register(Legacy4J::registerCommands);
        DefaultItemComponentEvents.MODIFY.register(c-> Legacy4J.changeItemDefaultComponents((i,b)-> c.modify(i,bc-> b.accept(bc::set))));
    }
}

package wily.legacy.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.*;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.network.CommonNetwork;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Mod(Legacy4J.MOD_ID)
@Mod.EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Legacy4JForge {


    public static IEventBus MOD_EVENT_BUS;

    public Legacy4JForge() {
        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();
        CommonNetwork.registerPayloads(new CommonNetwork.PayloadRegister() {
            @Override
            public <T extends CustomPacketPayload> void register(boolean client, ResourceLocation type, Function<FriendlyByteBuf, T> codec, CommonNetwork.Consumer<T> apply) {
                EventNetworkChannel NETWORK = ChannelBuilder.named(type).eventNetworkChannel();
                if (client || FMLEnvironment.dist.isClient()) NETWORK.addListener(p->{
                    if (p.getChannel().equals(type) && p.getPayload() != null) apply.apply(codec.apply(p.getPayload()), p.getSource().isClientSide() ? Legacy4JClient.SECURE_EXECUTOR : Legacy4J.SECURE_EXECUTOR, () -> p.getSource().isClientSide() ? Legacy4JForgeClient.getClientPlayer() : p.getSource().getSender());
                    p.getSource().setPacketHandled(true);
                    });
            }
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false, TickEvent.ServerTickEvent.class, e-> {
            if (e.phase == TickEvent.Phase.START) Legacy4J.preServerTick(e.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false, LevelEvent.Save.class, e-> {
            if (e.getLevel() instanceof ServerLevel l) Legacy4J.serverSave(l.getServer());
        });
        Legacy4J.init();
        if (FMLEnvironment.dist == Dist.CLIENT) Legacy4JForgeClient.init();
    }
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        Legacy4J.setup();
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event)
    {
        Legacy4J.registerBuiltInPacks((path, name, displayName, position, defaultEnabled) -> {
            Path resourcePath = ModList.get().getModFileById(Legacy4J.MOD_ID).getFile().findResource(path);
            for (PackType type : PackType.values()){
                if (event.getPackType() != type || !Files.isDirectory(resourcePath.resolve(type.getDirectory()))) continue;
                Pack pack = Pack.readMetaAndCreate(Legacy4J.MOD_ID + ":" + name, displayName,false, new PathPackResources.PathResourcesSupplier(resourcePath,true), type, position, PackSource.create(PackSource.BUILT_IN::decorate, defaultEnabled));
                event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
            }});
    }

}

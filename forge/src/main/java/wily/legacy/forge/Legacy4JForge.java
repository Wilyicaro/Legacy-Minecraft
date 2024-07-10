package wily.legacy.forge;

import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.GatherComponentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadFlow;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.network.CommonNetwork;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Mod(Legacy4J.MOD_ID)
@Mod.EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Legacy4JForge {
    public static IEventBus MOD_EVENT_BUS;
    public static Channel<CustomPacketPayload> NETWORK;

    public Legacy4JForge() {
        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();
        PayloadFlow<RegistryFriendlyByteBuf,CustomPacketPayload> flow =ChannelBuilder.named(Legacy4J.MOD_ID).payloadChannel().play().serverbound();
        CommonNetwork.registerPayloads(new CommonNetwork.PayloadRegister() {
            @Override
            public <T extends CustomPacketPayload> void register(boolean client, CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, CommonNetwork.Consumer<T> apply) {
                if (client) flow.serverbound().add(type,codec,(m,c)-> apply.apply(m,Legacy4J.SECURE_EXECUTOR,c::getSender));
                else flow.clientbound().add(type,codec,(m,c)-> apply.apply(m,Legacy4JClient.SECURE_EXECUTOR,Legacy4JForgeClient::getClientPlayer)).build();
            }
        });
        NETWORK = flow.build();

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false, TickEvent.ServerTickEvent.class, e-> {
            if (e.phase == TickEvent.Phase.START) Legacy4J.preServerTick(e.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false, LevelEvent.Save.class, e-> {
            if (e.getLevel() instanceof ServerLevel l) Legacy4J.serverSave(l.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false, RegisterCommandsEvent.class, e-> Legacy4J.registerCommands(e.getDispatcher(),e.getBuildContext(),e.getCommandSelection()));
        Legacy4J.init();

        if (FMLEnvironment.dist == Dist.CLIENT) Legacy4JForgeClient.init();
    }
    @SubscribeEvent
    public static void modifyItemDefaultComponents(GatherComponentsEvent event) {
        Legacy4J.changeItemDefaultComponents((i,b)->{
            if (event.getOwner() == i) b.accept(event::register);
        });
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
                Pack pack = Pack.readMetaAndCreate(new PackLocationInfo( Legacy4J.MOD_ID + ":" + name, displayName,PackSource.create(PackSource.BUILT_IN::decorate, defaultEnabled), Optional.of(new KnownPack(Legacy4J.MOD_ID,Legacy4J.MOD_ID + ":" + name, SharedConstants.getCurrentVersion().getId()))), new PathPackResources.PathResourcesSupplier(resourcePath), type, new PackSelectionConfig(false,position,false));
                event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
            }});
    }

}

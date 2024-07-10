package wily.legacy.neoforge;

import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.network.CommonNetwork;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Mod(Legacy4J.MOD_ID)
@EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Legacy4JForge {
    public static IEventBus MOD_EVENT_BUS;
    public Legacy4JForge(IEventBus bus) {
        MOD_EVENT_BUS = bus;
        Legacy4J.init();

        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, e-> Legacy4J.preServerTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(LevelEvent.Save.class, e-> Legacy4J.serverSave(e.getLevel().getServer()));
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e-> Legacy4J.registerCommands(e.getDispatcher(),e.getBuildContext(),e.getCommandSelection()));
        if (FMLEnvironment.dist == Dist.CLIENT) Legacy4JForgeClient.init();

    }
    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        Legacy4J.setup();
    }
    @SubscribeEvent
    public static void modifyItemDefaultComponents(ModifyDefaultComponentsEvent event) {
        Legacy4J.changeItemDefaultComponents((i,b)-> event.modify(i,bc-> b.accept(bc::set)));
    }
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("v1");
        CommonNetwork.registerPayloads(new CommonNetwork.PayloadRegister() {
            @Override
            public <T extends CustomPacketPayload> void register(boolean client, CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec, CommonNetwork.Consumer<T> apply) {
                if (client) registrar.playToServer(type,codec,(h,arg)->apply.apply(h,Legacy4J.SECURE_EXECUTOR,arg::player));
                else registrar.playToClient(type,codec,(h,arg)->apply.apply(h,Legacy4JClient.SECURE_EXECUTOR,arg::player));
            }
        });
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

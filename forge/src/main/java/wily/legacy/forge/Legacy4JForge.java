package wily.legacy.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.*;
import net.minecraftforge.network.event.EventNetworkChannel;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.network.CommonNetwork;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

@Mod(Legacy4J.MOD_ID)
@Mod.EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Legacy4JForge {


    public static IEventBus MOD_EVENT_BUS;

    public Legacy4JForge() {
        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();
        CommonNetwork.registerPayloads((client, type, codec, apply) -> {
            EventNetworkChannel NETWORK = NetworkRegistry.ChannelBuilder.named(type).networkProtocolVersion(()->"1").serverAcceptedVersions(s-> !s.equals(NetworkRegistry.ABSENT.version()) && Integer.parseInt(s) >= 1).clientAcceptedVersions(s->!s.equals(NetworkRegistry.ABSENT.version()) && Integer.parseInt(s) >= 1).eventNetworkChannel();
            if (client || FMLEnvironment.dist.isClient()) NETWORK.addListener(p->{
                    if (p.getPayload() != null) apply.apply(codec.apply(p.getPayload()), p.getSource().get().getDirection().getReceptionSide().isClient() ? Legacy4JClient.SECURE_EXECUTOR : Legacy4J.SECURE_EXECUTOR, () -> p.getSource().get().getDirection().getReceptionSide().isClient() ? Legacy4JForgeClient.getClientPlayer() : p.getSource().get().getSender());
                p.getSource().get().setPacketHandled(true);
                });
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL,false, TickEvent.ServerTickEvent.class, e-> {
            if (e.phase == TickEvent.Phase.START) Legacy4J.preServerTick(e.getServer());
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
                Pack pack = Pack.readMetaAndCreate(Legacy4J.MOD_ID + ":" + name, displayName,false,s-> new PathPackResources(s,resourcePath,true), type, position, PackSource.create(PackSource.BUILT_IN::decorate, defaultEnabled));
                event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
            }});
    }

}

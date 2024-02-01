package wily.legacy.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.resource.DelegatingPackResources;
import wily.legacy.LegacyMinecraft;
import wily.legacy.LegacyMinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

@Mod(LegacyMinecraft.MOD_ID)
@Mod.EventBusSubscriber(modid = LegacyMinecraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LegacyMinecraftForge {
    public LegacyMinecraftForge() {
        EventBuses.registerModEventBus(LegacyMinecraft.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        LegacyMinecraft.init();
        if (FMLEnvironment.dist == Dist.CLIENT) LegacyMinecraftClient.init();
    }
    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event)
    {
        LegacyMinecraft.registerBuiltInPacks((path, name, displayName, position, defaultEnabled) -> {
            Path resourcePath = ModList.get().getModFileById(LegacyMinecraft.MOD_ID).getFile().findResource(path);
            for (PackType type : PackType.values()){
                if (event.getPackType() != type || !Files.isDirectory(resourcePath.resolve(type.getDirectory()))) continue;
                Pack pack = Pack.readMetaAndCreate(LegacyMinecraft.MOD_ID + ":" + name, displayName,false, new PathPackResources.PathResourcesSupplier(resourcePath,true), type, position, PackSource.create(PackSource.BUILT_IN::decorate, defaultEnabled));
                event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
            }});
    }

}

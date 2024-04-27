package wily.legacy.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod(Legacy4J.MOD_ID)
@Mod.EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Legacy4JForge {
    public Legacy4JForge() {
        EventBuses.registerModEventBus(Legacy4J.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        Legacy4J.init();
        if (FMLEnvironment.dist == Dist.CLIENT) Legacy4JClient.init();
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

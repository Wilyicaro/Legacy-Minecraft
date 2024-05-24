package wily.legacy.neoforge;

import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Mod(Legacy4J.MOD_ID)
@EventBusSubscriber(modid = Legacy4J.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Legacy4JForge {
    public Legacy4JForge() {
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
                Pack pack = Pack.readMetaAndCreate(new PackLocationInfo( Legacy4J.MOD_ID + ":" + name, displayName,PackSource.create(PackSource.BUILT_IN::decorate, defaultEnabled), Optional.of(new KnownPack(Legacy4J.MOD_ID,Legacy4J.MOD_ID + ":" + name, SharedConstants.getCurrentVersion().getId()))), new PathPackResources.PathResourcesSupplier(resourcePath), type, new PackSelectionConfig(false,position,false));
                event.addRepositorySource((packConsumer) -> packConsumer.accept(pack));
            }});
    }

}

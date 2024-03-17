package wily.legacy.client;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.controller.ControllerHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class LegacyResourceManager implements PreparableReloadListener {

    public static final ResourceLocation GAMEPAD_MAPPINGS = new ResourceLocation(LegacyMinecraft.MOD_ID,"gamepad_mappings.txt");

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2) {
        return preparationBarrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            profilerFiller2.startTick();
            profilerFiller2.push("listener");
            PackRepository repo = minecraft.getResourcePackRepository();
            if ((Platform.isModLoaded("sodium") || Platform.isModLoaded("rubidium")) && repo.getSelectedIds().contains("legacy:legacy_waters")){
                repo.removePack("legacy:legacy_waters");
                minecraft.reloadResourcePacks();
            }
            resourceManager.getResource(GAMEPAD_MAPPINGS).ifPresent(r->{
                try {
                    ControllerHandler.applyGamePadMappingsFromBuffer(r.openAsReader());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            profilerFiller2.pop();
            profilerFiller2.endTick();
        }, executor2);
    }
}

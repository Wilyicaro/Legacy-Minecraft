package wily.legacy.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import wily.legacy.Legacy4J;
import wily.legacy.client.controller.ControllerHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static wily.legacy.Legacy4JClient.gammaEffect;

public class LegacyResourceManager implements PreparableReloadListener {

    public static final ResourceLocation GAMEPAD_MAPPINGS = new ResourceLocation(Legacy4J.MOD_ID,"gamepad_mappings.txt");
    public static final ResourceLocation INTRO_LOCATION = new ResourceLocation(Legacy4J.MOD_ID,"intro.json");
    public static final ResourceLocation GAMMA_LOCATION = new ResourceLocation(Legacy4J.MOD_ID,"shaders/post/gamma.json");

    public static final List<ResourceLocation> INTROS = new ArrayList<>();

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
            if (repo.addPack("programmer_art")) minecraft.reloadResourcePacks();
            resourceManager.getResource(GAMEPAD_MAPPINGS).ifPresent(r->{
                try {
                    ControllerHandler.applyGamePadMappingsFromBuffer(r.openAsReader());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            if (gammaEffect != null) gammaEffect.close();
            try {
                gammaEffect = new PostChain(minecraft.getTextureManager(), resourceManager, minecraft.getMainRenderTarget(), GAMMA_LOCATION);
                gammaEffect.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            } catch (IOException iOException) {
                Legacy4J.LOGGER.warn("Failed to load gamma: {}", GAMMA_LOCATION, iOException);
            } catch (JsonSyntaxException jsonSyntaxException) {
                Legacy4J.LOGGER.warn("Failed to parse shader: {}", GAMMA_LOCATION, jsonSyntaxException);
            }
            registerIntroLocations(resourceManager);
            profilerFiller2.pop();
            profilerFiller2.endTick();
        }, executor2);
    }

    public static void registerIntroLocations(ResourceManager resourceManager){
        try {
            INTROS.clear();
            JsonArray array = GsonHelper.parseArray(resourceManager.getResourceOrThrow(INTRO_LOCATION).openAsReader());
            array.forEach(e-> INTROS.add(new ResourceLocation(e.getAsString())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

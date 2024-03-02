package wily.legacy.client;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import org.lwjgl.glfw.GLFW;
import wily.legacy.LegacyMinecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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
                    BufferedReader bufferedReader = r.openAsReader();
                    String s = bufferedReader.lines().collect(Collectors.joining());
                    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                    ByteBuffer mappingsBuffer = ByteBuffer.allocateDirect(bytes.length + 1);
                    mappingsBuffer.put(bytes);
                    mappingsBuffer.rewind();
                    GLFW.glfwUpdateGamepadMappings(mappingsBuffer);
                    mappingsBuffer.clear();
                    bufferedReader.close();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            profilerFiller2.pop();
            profilerFiller2.endTick();
        }, executor2);
    }
}

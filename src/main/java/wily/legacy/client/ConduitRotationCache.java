package wily.legacy.client;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelResource;
import wily.factoryapi.base.config.FactoryConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ConduitRotationCache {
    private static final FactoryConfig.StorageHandler STORAGE = new FactoryConfig.StorageHandler().withFile("legacy/conduit_rotations.json");
    private static final Codec<Map<String, Integer>> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(HashMap::new, Function.identity());
    private static final FactoryConfig<Map<String, Integer>> rotations = STORAGE.register(FactoryConfig.create("rotations", null, () -> CODEC, new HashMap<>(), v -> {}, STORAGE));
    private static boolean loaded;

    public static void clear() {
        loaded = false;
    }

    public static void remember(ClientLevel level, BlockPos pos, float yRot) {
        load();
        Map<String, Integer> map = new HashMap<>(rotations.get());
        map.put(key(level, pos), rotationFromYaw(yRot));
        rotations.set(map);
        rotations.save();
    }

    public static Integer get(ClientLevel level, BlockPos pos) {
        load();
        return rotations.get().get(key(level, pos));
    }

    private static void load() {
        if (loaded) return;
        STORAGE.load();
        loaded = true;
    }

    private static String key(ClientLevel level, BlockPos pos) {
        return worldId() + "|" + level.dimension().identifier() + ":" + pos.asLong();
    }

    private static String worldId() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null) {
            return "local:" + minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize();
        }
        ServerData server = minecraft.getCurrentServer();
        return server == null ? "unknown" : "server:" + server.ip;
    }

    private static int rotationFromYaw(float yRot) {
        return (int)Math.floor(yRot * 16.0F / 360.0F + 0.5D) & 15;
    }
}

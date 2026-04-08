package wily.legacy.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelResource;
import wily.legacy.Legacy4J;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ConduitRotationCache {
    private static final Gson GSON = new Gson();
    private static final Map<String, Integer> rotations = new HashMap<>();
    private static String worldId;

    public static void clear() {
        rotations.clear();
        worldId = null;
    }

    public static void remember(ClientLevel level, BlockPos pos, float yRot) {
        load(level);
        rotations.put(key(level, pos), rotationFromYaw(yRot));
        save();
    }

    public static Integer get(ClientLevel level, BlockPos pos) {
        load(level);
        return rotations.get(key(level, pos));
    }

    private static String key(ClientLevel level, BlockPos pos) {
        return level.dimension().location() + ":" + pos.asLong();
    }

    private static int rotationFromYaw(float yRot) {
        return (int) Math.floor(yRot * 16.0F / 360.0F + 0.5D) & 15;
    }

    private static void load(ClientLevel level) {
        String id = worldId(level);
        if (Objects.equals(worldId, id)) return;
        worldId = id;
        rotations.clear();
        if (id == null) return;
        Path path = path(id);
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            root.entrySet().forEach(e -> rotations.put(e.getKey(), e.getValue().getAsInt()));
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Failed to load conduit rotations", e);
        }
    }

    private static void save() {
        if (worldId == null) return;
        Path path = path(worldId);
        JsonObject root = new JsonObject();
        rotations.forEach(root::addProperty);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Failed to save conduit rotations", e);
        }
    }

    private static String worldId(ClientLevel level) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer() && minecraft.getSingleplayerServer() != null) {
            return "local:" + minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize();
        }
        ServerData server = minecraft.getCurrentServer();
        return server == null ? null : "server:" + server.ip;
    }

    private static Path path(String worldId) {
        Minecraft minecraft = Minecraft.getInstance();
        String name = UUID.nameUUIDFromBytes(worldId.getBytes(StandardCharsets.UTF_8)).toString() + ".json";
        return minecraft.gameDirectory.toPath().resolve("legacy").resolve("conduit_rotations").resolve(name);
    }
}

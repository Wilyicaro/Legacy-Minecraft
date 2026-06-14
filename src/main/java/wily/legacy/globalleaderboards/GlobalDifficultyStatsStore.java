package wily.legacy.globalleaderboards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import wily.legacy.Legacy4J;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardDifficulty;
import wily.legacy.client.screen.globalleaderboards.storage.GlobalLeaderboardStatCodec;
import wily.legacy.init.LegacyRegistries;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class GlobalDifficultyStatsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String FILE_NAME = "legacy_global_leaderboards.json";
    private static final long SAVE_INTERVAL = 30000L;
    private static final Map<Path, WorldStats> WORLDS = new LinkedHashMap<>();
    private static long lastSaveAt;

    private GlobalDifficultyStatsStore() {
    }

    public static synchronized void award(ServerPlayer player, Stat<?> stat, int amount) {
        MinecraftServer server = player.level().getServer();
        if (server == null || !server.isSingleplayer() || amount <= 0 || !player.gameMode.getGameModeForPlayer().equals(GameType.SURVIVAL)) {
            return;
        }

        Path path = path(server);
        WorldStats stats = worldStats(path);
        GlobalLeaderboardDifficulty difficulty = GlobalLeaderboardDifficulty.of(player.level().getDifficulty(), server.isHardcore());
        Object2IntOpenHashMap<Stat<?>> values = stats.playerStats(player.getUUID(), difficulty);
        values.put(stat, (int) Math.min((long) values.getInt(stat) + amount, Integer.MAX_VALUE));
        values.put(Stats.CUSTOM.get(LegacyRegistries.DAYS_PLAYED_STAT), (int) Math.min(Math.max(0L, player.level().getLevelData().getGameTime() / 24000L), Integer.MAX_VALUE));
        stats.dirty = true;
        saveSoon(path, stats);
    }

    public static synchronized void saveAll() {
        WORLDS.forEach(GlobalDifficultyStatsStore::save);
    }

    public static synchronized EnumMap<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> readPlayer(Path path, UUID playerId) {
        if (playerId == null) {
            return emptyStats();
        }

        Path key = path.toAbsolutePath().normalize();
        WorldStats stats = WORLDS.get(key);
        return (stats == null ? load(key) : stats).copyPlayerStats(playerId);
    }

    public static EnumMap<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> emptyStats() {
        EnumMap<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> stats = new EnumMap<>(GlobalLeaderboardDifficulty.class);
        for (GlobalLeaderboardDifficulty difficulty : GlobalLeaderboardDifficulty.values()) {
            stats.put(difficulty, new Object2IntOpenHashMap<>());
        }
        return stats;
    }

    public static Path path(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE_NAME);
    }

    public static Path path(Path worldPath) {
        return worldPath.resolve("data").resolve(FILE_NAME);
    }

    private static WorldStats worldStats(Path path) {
        Path key = path.toAbsolutePath().normalize();
        return WORLDS.computeIfAbsent(key, GlobalDifficultyStatsStore::load);
    }

    private static WorldStats load(Path path) {
        WorldStats stats = new WorldStats();
        if (!Files.isRegularFile(path)) {
            return stats;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                return stats;
            }

            JsonObject players = object(element.getAsJsonObject(), "players");
            for (Map.Entry<String, JsonElement> playerEntry : players.entrySet()) {
                UUID playerId = parseUuid(playerEntry.getKey());
                if (playerId == null || !playerEntry.getValue().isJsonObject()) {
                    continue;
                }

                EnumMap<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> playerStats = new EnumMap<>(GlobalLeaderboardDifficulty.class);
                JsonObject difficulties = playerEntry.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> difficultyEntry : difficulties.entrySet()) {
                    GlobalLeaderboardDifficulty difficulty = GlobalLeaderboardDifficulty.byId(difficultyEntry.getKey());
                    if (difficultyEntry.getValue().isJsonObject()) {
                        playerStats.put(difficulty, decodeStats(difficultyEntry.getValue().getAsJsonObject()));
                    }
                }
                stats.players.put(playerId, playerStats);
            }
        } catch (IOException | RuntimeException err) {
            Legacy4J.LOGGER.warn("Failed to load global leaderboard stats {}", path, err);
        }

        return stats;
    }

    private static void saveSoon(Path path, WorldStats stats) {
        long now = System.currentTimeMillis();
        if (now - lastSaveAt >= SAVE_INTERVAL) {
            save(path, stats);
            lastSaveAt = now;
        }
    }

    private static void save(Path path, WorldStats stats) {
        if (!stats.dirty) {
            return;
        }

        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(toJson(stats), writer);
            }
            stats.dirty = false;
        } catch (IOException err) {
            Legacy4J.LOGGER.warn("Failed to save global leaderboard stats {}", path, err);
        }
    }

    private static JsonObject toJson(WorldStats stats) {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", 1);
        JsonObject players = new JsonObject();
        stats.players.forEach((playerId, difficulties) -> {
            JsonObject playerStats = new JsonObject();
            difficulties.forEach((difficulty, values) -> {
                if (!values.isEmpty()) {
                    playerStats.add(difficulty.id(), encodeStats(values));
                }
            });
            if (playerStats.size() > 0) {
                players.add(playerId.toString(), playerStats);
            }
        });
        root.add("players", players);
        return root;
    }

    private static JsonObject encodeStats(Object2IntMap<Stat<?>> stats) {
        JsonObject object = new JsonObject();
        stats.object2IntEntrySet().forEach(entry -> {
            String id = GlobalLeaderboardStatCodec.encode(entry.getKey());
            if (!id.isBlank() && entry.getIntValue() > 0) {
                object.addProperty(id, entry.getIntValue());
            }
        });
        return object;
    }

    private static Object2IntOpenHashMap<Stat<?>> decodeStats(JsonObject object) {
        Object2IntOpenHashMap<Stat<?>> stats = new Object2IntOpenHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                continue;
            }

            Stat<?> stat = GlobalLeaderboardStatCodec.decode(entry.getKey());
            if (stat != null) {
                stats.put(stat, value.getAsInt());
            }
        }
        return stats;
    }

    private static JsonObject object(JsonObject root, String key) {
        JsonElement element = root.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException err) {
            return null;
        }
    }

    private static final class WorldStats {
        private final Map<UUID, EnumMap<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>>> players = new LinkedHashMap<>();
        private boolean dirty;

        private Object2IntOpenHashMap<Stat<?>> playerStats(UUID playerId, GlobalLeaderboardDifficulty difficulty) {
            return players.computeIfAbsent(playerId, id -> new EnumMap<>(GlobalLeaderboardDifficulty.class)).computeIfAbsent(difficulty, key -> new Object2IntOpenHashMap<>());
        }

        private EnumMap<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> copyPlayerStats(UUID playerId) {
            EnumMap<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> copy = emptyStats();
            EnumMap<GlobalLeaderboardDifficulty, Object2IntOpenHashMap<Stat<?>>> source = players.get(playerId);
            if (source != null) {
                source.forEach((difficulty, stats) -> copy.put(difficulty, new Object2IntOpenHashMap<>(stats)));
            }
            return copy;
        }
    }
}

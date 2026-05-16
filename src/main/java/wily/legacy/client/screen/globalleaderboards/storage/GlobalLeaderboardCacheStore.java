package wily.legacy.client.screen.globalleaderboards.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardCache;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardEntry;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import wily.factoryapi.FactoryAPI;

public final class GlobalLeaderboardCacheStore {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

   private GlobalLeaderboardCacheStore() {
   }

   public static GlobalLeaderboardCacheStore.State load() {
      Path path = path();
      if (!Files.exists(path)) {
         return new GlobalLeaderboardCacheStore.State("", "", 0L, "", 0L, Map.of());
      }

      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         JsonElement element = JsonParser.parseReader(reader);
         if (!element.isJsonObject()) {
            return new GlobalLeaderboardCacheStore.State("", "", 0L, "", 0L, Map.of());
         }

         JsonObject root = element.getAsJsonObject();
         String playerUuid = stringValue(root, "playerUuid");
         String playerName = stringValue(root, "playerName");
         long updatedAt = longValue(root, "updatedAt");
         String lastSyncHash = stringValue(root, "lastSyncHash");
         long lastSyncAt = longValue(root, "lastSyncAt");
         LinkedHashMap<String, GlobalLeaderboardBoardCache> boardCaches = new LinkedHashMap<>();
         JsonObject boardsObject = root.has("boards") && root.get("boards").isJsonObject() ? root.getAsJsonObject("boards") : new JsonObject();
         boardsObject.entrySet().forEach(entry -> boardCaches.put(entry.getKey(), readBoardCache(entry.getKey(), entry.getValue().getAsJsonObject())));
         return new GlobalLeaderboardCacheStore.State(playerUuid, playerName, updatedAt, lastSyncHash, lastSyncAt, boardCaches);
      } catch (IOException err) {
         Legacy4J.LOGGER.warn("Failed to read global leaderboard cache {}", path, err);
         return new GlobalLeaderboardCacheStore.State("", "", 0L, "", 0L, Map.of());
      }
   }

   public static void save(GlobalLeaderboardCacheStore.State state) {
      Path path = path();
      try {
         Files.createDirectories(path.getParent());
         try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(toJson(state), writer);
         }
      } catch (IOException err) {
         Legacy4J.LOGGER.warn("Failed to write global leaderboard cache {}", path, err);
      }
   }

   private static JsonObject toJson(GlobalLeaderboardCacheStore.State state) {
      JsonObject root = new JsonObject();
      root.addProperty("formatVersion", 1);
      root.addProperty("playerUuid", state.playerUuid());
      root.addProperty("playerName", state.playerName());
      root.addProperty("updatedAt", state.updatedAt());
      root.addProperty("lastSyncHash", state.lastSyncHash());
      root.addProperty("lastSyncAt", state.lastSyncAt());
      JsonObject boardsObject = new JsonObject();
      state.boardCaches().forEach((boardId, cache) -> boardsObject.add(boardId, toJson(cache)));
      root.add("boards", boardsObject);
      return root;
   }

   private static JsonObject toJson(GlobalLeaderboardBoardCache cache) {
      JsonObject root = new JsonObject();
      root.addProperty("displayNameKey", cache.displayNameKey());
      root.addProperty("fetchedAt", cache.fetchedAt());
      root.addProperty("aroundFetchedAt", cache.aroundFetchedAt());
      root.addProperty("topFetchedAt", cache.topFetchedAt());
      root.add("aroundEntries", toJson(cache.aroundEntries()));
      root.add("topEntries", toJson(cache.topEntries()));
      return root;
   }

   private static JsonArray toJson(List<GlobalLeaderboardEntry> entries) {
      JsonArray array = new JsonArray();
      entries.forEach(entry -> {
         JsonObject object = new JsonObject();
         object.addProperty("rank", entry.rank());
         object.addProperty("playerUuid", entry.playerUuid());
         object.addProperty("playerName", entry.playerName());
         object.addProperty("totalScore", entry.totalScore());
         JsonObject statsObject = new JsonObject();
         entry.statValues().forEach((statId, value) -> statsObject.addProperty(statId, value));
         object.add("statValues", statsObject);
         array.add(object);
      });
      return array;
   }

   private static GlobalLeaderboardBoardCache readBoardCache(String boardId, JsonObject object) {
      String displayNameKey = stringValue(object, "displayNameKey");
      long fetchedAt = longValue(object, "fetchedAt");
      long aroundFetchedAt = longValue(object, "aroundFetchedAt", fetchedAt);
      long topFetchedAt = longValue(object, "topFetchedAt", fetchedAt);
      return new GlobalLeaderboardBoardCache(
         boardId,
         displayNameKey,
         fetchedAt,
         readEntries(object, "aroundEntries"),
         readEntries(object, "topEntries"),
         aroundFetchedAt,
         topFetchedAt
      );
   }

   private static List<GlobalLeaderboardEntry> readEntries(JsonObject root, String key) {
      if (!root.has(key) || !root.get(key).isJsonArray()) {
         return List.of();
      }

      List<GlobalLeaderboardEntry> entries = new ArrayList<>();
      JsonArray array = root.getAsJsonArray(key);
      array.forEach(element -> {
         if (!element.isJsonObject()) {
            return;
         }

         JsonObject object = element.getAsJsonObject();
         LinkedHashMap<String, Integer> statValues = new LinkedHashMap<>();
         JsonObject statsObject = object.has("statValues") && object.get("statValues").isJsonObject() ? object.getAsJsonObject("statValues") : new JsonObject();
         statsObject.entrySet().forEach(entry -> {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
               statValues.put(entry.getKey(), entry.getValue().getAsInt());
            }
         });
         entries.add(new GlobalLeaderboardEntry(intValue(object, "rank", entries.size() + 1), stringValue(object, "playerUuid"), stringValue(object, "playerName"), intValue(object, "totalScore", 0), statValues));
      });
      return entries;
   }

   private static String stringValue(JsonObject root, String key) {
      JsonElement element = root.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? element.getAsString() : "";
   }

   private static int intValue(JsonObject root, String key, int fallback) {
      JsonElement element = root.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsInt() : fallback;
   }

   private static long longValue(JsonObject root, String key) {
      JsonElement element = root.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsLong() : 0L;
   }

   private static long longValue(JsonObject root, String key, long fallback) {
      JsonElement element = root.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsLong() : fallback;
   }

   public record State(String playerUuid, String playerName, long updatedAt, String lastSyncHash, long lastSyncAt, Map<String, GlobalLeaderboardBoardCache> boardCaches) {
      public State {
         lastSyncHash = lastSyncHash == null ? "" : lastSyncHash;
         boardCaches = Map.copyOf(new LinkedHashMap<>(boardCaches));
      }
   }

   private static Path path() {
      return FactoryAPI.getConfigDirectory().resolve("legacy-global-leaderboards-cache.json");
   }
}

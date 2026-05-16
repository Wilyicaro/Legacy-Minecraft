package wily.legacy.client.screen.globalleaderboards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import wily.legacy.Legacy4J;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import wily.factoryapi.FactoryAPI;

public final class GlobalLeaderboardsConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final String DEFAULT_ENDPOINT = "https://l4j-global-leaderboards-api.creepereater201.workers.dev";
   private static GlobalLeaderboardsConfig.Values values;

   private GlobalLeaderboardsConfig() {
   }

   public static synchronized GlobalLeaderboardsConfig.Values get() {
      if (values != null) {
         return values;
      }

      GlobalLeaderboardsConfig.Values loaded = GlobalLeaderboardsConfig.Values.defaults();
      boolean changed = !readInto(loaded);
      values = loaded;
      if (changed) {
         write(values);
      }

      return values;
   }

   public static synchronized void save(GlobalLeaderboardsConfig.Values newValues) {
      values = newValues;
      write(newValues);
   }

   private static boolean readInto(GlobalLeaderboardsConfig.Values target) {
      Path path = path();
      if (!Files.exists(path)) {
         return false;
      }

      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         JsonElement element = JsonParser.parseReader(reader);
         if (!element.isJsonObject()) {
            return false;
         }

         JsonObject root = element.getAsJsonObject();
         int formatVersion = intValue(root, "formatVersion", 0);
         target.endpoint = normalizeEndpoint(stringValue(root, "endpoint", target.endpoint));
         target.syncOnLaunch = booleanValue(root, "syncOnLaunch", target.syncOnLaunch);
         target.prefetchAroundOnLaunch = formatVersion < 3 ? target.prefetchAroundOnLaunch : booleanValue(root, "prefetchAroundOnLaunch", target.prefetchAroundOnLaunch);
         target.prefetchTopOnLaunch = booleanValue(root, "prefetchTopOnLaunch", target.prefetchTopOnLaunch);
         target.aroundWindow = intValue(root, "aroundWindow", target.aroundWindow);
         target.topLimit = intValue(root, "topLimit", target.topLimit);
         target.syncCooldownSeconds = intValue(root, "syncCooldownSeconds", target.syncCooldownSeconds);
         target.fetchCooldownSeconds = intValue(root, "fetchCooldownSeconds", target.fetchCooldownSeconds);
         target.connectTimeoutSeconds = intValue(root, "connectTimeoutSeconds", target.connectTimeoutSeconds);
         target.readTimeoutSeconds = intValue(root, "readTimeoutSeconds", target.readTimeoutSeconds);
         return formatVersion >= 3;
      } catch (IOException | JsonSyntaxException err) {
         Legacy4J.LOGGER.warn("Failed to read global leaderboard config {}", path, err);
         return false;
      }
   }

   private static void write(GlobalLeaderboardsConfig.Values target) {
      Path path = path();
      try {
         Files.createDirectories(path.getParent());
         try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(toJson(target), writer);
         }
      } catch (IOException err) {
         Legacy4J.LOGGER.warn("Failed to write global leaderboard config {}", path, err);
      }
   }

   private static JsonObject toJson(GlobalLeaderboardsConfig.Values target) {
      JsonObject root = new JsonObject();
      root.addProperty("formatVersion", 3);
      root.addProperty("endpoint", target.endpoint);
      root.addProperty("syncOnLaunch", target.syncOnLaunch);
      root.addProperty("prefetchAroundOnLaunch", target.prefetchAroundOnLaunch);
      root.addProperty("prefetchTopOnLaunch", target.prefetchTopOnLaunch);
      root.addProperty("aroundWindow", target.aroundWindow);
      root.addProperty("topLimit", target.topLimit);
      root.addProperty("syncCooldownSeconds", target.syncCooldownSeconds);
      root.addProperty("fetchCooldownSeconds", target.fetchCooldownSeconds);
      root.addProperty("connectTimeoutSeconds", target.connectTimeoutSeconds);
      root.addProperty("readTimeoutSeconds", target.readTimeoutSeconds);
      return root;
   }

   private static String stringValue(JsonObject root, String key, String fallback) {
      JsonElement element = root.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? element.getAsString() : fallback;
   }

   private static boolean booleanValue(JsonObject root, String key, boolean fallback) {
      JsonElement element = root.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() ? element.getAsBoolean() : fallback;
   }

   private static int intValue(JsonObject root, String key, int fallback) {
      JsonElement element = root.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsInt() : fallback;
   }

   private static String normalizeEndpoint(String endpoint) {
      return endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint;
   }

   private static Path path() {
      return FactoryAPI.getConfigDirectory().resolve("legacy-global-leaderboards.json");
   }

   public static final class Values {
      private String endpoint;
      private boolean syncOnLaunch;
      private boolean prefetchAroundOnLaunch;
      private boolean prefetchTopOnLaunch;
      private int aroundWindow;
      private int topLimit;
      private int syncCooldownSeconds;
      private int fetchCooldownSeconds;
      private int connectTimeoutSeconds;
      private int readTimeoutSeconds;

      private Values() {
      }

      private static GlobalLeaderboardsConfig.Values defaults() {
         GlobalLeaderboardsConfig.Values values = new GlobalLeaderboardsConfig.Values();
         values.endpoint = DEFAULT_ENDPOINT;
         values.syncOnLaunch = true;
         values.prefetchAroundOnLaunch = true;
         values.prefetchTopOnLaunch = false;
         values.aroundWindow = 5;
         values.topLimit = 100;
         values.syncCooldownSeconds = 21600;
         values.fetchCooldownSeconds = 300;
         values.connectTimeoutSeconds = 10;
         values.readTimeoutSeconds = 20;
         return values;
      }

      public String endpoint() {
         return this.endpoint;
      }

      public boolean syncOnLaunch() {
         return this.syncOnLaunch;
      }

      public boolean prefetchAroundOnLaunch() {
         return this.prefetchAroundOnLaunch;
      }

      public boolean prefetchTopOnLaunch() {
         return this.prefetchTopOnLaunch;
      }

      public int aroundWindow() {
         return this.aroundWindow;
      }

      public int topLimit() {
         return this.topLimit;
      }

      public int syncCooldownSeconds() {
         return this.syncCooldownSeconds;
      }

      public int fetchCooldownSeconds() {
         return this.fetchCooldownSeconds;
      }

      public int connectTimeoutSeconds() {
         return this.connectTimeoutSeconds;
      }

      public int readTimeoutSeconds() {
         return this.readTimeoutSeconds;
      }

      public boolean hasEndpoint() {
         return !this.endpoint.isBlank();
      }
   }
}

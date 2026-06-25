package wily.legacy.client.screen.globalleaderboards.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import wily.legacy.Legacy4J;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardPage;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardRequest;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardRow;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardValue;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardViewMode;
import wily.legacy.client.screen.globalleaderboards.GlobalLeaderboardsConfig;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardSnapshot;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GlobalLeaderboardApiClient {
   private final HttpClient httpClient;
   private final GlobalLeaderboardsConfig.Values config;

   public GlobalLeaderboardApiClient(GlobalLeaderboardsConfig.Values config) {
      this.config = config;
      this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(timeoutSeconds(config.connectTimeoutSeconds()))).build();
   }

   public boolean syncBoards(String playerUuid, String playerName, Map<String, GlobalLeaderboardBoardSnapshot> boardSnapshots) {
      if (!this.hasEndpoint() || playerUuid.isBlank() || boardSnapshots.isEmpty()) {
         if (this.hasEndpoint()) {
            Legacy4J.LOGGER.debug("Skipping global leaderboard sync because no board snapshots were available");
         }
         return false;
      }

      JsonObject root = new JsonObject();
      root.addProperty("playerUuid", playerUuid);
      root.addProperty("playerName", playerName);
      JsonArray boards = new JsonArray();
      boardSnapshots.values().forEach(snapshot -> {
         if (snapshot.totalScore() <= 0 || snapshot.statValues().isEmpty()) {
            return;
         }

         JsonObject board = new JsonObject();
         board.addProperty("boardId", snapshot.boardId());
         board.addProperty("displayNameKey", snapshot.displayNameKey());
         board.addProperty("totalScore", snapshot.totalScore());
         JsonObject stats = new JsonObject();
         snapshot.statValues().forEach(stats::addProperty);
         board.add("statValues", stats);
         boards.add(board);
      });
      if (boards.isEmpty()) {
         Legacy4J.LOGGER.debug("Skipping global leaderboard sync because no positive board totals were available");
         return false;
      }
      root.add("boards", boards);
      return this.sendJson(this.resolve("/leaderboards/sync"), root);
   }

   public GlobalLeaderboardPage fetchBoard(GlobalLeaderboardRequest boardRequest) {
      String boardId = boardRequest.board().id();
      String playerUuid = boardRequest.playerUuid();
      GlobalLeaderboardViewMode viewMode = boardRequest.viewMode();
      if (!this.hasEndpoint() || boardId.isBlank()) {
         return GlobalLeaderboardPage.failed();
      }

      String mode = viewMode == GlobalLeaderboardViewMode.TOP ? "top" : "around";
      StringBuilder query = new StringBuilder();
      query.append("mode=").append(encode(mode));
      if (viewMode == GlobalLeaderboardViewMode.AROUND_ME && !playerUuid.isBlank()) {
         query.append("&playerUuid=").append(encode(playerUuid));
      }
      if (viewMode == GlobalLeaderboardViewMode.AROUND_ME) {
         query.append("&window=").append(boardRequest.aroundWindow());
      } else {
         query.append("&limit=").append(boardRequest.topLimit());
      }

      HttpRequest request = HttpRequest.newBuilder(this.resolve("/leaderboards/board/" + encode(boardId) + "?" + query))
         .timeout(Duration.ofSeconds(timeoutSeconds(this.config.readTimeoutSeconds())))
         .GET()
         .build();

      try {
         if (!this.hasEndpoint()) {
            return GlobalLeaderboardPage.failed();
         }
         HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
         if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Legacy4J.LOGGER.warn("Global leaderboard fetch for board {} failed with status {}", boardId, response.statusCode());
            return GlobalLeaderboardPage.failed();
         }

         JsonElement root = JsonParser.parseString(response.body());
         if (!root.isJsonObject()) {
            return GlobalLeaderboardPage.failed();
         }

         JsonObject object = root.getAsJsonObject();
         JsonArray entries = object.has("entries") && object.get("entries").isJsonArray() ? object.getAsJsonArray("entries") : new JsonArray();
         List<GlobalLeaderboardRow> parsed = new ArrayList<>();
         entries.forEach(element -> {
            if (!element.isJsonObject()) {
               return;
            }

            JsonObject entry = element.getAsJsonObject();
            LinkedHashMap<String, GlobalLeaderboardValue> statValues = new LinkedHashMap<>();
            JsonObject statsObject = entry.has("statValues") && entry.get("statValues").isJsonObject() ? entry.getAsJsonObject("statValues") : new JsonObject();
            statsObject.entrySet().forEach(statEntry -> statValues.put(statEntry.getKey(), valueFromJson(statEntry.getValue())));
            parsed.add(new GlobalLeaderboardRow(intValue(entry, "rank"), stringValue(entry, "playerUuid"), stringValue(entry, "playerName"), longValue(entry, "totalScore"), statValues));
         });
         return GlobalLeaderboardPage.successful(parsed);
      } catch (IOException | InterruptedException | RuntimeException err) {
         if (err instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         Legacy4J.LOGGER.warn("Failed to fetch global leaderboard board {}", boardId, err);
         return GlobalLeaderboardPage.failed();
      }
   }

   private boolean sendJson(URI uri, JsonObject body) {
      if (!this.hasEndpoint()) {
         return false;
      }

      HttpRequest request = HttpRequest.newBuilder(uri)
         .timeout(Duration.ofSeconds(timeoutSeconds(this.config.readTimeoutSeconds())))
         .header("Content-Type", "application/json")
         .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
         .build();

      try {
         HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
         if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Legacy4J.LOGGER.warn("Global leaderboard sync failed with status {}", response.statusCode());
            return false;
         } else {
            Legacy4J.LOGGER.debug("Global leaderboard sync completed with status {}", response.statusCode());
            return true;
         }
      } catch (IOException | InterruptedException err) {
         if (err instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         Legacy4J.LOGGER.warn("Failed to sync global leaderboards", err);
         return false;
      }
   }

   private boolean hasEndpoint() {
      return this.config.hasEndpoint() && GlobalLeaderboardsConfig.get().hasEndpoint();
   }

   private URI resolve(String path) {
      String base = this.config.endpoint();
      String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
      String normalizedPath = path.startsWith("/") ? path : "/" + path;
      return URI.create(normalizedBase + normalizedPath);
   }

   private static String encode(String value) {
      return URLEncoder.encode(value, StandardCharsets.UTF_8);
   }

   private static String stringValue(JsonObject object, String key) {
      JsonElement element = object.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? element.getAsString() : "";
   }

   private static int intValue(JsonObject object, String key) {
      JsonElement element = object.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsInt() : 0;
   }

   private static long longValue(JsonObject object, String key) {
      JsonElement element = object.get(key);
      return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsLong() : 0L;
   }

   private static GlobalLeaderboardValue valueFromJson(JsonElement element) {
      if (element == null || element.isJsonNull()) {
         return GlobalLeaderboardValue.EMPTY;
      }
      if (element.isJsonPrimitive()) {
         if (element.getAsJsonPrimitive().isNumber()) {
            return GlobalLeaderboardValue.number(element.getAsLong());
         }
         if (element.getAsJsonPrimitive().isString()) {
            return GlobalLeaderboardValue.text(element.getAsString());
         }
      }
      if (element.isJsonObject()) {
         JsonObject object = element.getAsJsonObject();
         return GlobalLeaderboardValue.of(longValue(object, "number"), stringValue(object, "text"));
      }
      return GlobalLeaderboardValue.EMPTY;
   }

   private static int timeoutSeconds(int seconds) {
      return Math.max(1, seconds);
   }
}

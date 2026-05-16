package wily.legacy.client.screen.globalleaderboards.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.globalleaderboards.GlobalLeaderboardsConfig;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardBoardSnapshot;
import wily.legacy.client.screen.globalleaderboards.model.GlobalLeaderboardEntry;
import wily.legacy.client.screen.globalleaderboards.screen.GlobalLeaderboardsScreen;
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
   private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
   private final GlobalLeaderboardsConfig.Values config;

   public GlobalLeaderboardApiClient(GlobalLeaderboardsConfig.Values config) {
      this.config = config;
   }

   public boolean syncBoards(String playerUuid, String playerName, Map<String, GlobalLeaderboardBoardSnapshot> boardSnapshots) {
      if (!this.config.hasEndpoint() || playerUuid.isBlank() || boardSnapshots.isEmpty()) {
         if (this.config.hasEndpoint()) {
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

   public List<GlobalLeaderboardEntry> fetchBoard(String boardId, String playerUuid, GlobalLeaderboardsScreen.ViewMode viewMode, int aroundWindow, int topLimit) {
      if (!this.config.hasEndpoint() || boardId.isBlank()) {
         return List.of();
      }

      String mode = viewMode == GlobalLeaderboardsScreen.ViewMode.TOP ? "top" : "around";
      StringBuilder query = new StringBuilder();
      query.append("mode=").append(encode(mode));
      if (viewMode == GlobalLeaderboardsScreen.ViewMode.AROUND_ME && !playerUuid.isBlank()) {
         query.append("&playerUuid=").append(encode(playerUuid));
      }
      if (viewMode == GlobalLeaderboardsScreen.ViewMode.AROUND_ME) {
         query.append("&window=").append(aroundWindow);
      } else {
         query.append("&limit=").append(topLimit);
      }

      HttpRequest request = HttpRequest.newBuilder(this.resolve("/leaderboards/board/" + encode(boardId) + "?" + query))
         .timeout(Duration.ofSeconds(this.config.readTimeoutSeconds()))
         .GET()
         .build();

      try {
         HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
         if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return List.of();
         }

         JsonElement root = JsonParser.parseString(response.body());
         if (!root.isJsonObject()) {
            return List.of();
         }

         JsonObject object = root.getAsJsonObject();
         JsonArray entries = object.has("entries") && object.get("entries").isJsonArray() ? object.getAsJsonArray("entries") : new JsonArray();
         List<GlobalLeaderboardEntry> parsed = new ArrayList<>();
         entries.forEach(element -> {
            if (!element.isJsonObject()) {
               return;
            }

            JsonObject entry = element.getAsJsonObject();
            LinkedHashMap<String, Integer> statValues = new LinkedHashMap<>();
            JsonObject statsObject = entry.has("statValues") && entry.get("statValues").isJsonObject() ? entry.getAsJsonObject("statValues") : new JsonObject();
            statsObject.entrySet().forEach(statEntry -> {
               if (statEntry.getValue().isJsonPrimitive() && statEntry.getValue().getAsJsonPrimitive().isNumber()) {
                  statValues.put(statEntry.getKey(), statEntry.getValue().getAsInt());
               }
            });
            parsed.add(new GlobalLeaderboardEntry(intValue(entry, "rank"), stringValue(entry, "playerUuid"), stringValue(entry, "playerName"), intValue(entry, "totalScore"), statValues));
         });
         return parsed;
      } catch (IOException | InterruptedException | RuntimeException err) {
         if (err instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         Legacy4J.LOGGER.warn("Failed to fetch global leaderboard board {}", boardId, err);
         return List.of();
      }
   }

   private boolean sendJson(URI uri, JsonObject body) {
      HttpRequest request = HttpRequest.newBuilder(uri)
         .timeout(Duration.ofSeconds(this.config.readTimeoutSeconds()))
         .header("Content-Type", "application/json")
         .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
         .build();

      try {
         HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
         if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Legacy4J.LOGGER.warn("Global leaderboard sync failed with status {} and body {}", response.statusCode(), response.body());
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
}

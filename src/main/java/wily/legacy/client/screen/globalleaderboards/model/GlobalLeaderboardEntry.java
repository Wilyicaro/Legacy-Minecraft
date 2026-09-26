package wily.legacy.client.screen.globalleaderboards.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record GlobalLeaderboardEntry(int rank, String playerUuid, String playerName, int totalScore, Map<String, Integer> statValues) {
   public GlobalLeaderboardEntry {
      statValues = statValues == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(statValues));
      playerUuid = playerUuid == null ? "" : playerUuid;
      playerName = playerName == null || playerName.isBlank() ? "Unknown" : playerName;
   }
}

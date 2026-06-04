package wily.legacy.client.screen.globalleaderboards.model;

import java.util.LinkedHashMap;
import java.util.Map;

public record GlobalLeaderboardBoardSnapshot(String boardId, String displayNameKey, int totalScore, Map<String, Integer> statValues) {
   public GlobalLeaderboardBoardSnapshot {
      displayNameKey = displayNameKey == null ? boardId : displayNameKey;
      statValues = statValues == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(statValues));
   }
}

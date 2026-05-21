package wily.legacy.api.client.leaderboards;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record GlobalLeaderboardRow(int rank, String playerUuid, String playerName, long totalScore, Map<String, GlobalLeaderboardValue> values) {
   public GlobalLeaderboardRow {
      playerUuid = playerUuid == null ? "" : playerUuid;
      playerName = playerName == null || playerName.isBlank() ? "Unknown" : playerName;
      values = copyValues(values);
   }

   public long number(String columnId) {
      return this.columnValue(columnId).number();
   }

   public String text(String columnId) {
      return this.columnValue(columnId).text();
   }

   public String displayText(String columnId) {
      GlobalLeaderboardValue value = this.columnValue(columnId);
      return value.hasText() ? value.text() : Long.toString(value.number());
   }

   public GlobalLeaderboardValue columnValue(String columnId) {
      return this.values.getOrDefault(columnId, GlobalLeaderboardValue.EMPTY);
   }

   public static GlobalLeaderboardRow fromValues(int rank, String playerUuid, String playerName, long totalScore, Map<String, GlobalLeaderboardValue> values) {
      return new GlobalLeaderboardRow(rank, playerUuid, playerName, totalScore, values);
   }

   public static GlobalLeaderboardRow fromNumbers(int rank, String playerUuid, String playerName, long totalScore, Map<String, ? extends Number> values) {
      LinkedHashMap<String, GlobalLeaderboardValue> mapped = new LinkedHashMap<>();
      if (values != null) {
         values.forEach((id, value) -> mapped.put(id, GlobalLeaderboardValue.number(value == null ? 0L : value.longValue())));
      }
      return new GlobalLeaderboardRow(rank, playerUuid, playerName, totalScore, mapped);
   }

   public static GlobalLeaderboardRow fromStrings(int rank, String playerUuid, String playerName, long totalScore, Map<String, String> values) {
      return fromText(rank, playerUuid, playerName, totalScore, values);
   }

   public static GlobalLeaderboardRow fromText(int rank, String playerUuid, String playerName, long totalScore, Map<String, String> values) {
      LinkedHashMap<String, GlobalLeaderboardValue> mapped = new LinkedHashMap<>();
      if (values != null) {
         values.forEach((id, value) -> mapped.put(id, GlobalLeaderboardValue.text(value)));
      }
      return new GlobalLeaderboardRow(rank, playerUuid, playerName, totalScore, mapped);
   }

   private static Map<String, GlobalLeaderboardValue> copyValues(Map<String, GlobalLeaderboardValue> values) {
      if (values == null || values.isEmpty()) {
         return Map.of();
      }

      LinkedHashMap<String, GlobalLeaderboardValue> mapped = new LinkedHashMap<>();
      values.forEach((id, value) -> {
         if (id != null && !id.isBlank()) {
            mapped.put(id, value == null ? GlobalLeaderboardValue.EMPTY : value);
         }
      });
      return Collections.unmodifiableMap(mapped);
   }
}

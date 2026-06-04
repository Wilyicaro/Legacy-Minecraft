package wily.legacy.api.client.leaderboards;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.network.chat.Component;

public record GlobalLeaderboardBoard(String providerId, String id, Component displayName, int order, List<GlobalLeaderboardColumn> columns) {
   public GlobalLeaderboardBoard(String providerId, String id, Component displayName, List<GlobalLeaderboardColumn> columns) {
      this(providerId, id, displayName, 1000, columns);
   }

   public GlobalLeaderboardBoard {
      providerId = cleanId(providerId, "providerId");
      id = cleanId(id, "id");
      Objects.requireNonNull(displayName, "displayName");
      columns = columns == null ? List.of() : List.copyOf(columns);
   }

   public String key() {
      return key(this.providerId, this.id);
   }

   public Map<String, GlobalLeaderboardColumn> columnsById() {
      LinkedHashMap<String, GlobalLeaderboardColumn> map = new LinkedHashMap<>();
      this.columns.forEach(column -> map.put(column.id(), column));
      return map;
   }

   public static String key(String providerId, String boardId) {
      return cleanId(providerId, "providerId") + "|" + cleanId(boardId, "boardId");
   }

   private static String cleanId(String id, String name) {
      Objects.requireNonNull(id, name);
      String value = id.trim();
      if (value.isEmpty()) {
         throw new IllegalArgumentException("Leaderboard " + name + " cannot be empty");
      }
      if (value.indexOf('|') >= 0) {
         throw new IllegalArgumentException("Leaderboard " + name + " cannot contain |");
      }
      return value;
   }
}

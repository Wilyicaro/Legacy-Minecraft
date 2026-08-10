package wily.legacy.api.client.leaderboards;

import java.util.List;

public record GlobalLeaderboardPage(boolean successful, List<GlobalLeaderboardRow> rows, boolean hasMore, String nextCursor, boolean hasPrevious, String previousCursor) {
   public GlobalLeaderboardPage(boolean successful, List<GlobalLeaderboardRow> rows) {
      this(successful, rows, false, "", false, "");
   }

   public GlobalLeaderboardPage(boolean successful, List<GlobalLeaderboardRow> rows, boolean hasMore, String nextCursor) {
      this(successful, rows, hasMore, nextCursor, false, "");
   }

   public GlobalLeaderboardPage {
      rows = rows == null ? List.of() : List.copyOf(rows);
      nextCursor = nextCursor == null ? "" : nextCursor;
      previousCursor = previousCursor == null ? "" : previousCursor;
   }

   public static GlobalLeaderboardPage successful(List<GlobalLeaderboardRow> rows) {
      return new GlobalLeaderboardPage(true, rows);
   }

   public static GlobalLeaderboardPage successful(List<GlobalLeaderboardRow> rows, boolean hasMore, String nextCursor) {
      return new GlobalLeaderboardPage(true, rows, hasMore, nextCursor, false, "");
   }

   public static GlobalLeaderboardPage successful(List<GlobalLeaderboardRow> rows, boolean hasMore, String nextCursor, boolean hasPrevious, String previousCursor) {
      return new GlobalLeaderboardPage(true, rows, hasMore, nextCursor, hasPrevious, previousCursor);
   }

   public static GlobalLeaderboardPage empty() {
      return new GlobalLeaderboardPage(true, List.of());
   }

   public static GlobalLeaderboardPage failed() {
      return new GlobalLeaderboardPage(false, List.of());
   }
}

package wily.legacy.api.client.leaderboards;

import java.util.List;

public record GlobalLeaderboardPage(boolean successful, List<GlobalLeaderboardRow> rows, boolean hasMore, String nextCursor, boolean hasPrevious, String previousCursor, int totalEntries) {
   public GlobalLeaderboardPage(boolean successful, List<GlobalLeaderboardRow> rows) {
      this(successful, rows, false, "", false, "", -1);
   }

   public GlobalLeaderboardPage(boolean successful, List<GlobalLeaderboardRow> rows, boolean hasMore, String nextCursor) {
      this(successful, rows, hasMore, nextCursor, false, "", -1);
   }

   public GlobalLeaderboardPage(boolean successful, List<GlobalLeaderboardRow> rows, boolean hasMore, String nextCursor, boolean hasPrevious, String previousCursor) {
      this(successful, rows, hasMore, nextCursor, hasPrevious, previousCursor, -1);
   }

   public GlobalLeaderboardPage {
      rows = rows == null ? List.of() : List.copyOf(rows);
      nextCursor = nextCursor == null ? "" : nextCursor;
      previousCursor = previousCursor == null ? "" : previousCursor;
      totalEntries = Math.max(-1, totalEntries);
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

   public static GlobalLeaderboardPage successful(List<GlobalLeaderboardRow> rows, boolean hasMore, String nextCursor, boolean hasPrevious, String previousCursor, int totalEntries) {
      return new GlobalLeaderboardPage(true, rows, hasMore, nextCursor, hasPrevious, previousCursor, totalEntries);
   }

   public static GlobalLeaderboardPage empty() {
      return new GlobalLeaderboardPage(true, List.of());
   }

   public static GlobalLeaderboardPage failed() {
      return new GlobalLeaderboardPage(false, List.of());
   }
}

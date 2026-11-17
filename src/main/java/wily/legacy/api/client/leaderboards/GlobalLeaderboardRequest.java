package wily.legacy.api.client.leaderboards;

import java.util.Objects;

public record GlobalLeaderboardRequest(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode, String playerUuid, String playerName, int aroundWindow, int topLimit, String cursor, int afterRank, String previousCursor, int beforeRank) {
   public GlobalLeaderboardRequest(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode, String playerUuid, String playerName, int aroundWindow, int topLimit) {
      this(board, viewMode, playerUuid, playerName, aroundWindow, topLimit, "", 0, "", 0);
   }

   public GlobalLeaderboardRequest(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode, String playerUuid, String playerName, int aroundWindow, int topLimit, String cursor, int afterRank) {
      this(board, viewMode, playerUuid, playerName, aroundWindow, topLimit, cursor, afterRank, "", 0);
   }

   public GlobalLeaderboardRequest {
      Objects.requireNonNull(board, "board");
      viewMode = viewMode == null ? GlobalLeaderboardViewMode.AROUND_ME : viewMode;
      playerUuid = playerUuid == null ? "" : playerUuid;
      playerName = playerName == null ? "" : playerName;
      cursor = cursor == null ? "" : cursor;
      afterRank = Math.max(0, afterRank);
      previousCursor = previousCursor == null ? "" : previousCursor;
      beforeRank = Math.max(0, beforeRank);
   }
}

package wily.legacy.client.screen.globalleaderboards.model;

import java.util.List;

public record GlobalLeaderboardBoardCache(String boardId, String displayNameKey, long fetchedAt, List<GlobalLeaderboardEntry> aroundEntries, List<GlobalLeaderboardEntry> topEntries, long aroundFetchedAt, long topFetchedAt) {
   public GlobalLeaderboardBoardCache(String boardId, String displayNameKey, long fetchedAt, List<GlobalLeaderboardEntry> aroundEntries, List<GlobalLeaderboardEntry> topEntries) {
      this(boardId, displayNameKey, fetchedAt, aroundEntries, topEntries, fetchedAt, fetchedAt);
   }

   public GlobalLeaderboardBoardCache {
      displayNameKey = displayNameKey == null ? boardId : displayNameKey;
      aroundEntries = aroundEntries == null ? List.of() : List.copyOf(aroundEntries);
      topEntries = topEntries == null ? List.of() : List.copyOf(topEntries);
   }

   public GlobalLeaderboardBoardCache withAroundEntries(List<GlobalLeaderboardEntry> entries, long timestamp) {
      return new GlobalLeaderboardBoardCache(this.boardId, this.displayNameKey, timestamp, entries, this.topEntries, timestamp, this.topFetchedAt);
   }

   public GlobalLeaderboardBoardCache withTopEntries(List<GlobalLeaderboardEntry> entries, long timestamp) {
      return new GlobalLeaderboardBoardCache(this.boardId, this.displayNameKey, timestamp, this.aroundEntries, entries, this.aroundFetchedAt, timestamp);
   }
}

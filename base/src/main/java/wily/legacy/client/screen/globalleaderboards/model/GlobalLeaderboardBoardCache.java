package wily.legacy.client.screen.globalleaderboards.model;

import java.util.List;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardBoard;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardRow;
import wily.legacy.api.client.leaderboards.LegacyLeaderboards;

public record GlobalLeaderboardBoardCache(String providerId, String boardId, String displayNameKey, long fetchedAt, List<GlobalLeaderboardRow> aroundEntries, List<GlobalLeaderboardRow> topEntries, long aroundFetchedAt, long topFetchedAt, int totalEntries) {
   public GlobalLeaderboardBoardCache(String providerId, String boardId, String displayNameKey, long fetchedAt, List<GlobalLeaderboardRow> aroundEntries, List<GlobalLeaderboardRow> topEntries) {
      this(providerId, boardId, displayNameKey, fetchedAt, aroundEntries, topEntries, fetchedAt, fetchedAt, -1);
   }

   public GlobalLeaderboardBoardCache(String boardId, String displayNameKey, long fetchedAt, List<GlobalLeaderboardRow> aroundEntries, List<GlobalLeaderboardRow> topEntries) {
      this(LegacyLeaderboards.LEGACY_PROVIDER, boardId, displayNameKey, fetchedAt, aroundEntries, topEntries, fetchedAt, fetchedAt, -1);
   }

   public GlobalLeaderboardBoardCache(String providerId, String boardId, String displayNameKey, long fetchedAt, List<GlobalLeaderboardRow> aroundEntries, List<GlobalLeaderboardRow> topEntries, long aroundFetchedAt, long topFetchedAt) {
      this(providerId, boardId, displayNameKey, fetchedAt, aroundEntries, topEntries, aroundFetchedAt, topFetchedAt, -1);
   }

   public GlobalLeaderboardBoardCache {
      providerId = providerId == null || providerId.isBlank() ? LegacyLeaderboards.LEGACY_PROVIDER : providerId;
      displayNameKey = displayNameKey == null ? boardId : displayNameKey;
      aroundEntries = aroundEntries == null ? List.of() : List.copyOf(aroundEntries);
      topEntries = topEntries == null ? List.of() : List.copyOf(topEntries);
      totalEntries = Math.max(-1, totalEntries);
   }

   public String key() {
      return GlobalLeaderboardBoard.key(this.providerId, this.boardId);
   }

   public GlobalLeaderboardBoardCache withAroundEntries(List<GlobalLeaderboardRow> entries, long timestamp) {
      return this.withAroundEntries(entries, timestamp, this.totalEntries);
   }

   public GlobalLeaderboardBoardCache withAroundEntries(List<GlobalLeaderboardRow> entries, long timestamp, int totalEntries) {
      return new GlobalLeaderboardBoardCache(this.providerId, this.boardId, this.displayNameKey, timestamp, entries, this.topEntries, timestamp, this.topFetchedAt, totalEntries);
   }

   public GlobalLeaderboardBoardCache withTopEntries(List<GlobalLeaderboardRow> entries, long timestamp) {
      return this.withTopEntries(entries, timestamp, this.totalEntries);
   }

   public GlobalLeaderboardBoardCache withTopEntries(List<GlobalLeaderboardRow> entries, long timestamp, int totalEntries) {
      return new GlobalLeaderboardBoardCache(this.providerId, this.boardId, this.displayNameKey, timestamp, this.aroundEntries, entries, this.aroundFetchedAt, timestamp, totalEntries);
   }
}

package wily.legacy.client.screen.globalleaderboards.model;

import wily.legacy.api.client.leaderboards.GlobalLeaderboardBoard;
import wily.legacy.api.client.leaderboards.GlobalLeaderboardRow;
import wily.legacy.api.client.leaderboards.LegacyLeaderboards;

import java.util.List;

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
        return GlobalLeaderboardBoard.key(providerId, boardId);
    }

    public GlobalLeaderboardBoardCache withAroundEntries(List<GlobalLeaderboardRow> entries, long timestamp) {
        return withAroundEntries(entries, timestamp, totalEntries);
    }

    public GlobalLeaderboardBoardCache withAroundEntries(List<GlobalLeaderboardRow> entries, long timestamp, int totalEntries) {
        return new GlobalLeaderboardBoardCache(providerId, boardId, displayNameKey, timestamp, entries, topEntries, timestamp, topFetchedAt, totalEntries);
    }

    public GlobalLeaderboardBoardCache withTopEntries(List<GlobalLeaderboardRow> entries, long timestamp) {
        return withTopEntries(entries, timestamp, totalEntries);
    }

    public GlobalLeaderboardBoardCache withTopEntries(List<GlobalLeaderboardRow> entries, long timestamp, int totalEntries) {
        return new GlobalLeaderboardBoardCache(providerId, boardId, displayNameKey, timestamp, aroundEntries, entries, aroundFetchedAt, timestamp, totalEntries);
    }
}

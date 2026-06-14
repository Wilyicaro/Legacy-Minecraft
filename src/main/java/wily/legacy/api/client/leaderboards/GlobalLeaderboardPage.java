package wily.legacy.api.client.leaderboards;

import java.util.List;

public record GlobalLeaderboardPage(boolean successful, List<GlobalLeaderboardRow> rows) {
    public GlobalLeaderboardPage {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public static GlobalLeaderboardPage successful(List<GlobalLeaderboardRow> rows) {
        return new GlobalLeaderboardPage(true, rows);
    }

    public static GlobalLeaderboardPage empty() {
        return new GlobalLeaderboardPage(true, List.of());
    }

    public static GlobalLeaderboardPage failed() {
        return new GlobalLeaderboardPage(false, List.of());
    }
}

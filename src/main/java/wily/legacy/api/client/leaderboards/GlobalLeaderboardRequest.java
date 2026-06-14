package wily.legacy.api.client.leaderboards;

import java.util.Objects;

public record GlobalLeaderboardRequest(GlobalLeaderboardBoard board, GlobalLeaderboardViewMode viewMode, String playerUuid, String playerName, int aroundWindow, int topLimit) {
    public GlobalLeaderboardRequest {
        Objects.requireNonNull(board, "board");
        viewMode = viewMode == null ? GlobalLeaderboardViewMode.AROUND_ME : viewMode;
        playerUuid = playerUuid == null ? "" : playerUuid;
        playerName = playerName == null ? "" : playerName;
    }
}

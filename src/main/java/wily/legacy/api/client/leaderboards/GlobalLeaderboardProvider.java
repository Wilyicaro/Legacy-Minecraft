package wily.legacy.api.client.leaderboards;

import java.util.concurrent.CompletableFuture;

public interface GlobalLeaderboardProvider {
    String id();

    CompletableFuture<GlobalLeaderboardPage> fetch(GlobalLeaderboardRequest request);
}

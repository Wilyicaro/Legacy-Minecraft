package wily.legacy.client.screen.globalleaderboards;

import wily.legacy.client.LegacyOptions;

public final class GlobalLeaderboardsConfig {
    private GlobalLeaderboardsConfig() {
    }

    public static Values get() {
        String endpoint = LegacyOptions.globalLeaderboardsEndpoint.get();
        return new Values(
                LegacyOptions.globalLeaderboardsOptOut.get(),
                endpoint == null ? "" : endpoint.trim(),
                LegacyOptions.globalLeaderboardsSyncOnLaunch.get(),
                LegacyOptions.globalLeaderboardsPrefetchAroundOnLaunch.get(),
                LegacyOptions.globalLeaderboardsPrefetchTopOnLaunch.get(),
                LegacyOptions.globalLeaderboardsAroundWindow.get(),
                LegacyOptions.globalLeaderboardsTopLimit.get(),
                LegacyOptions.globalLeaderboardsSyncCooldownSeconds.get(),
                LegacyOptions.globalLeaderboardsFetchCooldownSeconds.get(),
                LegacyOptions.globalLeaderboardsConnectTimeoutSeconds.get(),
                LegacyOptions.globalLeaderboardsReadTimeoutSeconds.get());
    }

    public record Values(boolean optedOut, String endpoint, boolean syncOnLaunch, boolean prefetchAroundOnLaunch, boolean prefetchTopOnLaunch, int aroundWindow, int topLimit, int syncCooldownSeconds, int fetchCooldownSeconds, int connectTimeoutSeconds, int readTimeoutSeconds) {
        public Values {
            endpoint = endpoint == null ? "" : endpoint.trim();
            aroundWindow = clamp(aroundWindow, 1, 10);
            topLimit = clamp(topLimit, 1, 100);
            syncCooldownSeconds = Math.max(3600, syncCooldownSeconds);
            fetchCooldownSeconds = Math.max(300, fetchCooldownSeconds);
            connectTimeoutSeconds = clamp(connectTimeoutSeconds, 1, 30);
            readTimeoutSeconds = clamp(readTimeoutSeconds, 1, 30);
        }

        public boolean hasEndpoint() {
            return !optedOut && !endpoint.isBlank();
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}

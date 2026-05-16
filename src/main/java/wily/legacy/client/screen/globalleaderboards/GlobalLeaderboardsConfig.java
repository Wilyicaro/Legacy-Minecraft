package wily.legacy.client.screen.globalleaderboards;

import wily.legacy.client.LegacyOptions;

public final class GlobalLeaderboardsConfig {
   private GlobalLeaderboardsConfig() {
   }

   public static GlobalLeaderboardsConfig.Values get() {
      String endpoint = LegacyOptions.globalLeaderboardsEndpoint.get();
      return new GlobalLeaderboardsConfig.Values(
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

   public record Values(String endpoint, boolean syncOnLaunch, boolean prefetchAroundOnLaunch, boolean prefetchTopOnLaunch, int aroundWindow, int topLimit, int syncCooldownSeconds, int fetchCooldownSeconds, int connectTimeoutSeconds, int readTimeoutSeconds) {
      public boolean hasEndpoint() {
         return !this.endpoint.isBlank();
      }
   }
}

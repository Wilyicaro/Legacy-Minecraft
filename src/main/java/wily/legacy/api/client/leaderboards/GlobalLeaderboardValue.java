package wily.legacy.api.client.leaderboards;

public record GlobalLeaderboardValue(long number, String text) {
    public static final GlobalLeaderboardValue EMPTY = new GlobalLeaderboardValue(0L, "");

    public GlobalLeaderboardValue {
        text = text == null ? "" : text;
    }

    public static GlobalLeaderboardValue number(long number) {
        return new GlobalLeaderboardValue(number, "");
    }

    public static GlobalLeaderboardValue text(String text) {
        return new GlobalLeaderboardValue(0L, text);
    }

    public static GlobalLeaderboardValue of(long number, String text) {
        return new GlobalLeaderboardValue(number, text);
    }

    public boolean hasText() {
        return !text.isBlank();
    }
}

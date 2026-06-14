package wily.legacy.api.client.leaderboards;

import net.minecraft.network.chat.Component;

import java.util.Objects;

public record GlobalLeaderboardColumn(String id, Component displayName, GlobalLeaderboardIcon icon, ValueFormatter formatter) {
    public GlobalLeaderboardColumn(String id, Component displayName, GlobalLeaderboardIcon icon) {
        this(id, displayName, icon, ValueFormatter.number());
    }

    public GlobalLeaderboardColumn {
        id = cleanId(id);
        Objects.requireNonNull(displayName, "displayName");
        icon = icon == null ? GlobalLeaderboardIcon.empty() : icon;
        formatter = formatter == null ? ValueFormatter.number() : formatter;
    }

    public Component format(GlobalLeaderboardValue value) {
        GlobalLeaderboardValue resolved = value == null ? GlobalLeaderboardValue.EMPTY : value;
        return resolved.hasText() ? Component.literal(resolved.text()) : formatter.format(resolved.number());
    }

    public Component format(long value) {
        return formatter.format(value);
    }

    private static String cleanId(String id) {
        Objects.requireNonNull(id, "id");
        String value = id.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Leaderboard column id cannot be empty");
        }
        return value;
    }

    @FunctionalInterface
    public interface ValueFormatter {
        Component format(long value);

        static ValueFormatter number() {
            return value -> Component.literal(Long.toString(value));
        }
    }
}

package wily.legacy.api.client.leaderboards;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;

import java.util.Locale;

public enum GlobalLeaderboardDifficulty {
    PEACEFUL,
    EASY,
    NORMAL,
    HARD,
    HARDCORE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Component displayName() {
        return switch (this) {
            case PEACEFUL -> Difficulty.PEACEFUL.getDisplayName();
            case EASY -> Difficulty.EASY.getDisplayName();
            case HARD -> Difficulty.HARD.getDisplayName();
            case HARDCORE -> Component.translatable("selectWorld.gameMode.hardcore");
            default -> Difficulty.NORMAL.getDisplayName();
        };
    }

    public String boardId(String boardId) {
        return boardId + "." + id();
    }

    public static GlobalLeaderboardDifficulty byId(String id) {
        for (GlobalLeaderboardDifficulty difficulty : values()) {
            if (difficulty.id().equals(id)) {
                return difficulty;
            }
        }
        return NORMAL;
    }

    public static GlobalLeaderboardDifficulty of(Difficulty difficulty) {
        if (difficulty == null) {
            return NORMAL;
        }
        return switch (difficulty) {
            case PEACEFUL -> PEACEFUL;
            case EASY -> EASY;
            case HARD -> HARD;
            default -> NORMAL;
        };
    }

    public static GlobalLeaderboardDifficulty of(Difficulty difficulty, boolean hardcore) {
        return hardcore ? HARDCORE : of(difficulty);
    }
}

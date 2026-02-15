package wily.legacy.CustomModelSkins.cpl.util;

public enum Hand {
    RIGHT, LEFT;

    public static <T extends Enum<T>> Hand of(T value) {
        return value == null ? null : (value.name().equals("RIGHT") ? RIGHT : LEFT);
    }

    public static <T extends Enum<T>> Hand of(Hand main, T value) {
        return value == null ? null : (value.name().equals("MAIN_HAND") ? main : (main == RIGHT ? LEFT : RIGHT));
    }
}

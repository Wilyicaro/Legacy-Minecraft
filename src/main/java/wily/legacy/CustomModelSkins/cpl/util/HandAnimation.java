package wily.legacy.CustomModelSkins.cpl.util;

public enum HandAnimation {
    NONE, EAT, DRINK, BLOCK, BOW, SPEAR, CROSSBOW, SPYGLASS,
    ;
    public static final HandAnimation[] VALUES = values();

    public static <T extends Enum<T>> HandAnimation of(T value) {
        String name = value.name();
        for (int i = 0; i < VALUES.length; i++) {
            HandAnimation e = VALUES[i];
            if (e.name().equalsIgnoreCase(name)) return e;
        }
        return NONE;
    }
}

package wily.legacy.skins.client.render.boxloader;

import java.util.Locale;

public enum AttachSlot {
    HEAD,
    HAT,
    BODY,
    JACKET,
    RIGHT_ARM,
    LEFT_ARM,
    RIGHT_SLEEVE,
    LEFT_SLEEVE,
    RIGHT_LEG,
    LEFT_LEG,
    RIGHT_PANTS,
    LEFT_PANTS;
    public static AttachSlot fromString(String key) {
        if (key == null) return null;
        String k = key.trim();
        if (k.isEmpty()) return null;
        k = k.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (k.equals("arm0") || k.equals("rightarm") || k.equals("right_arm") || k.equals("rarm")) return RIGHT_ARM;
        if (k.equals("arm1") || k.equals("leftarm") || k.equals("left_arm") || k.equals("larm")) return LEFT_ARM;
        if (k.equals("leg0") || k.equals("rightleg") || k.equals("right_leg") || k.equals("rleg")) return RIGHT_LEG;
        if (k.equals("leg1") || k.equals("leftleg") || k.equals("left_leg") || k.equals("lleg")) return LEFT_LEG;
        if (k.equals("sleeve0") || k.equals("right_sleeve") || k.equals("rightsleeve")) return RIGHT_SLEEVE;
        if (k.equals("sleeve1") || k.equals("left_sleeve") || k.equals("leftsleeve")) return LEFT_SLEEVE;
        if (k.equals("pants0") || k.equals("right_pants") || k.equals("rightpants")) return RIGHT_PANTS;
        if (k.equals("pants1") || k.equals("left_pants") || k.equals("leftpants")) return LEFT_PANTS;
        if (k.equals("head")) return HEAD;
        if (k.equals("hat")) return HAT;
        if (k.equals("body")) return BODY;
        if (k.equals("jacket")) return JACKET;
        try {
            return AttachSlot.valueOf(k.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) { return null; }
    }
}

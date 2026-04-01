package wily.legacy.Skins.client.util;

import net.minecraft.client.gui.Font;

public final class SkinTextUtil {
    private SkinTextUtil() {}

    public static String clip(Font font, String text, int maxWidth) {
        String value = text == null ? "" : text.replace("\u00E2\u20AC\u00A6", "...");
        if (font == null || maxWidth <= 0 || value.isBlank() || font.width(value) <= maxWidth) return value;
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("..."))) + "...";
    }
}

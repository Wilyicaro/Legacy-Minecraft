package wily.legacy.util.client;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import wily.legacy.Legacy4J;
import wily.legacy.client.LegacyOptions;

import java.util.function.Consumer;

public class LegacyFontUtil {
    public static final FontDescription MOJANGLES_11_FONT = new FontDescription.Resource(Legacy4J.createModLocation("default_11"));
    public static final Style MOJANGLES_11_STYLE = Style.EMPTY.withFont(MOJANGLES_11_FONT);
    public static final Style DEFAULT_FONT_STYLE = Style.EMPTY.withFont(FontDescription.DEFAULT);
    private static boolean legacyFont = true;
    public static boolean forceVanillaFontShadowColor = false;
    public static FontDescription defaultFontOverride = null;


    public static void applyFontOverrideIf(boolean b, FontDescription override, Consumer<Boolean> fontRender) {
        if (b) defaultFontOverride = override;
        fontRender.accept(b);
        if (b) defaultFontOverride = null;
    }

    public static void applyFontOverride(FontDescription override, Consumer<Boolean> fontRender) {
        applyFontOverrideIf(override != null, override, fontRender);
    }

    public static void applySmallerFont(FontDescription override, Consumer<Boolean> fontRender) {
        applyFontOverrideIf(LegacyOptions.getUIMode().isHDOrLower(), override, fontRender);
    }

    public static void applySDFont(Consumer<Boolean> fontRender) {
        LegacyFontUtil.applyDefault11If(LegacyOptions.getUIMode().isSD(), fontRender);
    }

    public static void applyDefault11If(boolean b, Consumer<Boolean> fontRender) {
        LegacyFontUtil.applyFontOverrideIf(b, LegacyFontUtil.MOJANGLES_11_FONT, fontRender);
    }

    public static boolean hasLegacyFont() {
        return legacyFont && LegacyOptions.legacyFont.get();
    }

    public static void enableLegacyFont() {
        legacyFont = true;
    }

    public static void disableLegacyFont() {
        legacyFont = false;
    }
}
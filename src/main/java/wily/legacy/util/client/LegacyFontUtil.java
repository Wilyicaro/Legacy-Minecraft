package wily.legacy.util.client;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;

import java.util.function.Consumer;

public class LegacyFontUtil {
    public static final FontDescription MOJANGLES_11_FONT = new FontDescription.Resource(Legacy4J.createModLocation("default_11"));
    public static boolean legacyFont = true;
    public static boolean forceVanillaFontShadowColor = false;
    public static FontDescription defaultFontOverride = null;

    public static void applyFontOverrideIf(boolean b, FontDescription override, Consumer<Boolean> fontRender) {
        if (b) defaultFontOverride = override;
        fontRender.accept(b);
        if (b) defaultFontOverride = null;
    }
}

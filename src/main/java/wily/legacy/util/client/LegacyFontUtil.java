package wily.legacy.util.client;

import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;

import java.util.function.Consumer;

public class LegacyFontUtil {
    public static final ResourceLocation MOJANGLES_11_FONT = Legacy4J.createModLocation("default_11");
    public static boolean legacyFont = true;
    public static boolean forceVanillaFontShadowColor = false;
    public static ResourceLocation defaultFontOverride = null;

    public static void applyFontOverrideIf(boolean b, ResourceLocation override, Consumer<Boolean> fontRender) {
        if (b) defaultFontOverride = override;
        fontRender.accept(b);
        if (b) defaultFontOverride = null;
    }
}

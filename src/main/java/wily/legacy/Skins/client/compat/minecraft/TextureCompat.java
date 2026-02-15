package wily.legacy.Skins.client.compat.minecraft;

import wily.legacy.Skins.util.ReflectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;

public final class TextureCompat {

    private static final Method MISSING_LOC;

    static {
        Method m;
        try {
            Class<?> cls = Class.forName("net.minecraft.client.renderer.texture.MissingTextureAtlasSprite");
            m = ReflectionUtil.findMethod(cls, "getLocation");
        } catch (Throwable t) {
            m = null;
        }
        MISSING_LOC = m;
    }

    private TextureCompat() {
    }

    public static boolean isMissingTexture(ResourceLocation id) {
        if (id == null) return true;
        try {
            Minecraft mc = Minecraft.getInstance();
            TextureManager tm = mc.getTextureManager();
            AbstractTexture tex = tm.getTexture(id);
            if (tex == null) return true;

            ResourceLocation missing = null;
            try {
                Object r = ReflectionUtil.invoke(MISSING_LOC, null);
                if (r instanceof ResourceLocation rl) missing = rl;
            } catch (Throwable ignored) {
            }
            if (missing != null && id.equals(missing)) return true;

            String cn = tex.getClass().getName().toLowerCase(java.util.Locale.ROOT);
            return cn.contains("missingtexture");
        } catch (Throwable ignored) {
            return false;
        }
    }
}

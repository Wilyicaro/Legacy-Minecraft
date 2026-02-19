package wily.legacy.CustomModelSkins.cpm.util;

import net.minecraft.resources.ResourceLocation;

public final class IdUtil {
    private IdUtil() {
    }

    public static ResourceLocation parse(String s) {
        if (s == null) return null;
        int i = s.indexOf(':');
        if (i < 0) {
            if (s.isEmpty()) return null;
            return ResourceLocation.fromNamespaceAndPath("minecraft", s);
        }
        String ns = s.substring(0, i);
        String path = s.substring(i + 1);
        if (ns.isEmpty() || path.isEmpty()) return null;
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }
}

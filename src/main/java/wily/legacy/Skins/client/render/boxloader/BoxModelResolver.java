package wily.legacy.Skins.client.render.boxloader;

import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;
import wily.legacy.Skins.skin.SkinEntry;

public final class BoxModelResolver {
    private BoxModelResolver() {
    }

    public static ResourceLocation resolveModelId(String skinId, SkinEntry entry) {
        String key = normalizeKey(skinId);
        if (key == null && entry != null && entry.texture() != null) {
            String p = entry.texture().getPath();
            int slash = p.lastIndexOf('/');
            String file = slash >= 0 ? p.substring(slash + 1) : p;
            if (file.toLowerCase().endsWith(".png")) file = file.substring(0, file.length() - 4);
            key = normalizeKey(file);
        }
        if (key == null) return null;
        return ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID, key);
    }

    private static String normalizeKey(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase();
        if (t.isEmpty()) return null;
        StringBuilder out = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-') {
                out.append(c);
            } else {
                out.append('_');
            }
        }
        String r = out.toString();
        while (r.contains("__")) r = r.replace("__", "_");
        if (r.startsWith("_")) r = r.substring(1);
        if (r.endsWith("_")) r = r.substring(0, r.length() - 1);
        return r.isEmpty() ? null : r;
    }
}

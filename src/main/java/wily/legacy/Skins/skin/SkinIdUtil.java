package wily.legacy.Skins.skin;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class SkinIdUtil {
    public static final String PACK_DEFAULT = "default";
    public static final String PACK_FAVOURITES = "favourites";
    public static final String AUTO_SELECT = "auto_select";
    private SkinIdUtil() { }
    public static boolean hasSkin(String skinId) { return skinId != null && !skinId.isBlank(); }
    public static boolean isAutoSelect(String skinId) { return AUTO_SELECT.equals(skinId); }
    public static boolean isBlankOrAutoSelect(String skinId) { return !hasSkin(skinId) || isAutoSelect(skinId); }
    public static String normalize(String skinId) { return skinId == null ? "" : skinId; }
    public static boolean isFavouritesPack(String packId) { return PACK_FAVOURITES.equals(packId); }

    public static String uniqueLoadedSkinId(String packId, String sourceId, Set<String> usedIds) {
        String base = normalize(sourceId);
        if (!hasSkin(base)) base = hasSkin(packId) ? packId : "skin";
        if (usedIds == null || !usedIds.contains(base)) return base;

        String scoped = hasSkin(packId) ? packId + "_" + base : base;
        if (!usedIds.contains(scoped)) return scoped;

        for (int i = 2; ; i++) {
            String candidate = scoped + "_" + i;
            if (!usedIds.contains(candidate)) return candidate;
        }
    }

    public static ResourceLocation modelId(String namespace, String skinId) {
        if (!hasSkin(namespace) || !hasSkin(skinId)) return null;
        return ResourceLocation.fromNamespaceAndPath(namespace, skinId);
    }

    public static ResourceLocation modelLocation(ResourceLocation texture) {
        if (texture == null) return null;
        String path = texture.getPath();
        int slash = path.lastIndexOf('/');
        if (slash < 0) return null;

        String folder = path.substring(0, slash);
        if (!folder.endsWith("/skins")) return null;

        String name = path.substring(slash + 1);
        if (!name.endsWith(".png")) return null;

        String boxFolder = folder.substring(0, folder.length() - "/skins".length()) + "/box_models/";
        String boxName = name.substring(0, name.length() - 4) + ".json";
        return ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), boxFolder + boxName);
    }
}

package wily.legacy.skins.skin;

import wily.legacy.client.LegacyOptions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public final class SkinDataStore {
    private static final Object LOCK = new Object();

    private SkinDataStore() {
    }

    public static String getSelectedSkin(UUID userId) {
        if (userId == null) return "";
        synchronized (LOCK) {
            return userId.toString().equals(LegacyOptions.selectedSkinUserId.get())
                    ? SkinIdUtil.normalize(LegacyOptions.selectedSkinId.get())
                    : "";
        }
    }

    public static String getSelectedPack(UUID userId) {
        if (userId == null) return null;
        synchronized (LOCK) {
            if (!userId.toString().equals(LegacyOptions.selectedSkinUserId.get())) return null;
            return SkinIdUtil.trimToNull(LegacyOptions.selectedSkinPackId.get());
        }
    }

    public static void setSelectedSkin(UUID userId, String skinId) {
        setSelectedSkin(userId, skinId, null);
    }

    public static void setSelectedSkin(UUID userId, String skinId, String packId) {
        if (userId == null) return;
        synchronized (LOCK) {
            LegacyOptions.selectedSkinUserId.set(userId.toString());
            LegacyOptions.selectedSkinId.set(SkinIdUtil.normalize(skinId));
            String id = SkinIdUtil.trimToNull(packId);
            LegacyOptions.selectedSkinPackId.set(id == null ? "" : id);
            LegacyOptions.CLIENT_STORAGE.save();
        }
    }

    public static boolean isFavorite(String skinId) {
        String id = SkinIdUtil.normalize(skinId);
        if (!SkinIdUtil.hasSkin(id)) return false;
        synchronized (LOCK) {
            return favoriteIds().contains(id);
        }
    }

    public static List<String> getFavorites() {
        synchronized (LOCK) {
            return List.copyOf(favoriteIds());
        }
    }

    public static void toggleFavorite(String skinId) {
        String id = SkinIdUtil.normalize(skinId);
        if (!SkinIdUtil.hasSkin(id)) return;
        synchronized (LOCK) {
            LinkedHashSet<String> ids = favoriteIds();
            if (!ids.remove(id)) ids.add(id);
            LegacyOptions.favoriteSkinIds.set(List.copyOf(ids));
            LegacyOptions.CLIENT_STORAGE.save();
        }
    }

    private static LinkedHashSet<String> favoriteIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String value : LegacyOptions.favoriteSkinIds.get()) {
            String id = SkinIdUtil.normalize(value);
            if (SkinIdUtil.hasSkin(id)) ids.add(id);
        }
        return ids;
    }
}

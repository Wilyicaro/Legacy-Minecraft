package wily.legacy.Skins.skin;

import wily.legacy.Skins.util.SkinPaths;
import java.nio.file.Path;
import java.util.*;

public final class FavoritesStore {
    private static final List<String> ORDERED = new ArrayList<>();
    private static final Set<String> SET = new HashSet<>();
    private static volatile boolean loaded;
    private static final Object LOCK = new Object();
    private FavoritesStore() { }
    public static void ensureLoaded() {
        if (loaded) return;
        synchronized (LOCK) { if (!loaded) loadLocked(); }
    }
    public static boolean isFavorite(String id) {
        if (id == null) return false;
        ensureLoaded();
        synchronized (LOCK) { return SET.contains(id); }
    }
    public static List<String> getFavorites() {
        ensureLoaded();
        synchronized (LOCK) { return Collections.unmodifiableList(new ArrayList<>(ORDERED)); }
    }
    public static void toggle(String id) {
        if (id == null || id.isBlank()) return;
        ensureLoaded();
        synchronized (LOCK) {
            if (SET.contains(id)) {
                SET.remove(id);
                ORDERED.removeIf(s -> s.equals(id));
            } else {
                SET.add(id);
                ORDERED.add(id);
            }
        }
        save();
    }
    private static Path filePath() { return SkinPaths.resolve("favourites.json"); }
    private static void loadLocked() {
        ORDERED.clear();
        SET.clear();
        for (String id : SkinJsonStore.read(filePath(), "favourites")) {
            if (SET.add(id)) { ORDERED.add(id); }
        }
        loaded = true;
    }
    private static void save() {
        ArrayList<String> values;
        synchronized (LOCK) { values = new ArrayList<>(ORDERED); }
        SkinJsonStore.write(filePath(), "favourites", values);
    }
}

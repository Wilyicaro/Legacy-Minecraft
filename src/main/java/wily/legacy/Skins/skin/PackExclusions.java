package wily.legacy.Skins.skin;

import wily.legacy.Skins.util.SkinPaths;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class PackExclusions {
    private static final Set<String> EXCLUDED = new HashSet<>();
    private static volatile boolean loaded;
    private static final Object LOCK = new Object();
    private PackExclusions() { }
    public static void ensureLoaded() {
        if (loaded) return;
        synchronized (LOCK) { if (!loaded) loadLocked(); }
    }
    public static void reload() {
        synchronized (LOCK) {
            loaded = false;
            loadLocked();
        }
    }
    public static boolean isExcluded(String packId) {
        if (packId == null) return false;
        ensureLoaded();
        synchronized (LOCK) { return EXCLUDED.contains(packId); }
    }
    public static Set<String> getExcluded() {
        ensureLoaded();
        synchronized (LOCK) { return Collections.unmodifiableSet(new HashSet<>(EXCLUDED)); }
    }
    public static void setExcluded(String packId, boolean excluded) {
        if (packId == null || packId.isBlank()) return;
        ensureLoaded();
        synchronized (LOCK) {
            if (excluded) EXCLUDED.add(packId);
            else EXCLUDED.remove(packId);
        }
        save();
    }
    private static Path filePath() { return SkinPaths.resolve("pack_exclusions.json"); }
    private static void loadLocked() {
        EXCLUDED.clear();
        for (String id : SkinJsonStore.read(filePath(), "excluded")) { EXCLUDED.add(id); }
        loaded = true;
    }
    private static void save() {
        HashSet<String> values;
        synchronized (LOCK) { values = new HashSet<>(EXCLUDED); }
        SkinJsonStore.write(filePath(), "excluded", values);
    }
}

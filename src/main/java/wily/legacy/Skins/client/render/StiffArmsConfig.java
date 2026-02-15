package wily.legacy.Skins.client.render;

import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class StiffArmsConfig {
    private static final String FILE_NAME = "consoleskins_stiff_arms.txt";
    private static final Set<String> DEFAULT_IDS;
    private static volatile long lastLoadMs;
    private static volatile Set<String> ids = Collections.emptySet();

    private StiffArmsConfig() {
    }

    static {
        Set<String> d = new HashSet<>();
        DEFAULT_IDS = Collections.unmodifiableSet(d);
    }

    public static boolean isStiffArmsSkin(String id) {
        if (id == null || id.isBlank()) return false;
        reloadIfNeeded();
        return ids.contains(id);
    }

    public static void reloadNow() {
        ids = loadIds();
        lastLoadMs = System.currentTimeMillis();
    }

    private static void reloadIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastLoadMs < 2000) return;
        reloadNow();
    }

    private static Set<String> loadIds() {
        try {
            File dir = Minecraft.getInstance().gameDirectory;
            File cfg = new File(dir, "config");
            File f = new File(cfg, FILE_NAME);
            Set<String> out = new HashSet<>(DEFAULT_IDS);
            if (!f.exists()) return out;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    out.add(line);
                }
            }
            return out;
        } catch (Throwable ignored) {
            return DEFAULT_IDS;
        }
    }
}

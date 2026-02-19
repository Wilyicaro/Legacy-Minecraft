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
public final class ViewBobbingConfig {
    private static final String FILE_NAME = "consoleskins_disable_view_bobbing.txt";
    private static final Set<String> DEFAULT_IDS = Set.of();

    private static volatile long lastLoadMs;
    private static volatile Set<String> ids = Collections.emptySet();

    private ViewBobbingConfig() {
    }

    public static boolean isViewBobbingDisabled(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;
        reloadIfNeeded();
        return ids.contains(skinId)
                || SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.DISABLE_VIEW_BOBBING, skinId)
                || StiffLegsConfig.isStiffLegsSkin(skinId);
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

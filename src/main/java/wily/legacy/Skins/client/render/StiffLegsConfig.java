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

public final class StiffLegsConfig {
    private static final String FILE_NAME = "consoleskins_stiff_legs.txt";
    private static final Set<String> DEFAULT_IDS;
    private static volatile long lastLoadMs;
    private static volatile Set<String> ids = Collections.emptySet();

    static {
        Set<String> d = new HashSet<>();

        d.add("cpm:legacy_skinpacks:skinpacks/festive_pack/marleysghost.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_charity/ghost4jstudios.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/halloween_mashup/ghost.cpmmodel");
        d.add("cpm:legacy_skinpacks:skinpacks/festive_mashup/skier.cpmmodel");
        d.add("legacy_skinpacks:skinpacks/festive_mashup/skier.cpmmodel");
        DEFAULT_IDS = Collections.unmodifiableSet(d);
    }

    private StiffLegsConfig() {
    }

    public static boolean isStiffLegsSkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;
        reloadIfNeeded();
        return ids.contains(skinId) || SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.STIFF_LEGS, skinId);
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

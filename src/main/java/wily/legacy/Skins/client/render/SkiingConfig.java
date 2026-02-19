package wily.legacy.Skins.client.render;

import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SkiingConfig {
    private static final String CFG_FILE = "consoleskins_skiing.txt";

    private static final Set<String> DEFAULTS;

    static {
        Set<String> d = new HashSet<>();
        d.add("cpm:legacy_skinpacks:skinpacks/festive_mashup/skier.cpmmodel");
        DEFAULTS = Collections.unmodifiableSet(d);
    }

    private static volatile Set<String> fileSet;
    private static volatile long lastLoadMs;

    private SkiingConfig() {
    }

    public static boolean isSkiingSkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;
        String id = normalize(skinId);
        if (id == null) return false;
        if (DEFAULTS.contains(id) || SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.SKIING, id)) return true;
        Set<String> f = fileSet;
        if (f == null || (System.currentTimeMillis() - lastLoadMs) > 3_000L) reloadQuiet();
        f = fileSet;
        return (f != null && f.contains(id)) || SkinPoseRegistry.has(SkinPoseRegistry.PoseTag.SKIING, id);
    }

    public static void reloadQuiet() {
        try {
            Minecraft mc = Minecraft.getInstance();
            Path dir = mc.gameDirectory != null ? mc.gameDirectory.toPath() : null;
            if (dir == null) return;
            Path cfg = dir.resolve("config").resolve(CFG_FILE);
            Set<String> out = new HashSet<>();
            if (Files.isRegularFile(cfg)) {
                try (BufferedReader br = Files.newBufferedReader(cfg, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = normalize(line);
                        if (line == null || line.isEmpty() || line.startsWith("#")) continue;
                        out.add(line);
                    }
                }
            }
            fileSet = out.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(out);
            lastLoadMs = System.currentTimeMillis();
        } catch (Throwable ignored) {
            lastLoadMs = System.currentTimeMillis();
        }
    }

    private static String normalize(String in) {
        if (in == null) return null;
        String s = in.trim();
        if (s.isEmpty()) return null;
        if (s.endsWith(".cpmmodel") && !s.startsWith("cpm:")) s = "cpm:" + s;
        return s;
    }
}

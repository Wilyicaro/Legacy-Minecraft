package wily.legacy.Skins.client.render;

import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class BadSantaSitConfig {
    private static final String CFG_FILE = "consoleskins_idle_sit.txt";

    private static final Set<String> DEFAULTS;

    static {
        Set<String> d = new HashSet<>();
        d.add("cpm:legacy_skinpacks:skinpacks/festive_pack/badsanta.cpmmodel");
        DEFAULTS = Collections.unmodifiableSet(d);
    }

    private static volatile Set<String> fileSet;
    private static volatile long lastLoadMs;

    private BadSantaSitConfig() {
    }

    public static boolean isIdleSitSkin(String skinId) {
        if (skinId == null || skinId.isBlank()) return false;
        if (DEFAULTS.contains(skinId)) return true;
        Set<String> f = fileSet;
        if (f == null || (System.currentTimeMillis() - lastLoadMs) > 3_000L) reloadQuiet();
        f = fileSet;
        return f != null && f.contains(skinId);
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
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
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
}

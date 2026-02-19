package wily.legacy.Skins.client.util;

import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConsoleSkinsClientSettings {
    private static final String FILE_NAME = "consoleskins_client_options.txt";

    private static final boolean DEFAULT_SMOOTH_PREVIEW_SCROLL = false;
    private static final boolean DEFAULT_SKIN_ANIMATIONS = true;
    private static volatile boolean loaded;
    private static volatile boolean smoothPreviewScroll;
    private static volatile boolean skinAnimations = DEFAULT_SKIN_ANIMATIONS;
    private static volatile String lastUsedCustomPackId;

    private ConsoleSkinsClientSettings() {
    }

    public static boolean isSmoothPreviewScroll() {
        ensureLoaded();
        return smoothPreviewScroll;
    }

    public static void setSmoothPreviewScroll(boolean enabled) {
        ensureLoaded();
        smoothPreviewScroll = enabled;
        saveQuiet();
    }

    public static boolean isSkinAnimations() {
        ensureLoaded();
        return skinAnimations;
    }

    public static void setSkinAnimations(boolean enabled) {
        ensureLoaded();
        skinAnimations = enabled;
        saveQuiet();

        try {
            wily.legacy.Skins.client.cpm.CpmModelManager.refreshUpsideDownFlags();
        } catch (Throwable ignored) {
        }
    }

    public static String getLastUsedCustomPackId() {
        ensureLoaded();
        return lastUsedCustomPackId;
    }

    public static void setLastUsedCustomPackId(String packId) {
        ensureLoaded();
        lastUsedCustomPackId = (packId == null || packId.isBlank()) ? null : packId.trim();
        saveQuiet();
    }


    public static void resetToDefaults() {
        ensureLoaded();
        smoothPreviewScroll = DEFAULT_SMOOTH_PREVIEW_SCROLL;
        skinAnimations = DEFAULT_SKIN_ANIMATIONS;
        lastUsedCustomPackId = null;
        saveQuiet();
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loadQuiet();
        loaded = true;
    }

    private static void loadQuiet() {
        try {

            smoothPreviewScroll = DEFAULT_SMOOTH_PREVIEW_SCROLL;
            skinAnimations = DEFAULT_SKIN_ANIMATIONS;
            lastUsedCustomPackId = null;

            Path cfg = resolveConfigFile();
            if (cfg == null || !Files.isRegularFile(cfg)) return;

            try (BufferedReader br = Files.newBufferedReader(cfg, StandardCharsets.UTF_8)) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    int eq = line.indexOf('=');
                    String k = eq >= 0 ? line.substring(0, eq).trim() : line.trim();
                    String v = eq >= 0 ? line.substring(eq + 1).trim() : "";

                    if (k.equalsIgnoreCase("smooth_preview_scroll") || k.equalsIgnoreCase("smoothPreviewScroll")) {
                        smoothPreviewScroll = parseBool(v, DEFAULT_SMOOTH_PREVIEW_SCROLL);
                        continue;
                    }

                    if (k.equalsIgnoreCase("skin_animations") || k.equalsIgnoreCase("skinAnimations") || k.equalsIgnoreCase("enable_skin_animations")) {
                        skinAnimations = parseBool(v, DEFAULT_SKIN_ANIMATIONS);
                        continue;
                    }

                    if (k.equalsIgnoreCase("last_used_custom_pack") || k.equalsIgnoreCase("lastUsedCustomPack") || k.equalsIgnoreCase("last_used_custom_pack_id")) {
                        lastUsedCustomPackId = v == null || v.isBlank() ? null : v.trim();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean parseBool(String v, boolean def) {
        if (v == null) return def;
        String s = v.trim().toLowerCase();
        if (s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("y") || s.equals("on")) return true;
        if (s.equals("false") || s.equals("0") || s.equals("no") || s.equals("n") || s.equals("off")) return false;
        return def;
    }

    private static void saveQuiet() {
        try {
            Path cfg = resolveConfigFile();
            if (cfg == null) return;

            Files.createDirectories(cfg.getParent());
            String out = "# ConsoleSkins client options (dont mind the typos)\n"
                    + "smooth_preview_scroll=" + smoothPreviewScroll + "\n"
                    + "skin_animations=" + skinAnimations + "\n"
                    + "last_used_custom_pack=" + (lastUsedCustomPackId == null ? "" : lastUsedCustomPackId) + "\n";
            Files.writeString(cfg, out, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
    }

    private static Path resolveConfigFile() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameDirectory == null) return null;
        return mc.gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }
}

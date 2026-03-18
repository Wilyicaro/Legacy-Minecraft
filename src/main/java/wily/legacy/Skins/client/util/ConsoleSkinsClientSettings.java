package wily.legacy.Skins.client.util;

import net.minecraft.client.Minecraft;
import wily.legacy.Skins.client.screen.changeskin.SkinPackViewFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConsoleSkinsClientSettings {
    private static final String FILE_NAME = "consoleskins_client_options.txt";

    private static final boolean DEFAULT_SMOOTH_PREVIEW_SCROLL = false;
    private static final boolean DEFAULT_SKIN_ANIMATIONS = true;
    private static final boolean DEFAULT_HIDE_ARMOR_ON_ALL_BOX_SKINS = false;
    private static final boolean DEFAULT_TU3_CHANGE_SKIN_SCREEN = false;
    private static final boolean DEFAULT_HIDE_DUPLICATE_EXTERNAL_LEGACY_PACKS = false;
    private static final boolean DEFAULT_SKIN_SELECTION_INITIALIZED = false;
    private static volatile boolean loaded;
    private static volatile boolean smoothPreviewScroll;
    private static volatile boolean skinAnimations = DEFAULT_SKIN_ANIMATIONS;
    private static volatile boolean hideArmorOnAllBoxSkins = DEFAULT_HIDE_ARMOR_ON_ALL_BOX_SKINS;
    private static volatile boolean tu3ChangeSkinScreen = DEFAULT_TU3_CHANGE_SKIN_SCREEN;
    private static volatile boolean hideDuplicateExternalLegacyPacks = DEFAULT_HIDE_DUPLICATE_EXTERNAL_LEGACY_PACKS;
    private static volatile boolean skinSelectionInitialized = DEFAULT_SKIN_SELECTION_INITIALIZED;
    private static volatile String lastUsedCustomPackId;
    private static volatile String lastSkinPackViewFilter;

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
    }

    public static boolean isHideArmorOnAllBoxSkins() {
        ensureLoaded();
        return hideArmorOnAllBoxSkins;
    }

    public static void setHideArmorOnAllBoxSkins(boolean enabled) {
        ensureLoaded();
        hideArmorOnAllBoxSkins = enabled;
        saveQuiet();
    }

    public static boolean isTu3ChangeSkinScreen() {
        ensureLoaded();
        return tu3ChangeSkinScreen;
    }

    public static void setTu3ChangeSkinScreen(boolean enabled) {
        ensureLoaded();
        tu3ChangeSkinScreen = enabled;
        saveQuiet();
    }

    public static boolean isHideDuplicateExternalLegacyPacks() {
        ensureLoaded();
        return hideDuplicateExternalLegacyPacks;
    }

    public static void setHideDuplicateExternalLegacyPacks(boolean enabled) {
        ensureLoaded();
        hideDuplicateExternalLegacyPacks = enabled;
        saveQuiet();
    }

    public static boolean isSkinSelectionInitialized() {
        ensureLoaded();
        return skinSelectionInitialized;
    }

    public static void setSkinSelectionInitialized(boolean initialized) {
        ensureLoaded();
        skinSelectionInitialized = initialized;
        saveQuiet();
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

    public static SkinPackViewFilter getLastSkinPackViewFilter() {
        ensureLoaded();
        if (lastSkinPackViewFilter == null || lastSkinPackViewFilter.isBlank()) return SkinPackViewFilter.DEFAULT;
        try {
            return SkinPackViewFilter.valueOf(lastSkinPackViewFilter);
        } catch (IllegalArgumentException ignored) {
            return SkinPackViewFilter.DEFAULT;
        }
    }

    public static void setLastSkinPackViewFilter(SkinPackViewFilter filter) {
        ensureLoaded();
        SkinPackViewFilter resolved = filter == null ? SkinPackViewFilter.DEFAULT : filter;
        lastSkinPackViewFilter = resolved.name();
        saveQuiet();
    }

    public static void resetToDefaults() {
        ensureLoaded();
        smoothPreviewScroll = DEFAULT_SMOOTH_PREVIEW_SCROLL;
        skinAnimations = DEFAULT_SKIN_ANIMATIONS;
        hideArmorOnAllBoxSkins = DEFAULT_HIDE_ARMOR_ON_ALL_BOX_SKINS;
        tu3ChangeSkinScreen = DEFAULT_TU3_CHANGE_SKIN_SCREEN;
        hideDuplicateExternalLegacyPacks = DEFAULT_HIDE_DUPLICATE_EXTERNAL_LEGACY_PACKS;
        skinSelectionInitialized = DEFAULT_SKIN_SELECTION_INITIALIZED;
        lastUsedCustomPackId = null;
        lastSkinPackViewFilter = SkinPackViewFilter.DEFAULT.name();
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
            hideArmorOnAllBoxSkins = DEFAULT_HIDE_ARMOR_ON_ALL_BOX_SKINS;
            tu3ChangeSkinScreen = DEFAULT_TU3_CHANGE_SKIN_SCREEN;
            hideDuplicateExternalLegacyPacks = DEFAULT_HIDE_DUPLICATE_EXTERNAL_LEGACY_PACKS;
            skinSelectionInitialized = DEFAULT_SKIN_SELECTION_INITIALIZED;
            lastUsedCustomPackId = null;
            lastSkinPackViewFilter = SkinPackViewFilter.DEFAULT.name();

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

                    if (k.equalsIgnoreCase("hide_armor_on_all_box_skins") || k.equalsIgnoreCase("hideArmorOnAllBoxSkins")
                            || k.equalsIgnoreCase("force_hide_armor_on_all_box_skins") || k.equalsIgnoreCase("forceHideArmorOnAllBoxSkins")
                            || k.equalsIgnoreCase("hide_armor_all_box_skins") || k.equalsIgnoreCase("hideArmorAllBoxSkins")) {
                        hideArmorOnAllBoxSkins = parseBool(v, DEFAULT_HIDE_ARMOR_ON_ALL_BOX_SKINS);
                        continue;
                    }

                    if (k.equalsIgnoreCase("tu3_change_skin_screen") || k.equalsIgnoreCase("tu3ChangeSkinScreen")
                            || k.equalsIgnoreCase("tu3_change_skin") || k.equalsIgnoreCase("tu3ChangeSkin")) {
                        tu3ChangeSkinScreen = parseBool(v, DEFAULT_TU3_CHANGE_SKIN_SCREEN);
                        continue;
                    }

                    if (k.equalsIgnoreCase("hide_duplicate_external_legacy_packs") || k.equalsIgnoreCase("hideDuplicateExternalLegacyPacks")
                            || k.equalsIgnoreCase("hide_duplicate_legacy_packs") || k.equalsIgnoreCase("hideDuplicateLegacyPacks")
                            || k.equalsIgnoreCase("hide_duplicate_external_packs") || k.equalsIgnoreCase("hideDuplicateExternalPacks")) {
                        hideDuplicateExternalLegacyPacks = parseBool(v, DEFAULT_HIDE_DUPLICATE_EXTERNAL_LEGACY_PACKS);
                        continue;
                    }

                    if (k.equalsIgnoreCase("skin_selection_initialized") || k.equalsIgnoreCase("skinSelectionInitialized")
                            || k.equalsIgnoreCase("initialized_skin_selection") || k.equalsIgnoreCase("initializedSkinSelection")) {
                        skinSelectionInitialized = parseBool(v, DEFAULT_SKIN_SELECTION_INITIALIZED);
                        continue;
                    }

                    if (k.equalsIgnoreCase("last_used_custom_pack") || k.equalsIgnoreCase("lastUsedCustomPack") || k.equalsIgnoreCase("last_used_custom_pack_id")) {
                        lastUsedCustomPackId = v == null || v.isBlank() ? null : v.trim();
                        continue;
                    }

                    if (k.equalsIgnoreCase("last_skin_pack_view_filter") || k.equalsIgnoreCase("lastSkinPackViewFilter")
                            || k.equalsIgnoreCase("last_change_skin_filter") || k.equalsIgnoreCase("lastChangeSkinFilter")) {
                        lastSkinPackViewFilter = v == null || v.isBlank() ? SkinPackViewFilter.DEFAULT.name() : v.trim();
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
                    + "hide_armor_on_all_box_skins=" + hideArmorOnAllBoxSkins + "\n"
                    + "tu3_change_skin_screen=" + tu3ChangeSkinScreen + "\n"
                    + "hide_duplicate_external_legacy_packs=" + hideDuplicateExternalLegacyPacks + "\n"
                    + "skin_selection_initialized=" + skinSelectionInitialized + "\n"
                    + "last_used_custom_pack=" + (lastUsedCustomPackId == null ? "" : lastUsedCustomPackId) + "\n"
                    + "last_skin_pack_view_filter=" + (lastSkinPackViewFilter == null ? SkinPackViewFilter.DEFAULT.name() : lastSkinPackViewFilter) + "\n";
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

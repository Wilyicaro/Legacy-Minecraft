package wily.legacy.client;

import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;

import java.util.Map;

public final class StorePreviewAtlas {
    public static final int TILE_SIZE = 360;
    public static final int COLUMNS = 5;
    public static final int ROWS = 5;
    public static final int ATLAS_WIDTH = TILE_SIZE * COLUMNS;
    public static final int ATLAS_HEIGHT = TILE_SIZE * ROWS;
    public static final ResourceLocation ATLAS = Legacy4J.createModLocation("textures/gui/store/content_preview_atlas.png");
    private static final Map<String, Entry> ENTRIES = Map.ofEntries(
        entry("supercute_texture_pack", 0),
        entry("city_texture_pack", 1),
        entry("plastic_texture_pack", 2),
        entry("natural_texture_pack", 3),
        entry("fantasy_texture_pack", 4),
        entry("candy_texture_pack", 5),
        entry("cartoon_texture_pack", 6),
        entry("steampunk_texture_pack", 7),
        entry("pattern_texture_pack", 8),
        entry("potc", 9),
        entry("egyptianmythology", 10),
        entry("norsemythology", 11),
        entry("festive", 12),
        entry("chinesemythology", 13),
        entry("greekmythology", 14),
        entry("fallout", 15),
        entry("mass_effect", 16),
        entry("littlebigplanet_mashup", 17),
        entry("halloween_mashup", 18),
        entry("supermario", 19),
        entry("builders_pack", 20)
    );

    private StorePreviewAtlas() {
    }

    public static Entry get(String packId) {
        return ENTRIES.get(packId);
    }

    private static Map.Entry<String, Entry> entry(String packId, int index) {
        int column = index % COLUMNS;
        int row = index / COLUMNS;
        return Map.entry(packId, new Entry(ATLAS, column * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE));
    }

    public record Entry(ResourceLocation resource, int u, int v, int width, int height) {
    }
}

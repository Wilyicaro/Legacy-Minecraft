package wily.legacy.client;

import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;

import java.util.List;
import java.util.Map;

public final class StorePreviewAtlas {
    public static final int TILE_SIZE = 360;
    public static final int COLUMNS = 5;
    public static final int ROWS = 5;
    public static final int ATLAS_WIDTH = TILE_SIZE * COLUMNS;
    public static final int ATLAS_HEIGHT = TILE_SIZE * ROWS;
    public static final int SKINPACK_PADDING = 2;
    public static final int SKINPACK_CELL_SIZE = TILE_SIZE + SKINPACK_PADDING * 2;
    public static final int SKINPACK_COLUMNS = 8;
    public static final int SKINPACK_ROWS = 9;
    public static final int SKINPACK_ATLAS_WIDTH = SKINPACK_CELL_SIZE * SKINPACK_COLUMNS;
    public static final int SKINPACK_ATLAS_HEIGHT = SKINPACK_CELL_SIZE * SKINPACK_ROWS;
    public static final ResourceLocation ATLAS = Legacy4J.createModLocation("textures/gui/store/content_preview_atlas.png");
    public static final ResourceLocation SKINPACK_ATLAS = Legacy4J.createModLocation("textures/gui/store/skinpack_preview_atlas.png");
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
        entry("builders_pack", 20),
        entry("skinpack_megabundle", 21),
        entry("starterpacks_bundle", 22)
    );
    private static final List<String> SKINPACK_IDS = List.of(
        "birthday5",
        "birthday4",
        "birthday3",
        "birthday2",
        "birthday",
        "battleandbeasts",
        "battleandthebeasts2",
        "adventuretime",
        "biomesettlers",
        "biomesettlers2",
        "campfiretales",
        "chinesemythology",
        "classicskinpack1",
        "doctor_who_volume_1",
        "doctor_who_volume_2",
        "egyptianmythology",
        "finalfantasyxv",
        "fallout",
        "festivemashup",
        "festive",
        "fromtheshadows",
        "greekmythology",
        "minecon2017",
        "halloweencharity",
        "halloween2015",
        "littlebigplanet",
        "magicthegathering",
        "masseffect",
        "minecon2015",
        "minecon2016",
        "minigameheros",
        "minigamemasters",
        "marvelsavengers",
        "marvelgog",
        "marvelsspiderman",
        "moana",
        "norsemythology",
        "powerrangers",
        "piratesofthecaribbean",
        "redstonespecialists",
        "soa",
        "skinpack1",
        "skinpack2",
        "skinpack3",
        "skinpack4",
        "skinpack5",
        "skin_pack_6",
        "supermario",
        "starwarssolo",
        "starwarsclassic",
        "starwarsprequel",
        "starwarsrebels",
        "starwarssequel",
        "stevenuniverse",
        "storymode",
        "strangerthings",
        "biomesettlers3",
        "skyrim",
        "thenightmarebeforechristmas",
        "supercuteskins",
        "theincredibles",
        "thesimpsons",
        "villians",
        "toystory",
        "halo"
    );

    private StorePreviewAtlas() {
    }

    public static Entry get(String packId) {
        return ENTRIES.get(packId);
    }

    public static Entry getSkinpack(String packId) {
        int index = SKINPACK_IDS.indexOf(packId);
        if (index < 0) return null;
        return skinpackEntry(index);
    }

    private static Map.Entry<String, Entry> entry(String packId, int index) {
        return Map.entry(packId, entry(ATLAS, COLUMNS, ATLAS_WIDTH, ATLAS_HEIGHT, index));
    }

    private static Entry entry(ResourceLocation atlas, int columns, int atlasWidth, int atlasHeight, int index) {
        int column = index % columns;
        int row = index / columns;
        return new Entry(atlas, column * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE, atlasWidth, atlasHeight);
    }

    private static Entry skinpackEntry(int index) {
        int column = index % SKINPACK_COLUMNS;
        int row = index / SKINPACK_COLUMNS;
        int x = column * SKINPACK_CELL_SIZE + SKINPACK_PADDING;
        int y = row * SKINPACK_CELL_SIZE + SKINPACK_PADDING;
        return new Entry(SKINPACK_ATLAS, x, y, TILE_SIZE, TILE_SIZE, SKINPACK_ATLAS_WIDTH, SKINPACK_ATLAS_HEIGHT);
    }

    public record Entry(ResourceLocation resource, int u, int v, int width, int height, int atlasWidth, int atlasHeight) {
    }
}

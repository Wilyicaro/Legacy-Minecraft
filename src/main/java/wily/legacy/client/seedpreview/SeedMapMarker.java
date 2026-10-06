package wily.legacy.client.seedpreview;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public record SeedMapMarker(BlockPos pos, Identifier texture, Component name, int size, boolean estimated) {
    private static final Identifier COMPASS = Identifier.withDefaultNamespace("textures/item/compass_16.png");

    public static SeedMapMarker spawn(BlockPos pos) {
        return new SeedMapMarker(pos, COMPASS, Component.translatable("legacy.menu.seed_preview.spawn"), 16, true);
    }

    public static SeedMapMarker seedStart(BlockPos pos) {
        return new SeedMapMarker(pos, COMPASS, Component.translatable("legacy.menu.seed_preview.seed_start"), 16, false);
    }

    public boolean isSpawn() {
        return texture.equals(COMPASS);
    }

    public static SeedMapMarker structure(BlockPos pos, Holder<StructureSet> set, Holder<Structure> structure) {
        String icon = structure.unwrapKey().map(key -> switch (key.identifier().toString()) {
            case "minecraft:village_desert" -> "desert_village";
            case "minecraft:village_plains" -> "plains_village";
            case "minecraft:village_savanna" -> "savanna_village";
            case "minecraft:village_snowy" -> "snowy_village";
            case "minecraft:village_taiga" -> "taiga_village";
            case "minecraft:mansion" -> "woodland_mansion";
            case "minecraft:monument" -> "ocean_monument";
            case "minecraft:jungle_pyramid" -> "jungle_temple";
            case "minecraft:swamp_hut" -> "swamp_hut";
            case "minecraft:trial_chambers" -> "trial_chambers";
            case "minecraft:ancient_city" -> "legacy:ancient_city";
            case "minecraft:buried_treasure" -> "legacy:buried_treasure";
            case "minecraft:desert_pyramid" -> "legacy:desert_pyramid";
            case "minecraft:igloo" -> "legacy:igloo";
            case "minecraft:mineshaft", "minecraft:mineshaft_mesa" -> "legacy:mineshaft";
            case "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm" -> "legacy:ocean_ruins";
            case "minecraft:pillager_outpost" -> "legacy:pillager_outpost";
            case "minecraft:ruined_portal", "minecraft:ruined_portal_desert", "minecraft:ruined_portal_jungle",
                 "minecraft:ruined_portal_mountain", "minecraft:ruined_portal_nether", "minecraft:ruined_portal_ocean",
                 "minecraft:ruined_portal_swamp" -> "legacy:ruined_portal";
            case "minecraft:shipwreck", "minecraft:shipwreck_beached" -> "legacy:shipwreck";
            case "minecraft:stronghold" -> "legacy:stronghold";
            case "minecraft:trail_ruins" -> "legacy:trail_ruins";
            default -> "red_x";
        }).orElse("red_x");
        Component name = set.unwrapKey().map(key -> Component.translatableWithFallback(
                "structure." + key.identifier().toLanguageKey(), key.identifier().toString()))
                .orElse(Component.translatable("legacy.menu.seed_preview.structure"));
        Identifier texture = Identifier.parse(icon).withPath(path -> "textures/map/decorations/" + path + ".png");
        return new SeedMapMarker(pos, texture, name, icon.equals("red_x") ? 5 : 7, true);
    }

    public Component tooltip() {
        return (estimated ? Component.translatable("legacy.menu.seed_preview.estimated", name) : name.copy())
                .append("\n\n").append(Component.translatable("legacy.menu.seed_preview.coordinates", pos.getX(), pos.getZ()))
                .append("\n\n").append(Component.translatable(estimated
                        ? "legacy.menu.seed_preview.estimate_description" : "legacy.menu.seed_preview.start_description"));
    }
}

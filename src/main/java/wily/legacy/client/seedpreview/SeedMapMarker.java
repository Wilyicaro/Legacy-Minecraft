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
            default -> "red_x";
        }).orElse("red_x");
        Component name = set.unwrapKey().map(key -> Component.translatableWithFallback(
                "structure." + key.identifier().toLanguageKey(), key.identifier().toString()))
                .orElse(Component.translatable("legacy.menu.seed_preview.structure"));
        return new SeedMapMarker(pos, Identifier.withDefaultNamespace("textures/map/decorations/" + icon + ".png"), name, icon.equals("red_x") ? 5 : 7, true);
    }

    public Component tooltip() {
        return (estimated ? Component.translatable("legacy.menu.seed_preview.estimated", name) : name.copy())
                .append("\n\n").append(Component.translatable("legacy.menu.seed_preview.coordinates", pos.getX(), pos.getZ()))
                .append("\n\n").append(Component.translatable(estimated
                        ? "legacy.menu.seed_preview.estimate_description" : "legacy.menu.seed_preview.start_description"));
    }
}

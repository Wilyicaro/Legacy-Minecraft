package wily.legacy.mobcaps;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.Level;

public final class ConsoleMobCaps {
    private static final String MAX_GENERAL_ANIMALS_SPAWNED = "legacy.message.mobcap.max_general_animals_spawned";
    private static final String MAX_MOOSHROOMS_SPAWNED = "legacy.message.mobcap.max_mooshrooms_spawned";
    private static final String MAX_WOLVES_SPAWNED = "legacy.message.mobcap.max_wolves_spawned";
    private static final String MAX_CHICKENS_SPAWNED = "legacy.message.mobcap.max_chickens_spawned";
    private static final String MAX_FISH_AND_SQUID = "legacy.message.mobcap.max_fish_and_squid";
    private static final String MAX_DOLPHINS_SPAWNED = "legacy.message.mobcap.max_dolphins_spawned";
    private static final String MAX_BATS_SPAWNED = "legacy.message.mobcap.max_bats_spawned";
    private static final String MAX_ENEMIES_SPAWNED = "legacy.message.mobcap.max_enemies_spawned";
    private static final String MAX_PHANTOMS_SPAWNED = "legacy.message.mobcap.max_phantoms_spawned";
    private static final String MAX_VILLAGERS_SPAWNED = "legacy.message.mobcap.max_villagers_spawned";
    private static final String MAX_HANGING = "legacy.message.mobcap.max_hanging_entities";
    private static final String MAX_ARMOR_STANDS = "legacy.message.mobcap.max_armor_stands";
    private static final String CANT_SPAWN_IN_PEACEFUL = "legacy.message.mobcap.cant_spawn_in_peaceful";
    private static final String MAX_GENERAL_ANIMALS_BRED = "legacy.message.mobcap.max_general_animals_bred";
    private static final String MAX_WOLVES_BRED = "legacy.message.mobcap.max_wolves_bred";
    private static final String MAX_CHICKENS_BRED = "legacy.message.mobcap.max_chickens_bred";
    private static final String MAX_MOOSHROOMS_BRED = "legacy.message.mobcap.max_mooshrooms_bred";
    private static final String MAX_BOATS = "legacy.message.mobcap.max_boats";
    private static final String MAX_END_CRYSTALS = "legacy.message.mobcap.max_end_crystals";

    private ConsoleMobCaps() {
    }

    static TrackedMobCap bucketForType(EntityType<?> type) {
        if (type == EntityType.CHICKEN) {
            return TrackedMobCap.CHICKENS;
        }
        if (type == EntityType.WOLF) {
            return TrackedMobCap.WOLVES;
        }
        if (type == EntityType.MOOSHROOM) {
            return TrackedMobCap.MOOSHROOMS;
        }
        if (type == EntityType.SQUID || type == EntityType.GLOW_SQUID) {
            return TrackedMobCap.SQUIDS;
        }
        if (type == EntityType.DOLPHIN) {
            return TrackedMobCap.DOLPHINS;
        }
        if (type == EntityType.SLIME || type == EntityType.MAGMA_CUBE) {
            return TrackedMobCap.SLIMES;
        }
        if (type == EntityType.GUARDIAN || type == EntityType.ELDER_GUARDIAN) {
            return TrackedMobCap.GUARDIANS;
        }
        if (type == EntityType.BAT) {
            return TrackedMobCap.AMBIENT;
        }
        if (type == EntityType.VILLAGER) {
            return TrackedMobCap.VILLAGERS;
        }
        if (type == EntityType.PHANTOM) {
            return TrackedMobCap.PHANTOMS;
        }
        if (type == EntityType.ARMOR_STAND) {
            return TrackedMobCap.ARMOR_STANDS;
        }
        if (type == EntityType.PAINTING || type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) {
            return TrackedMobCap.HANGING;
        }
        if (type == EntityType.SNOW_GOLEM) {
            return TrackedMobCap.SNOW_GOLEMS;
        }
        if (type == EntityType.IRON_GOLEM) {
            return TrackedMobCap.IRON_GOLEMS;
        }
        if (type == EntityType.WITHER) {
            return TrackedMobCap.WITHERS;
        }
        if (type == EntityType.END_CRYSTAL) {
            return TrackedMobCap.END_CRYSTALS;
        }
        if (type == EntityType.ENDER_DRAGON) {
            return null;
        }

        Class<? extends Entity> baseClass = type.getBaseClass();
        if (AbstractBoat.class.isAssignableFrom(baseClass)) {
            return TrackedMobCap.BOATS;
        }
        if (isHostile(type)) {
            return TrackedMobCap.MONSTERS;
        }

        TrackedMobCap categoryBucket = genericBucketForCategory(type.getCategory());
        if (categoryBucket != null) {
            return categoryBucket;
        }
        if (Mob.class.isAssignableFrom(baseClass)) {
            return TrackedMobCap.GENERAL_ANIMALS;
        }
        return null;
    }

    static TrackedMobCap bucketForEntity(Entity entity) {
        if (entity instanceof AbstractBoat) {
            return TrackedMobCap.BOATS;
        }
        if (entity instanceof HangingEntity) {
            return TrackedMobCap.HANGING;
        }
        if (entity instanceof ArmorStand) {
            return TrackedMobCap.ARMOR_STANDS;
        }
        return bucketForType(entity.getType());
    }

    static TrackedMobCap parentBucketForType(EntityType<?> type) {
        if (type == EntityType.MOOSHROOM || type == EntityType.SNOW_GOLEM) {
            return TrackedMobCap.GENERAL_ANIMALS;
        }
        if (type == EntityType.SQUID || type == EntityType.GLOW_SQUID) {
            return TrackedMobCap.WATER_ANIMALS;
        }
        if (type == EntityType.WITHER || type == EntityType.PHANTOM) {
            return TrackedMobCap.MONSTERS;
        }
        return null;
    }

    public static boolean canNaturalCategorySpawn(ServerLevel level, MobCategory category) {
        if (!LegacyMobCaps.isEnabled(level)) {
            return true;
        }

        WorldMobCapTracker tracker = LegacyMobCaps.tracker(level);
        return switch (category) {
            case MONSTER -> hasNaturalRoom(tracker, TrackedMobCap.MONSTERS)
                || hasNaturalRoom(tracker, TrackedMobCap.SLIMES)
                || hasNaturalRoom(tracker, TrackedMobCap.GUARDIANS);
            case AMBIENT -> hasNaturalRoom(tracker, TrackedMobCap.AMBIENT);
            case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> hasNaturalRoom(tracker, TrackedMobCap.WATER_ANIMALS)
                || hasNaturalRoom(tracker, TrackedMobCap.SQUIDS)
                || hasNaturalRoom(tracker, TrackedMobCap.DOLPHINS);
            case CREATURE -> hasNaturalRoom(tracker, TrackedMobCap.GENERAL_ANIMALS)
                || hasNaturalRoom(tracker, TrackedMobCap.CHICKENS)
                || hasNaturalRoom(tracker, TrackedMobCap.WOLVES)
                || hasNaturalRoom(tracker, TrackedMobCap.MOOSHROOMS);
            default -> true;
        };
    }

    public static boolean canNaturalMobSpawn(ServerLevel level, MobCategory category, EntityType<?> type) {
        if (!LegacyMobCaps.isEnabled(level)) {
            return true;
        }

        WorldMobCapTracker tracker = LegacyMobCaps.tracker(level);
        TrackedMobCap bucket = bucketForType(type);

        if (category.isFriendly() && !Level.OVERWORLD.equals(level.dimension())) {
            return false;
        }
        if (bucket == null) {
            return true;
        }
        if (!hasNaturalRoom(tracker, bucket)) {
            return false;
        }
        if (type == EntityType.GHAST) {
            return tracker.count(type) < 4;
        }
        if (type == EntityType.ZOMBIFIED_PIGLIN) {
            return tracker.count(type) < 40;
        }
        if (type == EntityType.ENDERMAN && Level.END.equals(level.dimension())) {
            int endermanCap = TrackedMobCap.MONSTERS.naturalLimit();
            if (level.getDifficulty() == Difficulty.NORMAL) {
                endermanCap -= TrackedMobCap.MONSTERS.naturalLimit() / 4;
            } else if (level.getDifficulty() == Difficulty.EASY || level.getDifficulty() == Difficulty.PEACEFUL) {
                endermanCap -= TrackedMobCap.MONSTERS.naturalLimit() / 2;
            }
            return tracker.count(type) < endermanCap;
        }
        if (bucket == TrackedMobCap.MONSTERS
            || bucket == TrackedMobCap.GENERAL_ANIMALS
            || bucket == TrackedMobCap.AMBIENT
            || bucket == TrackedMobCap.WATER_ANIMALS
            || bucket == TrackedMobCap.SLIMES
            || bucket == TrackedMobCap.GUARDIANS) {
            return tracker.count(type) < bucket.naturalLimit() / 2;
        }
        return true;
    }

    public static String spawnEggFailure(ServerLevel level, EntityType<?> type) {
        if (isHostile(type) && level.getDifficulty() == Difficulty.PEACEFUL) {
            return CANT_SPAWN_IN_PEACEFUL;
        }
        if (!LegacyMobCaps.isEnabled(level)) {
            return null;
        }

        WorldMobCapTracker tracker = LegacyMobCaps.tracker(level);
        TrackedMobCap bucket = bucketForType(type);

        if (bucket == null || tracker.count(bucket) < bucket.manualLimit()) {
            return null;
        }
        return switch (bucket) {
            case CHICKENS -> MAX_CHICKENS_SPAWNED;
            case WOLVES -> MAX_WOLVES_SPAWNED;
            case MOOSHROOMS -> MAX_MOOSHROOMS_SPAWNED;
            case WATER_ANIMALS, SQUIDS -> MAX_FISH_AND_SQUID;
            case DOLPHINS -> MAX_DOLPHINS_SPAWNED;
            case AMBIENT -> MAX_BATS_SPAWNED;
            case VILLAGERS -> MAX_VILLAGERS_SPAWNED;
            case PHANTOMS -> MAX_PHANTOMS_SPAWNED;
            case MONSTERS, SLIMES, GUARDIANS -> MAX_ENEMIES_SPAWNED;
            case GENERAL_ANIMALS -> MAX_GENERAL_ANIMALS_SPAWNED;
            default -> null;
        };
    }

    public static String breedingFailure(ServerLevel level, EntityType<?> type) {
        if (!LegacyMobCaps.isEnabled(level)) {
            return null;
        }

        WorldMobCapTracker tracker = LegacyMobCaps.tracker(level);
        TrackedMobCap bucket = bucketForType(type);

        if (bucket == TrackedMobCap.CHICKENS && tracker.count(TrackedMobCap.CHICKENS) >= TrackedMobCap.CHICKENS.breedingLimit()) {
            return MAX_CHICKENS_BRED;
        }
        if (bucket == TrackedMobCap.WOLVES && tracker.count(TrackedMobCap.WOLVES) >= TrackedMobCap.WOLVES.breedingLimit()) {
            return MAX_WOLVES_BRED;
        }
        if (bucket == TrackedMobCap.MOOSHROOMS && tracker.count(TrackedMobCap.MOOSHROOMS) >= TrackedMobCap.MOOSHROOMS.breedingLimit()) {
            return MAX_MOOSHROOMS_BRED;
        }
        if (bucket == TrackedMobCap.GENERAL_ANIMALS && tracker.count(TrackedMobCap.GENERAL_ANIMALS) >= TrackedMobCap.GENERAL_ANIMALS.breedingLimit()) {
            return MAX_GENERAL_ANIMALS_BRED;
        }
        return null;
    }

    public static boolean canVillagerBreed(ServerLevel level) {
        return !LegacyMobCaps.isEnabled(level) || LegacyMobCaps.tracker(level).count(TrackedMobCap.VILLAGERS) < TrackedMobCap.VILLAGERS.breedingLimit();
    }

    public static boolean canPlaceBoat(ServerLevel level) {
        return !LegacyMobCaps.isEnabled(level) || LegacyMobCaps.tracker(level).count(TrackedMobCap.BOATS) < TrackedMobCap.BOATS.manualLimit();
    }

    public static boolean canPlaceHanging(ServerLevel level) {
        return !LegacyMobCaps.isEnabled(level) || LegacyMobCaps.tracker(level).count(TrackedMobCap.HANGING) < TrackedMobCap.HANGING.manualLimit();
    }

    public static boolean canPlaceArmorStand(ServerLevel level) {
        return !LegacyMobCaps.isEnabled(level) || LegacyMobCaps.tracker(level).count(TrackedMobCap.ARMOR_STANDS) < TrackedMobCap.ARMOR_STANDS.manualLimit();
    }

    public static boolean canPlaceEndCrystal(ServerLevel level) {
        return !LegacyMobCaps.isEnabled(level) || LegacyMobCaps.tracker(level).count(TrackedMobCap.END_CRYSTALS) < TrackedMobCap.END_CRYSTALS.manualLimit();
    }

    public static boolean canTriggerSummon(ServerLevel level, EntityType<?> type) {
        if (!LegacyMobCaps.isEnabled(level)) {
            return true;
        }

        WorldMobCapTracker tracker = LegacyMobCaps.tracker(level);
        if (type == EntityType.SNOW_GOLEM) {
            return tracker.count(TrackedMobCap.SNOW_GOLEMS) < TrackedMobCap.SNOW_GOLEMS.manualLimit();
        }
        if (type == EntityType.IRON_GOLEM) {
            return tracker.count(TrackedMobCap.IRON_GOLEMS) < TrackedMobCap.IRON_GOLEMS.manualLimit();
        }
        if (type == EntityType.WITHER) {
            return tracker.count(TrackedMobCap.WITHERS) < TrackedMobCap.WITHERS.manualLimit();
        }
        return true;
    }

    public static boolean canHatchChicken(ServerLevel level) {
        return !LegacyMobCaps.isEnabled(level) || LegacyMobCaps.tracker(level).count(TrackedMobCap.CHICKENS) < TrackedMobCap.CHICKENS.breedingLimit();
    }

    public static void sendFailure(Player player, String translationKey) {
        player.sendSystemMessage(Component.translatable(translationKey));
        syncInventory(player);
    }

    public static void syncInventory(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.sendAllDataToRemote();
            if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
                serverPlayer.containerMenu.sendAllDataToRemote();
            }
        }
    }

    public static String maxBoatsMessage() {
        return MAX_BOATS;
    }

    public static String maxHangingMessage() {
        return MAX_HANGING;
    }

    public static String maxArmorStandsMessage() {
        return MAX_ARMOR_STANDS;
    }

    public static String maxEndCrystalsMessage() {
        return MAX_END_CRYSTALS;
    }

    public static String peacefulSpawnMessage() {
        return CANT_SPAWN_IN_PEACEFUL;
    }

    private static boolean isHostile(EntityType<?> type) {
        return Enemy.class.isAssignableFrom(type.getBaseClass()) || type.getCategory() == MobCategory.MONSTER;
    }

    private static TrackedMobCap genericBucketForCategory(MobCategory category) {
        return switch (category) {
            case MONSTER -> TrackedMobCap.MONSTERS;
            case AMBIENT -> TrackedMobCap.AMBIENT;
            case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> TrackedMobCap.WATER_ANIMALS;
            case CREATURE -> TrackedMobCap.GENERAL_ANIMALS;
            default -> null;
        };
    }

    private static boolean hasNaturalRoom(WorldMobCapTracker tracker, TrackedMobCap bucket) {
        return tracker.count(bucket) < bucket.naturalLimit();
    }
}

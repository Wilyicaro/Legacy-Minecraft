package wily.legacy.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class LegacyMobCaps {

    private LegacyMobCaps() {}

    public enum LegacyBucket {
        HOSTILE,
        PASSIVE,
        WOLF,
        CHICKEN,
        MOOSHROOM,
        ARMOR_STAND,
        BAT,
        FISH,
        SQUID,
        OTHER
    }

    private static final Set<EntityType<?>> PASSIVE_EXCLUSIONS = Set.of(
            EntityType.WOLF,
            EntityType.BAT,
            EntityType.CHICKEN,
            EntityType.MOOSHROOM
    );

    private static final Set<MobCategory> WATER_CATEGORIES = EnumSet.of(
            MobCategory.WATER_CREATURE,
            MobCategory.WATER_AMBIENT,
            MobCategory.UNDERGROUND_WATER_CREATURE
    );

    public static LegacyBucket bucketFor(MobCategory category) {
        if (category == MobCategory.MONSTER) return LegacyBucket.HOSTILE;
        if (category == MobCategory.CREATURE) return LegacyBucket.PASSIVE;
        if (category == MobCategory.AMBIENT) return LegacyBucket.BAT;
        if (WATER_CATEGORIES.contains(category)) return LegacyBucket.OTHER;
        return LegacyBucket.OTHER;
    }

    public static LegacyBucket bucketFor(EntityType<?> type) {
        if (type == EntityType.WOLF) return LegacyBucket.WOLF;
        if (type == EntityType.CHICKEN) return LegacyBucket.CHICKEN;
        if (type == EntityType.MOOSHROOM) return LegacyBucket.MOOSHROOM;
        if (type == EntityType.ARMOR_STAND) return LegacyBucket.ARMOR_STAND;
        if (type == EntityType.BAT) return LegacyBucket.BAT;

        if (type == EntityType.COD
                || type == EntityType.SALMON
                || type == EntityType.TROPICAL_FISH
                || type == EntityType.PUFFERFISH) {
            return LegacyBucket.FISH;
        }

        if (type == EntityType.SQUID || type == EntityType.GLOW_SQUID) {
            return LegacyBucket.SQUID;
        }

        return bucketFor(type.getCategory());
    }

    public static int capFor(LegacyBucket bucket) {
        return switch (bucket) {
            case HOSTILE -> 90;
            case PASSIVE -> 90;
            case WOLF -> 24;
            case CHICKEN -> 24;
            case MOOSHROOM -> 24;
            case ARMOR_STAND -> 24;
            case BAT -> 24;
            case FISH -> 24;
            case SQUID -> 24;
            case OTHER -> Integer.MAX_VALUE;
        };
    }

    public static boolean isCapped(ServerLevel level, ServerPlayer player, EntityType<?> type) {
        LegacyBucket bucket = bucketFor(type);
        int cap = capFor(bucket);
        if (cap == Integer.MAX_VALUE) return false;

        int current = (bucket == LegacyBucket.ARMOR_STAND)
                ? countArmorStands(level, player)
                : countMobs(level, player, bucket);

        return current >= cap;
    }

    private static int countArmorStands(ServerLevel level, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(128.0D, 256.0D, 128.0D);
        return level.getEntitiesOfClass(ArmorStand.class, box).size();
    }

    private static int countMobs(ServerLevel level, ServerPlayer player, LegacyBucket bucket) {
        AABB box = player.getBoundingBox().inflate(128.0D, 256.0D, 128.0D);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box);

        int count = 0;
        for (Mob mob : mobs) {
            EntityType<?> type = mob.getType();

            if (bucket == LegacyBucket.PASSIVE && PASSIVE_EXCLUSIONS.contains(type)) {
                continue;
            }

            if (bucketFor(type) == bucket) {
                count++;
            }
        }
        return count;
    }
}

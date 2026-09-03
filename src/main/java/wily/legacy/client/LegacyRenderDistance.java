package wily.legacy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class LegacyRenderDistance {
    private static final int BLOCK_ENTITY_CAP = 62;
    private static final Map<EntityType<?>, Integer> ENTITY_CAPS = Util.make(new IdentityHashMap<EntityType<?>, Integer>(), caps -> {
        put(caps, 23, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SILVERFISH, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.ENDERMITE);
        put(caps, 25, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.TROPICAL_FISH);
        put(caps, 27, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.RABBIT);
        put(caps, 30, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.COD, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SALMON);
        put(caps, 32, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.CHICKEN);
        put(caps, 33, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.CAT);
        put(caps, 40, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.CAVE_SPIDER, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.BAT);
        put(caps, 41, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.PARROT, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.WOLF);
        put(caps, 42, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.OCELOT);
        put(caps, 49, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.PHANTOM);
        put(caps, 51, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SQUID, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.PUFFERFISH, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.DOLPHIN);
        put(caps, 55, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.GUARDIAN, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.MINECART, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.CHEST_MINECART, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.FURNACE_MINECART, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.HOPPER_MINECART, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.TNT_MINECART, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SPAWNER_MINECART, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.COMMAND_BLOCK_MINECART);
        put(caps, 57, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.PIG);
        put(caps, 59, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.TURTLE);
        put(caps, 61, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.CREEPER);
        put(caps, 64, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.BLAZE, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SHULKER, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.VILLAGER, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.WANDERING_TRADER);
        put(caps, 66, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.ZOMBIE, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SHEEP);
        put(caps, 67, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.ZOMBIE_VILLAGER, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.HUSK, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.DROWNED, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.WITCH, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.ZOMBIFIED_PIGLIN, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.EVOKER, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.VINDICATOR, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.PILLAGER);
        put(caps, 68, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SKELETON, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.STRAY, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.COW);
        put(caps, 69, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SNOW_GOLEM);
        put(caps, 79, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.WITHER_SKELETON, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SPIDER, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.IRON_GOLEM);
        put(caps, 80, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.GHAST, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.ENDERMAN, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.RAVAGER, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.ELDER_GUARDIAN, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.POLAR_BEAR, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.PANDA, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.HORSE, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.DONKEY, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.MULE, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SKELETON_HORSE, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.ZOMBIE_HORSE, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.LLAMA, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.TRADER_LLAMA, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.WITHER);
        put(caps, 159, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.ITEM_FRAME, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.GLOW_ITEM_FRAME, /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.PAINTING);
    });
    private static final Set<BlockEntityType<?>> BLOCK_ENTITIES = Set.of(/*? if >=26.2 {*/net.minecraft.world.level.block.entity.BlockEntityTypes/*?} else {*//*BlockEntityType*//*?}*/.CHEST, /*? if >=26.2 {*/net.minecraft.world.level.block.entity.BlockEntityTypes/*?} else {*//*BlockEntityType*//*?}*/.TRAPPED_CHEST, /*? if >=26.2 {*/net.minecraft.world.level.block.entity.BlockEntityTypes/*?} else {*//*BlockEntityType*//*?}*/.ENDER_CHEST, /*? if >=26.2 {*/net.minecraft.world.level.block.entity.BlockEntityTypes/*?} else {*//*BlockEntityType*//*?}*/.BANNER, /*? if >=26.2 {*/net.minecraft.world.level.block.entity.BlockEntityTypes/*?} else {*//*BlockEntityType*//*?}*/.SIGN, /*? if >=26.2 {*/net.minecraft.world.level.block.entity.BlockEntityTypes/*?} else {*//*BlockEntityType*//*?}*/.HANGING_SIGN);

    private LegacyRenderDistance() {
    }

    public static void initDefault() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null || minecraft.options.entityDistanceScaling().get() >= 0.5) return;
        minecraft.options.entityDistanceScaling().set(1.0);
        LegacyOptions.legacyEntityDistance.set(true);
        minecraft.options.save();
        LegacyOptions.CLIENT_STORAGE.save();
    }

    public static boolean shouldRender(Entity entity, double x, double y, double z) {
        if (!usingLegacyEntityDistance()) return true;
        int cap = cap(entity);
        return cap == 0 || entity.distanceToSqr(x, y, z) <= (double) cap * cap;
    }

    public static boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        if (!usingLegacyEntityDistance()) return true;
        if (cameraPos == null) return true;
        return !BLOCK_ENTITIES.contains(blockEntity.getType()) || Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(cameraPos) <= (double) BLOCK_ENTITY_CAP * BLOCK_ENTITY_CAP;
    }

    public static boolean usingLegacyEntityDistance() {
        return LegacyOptions.legacyEntityDistance.get();
    }

    private static int cap(Entity entity) {
        Integer cap = ENTITY_CAPS.get(entity.getType());
        if (cap == null) return 0;
        if (entity instanceof LivingEntity living && living.isBaby() && !isFish(entity.getType())) return cap / 2;
        return cap;
    }

    @SafeVarargs
    private static <T> void put(Map<T, Integer> caps, int cap, T... types) {
        for (T type : types) caps.put(type, cap);
    }

    private static boolean isFish(EntityType<?> type) {
        return type == /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.COD || type == /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.SALMON || type == /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.PUFFERFISH || type == /*? if >=26.2 {*/net.minecraft.world.entity.EntityTypes/*?} else {*//*EntityType*//*?}*/.TROPICAL_FISH;
    }
}

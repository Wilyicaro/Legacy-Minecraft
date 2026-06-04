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
        put(caps, 23, EntityType.SILVERFISH, EntityType.ENDERMITE);
        put(caps, 25, EntityType.TROPICAL_FISH);
        put(caps, 27, EntityType.RABBIT);
        put(caps, 30, EntityType.COD, EntityType.SALMON);
        put(caps, 32, EntityType.CHICKEN);
        put(caps, 33, EntityType.CAT);
        put(caps, 40, EntityType.CAVE_SPIDER, EntityType.BAT);
        put(caps, 41, EntityType.PARROT, EntityType.WOLF);
        put(caps, 42, EntityType.OCELOT);
        put(caps, 49, EntityType.PHANTOM);
        put(caps, 51, EntityType.SQUID, EntityType.PUFFERFISH, EntityType.DOLPHIN);
        put(caps, 55, EntityType.GUARDIAN, EntityType.MINECART, EntityType.CHEST_MINECART, EntityType.FURNACE_MINECART, EntityType.HOPPER_MINECART, EntityType.TNT_MINECART, EntityType.SPAWNER_MINECART, EntityType.COMMAND_BLOCK_MINECART);
        put(caps, 57, EntityType.PIG);
        put(caps, 59, EntityType.TURTLE);
        put(caps, 61, EntityType.CREEPER);
        put(caps, 64, EntityType.BLAZE, EntityType.SHULKER, EntityType.VILLAGER, EntityType.WANDERING_TRADER);
        put(caps, 66, EntityType.ZOMBIE, EntityType.SHEEP);
        put(caps, 67, EntityType.ZOMBIE_VILLAGER, EntityType.HUSK, EntityType.DROWNED, EntityType.WITCH, EntityType.ZOMBIFIED_PIGLIN, EntityType.EVOKER, EntityType.VINDICATOR, EntityType.PILLAGER);
        put(caps, 68, EntityType.SKELETON, EntityType.STRAY, EntityType.COW);
        put(caps, 69, EntityType.SNOW_GOLEM);
        put(caps, 79, EntityType.WITHER_SKELETON, EntityType.SPIDER, EntityType.IRON_GOLEM);
        put(caps, 80, EntityType.GHAST, EntityType.ENDERMAN, EntityType.RAVAGER, EntityType.ELDER_GUARDIAN, EntityType.POLAR_BEAR, EntityType.PANDA, EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.WITHER);
        put(caps, 159, EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.PAINTING);
    });
    private static final Set<BlockEntityType<?>> BLOCK_ENTITIES = Set.of(BlockEntityType.CHEST, BlockEntityType.TRAPPED_CHEST, BlockEntityType.ENDER_CHEST, BlockEntityType.BANNER, BlockEntityType.SIGN, BlockEntityType.HANGING_SIGN);

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
        return type == EntityType.COD || type == EntityType.SALMON || type == EntityType.PUFFERFISH || type == EntityType.TROPICAL_FISH;
    }
}

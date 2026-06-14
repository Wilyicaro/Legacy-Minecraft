package wily.legacy.api.client.leaderboards;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.legacy.client.screen.LegacyIconHolder;

import java.util.Objects;
import java.util.function.BiFunction;

@FunctionalInterface
public interface GlobalLeaderboardIcon {
    SimpleLayoutRenderable create(int width, int height);

    static GlobalLeaderboardIcon empty() {
        return (width, height) -> SimpleLayoutRenderable.create(width, height, layout -> (graphics, mouseX, mouseY, delta) -> {
        });
    }

    static GlobalLeaderboardIcon item(ItemLike item) {
        Objects.requireNonNull(item, "item");
        return (width, height) -> {
            LegacyIconHolder holder = new LegacyIconHolder(width, height);
            holder.itemIcon = item.asItem().getDefaultInstance();
            return holder;
        };
    }

    static GlobalLeaderboardIcon item(ItemStack item) {
        Objects.requireNonNull(item, "item");
        return (width, height) -> {
            LegacyIconHolder holder = new LegacyIconHolder(width, height);
            holder.itemIcon = item;
            return holder;
        };
    }

    static GlobalLeaderboardIcon entity(EntityType<?> entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return (width, height) -> LegacyIconHolder.entityHolder(0, 0, width, height, entityType);
    }

    static GlobalLeaderboardIcon sprite(ResourceLocation sprite) {
        Objects.requireNonNull(sprite, "sprite");
        return (width, height) -> {
            LegacyIconHolder holder = new LegacyIconHolder(width, height);
            holder.iconSprite = sprite;
            return holder;
        };
    }

    static GlobalLeaderboardIcon custom(BiFunction<Integer, Integer, SimpleLayoutRenderable> factory) {
        Objects.requireNonNull(factory, "factory");
        return (width, height) -> {
            SimpleLayoutRenderable renderable = factory.apply(width, height);
            return renderable == null ? empty().create(width, height) : renderable;
        };
    }
}

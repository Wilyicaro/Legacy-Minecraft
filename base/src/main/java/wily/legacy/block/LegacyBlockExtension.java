package wily.legacy.block;

import net.minecraft.world.level.block.state.BlockState;

public interface LegacyBlockExtension {
    default boolean l4j$isFreeForFalling(BlockState state) {
        return false;
    }
}

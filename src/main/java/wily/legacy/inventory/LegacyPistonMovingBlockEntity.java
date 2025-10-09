package wily.legacy.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.TagValueInput;

public interface LegacyPistonMovingBlockEntity {
    CompoundTag getMovedBlockEntityTag();
    void setMovedBlockEntityTag(CompoundTag tag);
    BlockEntity getRenderingBlockEntity();
    void setRenderingBlockEntity(BlockEntity entity);
    BlockEntityType<?> getMovingBlockEntityType();
    void setMovingBlockEntityType(BlockEntityType<?> type);
    void createRenderingBlockEntity(Level level);

    default void load() {
        if (this instanceof BlockEntity be) {
            load(be.getLevel().getBlockEntity(be.getBlockPos()));
        }
    }

    default void load(BlockEntity blockEntity) {
        if (getMovedBlockEntityTag() == null) return;
        if (blockEntity != null) blockEntity.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, blockEntity.getLevel().registryAccess(), getMovedBlockEntityTag()));
    }
}

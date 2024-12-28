package wily.legacy.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface LegacyPistonMovingBlockEntity {
    CompoundTag getMovedBlockEntityTag();
    void setMovedBlockEntityTag(CompoundTag tag);
    default void load(){
        if (getMovedBlockEntityTag() == null) return;
        if (this instanceof BlockEntity blockEntity){
            BlockEntity be;
            if ((be = blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos())) != null) be./*? if <1.20.5 {*//*load*//*?} else {*/loadCustomOnly/*?}*/(getMovedBlockEntityTag()/*? if >=1.20.5 {*/, blockEntity.getLevel().registryAccess()/*?}*/);
        }
    }
}

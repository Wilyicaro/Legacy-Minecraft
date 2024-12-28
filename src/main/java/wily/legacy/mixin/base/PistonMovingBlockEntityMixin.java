package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.inventory.LegacyPistonMovingBlockEntity;

@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin implements LegacyPistonMovingBlockEntity {
    @Shadow private boolean isSourcePiston;
    @Unique
    CompoundTag movedBeTag;
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", shift = At.Shift.AFTER))
    private static void tick(Level level, BlockPos blockPos, BlockState blockState, PistonMovingBlockEntity pistonMovingBlockEntity, CallbackInfo ci) {
        if (pistonMovingBlockEntity instanceof LegacyPistonMovingBlockEntity be) be.load();
    }
    @Inject(method = "finalTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", shift = At.Shift.AFTER))
    private void tick(CallbackInfo ci) {
        if (!isSourcePiston) load();
    }
    @Inject(method = /*? if <1.20.5 {*//*"load"*//*?} else {*/"loadAdditional"/*?}*/, at = @At("RETURN"))
    protected void load(CompoundTag compoundTag/*? if >=1.20.5 {*/, HolderLookup.Provider provider/*?}*/, CallbackInfo ci) {
        CompoundTag newMovedBeTag = compoundTag.getCompound("movedBlockEntityTag");
        if (!newMovedBeTag.isEmpty()) movedBeTag = newMovedBeTag;
    }
    @Inject(method = "saveAdditional", at = @At("RETURN"))
    protected void saveAdditional(CompoundTag compoundTag/*? if >=1.20.5 {*/, HolderLookup.Provider provider/*?}*/, CallbackInfo ci) {
        if (movedBeTag != null) compoundTag.put("movedBlockEntityTag",movedBeTag);
    }
    @Override
    public CompoundTag getMovedBlockEntityTag() {
        return movedBeTag;
    }

    @Override
    public void setMovedBlockEntityTag(CompoundTag tag) {
        this.movedBeTag = tag;
    }
}

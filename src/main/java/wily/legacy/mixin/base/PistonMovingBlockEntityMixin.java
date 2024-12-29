package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.inventory.LegacyPistonMovingBlockEntity;

@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin extends BlockEntity implements LegacyPistonMovingBlockEntity {
    @Shadow private boolean isSourcePiston;
    @Shadow private BlockState movedState;
    @Unique
    CompoundTag movedBeTag;
    @Unique
    BlockEntity movingRendererBlockEntity;
    @Unique
    BlockEntityType<?> movingBlockEntityType;

    public PistonMovingBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

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
        ResourceLocation beTypeId = compoundTag.contains("movedBlockEntityType") ? FactoryAPI.createLocation(compoundTag.getString("movedBlockEntityType")) : null;
        if (beTypeId != null) movingBlockEntityType = FactoryAPIPlatform.getRegistryValue(beTypeId,BuiltInRegistries.BLOCK_ENTITY_TYPE);
        if (!newMovedBeTag.isEmpty()) {
            movedBeTag = newMovedBeTag;
            if (level.isClientSide() && movingBlockEntityType != null) createRenderingBlockEntity(getLevel());
        }
    }
    @Inject(method = "saveAdditional", at = @At("RETURN"))
    protected void saveAdditional(CompoundTag compoundTag/*? if >=1.20.5 {*/, HolderLookup.Provider provider/*?}*/, CallbackInfo ci) {
        if (movedBeTag != null) compoundTag.put("movedBlockEntityTag",movedBeTag);
        if (movingBlockEntityType != null) compoundTag.putString("movedBlockEntityType", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(movingBlockEntityType).toString());
    }
    @Override
    public CompoundTag getMovedBlockEntityTag() {
        return movedBeTag;
    }

    @Override
    public void setMovedBlockEntityTag(CompoundTag tag) {
        this.movedBeTag = tag;
    }

    @Override
    public BlockEntity getRenderingBlockEntity() {
        return movingRendererBlockEntity;
    }

    @Override
    public void setRenderingBlockEntity(BlockEntity entity) {
        load(movingRendererBlockEntity = entity);
    }

    @Override
    public BlockEntityType<?> getMovingBlockEntityType() {
        return movingBlockEntityType;
    }

    @Override
    public void setMovingBlockEntityType(BlockEntityType<?> type) {
        movingBlockEntityType = type;
    }

    @Override
    public void createRenderingBlockEntity(Level level) {
        BlockEntity entity = movingBlockEntityType.create(getBlockPos(),movedState);
        entity.setLevel(level);
        setRenderingBlockEntity(entity);
    }
}

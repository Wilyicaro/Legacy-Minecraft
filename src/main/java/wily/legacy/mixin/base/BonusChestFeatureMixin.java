package wily.legacy.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.BonusChestFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BonusChestFeature.class)
public class BonusChestFeatureMixin {
    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",ordinal = 0))
    public boolean place(WorldGenLevel instance, BlockPos blockPos, BlockState blockState, int i) {
        boolean b = instance.setBlock(blockPos,blockState,i);
        CompoundTag customNameTag = new CompoundTag();
        customNameTag.putString("CustomName", Component.Serializer.toJson(Component.translatable("selectWorld.bonusItems")/*? if >=1.20.5 {*/, instance.registryAccess()/*?}*/));
        instance.getBlockEntity(blockPos, BlockEntityType.CHEST).ifPresent(e-> e./*? if <1.20.5 {*//*load(customNameTag)*//*?} else {*/loadCustomOnly(customNameTag,instance.registryAccess())/*?}*/);
        return b;
    }
}

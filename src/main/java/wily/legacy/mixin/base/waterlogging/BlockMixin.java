package wily.legacy.mixin.base.waterlogging;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.util.LegacyWaterlogging;

import java.util.function.Function;

@Mixin(Block.class)
public class BlockMixin {
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/StateDefinition$Builder;create(Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/StateDefinition$Factory;)Lnet/minecraft/world/level/block/state/StateDefinition;"
        )
    )
    private StateDefinition<Block, BlockState> legacy$addWaterloggingState(
        StateDefinition.Builder<Block, BlockState> builder,
        Function<Block, BlockState> defaultStateGetter,
        StateDefinition.Factory<Block, BlockState> factory,
        BlockBehaviour.Properties properties
    ) {
        if (!LegacyWaterlogging.supportsWaterlogging((Block) (Object) this)) {
            return builder.create(defaultStateGetter, factory);
        }

        if (!legacy$hasProperty(builder, LegacyWaterlogging.WATERLOGGED)) {
            builder.add(LegacyWaterlogging.WATERLOGGED);
        }
        if (LegacyWaterlogging.supportsPassThroughWaterLevel((Block) (Object) this) && !legacy$hasProperty(builder, LegacyWaterlogging.PASS_THROUGH_WATER_LEVEL)) {
            builder.add(LegacyWaterlogging.PASS_THROUGH_WATER_LEVEL);
        }
        return builder.create(defaultStateGetter, factory);
    }

    @ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
    private BlockState legacy$normalizeDefaultState(BlockState state) {
        return LegacyWaterlogging.normalizeDefaultState(state);
    }

    private static boolean legacy$hasProperty(StateDefinition.Builder<Block, BlockState> builder, Property<?> property) {
        return ((StateDefinitionBuilderAccessor) builder).legacy$getProperties().containsKey(property.getName());
    }
}

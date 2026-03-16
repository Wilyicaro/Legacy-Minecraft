package wily.legacy.mixin.base.waterlogging;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.util.LegacyWaterlogging;

import java.util.function.Function;

@Mixin(Blocks.class)
public class BlocksMixin {
    @Inject(
        method = "register(Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Lnet/minecraft/world/level/block/Block;",
        at = @At("HEAD")
    )
    private static void legacy$beginRegistration(
        ResourceKey<Block> key,
        Function<BlockBehaviour.Properties, Block> factory,
        BlockBehaviour.Properties properties,
        CallbackInfoReturnable<Block> cir
    ) {
        LegacyWaterlogging.beginBlockRegistration(key.location().toString());
    }

    @Inject(
        method = "register(Lnet/minecraft/resources/ResourceKey;Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Lnet/minecraft/world/level/block/Block;",
        at = @At("RETURN")
    )
    private static void legacy$endRegistration(
        ResourceKey<Block> key,
        Function<BlockBehaviour.Properties, Block> factory,
        BlockBehaviour.Properties properties,
        CallbackInfoReturnable<Block> cir
    ) {
        LegacyWaterlogging.endBlockRegistration();
    }
}

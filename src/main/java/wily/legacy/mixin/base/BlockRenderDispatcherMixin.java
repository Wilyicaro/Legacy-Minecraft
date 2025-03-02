package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.Legacy4JClient;

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {
    //? if forge {
    /*@ModifyExpressionValue(method = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/resources/model/BakedModel;"))
    public BakedModel renderBatched(BakedModel original, BlockState blockState, BlockPos blockPos, BlockAndTintGetter blockAndTintGetter) {
        return Legacy4JClient.getFastLeavesModelReplacement(blockAndTintGetter, blockPos, blockState, original);
    }
    *///?} else if neoforge {
    /*@ModifyExpressionValue(method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/resources/model/BakedModel;"))
    public BakedModel renderBatched(BakedModel original, BlockState blockState, BlockPos blockPos, BlockAndTintGetter blockAndTintGetter) {
        return Legacy4JClient.getFastLeavesModelReplacement(blockAndTintGetter, blockPos, blockState, original);
    }
    *///?} else {
    @ModifyExpressionValue(method = "renderBatched", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/resources/model/BakedModel;"))
    public BakedModel renderBatched(BakedModel original, BlockState blockState, BlockPos blockPos, BlockAndTintGetter blockAndTintGetter) {
        return Legacy4JClient.getFastLeavesModelReplacement(blockAndTintGetter, blockPos, blockState, original);
    }
    //?}

}

//? if >1.21.4 {
package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.Legacy4JClient;

@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {
    @ModifyExpressionValue(method = /*? if neoforge {*//*"compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;"*//*?} else {*/"compile"/*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;getBlockModel(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/block/model/BlockStateModel;"))
    public BlockStateModel compile(BlockStateModel original, SectionPos sectionPos, RenderSectionRegion renderChunkRegion, @Local(ordinal = 2) BlockPos pos, @Local BlockState blockState) {
        return Legacy4JClient.getFastLeavesModelReplacement(renderChunkRegion, pos, blockState, original);
    }
}
//?}

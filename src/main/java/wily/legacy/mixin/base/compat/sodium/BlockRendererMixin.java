//? fabric || neoforge {
package wily.legacy.mixin.base.compat.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;

import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class BlockRendererMixin extends AbstractBlockRenderContext {
    @Inject(method = "renderModel", at = @At("HEAD"))
    public void renderModel(/*? if <1.21.5 {*//*BakedModel*//*?} else {*/BlockStateModel/*?}*/ model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci, @Local(argsOnly = true) LocalRef</*? if <1.21.5 {*//*BakedModel*//*?} else {*/BlockStateModel/*?}*/> bakedModelLocalRef) {
        bakedModelLocalRef.set(Legacy4JClient.getFastLeavesModelReplacement(slice, pos, state, model));
    }
}
//?}
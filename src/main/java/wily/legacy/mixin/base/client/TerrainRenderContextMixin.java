//? if fabric {
package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.TerrainRenderContext;
import net.minecraft.client.Minecraft;
//? if <1.21.5 {
/*import net.minecraft.client.resources.model.BakedModel;
*///?} else {
import net.minecraft.client.renderer.block.model.BlockStateModel;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;

@Mixin(TerrainRenderContext.class)
public class TerrainRenderContextMixin {
    @Inject(method = "bufferModel", at = @At("HEAD"))
    public void bufferModel(BlockStateModel model, BlockState blockState, BlockPos blockPos, CallbackInfo ci, @Local(argsOnly = true) LocalRef<BlockStateModel> bakedModelLocalRef) {
        bakedModelLocalRef.set(Legacy4JClient.getFastLeavesModelReplacement(Minecraft.getInstance().level, blockPos, blockState, model));
    }
}
//?}
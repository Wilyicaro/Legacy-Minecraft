//? if >=1.21 && (fabric || neoforge) {
package wily.legacy.mixin.base.compat.sodium;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
//? if >=1.21 {
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
//?} else {
/*import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
*///?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;

@Mixin(value = BlockRenderer.class, remap = false)
public class BlockRendererMixin {
    @Inject(method = "renderModel", at = @At("HEAD"))
    public void renderModel(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci, @Local(argsOnly = true) LocalRef<BakedModel> bakedModelLocalRef) {
        bakedModelLocalRef.set(Legacy4JClient.getFastLeavesModelReplacement(Minecraft.getInstance().level, pos, state, model));
    }
}
//?}
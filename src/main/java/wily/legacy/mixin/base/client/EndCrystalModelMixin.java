package wily.legacy.mixin.base.client;

import net.minecraft.client.model.geom.ModelPart;
//? if <1.21.11 {
/*import net.minecraft.client.model.EndCrystalModel;
 *///?} else {
import net.minecraft.client.model.object.crystal.EndCrystalModel;
//?}
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.CommonValue;

@Mixin(EndCrystalModel.class)
public class EndCrystalModelMixin {
    @Shadow
    @Final
    public ModelPart innerGlass;

    @Shadow
    @Final
    public ModelPart cube;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;)V", at = @At("TAIL"))
    private void setupAnim(EndCrystalRenderState renderState, CallbackInfo ci) {
        boolean ps4Model = CommonValue.PS4_END_CRYSTAL_MODEL.get();
        innerGlass.skipDraw = ps4Model;
        if (ps4Model) {
            cube.xScale = 1.0F;
            cube.yScale = 1.0F;
            cube.zScale = 1.0F;
        }
    }
}

package wily.legacy.mixin.base.client;

import net.minecraft.client.model.EndCrystalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.util.client.LegacyRenderUtil;

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
        boolean consoleAspects = LegacyRenderUtil.hasConsoleAspects();
        innerGlass.skipDraw = consoleAspects;
        if (consoleAspects) {
            cube.xScale = 1.0F;
            cube.yScale = 1.0F;
            cube.zScale = 1.0F;
        }
    }
}

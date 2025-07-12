package wily.legacy.mixin.base.client;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
//?}
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;

@Mixin(VillagerModel.class)
public class VillagerModelMixin {
    @Shadow @Final private ModelPart head;

    @Inject(method = "setupAnim*", at = @At("RETURN"))
    public void setupAnim(VillagerRenderState villager, CallbackInfo ci) {
        if (LegacyOptions.legacyBabyVillagerHead.get()){
            head.xScale = head.yScale = head.zScale = (villager.isBaby ? 1.5f : 1);
        }
    }
}

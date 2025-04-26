package wily.legacy.mixin.base;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
//? if >=1.21.2 {
/*import net.minecraft.client.renderer.entity.state.VillagerRenderState;
*///?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
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
    public void setupAnim(/*? if <1.21.2 {*/ Entity entity, float f, float g, float h, float i, float j/*?} else {*/ /*VillagerRenderState villager*//*?}*/, CallbackInfo ci) {
        if (/*? if <1.21.2 {*/entity instanceof Villager villager && /*?}*/LegacyOptions.legacyBabyVillagerHead.get()){
            head.xScale = head.yScale = head.zScale = (villager.isBaby/*? if <1.21.2 {*/()/*?}*/ ? 1.5f : 1);
        }
    }
}

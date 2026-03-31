package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAccessor<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> {
    @Accessor("modelSet")
    ArmorModelSet<A> consoleskins$getModelSet();

    @Accessor("babyModelSet")
    ArmorModelSet<A> consoleskins$getBabyModelSet();
}

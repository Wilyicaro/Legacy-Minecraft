package wily.legacy.mixin.base.cpm.access;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker for {@link LivingEntityRenderer#getModel()}.
 * <p>
 * NeoForge 1.21.x does not expose a stable field name for the renderer model
 * in mappings, but the accessor method is present and stable.
 */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    @Invoker("getModel")
    EntityModel<? super LivingEntityRenderState> cpm$invokeGetModel();
}

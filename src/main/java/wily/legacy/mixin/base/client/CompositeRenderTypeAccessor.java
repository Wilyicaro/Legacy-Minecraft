package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.CompositeRenderType.class)
public interface CompositeRenderTypeAccessor {
    //? if <1.21.5 {
    /*@Invoker("state")
    *///?} else {
    @Accessor("state")
    //?}
    RenderType.CompositeState getState();
}

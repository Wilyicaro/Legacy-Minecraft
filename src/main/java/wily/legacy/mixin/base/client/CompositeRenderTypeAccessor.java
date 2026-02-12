package wily.legacy.mixin.base.client;

import net.minecraft.client.renderer./*? if <1.21.11 {*//*RenderType*//*?} else {*/rendertype.*/*?}*/;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType/*? if <1.21.11 {*//*.CompositeRenderType*//*?}*/.class)
public interface CompositeRenderTypeAccessor {
    //? if <1.21.5 {
    /*@Invoker("state")
     *///?} else {
    @Accessor("state")
    //?}
    //? <1.21.11 {
    /*
    RenderType.CompositeState getState();
    *///?} else {
    RenderSetup getState();
    //?}
}

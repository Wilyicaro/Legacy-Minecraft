//? >=1.21.11 {
package wily.legacy.mixin.base;

import net.minecraft.resources.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding")
public interface TextureBindingAccessor {
    @Accessor("location")
    Identifier getLocation();
}
//?}

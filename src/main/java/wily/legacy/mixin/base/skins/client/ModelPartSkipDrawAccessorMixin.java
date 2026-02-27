package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.class)
public interface ModelPartSkipDrawAccessorMixin {
    @Accessor("skipDraw")
    boolean consoleskins$getSkipDraw();

    @Accessor("skipDraw")
    void consoleskins$setSkipDraw(boolean value);
}

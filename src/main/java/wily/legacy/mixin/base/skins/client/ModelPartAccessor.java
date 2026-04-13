package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.*;

@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("skipDraw")
    boolean consoleskins$getSkipDraw();
    @Accessor("skipDraw")
    void consoleskins$setSkipDraw(boolean value);
    @Accessor("cubes")
    List<ModelPart.Cube> consoleskins$getCubes();
    @Accessor("children")
    Map<String, ModelPart> consoleskins$getChildren();
}

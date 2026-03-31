package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
import java.util.Map;

@Mixin(ModelPart.class)
public interface ModelPartAccessor {
    @Accessor("cubes")
    List<ModelPart.Cube> consoleskins$getCubes();
    @Accessor("cubes")
    void consoleskins$setCubes(List<ModelPart.Cube> cubes);
    @Accessor("children")
    Map<String, ModelPart> consoleskins$getChildren();
    @Accessor("children")
    void consoleskins$setChildren(Map<String, ModelPart> children);
}

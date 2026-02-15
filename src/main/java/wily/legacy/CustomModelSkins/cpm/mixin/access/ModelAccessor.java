package wily.legacy.CustomModelSkins.cpm.mixin.access;

// Accessor for vanilla Model internals (render parts + flags).

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Model.class)
public interface ModelAccessor {
    @Accessor("root")
    ModelPart cpm$getRoot();

    @Accessor("root")
    @Mutable
    void cpm$setRoot(ModelPart root);
}

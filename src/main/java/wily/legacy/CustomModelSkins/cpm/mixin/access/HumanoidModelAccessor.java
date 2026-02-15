package wily.legacy.CustomModelSkins.cpm.mixin.access;

// Accessor mixin for vanilla humanoid model internals.

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HumanoidModel.class)
public interface HumanoidModelAccessor {
    @Accessor("head")
    ModelPart cpm$getHead();

    @Accessor("head")
    @Mutable
    void cpm$setHead(ModelPart p);

    @Accessor("hat")
    ModelPart cpm$getHat();

    @Accessor("hat")
    @Mutable
    void cpm$setHat(ModelPart p);

    @Accessor("body")
    @Mutable
    void cpm$setBody(ModelPart p);

    @Accessor("rightArm")
    @Mutable
    void cpm$setRightArm(ModelPart p);

    @Accessor("leftArm")
    @Mutable
    void cpm$setLeftArm(ModelPart p);

    @Accessor("rightLeg")
    @Mutable
    void cpm$setRightLeg(ModelPart p);

    @Accessor("leftLeg")
    @Mutable
    void cpm$setLeftLeg(ModelPart p);
}

package wily.legacy.CustomModelSkins.cpm.mixin.access;

// Accessor for PlayerModel internals used by CPM renderer.

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerModel.class)
public interface PlayerModelAccessor {
    @Accessor("leftSleeve")
    @Mutable
    void cpm$setLeftSleeve(ModelPart p);

    @Accessor("rightSleeve")
    @Mutable
    void cpm$setRightSleeve(ModelPart p);

    @Accessor("leftPants")
    @Mutable
    void cpm$setLeftPants(ModelPart p);

    @Accessor("rightPants")
    @Mutable
    void cpm$setRightPants(ModelPart p);

    @Accessor("jacket")
    @Mutable
    void cpm$setJacket(ModelPart p);
}

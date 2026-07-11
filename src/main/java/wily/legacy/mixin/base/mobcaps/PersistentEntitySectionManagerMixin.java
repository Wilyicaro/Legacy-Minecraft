package wily.legacy.mixin.base.mobcaps;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.LegacyMobCaps;

@Mixin(PersistentEntitySectionManager.class)
public class PersistentEntitySectionManagerMixin {
    @Inject(method = "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z", at = @At("RETURN"))
    private void addEntity(EntityAccess entity, boolean existing, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && entity instanceof Entity tracked) {
            LegacyMobCaps.handleEntityAdded(tracked);
        }
    }
}

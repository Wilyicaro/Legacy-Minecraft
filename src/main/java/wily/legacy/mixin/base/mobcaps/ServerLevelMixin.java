package wily.legacy.mixin.base.mobcaps;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.mobcaps.LegacyMobCaps;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "addEntity", at = @At("RETURN"))
    private void addEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            LegacyMobCaps.handleEntityAdded(entity);
        }
    }
}

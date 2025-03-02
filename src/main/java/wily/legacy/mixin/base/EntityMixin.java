package wily.legacy.mixin.base;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "setCustomName", at = @At("RETURN"))
    public void setCustomName(Component component, CallbackInfo ci) {
        if (((Object)this) instanceof Mob m) m.setPersistenceRequired();
    }
}

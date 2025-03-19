package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.init.LegacyGameRules;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract boolean isInWater();

    @Shadow public abstract Level level();

    @Inject(method = "setCustomName", at = @At("RETURN"))
    public void setCustomName(Component component, CallbackInfo ci) {
        if (((Object)this) instanceof Mob m) m.setPersistenceRequired();
    }

    @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"))
    protected boolean updateSwimming(boolean original) {
        return ((!level().isClientSide && level().getServer().getGameRules().getBoolean(LegacyGameRules.LEGACY_SWIMMING)) && this.isInWater() || original);
    }
}

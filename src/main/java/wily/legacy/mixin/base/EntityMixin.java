package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.init.LegacyGameRules;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private Entity self() {
        return (Entity) (Object) this;
    }

    @Inject(method = "setCustomName", at = @At("RETURN"))
    public void setCustomName(Component component, CallbackInfo ci) {
        if (self() instanceof Mob m) m.setPersistenceRequired();
    }

    //? if neoforge {
    /*@ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;canStartSwimming()Z", remap = false))
    protected boolean updateSwimming(boolean original) {
        return (LegacyGameRules.getSidedBooleanGamerule(self(), LegacyGameRules.LEGACY_SWIMMING) && (self().isInWater() && self().getXRot() > 0) || original) && !(self() instanceof Player p && p.getAbilities().flying);
    }
    *///?} else {
    @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"))
    protected boolean updateSwimming(boolean original) {
        return (LegacyGameRules.getSidedBooleanGamerule(self(), LegacyGameRules.LEGACY_SWIMMING) && (self().isInWater() && self().getXRot() > 0) || original) && !(self() instanceof Player p && p.getAbilities().flying);
    }
    //?}
}

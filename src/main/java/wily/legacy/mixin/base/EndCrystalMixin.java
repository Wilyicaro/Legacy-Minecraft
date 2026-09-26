package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.world.PlayerTrustAdmin;

@Mixin(EndCrystal.class)
public class EndCrystalMixin {
    @WrapMethod(method = "hurtServer")
    private boolean legacy$attributeExplosion(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        return PlayerTrustAdmin.withResponsiblePlayer(PlayerTrustAdmin.getResponsiblePlayer(level, source), () -> original.call(level, source, amount));
    }
}

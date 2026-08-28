package wily.legacy.mixin.base.villager_spawn_egg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if <=1.20.1 {
import wily.legacy.util.SpawnEggProfessionBehavior;
//?}

@Mixin(ResetProfession.class)
public class ResetProfessionMixin {
    //? if <=1.20.1 {
    @ModifyReturnValue(method = "create", at = @At("RETURN"))
    private static BehaviorControl<Villager> wrapSpawnEggProfessionBehavior(BehaviorControl<Villager> original) {
        return SpawnEggProfessionBehavior.reset(original);
    }
    //?}
    //? if >1.20.1 {
    /*@ModifyExpressionValue(method = /^? if fabric {^/"method_47038"/^?} else {^//^"lambda$create$0"^//^?}^/, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;getVillagerXp()I"))
    private static int keepSpawnEggProfession(int original, ServerLevel level, Villager villager, long time) {
        return villager.getTags().contains("legacy_spawn_egg_profession") ? 1 : original;
    }
    *///?}
}

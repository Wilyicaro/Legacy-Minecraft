package wily.legacy.mixin.base.villager_spawn_egg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <=1.20.1 {
import wily.legacy.util.SpawnEggProfessionBehavior;
//?}

@Mixin(AssignProfessionFromJobSite.class)
public class AssignProfessionMixin {
    //? if <=1.20.1 {
    @ModifyReturnValue(method = "create", at = @At("RETURN"))
    private static BehaviorControl<Villager> wrapSpawnEggProfessionBehavior(BehaviorControl<Villager> original) {
        return SpawnEggProfessionBehavior.assign(original);
    }
    //?}
    //? if >1.20.1 && <1.21.5 {
    /*@ModifyExpressionValue(method = /^? if fabric {^/"method_46890"/^?} else {^//^"lambda$create$4"^//^?}^/, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/VillagerData;getProfession()Lnet/minecraft/world/entity/npc/VillagerProfession;"))
    private static VillagerProfession allowSpawnEggProfessionSwap(VillagerProfession original, BehaviorBuilder.Instance<?> instance, MemoryAccessor<?, ?> potentialJobSite, MemoryAccessor<?, ?> jobSite, ServerLevel level, Villager villager, long time) {
        return original != VillagerProfession.NONE && villager.getVillagerXp() == 0 && villager.getTags().contains("legacy_spawn_egg_profession") ? VillagerProfession.NONE : original;
    }
    *///?} else if >=1.21.5 {
    /*@ModifyExpressionValue(method = /^? if fabric {^/"method_46890"/^?} else {^//^"lambda$create$4"^//^?}^/, at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;is(Lnet/minecraft/resources/ResourceKey;)Z"))
    private static boolean allowSpawnEggProfessionSwap(boolean original, BehaviorBuilder.Instance<?> instance, MemoryAccessor<?, ?> potentialJobSite, MemoryAccessor<?, ?> jobSite, ServerLevel level, Villager villager, long time) {
        return original || villager.getVillagerXp() == 0 && villager.getTags().contains("legacy_spawn_egg_profession");
    }
    *///?}

    //? if >1.20.1 && <1.21.5 {
    /*@Inject(method = /^? if fabric {^/"method_46891"/^?} else {^//^"lambda$create$3"^//^?}^/, at = @At("RETURN"))
    private static void clearSpawnEggProfessionTag(Villager villager, ServerLevel level, VillagerProfession profession, CallbackInfo ci) {
        villager.playSound(villager.getNotifyTradeSound(), 1.0f, villager.getVoicePitch());
        villager.removeTag("legacy_spawn_egg_profession");
    }
    *///?} else if >=1.21.5 {
    /*@Inject(method = /^? if fabric {^/"method_46891"/^?} else {^//^"lambda$create$3"^//^?}^/, at = @At("RETURN"))
    private static void clearSpawnEggProfessionTag(Villager villager, ServerLevel level, Holder.Reference<?> profession, CallbackInfo ci) {
        villager.playSound(villager.getNotifyTradeSound(), 1.0f, villager.getVoicePitch());
        villager.removeTag("legacy_spawn_egg_profession");
    }
    *///?}
}

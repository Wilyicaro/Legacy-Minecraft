package wily.legacy.mixin.base.villager_spawn_egg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AssignProfessionFromJobSite.class)
public class AssignProfessionMixin {
    //? if fabric {
    @ModifyExpressionValue(method = "lambda$create$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;is(Lnet/minecraft/resources/ResourceKey;)Z"))
    private static boolean allowSpawnEggProfessionSwap(boolean original, BehaviorBuilder.Instance<?> instance, MemoryAccessor<?, ?> potentialJobSite, MemoryAccessor<?, ?> jobSite, ServerLevel level, Villager villager, long time) {
        return original || villager.getVillagerXp() == 0 && villager.entityTags().contains("legacy_spawn_egg_profession");
    }

    @Inject(method = "lambda$create$6", at = @At("RETURN"))
    private static void clearSpawnEggProfessionTag(Villager villager, ServerLevel level, Holder.Reference<?> profession, CallbackInfo ci) {
        villager.removeTag("legacy_spawn_egg_profession");
    }
    //?} else {
    /*@ModifyExpressionValue(method = "lambda$create$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;is(Lnet/minecraft/resources/ResourceKey;)Z"))
    private static boolean allowSpawnEggProfessionSwap(boolean original, BehaviorBuilder.Instance<?> instance, MemoryAccessor<?, ?> potentialJobSite, MemoryAccessor<?, ?> jobSite, ServerLevel level, Villager villager, long time) {
        return original || villager.getVillagerXp() == 0 && villager.entityTags().contains("legacy_spawn_egg_profession");
    }

    @Inject(method = "lambda$create$6", at = @At("RETURN"))
    private static void clearSpawnEggProfessionTag(Villager villager, ServerLevel level, Holder.Reference<?> profession, CallbackInfo ci) {
        villager.removeTag("legacy_spawn_egg_profession");
    }
    *///?}
}

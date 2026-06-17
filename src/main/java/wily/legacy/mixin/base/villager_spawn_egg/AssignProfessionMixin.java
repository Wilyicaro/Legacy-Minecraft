package wily.legacy.mixin.base.villager_spawn_egg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AssignProfessionFromJobSite.class)
public class AssignProfessionMixin {
    //? if <1.21.5 {
    @ModifyExpressionValue(method = "method_46890", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/VillagerData;getProfession()Lnet/minecraft/world/entity/npc/VillagerProfession;"))
    private static VillagerProfession allowSpawnEggProfessionSwap(VillagerProfession original, BehaviorBuilder.Instance<?> instance, MemoryAccessor<?, ?> potentialJobSite, MemoryAccessor<?, ?> jobSite, ServerLevel level, Villager villager, long time) {
        return original != VillagerProfession.NONE && villager.getVillagerXp() == 0 && villager.getTags().contains("legacy_spawn_egg_profession") ? VillagerProfession.NONE : original;
    }
    //?} else {
    /*@ModifyExpressionValue(method = "method_46890", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;is(Lnet/minecraft/resources/ResourceKey;)Z"))
    private static boolean allowSpawnEggProfessionSwap(boolean original, BehaviorBuilder.Instance<?> instance, MemoryAccessor<?, ?> potentialJobSite, MemoryAccessor<?, ?> jobSite, ServerLevel level, Villager villager, long time) {
        return original || villager.getVillagerXp() == 0 && villager.getTags().contains("legacy_spawn_egg_profession");
    }
    *///?}

    @Inject(method = "method_46891", at = @At("RETURN"))
    private static void clearSpawnEggProfessionTag(Villager villager, ServerLevel level, /*? if >=1.21.5 {*//*Holder.Reference<?>*//*?} else {*/VillagerProfession/*?}*/ profession, CallbackInfo ci) {
        villager.playSound(villager.getNotifyTradeSound(), 1.0f, villager.getVoicePitch());
        villager.removeTag("legacy_spawn_egg_profession");
    }
}

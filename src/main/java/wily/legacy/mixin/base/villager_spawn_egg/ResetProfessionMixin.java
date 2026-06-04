package wily.legacy.mixin.base.villager_spawn_egg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ResetProfession.class)
public class ResetProfessionMixin {
    //? if fabric {
    @ModifyExpressionValue(method = "lambda$create$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;getVillagerXp()I"))
    private static int keepSpawnEggProfession(int original, ServerLevel level, Villager villager, long time) {
        return villager.entityTags().contains("legacy_spawn_egg_profession") ? 1 : original;
    }
    //?} else {
    /*@ModifyExpressionValue(method = "lambda$create$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;getVillagerXp()I"))
    private static int keepSpawnEggProfession(int original, ServerLevel level, Villager villager, long time) {
        return villager.entityTags().contains("legacy_spawn_egg_profession") ? 1 : original;
    }
    *///?}
}

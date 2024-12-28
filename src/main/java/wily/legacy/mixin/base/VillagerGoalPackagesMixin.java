package wily.legacy.mixin.base;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.entity.BabyVillagerFollowIronGolemBehavior;

import java.util.ArrayList;
import java.util.List;

@Mixin(VillagerGoalPackages.class)
public class VillagerGoalPackagesMixin {
    @Inject(method = "getCorePackage", at = @At("RETURN"), cancellable = true)
    private static void getCorePackage(VillagerProfession villagerProfession, float f, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> list = new ArrayList<>(cir.getReturnValue());
        list.add(Pair.of(7,new BabyVillagerFollowIronGolemBehavior()));
        cir.setReturnValue(ImmutableList.copyOf(list));
    }
}

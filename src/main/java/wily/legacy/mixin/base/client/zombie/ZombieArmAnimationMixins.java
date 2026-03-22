package wily.legacy.mixin.base.client.zombie;

import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ZombifiedPiglinRenderer;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;

@Mixin(AbstractZombieRenderer.class)
abstract class AbstractZombieRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void legacy$clearAggressivePose(Zombie zombie, ZombieRenderState state, float partialTick, CallbackInfo ci) {
        if (!LegacyOptions.legacyZombieAggressionAnimation.get()) return;
        state.isAggressive = false;
    }
}

@Mixin(ZombieVillagerRenderer.class)
abstract class ZombieVillagerRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void legacy$clearAggressivePose(ZombieVillager zombieVillager, ZombieVillagerRenderState state, float partialTick, CallbackInfo ci) {
        if (!LegacyOptions.legacyZombieAggressionAnimation.get()) return;
        state.isAggressive = false;
    }
}

@Mixin(ZombifiedPiglinRenderer.class)
abstract class ZombifiedPiglinRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void legacy$clearAggressivePose(ZombifiedPiglin zombifiedPiglin, ZombifiedPiglinRenderState state, float partialTick, CallbackInfo ci) {
        if (!LegacyOptions.legacyZombieAggressionAnimation.get()) return;
        state.isAggressive = false;
    }
}

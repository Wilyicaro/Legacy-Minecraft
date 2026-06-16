package wily.legacy.mixin.base.client.zombie;

//? if <1.21.2 {
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else {
/*import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.ZombifiedPiglinRenderer;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraft.world.entity.monster./^? if >=1.21.11 {^/zombie./^?}^/Zombie;
import net.minecraft.world.entity.monster./^? if >=1.21.11 {^/zombie./^?}^/ZombieVillager;
import net.minecraft.world.entity.monster./^? if >=1.21.11 {^/zombie./^?}^/ZombifiedPiglin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import wily.legacy.client.LegacyOptions;

//? if <1.21.2 {
@Mixin(ZombieModel.class)
abstract class ZombieModelMixin {
    @Inject(method = "isAggressive(Lnet/minecraft/world/entity/monster/Zombie;)Z", at = @At("HEAD"), cancellable = true)
    private void legacy$clearAggressivePose(Zombie zombie, CallbackInfoReturnable<Boolean> cir) {
        if (LegacyOptions.legacyZombieAggressionAnimation.get()) cir.setReturnValue(false);
    }
}

@Mixin(ZombieVillagerModel.class)
abstract class ZombieVillagerModelMixin {
    @Redirect(method = "setupAnim(Lnet/minecraft/world/entity/monster/Zombie;FFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Zombie;isAggressive()Z"))
    private boolean legacy$clearAggressivePose(Zombie zombie) {
        return !LegacyOptions.legacyZombieAggressionAnimation.get() && zombie.isAggressive();
    }
}

@Mixin(PiglinModel.class)
abstract class PiglinModelMixin {
    @Redirect(method = "setupAnim(Lnet/minecraft/world/entity/Mob;FFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;isAggressive()Z"))
    private boolean legacy$clearZombifiedPiglinAggressivePose(Mob mob) {
        return (mob.getType() != EntityType.ZOMBIFIED_PIGLIN || !LegacyOptions.legacyZombieAggressionAnimation.get()) && mob.isAggressive();
    }
}
//?} else {
/*@Mixin(AbstractZombieRenderer.class)
abstract class AbstractZombieRendererMixin {
    @Inject(method = /^? if <1.21.11 {^/"extractRenderState(Lnet/minecraft/world/entity/monster/Zombie;Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;F)V"/^?} else {^//^"extractRenderState(Lnet/minecraft/world/entity/monster/zombie/Zombie;Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;F)V"^//^?}^/, at = @At("TAIL"))
    private void legacy$clearAggressivePose(Zombie zombie, ZombieRenderState state, float partialTick, CallbackInfo ci) {
        if (LegacyOptions.legacyZombieAggressionAnimation.get()) state.isAggressive = false;
    }
}

@Mixin(ZombieVillagerRenderer.class)
abstract class ZombieVillagerRendererMixin {
    @Inject(method = /^? if <1.21.11 {^/"extractRenderState(Lnet/minecraft/world/entity/monster/ZombieVillager;Lnet/minecraft/client/renderer/entity/state/ZombieVillagerRenderState;F)V"/^?} else {^//^"extractRenderState(Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;Lnet/minecraft/client/renderer/entity/state/ZombieVillagerRenderState;F)V"^//^?}^/, at = @At("TAIL"))
    private void legacy$clearAggressivePose(ZombieVillager zombieVillager, ZombieVillagerRenderState state, float partialTick, CallbackInfo ci) {
        if (LegacyOptions.legacyZombieAggressionAnimation.get()) state.isAggressive = false;
    }
}

@Mixin(ZombifiedPiglinRenderer.class)
abstract class ZombifiedPiglinRendererMixin {
    @Inject(method = /^? if <1.21.11 {^/"extractRenderState(Lnet/minecraft/world/entity/monster/ZombifiedPiglin;Lnet/minecraft/client/renderer/entity/state/ZombifiedPiglinRenderState;F)V"/^?} else {^//^"extractRenderState(Lnet/minecraft/world/entity/monster/zombie/ZombifiedPiglin;Lnet/minecraft/client/renderer/entity/state/ZombifiedPiglinRenderState;F)V"^//^?}^/, at = @At("TAIL"))
    private void legacy$clearAggressivePose(ZombifiedPiglin zombifiedPiglin, ZombifiedPiglinRenderState state, float partialTick, CallbackInfo ci) {
        if (LegacyOptions.legacyZombieAggressionAnimation.get()) state.isAggressive = false;
    }
}
*///?}

package wily.legacy.mixin.base;

import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.network.ClientAnimalInLoveSyncPayload;

@Mixin(Animal.class)
public abstract class AnimalMixin extends AgeableMob {
    @Unique
    int lastInlove = 0;

    @Shadow public abstract void setInLoveTime(int i);

    @Shadow private int inLove;

    protected AnimalMixin(EntityType<? extends AgeableMob> entityType, Level level) {
        super(entityType, level);
    }
    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/animal/Animal;inLove:I", opcode = Opcodes.PUTFIELD))
    public void aiStep(Animal instance, int value) {
        if (!level().isClientSide) setInLoveTime(value);
    }
    @Inject(method = "aiStep", at = @At("HEAD"))
    public void aiStep(CallbackInfo ci) {
        if (lastInlove != inLove) ClientAnimalInLoveSyncPayload.sync((Animal)(Object) this);
        lastInlove = inLove;
    }
    @Inject(method = "canFallInLove", at = @At("HEAD"), cancellable = true)
    public void aiStep(CallbackInfoReturnable<Boolean> cir) {
        if (age != 0) cir.setReturnValue(false);
    }

}

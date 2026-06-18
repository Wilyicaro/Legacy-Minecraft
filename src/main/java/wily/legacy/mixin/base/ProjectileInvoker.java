package wily.legacy.mixin.base;

import net.minecraft.world.entity.projectile.Projectile;
//? if >=1.21 {
import net.minecraft.world.entity.projectile.ProjectileDeflection;
//?}
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Projectile.class)
public interface ProjectileInvoker {
    //? if <1.21 {
    /*@Invoker("onHit")
    void legacy$onHit(HitResult hitResult);
    *///?} else {
    @Invoker("hitTargetOrDeflectSelf")
    ProjectileDeflection legacy$hitTargetOrDeflectSelf(HitResult hitResult);
    //?}
}

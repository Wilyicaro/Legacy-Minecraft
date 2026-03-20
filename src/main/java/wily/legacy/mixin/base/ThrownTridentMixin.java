package wily.legacy.mixin.base;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin extends AbstractArrow {
    @Shadow
    @Final
    private static EntityDataAccessor<Byte> ID_LOYALTY;

    protected ThrownTridentMixin(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level/*? if >1.20.2 && <1.20.5 {*//*, ItemStack.EMPTY*//*?}*/);
    }

    @Override
    protected void onBelowWorld() {
        if (!this.isNoPhysics() && getEntityData().get(ID_LOYALTY) <= 0) {
            discard();
            return;
        }
        setNoPhysics(true);
    }
}

package wily.legacy.mixin.base;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.entity.PlayerTrustOwnedSource;

import java.util.UUID;

@Mixin(Projectile.class)
public abstract class PlayerTrustOwnedSourceMixin implements PlayerTrustOwnedSource {
    @Shadow
    protected @Nullable EntityReference<Entity> owner;

    @Override
    public @Nullable UUID getResponsiblePlayer() {
        return owner == null ? null : owner.getUUID();
    }

    @Override
    public void setResponsiblePlayer(@Nullable UUID player) {
        owner = player == null ? null : EntityReference.of(player);
    }
}

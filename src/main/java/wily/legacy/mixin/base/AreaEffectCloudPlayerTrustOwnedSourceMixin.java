package wily.legacy.mixin.base;

import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.entity.PlayerTrustOwnedSource;

import java.util.UUID;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudPlayerTrustOwnedSourceMixin implements PlayerTrustOwnedSource {
    @Shadow
    private @Nullable EntityReference<LivingEntity> owner;

    @Override
    public @Nullable UUID getResponsiblePlayer() {
        return owner == null ? null : owner.getUUID();
    }

    @Override
    public void setResponsiblePlayer(@Nullable UUID player) {
        owner = player == null ? null : EntityReference.of(player);
    }
}

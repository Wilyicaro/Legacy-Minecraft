package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.entity.PlayerTrustOwnedSource;

import java.util.UUID;

@Mixin(Creeper.class)
public class CreeperMixin implements PlayerTrustOwnedSource {
    @Unique
    private UUID legacy$responsiblePlayer;

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Creeper;ignite()V"))
    private void mobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer) setResponsiblePlayer(player.getUUID());
    }

    @WrapOperation(method = "spawnLingeringCloud", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean addLingeringCloud(Level level, Entity entity, Operation<Boolean> original) {
        if (entity instanceof PlayerTrustOwnedSource source) source.setResponsiblePlayer(getResponsiblePlayer());
        return original.call(level, entity);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void addAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        if (legacy$responsiblePlayer != null) output.store("LegacyTrustOwner", UUIDUtil.CODEC, legacy$responsiblePlayer);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void readAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        setResponsiblePlayer(input.read("LegacyTrustOwner", UUIDUtil.CODEC).orElse(null));
    }

    @Override
    public @Nullable UUID getResponsiblePlayer() {
        return legacy$responsiblePlayer;
    }

    @Override
    public void setResponsiblePlayer(@Nullable UUID player) {
        legacy$responsiblePlayer = player;
    }
}

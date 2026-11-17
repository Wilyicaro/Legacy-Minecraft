package wily.legacy.mixin.base;

import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.entity.PlayerTrustPolicy;
import wily.legacy.inventory.RenameItemMenu;

import java.util.Collection;
import java.util.List;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow @Final private static Logger LOGGER;
    @Shadow public ServerPlayer player;

    @Inject(method = "handleRenameItem", at = @At("RETURN"))
    public void handleRenameItem(ServerboundRenameItemPacket packet, CallbackInfo ci) {
        if (player.containerMenu instanceof RenameItemMenu renameMenu) {
            if (!player.containerMenu.stillValid(player)) {
                LOGGER.debug("Player {} interacted with invalid menu {}", player, renameMenu);
                return;
            }
            renameMenu.setResultItemName(packet.getName());
        }
    }

    @Inject(method = "handleInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;hasClientLoaded()Z"), cancellable = true)
    private void handleInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        Entity target = player.level().getEntity(packet.entityId());
        if (target != null && !PlayerTrustPolicy.of(player).canInteractWithEntity(target, packet.usingSecondaryAction(), player.getItemInHand(packet.hand()))) ci.cancel();
    }

    @Inject(method = "handleAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;hasClientLoaded()Z"), cancellable = true)
    private void handleAttack(ServerboundAttackPacket packet, CallbackInfo ci) {
        Entity target = player.level().getEntity(packet.entityId());
        if (target != null && !legacy$canAttack(target)) ci.cancel();
    }

    @Inject(method = "handlePlayerCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;hasClientLoaded()Z"), cancellable = true)
    private void handlePlayerCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (packet.getAction() == ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY && player.getVehicle() instanceof HasCustomInventoryScreen && !PlayerTrustPolicy.of(player).canOpenContainers()) ci.cancel();
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/PiercingWeapon;attack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"), cancellable = true)
    private void handleStab(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        Collection<EntityHitResult> targets = ProjectileUtil.getHitEntitiesAlong(player, player.getAttackRangeWith(player.getMainHandItem()), target -> PiercingWeapon.canHitEntity(player, target), ClipContext.Block.COLLIDER).map(hit -> List.<EntityHitResult>of(), hits -> hits);
        if (targets.stream().map(EntityHitResult::getEntity).anyMatch(target -> !legacy$canAttack(target))) ci.cancel();
    }

    private boolean legacy$canAttack(Entity target) {
        return PlayerTrustPolicy.of(player).canDamage(target);
    }
}

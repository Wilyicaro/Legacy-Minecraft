package wily.legacy.mixin;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.network.ClientMerchantTradingPacket;
import wily.legacy.network.CommonNetwork;

import java.util.List;
import java.util.Optional;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin extends AgeableMob {
    @Shadow public abstract boolean isClientSide();

    protected AbstractVillagerMixin(EntityType<? extends AgeableMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> list) {
        super.onSyncedDataUpdated(list);
        if (isClientSide()) CommonNetwork.sendToServer(new ClientMerchantTradingPacket(getId(), Optional.empty()));
    }
    @Inject(method = "setTradingPlayer", at = @At("RETURN"))
    public void setTradingPlayer(Player player, CallbackInfo ci) {
        if (!isClientSide()) ClientMerchantTradingPacket.sync((AbstractVillager) (Object) this);
    }

}

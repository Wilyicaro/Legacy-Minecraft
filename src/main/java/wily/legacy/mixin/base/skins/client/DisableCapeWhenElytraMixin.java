package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class DisableCapeWhenElytraMixin {

    @Inject(method = "isCapeLoaded", at = @At("HEAD"), cancellable = true, require = 0)
    private void consoleskins$disableCapeWhenElytra(CallbackInfoReturnable<Boolean> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        try {
            if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) cir.setReturnValue(false);
        } catch (Throwable ignored) {
        }
    }
}

package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.LegacyMinecraftClient;
import wily.legacy.client.screen.CreativeModeScreen;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handlePlayerInfoUpdate", at = @At("RETURN"))
    public void handleLogin(ClientboundPlayerInfoUpdatePacket clientboundPlayerInfoUpdatePacket, CallbackInfo ci) {
        LegacyMinecraftClient.onClientPlayerInfoChange();
    }
    @Inject(method = "handlePlayerInfoRemove", at = @At("RETURN"))
    public void handleLogin(ClientboundPlayerInfoRemovePacket clientboundPlayerInfoRemovePacket, CallbackInfo ci) {
        LegacyMinecraftClient.onClientPlayerInfoChange();
    }
    @Redirect(method = "handleContainerSetSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setCarried(Lnet/minecraft/world/item/ItemStack;)V"))
    public void handleContainerSetSlot(AbstractContainerMenu instance, ItemStack itemStack) {
        if (!(Minecraft.getInstance().screen instanceof CreativeModeScreen)) {
            instance.setCarried(itemStack);
        }
    }
    @Redirect(method = "handleContainerSetSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setItem(IILnet/minecraft/world/item/ItemStack;)V", ordinal = 0))
    public void handleContainerSetSlot(AbstractContainerMenu instance, int i, int j, ItemStack itemStack) {
        if (Minecraft.getInstance().screen instanceof CreativeModeScreen) {
            Minecraft.getInstance().player.inventoryMenu.setItem(i,j,itemStack);
        }else{
            instance.setItem(i,j,itemStack);
        }
    }
}
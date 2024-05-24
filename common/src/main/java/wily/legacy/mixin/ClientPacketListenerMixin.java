package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.CreativeModeScreen;
import wily.legacy.inventory.LegacyMerchantMenu;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {
    protected ClientPacketListenerMixin(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }
    @Redirect(method = "handleRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/MusicManager;stopPlaying()V"))
    public void handleRespawn(MusicManager instance) {

    }
    @Inject(method = "handlePlayerInfoUpdate", at = @At("RETURN"))
    public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket clientboundPlayerInfoUpdatePacket, CallbackInfo ci) {
        Legacy4JClient.onClientPlayerInfoChange();
    }
    @Inject(method = "handlePlayerInfoRemove", at = @At("RETURN"))
    public void handlePlayerInfoUpdate(ClientboundPlayerInfoRemovePacket clientboundPlayerInfoRemovePacket, CallbackInfo ci) {
        Legacy4JClient.onClientPlayerInfoChange();
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
    @Redirect(method = "handleSetEntityPassengersPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    public void handleSetEntityPassengersPacket(Gui instance, Component component, boolean bl) {

    }
    @Inject(method = "handleMerchantOffers", at = @At("RETURN"))
    public void handleMerchantOffers(ClientboundMerchantOffersPacket clientboundMerchantOffersPacket, CallbackInfo ci) {
        if (clientboundMerchantOffersPacket.getContainerId() == minecraft.player.containerMenu.containerId && minecraft.player.containerMenu instanceof LegacyMerchantMenu m) {
            m.merchant.overrideOffers(clientboundMerchantOffersPacket.getOffers());
            m.merchant.overrideXp(clientboundMerchantOffersPacket.getVillagerXp());
            m.merchantLevel = clientboundMerchantOffersPacket.getVillagerLevel();
            m.showProgressBar = clientboundMerchantOffersPacket.showProgress();
        }
    }
}
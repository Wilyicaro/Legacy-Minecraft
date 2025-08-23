package wily.legacy.mixin;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.CreativeModeScreen;
import wily.legacy.client.screen.LeaderboardsScreen;
import wily.legacy.client.screen.LegacyLoadingScreen;
import wily.legacy.inventory.LegacyMerchantMenu;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {
    @Shadow private LevelLoadStatusManager levelLoadStatusManager;

    protected ClientPacketListenerMixin(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }
    @Inject(method = "handleRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;startWaitingForNewLevel(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/client/multiplayer/ClientLevel;)V", shift = At.Shift.AFTER))
    public void handleRespawn(ClientboundRespawnPacket clientboundRespawnPacket, CallbackInfo ci) {
        if (!clientboundRespawnPacket.shouldKeep(ClientboundRespawnPacket.KEEP_ALL_DATA)){
            long createdTime = Util.getMillis();
            LegacyLoadingScreen respawningScreen = new LegacyLoadingScreen(Component.translatable("menu.respawning"),Component.empty()){
                @Override
                public void tick() {
                    if (levelLoadStatusManager.levelReady() || Util.getMillis() - createdTime >= 30000) minecraft.setScreen(null);
                }

                @Override
                public boolean isPauseScreen() {
                    return false;
                }
            };
            respawningScreen.genericLoading = true;
            minecraft.setScreen(respawningScreen);
        }
    }
    @Redirect(method = "handleRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/MusicManager;stopPlaying()V"))
    public void handleRespawn(MusicManager instance) {
        minecraft.getSoundManager().stop();
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
    @Inject(method = "handleAwardStats", at = @At("RETURN"))
    public void handleAwardStats(ClientboundAwardStatsPacket clientboundAwardStatsPacket, CallbackInfo ci) {
        if (minecraft.screen instanceof LeaderboardsScreen s) s.onStatsUpdated();
    }
}
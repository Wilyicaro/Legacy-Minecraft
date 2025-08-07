package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyMusicFader;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.CreativeModeScreen;
import wily.legacy.client.screen.LeaderboardsScreen;
import wily.legacy.client.screen.LegacyLoadingScreen;
import wily.legacy.inventory.LegacyMerchantMenu;
import wily.legacy.util.LegacyComponents;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin /*? if >1.20.2 {*/extends ClientCommonPacketListenerImpl/*?}*/ {
    //? if <=1.20.2 {
    /*@Shadow @Final
    private Minecraft minecraft;
    @Inject(method = "handleRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", shift = At.Shift.AFTER))
    public void handleRespawn(ClientboundRespawnPacket clientboundRespawnPacket, CallbackInfo ci) {
        if (clientboundRespawnPacket.shouldKeep(ClientboundRespawnPacket.KEEP_ALL_DATA)) return;
        LegacyLoadingScreen respawningScreen = LegacyLoadingScreen.getRespawningScreen(()-> false);
        minecraft.setScreen(new ReceivingLevelScreen(){
            @Override
            protected void init() {
                super.init();
                respawningScreen.init(minecraft, width, height);
            }

            @Override
            public void render(GuiGraphics guiGraphics, int i, int j, float f) {
                respawningScreen.render(guiGraphics, i, j, f);
            }
        });
    }
    *///?} else {
    @Shadow private LevelLoadStatusManager levelLoadStatusManager;
    @Shadow private ClientLevel level;

    protected ClientPacketListenerMixin(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraft, connection, commonListenerCookie);
    }

    @Inject(method = "handleRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setId(I)V"))
    public void handleRespawn(ClientboundRespawnPacket clientboundRespawnPacket, CallbackInfo ci) {
        if (!clientboundRespawnPacket.shouldKeep(ClientboundRespawnPacket.KEEP_ALL_DATA)){
            minecraft.setScreen(LegacyLoadingScreen.getRespawningScreen(levelLoadStatusManager::levelReady));
        }
    }
    //?}

    @Redirect(method = "handleRespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/MusicManager;stopPlaying()V"))
    public void handleRespawnMusic(MusicManager instance) {
        LegacyMusicFader.fadeOutBgMusic(true);
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    public void handleLoginMusic(ClientboundLoginPacket clientboundLoginPacket, CallbackInfo ci) {
        if (minecraft.level.dimension() != Level.OVERWORLD) LegacyMusicFader.fadeOutBgMusic(true);
    }

    @Inject(method = "handlePlayerInfoUpdate", at = @At("RETURN"))
    public void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket clientboundPlayerInfoUpdatePacket, CallbackInfo ci) {
        Legacy4JClient.onClientPlayerInfoChange();
    }

    @Inject(method = "handlePlayerInfoRemove", at = @At("RETURN"))
    public void handlePlayerInfoUpdate(ClientboundPlayerInfoRemovePacket clientboundPlayerInfoRemovePacket, CallbackInfo ci) {
        Legacy4JClient.onClientPlayerInfoChange();
    }

    @WrapWithCondition(method = /*? if <1.21.2 {*/"handleContainerSetSlot"/*?} else {*//*"handleSetCursorItem"*//*?}*/, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setCarried(Lnet/minecraft/world/item/ItemStack;)V"))
    public boolean handleContainerSetSlot(AbstractContainerMenu instance, ItemStack itemStack) {
        return !(minecraft.screen instanceof CreativeModeScreen);
    }

    //? if >=1.21.2 {
    /*@Inject(method = "handleContainerSetSlot", at = @At("RETURN"))
    public void handleContainerSetSlot(ClientboundContainerSetSlotPacket clientboundContainerSetSlotPacket, CallbackInfo ci) {
        if (this.minecraft.screen instanceof CreativeModeScreen) {
            minecraft.player.inventoryMenu.setRemoteSlot(clientboundContainerSetSlotPacket.getSlot(), clientboundContainerSetSlotPacket.getItem());
            minecraft.player.inventoryMenu.broadcastChanges();
        }
    }
    *///?}

    @WrapWithCondition(method = "handleContainerSetSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setItem(IILnet/minecraft/world/item/ItemStack;)V", ordinal = 0))
    public boolean handleContainerSetSlot(AbstractContainerMenu instance, int i, int j, ItemStack itemStack) {
        return !(minecraft.screen instanceof CreativeModeScreen);
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

    @Inject(method = "handleSystemChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V", shift = At.Shift.AFTER), cancellable = true)
    public void handleSystemChat(ClientboundSystemChatPacket clientboundSystemChatPacket, CallbackInfo ci) {
        if (!LegacyOptions.systemMessagesAsOverlay.get()) {
            minecraft.getChatListener().handleSystemMessage(clientboundSystemChatPacket.content(), false);
            ci.cancel();
        }
    }
}
package wily.legacy.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.client.screen.LegacyHorseInventoryScreen;
import wily.legacy.inventory.LegacyHorseMenu;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Shadow private ClientLevel level;

    @Redirect(method = "handleHorseScreenOpen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;containerMenu:Lnet/minecraft/world/inventory/AbstractContainerMenu;", opcode = Opcodes.PUTFIELD))
    public void handleHorseScreenOpen(LocalPlayer instance, AbstractContainerMenu value, ClientboundHorseScreenOpenPacket clientboundHorseScreenOpenPacket) {
      instance.containerMenu = new LegacyHorseMenu(clientboundHorseScreenOpenPacket.getContainerId(),instance.getInventory(),new SimpleContainer(clientboundHorseScreenOpenPacket.getSize()),(AbstractHorse) this.level.getEntity(clientboundHorseScreenOpenPacket.getEntityId()));
    }
    @Redirect(method = "handleHorseScreenOpen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    public void handleHorseScreenOpen(Minecraft instance, Screen screen) {
        instance.setScreen(new LegacyHorseInventoryScreen((LegacyHorseMenu)instance.player.containerMenu,instance.player.getInventory()));
    }

}

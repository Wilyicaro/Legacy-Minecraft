package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import wily.legacy.inventory.LegacyInventoryMenu;

import java.util.function.Supplier;

public class ServerOpenClientMenu implements CommonPacket{

    private final int clientMenu;

    public ServerOpenClientMenu(int clientMenu){
        this.clientMenu = clientMenu;
    }
    public ServerOpenClientMenu(FriendlyByteBuf buf){
        this(buf.readInt());
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(clientMenu);
    }

    @Override
    public void apply(Supplier<NetworkManager.PacketContext> ctx) {
        Player p = ctx.get().getPlayer();
        if (p instanceof ServerPlayer sp){
            if (clientMenu <=1) MenuRegistry.openMenu(sp,LegacyInventoryMenu.getMenuProvider(clientMenu == 1));
        }
    }
}

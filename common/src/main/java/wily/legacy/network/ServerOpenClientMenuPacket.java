package wily.legacy.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import wily.legacy.inventory.LegacyCraftingMenu;

import java.util.function.Supplier;

public record ServerOpenClientMenuPacket(BlockPos pos, int clientMenu) implements CommonNetwork.Packet {
    public ServerOpenClientMenuPacket(int clientMenu){
        this(BlockPos.ZERO,clientMenu);
    }

    public ServerOpenClientMenuPacket(FriendlyByteBuf buf){
        this(buf.readBlockPos(),buf.readInt());
    }
    @Override
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(clientMenu);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (p.get() instanceof ServerPlayer sp){
            sp.openMenu(LegacyCraftingMenu.getMenuProvider(pos,clientMenu == 1));
        }
    }
}
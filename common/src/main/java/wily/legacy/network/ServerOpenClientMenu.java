package wily.legacy.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import wily.legacy.inventory.LegacyCraftingMenu;

import java.util.function.Supplier;

public record ServerOpenClientMenu(BlockPos pos, int clientMenu) implements CommonPacket{
    public ServerOpenClientMenu(int clientMenu){
        this(BlockPos.ZERO,clientMenu);
    }

    public ServerOpenClientMenu(FriendlyByteBuf buf){
        this(buf.readBlockPos(),buf.readInt());
    }
    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(clientMenu);
    }

    @Override
    public void apply(Supplier<NetworkManager.PacketContext> ctx) {
        Player p = ctx.get().getPlayer();
        if (p instanceof ServerPlayer sp){
            MenuRegistry.openMenu(sp, clientMenu == 0 ? Blocks.CRAFTING_TABLE.getMenuProvider(sp.level().getBlockState(pos),sp.level(),pos) : LegacyCraftingMenu.getMenuProvider(pos,clientMenu == 2));
        }
    }
}
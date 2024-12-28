package wily.legacy.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.inventory.LegacyCraftingMenu;

import java.util.function.Supplier;

public record ServerOpenClientMenuPayload(BlockPos pos, int clientMenu) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<ServerOpenClientMenuPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("server_open_client_menu_payload"), ServerOpenClientMenuPayload::new);

    public ServerOpenClientMenuPayload(int clientMenu){
        this(BlockPos.ZERO,clientMenu);
    }

    public ServerOpenClientMenuPayload(CommonNetwork.PlayBuf buf){
        this(buf.get().readBlockPos(),buf.get().readInt());
    }
    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeBlockPos(pos);
        buf.get().writeInt(clientMenu);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (p.get() instanceof ServerPlayer sp){
            sp.openMenu(LegacyCraftingMenu.getMenuProvider(pos,clientMenu == 1));
        }
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }
}
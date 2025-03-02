package wily.legacy.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.inventory.LegacyCraftingMenu;

import java.util.Optional;
import java.util.function.Supplier;

public record ServerOpenClientMenuPayload(ClientMenu menu) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<ServerOpenClientMenuPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("server_open_client_menu_payload"), ServerOpenClientMenuPayload::new);

    public enum ClientMenu {
        PLAYER_CRAFTING(()->LegacyCraftingMenu.getMenuProvider(BlockPos.ZERO, true));

        public final Supplier<MenuProvider> menuProviderSupplier;

        ClientMenu(Supplier<MenuProvider> menuProviderSupplier){
            this.menuProviderSupplier = menuProviderSupplier;
        }
    }

    public static ServerOpenClientMenuPayload playerCrafting(){
        return new ServerOpenClientMenuPayload(ClientMenu.PLAYER_CRAFTING);
    }

    public ServerOpenClientMenuPayload(CommonNetwork.PlayBuf buf){
        this(buf.get().readEnum(ClientMenu.class));
    }
    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeEnum(menu);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (p.get() instanceof ServerPlayer sp) {
            sp.openMenu(menu.menuProviderSupplier.get());
        }
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }
}
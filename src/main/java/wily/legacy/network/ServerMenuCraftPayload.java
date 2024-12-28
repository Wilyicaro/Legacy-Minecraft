package wily.legacy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import wily.factoryapi.base.FactoryIngredient;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.Legacy4J;
import wily.legacy.client.RecipeInfo;
import wily.legacy.inventory.RecipeMenu;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record ServerMenuCraftPayload(Optional<ResourceLocation> craftId, List<Optional<Ingredient>> customIngredients, int button, boolean max) implements CommonNetwork.Payload {
    public static final CommonNetwork.Identifier<ServerMenuCraftPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("server_menu_craft"), ServerMenuCraftPayload::new);

    public ServerMenuCraftPayload(CommonNetwork.PlayBuf buf){
        this(buf.get().readOptional(FriendlyByteBuf::readResourceLocation), buf.get().readList(b-> buf.get().readOptional(b1->FactoryIngredient.decode(buf).toIngredient())),buf.get().readVarInt(), buf.get().readBoolean());
    }

    public ServerMenuCraftPayload(List<Optional<Ingredient>> ingredients, int button, boolean max){
        this(Optional.empty(),ingredients,button, max);
    }
    public ServerMenuCraftPayload(RecipeInfo<?> rcp, int button, boolean max){
        this(Optional.of(rcp.getId()),rcp.isOverride() ? rcp.getOptionalIngredients() : Collections.emptyList(),button, max);
    }
    public ServerMenuCraftPayload(RecipeInfo<?> rcp, boolean max){
        this(rcp,-1,max);
    }

    @Override
    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeOptional(craftId,FriendlyByteBuf::writeResourceLocation);
        buf.get().writeCollection(customIngredients,(r, o)->r.writeOptional(o, (b,i)-> FactoryIngredient.encode(buf, FactoryIngredient.of(i))));
        buf.get().writeVarInt(button);
        buf.get().writeBoolean(max);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (p.get() instanceof ServerPlayer sp && sp.containerMenu instanceof RecipeMenu m) executor.execute(()-> m.tryCraft(sp,this));
    }

    @Override
    public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
        return ID;
    }
}

package wily.legacy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import wily.legacy.inventory.LegacyIngredient;
import wily.legacy.inventory.RecipeMenu;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public record ServerMenuCraftPacket(ResourceLocation craftId, List<Ingredient> customIngredients, int button, boolean max) implements CommonNetwork.Packet {
    public ServerMenuCraftPacket(FriendlyByteBuf buf){
        this(buf.readResourceLocation(), buf.readList(i-> LegacyIngredient.decode(buf).toIngredient()),buf.readVarInt(), buf.readBoolean());
    }
    public static final ResourceLocation EMPTY = new ResourceLocation("empty");
    public ServerMenuCraftPacket(List<Ingredient> ingredients, int button, boolean max){
        this(EMPTY,ingredients,button, max);
    }
    public ServerMenuCraftPacket(RecipeHolder<?> rcp, int button, boolean max){
        this(rcp.id(),rcp.value() instanceof CustomRecipe ? rcp.value().getIngredients() : Collections.emptyList(),button, max);
    }
    public ServerMenuCraftPacket(RecipeHolder<?> rcp, boolean max){
        this(rcp,-1,max);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(craftId);
        buf.writeCollection(customIngredients,(b, i)->LegacyIngredient.encode(b,LegacyIngredient.of(i)));
        buf.writeVarInt(button);
        buf.writeBoolean(max);
    }

    @Override
    public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> p) {
        if (p.get() instanceof ServerPlayer sp && sp.containerMenu instanceof RecipeMenu m) executor.execute(()-> m.tryCraft(sp,this));
    }
}

package wily.legacy.network;

//? if >=1.21.2 {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
//?} else {
/*import wily.legacy.mixin.base.RecipeManagerAccessor;
*///?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.network.CommonNetwork;
//? if >1.20.1 {
import net.minecraft.world.item.crafting.RecipeHolder;
 //?}
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;

import java.util.*;
import java.util.function.Supplier;

public class CommonRecipeManager {

    public static <R extends Recipe<?>> /*? if >1.20.1 {*/RecipeHolder<R>/*?} else {*//*R*//*?}*/ byId(ResourceLocation id, RecipeType<R> type) {
        return (/*? if >1.20.1 {*/RecipeHolder<R>/*?} else {*//*R*//*?}*/)/*? if <1.21.2 {*//*getRecipeManager().byKey(id).orElse(null)*//*?} else {*/recipesByType.get(type).get(id)/*?}*/;
    }

    public static <R extends Recipe<?>> Collection</*? if >1.20.1 {*/RecipeHolder<R>/*?} else {*//*R*//*?}*/> byType(RecipeType<R> type) {
        return /*? if <1.21.2 {*/ /*((RecipeManagerAccessor)getRecipeManager()).getRecipeByType(type)/^? if <1.20.5 {^//^.values()^//^?}^/*//*?} else {*/(Collection) recipesByType.get(type).values()/*?}*/;
    }

    public static <R extends Recipe<I>, I extends /*? if <1.20.5 {*//*Container*//*?} else {*/RecipeInput/*?}*/> Optional</*? if >1.20.1 {*/RecipeHolder<R>/*?} else {*//*R*//*?}*/> getRecipeFor(RecipeType<R> type, I input, Level level) {
        Collection</*? if >1.20.1 {*/RecipeHolder<R>/*?} else {*//*R*//*?}*/> recipes = byType(type);
        for (/*? if >1.20.1 {*/RecipeHolder<R>/*?} else {*//*R*//*?}*/ recipe : recipes) {
            if (recipe/*? if >1.20.1 {*/.value()/*?}*/.matches(input,level)) return Optional.of(recipe);
        }
        return Optional.empty();
    }
    public static <R extends Recipe<I>, I extends /*? if <1.20.5 {*//*Container*//*?} else {*/RecipeInput/*?}*/> Optional<ItemStack> getResultFor(RecipeType<R> type, I input, Level level) {
        return getRecipeFor(type,input,level).map(r->r/*? if >1.20.1 {*/.value()/*?}*/.assemble(input,level.registryAccess()));
    }

    //? if <1.21.2 {
    /*public static RecipeManager getRecipeManager(){
        return FactoryAPIPlatform.isClient() ? Legacy4JClient.getRecipeManager() : Legacy4J.currentServer.getRecipeManager();
    }
    *///?}

    //? if >=1.21.2 {
    public static Map<RecipeType<?>,Map<ResourceLocation,RecipeHolder<?>>> recipesByType = Collections.emptyMap();


    public record ClientPayload(Map<RecipeType<?>,Map<ResourceLocation,RecipeHolder<?>>> syncRecipeTypes) implements CommonNetwork.Payload {
        public static final CommonNetwork.Identifier<ClientPayload> ID = CommonNetwork.Identifier.create(Legacy4J.createModLocation("send_client_recipes"), ClientPayload::new);

        public ClientPayload(CommonNetwork.PlayBuf buf){
            this(buf.get().readMap(b->b.readById(i-> BuiltInRegistries.RECIPE_TYPE./*? if <1.21.2 {*//*getHolder*//*?} else {*/get/*?}*/(i).get().value()), b->b.readMap(FriendlyByteBuf::readResourceLocation, b1->RecipeHolder.STREAM_CODEC.decode(buf.get()))));
        }

        @Override
        public void apply(CommonNetwork.SecureExecutor executor, Supplier<Player> player) {
            recipesByType = syncRecipeTypes;
        }

        @Override
        public CommonNetwork.Identifier<? extends CommonNetwork.Payload> identifier() {
            return ID;
        }

        @Override
        public void encode(CommonNetwork.PlayBuf buf) {
            buf.get().writeMap(syncRecipeTypes, (b,t)-> b.writeById(BuiltInRegistries.RECIPE_TYPE::getId,t),(b,t)-> b.writeMap(t,FriendlyByteBuf::writeResourceLocation,(b1,h)->RecipeHolder.STREAM_CODEC.encode(buf.get(),h)));
        }
    }
    //?}
}

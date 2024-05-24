package wily.legacy.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.init.LegacyGameRules;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(MapItemSavedData.class)
public abstract class MapItemSavedDataMixin {

    @Shadow @Final private List<MapItemSavedData.HoldingPlayer> carriedBy;


    @Shadow protected abstract void addDecoration(Holder<MapDecorationType> arg, LevelAccessor arg2, String string, double d, double e, double f, Component arg3);

    public MapItemSavedData self(){
        return (MapItemSavedData) (Object) this;
    }

    @Redirect(method = "tickCarriedBy", at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z", ordinal = 0))
    public boolean tickCarriedBy(Map<Player, MapItemSavedData.HoldingPlayer> map, Object b, Player player, ItemStack itemStack) {
        if (!player.getServer().getGameRules().getBoolean(LegacyGameRules.GLOBAL_MAP_PLAYER_ICON)) return !map.containsKey(player);
        if (player instanceof ServerPlayer sp && sp.getServer() != null){
            sp.getServer().getPlayerList().getPlayers().forEach(p-> {
                if (!map.containsKey(p)){
                    MapItemSavedData.HoldingPlayer hp = self().new HoldingPlayer(p);
                    carriedBy.add(hp);
                    map.put(p,hp);
                }
            });
        }
        return true;
    }
    @Redirect(method = "tickCarriedBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;contains(Ljava/util/function/Predicate;)Z"))
    public boolean tickCarriedBy(Inventory instance, Predicate<ItemStack> predicate) {
        return instance.player.getServer().getGameRules().getBoolean(LegacyGameRules.GLOBAL_MAP_PLAYER_ICON) || instance.contains(predicate);
    }
    @Redirect(method = "tickCarriedBy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;addDecoration(Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/LevelAccessor;Ljava/lang/String;DDDLnet/minecraft/network/chat/Component;)V", ordinal = 0))
    public void tickCarriedBy(MapItemSavedData instance, Holder<MapDecorationType> i, LevelAccessor g, String h, double b, double c, double j, Component k) {
        addDecoration(i,g,h,b,c,j,Component.literal(h));
    }
}

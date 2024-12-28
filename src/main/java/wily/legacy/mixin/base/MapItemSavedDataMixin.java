package wily.legacy.mixin.base;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
//? if >=1.20.5 {
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
//?}
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.Legacy4J;
import wily.legacy.init.LegacyGameRules;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(MapItemSavedData.class)
public abstract class MapItemSavedDataMixin {

    @Shadow @Final private List<MapItemSavedData.HoldingPlayer> carriedBy;


    @Shadow protected abstract void addDecoration(/*? if <1.20.5 {*//*MapDecoration.Type*//*?} else {*/Holder<MapDecorationType>/*?}*/ arg, LevelAccessor arg2, String string, double d, double e, double f, Component arg3);

    @Shadow @Final public int centerX;

    @Shadow @Final public int centerZ;

    @Shadow @Final public byte scale;

    @Shadow @Final private boolean trackingPosition;

    @Shadow @Final private boolean unlimitedTracking;

    @Shadow @Final public ResourceKey<Level> dimension;

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
    @Redirect(method = "tickCarriedBy", at = @At(value = "INVOKE", target = /*? if <1.20.5 {*//*"Lnet/minecraft/world/entity/player/Inventory;contains(Lnet/minecraft/world/item/ItemStack;)Z"*//*?} else {*/"Lnet/minecraft/world/entity/player/Inventory;contains(Ljava/util/function/Predicate;)Z"/*?}*/))
    public boolean tickCarriedBy(Inventory instance, /*? if <1.20.5 {*//*ItemStack*//*?} else {*/Predicate<ItemStack>/*?}*/ predicate) {
        return instance.player.getServer().getGameRules().getBoolean(LegacyGameRules.GLOBAL_MAP_PLAYER_ICON) || instance.contains(predicate);
    }
    @Redirect(method = "tickCarriedBy", at = @At(value = "INVOKE", target = /*? if <1.20.5 {*//*"Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;addDecoration(Lnet/minecraft/world/level/saveddata/maps/MapDecoration$Type;Lnet/minecraft/world/level/LevelAccessor;Ljava/lang/String;DDDLnet/minecraft/network/chat/Component;)V"*//*?} else {*/"Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;addDecoration(Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/LevelAccessor;Ljava/lang/String;DDDLnet/minecraft/network/chat/Component;)V"/*?}*/))
    public void tickCarriedBy(MapItemSavedData instance, /*? if <1.20.5 {*//*MapDecoration.Type*//*?} else {*/Holder<MapDecorationType>/*?}*/ i, LevelAccessor g, String h, double b, double c, double j, Component k) {
        addDecoration(i,g,h,b,c,j,Component.literal(h));
    }
    @Inject(method = "createFresh", at = @At("HEAD"), cancellable = true)
    private static void createFresh(double d, double e, byte b, boolean bl, boolean bl2, ResourceKey<Level> resourceKey, CallbackInfoReturnable<MapItemSavedData> cir) {
        if (Legacy4J.currentServer != null && !Legacy4J.currentServer.getGameRules().getBoolean(LegacyGameRules.LEGACY_MAP_GRID)) return;
        int i = 128 * (1 << b);
        cir.setReturnValue(new MapItemSavedData((((int)d + (i / 2) * Mth.sign(d)) / i) * i, (((int)e + (i / 2) * Mth.sign(e)) / i) * i, b, bl, bl2, false, resourceKey));
    }
    @Inject(method = "scaled", at = @At("HEAD"), cancellable = true)
    public void scaled(CallbackInfoReturnable<MapItemSavedData> cir) {
        if (Legacy4J.currentServer != null && !Legacy4J.currentServer.getGameRules().getBoolean(LegacyGameRules.LEGACY_MAP_GRID)) return;
        int i = 128 * (1 << (scale));
        cir.setReturnValue(MapItemSavedData.createFresh(this.centerX - (i / 2) * Mth.sign(this.centerX), this.centerZ - (i / 2) * Mth.sign(this.centerZ), (byte)Mth.clamp(this.scale + 1, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension));
    }
}

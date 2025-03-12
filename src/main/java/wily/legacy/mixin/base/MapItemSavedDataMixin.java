package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
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
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
//?}
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;
import wily.legacy.config.LegacyWorldOptions;
import wily.legacy.init.LegacyGameRules;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(MapItemSavedData.class)
public abstract class MapItemSavedDataMixin {

    @Shadow @Final private List<MapItemSavedData.HoldingPlayer> carriedBy;

    @Shadow @Final public int centerX;

    @Shadow @Final public int centerZ;

    @Shadow @Final public byte scale;

    @Shadow @Final private boolean trackingPosition;

    @Shadow @Final private boolean unlimitedTracking;

    @Shadow @Final public ResourceKey<Level> dimension;

    @Shadow @Final private Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers;

    @Shadow protected abstract void addDecoration(/*? if <1.20.5 {*//*MapDecoration.Type*//*?} else {*/Holder<MapDecorationType>/*?}*/ arg, LevelAccessor arg2, String string, double d, double e, double f, Component arg3);

    @Shadow protected abstract void removeDecoration(String string);public MapItemSavedData self(){
        return (MapItemSavedData) (Object) this;
    }

    @ModifyExpressionValue(method = "tickCarriedBy", at = @At(value = "INVOKE", target = "Ljava/util/Map;containsKey(Ljava/lang/Object;)Z", ordinal = 0))
    public boolean tickCarriedByAddGlobalPlayers(boolean original, Player player) {
        if (!player.getServer().getGameRules().getBoolean(LegacyGameRules.GLOBAL_MAP_PLAYER_ICON)) return original;
        if (player instanceof ServerPlayer sp && sp.getServer() != null){
            sp.getServer().getPlayerList().getPlayers().forEach(p-> {
                if (!carriedByPlayers.containsKey(p)){
                    MapItemSavedData.HoldingPlayer hp = self().new HoldingPlayer(p);
                    carriedBy.add(hp);
                    carriedByPlayers.put(p,hp);
                }
                if (p.level().dimension() != dimension){
                    removeDecoration(sp.getGameProfile().getName());
                }
            });
        }
        return true;
    }
    @ModifyExpressionValue(method = "tickCarriedBy", at = @At(value = "INVOKE", target = /*? if <1.20.5 {*//*"Lnet/minecraft/world/entity/player/Inventory;contains(Lnet/minecraft/world/item/ItemStack;)Z"*//*?} else {*/"Lnet/minecraft/world/entity/player/Inventory;contains(Ljava/util/function/Predicate;)Z"/*?}*/))
    public boolean tickCarriedByRemoveInvalid(boolean original, Player player) {
        return player.getServer().getGameRules().getBoolean(LegacyGameRules.GLOBAL_MAP_PLAYER_ICON) || original;
    }
    @ModifyArg(method = "tickCarriedBy", at = @At(value = "INVOKE", target = /*? if <1.20.5 {*//*"Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;addDecoration(Lnet/minecraft/world/level/saveddata/maps/MapDecoration$Type;Lnet/minecraft/world/level/LevelAccessor;Ljava/lang/String;DDDLnet/minecraft/network/chat/Component;)V"*//*?} else {*/"Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;addDecoration(Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/LevelAccessor;Ljava/lang/String;DDDLnet/minecraft/network/chat/Component;)V"/*?}*/, ordinal = 0))
    public Component tickCarriedBy(Component original, @Local MapItemSavedData.HoldingPlayer player) {
        return player.player.getName();
    }

    @Inject(method = "tickCarriedBy", at = @At("RETURN"), cancellable = true)
    private void tickCarriedBy(Player player, ItemStack itemStack, CallbackInfo ci) {
        var iterator = LegacyWorldOptions.usedEndPortalPositions.get().iterator();
        boolean modified = false;
        while (iterator.hasNext()){
            var next = iterator.next();
            if (next.isValid(player.getServer()) && dimension == Level.OVERWORLD){
                addDecoration(/*? if <1.20.5 {*//*MapDecoration.Type*//*?} else {*/MapDecorationTypes/*?}*/.TARGET_X, player.getServer().overworld(), next.identifier(), next.pos().getX(), next.pos().getZ(), 0, null);
            } else {
                removeDecoration(next.identifier());
                iterator.remove();
                modified = true;
            }
        }
        if (modified) LegacyWorldOptions.usedEndPortalPositions.save();
    }

    @Inject(method = "createFresh", at = @At("HEAD"), cancellable = true)
    private static void createFresh(double d, double e, byte b, boolean bl, boolean bl2, ResourceKey<Level> resourceKey, CallbackInfoReturnable<MapItemSavedData> cir) {
        if (FactoryAPI.currentServer != null && !FactoryAPI.currentServer.getGameRules().getBoolean(LegacyGameRules.LEGACY_MAP_GRID)) return;
        int i = 128 * (1 << b);
        cir.setReturnValue(new MapItemSavedData((((int)d + (i / 2) * Mth.sign(d)) / i) * i, (((int)e + (i / 2) * Mth.sign(e)) / i) * i, b, bl, bl2, false, resourceKey));
    }
    @Inject(method = "scaled", at = @At("HEAD"), cancellable = true)
    public void scaled(CallbackInfoReturnable<MapItemSavedData> cir) {
        if (FactoryAPI.currentServer != null && !FactoryAPI.currentServer.getGameRules().getBoolean(LegacyGameRules.LEGACY_MAP_GRID)) return;
        int i = 128 * (1 << (scale));
        cir.setReturnValue(MapItemSavedData.createFresh(this.centerX - (i / 2) * Mth.sign(this.centerX), this.centerZ - (i / 2) * Mth.sign(this.centerZ), (byte)Mth.clamp(this.scale + 1, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension));
    }
}

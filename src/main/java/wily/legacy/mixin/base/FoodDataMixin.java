package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.entity.LegacyPlayerInfo;

@Mixin(FoodData.class)
public class FoodDataMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = /*? if <1.21.2 {*/"Lnet/minecraft/world/entity/player/Player;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"/*?} else {*//*"Lnet/minecraft/server/level/ServerPlayer;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"*//*?}*/))
    public boolean tick(/*? if <1.21.2 {*/Player player, DamageSource source, float amount/*?} else {*//*ServerPlayer player, ServerLevel level, DamageSource source, float amount*//*?}*/) {
        if (player instanceof LegacyPlayerInfo p && p.isExhaustionDisabled()) return false;
        return /*? if <1.21.2 {*/player.hurt(source, amount)/*?} else {*//*player.hurtServer(level, source, amount)*//*?}*/;
    }
}

package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = /*? if <1.21.2 {*//*"Lnet/minecraft/world/level/Level;getDifficulty()Lnet/minecraft/world/Difficulty;"*//*?} else {*/"Lnet/minecraft/server/level/ServerLevel;getDifficulty()Lnet/minecraft/world/Difficulty;"/*?}*/))
    public Difficulty tick(/*? if <1.21.2 {*//*Level instance, Player player*//*?} else {*/ServerLevel instance, ServerPlayer player/*?}*/) {
        return player instanceof LegacyPlayerInfo p && p.isExhaustionDisabled() ? Difficulty.PEACEFUL : instance.getDifficulty();
    }
}

package wily.legacy.mixin.base;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.globalleaderboards.GlobalDifficultyStatsStore;

@Mixin(StatsCounter.class)
public abstract class StatsCounterMixin {
   @Unique
   private Stat<?> legacy$stat;
   @Unique
   private int legacy$value;

   @Shadow
   public abstract int getValue(Stat<?> stat);

   @Inject(method = "increment", at = @At("HEAD"))
   private void legacy$captureStat(Player player, Stat<?> stat, int count, CallbackInfo ci) {
      this.legacy$stat = stat;
      this.legacy$value = this.getValue(stat);
   }

   @Inject(method = "increment", at = @At("RETURN"))
   private void legacy$recordStat(Player player, Stat<?> stat, int count, CallbackInfo ci) {
      if (player instanceof ServerPlayer serverPlayer && this.legacy$stat == stat) {
         GlobalDifficultyStatsStore.award(serverPlayer, stat, this.getValue(stat) - this.legacy$value);
      }
      this.legacy$stat = null;
      this.legacy$value = 0;
   }
}

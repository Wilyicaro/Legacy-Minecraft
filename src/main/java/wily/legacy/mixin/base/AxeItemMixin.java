package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wily.legacy.util.LegacyItemUtil;

@Mixin(AxeItem.class)
public class AxeItemMixin {
    @ModifyReturnValue(method = "playerHasBlockingItemUseIntent", at = @At("RETURN"))
    private static boolean playerHasBlockingItemUseIntent(boolean original, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        return original && !LegacyItemUtil.isLegacyShield(player, player.getOffhandItem());
    }
}

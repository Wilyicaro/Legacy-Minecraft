package wily.legacy.mixin.base;

import wily.legacy.entity.LegacyMobCaps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandItem.class)
public abstract class ArmorStandItemMixin {

    private static final String ARMOR_STANDS_CAP_REACHED =
            "The maximum number of Armor Stands in a world has been reached.";

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void mobspawning$capArmorStands(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel level)) return;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        if (!LegacyMobCaps.isCapped(level, player, EntityType.ARMOR_STAND)) return;

        player.displayClientMessage(Component.literal(ARMOR_STANDS_CAP_REACHED), false);
        cir.setReturnValue(InteractionResult.FAIL);
        cir.cancel();
    }
}

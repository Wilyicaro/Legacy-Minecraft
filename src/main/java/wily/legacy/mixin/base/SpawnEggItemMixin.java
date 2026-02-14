package wily.legacy.mixin.base;

import wily.legacy.entity.LegacyMobCaps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnEggItem.class)
public abstract class SpawnEggItemMixin {

    private static final String KEY_ANIMALS_CAP_REACHED   = "legacy.spawn_egg.cap_reached.animals";
    private static final String KEY_ENEMIES_CAP_REACHED   = "legacy.spawn_egg.cap_reached.enemies";
    private static final String KEY_MOOSHROOMS_CAP_REACHED= "legacy.spawn_egg.cap_reached.mooshrooms";
    private static final String KEY_CHICKENS_CAP_REACHED  = "legacy.spawn_egg.cap_reached.chickens";
    private static final String KEY_WOLVES_CAP_REACHED    = "legacy.spawn_egg.cap_reached.wolves";
    private static final String KEY_BATS_CAP_REACHED      = "legacy.spawn_egg.cap_reached.bats";
    private static final String KEY_FISH_SQUID_CAP_REACHED= "legacy.spawn_egg.cap_reached.fish_squid";
    private static final String KEY_GENERIC_CAP_REACHED   = "legacy.spawn_egg.cap_reached.generic";

    @Shadow
    public abstract EntityType<?> getType(ItemStack stack);

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void mobspawning$capSpawnEggs(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(context.getPlayer() instanceof ServerPlayer player)) return;

        EntityType<?> type = this.getType(context.getItemInHand());
        if (!LegacyMobCaps.isCapped(serverLevel, player, type)) return;

        LegacyMobCaps.LegacyBucket bucket = LegacyMobCaps.bucketFor(type);

        String key = switch (bucket) {
            case PASSIVE -> KEY_ANIMALS_CAP_REACHED;
            case HOSTILE -> KEY_ENEMIES_CAP_REACHED;
            case MOOSHROOM -> KEY_MOOSHROOMS_CAP_REACHED;
            case CHICKEN -> KEY_CHICKENS_CAP_REACHED;
            case WOLF -> KEY_WOLVES_CAP_REACHED;
            case BAT -> KEY_BATS_CAP_REACHED;
            case FISH, SQUID -> KEY_FISH_SQUID_CAP_REACHED;
            default -> KEY_GENERIC_CAP_REACHED;
        };

        player.displayClientMessage(Component.translatable(key), false);
        cir.setReturnValue(InteractionResult.FAIL);
        cir.cancel();
    }
}

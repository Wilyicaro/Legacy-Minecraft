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

    private static final String ANIMALS_CAP_REACHED =
            "Can't use Spawn Egg at the moment. The maximum number of animals, excluding Wolves, Bats, Chickens and Mooshrooms, has been reached.";

    private static final String ENEMIES_CAP_REACHED =
            "Can't use Spawn Egg at the moment. The maximum number of enemies in a world has been reached.";

    private static final String MOOSHROOMS_CAP_REACHED =
            "Can't use Spawn Egg at the moment. The maximum number of Mooshrooms has been reached.";

    private static final String CHICKENS_CAP_REACHED =
            "Can't use Spawn Egg at the moment. The maximum number of Chickens in a world has been reached.";

    private static final String WOLVES_CAP_REACHED =
            "Can't use Spawn Egg at the moment. The maximum number of Wolves in a world has been reached.";

    private static final String BATS_CAP_REACHED =
            "Can't use Spawn Egg at the moment. The maximum number of Bats in a world has been reached.";

    private static final String FISH_SQUID_CAP_REACHED =
            "The maximum number of Fish and Squid in a world has been reached.";

    private static final String GENERIC_CAP_REACHED =
            "Can't use Spawn Egg at the moment. The maximum number of mobs in a world has been reached.";

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
        String msg = switch (bucket) {
            case PASSIVE -> ANIMALS_CAP_REACHED;
            case HOSTILE -> ENEMIES_CAP_REACHED;
            case MOOSHROOM -> MOOSHROOMS_CAP_REACHED;
            case CHICKEN -> CHICKENS_CAP_REACHED;
            case WOLF -> WOLVES_CAP_REACHED;
            case BAT -> BATS_CAP_REACHED;
            case FISH, SQUID -> FISH_SQUID_CAP_REACHED;
            default -> GENERIC_CAP_REACHED;
        };

        player.displayClientMessage(Component.literal(msg), false);
        cir.setReturnValue(InteractionResult.FAIL);
        cir.cancel();
    }
}

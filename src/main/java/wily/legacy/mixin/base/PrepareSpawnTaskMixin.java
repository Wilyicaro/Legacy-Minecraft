package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.FactoryAPIPlatform;
import wily.legacy.config.LegacyWorldOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(targets = {"net.minecraft.server.network.config.PrepareSpawnTask$Ready"})
public class PrepareSpawnTaskMixin {

    @Inject(method = "spawn", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER, ordinal = 0))
    private void spawn(Connection connection, CommonListenerCookie commonListenerCookie, CallbackInfoReturnable<ServerPlayer> cir, @Local Optional<CompoundTag> playerTag, @Local ServerPlayer serverPlayer) {
        if (playerTag.isEmpty()) {
            List<ItemStack> itemsToAdd = new ArrayList<>(LegacyWorldOptions.initialItems.get().stream().filter(i -> i.isEnabled(FactoryAPIPlatform.getEntityServer(serverPlayer))).map(LegacyWorldOptions.InitialItem::item).toList());

            for (int j = 0; j < 27; j++) {
                if (itemsToAdd.isEmpty()) break;
                for (int i = 0; i < itemsToAdd.size(); i++) {
                    if (serverPlayer.getInventory().add(9 + j, itemsToAdd.get(i).copy())) {
                        itemsToAdd.remove(i);
                        break;
                    }
                }
            }
        }

    }
}

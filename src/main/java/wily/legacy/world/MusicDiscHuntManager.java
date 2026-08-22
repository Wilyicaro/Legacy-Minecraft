package wily.legacy.world;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import wily.factoryapi.base.network.CommonNetwork;
import wily.legacy.config.LegacyWorldOptions;
import wily.legacy.entity.LegacyPlayer;
import wily.legacy.network.TipCommand;
import wily.legacy.util.LegacyTipBuilder;

import java.util.Set;

public final class MusicDiscHuntManager {
    private MusicDiscHuntManager() {
    }

    public static boolean isTracked(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (MusicDiscHunt hunt : LegacyWorldOptions.musicDiscHunts.get()) {
            for (MusicDiscHunt.Disc disc : hunt.discs()) {
                if (disc.matches(stack)) return true;
            }
        }
        return false;
    }

    public static void collect(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer) || stack.isEmpty()) return;
        Set<String> progress = ((LegacyPlayer) player).getMusicDiscHuntProgress();
        for (MusicDiscHunt hunt : LegacyWorldOptions.musicDiscHunts.get()) {
            for (MusicDiscHunt.Disc disc : hunt.discs()) {
                if (!disc.matches(stack)) continue;
                String key = hunt.progressKey(disc);
                if (progress.add(key)) sendProgressTip(serverPlayer, hunt, stack, hunt.foundCount(progress));
                return;
            }
        }
    }

    private static void sendProgressTip(ServerPlayer player, MusicDiscHunt hunt, ItemStack disc, int found) {
        LegacyTipBuilder tip = new LegacyTipBuilder()
            .title(CommonComponents.EMPTY)
            .tip(Component.translatable(hunt.message(), found, hunt.discs().size()))
            .itemIcon(disc.copyWithCount(1))
            .disappearTime(hunt.tipTime());
        CommonNetwork.sendToPlayer(player, new TipCommand.PersistentPayload(tip));
    }
}

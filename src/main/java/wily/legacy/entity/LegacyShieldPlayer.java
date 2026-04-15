package wily.legacy.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public interface LegacyShieldPlayer {
    int SHIELD_PAUSE_TICKS = 6;

    static boolean hasConflictingUse(Player player, InteractionHand shieldHand) {
        return player.isUsingItem() && player.getUsedItemHand() != shieldHand;
    }

    void pauseShield(int ticks);

    boolean isShieldPaused();
}

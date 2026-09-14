package wily.legacy.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import wily.legacy.init.LegacyGameRules;

public final class LegacyShieldControls {
    private LocalPlayer player;
    private InteractionHand hand;

    public void tick(Minecraft minecraft) {
        if (player != minecraft.player) {
            player = minecraft.player;
            hand = null;
        }
        if (player == null || minecraft.gameMode == null) {
            hand = null;
            return;
        }
        if (!isUsingShield(player)) hand = null;

        InteractionHand next = getShieldHand(minecraft);
        if (hand != null && hand != next) {
            minecraft.gameMode.releaseUsingItem(player);
            hand = null;
        }
        if (next == null || player.isUsingItem() || minecraft.gameMode.isDestroying()
                || player.getCooldowns().isOnCooldown(player.getItemInHand(next))) return;

        minecraft.gameMode.useItem(player, next);
        if (player.isUsingItem() && player.getUsedItemHand() == next && player.getUseItem().getItem() instanceof ShieldItem) {
            hand = next;
        }
    }

    public boolean isUsingShield(Player player) {
        return player != null && this.player == player && hand != null && player.isUsingItem()
                && player.getUsedItemHand() == hand && player.getUseItem().getItem() instanceof ShieldItem;
    }

    public void reset() {
        player = null;
        hand = null;
    }

    private InteractionHand getShieldHand(Minecraft minecraft) {
        if (minecraft.screen != null || !player.isAlive() || player.isSleeping() || player.isSpectator()
                || !LegacyGameRules.getSidedBooleanGamerule(player, LegacyGameRules.LEGACY_SHIELD_CONTROLS)) return null;
        if (!minecraft.options.keyShift.isDown() && !player.isPassenger()) return null;
        if (isPressed(minecraft.options.keyAttack) || isPressed(minecraft.options.keyUse) || isPressed(minecraft.options.keyPickItem)) return null;
        if (player.getOffhandItem().getItem() instanceof ShieldItem) return InteractionHand.OFF_HAND;
        return player.getMainHandItem().getItem() instanceof ShieldItem ? InteractionHand.MAIN_HAND : null;
    }

    private static boolean isPressed(KeyMapping key) {
        return key.isDown() || key.clickCount > 0;
    }
}

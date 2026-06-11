package wily.legacy.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;

public interface PlayerYBobbing {
    static PlayerYBobbing of(ClientAvatarState avatarState) {
        return (PlayerYBobbing) avatarState;
    }

    static float getAngle(Minecraft minecraft, float partialTicks) {
        return minecraft.gameRenderer != null && minecraft.player != null && minecraft.getCameraEntity() instanceof ClientAvatarEntity player && (!minecraft.player.getAbilities().flying || isLegacyElytraBoostBobbing(minecraft.player)) ? of(player.avatarState()).getAngle(partialTicks) : 0;
    }

    static boolean isLegacyElytraBoostBobbing(Player player) {
        return player instanceof LegacyLocalPlayer legacyPlayer && legacyPlayer.isLegacyElytraBoostBobbing();
    }

    static double getYBobMovement(Player player, boolean elytraBoostBobbing) {
        if (!elytraBoostBobbing) return player.getDeltaMovement().y;
        return player instanceof LegacyLocalPlayer legacyPlayer ? legacyPlayer.getLegacyElytraBoostYBobMovement() : player.getDeltaMovement().y;
    }

    float yBob();

    float oYBob();

    void setYBob(float bob);

    void setOYBob(float bob);

    default float getAngle(float partialTicks) {
        return Mth.lerp(partialTicks, oYBob(), yBob());
    }

    default void handleYBobbing(Player p) {
        setOYBob(yBob());
        boolean elytraBoostBobbing = isLegacyElytraBoostBobbing(p);
        boolean shouldBob = (!p.onGround() || elytraBoostBobbing) && !p.isDeadOrDying();
        double yMovement = getYBobMovement(p, elytraBoostBobbing);
        setYBob(yBob() + ((shouldBob ? (float) Math.atan(-yMovement * 0.2D) * 15.0F : 0) - yBob()) * 0.8F);
    }
}

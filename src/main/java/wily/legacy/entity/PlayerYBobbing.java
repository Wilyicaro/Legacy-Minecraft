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
        return minecraft.getCameraEntity() instanceof ClientAvatarEntity player && !minecraft.player.getAbilities().flying ? of(player.avatarState()).getAngle(partialTicks) : 0;
    }

    float yBob();

    float oYBob();

    void setYBob(float bob);

    void setOYBob(float bob);

    default float getAngle(float partialTicks) {
        return Mth.lerp(partialTicks, oYBob(), yBob());
    }

    default void handleYBobbing() {
        if (this instanceof Player p) {
            setOYBob(yBob());
            setYBob(yBob() + ((!p.onGround() && !p.isDeadOrDying() ? (float) Math.atan(-p.getDeltaMovement().y * 0.2D) * 15.0F : 0) - yBob()) * 0.8F);
        }
    }
}

package wily.legacy.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public interface PlayerYBobbing {
    float yBob();
    float oYBob();
    void setYBob(float bob);
    void setOYBob(float bob);
    default float getAngle(float partialTicks){
        return Mth.lerp(partialTicks,oYBob(),yBob());
    }
    default void handleYBobbing(){
        if (this instanceof Player p){
            setOYBob(yBob());
            setYBob(yBob() + ((!p.onGround() && !p.isDeadOrDying() ? (float) Math.atan(-p.getDeltaMovement().y * 0.2D) * 15.0F : 0) - yBob()) * 0.8F);
        }
    }
}

package wily.legacy.mixin;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import wily.legacy.player.LegacyPlayerInfo;

@Mixin(PlayerInfo.class)
public class PlayerInfoMixin implements LegacyPlayerInfo {
    int position = -1;
    boolean visible = true;
    boolean disableExhaustion = false;
    boolean mayFlySurvival = false;
    @Override
    public int getPosition() {
        return position;
    }
    @Override
    public void setPosition(int i) {
        position = i;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
    @Override
    public void setVisibility(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isExhaustionDisabled() {
        return disableExhaustion;
    }

    @Override
    public void setDisableExhaustion(boolean exhaustion) {
        this.disableExhaustion = exhaustion;
    }

    @Override
    public boolean mayFlySurvival() {
        return mayFlySurvival;
    }

    @Override
    public void setMayFlySurvival(boolean mayFly) {
        this.mayFlySurvival = mayFly;
    }
}

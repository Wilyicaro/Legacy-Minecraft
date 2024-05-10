package wily.legacy.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.player.LegacyPlayerInfo;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin implements LegacyPlayerInfo {
    @Shadow public abstract GameProfile getProfile();

    @Override
    public GameProfile legacyMinecraft$getProfile() {
        return getProfile();
    }

    int position = -1;
    boolean visible = true;
    boolean disableExhaustion = true;
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

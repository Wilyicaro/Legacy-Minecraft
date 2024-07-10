package wily.legacy.mixin;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.stats.Stat;
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
    boolean disableExhaustion = false;
    boolean mayFlySurvival = false;
    Object2IntMap<Stat<?>> statsMap = new Object2IntOpenHashMap<>();
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

    @Override
    public Object2IntMap<Stat<?>> getStatsMap() {
        return statsMap;
    }

    @Override
    public void setStatsMap(Object2IntMap<Stat<?>> statsMap) {
        this.statsMap = statsMap;
    }
}

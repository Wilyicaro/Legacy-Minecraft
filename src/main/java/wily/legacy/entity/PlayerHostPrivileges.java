package wily.legacy.entity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import wily.factoryapi.base.network.CommonNetwork;

public record PlayerHostPrivileges(boolean canBecomeInvisible, boolean canFly,
                                   boolean canDisableExhaustion, boolean canTeleport) {
    public static final PlayerHostPrivileges ALL = new PlayerHostPrivileges(true, true, true, true);
    public static final PlayerHostPrivileges NONE = new PlayerHostPrivileges(false, false, false, false);

    public static PlayerHostPrivileges decode(CommonNetwork.PlayBuf buf) {
        return new PlayerHostPrivileges(buf.get().readBoolean(), buf.get().readBoolean(), buf.get().readBoolean(), buf.get().readBoolean());
    }

    public static PlayerHostPrivileges load(ValueInput input) {
        return new PlayerHostPrivileges(
                input.getBooleanOr("CanBecomeInvisible", false),
                input.getBooleanOr("CanFly", false),
                input.getBooleanOr("CanDisableExhaustion", false),
                input.getBooleanOr("CanTeleport", false)
        );
    }

    public PlayerHostPrivileges withCanBecomeInvisible(boolean value) {
        return new PlayerHostPrivileges(value, canFly, canDisableExhaustion, canTeleport);
    }

    public PlayerHostPrivileges withCanFly(boolean value) {
        return new PlayerHostPrivileges(canBecomeInvisible, value, canDisableExhaustion, canTeleport);
    }

    public PlayerHostPrivileges withCanDisableExhaustion(boolean value) {
        return new PlayerHostPrivileges(canBecomeInvisible, canFly, value, canTeleport);
    }

    public PlayerHostPrivileges withCanTeleport(boolean value) {
        return new PlayerHostPrivileges(canBecomeInvisible, canFly, canDisableExhaustion, value);
    }

    public boolean hasPlayerOptions(boolean isSurvival) {
        return canBecomeInvisible || isSurvival && (canFly || canDisableExhaustion);
    }

    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeBoolean(canBecomeInvisible);
        buf.get().writeBoolean(canFly);
        buf.get().writeBoolean(canDisableExhaustion);
        buf.get().writeBoolean(canTeleport);
    }

    public void save(ValueOutput output) {
        output.putBoolean("CanBecomeInvisible", canBecomeInvisible);
        output.putBoolean("CanFly", canFly);
        output.putBoolean("CanDisableExhaustion", canDisableExhaustion);
        output.putBoolean("CanTeleport", canTeleport);
    }
}

package wily.legacy.entity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import wily.factoryapi.base.network.CommonNetwork;

public record PlayerTrustPermissions(boolean canBuildAndMine, boolean canUseDoorsAndSwitches,
                                     boolean canOpenContainers, boolean canAttackPlayers,
                                     boolean canAttackAnimals) {
    public static final PlayerTrustPermissions TRUSTED = new PlayerTrustPermissions(true, true, true, true, true);
    public static final PlayerTrustPermissions RESTRICTED = new PlayerTrustPermissions(false, false, false, false, false);

    public static PlayerTrustPermissions decode(CommonNetwork.PlayBuf buf) {
        return new PlayerTrustPermissions(buf.get().readBoolean(), buf.get().readBoolean(), buf.get().readBoolean(), buf.get().readBoolean(), buf.get().readBoolean());
    }

    public static PlayerTrustPermissions load(ValueInput input) {
        return new PlayerTrustPermissions(
                input.getBooleanOr("CanBuildAndMine", false),
                input.getBooleanOr("CanUseDoorsAndSwitches", false),
                input.getBooleanOr("CanOpenContainers", false),
                input.getBooleanOr("CanAttackPlayers", false),
                input.getBooleanOr("CanAttackAnimals", false)
        );
    }

    public PlayerTrustPermissions withCanBuildAndMine(boolean value) {
        return new PlayerTrustPermissions(value, canUseDoorsAndSwitches, canOpenContainers, canAttackPlayers, canAttackAnimals);
    }

    public PlayerTrustPermissions withCanUseDoorsAndSwitches(boolean value) {
        return new PlayerTrustPermissions(canBuildAndMine, value, canOpenContainers, canAttackPlayers, canAttackAnimals);
    }

    public PlayerTrustPermissions withCanOpenContainers(boolean value) {
        return new PlayerTrustPermissions(canBuildAndMine, canUseDoorsAndSwitches, value, canAttackPlayers, canAttackAnimals);
    }

    public PlayerTrustPermissions withCanAttackPlayers(boolean value) {
        return new PlayerTrustPermissions(canBuildAndMine, canUseDoorsAndSwitches, canOpenContainers, value, canAttackAnimals);
    }

    public PlayerTrustPermissions withCanAttackAnimals(boolean value) {
        return new PlayerTrustPermissions(canBuildAndMine, canUseDoorsAndSwitches, canOpenContainers, canAttackPlayers, value);
    }

    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeBoolean(canBuildAndMine);
        buf.get().writeBoolean(canUseDoorsAndSwitches);
        buf.get().writeBoolean(canOpenContainers);
        buf.get().writeBoolean(canAttackPlayers);
        buf.get().writeBoolean(canAttackAnimals);
    }

    public void save(ValueOutput output) {
        output.putBoolean("CanBuildAndMine", canBuildAndMine);
        output.putBoolean("CanUseDoorsAndSwitches", canUseDoorsAndSwitches);
        output.putBoolean("CanOpenContainers", canOpenContainers);
        output.putBoolean("CanAttackPlayers", canAttackPlayers);
        output.putBoolean("CanAttackAnimals", canAttackAnimals);
    }
}

package wily.legacy.entity;

public interface LegacyPlayer {

    boolean hasClassicCrafting();
    void setCrafting(boolean classic);

    default void copyFrom(LegacyPlayer player){
        setCrafting(player.hasClassicCrafting());
    }

}

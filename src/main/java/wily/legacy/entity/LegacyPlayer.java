package wily.legacy.entity;

public interface LegacyPlayer {

    boolean hasClassicCrafting();

    void setCrafting(boolean classic);

    boolean hasClassicTrading();

    void setTrading(boolean classic);

    boolean hasClassicStonecutting();

    void setStonecutting(boolean classic);

    boolean hasClassicLoom();

    void setLoom(boolean classic);

    default void copyFrom(LegacyPlayer player) {
        setCrafting(player.hasClassicCrafting());
        setTrading(player.hasClassicTrading());
        setStonecutting(player.hasClassicStonecutting());
        setLoom(player.hasClassicLoom());
    }

}

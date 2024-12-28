package wily.legacy.client;

import wily.legacy.client.screen.Assort;

public interface LegacyClientWorldSettings {
    long getDisplaySeed();
    void setDisplaySeed(long s);
    boolean trustPlayers();
    void setTrustPlayers(boolean trust);
    boolean isDifficultyLocked();
    void setDifficultyLocked(boolean locked);
    void setAllowCommands(boolean allow);
    void setSelectedResourceAssort(Assort assort);
    Assort getSelectedResourceAssort();

    static LegacyClientWorldSettings of(Object object){
        return (LegacyClientWorldSettings) object;
    }
}

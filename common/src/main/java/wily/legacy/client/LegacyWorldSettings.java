package wily.legacy.client;

import java.util.List;

public interface LegacyWorldSettings {
    long getDisplaySeed();
    void setDisplaySeed(long s);
    boolean trustPlayers();
    void setTrustPlayers(boolean trust);
    boolean isDifficultyLocked();
    void setDifficultyLocked(boolean locked);
    void setAllowCommands(boolean allow);
    void setSelectedResourcePacks(List<String> packs);
    List<String> getSelectedResourcePacks();
}

package wily.legacy.client;

public interface LegacyWorldSettings {
    long getDisplaySeed();
    void setDisplaySeed(long s);
    boolean trustPlayers();
    void setTrustPlayers(boolean trust);
    boolean isDifficultyLocked();
    void setDifficultyLocked(boolean locked);
    void setAllowCommands(boolean allow);
}

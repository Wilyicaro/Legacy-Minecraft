package wily.legacy.client;

public interface LegacyClientWorldSettings {
    long getDisplaySeed();
    void setDisplaySeed(long s);
    boolean trustPlayers();
    void setTrustPlayers(boolean trust);
    boolean isDifficultyLocked();
    void setDifficultyLocked(boolean locked);
    void setAllowCommands(boolean allow);
    void setSelectedResourceAlbum(PackAlbum album);
    PackAlbum getSelectedResourceAlbum();

    static LegacyClientWorldSettings of(Object object){
        return (LegacyClientWorldSettings) object;
    }
}

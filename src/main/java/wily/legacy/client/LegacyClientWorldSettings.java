package wily.legacy.client;

public interface LegacyClientWorldSettings {
    static LegacyClientWorldSettings of(Object object) {
        return (LegacyClientWorldSettings) object;
    }

    long getDisplaySeed();

    void setDisplaySeed(long s);

    boolean trustPlayers();

    void setTrustPlayers(boolean trust);

    boolean isDifficultyLocked();

    void setDifficultyLocked(boolean locked);

    void setAllowCommands(boolean allow);

    PackAlbum getSelectedResourceAlbum();

    void setSelectedResourceAlbum(PackAlbum album);
}
